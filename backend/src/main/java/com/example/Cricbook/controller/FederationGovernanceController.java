package com.example.Cricbook.controller;

import com.example.Cricbook.dto.FederationGovernanceRequests;
import com.example.Cricbook.service.FederationGovernanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class FederationGovernanceController {

    private static final String ADMIN_GOVERNANCE_ACCESS = "hasRole('SUPER_ADMIN') or hasRole('ADMIN') or hasAuthority('ADMIN_GOVERNANCE_READ') or hasAuthority('ADMIN_GOVERNANCE_FULL') or hasAuthority('ADMIN_CAREER_READ') or hasAuthority('ADMIN_CORPORATE') or hasAuthority('ANALYTICS_VIEW') or hasAuthority('SYSTEM_CONFIG_EDIT')";

    private final FederationGovernanceService federationGovernanceService;

    @GetMapping("/governance/federations")
    public ResponseEntity<List<Map<String, Object>>> publicFederations(@RequestParam(required = false) String sport,
                                                                       @RequestParam(required = false) String region) {
        return ResponseEntity.ok(federationGovernanceService.publicFederations(sport, region));
    }

    @GetMapping("/governance/rankings")
    public ResponseEntity<List<Map<String, Object>>> publicRankings(@RequestParam(required = false) String sport,
                                                                    @RequestParam(required = false) String type) {
        return ResponseEntity.ok(federationGovernanceService.publicRankings(sport, type));
    }

    @GetMapping("/admin/governance/dashboard")
    @PreAuthorize(ADMIN_GOVERNANCE_ACCESS)
    public ResponseEntity<Map<String, Object>> dashboard() {
        return ResponseEntity.ok(federationGovernanceService.dashboard());
    }

    @GetMapping("/admin/governance/bodies")
    @PreAuthorize(ADMIN_GOVERNANCE_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> bodies() {
        return ResponseEntity.ok(federationGovernanceService.bodies());
    }

    @PostMapping("/admin/governance/bodies")
    @PreAuthorize(ADMIN_GOVERNANCE_ACCESS)
    public ResponseEntity<Map<String, Object>> createBody(@RequestBody FederationGovernanceRequests.BodyRequest request,
                                                          Authentication authentication) {
        return ResponseEntity.ok(federationGovernanceService.createBody(request, actor(authentication)));
    }

    @PostMapping("/admin/governance/bodies/{id}/status")
    @PreAuthorize(ADMIN_GOVERNANCE_ACCESS)
    public ResponseEntity<Map<String, Object>> updateBodyStatus(@PathVariable Long id,
                                                                @RequestBody FederationGovernanceRequests.StatusRequest request,
                                                                Authentication authentication) {
        return ResponseEntity.ok(federationGovernanceService.updateBodyStatus(id, request, actor(authentication)));
    }

    @GetMapping("/admin/governance/franchises")
    @PreAuthorize(ADMIN_GOVERNANCE_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> franchises() {
        return ResponseEntity.ok(federationGovernanceService.franchises());
    }

    @PostMapping("/admin/governance/franchises")
    @PreAuthorize(ADMIN_GOVERNANCE_ACCESS)
    public ResponseEntity<Map<String, Object>> createFranchise(@RequestBody FederationGovernanceRequests.FranchiseRequest request,
                                                               Authentication authentication) {
        return ResponseEntity.ok(federationGovernanceService.createFranchise(request, actor(authentication)));
    }

    @GetMapping("/admin/governance/players")
    @PreAuthorize(ADMIN_GOVERNANCE_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> players() {
        return ResponseEntity.ok(federationGovernanceService.players());
    }

    @PostMapping("/admin/governance/players")
    @PreAuthorize(ADMIN_GOVERNANCE_ACCESS)
    public ResponseEntity<Map<String, Object>> registerPlayer(@RequestBody FederationGovernanceRequests.PlayerRegistrationRequest request,
                                                              Authentication authentication) {
        return ResponseEntity.ok(federationGovernanceService.registerPlayer(request, actor(authentication)));
    }

    @PostMapping("/admin/governance/players/{id}/verification")
    @PreAuthorize(ADMIN_GOVERNANCE_ACCESS)
    public ResponseEntity<Map<String, Object>> updatePlayerVerification(@PathVariable Long id,
                                                                        @RequestBody FederationGovernanceRequests.StatusRequest request,
                                                                        Authentication authentication) {
        return ResponseEntity.ok(federationGovernanceService.updatePlayerVerification(id, request, actor(authentication)));
    }

    @GetMapping("/admin/governance/drafts")
    @PreAuthorize(ADMIN_GOVERNANCE_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> drafts() {
        return ResponseEntity.ok(federationGovernanceService.drafts());
    }

    @PostMapping("/admin/governance/drafts")
    @PreAuthorize(ADMIN_GOVERNANCE_ACCESS)
    public ResponseEntity<Map<String, Object>> createDraft(@RequestBody FederationGovernanceRequests.DraftRequest request,
                                                           Authentication authentication) {
        return ResponseEntity.ok(federationGovernanceService.createDraft(request, actor(authentication)));
    }

    @PostMapping("/admin/governance/drafts/{id}/selections")
    @PreAuthorize(ADMIN_GOVERNANCE_ACCESS)
    public ResponseEntity<Map<String, Object>> selectDraftPlayer(@PathVariable Long id,
                                                                 @RequestBody FederationGovernanceRequests.DraftSelectionRequest request,
                                                                 Authentication authentication) {
        return ResponseEntity.ok(federationGovernanceService.selectDraftPlayer(id, request, actor(authentication)));
    }

    @GetMapping("/admin/governance/auctions")
    @PreAuthorize(ADMIN_GOVERNANCE_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> auctions() {
        return ResponseEntity.ok(federationGovernanceService.auctions());
    }

    @PostMapping("/admin/governance/auctions")
    @PreAuthorize(ADMIN_GOVERNANCE_ACCESS)
    public ResponseEntity<Map<String, Object>> createAuction(@RequestBody FederationGovernanceRequests.AuctionRequest request,
                                                             Authentication authentication) {
        return ResponseEntity.ok(federationGovernanceService.createAuction(request, actor(authentication)));
    }

    @PostMapping("/admin/governance/auctions/{id}/bids")
    @PreAuthorize(ADMIN_GOVERNANCE_ACCESS)
    public ResponseEntity<Map<String, Object>> placeBid(@PathVariable Long id,
                                                        @RequestBody FederationGovernanceRequests.BidRequest request,
                                                        Authentication authentication) {
        return ResponseEntity.ok(federationGovernanceService.placeBid(id, request, actor(authentication)));
    }

    @PostMapping("/admin/governance/auctions/{id}/complete")
    @PreAuthorize(ADMIN_GOVERNANCE_ACCESS)
    public ResponseEntity<Map<String, Object>> completeAuction(@PathVariable Long id,
                                                               @RequestBody FederationGovernanceRequests.StatusRequest request,
                                                               Authentication authentication) {
        return ResponseEntity.ok(federationGovernanceService.completeAuction(id, request, actor(authentication)));
    }

    @GetMapping("/admin/governance/transfers")
    @PreAuthorize(ADMIN_GOVERNANCE_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> transfers() {
        return ResponseEntity.ok(federationGovernanceService.transfers());
    }

    @PostMapping("/admin/governance/transfers")
    @PreAuthorize(ADMIN_GOVERNANCE_ACCESS)
    public ResponseEntity<Map<String, Object>> createTransfer(@RequestBody FederationGovernanceRequests.TransferRequest request,
                                                              Authentication authentication) {
        return ResponseEntity.ok(federationGovernanceService.createTransfer(request, actor(authentication)));
    }

    @PostMapping("/admin/governance/transfers/{id}/status")
    @PreAuthorize(ADMIN_GOVERNANCE_ACCESS)
    public ResponseEntity<Map<String, Object>> updateTransferStatus(@PathVariable Long id,
                                                                    @RequestBody FederationGovernanceRequests.StatusRequest request,
                                                                    Authentication authentication) {
        return ResponseEntity.ok(federationGovernanceService.updateTransferStatus(id, request, actor(authentication)));
    }

    @GetMapping("/admin/governance/licenses")
    @PreAuthorize(ADMIN_GOVERNANCE_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> licenses() {
        return ResponseEntity.ok(federationGovernanceService.licenses());
    }

    @PostMapping("/admin/governance/licenses")
    @PreAuthorize(ADMIN_GOVERNANCE_ACCESS)
    public ResponseEntity<Map<String, Object>> issueLicense(@RequestBody FederationGovernanceRequests.LicenseRequest request,
                                                            Authentication authentication) {
        return ResponseEntity.ok(federationGovernanceService.issueLicense(request, actor(authentication)));
    }

    @PostMapping("/admin/governance/licenses/{id}/status")
    @PreAuthorize(ADMIN_GOVERNANCE_ACCESS)
    public ResponseEntity<Map<String, Object>> updateLicenseStatus(@PathVariable Long id,
                                                                   @RequestBody FederationGovernanceRequests.StatusRequest request,
                                                                   Authentication authentication) {
        return ResponseEntity.ok(federationGovernanceService.updateLicenseStatus(id, request, actor(authentication)));
    }

    @GetMapping("/admin/governance/officials")
    @PreAuthorize(ADMIN_GOVERNANCE_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> officials() {
        return ResponseEntity.ok(federationGovernanceService.officials());
    }

    @PostMapping("/admin/governance/officials")
    @PreAuthorize(ADMIN_GOVERNANCE_ACCESS)
    public ResponseEntity<Map<String, Object>> registerOfficial(@RequestBody FederationGovernanceRequests.OfficialRequest request,
                                                                Authentication authentication) {
        return ResponseEntity.ok(federationGovernanceService.registerOfficial(request, actor(authentication)));
    }

    @PostMapping("/admin/governance/officials/{id}/status")
    @PreAuthorize(ADMIN_GOVERNANCE_ACCESS)
    public ResponseEntity<Map<String, Object>> updateOfficialStatus(@PathVariable Long id,
                                                                    @RequestBody FederationGovernanceRequests.StatusRequest request,
                                                                    Authentication authentication) {
        return ResponseEntity.ok(federationGovernanceService.updateOfficialStatus(id, request, actor(authentication)));
    }

    @GetMapping("/admin/governance/cases")
    @PreAuthorize(ADMIN_GOVERNANCE_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> disciplinaryCases() {
        return ResponseEntity.ok(federationGovernanceService.disciplinaryCases());
    }

    @PostMapping("/admin/governance/cases")
    @PreAuthorize(ADMIN_GOVERNANCE_ACCESS)
    public ResponseEntity<Map<String, Object>> openDisciplinaryCase(@RequestBody FederationGovernanceRequests.CaseRequest request,
                                                                    Authentication authentication) {
        return ResponseEntity.ok(federationGovernanceService.openDisciplinaryCase(request, actor(authentication)));
    }

    @PostMapping("/admin/governance/cases/{id}/resolve")
    @PreAuthorize(ADMIN_GOVERNANCE_ACCESS)
    public ResponseEntity<Map<String, Object>> resolveDisciplinaryCase(@PathVariable Long id,
                                                                       @RequestBody FederationGovernanceRequests.StatusRequest request,
                                                                       Authentication authentication) {
        return ResponseEntity.ok(federationGovernanceService.resolveDisciplinaryCase(id, request, actor(authentication)));
    }

    @GetMapping("/admin/governance/elections")
    @PreAuthorize(ADMIN_GOVERNANCE_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> elections() {
        return ResponseEntity.ok(federationGovernanceService.elections());
    }

    @PostMapping("/admin/governance/elections")
    @PreAuthorize(ADMIN_GOVERNANCE_ACCESS)
    public ResponseEntity<Map<String, Object>> createElection(@RequestBody FederationGovernanceRequests.ElectionRequest request,
                                                              Authentication authentication) {
        return ResponseEntity.ok(federationGovernanceService.createElection(request, actor(authentication)));
    }

    @PostMapping("/admin/governance/elections/{id}/status")
    @PreAuthorize(ADMIN_GOVERNANCE_ACCESS)
    public ResponseEntity<Map<String, Object>> updateElectionStatus(@PathVariable Long id,
                                                                    @RequestBody FederationGovernanceRequests.StatusRequest request,
                                                                    Authentication authentication) {
        return ResponseEntity.ok(federationGovernanceService.updateElectionStatus(id, request, actor(authentication)));
    }

    @PostMapping("/admin/governance/elections/{id}/votes")
    @PreAuthorize(ADMIN_GOVERNANCE_ACCESS)
    public ResponseEntity<Map<String, Object>> castVote(@PathVariable Long id,
                                                        @RequestBody FederationGovernanceRequests.VoteRequest request,
                                                        Authentication authentication) {
        return ResponseEntity.ok(federationGovernanceService.castVote(id, request, actor(authentication)));
    }

    @PostMapping("/admin/governance/elections/{id}/publish-results")
    @PreAuthorize(ADMIN_GOVERNANCE_ACCESS)
    public ResponseEntity<Map<String, Object>> publishElectionResults(@PathVariable Long id,
                                                                      Authentication authentication) {
        return ResponseEntity.ok(federationGovernanceService.publishElectionResults(id, actor(authentication)));
    }

    @GetMapping("/admin/governance/rankings")
    @PreAuthorize(ADMIN_GOVERNANCE_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> rankings() {
        return ResponseEntity.ok(federationGovernanceService.rankings());
    }

    @PostMapping("/admin/governance/rankings")
    @PreAuthorize(ADMIN_GOVERNANCE_ACCESS)
    public ResponseEntity<Map<String, Object>> upsertRanking(@RequestBody FederationGovernanceRequests.RankingRequest request,
                                                             Authentication authentication) {
        return ResponseEntity.ok(federationGovernanceService.upsertRanking(request, actor(authentication)));
    }

    @GetMapping("/admin/governance/integrity-alerts")
    @PreAuthorize(ADMIN_GOVERNANCE_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> integrityAlerts() {
        return ResponseEntity.ok(federationGovernanceService.integrityAlerts());
    }

    @PostMapping("/admin/governance/integrity-alerts")
    @PreAuthorize(ADMIN_GOVERNANCE_ACCESS)
    public ResponseEntity<Map<String, Object>> openIntegrityAlert(@RequestBody FederationGovernanceRequests.IntegrityAlertRequest request,
                                                                  Authentication authentication) {
        return ResponseEntity.ok(federationGovernanceService.openIntegrityAlert(request, actor(authentication)));
    }

    @PostMapping("/admin/governance/integrity-alerts/{id}/status")
    @PreAuthorize(ADMIN_GOVERNANCE_ACCESS)
    public ResponseEntity<Map<String, Object>> updateIntegrityAlertStatus(@PathVariable Long id,
                                                                          @RequestBody FederationGovernanceRequests.StatusRequest request,
                                                                          Authentication authentication) {
        return ResponseEntity.ok(federationGovernanceService.updateIntegrityAlertStatus(id, request, actor(authentication)));
    }

    @GetMapping("/admin/governance/audit")
    @PreAuthorize(ADMIN_GOVERNANCE_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> audits() {
        return ResponseEntity.ok(federationGovernanceService.audits());
    }

    private String actor(Authentication authentication) {
        return authentication == null ? "anonymous" : authentication.getName();
    }
}
