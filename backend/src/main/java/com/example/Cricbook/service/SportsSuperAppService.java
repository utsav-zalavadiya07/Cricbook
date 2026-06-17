package com.example.Cricbook.service;

import com.example.Cricbook.dto.SportsSuperAppRequests;
import com.example.Cricbook.entity.*;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SportsSuperAppService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Set<String> VISIBILITY_VALUES = Set.of("PUBLIC", "PRIVATE", "FAMILY", "SCOUTS_ONLY");
    private static final Set<String> MEMBERSHIP_TIERS = Set.of("FREE", "BRONZE", "SILVER", "GOLD", "PLATINUM", "ENTERPRISE");
    private static final BigDecimal MAX_WALLET_EVENT = new BigDecimal("500000");

    private final SportsIdentityRepository identityRepository;
    private final SportsPassportRepository passportRepository;
    private final UniversalWalletAccountRepository walletRepository;
    private final UniversalWalletLedgerRepository walletLedgerRepository;
    private final UnifiedMembershipRepository membershipRepository;
    private final UniversalRewardAccountRepository rewardAccountRepository;
    private final UniversalRewardLedgerRepository rewardLedgerRepository;
    private final SportsReputationEventRepository reputationEventRepository;
    private final SportsTimelineEventRepository timelineEventRepository;
    private final FamilyAccountRepository familyAccountRepository;
    private final SportsCreditLedgerRepository creditLedgerRepository;
    private final SuperAppAuditRepository auditRepository;
    private final BookingRepository bookingRepository;
    private final TournamentRegistrationRepository tournamentRegistrationRepository;
    private final CareerProfileRepository careerProfileRepository;
    private final FederationPlayerRegistrationRepository federationPlayerRegistrationRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> dashboard(String actor) {
        List<SportsIdentity> identities = identityRepository.findAllByOrderByCreatedAtDesc();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("kpis", kpis(identities));
        response.put("myIdentity", actor == null ? null : identityRepository.findByPrimaryEmailIgnoreCase(actor).map(this::mapIdentity).orElse(null));
        response.put("identities", identities.stream().limit(12).map(this::mapIdentity).toList());
        response.put("wallets", walletRepository.findAllByOrderByCreatedAtDesc().stream().limit(12).map(this::mapWallet).toList());
        response.put("memberships", membershipRepository.findAllByOrderByCreatedAtDesc().stream().limit(12).map(this::mapMembership).toList());
        response.put("rewards", rewardAccountRepository.findAllByOrderByCreatedAtDesc().stream().limit(12).map(this::mapRewardAccount).toList());
        response.put("timeline", timelineEventRepository.findAllByOrderByOccurredAtDesc().stream().limit(15).map(this::mapTimeline).toList());
        response.put("reputation", reputationEventRepository.findAllByOrderByCreatedAtDesc().stream().limit(15).map(this::mapReputation).toList());
        response.put("familyAccounts", familyAccountRepository.findAllByOrderByCreatedAtDesc().stream().limit(12).map(this::mapFamily).toList());
        response.put("crossModuleSignals", crossModuleSignals());
        response.put("priorityControls", priorityControls(identities));
        response.put("generatedAt", LocalDateTime.now());
        return response;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> adminDashboard() {
        Map<String, Object> response = dashboard("admin");
        response.put("audits", audits());
        response.put("walletLedger", walletLedgerRepository.findAllByOrderByCreatedAtDesc().stream().limit(25).map(this::mapWalletLedger).toList());
        response.put("rewardLedger", rewardLedgerRepository.findAllByOrderByCreatedAtDesc().stream().limit(25).map(this::mapRewardLedger).toList());
        response.put("creditLedger", creditLedgerRepository.findAllByOrderByCreatedAtDesc().stream().limit(25).map(this::mapCreditLedger).toList());
        response.put("governanceControls", governanceControls());
        return response;
    }

    @Transactional
    public Map<String, Object> createIdentity(SportsSuperAppRequests.IdentityRequest request, String actor) {
        String email = required(request == null ? null : request.getPrimaryEmail(), "Primary email is required").toLowerCase(Locale.ROOT);
        if (identityRepository.existsByPrimaryEmailIgnoreCase(email)) {
            throw ApiException.conflict("Sports identity already exists for this email");
        }
        String displayName = required(request.getDisplayName(), "Display name is required");
        String ageGroup = normalize(request.getAgeGroup(), "OPEN");
        String guardianEmail = blankToNull(request.getGuardianEmail());
        if (isYouth(ageGroup) && guardianEmail == null) {
            throw ApiException.badRequest("Guardian email is required for youth sports identities");
        }
        String visibility = normalize(request.getProfileVisibility(), "PUBLIC");
        if (!VISIBILITY_VALUES.contains(visibility)) {
            throw ApiException.badRequest("Unsupported profile visibility");
        }

        SportsIdentity identity = SportsIdentity.builder()
                .userId(request.getUserId())
                .sportsId(generateSportsId())
                .primaryEmail(email)
                .displayName(displayName)
                .phone(blankToNull(request.getPhone()))
                .city(blankToNull(request.getCity()))
                .primarySport(defaultString(request.getPrimarySport(), "CRICKET").toUpperCase(Locale.ROOT))
                .ageGroup(ageGroup)
                .guardianEmail(guardianEmail)
                .profileVisibility(visibility)
                .identityScore(identityScore(request))
                .linkedModules(defaultLinkedModules(request.getLinkedModules()))
                .build();
        SportsIdentity saved = identityRepository.save(identity);
        getOrCreateWallet(saved.getId());
        getOrCreateRewardAccount(saved.getId());
        audit("SPORTS_IDENTITY", saved.getId(), "IDENTITY_CREATED", actor, "Sports ID generated: " + saved.getSportsId());
        return mapIdentity(saved);
    }

    @Transactional
    public Map<String, Object> generatePassport(Long identityId, SportsSuperAppRequests.PassportRequest request, String actor) {
        SportsIdentity identity = identity(identityId);
        String visibility = normalize(request == null ? null : request.getVisibility(), identity.getProfileVisibility());
        if (!VISIBILITY_VALUES.contains(visibility)) {
            throw ApiException.badRequest("Unsupported passport visibility");
        }
        SportsPassport passport = passportRepository.findByIdentityId(identityId)
                .orElseGet(() -> SportsPassport.builder()
                        .identityId(identityId)
                        .shareCode(generateShareCode())
                        .build());
        passport.setVisibility(visibility);
        passport.setExpiresAt(request == null ? null : request.getExpiresAt());
        passport.setQrPayload("trufsports://passport/" + passport.getShareCode());
        passport.setStatisticsSnapshot(statisticsSnapshot(identity));
        passport.setAchievementsSnapshot(achievementSnapshot(identity));
        passport.setMembershipSnapshot(membershipRepository.findFirstByIdentityIdAndStatusOrderByCreatedAtDesc(identityId, "ACTIVE")
                .map(membership -> membership.getTier() + " until " + membership.getExpiresAt())
                .orElse("No active unified membership"));
        passport.setReputationSnapshot("Reputation " + identity.getReputationScore() + "/100");
        SportsPassport saved = passportRepository.save(passport);
        audit("SPORTS_PASSPORT", saved.getId(), "PASSPORT_REFRESHED", actor, saved.getShareCode());
        return mapPassport(saved, identity);
    }

    @Transactional
    public Map<String, Object> publicPassport(String shareCode) {
        SportsPassport passport = passportRepository.findByShareCode(required(shareCode, "Share code is required"))
                .orElseThrow(() -> ApiException.notFound("Sports passport not found"));
        if (!"PUBLIC".equalsIgnoreCase(passport.getVisibility())) {
            throw ApiException.forbidden("Sports passport is not public");
        }
        if (passport.getExpiresAt() != null && passport.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw ApiException.forbidden("Sports passport link has expired");
        }
        passport.setViewCount(defaultInt(passport.getViewCount()) + 1);
        SportsPassport saved = passportRepository.save(passport);
        return mapPassport(saved, identity(saved.getIdentityId()));
    }

    @Transactional
    public Map<String, Object> creditWallet(Long identityId, SportsSuperAppRequests.WalletLedgerRequest request, String actor) {
        SportsIdentity identity = identity(identityId);
        BigDecimal amount = walletAmount(request == null ? null : request.getAmount());
        UniversalWalletAccount wallet = getOrCreateWallet(identityId);
        wallet.setBalance(wallet.getBalance().add(amount));
        UniversalWalletAccount saved = walletRepository.save(wallet);
        UniversalWalletLedger ledger = walletLedgerRepository.save(UniversalWalletLedger.builder()
                .walletId(saved.getId())
                .identityId(identityId)
                .transactionType("CREDIT")
                .sourceModule(defaultString(request.getSourceModule(), "SUPER_APP"))
                .amount(amount)
                .balanceAfter(saved.getBalance())
                .referenceType(blankToNull(request.getReferenceType()))
                .referenceId(blankToNull(request.getReferenceId()))
                .expiresAt(request.getExpiresAt())
                .note(blankToNull(request.getNote()))
                .build());
        audit("UNIVERSAL_WALLET", saved.getId(), "WALLET_CREDITED", actor, identity.getSportsId() + " credited " + amount);
        return walletResponse(saved, ledger);
    }

    @Transactional
    public Map<String, Object> debitWallet(Long identityId, SportsSuperAppRequests.WalletLedgerRequest request, String actor) {
        if (request == null) {
            throw ApiException.badRequest("Wallet debit request is required");
        }
        identity(identityId);
        BigDecimal amount = walletAmount(request == null ? null : request.getAmount());
        UniversalWalletAccount wallet = getOrCreateWallet(identityId);
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw ApiException.badRequest("Insufficient universal wallet balance");
        }
        wallet.setBalance(wallet.getBalance().subtract(amount));
        UniversalWalletAccount saved = walletRepository.save(wallet);
        UniversalWalletLedger ledger = walletLedgerRepository.save(UniversalWalletLedger.builder()
                .walletId(saved.getId())
                .identityId(identityId)
                .transactionType("DEBIT")
                .sourceModule(defaultString(request.getSourceModule(), "SUPER_APP"))
                .amount(amount.negate())
                .balanceAfter(saved.getBalance())
                .referenceType(blankToNull(request.getReferenceType()))
                .referenceId(blankToNull(request.getReferenceId()))
                .note(blankToNull(request.getNote()))
                .build());
        audit("UNIVERSAL_WALLET", saved.getId(), "WALLET_DEBITED", actor, "Debit " + amount);
        return walletResponse(saved, ledger);
    }

    @Transactional
    public Map<String, Object> transferWallet(Long fromIdentityId, SportsSuperAppRequests.WalletTransferRequest request, String actor) {
        if (request == null) {
            throw ApiException.badRequest("Wallet transfer request is required");
        }
        Long toIdentityId = requiredId(request == null ? null : request.getToIdentityId(), "Target Sports Identity ID is required");
        if (fromIdentityId.equals(toIdentityId)) {
            throw ApiException.badRequest("Cannot transfer wallet balance to the same Sports Identity");
        }
        identity(fromIdentityId);
        identity(toIdentityId);
        BigDecimal amount = walletAmount(request.getAmount());
        UniversalWalletAccount fromWallet = getOrCreateWallet(fromIdentityId);
        UniversalWalletAccount toWallet = getOrCreateWallet(toIdentityId);
        if (fromWallet.getBalance().compareTo(amount) < 0) {
            throw ApiException.badRequest("Insufficient universal wallet balance");
        }
        fromWallet.setBalance(fromWallet.getBalance().subtract(amount));
        toWallet.setBalance(toWallet.getBalance().add(amount));
        walletRepository.save(fromWallet);
        walletRepository.save(toWallet);

        String sourceModule = defaultString(request.getSourceModule(), "SUPER_APP_TRANSFER");
        walletLedgerRepository.save(UniversalWalletLedger.builder()
                .walletId(fromWallet.getId())
                .identityId(fromIdentityId)
                .transactionType("TRANSFER_OUT")
                .sourceModule(sourceModule)
                .amount(amount.negate())
                .balanceAfter(fromWallet.getBalance())
                .referenceType(blankToNull(request.getReferenceType()))
                .referenceId(blankToNull(request.getReferenceId()))
                .note(blankToNull(request.getNote()))
                .build());
        UniversalWalletLedger inbound = walletLedgerRepository.save(UniversalWalletLedger.builder()
                .walletId(toWallet.getId())
                .identityId(toIdentityId)
                .transactionType("TRANSFER_IN")
                .sourceModule(sourceModule)
                .amount(amount)
                .balanceAfter(toWallet.getBalance())
                .referenceType(blankToNull(request.getReferenceType()))
                .referenceId(blankToNull(request.getReferenceId()))
                .note(blankToNull(request.getNote()))
                .build());
        audit("UNIVERSAL_WALLET", fromWallet.getId(), "WALLET_TRANSFERRED", actor, "Transfer " + amount + " to identity " + toIdentityId);
        return Map.of("fromWallet", mapWallet(fromWallet), "toWallet", mapWallet(toWallet), "ledger", mapWalletLedger(inbound));
    }

    @Transactional
    public Map<String, Object> createMembership(Long identityId, SportsSuperAppRequests.MembershipRequest request, String actor) {
        if (request == null) {
            throw ApiException.badRequest("Membership request is required");
        }
        identity(identityId);
        String tier = normalize(request == null ? null : request.getTier(), "BRONZE");
        if (!MEMBERSHIP_TIERS.contains(tier)) {
            throw ApiException.badRequest("Unsupported unified membership tier");
        }
        LocalDateTime startsAt = request.getStartsAt() == null ? LocalDateTime.now() : request.getStartsAt();
        LocalDateTime expiresAt = request.getExpiresAt() == null ? startsAt.plusMonths(1) : request.getExpiresAt();
        if (!expiresAt.isAfter(startsAt)) {
            throw ApiException.badRequest("Membership expiry must be after start date");
        }
        UnifiedMembership saved = membershipRepository.save(UnifiedMembership.builder()
                .identityId(identityId)
                .tier(tier)
                .billingCycle(normalize(request.getBillingCycle(), "MONTHLY"))
                .price(defaultMoney(request.getPrice()))
                .startsAt(startsAt)
                .expiresAt(expiresAt)
                .autoRenew(Boolean.TRUE.equals(request.getAutoRenew()))
                .benefitsSummary(defaultString(request.getBenefitsSummary(), "Unified access across bookings, rewards, tournaments, academy, and marketplace."))
                .build());
        audit("UNIFIED_MEMBERSHIP", saved.getId(), "MEMBERSHIP_CREATED", actor, tier);
        return mapMembership(saved);
    }

    @Transactional
    public Map<String, Object> earnRewards(Long identityId, SportsSuperAppRequests.RewardRequest request, String actor) {
        identity(identityId);
        int points = positivePoints(request == null ? null : request.getPoints());
        UniversalRewardAccount account = getOrCreateRewardAccount(identityId);
        account.setPointsBalance(account.getPointsBalance() + points);
        account.setLifetimeEarned(account.getLifetimeEarned() + points);
        UniversalRewardAccount saved = rewardAccountRepository.save(account);
        UniversalRewardLedger ledger = rewardLedgerRepository.save(UniversalRewardLedger.builder()
                .accountId(saved.getId())
                .identityId(identityId)
                .eventType("EARN")
                .sourceModule(defaultString(request.getSourceModule(), "SUPER_APP"))
                .points(points)
                .balanceAfter(saved.getPointsBalance())
                .expiresAt(request.getExpiresAt())
                .note(blankToNull(request.getNote()))
                .build());
        audit("UNIVERSAL_REWARD", saved.getId(), "REWARD_EARNED", actor, points + " points");
        return rewardResponse(saved, ledger);
    }

    @Transactional
    public Map<String, Object> redeemRewards(Long identityId, SportsSuperAppRequests.RewardRequest request, String actor) {
        identity(identityId);
        int points = positivePoints(request == null ? null : request.getPoints());
        UniversalRewardAccount account = getOrCreateRewardAccount(identityId);
        if (account.getPointsBalance() < points) {
            throw ApiException.badRequest("Insufficient universal rewards balance");
        }
        account.setPointsBalance(account.getPointsBalance() - points);
        account.setLifetimeRedeemed(account.getLifetimeRedeemed() + points);
        UniversalRewardAccount saved = rewardAccountRepository.save(account);
        UniversalRewardLedger ledger = rewardLedgerRepository.save(UniversalRewardLedger.builder()
                .accountId(saved.getId())
                .identityId(identityId)
                .eventType("REDEEM")
                .sourceModule(defaultString(request.getSourceModule(), "SUPER_APP"))
                .points(-points)
                .balanceAfter(saved.getPointsBalance())
                .note(blankToNull(request.getNote()))
                .build());
        audit("UNIVERSAL_REWARD", saved.getId(), "REWARD_REDEEMED", actor, points + " points");
        return rewardResponse(saved, ledger);
    }

    @Transactional
    public Map<String, Object> recordReputationEvent(Long identityId, SportsSuperAppRequests.ReputationRequest request, String actor) {
        SportsIdentity identity = identity(identityId);
        int impact = request == null || request.getImpact() == null ? 0 : request.getImpact();
        if (impact < -50 || impact > 50) {
            throw ApiException.badRequest("Reputation impact must be between -50 and 50");
        }
        int scoreAfter = clamp(defaultInt(identity.getReputationScore()) + impact, 0, 100);
        identity.setReputationScore(scoreAfter);
        identityRepository.save(identity);
        SportsReputationEvent saved = reputationEventRepository.save(SportsReputationEvent.builder()
                .identityId(identityId)
                .eventType(normalize(request.getEventType(), "MANUAL_REVIEW"))
                .sourceModule(defaultString(request.getSourceModule(), "SUPER_APP"))
                .impact(impact)
                .scoreAfter(scoreAfter)
                .note(blankToNull(request.getNote()))
                .build());
        audit("SPORTS_REPUTATION", saved.getId(), "REPUTATION_UPDATED", actor, "Score " + scoreAfter);
        return mapReputation(saved);
    }

    @Transactional
    public Map<String, Object> addTimelineEvent(Long identityId, SportsSuperAppRequests.TimelineRequest request, String actor) {
        if (request == null) {
            throw ApiException.badRequest("Timeline request is required");
        }
        identity(identityId);
        SportsTimelineEvent saved = timelineEventRepository.save(SportsTimelineEvent.builder()
                .identityId(identityId)
                .eventType(normalize(request == null ? null : request.getEventType(), "SUPER_APP_EVENT"))
                .sourceModule(defaultString(request == null ? null : request.getSourceModule(), "SUPER_APP"))
                .title(required(request == null ? null : request.getTitle(), "Timeline title is required"))
                .occurredAt(request.getOccurredAt() == null ? LocalDateTime.now() : request.getOccurredAt())
                .visibility(normalize(request.getVisibility(), "PRIVATE"))
                .metadata(blankToNull(request.getMetadata()))
                .build());
        audit("SPORTS_TIMELINE", saved.getId(), "TIMELINE_EVENT_ADDED", actor, saved.getTitle());
        return mapTimeline(saved);
    }

    @Transactional
    public Map<String, Object> createFamilyAccount(SportsSuperAppRequests.FamilyAccountRequest request, String actor) {
        if (request == null) {
            throw ApiException.badRequest("Family account request is required");
        }
        Long parentId = requiredId(request == null ? null : request.getParentIdentityId(), "Parent Sports Identity ID is required");
        Long childId = requiredId(request.getChildIdentityId(), "Child Sports Identity ID is required");
        if (parentId.equals(childId)) {
            throw ApiException.badRequest("Parent and child identities must be different");
        }
        SportsIdentity parent = identity(parentId);
        SportsIdentity child = identity(childId);
        String guardianEmail = required(request.getGuardianEmail(), "Guardian email is required").toLowerCase(Locale.ROOT);
        if (isYouth(child.getAgeGroup()) && !guardianEmail.equalsIgnoreCase(parent.getPrimaryEmail()) && !guardianEmail.equalsIgnoreCase(child.getGuardianEmail())) {
            throw ApiException.badRequest("Youth family account must use the parent or registered guardian email");
        }
        if (familyAccountRepository.existsByParentIdentityIdAndChildIdentityId(parentId, childId)) {
            throw ApiException.conflict("Family account link already exists");
        }
        FamilyAccount saved = familyAccountRepository.save(FamilyAccount.builder()
                .parentIdentityId(parentId)
                .childIdentityId(childId)
                .guardianEmail(guardianEmail)
                .relationshipType(normalize(request.getRelationshipType(), "GUARDIAN"))
                .approvalRequired(request.getApprovalRequired() == null || request.getApprovalRequired())
                .build());
        audit("FAMILY_ACCOUNT", saved.getId(), "FAMILY_LINK_CREATED", actor, child.getSportsId() + " linked to " + parent.getSportsId());
        return mapFamily(saved);
    }

    @Transactional
    public Map<String, Object> recordSportsCredits(Long identityId, SportsSuperAppRequests.SportsCreditRequest request, String actor) {
        if (request == null) {
            throw ApiException.badRequest("Sports credit request is required");
        }
        identity(identityId);
        int credits = positivePoints(request == null ? null : request.getCredits());
        List<SportsCreditLedger> previous = creditLedgerRepository.findByIdentityIdOrderByCreatedAtDesc(identityId);
        int balance = previous.isEmpty() ? credits : defaultInt(previous.get(0).getBalanceAfter()) + credits;
        SportsCreditLedger saved = creditLedgerRepository.save(SportsCreditLedger.builder()
                .identityId(identityId)
                .eventType(normalize(request.getEventType(), "EARN"))
                .sourceModule(defaultString(request.getSourceModule(), "SUPER_APP"))
                .credits(credits)
                .balanceAfter(balance)
                .note(blankToNull(request.getNote()))
                .build());
        audit("SPORTS_CREDIT", saved.getId(), "SPORTS_CREDIT_RECORDED", actor, credits + " credits");
        return mapCreditLedger(saved);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> unifiedSearch(String query) {
        String value = required(query, "Search query is required");
        return identityRepository
                .findByDisplayNameContainingIgnoreCaseOrPrimaryEmailContainingIgnoreCaseOrSportsIdContainingIgnoreCaseOrderByCreatedAtDesc(value, value, value)
                .stream()
                .limit(25)
                .map(this::mapIdentity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> identities() {
        return identityRepository.findAllByOrderByCreatedAtDesc().stream().map(this::mapIdentity).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> wallets() {
        return walletRepository.findAllByOrderByCreatedAtDesc().stream().map(this::mapWallet).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> walletLedger() {
        return walletLedgerRepository.findAllByOrderByCreatedAtDesc().stream().map(this::mapWalletLedger).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> memberships() {
        return membershipRepository.findAllByOrderByCreatedAtDesc().stream().map(this::mapMembership).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> rewardAccounts() {
        return rewardAccountRepository.findAllByOrderByCreatedAtDesc().stream().map(this::mapRewardAccount).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> reputationEvents() {
        return reputationEventRepository.findAllByOrderByCreatedAtDesc().stream().map(this::mapReputation).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> timelineEvents() {
        return timelineEventRepository.findAllByOrderByOccurredAtDesc().stream().map(this::mapTimeline).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> familyAccounts() {
        return familyAccountRepository.findAllByOrderByCreatedAtDesc().stream().map(this::mapFamily).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> audits() {
        return auditRepository.findAllByOrderByCreatedAtDesc().stream().limit(100).map(this::mapAudit).toList();
    }

    private SportsIdentity identity(Long id) {
        return identityRepository.findById(requiredId(id, "Sports Identity ID is required"))
                .orElseThrow(() -> ApiException.notFound("Sports Identity not found"));
    }

    private UniversalWalletAccount getOrCreateWallet(Long identityId) {
        return walletRepository.findByIdentityId(identityId)
                .orElseGet(() -> walletRepository.save(UniversalWalletAccount.builder()
                        .identityId(identityId)
                        .balance(BigDecimal.ZERO)
                        .lockedAmount(BigDecimal.ZERO)
                        .build()));
    }

    private UniversalRewardAccount getOrCreateRewardAccount(Long identityId) {
        return rewardAccountRepository.findByIdentityId(identityId)
                .orElseGet(() -> rewardAccountRepository.save(UniversalRewardAccount.builder()
                        .identityId(identityId)
                        .build()));
    }

    private Map<String, Object> kpis(List<SportsIdentity> identities) {
        Map<String, Object> kpis = new LinkedHashMap<>();
        kpis.put("sportsIdentities", identities.size());
        kpis.put("activeIdentities", identities.stream().filter(identity -> "ACTIVE".equalsIgnoreCase(identity.getStatus())).count());
        kpis.put("publicPassports", passportRepository.findAllByOrderByCreatedAtDesc().stream().filter(passport -> "PUBLIC".equalsIgnoreCase(passport.getVisibility())).count());
        kpis.put("walletAccounts", walletRepository.count());
        kpis.put("activeMemberships", membershipRepository.findAllByOrderByCreatedAtDesc().stream().filter(membership -> "ACTIVE".equalsIgnoreCase(membership.getStatus())).count());
        kpis.put("rewardAccounts", rewardAccountRepository.count());
        kpis.put("familyLinks", familyAccountRepository.count());
        kpis.put("averageReputation", average(identities.stream().map(SportsIdentity::getReputationScore).toList()));
        return kpis;
    }

    private Map<String, Object> crossModuleSignals() {
        Map<String, Object> signals = new LinkedHashMap<>();
        signals.put("bookings", bookingRepository.count());
        signals.put("tournamentRegistrations", tournamentRegistrationRepository.count());
        signals.put("careerProfiles", careerProfileRepository.count());
        signals.put("federationRegistrations", federationPlayerRegistrationRepository.count());
        signals.put("integratedModules", List.of("BOOKINGS", "TOURNAMENTS", "CAREER", "FEDERATION", "WALLET", "MEMBERSHIP", "REWARDS"));
        return signals;
    }

    private List<Map<String, Object>> priorityControls(List<SportsIdentity> identities) {
        long youthWithoutGuardian = identities.stream().filter(identity -> isYouth(identity.getAgeGroup()) && blankToNull(identity.getGuardianEmail()) == null).count();
        long lowReputation = identities.stream().filter(identity -> defaultInt(identity.getReputationScore()) < 40).count();
        return List.of(
                Map.of("control", "Identity uniqueness", "status", "ENFORCED", "detail", "Sports ID and primary email are unique"),
                Map.of("control", "Wallet double-spend prevention", "status", "ENFORCED", "detail", "Debits and transfers require available balance"),
                Map.of("control", "Youth guardian coverage", "status", youthWithoutGuardian == 0 ? "CLEAR" : "REVIEW", "detail", youthWithoutGuardian + " identities need guardian review"),
                Map.of("control", "Reputation risk", "status", lowReputation == 0 ? "CLEAR" : "WATCH", "detail", lowReputation + " low reputation identities")
        );
    }

    private List<Map<String, Object>> governanceControls() {
        return List.of(
                Map.of("name", "One Sports Identity", "status", "ACTIVE", "risk", "DUPLICATE_IDENTITY"),
                Map.of("name", "One Wallet Ledger", "status", "ACTIVE", "risk", "DOUBLE_SPEND"),
                Map.of("name", "Passport Sharing", "status", "ACTIVE", "risk", "PRIVACY_EXPOSURE"),
                Map.of("name", "Family Accounts", "status", "ACTIVE", "risk", "YOUTH_CONSENT"),
                Map.of("name", "Cross-Module Personalization", "status", "FOUNDATION_READY", "risk", "DATA_CONSISTENCY")
        );
    }

    private String generateSportsId() {
        String sportsId;
        do {
            sportsId = "TSID-" + LocalDateTime.now().getYear() + "-" + (100000 + RANDOM.nextInt(900000));
        } while (identityRepository.existsBySportsId(sportsId));
        return sportsId;
    }

    private String generateShareCode() {
        String shareCode;
        do {
            shareCode = "PASS-" + Long.toUnsignedString(RANDOM.nextLong(), 36).toUpperCase(Locale.ROOT);
        } while (passportRepository.existsByShareCode(shareCode));
        return shareCode;
    }

    private String statisticsSnapshot(SportsIdentity identity) {
        return "Bookings=" + bookingRepository.count()
                + "; TournamentRegistrations=" + tournamentRegistrationRepository.count()
                + "; CareerProfiles=" + careerProfileRepository.count()
                + "; PrimarySport=" + identity.getPrimarySport();
    }

    private String achievementSnapshot(SportsIdentity identity) {
        return "SportsID=" + identity.getSportsId()
                + "; IdentityScore=" + identity.getIdentityScore()
                + "; Reputation=" + identity.getReputationScore();
    }

    private void audit(String entityType, Long entityId, String action, String actor, String details) {
        auditRepository.save(SuperAppAudit.builder()
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .actor(defaultString(actor, "system"))
                .details(details)
                .build());
    }

    private Map<String, Object> walletResponse(UniversalWalletAccount wallet, UniversalWalletLedger ledger) {
        return Map.of("wallet", mapWallet(wallet), "ledger", mapWalletLedger(ledger));
    }

    private Map<String, Object> rewardResponse(UniversalRewardAccount account, UniversalRewardLedger ledger) {
        return Map.of("account", mapRewardAccount(account), "ledger", mapRewardLedger(ledger));
    }

    private Map<String, Object> mapIdentity(SportsIdentity identity) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", identity.getId());
        map.put("userId", identity.getUserId());
        map.put("sportsId", identity.getSportsId());
        map.put("displayName", identity.getDisplayName());
        map.put("primaryEmail", identity.getPrimaryEmail());
        map.put("phone", identity.getPhone());
        map.put("city", identity.getCity());
        map.put("primarySport", identity.getPrimarySport());
        map.put("ageGroup", identity.getAgeGroup());
        map.put("guardianEmail", identity.getGuardianEmail());
        map.put("status", identity.getStatus());
        map.put("identityScore", identity.getIdentityScore());
        map.put("reputationScore", identity.getReputationScore());
        map.put("profileVisibility", identity.getProfileVisibility());
        map.put("linkedModules", identity.getLinkedModules());
        map.put("createdAt", identity.getCreatedAt());
        return map;
    }

    private Map<String, Object> mapPassport(SportsPassport passport, SportsIdentity identity) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", passport.getId());
        map.put("identity", mapIdentity(identity));
        map.put("shareCode", passport.getShareCode());
        map.put("visibility", passport.getVisibility());
        map.put("qrPayload", passport.getQrPayload());
        map.put("statisticsSnapshot", passport.getStatisticsSnapshot());
        map.put("achievementsSnapshot", passport.getAchievementsSnapshot());
        map.put("membershipSnapshot", passport.getMembershipSnapshot());
        map.put("reputationSnapshot", passport.getReputationSnapshot());
        map.put("viewCount", passport.getViewCount());
        map.put("expiresAt", passport.getExpiresAt());
        map.put("createdAt", passport.getCreatedAt());
        return map;
    }

    private Map<String, Object> mapWallet(UniversalWalletAccount wallet) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", wallet.getId());
        map.put("identityId", wallet.getIdentityId());
        map.put("balance", wallet.getBalance());
        map.put("lockedAmount", wallet.getLockedAmount());
        map.put("status", wallet.getStatus());
        map.put("createdAt", wallet.getCreatedAt());
        return map;
    }

    private Map<String, Object> mapWalletLedger(UniversalWalletLedger ledger) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", ledger.getId());
        map.put("walletId", ledger.getWalletId());
        map.put("identityId", ledger.getIdentityId());
        map.put("transactionType", ledger.getTransactionType());
        map.put("sourceModule", ledger.getSourceModule());
        map.put("amount", ledger.getAmount());
        map.put("balanceAfter", ledger.getBalanceAfter());
        map.put("referenceType", ledger.getReferenceType());
        map.put("referenceId", ledger.getReferenceId());
        map.put("note", ledger.getNote());
        map.put("createdAt", ledger.getCreatedAt());
        return map;
    }

    private Map<String, Object> mapMembership(UnifiedMembership membership) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", membership.getId());
        map.put("identityId", membership.getIdentityId());
        map.put("tier", membership.getTier());
        map.put("billingCycle", membership.getBillingCycle());
        map.put("status", membership.getStatus());
        map.put("price", membership.getPrice());
        map.put("startsAt", membership.getStartsAt());
        map.put("expiresAt", membership.getExpiresAt());
        map.put("autoRenew", membership.getAutoRenew());
        map.put("benefitsSummary", membership.getBenefitsSummary());
        return map;
    }

    private Map<String, Object> mapRewardAccount(UniversalRewardAccount account) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", account.getId());
        map.put("identityId", account.getIdentityId());
        map.put("pointsBalance", account.getPointsBalance());
        map.put("lifetimeEarned", account.getLifetimeEarned());
        map.put("lifetimeRedeemed", account.getLifetimeRedeemed());
        return map;
    }

    private Map<String, Object> mapRewardLedger(UniversalRewardLedger ledger) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", ledger.getId());
        map.put("accountId", ledger.getAccountId());
        map.put("identityId", ledger.getIdentityId());
        map.put("eventType", ledger.getEventType());
        map.put("sourceModule", ledger.getSourceModule());
        map.put("points", ledger.getPoints());
        map.put("balanceAfter", ledger.getBalanceAfter());
        map.put("note", ledger.getNote());
        map.put("createdAt", ledger.getCreatedAt());
        return map;
    }

    private Map<String, Object> mapReputation(SportsReputationEvent event) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", event.getId());
        map.put("identityId", event.getIdentityId());
        map.put("eventType", event.getEventType());
        map.put("sourceModule", event.getSourceModule());
        map.put("impact", event.getImpact());
        map.put("scoreAfter", event.getScoreAfter());
        map.put("note", event.getNote());
        map.put("createdAt", event.getCreatedAt());
        return map;
    }

    private Map<String, Object> mapTimeline(SportsTimelineEvent event) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", event.getId());
        map.put("identityId", event.getIdentityId());
        map.put("eventType", event.getEventType());
        map.put("sourceModule", event.getSourceModule());
        map.put("title", event.getTitle());
        map.put("occurredAt", event.getOccurredAt());
        map.put("visibility", event.getVisibility());
        map.put("metadata", event.getMetadata());
        return map;
    }

    private Map<String, Object> mapFamily(FamilyAccount account) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", account.getId());
        map.put("parentIdentityId", account.getParentIdentityId());
        map.put("childIdentityId", account.getChildIdentityId());
        map.put("guardianEmail", account.getGuardianEmail());
        map.put("relationshipType", account.getRelationshipType());
        map.put("approvalRequired", account.getApprovalRequired());
        map.put("status", account.getStatus());
        map.put("createdAt", account.getCreatedAt());
        return map;
    }

    private Map<String, Object> mapCreditLedger(SportsCreditLedger ledger) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", ledger.getId());
        map.put("identityId", ledger.getIdentityId());
        map.put("eventType", ledger.getEventType());
        map.put("sourceModule", ledger.getSourceModule());
        map.put("credits", ledger.getCredits());
        map.put("balanceAfter", ledger.getBalanceAfter());
        map.put("note", ledger.getNote());
        map.put("createdAt", ledger.getCreatedAt());
        return map;
    }

    private Map<String, Object> mapAudit(SuperAppAudit audit) {
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

    private int identityScore(SportsSuperAppRequests.IdentityRequest request) {
        int score = 60;
        if (blankToNull(request.getPrimaryEmail()) != null) {
            score += 10;
        }
        if (blankToNull(request.getPhone()) != null) {
            score += 10;
        }
        if (blankToNull(request.getCity()) != null) {
            score += 5;
        }
        if (blankToNull(request.getPrimarySport()) != null) {
            score += 5;
        }
        if (blankToNull(request.getLinkedModules()) != null) {
            score += 10;
        }
        return clamp(score, 0, 100);
    }

    private String defaultLinkedModules(String linkedModules) {
        return defaultString(linkedModules, "BOOKINGS,WALLET,MEMBERSHIP,REWARDS,TOURNAMENTS,CAREER,GOVERNANCE");
    }

    private BigDecimal walletAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw ApiException.badRequest("Amount must be greater than zero");
        }
        if (amount.compareTo(MAX_WALLET_EVENT) > 0) {
            throw ApiException.badRequest("Amount exceeds universal wallet transaction limit");
        }
        if (amount.scale() > 2) {
            throw ApiException.badRequest("Amount can have at most 2 decimal places");
        }
        return amount;
    }

    private BigDecimal defaultMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private int positivePoints(Integer points) {
        if (points == null || points <= 0) {
            throw ApiException.badRequest("Points must be greater than zero");
        }
        return points;
    }

    private Long requiredId(Long id, String message) {
        if (id == null || id <= 0) {
            throw ApiException.badRequest(message);
        }
        return id;
    }

    private String required(String value, String message) {
        String cleaned = blankToNull(value);
        if (cleaned == null) {
            throw ApiException.badRequest(message);
        }
        return cleaned;
    }

    private String normalize(String value, String defaultValue) {
        return defaultString(value, defaultValue).trim().toUpperCase(Locale.ROOT);
    }

    private String defaultString(String value, String defaultValue) {
        String cleaned = blankToNull(value);
        return cleaned == null ? defaultValue : cleaned;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private boolean isYouth(String ageGroup) {
        String value = normalize(ageGroup, "OPEN");
        return value.startsWith("U") || value.contains("YOUTH") || value.contains("MINOR");
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private int average(List<Integer> values) {
        List<Integer> filtered = values.stream().filter(value -> value != null && value > 0).toList();
        if (filtered.isEmpty()) {
            return 0;
        }
        return Math.round((float) filtered.stream().mapToInt(Integer::intValue).sum() / filtered.size());
    }
}
