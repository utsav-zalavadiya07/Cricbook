package com.example.Cricbook.service;

import com.example.Cricbook.dto.*;
import com.example.Cricbook.entity.*;
import com.example.Cricbook.enums.*;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TournamentService {

    private final UserRepository userRepository;
    private final TurfRepository turfRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TournamentRepository tournamentRepository;
    private final TournamentRegistrationRepository registrationRepository;
    private final TournamentFixtureRepository fixtureRepository;
    private final TournamentStandingRepository standingRepository;
    private final TournamentRewardRepository rewardRepository;
    private final FeedEventRepository feedEventRepository;
    private final NotificationService notificationService;

    @Transactional
    public TournamentResponse createTournament(String username, CreateTournamentRequest request) {
        if (request == null) {
            throw new ApiException("Tournament details are required");
        }
        User organizer = findUser(username);
        Turf turf = request.getTurfId() == null ? null : turfRepository.findById(request.getTurfId())
                .orElseThrow(() -> new ApiException("Turf not found"));
        String name = required(request.getName(), "Tournament name is required");
        String city = required(request.getCity(), "City is required");
        LocalDate startDate = requiredObject(request.getStartDate(), "Start date is required");
        if (request.getEndDate() != null && request.getEndDate().isBefore(startDate)) {
            throw new ApiException("End date cannot be before start date");
        }
        if (tournamentRepository.existsByNameIgnoreCaseAndCityIgnoreCase(name, city)) {
            throw new ApiException("A tournament with this name already exists in this city");
        }
        TournamentStatus initialStatus = request.getStatus() == null ? TournamentStatus.REGISTRATION_OPEN : request.getStatus();
        if (!List.of(TournamentStatus.DRAFT, TournamentStatus.UPCOMING, TournamentStatus.REGISTRATION_OPEN).contains(initialStatus)) {
            throw new ApiException("New tournaments can only start as draft, upcoming, or registration open");
        }

        Tournament tournament = tournamentRepository.save(Tournament.builder()
                .organizer(organizer)
                .turf(turf)
                .name(name)
                .sport(defaultValue(request.getSport(), "Cricket"))
                .city(city)
                .venue(defaultValue(request.getVenue(), turf == null ? request.getCity() : turf.getLocation()))
                .description(request.getDescription())
                .bannerUrl(request.getBannerUrl())
                .rules(request.getRules())
                .requiresApproval(request.getRequiresApproval() == null ? Boolean.TRUE : request.getRequiresApproval())
                .format(request.getFormat() == null ? TournamentFormat.LEAGUE : request.getFormat())
                .status(initialStatus)
                .maxTeams(resolveMinimum(request.getMaxTeams(), 8, 2, "Max teams must be at least 2"))
                .entryFee(resolveMoney(request.getEntryFee(), "Entry fee cannot be negative"))
                .prizePool(request.getPrizePool() == null ? null : resolveMoney(request.getPrizePool(), "Prize pool cannot be negative"))
                .startDate(startDate)
                .endDate(request.getEndDate())
                .build());
        publishTournamentFeed(organizer, tournament, FeedEventType.TEAM_CREATED, "Tournament created", organizer.getName() + " created " + tournament.getName());
        return mapTournament(tournament);
    }

    @Transactional(readOnly = true)
    public List<TournamentResponse> getMyTournaments(String username) {
        User user = findUser(username);
        return tournamentRepository.findByOrganizerIdOrderByStartDateDesc(user.getId()).stream().map(this::mapTournament).toList();
    }

    @Transactional(readOnly = true)
    public List<TournamentResponse> getOpenTournaments() {
        return tournamentRepository.findByStatusesOrderByStartDateAsc(List.of(TournamentStatus.REGISTRATION_OPEN, TournamentStatus.ONGOING, TournamentStatus.UPCOMING))
                .stream()
                .map(this::mapTournament)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TournamentResponse> searchTournaments(
            String query,
            String city,
            TournamentFormat format,
            TournamentStatus status,
            BigDecimal maxEntryFee,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        if (fromDate != null && toDate != null && toDate.isBefore(fromDate)) {
            throw new ApiException("To date cannot be before from date");
        }
        return tournamentRepository.searchTournaments(
                        cleanSearch(query),
                        cleanSearch(city),
                        format,
                        status,
                        maxEntryFee,
                        fromDate,
                        toDate
                )
                .stream()
                .map(this::mapTournament)
                .toList();
    }

    @Transactional(readOnly = true)
    public TournamentResponse getTournament(Long tournamentId) {
        return mapTournament(findTournament(tournamentId));
    }

    @Transactional
    public TournamentResponse updateTournamentStatus(String username, Long tournamentId, UpdateTournamentStatusRequest request) {
        if (request == null || request.getStatus() == null) {
            throw new ApiException("Tournament status is required");
        }
        User user = findUser(username);
        Tournament tournament = findTournament(tournamentId);
        assertOrganizer(tournament, user);
        TournamentStatus current = tournament.getStatus();
        TournamentStatus next = request.getStatus();
        if (current == next) {
            return mapTournament(tournament);
        }
        if (!canTransition(current, next)) {
            throw new ApiException("Tournament cannot move from " + current + " to " + next);
        }
        tournament.setStatus(next);
        Tournament saved = tournamentRepository.save(tournament);
        publishTournamentFeed(
                user,
                saved,
                FeedEventType.PROFILE_UPDATED,
                "Tournament status updated",
                saved.getName() + " moved to " + next + (request.getNote() == null || request.getNote().isBlank() ? "" : ": " + request.getNote().trim())
        );
        return mapTournament(saved);
    }

    @Transactional
    public TournamentRegistrationResponse registerTeam(String username, Long tournamentId, TournamentRegistrationRequest request) {
        if (request == null || request.getTeamId() == null) {
            throw new ApiException("Team is required");
        }
        User user = findUser(username);
        Tournament tournament = findTournament(tournamentId);
        if (tournament.getStatus() != TournamentStatus.REGISTRATION_OPEN) {
            throw new ApiException("Tournament registration is closed");
        }
        Team team = teamRepository.findById(request.getTeamId())
                .orElseThrow(() -> new ApiException("Team not found"));
        if (!team.getCaptain().getId().equals(user.getId()) && !teamMemberRepository.existsByTeamIdAndUserId(team.getId(), user.getId())) {
            throw new ApiException("You must belong to this team to register it");
        }
        if (registrationRepository.existsByTournamentIdAndTeamId(tournamentId, team.getId())) {
            throw new ApiException("Team already registered");
        }

        long approvedCount = registrationRepository.countByTournamentIdAndStatus(tournamentId, TournamentRegistrationStatus.REGISTERED);
        TournamentRegistrationStatus status = approvedCount >= tournament.getMaxTeams()
                ? TournamentRegistrationStatus.WAITLISTED
                : Boolean.TRUE.equals(tournament.getRequiresApproval()) ? TournamentRegistrationStatus.PENDING : TournamentRegistrationStatus.REGISTERED;

        TournamentRegistration registration = registrationRepository.save(TournamentRegistration.builder()
                .tournament(tournament)
                .team(team)
                .registeredBy(user)
                .status(status)
                .paid(tournament.getEntryFee() == null || tournament.getEntryFee().compareTo(BigDecimal.ZERO) == 0)
                .build());

        if (status == TournamentRegistrationStatus.REGISTERED) {
            createStanding(tournament, team);
        }
        notificationService.pushNotification(
                tournament.getOrganizer().getId(),
                tournament.getOrganizer().getEmail(),
                team.getName() + " requested tournament registration",
                team.getName() + " submitted registration for " + tournament.getName(),
                NotificationChannel.PUSH,
                null,
                "/tournaments"
        );
        return mapRegistration(registration);
    }

    @Transactional(readOnly = true)
    public List<TournamentRegistrationResponse> getRegistrations(Long tournamentId) {
        findTournament(tournamentId);
        return registrationRepository.findByTournamentIdOrderByCreatedAtAsc(tournamentId)
                .stream()
                .map(this::mapRegistration)
                .toList();
    }

    @Transactional
    public TournamentRegistrationResponse approveRegistration(String username, Long tournamentId, Long registrationId, RegistrationDecisionRequest request) {
        User user = findUser(username);
        Tournament tournament = findTournament(tournamentId);
        assertOrganizer(tournament, user);
        TournamentRegistration registration = registrationRepository.findByIdAndTournamentId(registrationId, tournamentId)
                .orElseThrow(() -> new ApiException("Registration not found"));
        if (registration.getStatus() == TournamentRegistrationStatus.REGISTERED) {
            throw new ApiException("Registration already approved");
        }
        if (registrationRepository.countByTournamentIdAndStatus(tournamentId, TournamentRegistrationStatus.REGISTERED) >= tournament.getMaxTeams()) {
            registration.setStatus(TournamentRegistrationStatus.WAITLISTED);
        } else {
            registration.setStatus(TournamentRegistrationStatus.REGISTERED);
            createStanding(tournament, registration.getTeam());
        }
        registration.setNote(request == null ? null : request.getNote());
        TournamentRegistration saved = registrationRepository.save(registration);
        notificationService.pushNotification(
                saved.getRegisteredBy().getId(),
                saved.getRegisteredBy().getEmail(),
                saved.getTeam().getName() + " registration updated",
                "Registration for " + tournament.getName() + " is now " + saved.getStatus(),
                NotificationChannel.PUSH,
                null,
                "/tournaments"
        );
        return mapRegistration(saved);
    }

    @Transactional
    public TournamentRegistrationResponse rejectRegistration(String username, Long tournamentId, Long registrationId, RegistrationDecisionRequest request) {
        User user = findUser(username);
        Tournament tournament = findTournament(tournamentId);
        assertOrganizer(tournament, user);
        TournamentRegistration registration = registrationRepository.findByIdAndTournamentId(registrationId, tournamentId)
                .orElseThrow(() -> new ApiException("Registration not found"));
        registration.setStatus(TournamentRegistrationStatus.REJECTED);
        registration.setNote(request == null ? null : request.getNote());
        TournamentRegistration saved = registrationRepository.save(registration);
        notificationService.pushNotification(
                saved.getRegisteredBy().getId(),
                saved.getRegisteredBy().getEmail(),
                saved.getTeam().getName() + " registration rejected",
                "Registration for " + tournament.getName() + " was rejected",
                NotificationChannel.PUSH,
                null,
                "/tournaments"
        );
        return mapRegistration(saved);
    }

    @Transactional
    public TournamentRegistrationResponse cancelRegistration(String username, Long tournamentId, Long registrationId) {
        User user = findUser(username);
        TournamentRegistration registration = registrationRepository.findByIdAndTournamentId(registrationId, tournamentId)
                .orElseThrow(() -> new ApiException("Registration not found"));
        if (!registration.getRegisteredBy().getId().equals(user.getId())
                && !registration.getTeam().getCaptain().getId().equals(user.getId())
                && !registration.getTournament().getOrganizer().getId().equals(user.getId())) {
            throw new ApiException("You cannot cancel this registration");
        }
        registration.setStatus(TournamentRegistrationStatus.CANCELLED);
        TournamentRegistration saved = registrationRepository.save(registration);
        promoteWaitlist(saved.getTournament());
        return mapRegistration(saved);
    }

    @Transactional
    public List<TournamentFixtureResponse> generateFixtures(String username, Long tournamentId) {
        User user = findUser(username);
        Tournament tournament = findTournament(tournamentId);
        assertOrganizer(tournament, user);
        if (fixtureRepository.existsByTournamentId(tournamentId)) {
            throw new ApiException("Fixtures already generated");
        }

        List<Team> teams = registrationRepository.findByTournamentIdOrderByCreatedAtAsc(tournamentId)
                .stream()
                .filter(registration -> registration.getStatus() == TournamentRegistrationStatus.REGISTERED)
                .map(TournamentRegistration::getTeam)
                .toList();
        if (teams.size() < 2) {
            throw new ApiException("At least two teams are required");
        }
        if (isKnockoutFormat(tournament) && !isPowerOfTwo(teams.size())) {
            throw new ApiException("Knockout fixtures require a power-of-two team count");
        }

        List<TournamentFixture> fixtures = isKnockoutFormat(tournament)
                ? knockoutFixtures(tournament, teams)
                : leagueFixtures(tournament, teams);

        tournament.setStatus(TournamentStatus.ONGOING);
        tournamentRepository.save(tournament);
        publishTournamentFeed(user, tournament, FeedEventType.MATCH_CREATED, "Fixtures generated", "Fixtures generated for " + tournament.getName());
        return fixtureRepository.saveAll(fixtures).stream().map(this::mapFixture).toList();
    }

    @Transactional
    public List<TournamentFixtureResponse> regenerateFixtures(String username, Long tournamentId) {
        User user = findUser(username);
        Tournament tournament = findTournament(tournamentId);
        assertOrganizer(tournament, user);
        fixtureRepository.deleteByTournamentId(tournamentId);
        standingRepository.deleteByTournamentId(tournamentId);
        registrationRepository.findByTournamentIdAndStatusOrderByCreatedAtAsc(tournamentId, TournamentRegistrationStatus.REGISTERED)
                .forEach(registration -> createStanding(tournament, registration.getTeam()));
        return generateFixtures(username, tournamentId);
    }

    @Transactional(readOnly = true)
    public List<TournamentFixtureResponse> getFixtures(Long tournamentId) {
        findTournament(tournamentId);
        return fixtureRepository.findByTournamentIdOrderByRoundAscMatchDateAscStartTimeAsc(tournamentId)
                .stream()
                .map(this::mapFixture)
                .toList();
    }

    @Transactional
    public TournamentFixtureResponse recordFixtureResult(String username, Long fixtureId, FixtureResultRequest request) {
        if (request == null || request.getTeamAScore() == null || request.getTeamBScore() == null) {
            throw new ApiException("Both scores are required");
        }
        User user = findUser(username);
        TournamentFixture fixture = fixtureRepository.findById(fixtureId)
                .orElseThrow(() -> new ApiException("Fixture not found"));
        assertOrganizer(fixture.getTournament(), user);
        if (fixture.getStatus() == FixtureStatus.COMPLETED) {
            throw new ApiException("Fixture result is already recorded");
        }
        if (request.getTeamAScore() < 0 || request.getTeamBScore() < 0) {
            throw new ApiException("Scores cannot be negative");
        }
        boolean draw = request.getTeamAScore().equals(request.getTeamBScore());
        if (draw && isKnockoutFormat(fixture.getTournament())) {
            throw new ApiException("Knockout fixtures require a winner");
        }

        fixture.setTeamAScore(request.getTeamAScore());
        fixture.setTeamBScore(request.getTeamBScore());
        fixture.setWinner(draw ? null : request.getTeamAScore() > request.getTeamBScore() ? fixture.getTeamA() : fixture.getTeamB());
        fixture.setStatus(FixtureStatus.COMPLETED);
        updateStandings(fixture, draw);
        TournamentFixture saved = fixtureRepository.save(fixture);
        if (!draw) {
            advanceKnockoutBracket(saved);
        }
        publishTournamentFeed(user, fixture.getTournament(), FeedEventType.MATCH_JOINED, "Fixture completed", fixture.getTeamA().getName() + " vs " + fixture.getTeamB().getName() + " completed");
        return mapFixture(saved);
    }

    @Transactional(readOnly = true)
    public List<TournamentStandingResponse> getStandings(Long tournamentId) {
        findTournament(tournamentId);
        return standingRepository.findByTournamentIdOrderByPointsDescWinsDescRunsForDesc(tournamentId)
                .stream()
                .sorted(Comparator
                        .comparing(TournamentStanding::getPoints, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(TournamentStanding::getWins, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(this::netRuns, Comparator.reverseOrder())
                        .thenComparing(standing -> standing.getTeam().getName()))
                .map(this::mapStanding)
                .toList();
    }

    @Transactional
    public TournamentFixtureResponse scheduleFixture(String username, Long fixtureId, TournamentFixtureScheduleRequest request) {
        User user = findUser(username);
        TournamentFixture fixture = fixtureRepository.findById(fixtureId)
                .orElseThrow(() -> new ApiException("Fixture not found"));
        assertOrganizer(fixture.getTournament(), user);
        Turf turf = request.getTurfId() == null ? null : turfRepository.findById(request.getTurfId())
                .orElseThrow(() -> new ApiException("Turf not found"));
        if (request.getMatchDate() != null
                && request.getStartTime() != null
                && turf != null
                && fixtureRepository.existsByTurfIdAndMatchDateAndStartTimeAndStatusInAndIdNot(
                turf.getId(),
                request.getMatchDate(),
                request.getStartTime(),
                List.of(FixtureStatus.SCHEDULED, FixtureStatus.LIVE),
                fixture.getId()
        )) {
            throw new ApiException("Selected turf and time already has another tournament fixture");
        }
        fixture.setTurf(turf);
        fixture.setSlotId(request.getSlotId());
        fixture.setMatchDate(request.getMatchDate());
        fixture.setStartTime(request.getStartTime());
        fixture.setScheduleNote(request.getScheduleNote());
        return mapFixture(fixtureRepository.save(fixture));
    }

    @Transactional
    public TournamentRewardResponse addReward(String username, Long tournamentId, CreateTournamentRewardRequest request) {
        User user = findUser(username);
        Tournament tournament = findTournament(tournamentId);
        assertOrganizer(tournament, user);
        if (request == null || request.getRewardType() == null || request.getTitle() == null || request.getTitle().isBlank() || request.getPosition() == null) {
            throw new ApiException("Reward details are required");
        }
        Team winner = request.getWinnerTeamId() == null ? null : teamRepository.findById(request.getWinnerTeamId())
                .orElseThrow(() -> new ApiException("Winner team not found"));
        TournamentReward reward = rewardRepository.save(TournamentReward.builder()
                .tournament(tournament)
                .winnerTeam(winner)
                .rewardType(request.getRewardType())
                .title(required(request.getTitle(), "Reward title is required"))
                .description(request.getDescription())
                .position(request.getPosition())
                .cashAmount(request.getCashAmount())
                .build());
        publishTournamentFeed(user, tournament, FeedEventType.PROFILE_UPDATED, "Reward assigned", reward.getTitle() + " added to " + tournament.getName());
        return mapReward(reward);
    }

    @Transactional(readOnly = true)
    public List<TournamentRewardResponse> getRewards(Long tournamentId) {
        findTournament(tournamentId);
        return rewardRepository.findByTournamentIdOrderByPositionAscCreatedAtAsc(tournamentId).stream().map(this::mapReward).toList();
    }

    @Transactional(readOnly = true)
    public TournamentDashboardResponse getDashboard(Long tournamentId) {
        Tournament tournament = findTournament(tournamentId);
        List<TournamentRegistrationResponse> registrations = getRegistrations(tournamentId);
        List<TournamentFixtureResponse> fixtures = getFixtures(tournamentId);
        List<TournamentStandingResponse> standings = getStandings(tournamentId);
        List<TournamentRewardResponse> rewards = getRewards(tournamentId);
        List<FeedEventResponse> feed = feedEventRepository.findTop25ByEntityTypeAndEntityIdOrderByCreatedAtDesc("TOURNAMENT", tournamentId)
                .stream().map(this::mapFeedEvent).toList();

        int approvedTeams = (int) registrations.stream().filter(item -> item.getStatus() == TournamentRegistrationStatus.REGISTERED).count();
        int pendingTeams = (int) registrations.stream().filter(item -> item.getStatus() == TournamentRegistrationStatus.PENDING).count();
        int waitlistTeams = (int) registrations.stream().filter(item -> item.getStatus() == TournamentRegistrationStatus.WAITLISTED).count();
        int completedFixtures = (int) fixtures.stream().filter(item -> item.getStatus() == FixtureStatus.COMPLETED).count();
        int upcomingFixtures = (int) fixtures.stream().filter(item -> item.getStatus() == FixtureStatus.SCHEDULED).count();
        BigDecimal revenue = tournament.getEntryFee().multiply(BigDecimal.valueOf(approvedTeams));

        return TournamentDashboardResponse.builder()
                .tournament(mapTournament(tournament))
                .registrations(registrations)
                .fixtures(fixtures)
                .standings(standings)
                .rewards(rewards)
                .feed(feed)
                .approvedTeams(approvedTeams)
                .pendingTeams(pendingTeams)
                .waitlistTeams(waitlistTeams)
                .completedFixtures(completedFixtures)
                .upcomingFixtures(upcomingFixtures)
                .registrationRevenue(revenue)
                .build();
    }

    private List<TournamentFixture> leagueFixtures(Tournament tournament, List<Team> teams) {
        List<TournamentFixture> fixtures = new ArrayList<>();
        int round = 1;
        LocalDate date = tournament.getStartDate();
        for (int i = 0; i < teams.size(); i++) {
            for (int j = i + 1; j < teams.size(); j++) {
                fixtures.add(newFixture(tournament, teams.get(i), teams.get(j), round++, date));
                date = date.plusDays(1);
            }
        }
        return fixtures;
    }

    private List<TournamentFixture> knockoutFixtures(Tournament tournament, List<Team> teams) {
        List<TournamentFixture> fixtures = new ArrayList<>();
        LocalDate date = tournament.getStartDate();
        for (int i = 0; i + 1 < teams.size(); i += 2) {
            fixtures.add(newFixture(tournament, teams.get(i), teams.get(i + 1), 1, date));
            date = date.plusDays(1);
        }
        return fixtures;
    }

    private TournamentFixture newFixture(Tournament tournament, Team teamA, Team teamB, Integer round, LocalDate date) {
        return TournamentFixture.builder()
                .tournament(tournament)
                .teamA(teamA)
                .teamB(teamB)
                .round(round)
                .matchDate(date)
                .status(FixtureStatus.SCHEDULED)
                .build();
    }

    private void createStanding(Tournament tournament, Team team) {
        standingRepository.findByTournamentIdAndTeamId(tournament.getId(), team.getId())
                .orElseGet(() -> standingRepository.save(TournamentStanding.builder()
                        .tournament(tournament)
                        .team(team)
                        .played(0)
                        .wins(0)
                        .losses(0)
                        .draws(0)
                        .points(0)
                        .runsFor(0)
                        .runsAgainst(0)
                        .build()));
    }

    private void updateStandings(TournamentFixture fixture, boolean draw) {
        updateStanding(fixture.getTournament(), fixture.getTeamA(), fixture.getTeamAScore(), fixture.getTeamBScore(), !draw && fixture.getWinner().getId().equals(fixture.getTeamA().getId()), draw);
        updateStanding(fixture.getTournament(), fixture.getTeamB(), fixture.getTeamBScore(), fixture.getTeamAScore(), !draw && fixture.getWinner().getId().equals(fixture.getTeamB().getId()), draw);
    }

    private void updateStanding(Tournament tournament, Team team, Integer runsFor, Integer runsAgainst, boolean won, boolean draw) {
        TournamentStanding standing = standingRepository.findByTournamentIdAndTeamId(tournament.getId(), team.getId())
                .orElseGet(() -> TournamentStanding.builder()
                        .tournament(tournament)
                        .team(team)
                        .played(0)
                        .wins(0)
                        .losses(0)
                        .draws(0)
                        .points(0)
                        .runsFor(0)
                        .runsAgainst(0)
                        .build());
        standing.setPlayed(safeInt(standing.getPlayed()) + 1);
        standing.setWins(safeInt(standing.getWins()) + (won ? 1 : 0));
        standing.setLosses(safeInt(standing.getLosses()) + (!won && !draw ? 1 : 0));
        standing.setDraws(safeInt(standing.getDraws()) + (draw ? 1 : 0));
        standing.setPoints(safeInt(standing.getPoints()) + (won ? 2 : draw ? 1 : 0));
        standing.setRunsFor(safeInt(standing.getRunsFor()) + runsFor);
        standing.setRunsAgainst(safeInt(standing.getRunsAgainst()) + runsAgainst);
        standingRepository.save(standing);
    }

    private void advanceKnockoutBracket(TournamentFixture completedFixture) {
        Tournament tournament = completedFixture.getTournament();
        if (!isKnockoutFormat(tournament)) {
            return;
        }

        List<TournamentFixture> currentRoundFixtures = fixtureRepository.findByTournamentIdAndRoundOrderByMatchDateAscStartTimeAscIdAsc(
                tournament.getId(),
                completedFixture.getRound()
        );
        boolean roundFinished = currentRoundFixtures.stream().allMatch(item -> item.getStatus() == FixtureStatus.COMPLETED && item.getWinner() != null);
        if (!roundFinished) {
            return;
        }

        List<Team> winners = currentRoundFixtures.stream().map(TournamentFixture::getWinner).toList();
        if (winners.size() == 1) {
            tournament.setStatus(TournamentStatus.COMPLETED);
            tournamentRepository.save(tournament);
            publishTournamentFeed(
                    tournament.getOrganizer(),
                    tournament,
                    FeedEventType.PROFILE_UPDATED,
                    "Tournament winner declared",
                    winners.get(0).getName() + " won " + tournament.getName()
            );
            return;
        }

        if (fixtureRepository.countByTournamentIdAndRound(tournament.getId(), completedFixture.getRound() + 1) > 0) {
            return;
        }

        LocalDate nextDate = currentRoundFixtures.stream()
                .map(TournamentFixture::getMatchDate)
                .filter(date -> date != null)
                .max(LocalDate::compareTo)
                .orElse(tournament.getStartDate())
                .plusDays(1);

        List<TournamentFixture> nextRoundFixtures = new ArrayList<>();
        for (int i = 0; i + 1 < winners.size(); i += 2) {
            nextRoundFixtures.add(newFixture(tournament, winners.get(i), winners.get(i + 1), completedFixture.getRound() + 1, nextDate));
            nextDate = nextDate.plusDays(1);
        }
        List<TournamentFixture> savedNextRoundFixtures = fixtureRepository.saveAll(nextRoundFixtures);
        publishTournamentFeed(
                tournament.getOrganizer(),
                tournament,
                FeedEventType.MATCH_CREATED,
                stageLabelForFixture(savedNextRoundFixtures.get(0)),
                "Next round fixtures are ready for " + tournament.getName()
        );
    }

    private TournamentResponse mapTournament(Tournament tournament) {
        return TournamentResponse.builder()
                .id(tournament.getId())
                .organizerId(tournament.getOrganizer().getId())
                .organizerName(tournament.getOrganizer().getName())
                .turfId(tournament.getTurf() == null ? null : tournament.getTurf().getId())
                .turfName(tournament.getTurf() == null ? null : tournament.getTurf().getName())
                .name(tournament.getName())
                .sport(tournament.getSport())
                .city(tournament.getCity())
                .venue(tournament.getVenue())
                .description(tournament.getDescription())
                .bannerUrl(tournament.getBannerUrl())
                .rules(tournament.getRules())
                .requiresApproval(tournament.getRequiresApproval())
                .format(tournament.getFormat())
                .status(tournament.getStatus())
                .maxTeams(tournament.getMaxTeams())
                .registeredTeams((int) registrationRepository.countByTournamentIdAndStatus(tournament.getId(), TournamentRegistrationStatus.REGISTERED))
                .entryFee(tournament.getEntryFee())
                .prizePool(tournament.getPrizePool())
                .startDate(tournament.getStartDate())
                .endDate(tournament.getEndDate())
                .createdAt(tournament.getCreatedAt())
                .build();
    }

    private TournamentRegistrationResponse mapRegistration(TournamentRegistration registration) {
        return TournamentRegistrationResponse.builder()
                .id(registration.getId())
                .tournamentId(registration.getTournament().getId())
                .teamId(registration.getTeam().getId())
                .teamName(registration.getTeam().getName())
                .registeredById(registration.getRegisteredBy().getId())
                .registeredByName(registration.getRegisteredBy().getName())
                .status(registration.getStatus())
                .paid(registration.getPaid())
                .note(registration.getNote())
                .createdAt(registration.getCreatedAt())
                .build();
    }

    private TournamentFixtureResponse mapFixture(TournamentFixture fixture) {
        boolean knockoutStage = isKnockoutFormat(fixture.getTournament());
        return TournamentFixtureResponse.builder()
                .id(fixture.getId())
                .tournamentId(fixture.getTournament().getId())
                .teamAId(fixture.getTeamA().getId())
                .teamAName(fixture.getTeamA().getName())
                .teamBId(fixture.getTeamB().getId())
                .teamBName(fixture.getTeamB().getName())
                .winnerTeamId(fixture.getWinner() == null ? null : fixture.getWinner().getId())
                .winnerTeamName(fixture.getWinner() == null ? null : fixture.getWinner().getName())
                .round(fixture.getRound())
                .matchDate(fixture.getMatchDate())
                .turfId(fixture.getTurf() == null ? null : fixture.getTurf().getId())
                .turfName(fixture.getTurf() == null ? null : fixture.getTurf().getName())
                .slotId(fixture.getSlotId())
                .startTime(fixture.getStartTime())
                .scheduleNote(fixture.getScheduleNote())
                .teamAScore(fixture.getTeamAScore())
                .teamBScore(fixture.getTeamBScore())
                .stageLabel(stageLabelForFixture(fixture))
                .knockoutStage(knockoutStage)
                .status(fixture.getStatus())
                .build();
    }

    private TournamentStandingResponse mapStanding(TournamentStanding standing) {
        return TournamentStandingResponse.builder()
                .teamId(standing.getTeam().getId())
                .teamName(standing.getTeam().getName())
                .played(standing.getPlayed())
                .wins(standing.getWins())
                .losses(standing.getLosses())
                .draws(safeInt(standing.getDraws()))
                .points(standing.getPoints())
                .runsFor(standing.getRunsFor())
                .runsAgainst(standing.getRunsAgainst())
                .netRuns(netRuns(standing))
                .netRunRate(netRunRate(standing))
                .build();
    }

    private TournamentRewardResponse mapReward(TournamentReward reward) {
        return TournamentRewardResponse.builder()
                .id(reward.getId())
                .tournamentId(reward.getTournament().getId())
                .rewardType(reward.getRewardType())
                .title(reward.getTitle())
                .description(reward.getDescription())
                .position(reward.getPosition())
                .cashAmount(reward.getCashAmount())
                .winnerTeamId(reward.getWinnerTeam() == null ? null : reward.getWinnerTeam().getId())
                .winnerTeamName(reward.getWinnerTeam() == null ? null : reward.getWinnerTeam().getName())
                .createdAt(reward.getCreatedAt())
                .build();
    }

    private FeedEventResponse mapFeedEvent(FeedEvent event) {
        return FeedEventResponse.builder()
                .id(event.getId())
                .actorId(event.getActor().getId())
                .actorName(event.getActor().getName())
                .eventType(event.getEventType())
                .title(event.getTitle())
                .summary(event.getSummary())
                .entityId(event.getEntityId())
                .entityType(event.getEntityType())
                .deepLink(event.getDeepLink())
                .createdAt(event.getCreatedAt())
                .build();
    }

    private Tournament findTournament(Long tournamentId) {
        return tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new ApiException("Tournament not found"));
    }

    private User findUser(String loginId) {
        return userRepository.findByEmailOrPhone(loginId)
                .orElseThrow(() -> new ApiException("User not found"));
    }

    private void assertOrganizer(Tournament tournament, User user) {
        if (!tournament.getOrganizer().getId().equals(user.getId())) {
            throw new ApiException("Only organizer can perform this action");
        }
    }

    private boolean isKnockoutFormat(Tournament tournament) {
        return tournament.getFormat() == TournamentFormat.KNOCKOUT || tournament.getFormat() == TournamentFormat.HYBRID;
    }

    private boolean canTransition(TournamentStatus current, TournamentStatus next) {
        return switch (current) {
            case DRAFT -> List.of(TournamentStatus.UPCOMING, TournamentStatus.REGISTRATION_OPEN, TournamentStatus.CANCELLED).contains(next);
            case UPCOMING -> List.of(TournamentStatus.REGISTRATION_OPEN, TournamentStatus.ONGOING, TournamentStatus.CANCELLED).contains(next);
            case REGISTRATION_OPEN -> List.of(TournamentStatus.REGISTRATION_CLOSED, TournamentStatus.ONGOING, TournamentStatus.CANCELLED).contains(next);
            case REGISTRATION_CLOSED -> List.of(TournamentStatus.REGISTRATION_OPEN, TournamentStatus.ONGOING, TournamentStatus.CANCELLED).contains(next);
            case ONGOING -> List.of(TournamentStatus.COMPLETED, TournamentStatus.CANCELLED).contains(next);
            case COMPLETED, CANCELLED -> false;
        };
    }

    private boolean isPowerOfTwo(int count) {
        return count > 1 && (count & (count - 1)) == 0;
    }

    private String stageLabel(Long tournamentId, Integer round) {
        long fixtureCount = fixtureRepository.countByTournamentIdAndRound(tournamentId, round);
        if (fixtureCount == 1) {
            return "Final";
        }
        if (fixtureCount == 2) {
            return "Semifinal";
        }
        if (fixtureCount == 4) {
            return "Quarterfinal";
        }
        if (fixtureCount == 8) {
            return "Round of 16";
        }
        return "Round " + round;
    }

    private String stageLabelForFixture(TournamentFixture fixture) {
        if (!isKnockoutFormat(fixture.getTournament())) {
            return "Match " + fixture.getRound();
        }
        return stageLabel(fixture.getTournament().getId(), fixture.getRound());
    }

    private void promoteWaitlist(Tournament tournament) {
        if (registrationRepository.countByTournamentIdAndStatus(tournament.getId(), TournamentRegistrationStatus.REGISTERED) >= tournament.getMaxTeams()) {
            return;
        }
        registrationRepository.findByTournamentIdAndStatusOrderByCreatedAtAsc(tournament.getId(), TournamentRegistrationStatus.WAITLISTED)
                .stream()
                .findFirst()
                .ifPresent(waitlist -> {
                    waitlist.setStatus(TournamentRegistrationStatus.REGISTERED);
                    registrationRepository.save(waitlist);
                    createStanding(tournament, waitlist.getTeam());
                });
    }

    private void publishTournamentFeed(User actor, Tournament tournament, FeedEventType type, String title, String summary) {
        feedEventRepository.save(FeedEvent.builder()
                .actor(actor)
                .eventType(type)
                .title(title)
                .summary(summary)
                .entityId(tournament.getId())
                .entityType("TOURNAMENT")
                .deepLink("/tournaments/" + tournament.getId())
                .build());
    }

    private String required(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ApiException(message);
        }
        return value.trim();
    }

    private String defaultValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private <T> T requiredObject(T value, String message) {
        if (value == null) {
            throw new ApiException(message);
        }
        return value;
    }

    private Integer resolveMinimum(Integer value, Integer fallback, Integer minimum, String message) {
        Integer resolved = value == null ? fallback : value;
        if (resolved < minimum) {
            throw new ApiException(message);
        }
        return resolved;
    }

    private BigDecimal resolveMoney(BigDecimal value, String message) {
        BigDecimal resolved = value == null ? BigDecimal.ZERO : value;
        if (resolved.compareTo(BigDecimal.ZERO) < 0) {
            throw new ApiException(message);
        }
        return resolved;
    }

    private String cleanSearch(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private int netRuns(TournamentStanding standing) {
        return safeInt(standing.getRunsFor()) - safeInt(standing.getRunsAgainst());
    }

    private double netRunRate(TournamentStanding standing) {
        int played = safeInt(standing.getPlayed());
        if (played == 0) {
            return 0D;
        }
        return Math.round((netRuns(standing) / (double) played) * 100.0D) / 100.0D;
    }
}
