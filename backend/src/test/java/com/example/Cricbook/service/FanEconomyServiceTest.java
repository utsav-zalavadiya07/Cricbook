package com.example.Cricbook.service;

import com.example.Cricbook.dto.FanEconomyRequests;
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
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FanEconomyServiceTest {

    @Mock
    private FanProfileRepository fanProfileRepository;
    @Mock
    private FanClubRepository fanClubRepository;
    @Mock
    private FanClubMembershipRepository fanClubMembershipRepository;
    @Mock
    private DigitalCollectibleRepository digitalCollectibleRepository;
    @Mock
    private FanCollectibleOwnershipRepository collectibleOwnershipRepository;
    @Mock
    private PredictionGameRepository predictionGameRepository;
    @Mock
    private FanPredictionEntryRepository predictionEntryRepository;
    @Mock
    private CreatorProfileRepository creatorProfileRepository;
    @Mock
    private CreatorContentRepository creatorContentRepository;
    @Mock
    private FanMarketplaceListingRepository marketplaceListingRepository;
    @Mock
    private WatchPartyRepository watchPartyRepository;
    @Mock
    private WatchPartyParticipantRepository watchPartyParticipantRepository;
    @Mock
    private FanEconomyAuditRepository auditRepository;

    @InjectMocks
    private FanEconomyService fanEconomyService;

    @BeforeEach
    void setUp() {
        lenient().when(auditRepository.save(any(FanEconomyAudit.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createFanProfileBlocksDuplicateEmail() {
        FanEconomyRequests.FanProfileRequest request = new FanEconomyRequests.FanProfileRequest();
        request.setDisplayName("Utsav Fan");
        request.setEmail("utsav@example.com");
        request.setFavoriteSport("CRICKET");

        when(fanProfileRepository.existsByEmailIgnoreCase("utsav@example.com")).thenReturn(true);

        assertThrows(ApiException.class, () -> fanEconomyService.createFanProfile(request, "admin@example.com"));
        verify(fanProfileRepository, never()).save(any(FanProfile.class));
    }

    @Test
    void joinFanClubIncrementsMembersAndBlocksDuplicateMembership() {
        FanProfile fan = FanProfile.builder()
                .id(1L)
                .fanCode("TFAN-2026-111111")
                .displayName("Fan")
                .email("fan@example.com")
                .status("ACTIVE")
                .build();
        FanClub club = FanClub.builder()
                .id(2L)
                .clubCode("TFCLUB-2026-222222")
                .name("Ahmedabad Fans")
                .sport("CRICKET")
                .status("ACTIVE")
                .memberCount(0)
                .build();
        FanEconomyRequests.JoinFanClubRequest request = new FanEconomyRequests.JoinFanClubRequest();
        request.setFanProfileId(1L);

        when(fanClubRepository.findById(2L)).thenReturn(Optional.of(club));
        when(fanProfileRepository.findById(1L)).thenReturn(Optional.of(fan));
        when(fanClubMembershipRepository.findByFanProfileIdAndFanClubIdAndStatus(1L, 2L, "ACTIVE")).thenReturn(Optional.empty());
        when(fanClubMembershipRepository.save(any(FanClubMembership.class))).thenAnswer(invocation -> {
            FanClubMembership membership = invocation.getArgument(0);
            membership.setId(10L);
            return membership;
        });
        when(fanClubMembershipRepository.countByFanClubIdAndStatus(2L, "ACTIVE")).thenReturn(1L);
        when(fanClubRepository.save(any(FanClub.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = fanEconomyService.joinFanClub(2L, request, "fan@example.com");

        assertEquals(10L, response.get("id"));
        assertEquals(1, club.getMemberCount());
        verify(auditRepository).save(any(FanEconomyAudit.class));

        when(fanClubMembershipRepository.findByFanProfileIdAndFanClubIdAndStatus(1L, 2L, "ACTIVE"))
                .thenReturn(Optional.of(FanClubMembership.builder().id(10L).fanProfileId(1L).fanClubId(2L).status("ACTIVE").build()));

        assertThrows(ApiException.class, () -> fanEconomyService.joinFanClub(2L, request, "fan@example.com"));
    }

    @Test
    void claimCollectibleEnforcesDuplicateOwnershipAndSupplyLimit() {
        FanProfile fan = FanProfile.builder()
                .id(1L)
                .fanCode("TFAN-2026-111111")
                .displayName("Fan")
                .email("fan@example.com")
                .status("ACTIVE")
                .build();
        FanProfile secondFan = FanProfile.builder()
                .id(2L)
                .fanCode("TFAN-2026-222222")
                .displayName("Buyer")
                .email("buyer@example.com")
                .status("ACTIVE")
                .build();
        DigitalCollectible collectible = DigitalCollectible.builder()
                .id(5L)
                .collectibleCode("TFDC-2026-333333")
                .title("Final Winning Moment")
                .collectibleType("MOMENT")
                .rarity("RARE")
                .status("ACTIVE")
                .supplyLimit(1)
                .mintedCount(0)
                .build();
        FanEconomyRequests.ClaimCollectibleRequest request = new FanEconomyRequests.ClaimCollectibleRequest();
        request.setFanProfileId(1L);

        when(digitalCollectibleRepository.findById(5L)).thenReturn(Optional.of(collectible));
        when(fanProfileRepository.findById(1L)).thenReturn(Optional.of(fan));
        when(collectibleOwnershipRepository.existsByFanProfileIdAndCollectibleIdAndOwnershipStatus(1L, 5L, "ACTIVE")).thenReturn(false);
        when(digitalCollectibleRepository.save(any(DigitalCollectible.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(collectibleOwnershipRepository.save(any(FanCollectibleOwnership.class))).thenAnswer(invocation -> {
            FanCollectibleOwnership ownership = invocation.getArgument(0);
            ownership.setId(20L);
            return ownership;
        });

        var response = fanEconomyService.claimCollectible(5L, request, "fan@example.com");

        assertEquals(20L, response.get("id"));
        assertEquals(1, collectible.getMintedCount());

        when(collectibleOwnershipRepository.existsByFanProfileIdAndCollectibleIdAndOwnershipStatus(1L, 5L, "ACTIVE")).thenReturn(true);
        assertThrows(ApiException.class, () -> fanEconomyService.claimCollectible(5L, request, "fan@example.com"));

        FanEconomyRequests.ClaimCollectibleRequest secondRequest = new FanEconomyRequests.ClaimCollectibleRequest();
        secondRequest.setFanProfileId(2L);
        when(fanProfileRepository.findById(2L)).thenReturn(Optional.of(secondFan));
        when(collectibleOwnershipRepository.existsByFanProfileIdAndCollectibleIdAndOwnershipStatus(2L, 5L, "ACTIVE")).thenReturn(false);
        assertThrows(ApiException.class, () -> fanEconomyService.claimCollectible(5L, secondRequest, "buyer@example.com"));
    }

    @Test
    void scorePredictionAwardsRankPointsOnce() {
        PredictionGame game = PredictionGame.builder()
                .id(3L)
                .gameCode("TFPRED-2026-123456")
                .title("Final winner")
                .predictionType("MATCH_WINNER")
                .pointsForCorrect(120)
                .deadlineAt(LocalDateTime.now().plusDays(1))
                .status("ACTIVE")
                .build();
        FanPredictionEntry entry = FanPredictionEntry.builder()
                .id(4L)
                .gameId(3L)
                .fanProfileId(1L)
                .predictionValue("Team A")
                .resultStatus("PENDING")
                .pointsAwarded(0)
                .build();
        FanProfile fan = FanProfile.builder()
                .id(1L)
                .fanCode("TFAN-2026-111111")
                .displayName("Fan")
                .email("fan@example.com")
                .rankPoints(10)
                .fanScore(20)
                .status("ACTIVE")
                .build();
        FanEconomyRequests.PredictionScoreRequest request = new FanEconomyRequests.PredictionScoreRequest();
        request.setResultStatus("CORRECT");
        request.setSettlementNote("Official score matched");

        when(predictionEntryRepository.findById(4L)).thenReturn(Optional.of(entry));
        when(predictionGameRepository.findById(3L)).thenReturn(Optional.of(game));
        when(fanProfileRepository.findById(1L)).thenReturn(Optional.of(fan));
        when(predictionEntryRepository.save(any(FanPredictionEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(fanProfileRepository.save(any(FanProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = fanEconomyService.scorePrediction(4L, request, "admin@example.com");

        assertEquals("CORRECT", response.get("resultStatus"));
        assertEquals(120, response.get("pointsAwarded"));
        assertEquals(130, fan.getRankPoints());
        assertEquals(140, fan.getFanScore());

        assertThrows(ApiException.class, () -> fanEconomyService.scorePrediction(4L, request, "admin@example.com"));
    }

    @Test
    void purchaseMarketplaceListingTransfersOwnershipAndBlocksOwnerSelfBuy() {
        FanMarketplaceListing listing = FanMarketplaceListing.builder()
                .id(8L)
                .listingCode("TFMKT-2026-444444")
                .ownerFanProfileId(1L)
                .collectibleOwnershipId(7L)
                .price(BigDecimal.valueOf(500))
                .status("ACTIVE")
                .build();
        FanCollectibleOwnership ownership = FanCollectibleOwnership.builder()
                .id(7L)
                .fanProfileId(1L)
                .collectibleId(5L)
                .ownershipReference("TFDC-2026-333333-OWN-1")
                .serialNumber(1)
                .ownershipStatus("ACTIVE")
                .transferHistory("CLAIMED_BY=TFAN-2026-111111")
                .build();
        FanProfile buyer = FanProfile.builder()
                .id(2L)
                .fanCode("TFAN-2026-222222")
                .displayName("Buyer")
                .email("buyer@example.com")
                .status("ACTIVE")
                .build();
        FanEconomyRequests.MarketplacePurchaseRequest request = new FanEconomyRequests.MarketplacePurchaseRequest();
        request.setBuyerFanProfileId(2L);

        when(marketplaceListingRepository.findById(8L)).thenReturn(Optional.of(listing));
        when(fanProfileRepository.findById(2L)).thenReturn(Optional.of(buyer));
        when(collectibleOwnershipRepository.findById(7L)).thenReturn(Optional.of(ownership));
        when(collectibleOwnershipRepository.save(any(FanCollectibleOwnership.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(marketplaceListingRepository.save(any(FanMarketplaceListing.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = fanEconomyService.purchaseMarketplaceListing(8L, request, "buyer@example.com");

        assertEquals("SOLD", response.get("status"));
        assertEquals(2L, ownership.getFanProfileId());
        assertEquals(2L, listing.getBuyerFanProfileId());

        FanMarketplaceListing ownerListing = FanMarketplaceListing.builder()
                .id(9L)
                .listingCode("TFMKT-2026-555555")
                .ownerFanProfileId(2L)
                .collectibleOwnershipId(7L)
                .price(BigDecimal.valueOf(500))
                .status("ACTIVE")
                .build();
        when(marketplaceListingRepository.findById(9L)).thenReturn(Optional.of(ownerListing));
        assertThrows(ApiException.class, () -> fanEconomyService.purchaseMarketplaceListing(9L, request, "buyer@example.com"));
    }
}
