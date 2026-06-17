package com.example.Cricbook.service;

import com.example.Cricbook.dto.SportsSuperAppRequests;
import com.example.Cricbook.entity.*;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SportsSuperAppServiceTest {

    @Mock
    private SportsIdentityRepository identityRepository;
    @Mock
    private SportsPassportRepository passportRepository;
    @Mock
    private UniversalWalletAccountRepository walletRepository;
    @Mock
    private UniversalWalletLedgerRepository walletLedgerRepository;
    @Mock
    private UnifiedMembershipRepository membershipRepository;
    @Mock
    private UniversalRewardAccountRepository rewardAccountRepository;
    @Mock
    private UniversalRewardLedgerRepository rewardLedgerRepository;
    @Mock
    private SportsReputationEventRepository reputationEventRepository;
    @Mock
    private SportsTimelineEventRepository timelineEventRepository;
    @Mock
    private FamilyAccountRepository familyAccountRepository;
    @Mock
    private SportsCreditLedgerRepository creditLedgerRepository;
    @Mock
    private SuperAppAuditRepository auditRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private TournamentRegistrationRepository tournamentRegistrationRepository;
    @Mock
    private CareerProfileRepository careerProfileRepository;
    @Mock
    private FederationPlayerRegistrationRepository federationPlayerRegistrationRepository;

    @InjectMocks
    private SportsSuperAppService sportsSuperAppService;

    @BeforeEach
    void setUp() {
        lenient().when(auditRepository.save(any(SuperAppAudit.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createIdentityGeneratesSportsIdAndBlocksDuplicateEmail() {
        SportsSuperAppRequests.IdentityRequest request = new SportsSuperAppRequests.IdentityRequest();
        request.setDisplayName("Super App Player");
        request.setPrimaryEmail("player@example.com");
        request.setPhone("9999999999");
        request.setCity("Ahmedabad");
        request.setPrimarySport("CRICKET");

        when(identityRepository.existsByPrimaryEmailIgnoreCase("player@example.com")).thenReturn(false);
        when(identityRepository.existsBySportsId(any())).thenReturn(false);
        when(identityRepository.save(any(SportsIdentity.class))).thenAnswer(invocation -> {
            SportsIdentity identity = invocation.getArgument(0);
            identity.setId(1L);
            return identity;
        });
        when(walletRepository.findByIdentityId(1L)).thenReturn(Optional.empty());
        when(walletRepository.save(any(UniversalWalletAccount.class))).thenAnswer(invocation -> {
            UniversalWalletAccount wallet = invocation.getArgument(0);
            wallet.setId(10L);
            return wallet;
        });
        when(rewardAccountRepository.findByIdentityId(1L)).thenReturn(Optional.empty());
        when(rewardAccountRepository.save(any(UniversalRewardAccount.class))).thenAnswer(invocation -> {
            UniversalRewardAccount account = invocation.getArgument(0);
            account.setId(20L);
            return account;
        });

        Map<String, Object> response = sportsSuperAppService.createIdentity(request, "admin@example.com");

        assertEquals("Super App Player", response.get("displayName"));
        assertTrue(String.valueOf(response.get("sportsId")).startsWith("TSID-"));
        assertEquals(90, response.get("identityScore"));
        verify(walletRepository).save(any(UniversalWalletAccount.class));
        verify(rewardAccountRepository).save(any(UniversalRewardAccount.class));

        when(identityRepository.existsByPrimaryEmailIgnoreCase("player@example.com")).thenReturn(true);
        assertThrows(ApiException.class, () -> sportsSuperAppService.createIdentity(request, "admin@example.com"));
    }

    @Test
    void debitWalletPreventsDoubleSpendAndRecordsLedger() {
        SportsIdentity identity = SportsIdentity.builder()
                .id(1L)
                .sportsId("TSID-2026-111111")
                .primaryEmail("wallet@example.com")
                .displayName("Wallet Player")
                .primarySport("CRICKET")
                .build();
        UniversalWalletAccount wallet = UniversalWalletAccount.builder()
                .id(10L)
                .identityId(1L)
                .balance(BigDecimal.valueOf(100))
                .lockedAmount(BigDecimal.ZERO)
                .build();
        SportsSuperAppRequests.WalletLedgerRequest request = new SportsSuperAppRequests.WalletLedgerRequest();
        request.setAmount(BigDecimal.valueOf(150));
        request.setSourceModule("BOOKING");

        when(identityRepository.findById(1L)).thenReturn(Optional.of(identity));
        when(walletRepository.findByIdentityId(1L)).thenReturn(Optional.of(wallet));

        assertThrows(ApiException.class, () -> sportsSuperAppService.debitWallet(1L, request, "player@example.com"));

        request.setAmount(BigDecimal.valueOf(40));
        when(walletRepository.save(any(UniversalWalletAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(walletLedgerRepository.save(any(UniversalWalletLedger.class))).thenAnswer(invocation -> {
            UniversalWalletLedger ledger = invocation.getArgument(0);
            ledger.setId(30L);
            return ledger;
        });

        Map<String, Object> response = sportsSuperAppService.debitWallet(1L, request, "player@example.com");
        Map<?, ?> walletResponse = (Map<?, ?>) response.get("wallet");

        assertEquals(0, BigDecimal.valueOf(60).compareTo((BigDecimal) walletResponse.get("balance")));
        verify(walletLedgerRepository).save(any(UniversalWalletLedger.class));
    }

    @Test
    void familyAccountRequiresGuardianForYouthIdentity() {
        SportsIdentity parent = SportsIdentity.builder()
                .id(1L)
                .sportsId("TSID-2026-222222")
                .primaryEmail("parent@example.com")
                .displayName("Parent")
                .primarySport("CRICKET")
                .build();
        SportsIdentity child = SportsIdentity.builder()
                .id(2L)
                .sportsId("TSID-2026-333333")
                .primaryEmail("child@example.com")
                .displayName("Child")
                .primarySport("CRICKET")
                .ageGroup("U16")
                .guardianEmail("parent@example.com")
                .build();
        SportsSuperAppRequests.FamilyAccountRequest request = new SportsSuperAppRequests.FamilyAccountRequest();
        request.setParentIdentityId(1L);
        request.setChildIdentityId(2L);
        request.setGuardianEmail("unknown@example.com");

        when(identityRepository.findById(1L)).thenReturn(Optional.of(parent));
        when(identityRepository.findById(2L)).thenReturn(Optional.of(child));

        assertThrows(ApiException.class, () -> sportsSuperAppService.createFamilyAccount(request, "parent@example.com"));

        request.setGuardianEmail("parent@example.com");
        when(familyAccountRepository.existsByParentIdentityIdAndChildIdentityId(1L, 2L)).thenReturn(false);
        when(familyAccountRepository.save(any(FamilyAccount.class))).thenAnswer(invocation -> {
            FamilyAccount account = invocation.getArgument(0);
            account.setId(40L);
            return account;
        });

        Map<String, Object> response = sportsSuperAppService.createFamilyAccount(request, "parent@example.com");

        assertEquals(40L, response.get("id"));
        assertEquals("parent@example.com", response.get("guardianEmail"));
    }

    @Test
    void generateAndOpenPublicPassportIncrementsViews() {
        SportsIdentity identity = SportsIdentity.builder()
                .id(1L)
                .sportsId("TSID-2026-444444")
                .primaryEmail("passport@example.com")
                .displayName("Passport Player")
                .primarySport("CRICKET")
                .profileVisibility("PUBLIC")
                .build();

        when(identityRepository.findById(1L)).thenReturn(Optional.of(identity));
        when(passportRepository.findByIdentityId(1L)).thenReturn(Optional.empty());
        when(passportRepository.existsByShareCode(any())).thenReturn(false);
        when(passportRepository.save(any(SportsPassport.class))).thenAnswer(invocation -> {
            SportsPassport passport = invocation.getArgument(0);
            passport.setId(50L);
            return passport;
        });

        Map<String, Object> generated = sportsSuperAppService.generatePassport(1L, null, "player@example.com");

        assertEquals("PUBLIC", generated.get("visibility"));
        assertNotNull(generated.get("shareCode"));

        SportsPassport passport = SportsPassport.builder()
                .id(50L)
                .identityId(1L)
                .shareCode(String.valueOf(generated.get("shareCode")))
                .visibility("PUBLIC")
                .viewCount(0)
                .build();
        when(passportRepository.findByShareCode(passport.getShareCode())).thenReturn(Optional.of(passport));

        Map<String, Object> opened = sportsSuperAppService.publicPassport(passport.getShareCode());

        assertEquals(1, opened.get("viewCount"));
    }

    @Test
    void reputationEventClampsScoreAtHundred() {
        SportsIdentity identity = SportsIdentity.builder()
                .id(1L)
                .sportsId("TSID-2026-555555")
                .primaryEmail("trust@example.com")
                .displayName("Trust Player")
                .primarySport("CRICKET")
                .reputationScore(95)
                .build();
        SportsSuperAppRequests.ReputationRequest request = new SportsSuperAppRequests.ReputationRequest();
        request.setEventType("TOURNAMENT_WIN");
        request.setSourceModule("TOURNAMENT");
        request.setImpact(10);

        when(identityRepository.findById(1L)).thenReturn(Optional.of(identity));
        when(identityRepository.save(any(SportsIdentity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reputationEventRepository.save(any(SportsReputationEvent.class))).thenAnswer(invocation -> {
            SportsReputationEvent event = invocation.getArgument(0);
            event.setId(60L);
            return event;
        });

        Map<String, Object> response = sportsSuperAppService.recordReputationEvent(1L, request, "admin@example.com");

        assertEquals(100, response.get("scoreAfter"));
        assertEquals(100, identity.getReputationScore());
    }
}
