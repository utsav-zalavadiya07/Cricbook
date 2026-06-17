package com.example.Cricbook.service;

import com.example.Cricbook.dto.FanEconomyRequests;
import com.example.Cricbook.entity.*;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FanEconomyService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Set<String> PROFILE_STATUSES = Set.of("ACTIVE", "SUSPENDED");
    private static final Set<String> CLUB_TYPES = Set.of("TEAM", "PLAYER", "TOURNAMENT", "CITY", "SPORT");
    private static final Set<String> VISIBILITY = Set.of("PUBLIC", "PRIVATE", "MEMBERS");
    private static final Set<String> COLLECTIBLE_TYPES = Set.of("CARD", "MOMENT", "BADGE", "DIGITAL_MERCH", "TROPHY");
    private static final Set<String> COLLECTIBLE_RARITIES = Set.of("COMMON", "RARE", "EPIC", "LEGENDARY");
    private static final Set<String> PREDICTION_TYPES = Set.of("MATCH_WINNER", "TOP_SCORER", "SCORE_RANGE", "PLAYER_OF_MATCH", "CUSTOM");
    private static final Set<String> RESULT_STATUSES = Set.of("CORRECT", "PARTIAL", "WRONG", "VOID");
    private static final Set<String> CONTENT_TYPES = Set.of("POST", "VIDEO", "PODCAST", "ANALYSIS", "SHORT");
    private static final Set<String> ACCESS_MODES = Set.of("PUBLIC", "INVITE_ONLY", "PAID");

    private final FanProfileRepository fanProfileRepository;
    private final FanClubRepository fanClubRepository;
    private final FanClubMembershipRepository fanClubMembershipRepository;
    private final DigitalCollectibleRepository digitalCollectibleRepository;
    private final FanCollectibleOwnershipRepository collectibleOwnershipRepository;
    private final PredictionGameRepository predictionGameRepository;
    private final FanPredictionEntryRepository predictionEntryRepository;
    private final CreatorProfileRepository creatorProfileRepository;
    private final CreatorContentRepository creatorContentRepository;
    private final FanMarketplaceListingRepository marketplaceListingRepository;
    private final WatchPartyRepository watchPartyRepository;
    private final WatchPartyParticipantRepository watchPartyParticipantRepository;
    private final FanEconomyAuditRepository auditRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> dashboard() {
        List<FanProfile> fans = fanProfileRepository.findAllByOrderByCreatedAtDesc();
        List<FanClub> clubs = fanClubRepository.findByStatusOrderByCreatedAtDesc("ACTIVE");
        List<DigitalCollectible> collectibles = digitalCollectibleRepository.findByStatusOrderByCreatedAtDesc("ACTIVE");
        List<PredictionGame> predictionGames = predictionGameRepository.findByStatusOrderByCreatedAtDesc("ACTIVE");
        List<CreatorProfile> creators = creatorProfileRepository.findAllByOrderByCreatedAtDesc();
        List<FanMarketplaceListing> listings = marketplaceListingRepository.findByStatusOrderByCreatedAtDesc("ACTIVE");
        List<WatchParty> watchParties = watchPartyRepository.findByStatusOrderByScheduledAtAsc("SCHEDULED");

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("kpis", kpis(fans, clubs, collectibles, predictionGames, creators, listings, watchParties));
        response.put("fanRanking", fanProfileRepository.findTop10ByStatusOrderByRankPointsDescFanScoreDesc("ACTIVE").stream().map(this::mapFanProfile).toList());
        response.put("clubs", clubs.stream().limit(12).map(this::mapFanClub).toList());
        response.put("collectibles", collectibles.stream().limit(12).map(this::mapCollectible).toList());
        response.put("predictionGames", predictionGames.stream().limit(12).map(this::mapPredictionGame).toList());
        response.put("creators", creators.stream().limit(12).map(this::mapCreatorProfile).toList());
        response.put("marketplaceListings", listings.stream().limit(12).map(this::mapMarketplaceListing).toList());
        response.put("watchParties", watchParties.stream().limit(12).map(this::mapWatchParty).toList());
        response.put("economyControls", economyControls());
        response.put("generatedAt", LocalDateTime.now());
        return response;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> adminDashboard() {
        Map<String, Object> response = dashboard();
        response.put("allFans", fans());
        response.put("memberships", fanClubMembershipRepository.findAllByOrderByJoinedAtDesc().stream().limit(25).map(this::mapFanClubMembership).toList());
        response.put("ownerships", collectibleOwnershipRepository.findAllByOrderByAcquiredAtDesc().stream().limit(25).map(this::mapOwnership).toList());
        response.put("predictionEntries", predictionEntryRepository.findAllByOrderByCreatedAtDesc().stream().limit(25).map(this::mapPredictionEntry).toList());
        response.put("creatorContents", creatorContentRepository.findAllByOrderByCreatedAtDesc().stream().limit(25).map(this::mapCreatorContent).toList());
        response.put("watchPartyParticipants", watchPartyParticipantRepository.findAllByOrderByJoinedAtDesc().stream().limit(25).map(this::mapWatchPartyParticipant).toList());
        response.put("audits", audits());
        return response;
    }

    @Transactional
    public Map<String, Object> createFanProfile(FanEconomyRequests.FanProfileRequest request, String actor) {
        if (request == null) {
            throw ApiException.badRequest("Fan profile request is required");
        }
        String email = required(request.getEmail(), "Fan email is required").toLowerCase(Locale.ROOT);
        if (fanProfileRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("Fan profile already exists for this email");
        }
        FanProfile profile = FanProfile.builder()
                .fanCode(generateFanCode())
                .displayName(required(request.getDisplayName(), "Display name is required"))
                .email(email)
                .city(blankToNull(request.getCity()))
                .favoriteSport(normalize(request.getFavoriteSport(), "CRICKET"))
                .favoriteTeam(blankToNull(request.getFavoriteTeam()))
                .favoritePlayer(blankToNull(request.getFavoritePlayer()))
                .avatarUrl(blankToNull(request.getAvatarUrl()))
                .build();
        FanProfile saved = fanProfileRepository.save(profile);
        audit("FAN_PROFILE", saved.getId(), "FAN_PROFILE_CREATED", actor, saved.getEmail());
        return mapFanProfile(saved);
    }

    @Transactional
    public Map<String, Object> updateFanStatus(Long fanProfileId, String status, String actor) {
        FanProfile profile = fanProfile(fanProfileId);
        String normalizedStatus = normalize(status, "ACTIVE");
        if (!PROFILE_STATUSES.contains(normalizedStatus)) {
            throw ApiException.badRequest("Unsupported fan profile status");
        }
        profile.setStatus(normalizedStatus);
        FanProfile saved = fanProfileRepository.save(profile);
        audit("FAN_PROFILE", saved.getId(), "FAN_PROFILE_STATUS_UPDATED", actor, normalizedStatus);
        return mapFanProfile(saved);
    }

    @Transactional
    public Map<String, Object> createFanClub(FanEconomyRequests.FanClubRequest request, String actor) {
        if (request == null) {
            throw ApiException.badRequest("Fan club request is required");
        }
        String name = required(request.getName(), "Fan club name is required");
        String sport = normalize(request.getSport(), "CRICKET");
        if (fanClubRepository.existsByNameIgnoreCaseAndSportIgnoreCase(name, sport)) {
            throw ApiException.conflict("Fan club already exists for this sport");
        }
        String clubType = normalize(request.getClubType(), "TEAM");
        if (!CLUB_TYPES.contains(clubType)) {
            throw ApiException.badRequest("Unsupported fan club type");
        }
        String visibility = normalize(request.getVisibility(), "PUBLIC");
        if (!VISIBILITY.contains(visibility)) {
            throw ApiException.badRequest("Unsupported fan club visibility");
        }
        FanClub club = FanClub.builder()
                .clubCode(generateClubCode())
                .name(name)
                .sport(sport)
                .clubType(clubType)
                .linkedEntityType(blankToNull(request.getLinkedEntityType()))
                .linkedEntityId(request.getLinkedEntityId())
                .city(blankToNull(request.getCity()))
                .visibility(visibility)
                .description(blankToNull(request.getDescription()))
                .build();
        FanClub saved = fanClubRepository.save(club);
        audit("FAN_CLUB", saved.getId(), "FAN_CLUB_CREATED", actor, saved.getClubCode());
        return mapFanClub(saved);
    }

    @Transactional
    public Map<String, Object> joinFanClub(Long fanClubId, FanEconomyRequests.JoinFanClubRequest request, String actor) {
        FanClub club = fanClub(fanClubId);
        Long fanProfileId = request == null ? null : request.getFanProfileId();
        FanProfile profile = activeFanProfile(fanProfileId);
        if (!"ACTIVE".equalsIgnoreCase(club.getStatus())) {
            throw ApiException.badRequest("Only active fan clubs can accept members");
        }
        fanClubMembershipRepository.findByFanProfileIdAndFanClubIdAndStatus(profile.getId(), club.getId(), "ACTIVE")
                .ifPresent(existing -> {
                    throw ApiException.conflict("Fan already joined this club");
                });
        FanClubMembership membership = FanClubMembership.builder()
                .fanProfileId(profile.getId())
                .fanClubId(club.getId())
                .build();
        FanClubMembership saved = fanClubMembershipRepository.save(membership);
        long activeMembers = fanClubMembershipRepository.countByFanClubIdAndStatus(club.getId(), "ACTIVE");
        club.setMemberCount((int) (activeMembers > 0 ? activeMembers : safeInt(club.getMemberCount()) + 1));
        fanClubRepository.save(club);
        audit("FAN_CLUB", club.getId(), "FAN_CLUB_JOINED", actor, profile.getFanCode() + " joined");
        Map<String, Object> response = mapFanClubMembership(saved);
        response.put("club", mapFanClub(club));
        return response;
    }

    @Transactional
    public Map<String, Object> createCollectible(FanEconomyRequests.CollectibleRequest request, String actor) {
        if (request == null) {
            throw ApiException.badRequest("Collectible request is required");
        }
        String collectibleType = normalize(request.getCollectibleType(), "CARD");
        if (!COLLECTIBLE_TYPES.contains(collectibleType)) {
            throw ApiException.badRequest("Unsupported collectible type");
        }
        String rarity = normalize(request.getRarity(), "COMMON");
        if (!COLLECTIBLE_RARITIES.contains(rarity)) {
            throw ApiException.badRequest("Unsupported collectible rarity");
        }
        DigitalCollectible collectible = DigitalCollectible.builder()
                .collectibleCode(generateCollectibleCode())
                .title(required(request.getTitle(), "Collectible title is required"))
                .collectibleType(collectibleType)
                .rarity(rarity)
                .sourceModule(defaultString(request.getSourceModule(), "FAN_ECONOMY"))
                .sourceRefId(request.getSourceRefId())
                .supplyLimit(positiveInt(request.getSupplyLimit(), "Collectible supply limit"))
                .mediaUrl(blankToNull(request.getMediaUrl()))
                .credentialReference(blankToNull(request.getCredentialReference()))
                .build();
        DigitalCollectible saved = digitalCollectibleRepository.save(collectible);
        audit("DIGITAL_COLLECTIBLE", saved.getId(), "COLLECTIBLE_CREATED", actor, saved.getCollectibleCode());
        return mapCollectible(saved);
    }

    @Transactional
    public Map<String, Object> claimCollectible(Long collectibleId, FanEconomyRequests.ClaimCollectibleRequest request, String actor) {
        DigitalCollectible collectible = collectible(collectibleId);
        FanProfile profile = activeFanProfile(request == null ? null : request.getFanProfileId());
        if (!"ACTIVE".equalsIgnoreCase(collectible.getStatus())) {
            throw ApiException.badRequest("Collectible is not active");
        }
        if (collectibleOwnershipRepository.existsByFanProfileIdAndCollectibleIdAndOwnershipStatus(profile.getId(), collectible.getId(), "ACTIVE")) {
            throw ApiException.conflict("Fan already owns this collectible");
        }
        int nextSerial = safeInt(collectible.getMintedCount()) + 1;
        if (nextSerial > safeInt(collectible.getSupplyLimit())) {
            throw ApiException.conflict("Collectible supply is exhausted");
        }
        collectible.setMintedCount(nextSerial);
        digitalCollectibleRepository.save(collectible);
        FanCollectibleOwnership ownership = FanCollectibleOwnership.builder()
                .collectibleId(collectible.getId())
                .fanProfileId(profile.getId())
                .serialNumber(nextSerial)
                .ownershipReference(collectible.getCollectibleCode() + "-OWN-" + nextSerial)
                .transferHistory("CLAIMED_BY=" + profile.getFanCode())
                .build();
        FanCollectibleOwnership saved = collectibleOwnershipRepository.save(ownership);
        audit("DIGITAL_COLLECTIBLE", collectible.getId(), "COLLECTIBLE_CLAIMED", actor, saved.getOwnershipReference());
        Map<String, Object> response = mapOwnership(saved);
        response.put("collectible", mapCollectible(collectible));
        return response;
    }

    @Transactional
    public Map<String, Object> createPredictionGame(FanEconomyRequests.PredictionGameRequest request, String actor) {
        if (request == null) {
            throw ApiException.badRequest("Prediction game request is required");
        }
        String predictionType = normalize(request.getPredictionType(), "MATCH_WINNER");
        if (!PREDICTION_TYPES.contains(predictionType)) {
            throw ApiException.badRequest("Unsupported prediction type");
        }
        LocalDateTime deadline = request.getDeadlineAt() == null ? LocalDateTime.now().plusDays(7) : request.getDeadlineAt();
        if (!deadline.isAfter(LocalDateTime.now())) {
            throw ApiException.badRequest("Prediction deadline must be in the future");
        }
        PredictionGame game = PredictionGame.builder()
                .gameCode(generatePredictionCode())
                .title(required(request.getTitle(), "Prediction title is required"))
                .sport(normalize(request.getSport(), "CRICKET"))
                .targetType(defaultString(request.getTargetType(), "MATCH"))
                .targetRefId(request.getTargetRefId())
                .predictionType(predictionType)
                .pointsForCorrect(positiveInt(request.getPointsForCorrect(), "Prediction points"))
                .deadlineAt(deadline)
                .settlementRule(blankToNull(request.getSettlementRule()))
                .build();
        PredictionGame saved = predictionGameRepository.save(game);
        audit("PREDICTION_GAME", saved.getId(), "PREDICTION_GAME_CREATED", actor, saved.getGameCode());
        return mapPredictionGame(saved);
    }

    @Transactional
    public Map<String, Object> submitPrediction(Long gameId, FanEconomyRequests.PredictionEntryRequest request, String actor) {
        PredictionGame game = predictionGame(gameId);
        FanProfile profile = activeFanProfile(request == null ? null : request.getFanProfileId());
        if (!"ACTIVE".equalsIgnoreCase(game.getStatus())) {
            throw ApiException.badRequest("Prediction game is not active");
        }
        if (!game.getDeadlineAt().isAfter(LocalDateTime.now())) {
            throw ApiException.badRequest("Prediction deadline has passed");
        }
        predictionEntryRepository.findByGameIdAndFanProfileId(game.getId(), profile.getId())
                .ifPresent(existing -> {
                    throw ApiException.conflict("Fan already submitted a prediction for this game");
                });
        FanPredictionEntry entry = FanPredictionEntry.builder()
                .gameId(game.getId())
                .fanProfileId(profile.getId())
                .predictionValue(required(request.getPredictionValue(), "Prediction value is required"))
                .build();
        FanPredictionEntry saved = predictionEntryRepository.save(entry);
        audit("PREDICTION_GAME", game.getId(), "PREDICTION_SUBMITTED", actor, profile.getFanCode());
        return mapPredictionEntry(saved);
    }

    @Transactional
    public Map<String, Object> scorePrediction(Long entryId, FanEconomyRequests.PredictionScoreRequest request, String actor) {
        FanPredictionEntry entry = predictionEntry(entryId);
        PredictionGame game = predictionGame(entry.getGameId());
        FanProfile profile = fanProfile(entry.getFanProfileId());
        if (!"PENDING".equalsIgnoreCase(entry.getResultStatus())) {
            throw ApiException.badRequest("Prediction entry has already been scored");
        }
        String resultStatus = normalize(request == null ? null : request.getResultStatus(), "WRONG");
        if (!RESULT_STATUSES.contains(resultStatus)) {
            throw ApiException.badRequest("Unsupported prediction result status");
        }
        int points = switch (resultStatus) {
            case "CORRECT" -> safeInt(game.getPointsForCorrect());
            case "PARTIAL" -> safeInt(game.getPointsForCorrect()) / 2;
            default -> 0;
        };
        entry.setResultStatus(resultStatus);
        entry.setPointsAwarded(points);
        entry.setSettlementNote(blankToNull(request == null ? null : request.getSettlementNote()));
        FanPredictionEntry saved = predictionEntryRepository.save(entry);
        if (points > 0) {
            profile.setRankPoints(safeInt(profile.getRankPoints()) + points);
            profile.setFanScore(safeInt(profile.getFanScore()) + points);
            fanProfileRepository.save(profile);
        }
        audit("PREDICTION_ENTRY", saved.getId(), "PREDICTION_SCORED", actor, resultStatus + " +" + points);
        Map<String, Object> response = mapPredictionEntry(saved);
        response.put("fan", mapFanProfile(profile));
        return response;
    }

    @Transactional
    public Map<String, Object> createCreatorProfile(FanEconomyRequests.CreatorProfileRequest request, String actor) {
        if (request == null) {
            throw ApiException.badRequest("Creator profile request is required");
        }
        String email = required(request.getEmail(), "Creator email is required").toLowerCase(Locale.ROOT);
        if (creatorProfileRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("Creator profile already exists for this email");
        }
        CreatorProfile profile = CreatorProfile.builder()
                .displayName(required(request.getDisplayName(), "Creator display name is required"))
                .email(email)
                .city(blankToNull(request.getCity()))
                .creatorCategory(normalize(request.getCreatorCategory(), "FAN_CREATOR"))
                .bio(blankToNull(request.getBio()))
                .profileUrl(blankToNull(request.getProfileUrl()))
                .build();
        CreatorProfile saved = creatorProfileRepository.save(profile);
        audit("CREATOR_PROFILE", saved.getId(), "CREATOR_PROFILE_CREATED", actor, saved.getEmail());
        return mapCreatorProfile(saved);
    }

    @Transactional
    public Map<String, Object> publishCreatorContent(Long creatorProfileId, FanEconomyRequests.CreatorContentRequest request, String actor) {
        CreatorProfile creator = creatorProfile(creatorProfileId);
        if (request == null) {
            throw ApiException.badRequest("Creator content request is required");
        }
        String contentType = normalize(request.getContentType(), "POST");
        if (!CONTENT_TYPES.contains(contentType)) {
            throw ApiException.badRequest("Unsupported creator content type");
        }
        String visibility = normalize(request.getVisibility(), "PUBLIC");
        if (!VISIBILITY.contains(visibility)) {
            throw ApiException.badRequest("Unsupported creator content visibility");
        }
        CreatorContent content = CreatorContent.builder()
                .creatorProfileId(creator.getId())
                .title(required(request.getTitle(), "Content title is required"))
                .contentType(contentType)
                .sport(normalize(request.getSport(), "CRICKET"))
                .mediaUrl(blankToNull(request.getMediaUrl()))
                .visibility(visibility)
                .sponsored(Boolean.TRUE.equals(request.getSponsored()))
                .build();
        CreatorContent saved = creatorContentRepository.save(content);
        audit("CREATOR_CONTENT", saved.getId(), "CREATOR_CONTENT_PUBLISHED", actor, creator.getEmail());
        return mapCreatorContent(saved);
    }

    @Transactional
    public Map<String, Object> createMarketplaceListing(FanEconomyRequests.MarketplaceListingRequest request, String actor) {
        if (request == null) {
            throw ApiException.badRequest("Marketplace listing request is required");
        }
        FanProfile owner = activeFanProfile(request.getOwnerFanProfileId());
        FanCollectibleOwnership ownership = ownership(request.getCollectibleOwnershipId());
        if (!owner.getId().equals(ownership.getFanProfileId()) || !"ACTIVE".equalsIgnoreCase(ownership.getOwnershipStatus())) {
            throw ApiException.forbidden("Only the active owner can list this collectible");
        }
        FanMarketplaceListing listing = FanMarketplaceListing.builder()
                .listingCode(generateListingCode())
                .ownerFanProfileId(owner.getId())
                .collectibleOwnershipId(ownership.getId())
                .price(money(request.getPrice(), "Listing price"))
                .currency(defaultString(request.getCurrency(), "INR"))
                .build();
        FanMarketplaceListing saved = marketplaceListingRepository.save(listing);
        audit("FAN_MARKETPLACE", saved.getId(), "MARKETPLACE_LISTING_CREATED", actor, saved.getListingCode());
        return mapMarketplaceListing(saved);
    }

    @Transactional
    public Map<String, Object> purchaseMarketplaceListing(Long listingId, FanEconomyRequests.MarketplacePurchaseRequest request, String actor) {
        FanMarketplaceListing listing = marketplaceListing(listingId);
        FanProfile buyer = activeFanProfile(request == null ? null : request.getBuyerFanProfileId());
        if (!"ACTIVE".equalsIgnoreCase(listing.getStatus())) {
            throw ApiException.badRequest("Marketplace listing is not active");
        }
        if (buyer.getId().equals(listing.getOwnerFanProfileId())) {
            throw ApiException.badRequest("Owner cannot buy their own listing");
        }
        FanCollectibleOwnership ownership = ownership(listing.getCollectibleOwnershipId());
        if (!listing.getOwnerFanProfileId().equals(ownership.getFanProfileId())) {
            throw ApiException.conflict("Listing ownership is stale and requires review");
        }
        ownership.setFanProfileId(buyer.getId());
        ownership.setTransferHistory(defaultString(ownership.getTransferHistory(), "") + "|SOLD_TO=" + buyer.getFanCode());
        collectibleOwnershipRepository.save(ownership);
        listing.setBuyerFanProfileId(buyer.getId());
        listing.setStatus("SOLD");
        FanMarketplaceListing saved = marketplaceListingRepository.save(listing);
        audit("FAN_MARKETPLACE", saved.getId(), "MARKETPLACE_LISTING_SOLD", actor, buyer.getFanCode());
        Map<String, Object> response = mapMarketplaceListing(saved);
        response.put("ownership", mapOwnership(ownership));
        return response;
    }

    @Transactional
    public Map<String, Object> createWatchParty(FanEconomyRequests.WatchPartyRequest request, String actor) {
        if (request == null) {
            throw ApiException.badRequest("Watch party request is required");
        }
        FanProfile host = activeFanProfile(request.getHostFanProfileId());
        String accessMode = normalize(request.getAccessMode(), "PUBLIC");
        if (!ACCESS_MODES.contains(accessMode)) {
            throw ApiException.badRequest("Unsupported watch party access mode");
        }
        WatchParty party = WatchParty.builder()
                .partyCode(generatePartyCode())
                .title(required(request.getTitle(), "Watch party title is required"))
                .hostFanProfileId(host.getId())
                .sport(normalize(request.getSport(), host.getFavoriteSport()))
                .matchId(request.getMatchId())
                .tournamentId(request.getTournamentId())
                .platform(defaultString(request.getPlatform(), "IN_APP"))
                .capacity(positiveInt(request.getCapacity(), "Watch party capacity"))
                .scheduledAt(request.getScheduledAt() == null ? LocalDateTime.now().plusHours(2) : request.getScheduledAt())
                .accessMode(accessMode)
                .build();
        WatchParty saved = watchPartyRepository.save(party);
        audit("WATCH_PARTY", saved.getId(), "WATCH_PARTY_CREATED", actor, saved.getPartyCode());
        return mapWatchParty(saved);
    }

    @Transactional
    public Map<String, Object> joinWatchParty(Long watchPartyId, FanEconomyRequests.JoinWatchPartyRequest request, String actor) {
        WatchParty party = watchParty(watchPartyId);
        FanProfile profile = activeFanProfile(request == null ? null : request.getFanProfileId());
        if (!"SCHEDULED".equalsIgnoreCase(party.getStatus())) {
            throw ApiException.badRequest("Only scheduled watch parties can be joined");
        }
        if (safeInt(party.getParticipantCount()) >= safeInt(party.getCapacity())) {
            throw ApiException.conflict("Watch party capacity is full");
        }
        watchPartyParticipantRepository.findByWatchPartyIdAndFanProfileIdAndStatus(party.getId(), profile.getId(), "ACTIVE")
                .ifPresent(existing -> {
                    throw ApiException.conflict("Fan already joined this watch party");
                });
        WatchPartyParticipant participant = WatchPartyParticipant.builder()
                .watchPartyId(party.getId())
                .fanProfileId(profile.getId())
                .build();
        WatchPartyParticipant saved = watchPartyParticipantRepository.save(participant);
        party.setParticipantCount(safeInt(party.getParticipantCount()) + 1);
        watchPartyRepository.save(party);
        audit("WATCH_PARTY", party.getId(), "WATCH_PARTY_JOINED", actor, profile.getFanCode());
        Map<String, Object> response = mapWatchPartyParticipant(saved);
        response.put("party", mapWatchParty(party));
        return response;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> fans() {
        return fanProfileRepository.findAllByOrderByCreatedAtDesc().stream().map(this::mapFanProfile).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> fanClubs() {
        return fanClubRepository.findByStatusOrderByCreatedAtDesc("ACTIVE").stream().map(this::mapFanClub).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> collectibles() {
        return digitalCollectibleRepository.findByStatusOrderByCreatedAtDesc("ACTIVE").stream().map(this::mapCollectible).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> predictionGames() {
        return predictionGameRepository.findByStatusOrderByCreatedAtDesc("ACTIVE").stream().map(this::mapPredictionGame).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> creators() {
        return creatorProfileRepository.findAllByOrderByCreatedAtDesc().stream().map(this::mapCreatorProfile).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> marketplaceListings() {
        return marketplaceListingRepository.findByStatusOrderByCreatedAtDesc("ACTIVE").stream().map(this::mapMarketplaceListing).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> watchParties() {
        return watchPartyRepository.findByStatusOrderByScheduledAtAsc("SCHEDULED").stream().map(this::mapWatchParty).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> audits() {
        return auditRepository.findAllByOrderByCreatedAtDesc().stream().limit(50).map(this::mapAudit).toList();
    }

    private Map<String, Object> kpis(List<FanProfile> fans,
                                     List<FanClub> clubs,
                                     List<DigitalCollectible> collectibles,
                                     List<PredictionGame> predictionGames,
                                     List<CreatorProfile> creators,
                                     List<FanMarketplaceListing> listings,
                                     List<WatchParty> watchParties) {
        BigDecimal marketplaceValue = listings.stream()
                .map(FanMarketplaceListing::getPrice)
                .map(this::defaultMoney)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        Map<String, Object> kpis = new LinkedHashMap<>();
        kpis.put("fans", fans.size());
        kpis.put("activeFans", fans.stream().filter(item -> "ACTIVE".equalsIgnoreCase(item.getStatus())).count());
        kpis.put("fanClubs", clubs.size());
        kpis.put("fanClubMembers", clubs.stream().mapToInt(item -> safeInt(item.getMemberCount())).sum());
        kpis.put("collectibles", collectibles.size());
        kpis.put("mintedCollectibles", collectibles.stream().mapToInt(item -> safeInt(item.getMintedCount())).sum());
        kpis.put("predictionGames", predictionGames.size());
        kpis.put("creators", creators.size());
        kpis.put("monetizedCreators", creators.stream().filter(item -> Boolean.TRUE.equals(item.getMonetizationEnabled())).count());
        kpis.put("marketplaceListings", listings.size());
        kpis.put("marketplaceValue", marketplaceValue);
        kpis.put("watchParties", watchParties.size());
        kpis.put("watchPartyCapacity", watchParties.stream().mapToInt(item -> safeInt(item.getCapacity())).sum());
        return kpis;
    }

    private List<Map<String, Object>> economyControls() {
        return List.of(
                Map.of("name", "Collectible supply guard", "status", "ACTIVE", "detail", "Mint count cannot exceed configured supply."),
                Map.of("name", "Fan ownership transfer", "status", "ACTIVE", "detail", "Marketplace purchase transfers the ownership record to the buyer."),
                Map.of("name", "Prediction one-entry rule", "status", "ACTIVE", "detail", "One active prediction entry is accepted per fan per game."),
                Map.of("name", "Watch party capacity", "status", "ACTIVE", "detail", "Participants are capped by configured party capacity."),
                Map.of("name", "Audit trail", "status", "ACTIVE", "detail", "Profile, collectible, prediction, marketplace and watch party actions are recorded.")
        );
    }

    private Map<String, Object> mapFanProfile(FanProfile profile) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", profile.getId());
        map.put("fanCode", profile.getFanCode());
        map.put("displayName", profile.getDisplayName());
        map.put("email", profile.getEmail());
        map.put("city", profile.getCity());
        map.put("favoriteSport", profile.getFavoriteSport());
        map.put("favoriteTeam", profile.getFavoriteTeam());
        map.put("favoritePlayer", profile.getFavoritePlayer());
        map.put("avatarUrl", profile.getAvatarUrl());
        map.put("fanScore", safeInt(profile.getFanScore()));
        map.put("rankPoints", safeInt(profile.getRankPoints()));
        map.put("status", profile.getStatus());
        map.put("createdAt", profile.getCreatedAt());
        map.put("updatedAt", profile.getUpdatedAt());
        return map;
    }

    private Map<String, Object> mapFanClub(FanClub club) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", club.getId());
        map.put("clubCode", club.getClubCode());
        map.put("name", club.getName());
        map.put("sport", club.getSport());
        map.put("clubType", club.getClubType());
        map.put("linkedEntityType", club.getLinkedEntityType());
        map.put("linkedEntityId", club.getLinkedEntityId());
        map.put("city", club.getCity());
        map.put("visibility", club.getVisibility());
        map.put("status", club.getStatus());
        map.put("memberCount", safeInt(club.getMemberCount()));
        map.put("description", club.getDescription());
        map.put("createdAt", club.getCreatedAt());
        return map;
    }

    private Map<String, Object> mapFanClubMembership(FanClubMembership membership) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", membership.getId());
        map.put("fanProfileId", membership.getFanProfileId());
        map.put("fanClubId", membership.getFanClubId());
        map.put("role", membership.getRole());
        map.put("status", membership.getStatus());
        map.put("joinedAt", membership.getJoinedAt());
        return map;
    }

    private Map<String, Object> mapCollectible(DigitalCollectible collectible) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", collectible.getId());
        map.put("collectibleCode", collectible.getCollectibleCode());
        map.put("title", collectible.getTitle());
        map.put("collectibleType", collectible.getCollectibleType());
        map.put("rarity", collectible.getRarity());
        map.put("sourceModule", collectible.getSourceModule());
        map.put("sourceRefId", collectible.getSourceRefId());
        map.put("supplyLimit", safeInt(collectible.getSupplyLimit()));
        map.put("mintedCount", safeInt(collectible.getMintedCount()));
        map.put("mediaUrl", collectible.getMediaUrl());
        map.put("credentialReference", collectible.getCredentialReference());
        map.put("status", collectible.getStatus());
        map.put("createdAt", collectible.getCreatedAt());
        return map;
    }

    private Map<String, Object> mapOwnership(FanCollectibleOwnership ownership) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", ownership.getId());
        map.put("collectibleId", ownership.getCollectibleId());
        map.put("fanProfileId", ownership.getFanProfileId());
        map.put("ownershipReference", ownership.getOwnershipReference());
        map.put("serialNumber", ownership.getSerialNumber());
        map.put("ownershipStatus", ownership.getOwnershipStatus());
        map.put("transferHistory", ownership.getTransferHistory());
        map.put("acquiredAt", ownership.getAcquiredAt());
        return map;
    }

    private Map<String, Object> mapPredictionGame(PredictionGame game) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", game.getId());
        map.put("gameCode", game.getGameCode());
        map.put("title", game.getTitle());
        map.put("sport", game.getSport());
        map.put("targetType", game.getTargetType());
        map.put("targetRefId", game.getTargetRefId());
        map.put("predictionType", game.getPredictionType());
        map.put("pointsForCorrect", safeInt(game.getPointsForCorrect()));
        map.put("deadlineAt", game.getDeadlineAt());
        map.put("settlementRule", game.getSettlementRule());
        map.put("status", game.getStatus());
        return map;
    }

    private Map<String, Object> mapPredictionEntry(FanPredictionEntry entry) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", entry.getId());
        map.put("gameId", entry.getGameId());
        map.put("fanProfileId", entry.getFanProfileId());
        map.put("predictionValue", entry.getPredictionValue());
        map.put("resultStatus", entry.getResultStatus());
        map.put("pointsAwarded", safeInt(entry.getPointsAwarded()));
        map.put("settlementNote", entry.getSettlementNote());
        map.put("createdAt", entry.getCreatedAt());
        return map;
    }

    private Map<String, Object> mapCreatorProfile(CreatorProfile creator) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", creator.getId());
        map.put("displayName", creator.getDisplayName());
        map.put("email", creator.getEmail());
        map.put("city", creator.getCity());
        map.put("creatorCategory", creator.getCreatorCategory());
        map.put("bio", creator.getBio());
        map.put("profileUrl", creator.getProfileUrl());
        map.put("verificationStatus", creator.getVerificationStatus());
        map.put("monetizationEnabled", creator.getMonetizationEnabled());
        map.put("revenueBalance", defaultMoney(creator.getRevenueBalance()));
        map.put("createdAt", creator.getCreatedAt());
        return map;
    }

    private Map<String, Object> mapCreatorContent(CreatorContent content) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", content.getId());
        map.put("creatorProfileId", content.getCreatorProfileId());
        map.put("title", content.getTitle());
        map.put("contentType", content.getContentType());
        map.put("sport", content.getSport());
        map.put("mediaUrl", content.getMediaUrl());
        map.put("visibility", content.getVisibility());
        map.put("sponsored", content.getSponsored());
        map.put("engagementCount", safeInt(content.getEngagementCount()));
        map.put("status", content.getStatus());
        map.put("createdAt", content.getCreatedAt());
        return map;
    }

    private Map<String, Object> mapMarketplaceListing(FanMarketplaceListing listing) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", listing.getId());
        map.put("listingCode", listing.getListingCode());
        map.put("ownerFanProfileId", listing.getOwnerFanProfileId());
        map.put("collectibleOwnershipId", listing.getCollectibleOwnershipId());
        map.put("price", defaultMoney(listing.getPrice()));
        map.put("currency", listing.getCurrency());
        map.put("buyerFanProfileId", listing.getBuyerFanProfileId());
        map.put("status", listing.getStatus());
        map.put("createdAt", listing.getCreatedAt());
        return map;
    }

    private Map<String, Object> mapWatchParty(WatchParty party) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", party.getId());
        map.put("partyCode", party.getPartyCode());
        map.put("title", party.getTitle());
        map.put("hostFanProfileId", party.getHostFanProfileId());
        map.put("sport", party.getSport());
        map.put("matchId", party.getMatchId());
        map.put("tournamentId", party.getTournamentId());
        map.put("platform", party.getPlatform());
        map.put("capacity", safeInt(party.getCapacity()));
        map.put("participantCount", safeInt(party.getParticipantCount()));
        map.put("scheduledAt", party.getScheduledAt());
        map.put("accessMode", party.getAccessMode());
        map.put("status", party.getStatus());
        return map;
    }

    private Map<String, Object> mapWatchPartyParticipant(WatchPartyParticipant participant) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", participant.getId());
        map.put("watchPartyId", participant.getWatchPartyId());
        map.put("fanProfileId", participant.getFanProfileId());
        map.put("status", participant.getStatus());
        map.put("joinedAt", participant.getJoinedAt());
        return map;
    }

    private Map<String, Object> mapAudit(FanEconomyAudit audit) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", audit.getId());
        map.put("entityType", audit.getEntityType());
        map.put("entityId", audit.getEntityId());
        map.put("action", audit.getAction());
        map.put("actor", audit.getActor());
        map.put("details", audit.getDetails());
        map.put("createdAt", audit.getCreatedAt());
        return map;
    }

    private FanProfile fanProfile(Long id) {
        if (id == null) {
            throw ApiException.badRequest("Fan profile id is required");
        }
        return fanProfileRepository.findById(id).orElseThrow(() -> ApiException.notFound("Fan profile not found"));
    }

    private FanProfile activeFanProfile(Long id) {
        FanProfile profile = fanProfile(id);
        if (!"ACTIVE".equalsIgnoreCase(profile.getStatus())) {
            throw ApiException.forbidden("Fan profile is not active");
        }
        return profile;
    }

    private FanClub fanClub(Long id) {
        if (id == null) {
            throw ApiException.badRequest("Fan club id is required");
        }
        return fanClubRepository.findById(id).orElseThrow(() -> ApiException.notFound("Fan club not found"));
    }

    private DigitalCollectible collectible(Long id) {
        if (id == null) {
            throw ApiException.badRequest("Collectible id is required");
        }
        return digitalCollectibleRepository.findById(id).orElseThrow(() -> ApiException.notFound("Collectible not found"));
    }

    private FanCollectibleOwnership ownership(Long id) {
        if (id == null) {
            throw ApiException.badRequest("Collectible ownership id is required");
        }
        return collectibleOwnershipRepository.findById(id).orElseThrow(() -> ApiException.notFound("Collectible ownership not found"));
    }

    private PredictionGame predictionGame(Long id) {
        if (id == null) {
            throw ApiException.badRequest("Prediction game id is required");
        }
        return predictionGameRepository.findById(id).orElseThrow(() -> ApiException.notFound("Prediction game not found"));
    }

    private FanPredictionEntry predictionEntry(Long id) {
        if (id == null) {
            throw ApiException.badRequest("Prediction entry id is required");
        }
        return predictionEntryRepository.findById(id).orElseThrow(() -> ApiException.notFound("Prediction entry not found"));
    }

    private CreatorProfile creatorProfile(Long id) {
        if (id == null) {
            throw ApiException.badRequest("Creator profile id is required");
        }
        return creatorProfileRepository.findById(id).orElseThrow(() -> ApiException.notFound("Creator profile not found"));
    }

    private FanMarketplaceListing marketplaceListing(Long id) {
        if (id == null) {
            throw ApiException.badRequest("Marketplace listing id is required");
        }
        return marketplaceListingRepository.findById(id).orElseThrow(() -> ApiException.notFound("Marketplace listing not found"));
    }

    private WatchParty watchParty(Long id) {
        if (id == null) {
            throw ApiException.badRequest("Watch party id is required");
        }
        return watchPartyRepository.findById(id).orElseThrow(() -> ApiException.notFound("Watch party not found"));
    }

    private void audit(String entityType, Long entityId, String action, String actor, String details) {
        auditRepository.save(FanEconomyAudit.builder()
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .actor(defaultString(actor, "system"))
                .details(blankToNull(details))
                .build());
    }

    private String generateFanCode() {
        return unique("TFAN-", fanProfileRepository::existsByFanCode);
    }

    private String generateClubCode() {
        return unique("TFCLUB-", fanClubRepository::existsByClubCode);
    }

    private String generateCollectibleCode() {
        return unique("TFDC-", digitalCollectibleRepository::existsByCollectibleCode);
    }

    private String generatePredictionCode() {
        return unique("TFPRED-", predictionGameRepository::existsByGameCode);
    }

    private String generateListingCode() {
        return unique("TFMKT-", marketplaceListingRepository::existsByListingCode);
    }

    private String generatePartyCode() {
        return unique("TFWP-", watchPartyRepository::existsByPartyCode);
    }

    private String unique(String prefix, java.util.function.Predicate<String> exists) {
        String value;
        do {
            value = prefix + LocalDateTime.now().getYear() + "-" + (100000 + RANDOM.nextInt(900000));
        } while (exists.test(value));
        return value;
    }

    private String required(String value, String message) {
        String normalized = blankToNull(value);
        if (normalized == null) {
            throw ApiException.badRequest(message);
        }
        return normalized;
    }

    private String normalize(String value, String fallback) {
        return defaultString(value, fallback).trim().toUpperCase(Locale.ROOT).replace(' ', '_');
    }

    private String defaultString(String value, String fallback) {
        String normalized = blankToNull(value);
        return normalized == null ? fallback : normalized;
    }

    private String blankToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private int positiveInt(Integer value, String label) {
        int number = value == null ? 1 : value;
        if (number <= 0) {
            throw ApiException.badRequest(label + " must be greater than zero");
        }
        return number;
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private BigDecimal money(BigDecimal value, String label) {
        BigDecimal amount = defaultMoney(value);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw ApiException.badRequest(label + " must be greater than zero");
        }
        return amount;
    }

    private BigDecimal defaultMoney(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }
}
