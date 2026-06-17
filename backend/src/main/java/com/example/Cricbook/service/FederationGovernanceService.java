package com.example.Cricbook.service;

import com.example.Cricbook.dto.FederationGovernanceRequests;
import com.example.Cricbook.entity.*;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FederationGovernanceService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Set<String> BODY_TYPES = Set.of("NATIONAL_FEDERATION", "STATE_ASSOCIATION", "DISTRICT_ASSOCIATION", "UNIVERSITY_LEAGUE", "CORPORATE_LEAGUE");
    private static final Set<String> BODY_STATUSES = Set.of("ACTIVE", "SUSPENDED", "ARCHIVED", "INACTIVE");
    private static final Set<String> PLAYER_VERIFICATION_STATUSES = Set.of("PENDING", "VERIFIED", "REJECTED", "DOCUMENTS_REQUIRED");
    private static final Set<String> AUCTION_STATUSES = Set.of("PUBLISHED", "LIVE", "SOLD", "UNSOLD", "CANCELLED");
    private static final Set<String> TRANSFER_STATUSES = Set.of("REQUESTED", "APPROVED", "REJECTED", "COMPLETED", "CANCELLED");
    private static final Set<String> CASE_STATUSES = Set.of("OPEN", "INVESTIGATING", "ESCALATED", "CLOSED");
    private static final Set<String> ELECTION_STATUSES = Set.of("DRAFT", "OPEN", "CLOSED", "RESULT_PUBLISHED", "DISPUTED");

    private final FederationBodyRepository bodyRepository;
    private final FederationFranchiseRepository franchiseRepository;
    private final FederationPlayerRegistrationRepository playerRepository;
    private final FederationDraftRepository draftRepository;
    private final FederationDraftSelectionRepository draftSelectionRepository;
    private final FederationAuctionRepository auctionRepository;
    private final FederationAuctionBidRepository auctionBidRepository;
    private final FederationTransferRepository transferRepository;
    private final FederationLicenseRepository licenseRepository;
    private final FederationOfficialRepository officialRepository;
    private final FederationDisciplinaryCaseRepository caseRepository;
    private final FederationElectionRepository electionRepository;
    private final FederationElectionVoteRepository voteRepository;
    private final FederationRankingRepository rankingRepository;
    private final FederationIntegrityAlertRepository integrityAlertRepository;
    private final FederationGovernanceAuditRepository auditRepository;
    private final CareerProfileRepository careerProfileRepository;
    private final TournamentRepository tournamentRepository;
    private final CorporateLeagueRepository corporateLeagueRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> dashboard() {
        List<FederationBody> bodies = bodyRepository.findAllByOrderByCreatedAtDesc();
        List<FederationFranchise> franchises = franchiseRepository.findAllByOrderByCreatedAtDesc();
        List<FederationPlayerRegistration> players = playerRepository.findAllByOrderByCreatedAtDesc();
        List<FederationDraft> drafts = draftRepository.findAllByOrderByCreatedAtDesc();
        List<FederationAuction> auctions = auctionRepository.findAllByOrderByCreatedAtDesc();
        List<FederationTransfer> transfers = transferRepository.findAllByOrderByCreatedAtDesc();
        List<FederationLicense> licenses = licenseRepository.findAllByOrderByCreatedAtDesc();
        List<FederationOfficial> officials = officialRepository.findAllByOrderByCreatedAtDesc();
        List<FederationDisciplinaryCase> cases = caseRepository.findAllByOrderByCreatedAtDesc();
        List<FederationElection> elections = electionRepository.findAllByOrderByCreatedAtDesc();
        List<FederationRanking> rankings = rankingRepository.findAllByOrderByPointsDescCreatedAtDesc();
        List<FederationIntegrityAlert> alerts = integrityAlertRepository.findAllByOrderByCreatedAtDesc();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("kpis", kpis(bodies, franchises, players, drafts, auctions, transfers, licenses, officials, cases, elections, alerts));
        response.put("federations", bodies.stream().filter(body -> "NATIONAL_FEDERATION".equalsIgnoreCase(body.getBodyType())).limit(12).map(this::mapBody).toList());
        response.put("associations", bodies.stream().filter(body -> !"NATIONAL_FEDERATION".equalsIgnoreCase(body.getBodyType())).limit(12).map(this::mapBody).toList());
        response.put("franchises", franchises.stream().limit(12).map(this::mapFranchise).toList());
        response.put("playerRegistry", players.stream().limit(12).map(this::mapPlayer).toList());
        response.put("drafts", drafts.stream().limit(10).map(this::mapDraft).toList());
        response.put("auctions", auctions.stream().limit(10).map(this::mapAuction).toList());
        response.put("transfers", transfers.stream().limit(10).map(this::mapTransfer).toList());
        response.put("licenses", licenses.stream().limit(10).map(this::mapLicense).toList());
        response.put("officials", officials.stream().limit(10).map(this::mapOfficial).toList());
        response.put("disciplinaryCases", cases.stream().limit(10).map(this::mapCase).toList());
        response.put("elections", elections.stream().limit(10).map(this::mapElection).toList());
        response.put("rankings", rankings.stream().limit(15).map(this::mapRanking).toList());
        response.put("integrityAlerts", alerts.stream().limit(10).map(this::mapIntegrityAlert).toList());
        response.put("analytics", analytics(bodies, players, franchises, transfers, alerts));
        response.put("compliance", complianceControls(players, franchises, licenses, cases, alerts));
        response.put("priorityActions", priorityActions(bodies, players, drafts, auctions, transfers, cases, elections, alerts));
        response.put("crossEpicSignals", crossEpicSignals());
        response.put("audits", audits());
        response.put("generatedAt", LocalDateTime.now());
        return response;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> publicFederations(String sport, String region) {
        return bodyRepository.findByStatusOrderByCreatedAtDesc("ACTIVE").stream()
                .filter(body -> matches(sport, body.getSport()))
                .filter(body -> matches(region, body.getRegion()))
                .map(this::mapBody)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> publicRankings(String sport, String type) {
        Map<Long, FederationBody> federationById = bodyRepository.findAll().stream()
                .collect(Collectors.toMap(FederationBody::getId, Function.identity(), (left, right) -> left));
        return rankingRepository.findAllByOrderByPointsDescCreatedAtDesc().stream()
                .filter(ranking -> {
                    FederationBody federation = federationById.get(ranking.getFederationId());
                    return federation == null || matches(sport, federation.getSport());
                })
                .filter(ranking -> matches(type, ranking.getRankingType()))
                .limit(50)
                .map(this::mapRanking)
                .toList();
    }

    @Transactional
    public Map<String, Object> createBody(FederationGovernanceRequests.BodyRequest request, String actor) {
        String name = required(request == null ? null : request.getName(), "Federation or association name is required");
        String bodyType = normalize(request.getBodyType(), "NATIONAL_FEDERATION");
        if (!BODY_TYPES.contains(bodyType)) {
            throw ApiException.badRequest("Unsupported federation body type");
        }
        if (bodyRepository.existsByNameIgnoreCaseAndBodyTypeIgnoreCase(name, bodyType)) {
            throw ApiException.conflict("Federation body already exists");
        }
        if (request.getParentBodyId() != null) {
            body(request.getParentBodyId());
        }
        FederationBody saved = bodyRepository.save(FederationBody.builder()
                .parentBodyId(request.getParentBodyId())
                .name(name)
                .bodyType(bodyType)
                .sport(defaultString(request.getSport(), "CRICKET"))
                .region(blankToNull(request.getRegion()))
                .registrationNumber(blankToNull(request.getRegistrationNumber()))
                .website(validateOptionalUrl(request.getWebsite(), "Website"))
                .contactEmail(blankToNull(request.getContactEmail()))
                .governanceNotes(blankToNull(request.getGovernanceNotes()))
                .complianceScore(complianceScore(bodyType, request.getRegistrationNumber(), request.getContactEmail()))
                .build());
        audit("FEDERATION_BODY", saved.getId(), "BODY_CREATED", actor, saved.getBodyType() + " created: " + saved.getName());
        return mapBody(saved);
    }

    @Transactional
    public Map<String, Object> updateBodyStatus(Long id, FederationGovernanceRequests.StatusRequest request, String actor) {
        FederationBody body = body(id);
        String status = normalize(request == null ? null : request.getStatus(), "ACTIVE");
        if (!BODY_STATUSES.contains(status)) {
            throw ApiException.badRequest("Unsupported federation body status");
        }
        body.setStatus(status);
        FederationBody saved = bodyRepository.save(body);
        audit("FEDERATION_BODY", saved.getId(), "BODY_" + status, actor, request == null ? null : request.getNotes());
        return mapBody(saved);
    }

    @Transactional
    public Map<String, Object> createFranchise(FederationGovernanceRequests.FranchiseRequest request, String actor) {
        Long federationId = requiredId(request == null ? null : request.getFederationId(), "Federation ID is required");
        body(federationId);
        String name = required(request.getName(), "Franchise name is required");
        if (franchiseRepository.existsByFederationIdAndNameIgnoreCase(federationId, name)) {
            throw ApiException.conflict("Franchise already exists for this federation");
        }
        FederationFranchise saved = franchiseRepository.save(FederationFranchise.builder()
                .federationId(federationId)
                .name(name)
                .ownerName(blankToNull(request.getOwnerName()))
                .city(blankToNull(request.getCity()))
                .homeVenue(blankToNull(request.getHomeVenue()))
                .salaryCap(defaultMoney(request.getSalaryCap()))
                .build());
        audit("FEDERATION_FRANCHISE", saved.getId(), "FRANCHISE_CREATED", actor, saved.getName());
        return mapFranchise(saved);
    }

    @Transactional
    public Map<String, Object> registerPlayer(FederationGovernanceRequests.PlayerRegistrationRequest request, String actor) {
        String email = required(request == null ? null : request.getEmail(), "Player email is required").toLowerCase(Locale.ROOT);
        if (playerRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("Player already has a central sports registration");
        }
        if (request.getFederationId() != null) {
            body(request.getFederationId());
        }
        CareerProfile careerProfile = null;
        if (request.getCareerProfileId() != null) {
            careerProfile = careerProfileRepository.findById(request.getCareerProfileId())
                    .orElseThrow(() -> ApiException.notFound("Career profile not found"));
        }
        String sportsId = sportsId();
        FederationPlayerRegistration player = FederationPlayerRegistration.builder()
                .federationId(request.getFederationId())
                .careerProfileId(request.getCareerProfileId())
                .sportsId(sportsId)
                .playerName(defaultString(request.getPlayerName(), careerProfile == null ? null : careerProfile.getPlayerName()))
                .email(email)
                .phone(blankToNull(request.getPhone()))
                .sport(defaultString(request.getSport(), careerProfile == null ? "CRICKET" : careerProfile.getSport()))
                .city(defaultString(request.getCity(), careerProfile == null ? null : careerProfile.getCity()))
                .ageGroup(defaultString(request.getAgeGroup(), careerProfile == null ? "OPEN" : careerProfile.getAgeGroup()))
                .dateOfBirth(request.getDateOfBirth())
                .identityDocumentUrl(validateOptionalUrl(request.getIdentityDocumentUrl(), "Identity document"))
                .eligibilityScore(eligibilityScore(request, careerProfile))
                .rankingScore(careerProfile == null ? 0 : defaultInt(careerProfile.getRankingScore()))
                .build();
        if (player.getPlayerName() == null || player.getPlayerName().isBlank()) {
            throw ApiException.badRequest("Player name is required");
        }
        FederationPlayerRegistration saved = playerRepository.save(player);
        audit("FEDERATION_PLAYER", saved.getId(), "PLAYER_REGISTERED", actor, "Sports ID generated: " + saved.getSportsId());
        return mapPlayer(saved);
    }

    @Transactional
    public Map<String, Object> updatePlayerVerification(Long id, FederationGovernanceRequests.StatusRequest request, String actor) {
        FederationPlayerRegistration player = player(id);
        String status = normalize(request == null ? null : request.getStatus(), "VERIFIED");
        if (!PLAYER_VERIFICATION_STATUSES.contains(status)) {
            throw ApiException.badRequest("Unsupported player verification status");
        }
        player.setVerificationStatus(status);
        player.setStatus("VERIFIED".equals(status) ? "ELIGIBLE" : player.getStatus());
        if ("REJECTED".equals(status)) {
            player.setStatus("REJECTED");
        }
        FederationPlayerRegistration saved = playerRepository.save(player);
        audit("FEDERATION_PLAYER", saved.getId(), "PLAYER_" + status, actor, request == null ? null : request.getNotes());
        return mapPlayer(saved);
    }

    @Transactional
    public Map<String, Object> createDraft(FederationGovernanceRequests.DraftRequest request, String actor) {
        Long federationId = requiredId(request == null ? null : request.getFederationId(), "Federation ID is required");
        body(federationId);
        FederationDraft saved = draftRepository.save(FederationDraft.builder()
                .federationId(federationId)
                .name(required(request.getName(), "Draft name is required"))
                .sport(defaultString(request.getSport(), "CRICKET"))
                .season(blankToNull(request.getSeason()))
                .draftOrderFranchiseIds(blankToNull(request.getDraftOrderFranchiseIds()))
                .startsAt(request.getStartsAt())
                .status("PUBLISHED")
                .build());
        audit("FEDERATION_DRAFT", saved.getId(), "DRAFT_CREATED", actor, saved.getName());
        return mapDraft(saved);
    }

    @Transactional
    public Map<String, Object> selectDraftPlayer(Long draftId, FederationGovernanceRequests.DraftSelectionRequest request, String actor) {
        FederationDraft draft = draft(draftId);
        if ("ARCHIVED".equalsIgnoreCase(draft.getStatus()) || "COMPLETED".equalsIgnoreCase(draft.getStatus())) {
            throw ApiException.badRequest("Draft is not open for selections");
        }
        Long playerId = requiredId(request == null ? null : request.getPlayerRegistrationId(), "Player registration ID is required");
        Long franchiseId = requiredId(request.getFranchiseId(), "Franchise ID is required");
        FederationPlayerRegistration player = player(playerId);
        FederationFranchise franchise = franchise(franchiseId);
        if (!Objects.equals(franchise.getFederationId(), draft.getFederationId())) {
            throw ApiException.badRequest("Franchise does not belong to this draft federation");
        }
        if (draftSelectionRepository.existsByDraftIdAndPlayerRegistrationId(draftId, playerId)) {
            throw ApiException.conflict("Player has already been selected in this draft");
        }
        if (!"VERIFIED".equalsIgnoreCase(player.getVerificationStatus()) && !"ELIGIBLE".equalsIgnoreCase(player.getStatus())) {
            throw ApiException.badRequest("Player must be verified before draft selection");
        }
        int pickNumber = (int) draftSelectionRepository.countByDraftId(draftId) + 1;
        FederationDraftSelection selection = draftSelectionRepository.save(FederationDraftSelection.builder()
                .draftId(draftId)
                .franchiseId(franchiseId)
                .playerRegistrationId(playerId)
                .pickNumber(pickNumber)
                .selectionType(normalize(request.getSelectionType(), "MANUAL"))
                .selectedBy(actor)
                .build());
        player.setCurrentFranchiseId(franchiseId);
        player.setStatus("FRANCHISE_ASSIGNED");
        playerRepository.save(player);
        franchise.setRosterSize(defaultInt(franchise.getRosterSize()) + 1);
        franchiseRepository.save(franchise);
        draft.setCompletedPicks(pickNumber);
        draft.setStatus("IN_PROGRESS");
        draftRepository.save(draft);
        audit("FEDERATION_DRAFT_SELECTION", selection.getId(), "DRAFT_PLAYER_SELECTED", actor, "Pick " + pickNumber);
        return mapDraftSelection(selection);
    }

    @Transactional
    public Map<String, Object> createAuction(FederationGovernanceRequests.AuctionRequest request, String actor) {
        Long federationId = requiredId(request == null ? null : request.getFederationId(), "Federation ID is required");
        Long playerId = requiredId(request.getPlayerRegistrationId(), "Player registration ID is required");
        body(federationId);
        player(playerId);
        BigDecimal basePrice = defaultMoney(request.getBasePrice());
        FederationAuction saved = auctionRepository.save(FederationAuction.builder()
                .federationId(federationId)
                .playerRegistrationId(playerId)
                .auctionName(required(request.getAuctionName(), "Auction name is required"))
                .basePrice(basePrice)
                .currentBid(basePrice)
                .startsAt(request.getStartsAt())
                .endsAt(request.getEndsAt())
                .status("PUBLISHED")
                .build());
        audit("FEDERATION_AUCTION", saved.getId(), "AUCTION_CREATED", actor, saved.getAuctionName());
        return mapAuction(saved);
    }

    @Transactional
    public Map<String, Object> placeBid(Long auctionId, FederationGovernanceRequests.BidRequest request, String actor) {
        FederationAuction auction = auction(auctionId);
        if (!Set.of("PUBLISHED", "LIVE").contains(auction.getStatus())) {
            throw ApiException.badRequest("Auction is not open for bids");
        }
        Long franchiseId = requiredId(request == null ? null : request.getFranchiseId(), "Franchise ID is required");
        FederationFranchise franchise = franchise(franchiseId);
        if (!Objects.equals(franchise.getFederationId(), auction.getFederationId())) {
            throw ApiException.badRequest("Franchise does not belong to this auction federation");
        }
        BigDecimal bidAmount = request.getBidAmount();
        if (bidAmount == null || bidAmount.compareTo(auction.getCurrentBid()) <= 0) {
            throw ApiException.badRequest("Bid must be greater than current bid");
        }
        BigDecimal projectedSpend = defaultMoney(franchise.getUsedBudget()).add(bidAmount);
        if (defaultMoney(franchise.getSalaryCap()).compareTo(BigDecimal.ZERO) > 0 && projectedSpend.compareTo(franchise.getSalaryCap()) > 0) {
            throw ApiException.badRequest("Bid exceeds franchise salary cap");
        }
        FederationAuctionBid bid = auctionBidRepository.save(FederationAuctionBid.builder()
                .auctionId(auctionId)
                .franchiseId(franchiseId)
                .bidAmount(bidAmount)
                .bidderName(defaultString(request.getBidderName(), actor))
                .build());
        auction.setStatus("LIVE");
        auction.setCurrentBid(bidAmount);
        auction.setHighestBidFranchiseId(franchiseId);
        auction.setBidCount(defaultInt(auction.getBidCount()) + 1);
        auctionRepository.save(auction);
        audit("FEDERATION_AUCTION_BID", bid.getId(), "AUCTION_BID_PLACED", actor, bidAmount.toPlainString());
        return mapAuctionBid(bid);
    }

    @Transactional
    public Map<String, Object> completeAuction(Long auctionId, FederationGovernanceRequests.StatusRequest request, String actor) {
        FederationAuction auction = auction(auctionId);
        String status = normalize(request == null ? null : request.getStatus(), auction.getHighestBidFranchiseId() == null ? "UNSOLD" : "SOLD");
        if (!AUCTION_STATUSES.contains(status)) {
            throw ApiException.badRequest("Unsupported auction status");
        }
        auction.setStatus(status);
        FederationAuction saved = auctionRepository.save(auction);
        if ("SOLD".equals(status)) {
            if (saved.getHighestBidFranchiseId() == null) {
                throw ApiException.badRequest("Cannot mark auction sold without a winning bid");
            }
            FederationPlayerRegistration player = player(saved.getPlayerRegistrationId());
            FederationFranchise franchise = franchise(saved.getHighestBidFranchiseId());
            player.setCurrentFranchiseId(franchise.getId());
            player.setStatus("FRANCHISE_ASSIGNED");
            playerRepository.save(player);
            franchise.setRosterSize(defaultInt(franchise.getRosterSize()) + 1);
            franchise.setUsedBudget(defaultMoney(franchise.getUsedBudget()).add(defaultMoney(saved.getCurrentBid())));
            franchiseRepository.save(franchise);
        }
        audit("FEDERATION_AUCTION", saved.getId(), "AUCTION_" + status, actor, request == null ? null : request.getNotes());
        return mapAuction(saved);
    }

    @Transactional
    public Map<String, Object> createTransfer(FederationGovernanceRequests.TransferRequest request, String actor) {
        if (!Boolean.TRUE.equals(request == null ? null : request.getTransferWindowOpen())) {
            throw ApiException.badRequest("Transfers are blocked outside the active transfer window");
        }
        Long playerId = requiredId(request.getPlayerRegistrationId(), "Player registration ID is required");
        Long toFranchiseId = requiredId(request.getToFranchiseId(), "Destination franchise ID is required");
        FederationPlayerRegistration player = player(playerId);
        franchise(toFranchiseId);
        FederationTransfer saved = transferRepository.save(FederationTransfer.builder()
                .federationId(requiredId(request.getFederationId(), "Federation ID is required"))
                .playerRegistrationId(playerId)
                .fromFranchiseId(player.getCurrentFranchiseId())
                .toFranchiseId(toFranchiseId)
                .transferType(normalize(request.getTransferType(), "PERMANENT"))
                .effectiveFrom(request.getEffectiveFrom())
                .effectiveTo(request.getEffectiveTo())
                .reason(blankToNull(request.getReason()))
                .build());
        audit("FEDERATION_TRANSFER", saved.getId(), "TRANSFER_REQUESTED", actor, saved.getTransferType());
        return mapTransfer(saved);
    }

    @Transactional
    public Map<String, Object> updateTransferStatus(Long transferId, FederationGovernanceRequests.StatusRequest request, String actor) {
        FederationTransfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> ApiException.notFound("Transfer request not found"));
        String status = normalize(request == null ? null : request.getStatus(), "APPROVED");
        if (!TRANSFER_STATUSES.contains(status)) {
            throw ApiException.badRequest("Unsupported transfer status");
        }
        transfer.setStatus(status);
        transfer.setReviewedBy(actor);
        transfer.setReviewedAt(LocalDateTime.now());
        FederationTransfer saved = transferRepository.save(transfer);
        if ("APPROVED".equals(status) || "COMPLETED".equals(status)) {
            FederationPlayerRegistration player = player(saved.getPlayerRegistrationId());
            player.setCurrentFranchiseId(saved.getToFranchiseId());
            player.setStatus("TRANSFERRED");
            playerRepository.save(player);
            adjustRoster(saved.getFromFranchiseId(), -1);
            adjustRoster(saved.getToFranchiseId(), 1);
        }
        audit("FEDERATION_TRANSFER", saved.getId(), "TRANSFER_" + status, actor, request == null ? null : request.getNotes());
        return mapTransfer(saved);
    }

    @Transactional
    public Map<String, Object> issueLicense(FederationGovernanceRequests.LicenseRequest request, String actor) {
        Long federationId = requiredId(request == null ? null : request.getFederationId(), "Federation ID is required");
        body(federationId);
        String licenseNumber = licenseNumber();
        FederationLicense saved = licenseRepository.save(FederationLicense.builder()
                .federationId(federationId)
                .holderType(normalize(request.getHolderType(), "PLAYER"))
                .holderId(requiredId(request.getHolderId(), "License holder ID is required"))
                .licenseNumber(licenseNumber)
                .licenseType(normalize(request.getLicenseType(), "PLAYER"))
                .validFrom(request.getValidFrom() == null ? LocalDate.now() : request.getValidFrom())
                .validUntil(request.getValidUntil() == null ? LocalDate.now().plusYears(1) : request.getValidUntil())
                .build());
        audit("FEDERATION_LICENSE", saved.getId(), "LICENSE_ISSUED", actor, saved.getLicenseNumber());
        return mapLicense(saved);
    }

    @Transactional
    public Map<String, Object> updateLicenseStatus(Long id, FederationGovernanceRequests.StatusRequest request, String actor) {
        FederationLicense license = licenseRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("License not found"));
        license.setStatus(normalize(request == null ? null : request.getStatus(), "ACTIVE"));
        FederationLicense saved = licenseRepository.save(license);
        audit("FEDERATION_LICENSE", saved.getId(), "LICENSE_" + saved.getStatus(), actor, request == null ? null : request.getNotes());
        return mapLicense(saved);
    }

    @Transactional
    public Map<String, Object> registerOfficial(FederationGovernanceRequests.OfficialRequest request, String actor) {
        Long federationId = requiredId(request == null ? null : request.getFederationId(), "Federation ID is required");
        body(federationId);
        FederationOfficial saved = officialRepository.save(FederationOfficial.builder()
                .federationId(federationId)
                .name(required(request.getName(), "Official name is required"))
                .email(required(request.getEmail(), "Official email is required").toLowerCase(Locale.ROOT))
                .officialType(normalize(request.getOfficialType(), "UMPIRE"))
                .certificationUrl(validateOptionalUrl(request.getCertificationUrl(), "Certification"))
                .certificationValidUntil(request.getCertificationValidUntil())
                .build());
        audit("FEDERATION_OFFICIAL", saved.getId(), "OFFICIAL_REGISTERED", actor, saved.getOfficialType());
        return mapOfficial(saved);
    }

    @Transactional
    public Map<String, Object> updateOfficialStatus(Long id, FederationGovernanceRequests.StatusRequest request, String actor) {
        FederationOfficial official = officialRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Official not found"));
        official.setStatus(normalize(request == null ? null : request.getStatus(), "VERIFIED"));
        FederationOfficial saved = officialRepository.save(official);
        audit("FEDERATION_OFFICIAL", saved.getId(), "OFFICIAL_" + saved.getStatus(), actor, request == null ? null : request.getNotes());
        return mapOfficial(saved);
    }

    @Transactional
    public Map<String, Object> openDisciplinaryCase(FederationGovernanceRequests.CaseRequest request, String actor) {
        Long federationId = requiredId(request == null ? null : request.getFederationId(), "Federation ID is required");
        body(federationId);
        FederationDisciplinaryCase saved = caseRepository.save(FederationDisciplinaryCase.builder()
                .federationId(federationId)
                .subjectType(normalize(request.getSubjectType(), "PLAYER"))
                .subjectId(requiredId(request.getSubjectId(), "Subject ID is required"))
                .caseType(required(request.getCaseType(), "Case type is required"))
                .severity(normalize(request.getSeverity(), "MEDIUM"))
                .investigator(blankToNull(request.getInvestigator()))
                .evidenceSummary(blankToNull(request.getEvidenceSummary()))
                .build());
        audit("FEDERATION_CASE", saved.getId(), "CASE_OPENED", actor, saved.getCaseType());
        return mapCase(saved);
    }

    @Transactional
    public Map<String, Object> resolveDisciplinaryCase(Long id, FederationGovernanceRequests.StatusRequest request, String actor) {
        FederationDisciplinaryCase caseItem = caseRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Disciplinary case not found"));
        String status = normalize(request == null ? null : request.getStatus(), "CLOSED");
        if (!CASE_STATUSES.contains(status)) {
            throw ApiException.badRequest("Unsupported disciplinary case status");
        }
        caseItem.setStatus(status);
        caseItem.setPenalty(normalize(request == null ? null : request.getPenalty(), "NONE"));
        caseItem.setPenaltyUntil(request == null ? null : request.getPenaltyUntil());
        caseItem.setResolutionNotes(blankToNull(request == null ? null : request.getNotes()));
        FederationDisciplinaryCase saved = caseRepository.save(caseItem);
        enforcePenalty(saved);
        audit("FEDERATION_CASE", saved.getId(), "CASE_" + status, actor, saved.getPenalty());
        return mapCase(saved);
    }

    @Transactional
    public Map<String, Object> createElection(FederationGovernanceRequests.ElectionRequest request, String actor) {
        Long federationId = requiredId(request == null ? null : request.getFederationId(), "Federation ID is required");
        body(federationId);
        FederationElection saved = electionRepository.save(FederationElection.builder()
                .federationId(federationId)
                .title(required(request.getTitle(), "Election title is required"))
                .positionName(required(request.getPositionName(), "Position name is required"))
                .candidateNames(required(request.getCandidateNames(), "Candidates are required"))
                .votingOpensAt(request.getVotingOpensAt())
                .votingClosesAt(request.getVotingClosesAt())
                .build());
        audit("FEDERATION_ELECTION", saved.getId(), "ELECTION_CREATED", actor, saved.getTitle());
        return mapElection(saved);
    }

    @Transactional
    public Map<String, Object> updateElectionStatus(Long id, FederationGovernanceRequests.StatusRequest request, String actor) {
        FederationElection election = election(id);
        String status = normalize(request == null ? null : request.getStatus(), "OPEN");
        if (!ELECTION_STATUSES.contains(status)) {
            throw ApiException.badRequest("Unsupported election status");
        }
        election.setStatus(status);
        FederationElection saved = electionRepository.save(election);
        audit("FEDERATION_ELECTION", saved.getId(), "ELECTION_" + status, actor, request == null ? null : request.getNotes());
        return mapElection(saved);
    }

    @Transactional
    public Map<String, Object> castVote(Long electionId, FederationGovernanceRequests.VoteRequest request, String actor) {
        FederationElection election = election(electionId);
        if (!"OPEN".equalsIgnoreCase(election.getStatus())) {
            throw ApiException.badRequest("Election voting is not open");
        }
        String voterReference = required(request == null ? null : request.getVoterReference(), "Voter reference is required");
        String candidateName = required(request.getCandidateName(), "Candidate name is required");
        if (!containsToken(election.getCandidateNames(), candidateName)) {
            throw ApiException.badRequest("Candidate is not nominated for this election");
        }
        if (voteRepository.existsByElectionIdAndVoterReference(electionId, voterReference)) {
            throw ApiException.conflict("Voter has already cast a vote");
        }
        FederationElectionVote vote = voteRepository.save(FederationElectionVote.builder()
                .electionId(electionId)
                .voterReference(voterReference)
                .candidateName(candidateName)
                .build());
        election.setVoteCount(defaultInt(election.getVoteCount()) + 1);
        electionRepository.save(election);
        audit("FEDERATION_ELECTION_VOTE", vote.getId(), "VOTE_CAST", actor, "Vote captured for election " + electionId);
        return mapVote(vote);
    }

    @Transactional
    public Map<String, Object> publishElectionResults(Long electionId, String actor) {
        FederationElection election = election(electionId);
        List<FederationElectionVote> votes = voteRepository.findByElectionIdOrderByCreatedAtDesc(electionId);
        String winner = votes.stream()
                .collect(Collectors.groupingBy(FederationElectionVote::getCandidateName, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("NO_VOTES");
        election.setWinnerName(winner);
        election.setStatus("RESULT_PUBLISHED");
        FederationElection saved = electionRepository.save(election);
        audit("FEDERATION_ELECTION", saved.getId(), "ELECTION_RESULTS_PUBLISHED", actor, winner);
        return mapElection(saved);
    }

    @Transactional
    public Map<String, Object> upsertRanking(FederationGovernanceRequests.RankingRequest request, String actor) {
        Long federationId = requiredId(request == null ? null : request.getFederationId(), "Federation ID is required");
        body(federationId);
        FederationRanking saved = rankingRepository.save(FederationRanking.builder()
                .federationId(federationId)
                .entityType(normalize(request.getEntityType(), "PLAYER"))
                .entityId(requiredId(request.getEntityId(), "Ranking entity ID is required"))
                .rankingType(normalize(request.getRankingType(), "NATIONAL"))
                .category(defaultString(request.getCategory(), "OPEN"))
                .points(defaultInt(request.getPoints()))
                .calculationNotes(blankToNull(request.getCalculationNotes()))
                .build());
        recomputeRankings(federationId);
        audit("FEDERATION_RANKING", saved.getId(), "RANKING_UPDATED", actor, String.valueOf(saved.getPoints()));
        return mapRanking(saved);
    }

    @Transactional
    public Map<String, Object> openIntegrityAlert(FederationGovernanceRequests.IntegrityAlertRequest request, String actor) {
        Long federationId = requiredId(request == null ? null : request.getFederationId(), "Federation ID is required");
        body(federationId);
        FederationIntegrityAlert saved = integrityAlertRepository.save(FederationIntegrityAlert.builder()
                .federationId(federationId)
                .alertType(required(request.getAlertType(), "Integrity alert type is required"))
                .severity(normalize(request.getSeverity(), "MEDIUM"))
                .subjectReference(required(request.getSubjectReference(), "Subject reference is required"))
                .evidence(blankToNull(request.getEvidence()))
                .riskScore(request.getRiskScore() == null ? 50 : request.getRiskScore())
                .build());
        audit("FEDERATION_INTEGRITY", saved.getId(), "INTEGRITY_ALERT_OPENED", actor, saved.getAlertType());
        return mapIntegrityAlert(saved);
    }

    @Transactional
    public Map<String, Object> updateIntegrityAlertStatus(Long id, FederationGovernanceRequests.StatusRequest request, String actor) {
        FederationIntegrityAlert alert = integrityAlertRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Integrity alert not found"));
        alert.setStatus(normalize(request == null ? null : request.getStatus(), "RESOLVED"));
        FederationIntegrityAlert saved = integrityAlertRepository.save(alert);
        audit("FEDERATION_INTEGRITY", saved.getId(), "INTEGRITY_" + saved.getStatus(), actor, request == null ? null : request.getNotes());
        return mapIntegrityAlert(saved);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> bodies() {
        return bodyRepository.findAllByOrderByCreatedAtDesc().stream().map(this::mapBody).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> franchises() {
        return franchiseRepository.findAllByOrderByCreatedAtDesc().stream().map(this::mapFranchise).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> players() {
        return playerRepository.findAllByOrderByCreatedAtDesc().stream().map(this::mapPlayer).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> drafts() {
        return draftRepository.findAllByOrderByCreatedAtDesc().stream().map(this::mapDraft).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> auctions() {
        return auctionRepository.findAllByOrderByCreatedAtDesc().stream().map(this::mapAuction).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> transfers() {
        return transferRepository.findAllByOrderByCreatedAtDesc().stream().map(this::mapTransfer).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> licenses() {
        return licenseRepository.findAllByOrderByCreatedAtDesc().stream().map(this::mapLicense).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> officials() {
        return officialRepository.findAllByOrderByCreatedAtDesc().stream().map(this::mapOfficial).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> disciplinaryCases() {
        return caseRepository.findAllByOrderByCreatedAtDesc().stream().map(this::mapCase).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> elections() {
        return electionRepository.findAllByOrderByCreatedAtDesc().stream().map(this::mapElection).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> rankings() {
        return rankingRepository.findAllByOrderByPointsDescCreatedAtDesc().stream().map(this::mapRanking).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> integrityAlerts() {
        return integrityAlertRepository.findAllByOrderByCreatedAtDesc().stream().map(this::mapIntegrityAlert).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> audits() {
        return auditRepository.findTop30ByOrderByCreatedAtDesc().stream().map(this::mapAudit).toList();
    }

    private Map<String, Object> kpis(List<FederationBody> bodies,
                                    List<FederationFranchise> franchises,
                                    List<FederationPlayerRegistration> players,
                                    List<FederationDraft> drafts,
                                    List<FederationAuction> auctions,
                                    List<FederationTransfer> transfers,
                                    List<FederationLicense> licenses,
                                    List<FederationOfficial> officials,
                                    List<FederationDisciplinaryCase> cases,
                                    List<FederationElection> elections,
                                    List<FederationIntegrityAlert> alerts) {
        Map<String, Object> kpis = new LinkedHashMap<>();
        kpis.put("federations", countBy(bodies, item -> "NATIONAL_FEDERATION".equalsIgnoreCase(item.getBodyType())));
        kpis.put("associations", countBy(bodies, item -> !"NATIONAL_FEDERATION".equalsIgnoreCase(item.getBodyType())));
        kpis.put("franchises", franchises.size());
        kpis.put("registeredPlayers", players.size());
        kpis.put("verifiedPlayers", countBy(players, item -> "VERIFIED".equalsIgnoreCase(item.getVerificationStatus())));
        kpis.put("activeDrafts", countBy(drafts, item -> Set.of("PUBLISHED", "IN_PROGRESS").contains(item.getStatus())));
        kpis.put("liveAuctions", countBy(auctions, item -> "LIVE".equalsIgnoreCase(item.getStatus())));
        kpis.put("pendingTransfers", countBy(transfers, item -> "REQUESTED".equalsIgnoreCase(item.getStatus())));
        kpis.put("activeLicenses", countBy(licenses, item -> "ACTIVE".equalsIgnoreCase(item.getStatus())));
        kpis.put("verifiedOfficials", countBy(officials, item -> "VERIFIED".equalsIgnoreCase(item.getStatus())));
        kpis.put("openCases", countBy(cases, item -> !"CLOSED".equalsIgnoreCase(item.getStatus())));
        kpis.put("openElections", countBy(elections, item -> "OPEN".equalsIgnoreCase(item.getStatus())));
        kpis.put("openIntegrityAlerts", countBy(alerts, item -> "OPEN".equalsIgnoreCase(item.getStatus())));
        return kpis;
    }

    private Map<String, Object> analytics(List<FederationBody> bodies,
                                          List<FederationPlayerRegistration> players,
                                          List<FederationFranchise> franchises,
                                          List<FederationTransfer> transfers,
                                          List<FederationIntegrityAlert> alerts) {
        Map<String, Object> analytics = new LinkedHashMap<>();
        analytics.put("bodyTypes", bodies.stream().collect(Collectors.groupingBy(FederationBody::getBodyType, TreeMap::new, Collectors.counting())));
        analytics.put("playersBySport", players.stream().collect(Collectors.groupingBy(FederationPlayerRegistration::getSport, TreeMap::new, Collectors.counting())));
        analytics.put("franchisesByCity", franchises.stream().collect(Collectors.groupingBy(item -> defaultString(item.getCity(), "Unassigned"), TreeMap::new, Collectors.counting())));
        analytics.put("transferPipeline", transfers.stream().collect(Collectors.groupingBy(FederationTransfer::getStatus, TreeMap::new, Collectors.counting())));
        analytics.put("integrityRiskScore", alerts.stream().mapToInt(item -> defaultInt(item.getRiskScore())).average().orElse(0));
        analytics.put("nationalReadiness", bodies.isEmpty() ? "SETUP_REQUIRED" : "FOUNDATION_READY");
        return analytics;
    }

    private List<Map<String, Object>> complianceControls(List<FederationPlayerRegistration> players,
                                                          List<FederationFranchise> franchises,
                                                          List<FederationLicense> licenses,
                                                          List<FederationDisciplinaryCase> cases,
                                                          List<FederationIntegrityAlert> alerts) {
        List<Map<String, Object>> controls = new ArrayList<>();
        controls.add(control("Central player IDs", players.size(), countBy(players, item -> item.getSportsId() != null), "Sports ID assigned for every registry record"));
        controls.add(control("Franchise salary caps", franchises.size(), countBy(franchises, item -> defaultMoney(item.getSalaryCap()).compareTo(defaultMoney(item.getUsedBudget())) >= 0), "Budget controls and roster spend tracked"));
        controls.add(control("License lifecycle", licenses.size(), countBy(licenses, item -> item.getValidUntil() != null && !item.getValidUntil().isBefore(LocalDate.now())), "Expired licenses require renewal"));
        controls.add(control("Disciplinary enforcement", cases.size(), countBy(cases, item -> !"OPEN".equalsIgnoreCase(item.getStatus())), "Open cases need investigator action"));
        controls.add(control("Integrity monitoring", alerts.size(), countBy(alerts, item -> !"OPEN".equalsIgnoreCase(item.getStatus())), "Open alerts require review"));
        return controls;
    }

    private Map<String, Object> crossEpicSignals() {
        Map<String, Object> signals = new LinkedHashMap<>();
        signals.put("careerProfiles", careerProfileRepository.count());
        signals.put("tournaments", tournamentRepository.count());
        signals.put("corporateLeagues", corporateLeagueRepository.count());
        signals.put("integrationNotes", List.of("Career profiles can seed central sports IDs", "Tournament and corporate league outcomes can feed national rankings", "Governance audit remains separate from platform-wide audit streams"));
        return signals;
    }

    private List<Map<String, Object>> priorityActions(List<FederationBody> bodies,
                                                       List<FederationPlayerRegistration> players,
                                                       List<FederationDraft> drafts,
                                                       List<FederationAuction> auctions,
                                                       List<FederationTransfer> transfers,
                                                       List<FederationDisciplinaryCase> cases,
                                                       List<FederationElection> elections,
                                                       List<FederationIntegrityAlert> alerts) {
        List<Map<String, Object>> actions = new ArrayList<>();
        if (bodies.isEmpty()) {
            actions.add(action("Create national federation", "No federation hierarchy exists yet", "HIGH"));
        }
        long pendingPlayers = countBy(players, item -> "PENDING".equalsIgnoreCase(item.getVerificationStatus()));
        if (pendingPlayers > 0) {
            actions.add(action("Verify central player registrations", pendingPlayers + " players pending identity approval", "HIGH"));
        }
        long liveAuctions = countBy(auctions, item -> "LIVE".equalsIgnoreCase(item.getStatus()));
        if (liveAuctions > 0) {
            actions.add(action("Monitor live auctions", liveAuctions + " auctions have active bid state", "HIGH"));
        }
        long pendingTransfers = countBy(transfers, item -> "REQUESTED".equalsIgnoreCase(item.getStatus()));
        if (pendingTransfers > 0) {
            actions.add(action("Review transfer requests", pendingTransfers + " transfers need governance approval", "MEDIUM"));
        }
        long openCases = countBy(cases, item -> !"CLOSED".equalsIgnoreCase(item.getStatus()));
        if (openCases > 0) {
            actions.add(action("Resolve disciplinary workflow", openCases + " cases open", "HIGH"));
        }
        long openElections = countBy(elections, item -> "OPEN".equalsIgnoreCase(item.getStatus()));
        if (openElections > 0) {
            actions.add(action("Supervise federation elections", openElections + " elections accepting votes", "HIGH"));
        }
        long openAlerts = countBy(alerts, item -> "OPEN".equalsIgnoreCase(item.getStatus()));
        if (openAlerts > 0) {
            actions.add(action("Investigate integrity alerts", openAlerts + " alerts open", "HIGH"));
        }
        long activeDrafts = countBy(drafts, item -> Set.of("PUBLISHED", "IN_PROGRESS").contains(item.getStatus()));
        if (activeDrafts > 0) {
            actions.add(action("Track draft completion", activeDrafts + " drafts in allocation flow", "MEDIUM"));
        }
        return actions;
    }

    private Map<String, Object> control(String title, long total, long passed, String note) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("title", title);
        map.put("total", total);
        map.put("passed", passed);
        map.put("status", total == 0 ? "NO_DATA" : total == passed ? "PASS" : "ATTENTION");
        map.put("note", note);
        return map;
    }

    private Map<String, Object> action(String title, String message, String severity) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("title", title);
        map.put("message", message);
        map.put("severity", severity);
        return map;
    }

    private FederationBody body(Long id) {
        return bodyRepository.findById(id).orElseThrow(() -> ApiException.notFound("Federation body not found"));
    }

    private FederationFranchise franchise(Long id) {
        return franchiseRepository.findById(id).orElseThrow(() -> ApiException.notFound("Franchise not found"));
    }

    private FederationPlayerRegistration player(Long id) {
        return playerRepository.findById(id).orElseThrow(() -> ApiException.notFound("Central player registration not found"));
    }

    private FederationDraft draft(Long id) {
        return draftRepository.findById(id).orElseThrow(() -> ApiException.notFound("Draft not found"));
    }

    private FederationAuction auction(Long id) {
        return auctionRepository.findById(id).orElseThrow(() -> ApiException.notFound("Auction not found"));
    }

    private FederationElection election(Long id) {
        return electionRepository.findById(id).orElseThrow(() -> ApiException.notFound("Election not found"));
    }

    private void adjustRoster(Long franchiseId, int delta) {
        if (franchiseId == null) {
            return;
        }
        franchiseRepository.findById(franchiseId).ifPresent(franchise -> {
            franchise.setRosterSize(Math.max(0, defaultInt(franchise.getRosterSize()) + delta));
            franchiseRepository.save(franchise);
        });
    }

    private void enforcePenalty(FederationDisciplinaryCase caseItem) {
        if (!"CLOSED".equalsIgnoreCase(caseItem.getStatus()) || "NONE".equalsIgnoreCase(caseItem.getPenalty())) {
            return;
        }
        if ("PLAYER".equalsIgnoreCase(caseItem.getSubjectType())) {
            playerRepository.findById(caseItem.getSubjectId()).ifPresent(player -> {
                player.setStatus("BAN".equalsIgnoreCase(caseItem.getPenalty()) ? "BANNED" : "SUSPENDED");
                playerRepository.save(player);
            });
        }
        if ("FRANCHISE".equalsIgnoreCase(caseItem.getSubjectType())) {
            franchiseRepository.findById(caseItem.getSubjectId()).ifPresent(franchise -> {
                franchise.setStatus("SUSPENDED");
                franchiseRepository.save(franchise);
            });
        }
    }

    private void recomputeRankings(Long federationId) {
        List<FederationRanking> rankings = rankingRepository.findByFederationIdOrderByPointsDescCreatedAtDesc(federationId);
        int position = 1;
        for (FederationRanking ranking : rankings) {
            ranking.setRankPosition(position++);
            rankingRepository.save(ranking);
        }
    }

    private void audit(String entityType, Long entityId, String action, String actor, String details) {
        auditRepository.save(FederationGovernanceAudit.builder()
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .actor(defaultString(actor, "system"))
                .details(blankToNull(details))
                .build());
    }

    private String sportsId() {
        String id;
        do {
            id = "TSI-" + LocalDate.now().getYear() + "-" + (100000 + RANDOM.nextInt(900000));
        } while (playerRepository.existsBySportsId(id));
        return id;
    }

    private String licenseNumber() {
        String number;
        do {
            number = "LIC-" + LocalDate.now().getYear() + "-" + (100000 + RANDOM.nextInt(900000));
        } while (licenseRepository.existsByLicenseNumber(number));
        return number;
    }

    private int complianceScore(String bodyType, String registrationNumber, String contactEmail) {
        int score = "NATIONAL_FEDERATION".equalsIgnoreCase(bodyType) ? 70 : 60;
        if (registrationNumber != null && !registrationNumber.isBlank()) {
            score += 15;
        }
        if (contactEmail != null && contactEmail.contains("@")) {
            score += 10;
        }
        return Math.min(score, 100);
    }

    private int eligibilityScore(FederationGovernanceRequests.PlayerRegistrationRequest request, CareerProfile careerProfile) {
        int score = careerProfile == null ? 55 : Math.min(85, defaultInt(careerProfile.getRankingScore()) / 2 + 55);
        if (request.getIdentityDocumentUrl() != null && !request.getIdentityDocumentUrl().isBlank()) {
            score += 15;
        }
        if (request.getDateOfBirth() != null) {
            score += 5;
        }
        return Math.min(score, 100);
    }

    private Map<String, Object> mapBody(FederationBody body) {
        Map<String, Object> map = base(body.getId(), body.getStatus(), body.getCreatedAt());
        map.put("parentBodyId", body.getParentBodyId());
        map.put("name", body.getName());
        map.put("bodyType", body.getBodyType());
        map.put("sport", body.getSport());
        map.put("region", body.getRegion());
        map.put("registrationNumber", body.getRegistrationNumber());
        map.put("website", body.getWebsite());
        map.put("contactEmail", body.getContactEmail());
        map.put("affiliatedMembers", body.getAffiliatedMembers());
        map.put("complianceScore", body.getComplianceScore());
        map.put("governanceNotes", body.getGovernanceNotes());
        return map;
    }

    private Map<String, Object> mapFranchise(FederationFranchise franchise) {
        Map<String, Object> map = base(franchise.getId(), franchise.getStatus(), franchise.getCreatedAt());
        map.put("federationId", franchise.getFederationId());
        map.put("name", franchise.getName());
        map.put("ownerName", franchise.getOwnerName());
        map.put("city", franchise.getCity());
        map.put("homeVenue", franchise.getHomeVenue());
        map.put("titlesWon", franchise.getTitlesWon());
        map.put("rosterSize", franchise.getRosterSize());
        map.put("salaryCap", franchise.getSalaryCap());
        map.put("usedBudget", franchise.getUsedBudget());
        map.put("ownershipHistory", franchise.getOwnershipHistory());
        return map;
    }

    private Map<String, Object> mapPlayer(FederationPlayerRegistration player) {
        Map<String, Object> map = base(player.getId(), player.getStatus(), player.getCreatedAt());
        map.put("federationId", player.getFederationId());
        map.put("careerProfileId", player.getCareerProfileId());
        map.put("sportsId", player.getSportsId());
        map.put("playerName", player.getPlayerName());
        map.put("email", player.getEmail());
        map.put("sport", player.getSport());
        map.put("city", player.getCity());
        map.put("ageGroup", player.getAgeGroup());
        map.put("dateOfBirth", player.getDateOfBirth());
        map.put("identityDocumentUrl", player.getIdentityDocumentUrl());
        map.put("currentFranchiseId", player.getCurrentFranchiseId());
        map.put("verificationStatus", player.getVerificationStatus());
        map.put("eligibilityScore", player.getEligibilityScore());
        map.put("rankingScore", player.getRankingScore());
        return map;
    }

    private Map<String, Object> mapDraft(FederationDraft draft) {
        Map<String, Object> map = base(draft.getId(), draft.getStatus(), draft.getCreatedAt());
        map.put("federationId", draft.getFederationId());
        map.put("name", draft.getName());
        map.put("sport", draft.getSport());
        map.put("season", draft.getSeason());
        map.put("draftOrderFranchiseIds", draft.getDraftOrderFranchiseIds());
        map.put("poolSize", draft.getPoolSize());
        map.put("completedPicks", draft.getCompletedPicks());
        map.put("startsAt", draft.getStartsAt());
        return map;
    }

    private Map<String, Object> mapDraftSelection(FederationDraftSelection selection) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", selection.getId());
        map.put("draftId", selection.getDraftId());
        map.put("franchiseId", selection.getFranchiseId());
        map.put("playerRegistrationId", selection.getPlayerRegistrationId());
        map.put("pickNumber", selection.getPickNumber());
        map.put("selectionType", selection.getSelectionType());
        map.put("selectedBy", selection.getSelectedBy());
        map.put("selectedAt", selection.getSelectedAt());
        return map;
    }

    private Map<String, Object> mapAuction(FederationAuction auction) {
        Map<String, Object> map = base(auction.getId(), auction.getStatus(), auction.getCreatedAt());
        map.put("federationId", auction.getFederationId());
        map.put("playerRegistrationId", auction.getPlayerRegistrationId());
        map.put("auctionName", auction.getAuctionName());
        map.put("basePrice", auction.getBasePrice());
        map.put("currentBid", auction.getCurrentBid());
        map.put("highestBidFranchiseId", auction.getHighestBidFranchiseId());
        map.put("bidCount", auction.getBidCount());
        map.put("startsAt", auction.getStartsAt());
        map.put("endsAt", auction.getEndsAt());
        return map;
    }

    private Map<String, Object> mapAuctionBid(FederationAuctionBid bid) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", bid.getId());
        map.put("auctionId", bid.getAuctionId());
        map.put("franchiseId", bid.getFranchiseId());
        map.put("bidAmount", bid.getBidAmount());
        map.put("bidderName", bid.getBidderName());
        map.put("createdAt", bid.getCreatedAt());
        return map;
    }

    private Map<String, Object> mapTransfer(FederationTransfer transfer) {
        Map<String, Object> map = base(transfer.getId(), transfer.getStatus(), transfer.getCreatedAt());
        map.put("federationId", transfer.getFederationId());
        map.put("playerRegistrationId", transfer.getPlayerRegistrationId());
        map.put("fromFranchiseId", transfer.getFromFranchiseId());
        map.put("toFranchiseId", transfer.getToFranchiseId());
        map.put("transferType", transfer.getTransferType());
        map.put("effectiveFrom", transfer.getEffectiveFrom());
        map.put("effectiveTo", transfer.getEffectiveTo());
        map.put("reason", transfer.getReason());
        map.put("reviewedBy", transfer.getReviewedBy());
        map.put("reviewedAt", transfer.getReviewedAt());
        return map;
    }

    private Map<String, Object> mapLicense(FederationLicense license) {
        Map<String, Object> map = base(license.getId(), license.getStatus(), license.getCreatedAt());
        map.put("federationId", license.getFederationId());
        map.put("holderType", license.getHolderType());
        map.put("holderId", license.getHolderId());
        map.put("licenseNumber", license.getLicenseNumber());
        map.put("licenseType", license.getLicenseType());
        map.put("validFrom", license.getValidFrom());
        map.put("validUntil", license.getValidUntil());
        return map;
    }

    private Map<String, Object> mapOfficial(FederationOfficial official) {
        Map<String, Object> map = base(official.getId(), official.getStatus(), official.getCreatedAt());
        map.put("federationId", official.getFederationId());
        map.put("name", official.getName());
        map.put("email", official.getEmail());
        map.put("officialType", official.getOfficialType());
        map.put("certificationUrl", official.getCertificationUrl());
        map.put("certificationValidUntil", official.getCertificationValidUntil());
        map.put("available", official.getAvailable());
        return map;
    }

    private Map<String, Object> mapCase(FederationDisciplinaryCase caseItem) {
        Map<String, Object> map = base(caseItem.getId(), caseItem.getStatus(), caseItem.getCreatedAt());
        map.put("federationId", caseItem.getFederationId());
        map.put("subjectType", caseItem.getSubjectType());
        map.put("subjectId", caseItem.getSubjectId());
        map.put("caseType", caseItem.getCaseType());
        map.put("severity", caseItem.getSeverity());
        map.put("investigator", caseItem.getInvestigator());
        map.put("evidenceSummary", caseItem.getEvidenceSummary());
        map.put("resolutionNotes", caseItem.getResolutionNotes());
        map.put("penalty", caseItem.getPenalty());
        map.put("penaltyUntil", caseItem.getPenaltyUntil());
        return map;
    }

    private Map<String, Object> mapElection(FederationElection election) {
        Map<String, Object> map = base(election.getId(), election.getStatus(), election.getCreatedAt());
        map.put("federationId", election.getFederationId());
        map.put("title", election.getTitle());
        map.put("positionName", election.getPositionName());
        map.put("candidateNames", election.getCandidateNames());
        map.put("votingOpensAt", election.getVotingOpensAt());
        map.put("votingClosesAt", election.getVotingClosesAt());
        map.put("winnerName", election.getWinnerName());
        map.put("voteCount", election.getVoteCount());
        return map;
    }

    private Map<String, Object> mapVote(FederationElectionVote vote) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", vote.getId());
        map.put("electionId", vote.getElectionId());
        map.put("voterReference", vote.getVoterReference());
        map.put("candidateName", vote.getCandidateName());
        map.put("createdAt", vote.getCreatedAt());
        return map;
    }

    private Map<String, Object> mapRanking(FederationRanking ranking) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", ranking.getId());
        map.put("federationId", ranking.getFederationId());
        map.put("entityType", ranking.getEntityType());
        map.put("entityId", ranking.getEntityId());
        map.put("rankingType", ranking.getRankingType());
        map.put("category", ranking.getCategory());
        map.put("points", ranking.getPoints());
        map.put("rankPosition", ranking.getRankPosition());
        map.put("calculationNotes", ranking.getCalculationNotes());
        map.put("createdAt", ranking.getCreatedAt());
        return map;
    }

    private Map<String, Object> mapIntegrityAlert(FederationIntegrityAlert alert) {
        Map<String, Object> map = base(alert.getId(), alert.getStatus(), alert.getCreatedAt());
        map.put("federationId", alert.getFederationId());
        map.put("alertType", alert.getAlertType());
        map.put("severity", alert.getSeverity());
        map.put("subjectReference", alert.getSubjectReference());
        map.put("evidence", alert.getEvidence());
        map.put("riskScore", alert.getRiskScore());
        return map;
    }

    private Map<String, Object> mapAudit(FederationGovernanceAudit audit) {
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

    private Map<String, Object> base(Long id, String status, LocalDateTime createdAt) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("status", status);
        map.put("createdAt", createdAt);
        return map;
    }

    private boolean matches(String filter, String value) {
        return filter == null || filter.isBlank() || (value != null && value.toLowerCase(Locale.ROOT).contains(filter.toLowerCase(Locale.ROOT)));
    }

    private boolean containsToken(String csv, String value) {
        if (csv == null || value == null) {
            return false;
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .anyMatch(candidate -> candidate.equalsIgnoreCase(value.trim()));
    }

    private String required(String value, String message) {
        if (value == null || value.isBlank()) {
            throw ApiException.badRequest(message);
        }
        return value.trim();
    }

    private Long requiredId(Long value, String message) {
        if (value == null || value <= 0) {
            throw ApiException.badRequest(message);
        }
        return value;
    }

    private String normalize(String value, String fallback) {
        String selected = value == null || value.isBlank() ? fallback : value;
        return selected.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String validateOptionalUrl(String value, String label) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            throw ApiException.badRequest(label + " URL must start with http:// or https://");
        }
        return trimmed;
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private BigDecimal defaultMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private <T> long countBy(List<T> items, java.util.function.Predicate<T> predicate) {
        return items.stream().filter(predicate).count();
    }
}
