package com.example.Cricbook.controller;

import com.example.Cricbook.dto.SportsSuperAppRequests;
import com.example.Cricbook.service.SportsSuperAppService;
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
public class SportsSuperAppController {

    private static final String ADMIN_SUPER_APP_ACCESS = "hasRole('SUPER_ADMIN') or hasRole('ADMIN') or hasAuthority('ADMIN_SUPER_APP') or hasAuthority('ADMIN_GOVERNANCE_FULL') or hasAuthority('SYSTEM_CONFIG_EDIT')";

    private final SportsSuperAppService sportsSuperAppService;

    @GetMapping("/super-app/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard(Authentication authentication) {
        return ResponseEntity.ok(sportsSuperAppService.dashboard(actor(authentication)));
    }

    @PostMapping("/super-app/identities")
    public ResponseEntity<Map<String, Object>> createIdentity(@RequestBody SportsSuperAppRequests.IdentityRequest request,
                                                              Authentication authentication) {
        return ResponseEntity.ok(sportsSuperAppService.createIdentity(request, actor(authentication)));
    }

    @PostMapping("/super-app/identities/{identityId}/passport")
    public ResponseEntity<Map<String, Object>> generatePassport(@PathVariable Long identityId,
                                                                @RequestBody(required = false) SportsSuperAppRequests.PassportRequest request,
                                                                Authentication authentication) {
        return ResponseEntity.ok(sportsSuperAppService.generatePassport(identityId, request, actor(authentication)));
    }

    @GetMapping("/super-app/passports/{shareCode}")
    public ResponseEntity<Map<String, Object>> publicPassport(@PathVariable String shareCode) {
        return ResponseEntity.ok(sportsSuperAppService.publicPassport(shareCode));
    }

    @GetMapping("/super-app/search")
    public ResponseEntity<List<Map<String, Object>>> unifiedSearch(@RequestParam String query) {
        return ResponseEntity.ok(sportsSuperAppService.unifiedSearch(query));
    }

    @PostMapping("/super-app/identities/{identityId}/wallet/credit")
    public ResponseEntity<Map<String, Object>> creditWallet(@PathVariable Long identityId,
                                                            @RequestBody SportsSuperAppRequests.WalletLedgerRequest request,
                                                            Authentication authentication) {
        return ResponseEntity.ok(sportsSuperAppService.creditWallet(identityId, request, actor(authentication)));
    }

    @PostMapping("/super-app/identities/{identityId}/wallet/debit")
    public ResponseEntity<Map<String, Object>> debitWallet(@PathVariable Long identityId,
                                                           @RequestBody SportsSuperAppRequests.WalletLedgerRequest request,
                                                           Authentication authentication) {
        return ResponseEntity.ok(sportsSuperAppService.debitWallet(identityId, request, actor(authentication)));
    }

    @PostMapping("/super-app/identities/{identityId}/wallet/transfer")
    public ResponseEntity<Map<String, Object>> transferWallet(@PathVariable Long identityId,
                                                              @RequestBody SportsSuperAppRequests.WalletTransferRequest request,
                                                              Authentication authentication) {
        return ResponseEntity.ok(sportsSuperAppService.transferWallet(identityId, request, actor(authentication)));
    }

    @PostMapping("/super-app/identities/{identityId}/membership")
    public ResponseEntity<Map<String, Object>> createMembership(@PathVariable Long identityId,
                                                                @RequestBody SportsSuperAppRequests.MembershipRequest request,
                                                                Authentication authentication) {
        return ResponseEntity.ok(sportsSuperAppService.createMembership(identityId, request, actor(authentication)));
    }

    @PostMapping("/super-app/identities/{identityId}/rewards/earn")
    public ResponseEntity<Map<String, Object>> earnRewards(@PathVariable Long identityId,
                                                           @RequestBody SportsSuperAppRequests.RewardRequest request,
                                                           Authentication authentication) {
        return ResponseEntity.ok(sportsSuperAppService.earnRewards(identityId, request, actor(authentication)));
    }

    @PostMapping("/super-app/identities/{identityId}/rewards/redeem")
    public ResponseEntity<Map<String, Object>> redeemRewards(@PathVariable Long identityId,
                                                             @RequestBody SportsSuperAppRequests.RewardRequest request,
                                                             Authentication authentication) {
        return ResponseEntity.ok(sportsSuperAppService.redeemRewards(identityId, request, actor(authentication)));
    }

    @PostMapping("/super-app/identities/{identityId}/reputation")
    public ResponseEntity<Map<String, Object>> recordReputation(@PathVariable Long identityId,
                                                                @RequestBody SportsSuperAppRequests.ReputationRequest request,
                                                                Authentication authentication) {
        return ResponseEntity.ok(sportsSuperAppService.recordReputationEvent(identityId, request, actor(authentication)));
    }

    @PostMapping("/super-app/identities/{identityId}/timeline")
    public ResponseEntity<Map<String, Object>> addTimeline(@PathVariable Long identityId,
                                                           @RequestBody SportsSuperAppRequests.TimelineRequest request,
                                                           Authentication authentication) {
        return ResponseEntity.ok(sportsSuperAppService.addTimelineEvent(identityId, request, actor(authentication)));
    }

    @PostMapping("/super-app/identities/{identityId}/credits")
    public ResponseEntity<Map<String, Object>> recordCredits(@PathVariable Long identityId,
                                                             @RequestBody SportsSuperAppRequests.SportsCreditRequest request,
                                                             Authentication authentication) {
        return ResponseEntity.ok(sportsSuperAppService.recordSportsCredits(identityId, request, actor(authentication)));
    }

    @PostMapping("/super-app/family")
    public ResponseEntity<Map<String, Object>> createFamilyAccount(@RequestBody SportsSuperAppRequests.FamilyAccountRequest request,
                                                                   Authentication authentication) {
        return ResponseEntity.ok(sportsSuperAppService.createFamilyAccount(request, actor(authentication)));
    }

    @GetMapping("/admin/super-app/dashboard")
    @PreAuthorize(ADMIN_SUPER_APP_ACCESS)
    public ResponseEntity<Map<String, Object>> adminDashboard() {
        return ResponseEntity.ok(sportsSuperAppService.adminDashboard());
    }

    @GetMapping("/admin/super-app/identities")
    @PreAuthorize(ADMIN_SUPER_APP_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> identities() {
        return ResponseEntity.ok(sportsSuperAppService.identities());
    }

    @GetMapping("/admin/super-app/wallets")
    @PreAuthorize(ADMIN_SUPER_APP_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> wallets() {
        return ResponseEntity.ok(sportsSuperAppService.wallets());
    }

    @GetMapping("/admin/super-app/wallet-ledger")
    @PreAuthorize(ADMIN_SUPER_APP_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> walletLedger() {
        return ResponseEntity.ok(sportsSuperAppService.walletLedger());
    }

    @GetMapping("/admin/super-app/memberships")
    @PreAuthorize(ADMIN_SUPER_APP_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> memberships() {
        return ResponseEntity.ok(sportsSuperAppService.memberships());
    }

    @GetMapping("/admin/super-app/rewards")
    @PreAuthorize(ADMIN_SUPER_APP_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> rewards() {
        return ResponseEntity.ok(sportsSuperAppService.rewardAccounts());
    }

    @GetMapping("/admin/super-app/reputation")
    @PreAuthorize(ADMIN_SUPER_APP_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> reputation() {
        return ResponseEntity.ok(sportsSuperAppService.reputationEvents());
    }

    @GetMapping("/admin/super-app/timeline")
    @PreAuthorize(ADMIN_SUPER_APP_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> timeline() {
        return ResponseEntity.ok(sportsSuperAppService.timelineEvents());
    }

    @GetMapping("/admin/super-app/family")
    @PreAuthorize(ADMIN_SUPER_APP_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> family() {
        return ResponseEntity.ok(sportsSuperAppService.familyAccounts());
    }

    @GetMapping("/admin/super-app/audit")
    @PreAuthorize(ADMIN_SUPER_APP_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> audit() {
        return ResponseEntity.ok(sportsSuperAppService.audits());
    }

    private String actor(Authentication authentication) {
        return authentication == null ? "system" : authentication.getName();
    }
}
