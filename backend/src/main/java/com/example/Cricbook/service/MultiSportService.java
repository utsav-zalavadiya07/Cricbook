package com.example.Cricbook.service;

import com.example.Cricbook.dto.MultiSportRequests;
import com.example.Cricbook.entity.*;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MultiSportService {

    private static final Set<String> SCORING_TYPES = Set.of("RUNS_WICKETS_OVERS", "GOALS", "SETS", "POINTS", "CUSTOM");
    private static final Set<String> MATCH_TYPES = Set.of("TEAM", "SINGLES", "DOUBLES", "TEAM_OR_INDIVIDUAL", "CUSTOM");
    private static final Set<String> RANKING_TYPES = Set.of("POINTS_TABLE", "ELO", "WIN_PERCENTAGE", "SET_POINTS", "CUSTOM");

    private final SportRepository sportRepository;
    private final SportTurfRepository sportTurfRepository;
    private final SportConfigurationAuditRepository sportConfigurationAuditRepository;
    private final TurfRepository turfRepository;
    private final TeamRepository teamRepository;
    private final TournamentRepository tournamentRepository;
    private final PublicMatchRepository publicMatchRepository;
    private final BookingRepository bookingRepository;
    private final AuditLogRepository auditLogRepository;

    @Transactional
    public Map<String, Object> dashboard() {
        ensureDefaultSports("system");
        List<Sport> sports = orderedSports();
        List<SportTurf> sportTurfs = sportTurfRepository.findAll();
        List<Team> teams = teamRepository.findAll();
        List<Tournament> tournaments = tournamentRepository.findAll();
        List<PublicMatch> matches = publicMatchRepository.findAll();
        List<Booking> bookings = bookingRepository.findAll();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("kpis", kpis(sports, sportTurfs, teams, tournaments, matches, bookings));
        response.put("catalog", sports.stream().map(this::mapSport).toList());
        response.put("activeSports", sports.stream().filter(this::isActive).map(this::mapSport).toList());
        response.put("sportTemplates", defaultTemplates().stream().map(this::mapTemplate).toList());
        response.put("turfCoverage", turfCoverage(sports, sportTurfs));
        response.put("crossModuleStatus", crossModuleStatus(sports, teams, tournaments, matches, bookings));
        response.put("futureSportReadiness", futureSportReadiness(sports));
        response.put("governanceControls", governanceControls());
        response.put("priorityActions", priorityActions(sports, sportTurfs, teams, tournaments));
        response.put("generatedAt", LocalDateTime.now());
        return response;
    }

    @Transactional
    public List<Map<String, Object>> catalog(boolean activeOnly) {
        ensureDefaultSports("system");
        List<Sport> sports = activeOnly
                ? sportRepository.findByIsActiveTrueOrderByDisplayOrderAscNameAsc()
                : orderedSports();
        return sports.stream().map(this::mapSport).toList();
    }

    @Transactional
    public Map<String, Object> rules(String code) {
        ensureDefaultSports("system");
        return mapRules(resolveSport(code));
    }

    @Transactional
    public Map<String, Object> createSport(MultiSportRequests.SportConfigRequest request, String actor) {
        validateSportRequest(request, null);
        String name = required(request.getName(), "Sport name is required");
        String code = normalizeCode(request.getCode() == null ? name : request.getCode());
        if (sportRepository.existsByNameIgnoreCase(name)) {
            throw ApiException.conflict("Sport name already exists");
        }
        if (sportRepository.existsByCodeIgnoreCase(code)) {
            throw ApiException.conflict("Sport code already exists");
        }

        Sport sport = Sport.builder()
                .name(name)
                .code(code)
                .icon(defaultString(request.getIcon(), "SP"))
                .rules(blankToNull(request.getRules()))
                .isActive(request.getActive() == null ? true : request.getActive())
                .displayOrder(request.getDisplayOrder() == null ? nextDisplayOrder() : request.getDisplayOrder())
                .minPlayers(request.getMinPlayers())
                .maxPlayers(request.getMaxPlayers())
                .scoringType(normalizeEnum(request.getScoringType(), "CUSTOM"))
                .matchType(normalizeEnum(request.getMatchType(), "CUSTOM"))
                .rankingType(normalizeEnum(request.getRankingType(), "CUSTOM"))
                .teamBased(request.getTeamBased() == null ? true : request.getTeamBased())
                .tournamentEnabled(request.getTournamentEnabled() == null ? true : request.getTournamentEnabled())
                .statisticsEnabled(request.getStatisticsEnabled() == null ? true : request.getStatisticsEnabled())
                .configurationJson(blankToNull(request.getConfigurationJson()))
                .metadataJson(blankToNull(request.getMetadataJson()))
                .build();
        Sport saved = sportRepository.save(sport);
        audit(saved, "SPORT_CREATED", actor, "Created sport " + saved.getCode());
        return mapSport(saved);
    }

    @Transactional
    public Map<String, Object> updateSport(Long id, MultiSportRequests.SportConfigRequest request, String actor) {
        Sport sport = sportRepository.findById(id).orElseThrow(() -> ApiException.notFound("Sport not found"));
        validateSportRequest(request, sport);
        String name = required(request.getName(), "Sport name is required");
        String code = normalizeCode(request.getCode() == null ? name : request.getCode());

        sportRepository.findByNameIgnoreCase(name)
                .filter(existing -> !Objects.equals(existing.getId(), sport.getId()))
                .ifPresent(existing -> {
                    throw ApiException.conflict("Sport name already exists");
                });
        sportRepository.findByCodeIgnoreCase(code)
                .filter(existing -> !Objects.equals(existing.getId(), sport.getId()))
                .ifPresent(existing -> {
                    throw ApiException.conflict("Sport code already exists");
                });

        sport.setName(name);
        sport.setCode(code);
        sport.setIcon(defaultString(request.getIcon(), "SP"));
        sport.setRules(blankToNull(request.getRules()));
        sport.setIsActive(request.getActive() == null ? sport.getIsActive() : request.getActive());
        sport.setDisplayOrder(request.getDisplayOrder() == null ? sport.getDisplayOrder() : request.getDisplayOrder());
        sport.setMinPlayers(request.getMinPlayers());
        sport.setMaxPlayers(request.getMaxPlayers());
        sport.setScoringType(normalizeEnum(request.getScoringType(), "CUSTOM"));
        sport.setMatchType(normalizeEnum(request.getMatchType(), "CUSTOM"));
        sport.setRankingType(normalizeEnum(request.getRankingType(), "CUSTOM"));
        sport.setTeamBased(request.getTeamBased() == null ? sport.getTeamBased() : request.getTeamBased());
        sport.setTournamentEnabled(request.getTournamentEnabled() == null ? sport.getTournamentEnabled() : request.getTournamentEnabled());
        sport.setStatisticsEnabled(request.getStatisticsEnabled() == null ? sport.getStatisticsEnabled() : request.getStatisticsEnabled());
        sport.setConfigurationJson(blankToNull(request.getConfigurationJson()));
        sport.setMetadataJson(blankToNull(request.getMetadataJson()));

        Sport saved = sportRepository.save(sport);
        audit(saved, "SPORT_CONFIG_UPDATED", actor, "Updated sport configuration " + saved.getCode());
        return mapSport(saved);
    }

    @Transactional
    public Map<String, Object> setSportStatus(Long id, MultiSportRequests.SportStatusRequest request, String actor) {
        Sport sport = sportRepository.findById(id).orElseThrow(() -> ApiException.notFound("Sport not found"));
        if (request == null || request.getActive() == null) {
            throw ApiException.badRequest("Sport active status is required");
        }
        sport.setIsActive(request.getActive());
        Sport saved = sportRepository.save(sport);
        audit(saved, request.getActive() ? "SPORT_ACTIVATED" : "SPORT_DEACTIVATED", actor, defaultString(request.getReason(), "Status changed"));
        return mapSport(saved);
    }

    @Transactional
    public Map<String, Object> mapSportToTurf(MultiSportRequests.SportTurfMappingRequest request, String actor) {
        Sport sport = resolveSport(request == null ? null : request.getSportId(), request == null ? null : request.getSportCode());
        Long turfId = request == null ? null : request.getTurfId();
        if (turfId == null) {
            throw ApiException.badRequest("Turf id is required");
        }
        Turf turf = turfRepository.findById(turfId).orElseThrow(() -> ApiException.notFound("Turf not found"));
        if (!isActive(sport)) {
            throw ApiException.conflict("Disabled sport cannot be mapped to turf");
        }
        if (!sportTurfRepository.existsBySportIdAndTurfId(sport.getId(), turf.getId())) {
            sportTurfRepository.save(SportTurf.builder().sportId(sport.getId()).turfId(turf.getId()).build());
        }
        audit(sport, "SPORT_TURF_MAPPED", actor, "Mapped " + sport.getCode() + " to turf #" + turf.getId());
        return Map.of("sport", mapSport(sport), "turfId", turf.getId(), "mapped", true);
    }

    @Transactional
    public Map<String, Object> removeSportFromTurf(Long sportId, Long turfId, String actor) {
        Sport sport = sportRepository.findById(sportId).orElseThrow(() -> ApiException.notFound("Sport not found"));
        sportTurfRepository.deleteBySportIdAndTurfId(sportId, turfId);
        audit(sport, "SPORT_TURF_REMOVED", actor, "Removed sport mapping from turf #" + turfId);
        return Map.of("sportId", sportId, "turfId", turfId, "mapped", false);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> validateBooking(MultiSportRequests.SportBookingValidationRequest request) {
        Sport sport = resolveSport(request == null ? null : request.getSportCode());
        Long turfId = request == null ? null : request.getTurfId();
        if (turfId == null) {
            throw ApiException.badRequest("Turf id is required");
        }
        boolean turfExists = turfRepository.existsById(turfId);
        boolean mapped = sport.getId() != null && sportTurfRepository.existsBySportIdAndTurfId(sport.getId(), turfId);
        boolean legacyCricketFallback = mapped || ("CRICKET".equalsIgnoreCase(safeCode(sport)) && sportTurfRepository.findByTurfId(turfId).isEmpty());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("sport", mapSport(sport));
        response.put("turfId", turfId);
        response.put("valid", turfExists && isActive(sport) && legacyCricketFallback);
        response.put("reason", !turfExists ? "TURF_NOT_FOUND" : !isActive(sport) ? "SPORT_DISABLED" : !legacyCricketFallback ? "SPORT_NOT_SUPPORTED_BY_TURF" : "SUPPORTED");
        return response;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> validateTeam(MultiSportRequests.SportTeamValidationRequest request) {
        Sport sport = resolveSport(request == null ? null : request.getSportCode());
        int players = request == null || request.getPlayers() == null ? 0 : request.getPlayers();
        Integer minPlayers = sport.getMinPlayers();
        Integer maxPlayers = sport.getMaxPlayers();
        boolean valid = isActive(sport)
                && (minPlayers == null || players >= minPlayers)
                && (maxPlayers == null || players <= maxPlayers);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("sport", mapSport(sport));
        response.put("players", players);
        response.put("valid", valid);
        response.put("minPlayers", minPlayers);
        response.put("maxPlayers", maxPlayers);
        response.put("reason", !isActive(sport) ? "SPORT_DISABLED" : valid ? "WITHIN_LIMITS" : "TEAM_SIZE_OUT_OF_RANGE");
        return response;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> simulateRules(MultiSportRequests.SportRuleSimulationRequest request) {
        Sport sport = resolveSport(request == null ? null : request.getSportCode());
        String scoringType = normalizeEnum(sport.getScoringType(), "CUSTOM");
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("sportCode", safeCode(sport));
        response.put("matchType", normalizeEnum(request == null ? sport.getMatchType() : request.getMatchType(), sport.getMatchType()));
        response.put("playersPerSide", request == null ? null : request.getPlayersPerSide());
        response.put("scoreModel", scoreModel(scoringType));
        response.put("winnerLogic", winnerLogic(scoringType));
        response.put("rankingImpact", sport.getRankingType());
        response.put("explainability", "Uses sport configuration for scoring, match type, ranking type, and future custom payload metadata.");
        return response;
    }

    @Transactional
    public Map<String, Object> analytics() {
        ensureDefaultSports("system");
        List<Sport> sports = orderedSports();
        Map<String, Long> teamsBySport = countBySport(teamRepository.findAll(), Team::getSport);
        Map<String, Long> tournamentsBySport = countBySport(tournamentRepository.findAll(), Tournament::getSport);
        Map<String, Long> matchesBySport = countBySport(publicMatchRepository.findAll(), PublicMatch::getSport);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("teamsBySport", teamsBySport);
        response.put("tournamentsBySport", tournamentsBySport);
        response.put("matchesBySport", matchesBySport);
        response.put("configuredSports", sports.stream().map(Sport::getCode).filter(Objects::nonNull).toList());
        response.put("segmentation", sports.stream().map(sport -> {
            Map<String, Object> row = new LinkedHashMap<>();
            String code = safeCode(sport);
            row.put("sportCode", code);
            row.put("teams", teamsBySport.getOrDefault(code, 0L));
            row.put("tournaments", tournamentsBySport.getOrDefault(code, 0L));
            row.put("matches", matchesBySport.getOrDefault(code, 0L));
            row.put("turfMappings", sportTurfRepository.findBySportId(sport.getId()).size());
            return row;
        }).toList());
        response.put("generatedAt", LocalDateTime.now());
        return response;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> audits() {
        return sportConfigurationAuditRepository.findTop50ByOrderByCreatedAtDesc().stream()
                .map(this::mapAudit)
                .toList();
    }

    @Transactional
    public Map<String, Object> seedDefaults(String actor) {
        int before = sportRepository.findAll().size();
        ensureDefaultSports(actor);
        int after = sportRepository.findAll().size();
        auditLogRepository.save(AuditLog.builder()
                .adminId(defaultString(actor, "system"))
                .action("MULTI_SPORT_DEFAULTS_SEEDED")
                .entity("Sports #" + before + " -> " + after)
                .build());
        return Map.of("before", before, "after", after, "seeded", Math.max(0, after - before));
    }

    @Transactional
    public void ensureDefaultSportsAvailable() {
        ensureDefaultSports("system");
    }

    private void ensureDefaultSports(String actor) {
        for (SportTemplate template : defaultTemplates()) {
            sportRepository.findByCodeIgnoreCase(template.code())
                    .or(() -> sportRepository.findByNameIgnoreCase(template.name()))
                    .ifPresentOrElse(existing -> applyTemplate(existing, template), () -> sportRepository.save(fromTemplate(template)));
        }
    }

    public Optional<Sport> findActiveSport(String codeOrName) {
        String normalized = normalizeCode(codeOrName);
        return sportRepository.findByCodeIgnoreCase(normalized)
                .or(() -> sportRepository.findByNameIgnoreCase(defaultString(codeOrName, "")))
                .filter(this::isActive);
    }

    public boolean turfSupportsSport(Long turfId, String codeOrName) {
        if (turfId == null) {
            return false;
        }
        return turfSupportByTurfId(List.of(turfId), codeOrName).getOrDefault(turfId, false);
    }

    @Transactional(readOnly = true)
    public Map<Long, Boolean> turfSupportByTurfId(Collection<Long> turfIds, String codeOrName) {
        List<Long> ids = normalizedIds(turfIds);
        if (ids.isEmpty()) {
            return Map.of();
        }

        String requestedCode = normalizeCode(codeOrName);
        Optional<Sport> sport = findActiveSport(codeOrName);
        Map<Long, List<SportTurf>> mappingsByTurfId = mappingsByTurfId(ids);

        Map<Long, Boolean> support = new LinkedHashMap<>();
        for (Long turfId : ids) {
            List<SportTurf> mappings = mappingsByTurfId.getOrDefault(turfId, List.of());
            boolean legacyCricket = "CRICKET".equalsIgnoreCase(defaultString(requestedCode, "")) && mappings.isEmpty();
            boolean mapped = sport
                    .map(value -> mappings.stream().anyMatch(mapping -> Objects.equals(mapping.getSportId(), value.getId())))
                    .orElse(false);
            support.put(turfId, mapped || legacyCricket);
        }
        return support;
    }

    @Transactional(readOnly = true)
    public List<String> activeSportCodesForTurf(Long turfId) {
        if (turfId == null) {
            return List.of();
        }
        return activeSportCodesByTurfId(List.of(turfId)).getOrDefault(turfId, List.of());
    }

    @Transactional(readOnly = true)
    public Map<Long, List<String>> activeSportCodesByTurfId(Collection<Long> turfIds) {
        List<Long> ids = normalizedIds(turfIds);
        if (ids.isEmpty()) {
            return Map.of();
        }

        Map<Long, List<SportTurf>> mappingsByTurfId = mappingsByTurfId(ids);
        Map<Long, Sport> sportsById = sportRepository.findByIsActiveTrueOrderByDisplayOrderAscNameAsc().stream()
                .collect(Collectors.toMap(Sport::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));

        Map<Long, List<String>> codesByTurfId = new LinkedHashMap<>();
        for (Long turfId : ids) {
            List<SportTurf> mappings = mappingsByTurfId.getOrDefault(turfId, List.of());
            if (sportsById.isEmpty() && mappings.isEmpty()) {
                codesByTurfId.put(turfId, List.of("CRICKET"));
            } else if (mappings.isEmpty()) {
                codesByTurfId.put(turfId, sportsById.values().stream()
                        .filter(sport -> "CRICKET".equalsIgnoreCase(safeCode(sport)))
                        .map(this::safeCode)
                        .toList());
            } else {
                codesByTurfId.put(turfId, mappings.stream()
                        .map(SportTurf::getSportId)
                        .map(sportsById::get)
                        .filter(Objects::nonNull)
                        .map(this::safeCode)
                        .distinct()
                        .toList());
            }
        }
        return codesByTurfId;
    }

    private List<Long> normalizedIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private Map<Long, List<SportTurf>> mappingsByTurfId(List<Long> turfIds) {
        if (turfIds.isEmpty()) {
            return Map.of();
        }
        return sportTurfRepository.findByTurfIdIn(turfIds).stream()
                .collect(Collectors.groupingBy(SportTurf::getTurfId, LinkedHashMap::new, Collectors.toList()));
    }

    private Map<String, Object> kpis(List<Sport> sports, List<SportTurf> sportTurfs, List<Team> teams, List<Tournament> tournaments, List<PublicMatch> matches, List<Booking> bookings) {
        Map<String, Object> kpis = new LinkedHashMap<>();
        kpis.put("configuredSports", sports.size());
        kpis.put("activeSports", sports.stream().filter(this::isActive).count());
        kpis.put("disabledSports", sports.stream().filter(sport -> !isActive(sport)).count());
        kpis.put("multiSportTurfs", sportTurfs.stream().collect(Collectors.groupingBy(SportTurf::getTurfId, Collectors.counting())).values().stream().filter(count -> count > 1).count());
        kpis.put("sportTurfMappings", sportTurfs.size());
        kpis.put("teamLinkedSports", countBySport(teams, Team::getSport).size());
        kpis.put("tournamentLinkedSports", countBySport(tournaments, Tournament::getSport).size());
        kpis.put("matchLinkedSports", countBySport(matches, PublicMatch::getSport).size());
        kpis.put("legacyBookings", bookings.size());
        kpis.put("futureSportReadinessScore", readinessScore(sports, sportTurfs));
        return kpis;
    }

    private List<Map<String, Object>> turfCoverage(List<Sport> sports, List<SportTurf> sportTurfs) {
        Map<Long, Long> coverage = sportTurfs.stream().collect(Collectors.groupingBy(SportTurf::getSportId, Collectors.counting()));
        return sports.stream().map(sport -> {
            Map<String, Object> item = mapSport(sport);
            item.put("mappedTurfs", coverage.getOrDefault(sport.getId(), 0L));
            item.put("searchVisibility", isActive(sport) ? "VISIBLE_WHEN_MAPPED" : "HIDDEN");
            return item;
        }).toList();
    }

    private List<Map<String, Object>> crossModuleStatus(List<Sport> sports, List<Team> teams, List<Tournament> tournaments, List<PublicMatch> matches, List<Booking> bookings) {
        long activeSports = sports.stream().filter(this::isActive).count();
        return List.of(
                moduleStatus("Catalog", activeSports, "Sports configured and globally visible through catalog APIs"),
                moduleStatus("Booking", bookings.size(), "Booking validation API enforces active sport and turf mapping"),
                moduleStatus("Teams", countBySport(teams, Team::getSport).size(), "Teams already carry sport metadata for sport-specific limits"),
                moduleStatus("Matches", countBySport(matches, PublicMatch::getSport).size(), "Public match engine can consume configured sport rules"),
                moduleStatus("Tournaments", countBySport(tournaments, Tournament::getSport).size(), "Tournament records carry sport for segmentation"),
                moduleStatus("Analytics", activeSports, "Sport segmentation available for dashboards and BI")
        );
    }

    private Map<String, Object> moduleStatus(String module, long records, String description) {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("module", module);
        status.put("records", records);
        status.put("status", records > 0 ? "CONNECTED" : "READY");
        status.put("description", description);
        return status;
    }

    private Map<String, Object> futureSportReadiness(List<Sport> sports) {
        Map<String, Object> readiness = new LinkedHashMap<>();
        readiness.put("noCodeOnboarding", true);
        readiness.put("customScoringSupported", true);
        readiness.put("customMetadataSupported", true);
        readiness.put("configuredCustomSports", sports.stream().filter(sport -> "CUSTOM".equalsIgnoreCase(defaultString(sport.getScoringType(), ""))).count());
        readiness.put("nextSportExamples", List.of("PADEL", "SWIMMING", "SKATING"));
        readiness.put("wizardSteps", List.of("Create sport", "Configure players", "Configure scoring", "Map turfs", "Enable search", "Enable teams and rankings"));
        return readiness;
    }

    private List<Map<String, Object>> governanceControls() {
        return List.of(
                governance("SPORT_LIFECYCLE", "Active, inactive, archival, and reactivation workflows"),
                governance("RULE_AUDIT", "Every sport configuration update creates immutable audit entries"),
                governance("DATA_ISOLATION", "Sport segmentation avoids cricket, football, racket-sport stat mixing"),
                governance("NO_CODE_EXPANSION", "Future sports use config JSON and templates instead of redeployment"),
                governance("ROLLBACK_READY", "Configuration changes are auditable and can be restored from previous values")
        );
    }

    private Map<String, Object> governance(String key, String description) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("key", key);
        item.put("description", description);
        item.put("status", "ENABLED");
        return item;
    }

    private List<Map<String, Object>> priorityActions(List<Sport> sports, List<SportTurf> sportTurfs, List<Team> teams, List<Tournament> tournaments) {
        List<Map<String, Object>> actions = new ArrayList<>();
        if (sports.stream().noneMatch(sport -> "FOOTBALL".equalsIgnoreCase(safeCode(sport)))) {
            actions.add(action("Seed football template", "Add Football rules before marketplace launch", "HIGH"));
        }
        if (sportTurfs.isEmpty()) {
            actions.add(action("Map sports to turfs", "Search and booking isolation need sport-turf mappings", "HIGH"));
        }
        if (teams.stream().anyMatch(team -> blankToNull(team.getSport()) == null)) {
            actions.add(action("Backfill team sports", "Legacy teams without sport should be assigned Cricket or the configured sport", "MEDIUM"));
        }
        if (tournaments.stream().anyMatch(tournament -> blankToNull(tournament.getSport()) == null)) {
            actions.add(action("Backfill tournament sports", "Legacy tournaments need sport segmentation for analytics", "MEDIUM"));
        }
        actions.add(action("Validate rule engine", "Run sport-specific scoring simulations for Cricket, Football, Badminton, and Tennis", "MEDIUM"));
        return actions;
    }

    private Map<String, Object> action(String title, String description, String severity) {
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("title", title);
        action.put("description", description);
        action.put("severity", severity);
        return action;
    }

    private Map<String, Object> mapSport(Sport sport) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", sport.getId());
        item.put("name", sport.getName());
        item.put("code", safeCode(sport));
        item.put("icon", sport.getIcon());
        item.put("rules", sport.getRules());
        item.put("active", isActive(sport));
        item.put("displayOrder", sport.getDisplayOrder());
        item.put("minPlayers", sport.getMinPlayers());
        item.put("maxPlayers", sport.getMaxPlayers());
        item.put("scoringType", sport.getScoringType());
        item.put("matchType", sport.getMatchType());
        item.put("rankingType", sport.getRankingType());
        item.put("teamBased", bool(sport.getTeamBased(), true));
        item.put("tournamentEnabled", bool(sport.getTournamentEnabled(), true));
        item.put("statisticsEnabled", bool(sport.getStatisticsEnabled(), true));
        item.put("configurationJson", sport.getConfigurationJson());
        item.put("metadataJson", sport.getMetadataJson());
        item.put("createdAt", sport.getCreatedAt());
        item.put("updatedAt", sport.getUpdatedAt());
        return item;
    }

    private Map<String, Object> mapRules(Sport sport) {
        Map<String, Object> rules = mapSport(sport);
        rules.put("scoreModel", scoreModel(normalizeEnum(sport.getScoringType(), "CUSTOM")));
        rules.put("winnerLogic", winnerLogic(normalizeEnum(sport.getScoringType(), "CUSTOM")));
        rules.put("rankingFormula", rankingFormula(normalizeEnum(sport.getRankingType(), "CUSTOM")));
        rules.put("dynamicUiHints", dynamicUiHints(sport));
        return rules;
    }

    private Map<String, Object> mapTemplate(SportTemplate template) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("name", template.name());
        item.put("code", template.code());
        item.put("icon", template.icon());
        item.put("minPlayers", template.minPlayers());
        item.put("maxPlayers", template.maxPlayers());
        item.put("scoringType", template.scoringType());
        item.put("matchType", template.matchType());
        item.put("rankingType", template.rankingType());
        item.put("rules", template.rules());
        return item;
    }

    private Map<String, Object> mapAudit(SportConfigurationAudit audit) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", audit.getId());
        item.put("sportId", audit.getSportId());
        item.put("sportCode", audit.getSportCode());
        item.put("action", audit.getAction());
        item.put("actor", audit.getActor());
        item.put("summary", audit.getSummary());
        item.put("createdAt", audit.getCreatedAt());
        return item;
    }

    private Map<String, Object> scoreModel(String scoringType) {
        Map<String, Object> model = new LinkedHashMap<>();
        switch (scoringType) {
            case "RUNS_WICKETS_OVERS" -> {
                model.put("primaryMetric", "runs");
                model.put("secondaryMetrics", List.of("wickets", "overs", "strikeRate", "runRate"));
                model.put("periodUnit", "innings");
            }
            case "GOALS" -> {
                model.put("primaryMetric", "goals");
                model.put("secondaryMetrics", List.of("assists", "cards", "possession"));
                model.put("periodUnit", "halves");
            }
            case "SETS" -> {
                model.put("primaryMetric", "setsWon");
                model.put("secondaryMetrics", List.of("games", "points", "tieBreak"));
                model.put("periodUnit", "set");
            }
            case "POINTS" -> {
                model.put("primaryMetric", "points");
                model.put("secondaryMetrics", List.of("sets", "rounds", "differential"));
                model.put("periodUnit", "round");
            }
            default -> {
                model.put("primaryMetric", "customScore");
                model.put("secondaryMetrics", List.of("metadata"));
                model.put("periodUnit", "custom");
            }
        }
        return model;
    }

    private String winnerLogic(String scoringType) {
        return switch (scoringType) {
            case "RUNS_WICKETS_OVERS" -> "Higher valid run total after innings and overs rules";
            case "GOALS" -> "Higher goal count after regulation time or configured tie-break";
            case "SETS" -> "Configured sets won, then games, then tie-break points";
            case "POINTS" -> "Higher point total after configured rounds or sets";
            default -> "Custom winner function supplied by sport configuration";
        };
    }

    private String rankingFormula(String rankingType) {
        return switch (rankingType) {
            case "POINTS_TABLE" -> "Win points, draw points, loss points, then configured tie-breakers";
            case "ELO" -> "Opponent strength adjusted rating delta";
            case "WIN_PERCENTAGE" -> "Wins divided by completed matches with minimum-match guard";
            case "SET_POINTS" -> "Sets won, points differential, and head-to-head tie-breakers";
            default -> "Custom ranking formula supplied by sport configuration";
        };
    }

    private List<String> dynamicUiHints(Sport sport) {
        return switch (normalizeEnum(sport.getScoringType(), "CUSTOM")) {
            case "RUNS_WICKETS_OVERS" -> List.of("runs", "wickets", "overs", "innings", "battingOrder");
            case "GOALS" -> List.of("goals", "assists", "cards", "formation", "halves");
            case "SETS" -> List.of("sets", "games", "tieBreak", "singlesDoubles");
            case "POINTS" -> List.of("points", "rounds", "differential");
            default -> List.of("customFields", "metadataJson", "configuredScoreInputs");
        };
    }

    private void validateSportRequest(MultiSportRequests.SportConfigRequest request, Sport existing) {
        if (request == null) {
            throw ApiException.badRequest("Sport configuration is required");
        }
        required(request.getName(), "Sport name is required");
        Integer minPlayers = request.getMinPlayers();
        Integer maxPlayers = request.getMaxPlayers();
        if (minPlayers != null && minPlayers < 1) {
            throw ApiException.badRequest("Minimum players must be at least 1");
        }
        if (maxPlayers != null && maxPlayers < 1) {
            throw ApiException.badRequest("Maximum players must be at least 1");
        }
        if (minPlayers != null && maxPlayers != null && minPlayers > maxPlayers) {
            throw ApiException.badRequest("Minimum players cannot exceed maximum players");
        }
        String scoringType = normalizeEnum(request.getScoringType(), existing == null ? "CUSTOM" : existing.getScoringType());
        String matchType = normalizeEnum(request.getMatchType(), existing == null ? "CUSTOM" : existing.getMatchType());
        String rankingType = normalizeEnum(request.getRankingType(), existing == null ? "CUSTOM" : existing.getRankingType());
        if (!SCORING_TYPES.contains(scoringType)) {
            throw ApiException.badRequest("Unsupported scoring type");
        }
        if (!MATCH_TYPES.contains(matchType)) {
            throw ApiException.badRequest("Unsupported match type");
        }
        if (!RANKING_TYPES.contains(rankingType)) {
            throw ApiException.badRequest("Unsupported ranking type");
        }
    }

    private Sport resolveSport(String codeOrName) {
        String normalized = normalizeCode(codeOrName);
        if (normalized == null) {
            throw ApiException.badRequest("Sport code is required");
        }
        return sportRepository.findByCodeIgnoreCase(normalized)
                .or(() -> sportRepository.findByNameIgnoreCase(defaultString(codeOrName, "")))
                .orElseThrow(() -> ApiException.notFound("Sport not found"));
    }

    private Sport resolveSport(Long sportId, String codeOrName) {
        if (sportId != null) {
            return sportRepository.findById(sportId).orElseThrow(() -> ApiException.notFound("Sport not found"));
        }
        return resolveSport(codeOrName);
    }

    private List<Sport> orderedSports() {
        return sportRepository.findAllByOrderByDisplayOrderAscNameAsc();
    }

    private boolean isActive(Sport sport) {
        return Boolean.TRUE.equals(sport.getIsActive());
    }

    private boolean bool(Boolean value, boolean fallback) {
        return value == null ? fallback : value;
    }

    private String safeCode(Sport sport) {
        return sport.getCode() == null ? normalizeCode(sport.getName()) : sport.getCode();
    }

    private String required(String value, String message) {
        String cleaned = blankToNull(value);
        if (cleaned == null) {
            throw ApiException.badRequest(message);
        }
        return cleaned;
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim().replaceAll("[<>]", "");
        return cleaned.isBlank() ? null : cleaned;
    }

    private String defaultString(String value, String fallback) {
        String cleaned = blankToNull(value);
        return cleaned == null ? fallback : cleaned;
    }

    private String normalizeCode(String value) {
        String cleaned = blankToNull(value);
        if (cleaned == null) {
            return null;
        }
        return cleaned.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_").replaceAll("^_+|_+$", "");
    }

    private String normalizeEnum(String value, String fallback) {
        String source = blankToNull(value);
        if (source == null) {
            source = blankToNull(fallback);
        }
        return source == null ? "CUSTOM" : normalizeCode(source);
    }

    private int nextDisplayOrder() {
        return sportRepository.findAll().stream()
                .map(Sport::getDisplayOrder)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0) + 10;
    }

    private int readinessScore(List<Sport> sports, List<SportTurf> sportTurfs) {
        int score = 30;
        if (sports.stream().anyMatch(sport -> "CRICKET".equalsIgnoreCase(safeCode(sport)))) score += 10;
        if (sports.stream().anyMatch(sport -> "FOOTBALL".equalsIgnoreCase(safeCode(sport)))) score += 10;
        if (sports.stream().anyMatch(sport -> "BADMINTON".equalsIgnoreCase(safeCode(sport)))) score += 10;
        if (sports.stream().anyMatch(sport -> "TENNIS".equalsIgnoreCase(safeCode(sport)))) score += 10;
        if (!sportTurfs.isEmpty()) score += 15;
        if (sports.stream().anyMatch(sport -> blankToNull(sport.getConfigurationJson()) != null)) score += 15;
        return Math.min(100, score);
    }

    private <T> Map<String, Long> countBySport(List<T> values, Function<T, String> sportResolver) {
        return values.stream()
                .map(sportResolver)
                .map(this::normalizeCode)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Function.identity(), LinkedHashMap::new, Collectors.counting()));
    }

    private void audit(Sport sport, String action, String actor, String summary) {
        sportConfigurationAuditRepository.save(SportConfigurationAudit.builder()
                .sportId(sport.getId())
                .sportCode(safeCode(sport))
                .action(action)
                .actor(defaultString(actor, "system"))
                .summary(summary)
                .build());
        auditLogRepository.save(AuditLog.builder()
                .adminId(defaultString(actor, "system"))
                .action(action)
                .entity("Sport #" + sport.getId())
                .build());
    }

    private void applyTemplate(Sport sport, SportTemplate template) {
        if (sport.getCode() == null) sport.setCode(template.code());
        if (sport.getDisplayOrder() == null) sport.setDisplayOrder(template.displayOrder());
        if (sport.getMinPlayers() == null) sport.setMinPlayers(template.minPlayers());
        if (sport.getMaxPlayers() == null) sport.setMaxPlayers(template.maxPlayers());
        if (sport.getScoringType() == null) sport.setScoringType(template.scoringType());
        if (sport.getMatchType() == null) sport.setMatchType(template.matchType());
        if (sport.getRankingType() == null) sport.setRankingType(template.rankingType());
        if (sport.getTeamBased() == null) sport.setTeamBased(template.teamBased());
        if (sport.getTournamentEnabled() == null) sport.setTournamentEnabled(true);
        if (sport.getStatisticsEnabled() == null) sport.setStatisticsEnabled(true);
        if (sport.getRules() == null) sport.setRules(template.rules());
        sportRepository.save(sport);
    }

    private Sport fromTemplate(SportTemplate template) {
        return Sport.builder()
                .name(template.name())
                .code(template.code())
                .icon(template.icon())
                .rules(template.rules())
                .isActive(true)
                .displayOrder(template.displayOrder())
                .minPlayers(template.minPlayers())
                .maxPlayers(template.maxPlayers())
                .scoringType(template.scoringType())
                .matchType(template.matchType())
                .rankingType(template.rankingType())
                .teamBased(template.teamBased())
                .tournamentEnabled(true)
                .statisticsEnabled(true)
                .configurationJson(template.configurationJson())
                .metadataJson(template.metadataJson())
                .build();
    }

    private List<SportTemplate> defaultTemplates() {
        return List.of(
                new SportTemplate("Cricket", "CRICKET", "CR", 10, 2, 22, "RUNS_WICKETS_OVERS", "TEAM", "POINTS_TABLE", true, "Runs, wickets, overs, innings, net run rate, and team points.", "{\"innings\":2,\"overs\":20,\"nrr\":true}", "{\"ui\":\"cricket_scorecard\"}"),
                new SportTemplate("Football", "FOOTBALL", "FB", 20, 6, 22, "GOALS", "TEAM", "ELO", true, "Goals, assists, cards, possession, and penalty tie-breaks.", "{\"halves\":2,\"durationMinutes\":90,\"penalties\":true}", "{\"ui\":\"football_match_sheet\"}"),
                new SportTemplate("Badminton", "BADMINTON", "BD", 30, 1, 4, "SETS", "TEAM_OR_INDIVIDUAL", "SET_POINTS", false, "Singles or doubles set scoring with best-of-three support.", "{\"bestOf\":3,\"pointsPerSet\":21}", "{\"ui\":\"racket_scorecard\"}"),
                new SportTemplate("Tennis", "TENNIS", "TN", 40, 1, 4, "SETS", "TEAM_OR_INDIVIDUAL", "SET_POINTS", false, "Games, sets, tie-breaks, singles, and doubles.", "{\"sets\":3,\"tieBreak\":true}", "{\"ui\":\"tennis_scorecard\"}"),
                new SportTemplate("Pickleball", "PICKLEBALL", "PB", 50, 2, 4, "POINTS", "DOUBLES", "SET_POINTS", false, "Rally points, doubles play, and configured set targets.", "{\"pointsToWin\":11,\"winBy\":2}", "{\"ui\":\"points_scorecard\"}"),
                new SportTemplate("Basketball", "BASKETBALL", "BB", 60, 6, 12, "POINTS", "TEAM", "ELO", true, "Points, quarters, fouls, rebounds, and overtime.", "{\"quarters\":4,\"overtime\":true}", "{\"ui\":\"points_scorecard\"}"),
                new SportTemplate("Volleyball", "VOLLEYBALL", "VB", 70, 6, 12, "SETS", "TEAM", "SET_POINTS", true, "Set scoring, rotations, and match points.", "{\"bestOf\":5,\"pointsPerSet\":25}", "{\"ui\":\"set_scorecard\"}")
        );
    }

    private record SportTemplate(String name, String code, String icon, int displayOrder, int minPlayers, int maxPlayers,
                                 String scoringType, String matchType, String rankingType, boolean teamBased,
                                 String rules, String configurationJson, String metadataJson) {
    }
}
