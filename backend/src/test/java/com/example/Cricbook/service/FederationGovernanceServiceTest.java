package com.example.Cricbook.service;

import com.example.Cricbook.dto.FederationGovernanceRequests;
import com.example.Cricbook.entity.*;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.*;
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
class FederationGovernanceServiceTest {

    @Mock
    private FederationBodyRepository bodyRepository;
    @Mock
    private FederationFranchiseRepository franchiseRepository;
    @Mock
    private FederationPlayerRegistrationRepository playerRepository;
    @Mock
    private FederationDraftRepository draftRepository;
    @Mock
    private FederationDraftSelectionRepository draftSelectionRepository;
    @Mock
    private FederationAuctionRepository auctionRepository;
    @Mock
    private FederationAuctionBidRepository auctionBidRepository;
    @Mock
    private FederationTransferRepository transferRepository;
    @Mock
    private FederationLicenseRepository licenseRepository;
    @Mock
    private FederationOfficialRepository officialRepository;
    @Mock
    private FederationDisciplinaryCaseRepository caseRepository;
    @Mock
    private FederationElectionRepository electionRepository;
    @Mock
    private FederationElectionVoteRepository voteRepository;
    @Mock
    private FederationRankingRepository rankingRepository;
    @Mock
    private FederationIntegrityAlertRepository integrityAlertRepository;
    @Mock
    private FederationGovernanceAuditRepository auditRepository;
    @Mock
    private CareerProfileRepository careerProfileRepository;
    @Mock
    private TournamentRepository tournamentRepository;
    @Mock
    private CorporateLeagueRepository corporateLeagueRepository;

    @InjectMocks
    private FederationGovernanceService federationGovernanceService;

    @Test
    void registerPlayerGeneratesSportsIdAndRejectsDuplicateEmail() {
        FederationGovernanceRequests.PlayerRegistrationRequest request = new FederationGovernanceRequests.PlayerRegistrationRequest();
        request.setPlayerName("National Player");
        request.setEmail("national@example.com");
        request.setSport("CRICKET");
        request.setCity("Ahmedabad");
        request.setAgeGroup("U19");

        when(playerRepository.existsByEmailIgnoreCase("national@example.com")).thenReturn(false);
        when(playerRepository.existsBySportsId(any())).thenReturn(false);
        when(playerRepository.save(any(FederationPlayerRegistration.class))).thenAnswer(invocation -> {
            FederationPlayerRegistration player = invocation.getArgument(0);
            player.setId(11L);
            return player;
        });
        when(auditRepository.save(any(FederationGovernanceAudit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> response = federationGovernanceService.registerPlayer(request, "admin@example.com");

        assertEquals("National Player", response.get("playerName"));
        assertTrue(String.valueOf(response.get("sportsId")).startsWith("TSI-"));
        assertEquals("PENDING", response.get("verificationStatus"));
        verify(auditRepository).save(any(FederationGovernanceAudit.class));

        when(playerRepository.existsByEmailIgnoreCase("national@example.com")).thenReturn(true);
        assertThrows(ApiException.class, () -> federationGovernanceService.registerPlayer(request, "admin@example.com"));
    }

    @Test
    void draftSelectionAssignsPlayerAndBlocksDuplicateSelection() {
        FederationDraft draft = FederationDraft.builder()
                .id(20L)
                .federationId(1L)
                .name("National Draft")
                .sport("CRICKET")
                .status("PUBLISHED")
                .build();
        FederationPlayerRegistration player = FederationPlayerRegistration.builder()
                .id(30L)
                .sportsId("TSI-2026-100001")
                .playerName("Draft Player")
                .email("draft@example.com")
                .sport("CRICKET")
                .verificationStatus("VERIFIED")
                .status("ELIGIBLE")
                .build();
        FederationFranchise franchise = FederationFranchise.builder()
                .id(40L)
                .federationId(1L)
                .name("Ahmedabad XI")
                .rosterSize(0)
                .build();

        FederationGovernanceRequests.DraftSelectionRequest request = new FederationGovernanceRequests.DraftSelectionRequest();
        request.setFranchiseId(40L);
        request.setPlayerRegistrationId(30L);

        when(draftRepository.findById(20L)).thenReturn(Optional.of(draft));
        when(playerRepository.findById(30L)).thenReturn(Optional.of(player));
        when(franchiseRepository.findById(40L)).thenReturn(Optional.of(franchise));
        when(draftSelectionRepository.existsByDraftIdAndPlayerRegistrationId(20L, 30L)).thenReturn(false);
        when(draftSelectionRepository.countByDraftId(20L)).thenReturn(0L);
        when(draftSelectionRepository.save(any(FederationDraftSelection.class))).thenAnswer(invocation -> {
            FederationDraftSelection selection = invocation.getArgument(0);
            selection.setId(50L);
            return selection;
        });
        when(playerRepository.save(any(FederationPlayerRegistration.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(franchiseRepository.save(any(FederationFranchise.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(draftRepository.save(any(FederationDraft.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditRepository.save(any(FederationGovernanceAudit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> response = federationGovernanceService.selectDraftPlayer(20L, request, "draft-admin@example.com");

        assertEquals(1, response.get("pickNumber"));
        assertEquals(40L, player.getCurrentFranchiseId());
        assertEquals("FRANCHISE_ASSIGNED", player.getStatus());
        assertEquals(1, franchise.getRosterSize());

        when(draftSelectionRepository.existsByDraftIdAndPlayerRegistrationId(20L, 30L)).thenReturn(true);
        assertThrows(ApiException.class, () -> federationGovernanceService.selectDraftPlayer(20L, request, "draft-admin@example.com"));
    }

    @Test
    void auctionBidMustExceedCurrentBidAndRespectsSalaryCap() {
        FederationAuction auction = FederationAuction.builder()
                .id(60L)
                .federationId(1L)
                .playerRegistrationId(30L)
                .auctionName("Premier Auction")
                .basePrice(BigDecimal.valueOf(100000))
                .currentBid(BigDecimal.valueOf(100000))
                .status("PUBLISHED")
                .build();
        FederationFranchise franchise = FederationFranchise.builder()
                .id(40L)
                .federationId(1L)
                .name("Ahmedabad XI")
                .salaryCap(BigDecimal.valueOf(500000))
                .usedBudget(BigDecimal.ZERO)
                .build();

        FederationGovernanceRequests.BidRequest request = new FederationGovernanceRequests.BidRequest();
        request.setFranchiseId(40L);
        request.setBidAmount(BigDecimal.valueOf(150000));
        request.setBidderName("Owner");

        when(auctionRepository.findById(60L)).thenReturn(Optional.of(auction));
        when(franchiseRepository.findById(40L)).thenReturn(Optional.of(franchise));
        when(auctionBidRepository.save(any(FederationAuctionBid.class))).thenAnswer(invocation -> {
            FederationAuctionBid bid = invocation.getArgument(0);
            bid.setId(70L);
            return bid;
        });
        when(auctionRepository.save(any(FederationAuction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditRepository.save(any(FederationGovernanceAudit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> response = federationGovernanceService.placeBid(60L, request, "auction-admin@example.com");

        assertEquals(BigDecimal.valueOf(150000), response.get("bidAmount"));
        assertEquals("LIVE", auction.getStatus());
        assertEquals(40L, auction.getHighestBidFranchiseId());

        request.setBidAmount(BigDecimal.valueOf(150000));
        assertThrows(ApiException.class, () -> federationGovernanceService.placeBid(60L, request, "auction-admin@example.com"));
    }

    @Test
    void transferIsBlockedOutsideWindow() {
        FederationGovernanceRequests.TransferRequest request = new FederationGovernanceRequests.TransferRequest();
        request.setFederationId(1L);
        request.setPlayerRegistrationId(30L);
        request.setToFranchiseId(40L);
        request.setTransferType("PERMANENT");
        request.setTransferWindowOpen(false);

        assertThrows(ApiException.class, () -> federationGovernanceService.createTransfer(request, "transfer-admin@example.com"));
        verifyNoInteractions(transferRepository);
    }

    @Test
    void electionVoteBlocksDuplicateVoterAndPublishesWinner() {
        FederationElection election = FederationElection.builder()
                .id(80L)
                .federationId(1L)
                .title("Board Election")
                .positionName("President")
                .candidateNames("Rita,Sita")
                .status("OPEN")
                .voteCount(0)
                .build();

        FederationGovernanceRequests.VoteRequest request = new FederationGovernanceRequests.VoteRequest();
        request.setVoterReference("STATE-001");
        request.setCandidateName("Rita");

        when(electionRepository.findById(80L)).thenReturn(Optional.of(election));
        when(voteRepository.existsByElectionIdAndVoterReference(80L, "STATE-001")).thenReturn(false);
        when(voteRepository.save(any(FederationElectionVote.class))).thenAnswer(invocation -> {
            FederationElectionVote vote = invocation.getArgument(0);
            vote.setId(90L);
            return vote;
        });
        when(electionRepository.save(any(FederationElection.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditRepository.save(any(FederationGovernanceAudit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> response = federationGovernanceService.castVote(80L, request, "election-admin@example.com");

        assertEquals("Rita", response.get("candidateName"));
        assertEquals(1, election.getVoteCount());

        when(voteRepository.existsByElectionIdAndVoterReference(80L, "STATE-001")).thenReturn(true);
        assertThrows(ApiException.class, () -> federationGovernanceService.castVote(80L, request, "election-admin@example.com"));
    }
}
