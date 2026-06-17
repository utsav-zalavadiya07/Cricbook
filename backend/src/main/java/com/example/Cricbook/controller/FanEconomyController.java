package com.example.Cricbook.controller;

import com.example.Cricbook.dto.FanEconomyRequests;
import com.example.Cricbook.service.FanEconomyService;
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
public class FanEconomyController {

    private static final String ADMIN_FAN_ECONOMY_ACCESS = "hasRole('SUPER_ADMIN') or hasRole('ADMIN') or hasAuthority('FAN_ECONOMY_ADMIN') or hasAuthority('ADMIN_GOVERNANCE_FULL') or hasAuthority('CONTENT_MODERATION_FULL') or hasAuthority('SYSTEM_CONFIG_EDIT')";

    private final FanEconomyService fanEconomyService;

    @GetMapping("/fan-economy/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard() {
        return ResponseEntity.ok(fanEconomyService.dashboard());
    }

    @GetMapping("/fan-economy/clubs")
    public ResponseEntity<List<Map<String, Object>>> fanClubs() {
        return ResponseEntity.ok(fanEconomyService.fanClubs());
    }

    @GetMapping("/fan-economy/collectibles")
    public ResponseEntity<List<Map<String, Object>>> collectibles() {
        return ResponseEntity.ok(fanEconomyService.collectibles());
    }

    @GetMapping("/fan-economy/predictions")
    public ResponseEntity<List<Map<String, Object>>> predictionGames() {
        return ResponseEntity.ok(fanEconomyService.predictionGames());
    }

    @GetMapping("/fan-economy/creators")
    public ResponseEntity<List<Map<String, Object>>> creators() {
        return ResponseEntity.ok(fanEconomyService.creators());
    }

    @GetMapping("/fan-economy/marketplace/listings")
    public ResponseEntity<List<Map<String, Object>>> marketplaceListings() {
        return ResponseEntity.ok(fanEconomyService.marketplaceListings());
    }

    @GetMapping("/fan-economy/watch-parties")
    public ResponseEntity<List<Map<String, Object>>> watchParties() {
        return ResponseEntity.ok(fanEconomyService.watchParties());
    }

    @PostMapping("/fan-economy/fans")
    public ResponseEntity<Map<String, Object>> createFanProfile(@RequestBody FanEconomyRequests.FanProfileRequest request,
                                                                Authentication authentication) {
        return ResponseEntity.ok(fanEconomyService.createFanProfile(request, actor(authentication)));
    }

    @PostMapping("/fan-economy/clubs")
    public ResponseEntity<Map<String, Object>> createFanClub(@RequestBody FanEconomyRequests.FanClubRequest request,
                                                             Authentication authentication) {
        return ResponseEntity.ok(fanEconomyService.createFanClub(request, actor(authentication)));
    }

    @PostMapping("/fan-economy/clubs/{fanClubId}/join")
    public ResponseEntity<Map<String, Object>> joinFanClub(@PathVariable Long fanClubId,
                                                           @RequestBody FanEconomyRequests.JoinFanClubRequest request,
                                                           Authentication authentication) {
        return ResponseEntity.ok(fanEconomyService.joinFanClub(fanClubId, request, actor(authentication)));
    }

    @PostMapping("/fan-economy/collectibles/{collectibleId}/claim")
    public ResponseEntity<Map<String, Object>> claimCollectible(@PathVariable Long collectibleId,
                                                                @RequestBody FanEconomyRequests.ClaimCollectibleRequest request,
                                                                Authentication authentication) {
        return ResponseEntity.ok(fanEconomyService.claimCollectible(collectibleId, request, actor(authentication)));
    }

    @PostMapping("/fan-economy/predictions/{gameId}/entries")
    public ResponseEntity<Map<String, Object>> submitPrediction(@PathVariable Long gameId,
                                                                @RequestBody FanEconomyRequests.PredictionEntryRequest request,
                                                                Authentication authentication) {
        return ResponseEntity.ok(fanEconomyService.submitPrediction(gameId, request, actor(authentication)));
    }

    @PostMapping("/fan-economy/creators")
    public ResponseEntity<Map<String, Object>> createCreator(@RequestBody FanEconomyRequests.CreatorProfileRequest request,
                                                             Authentication authentication) {
        return ResponseEntity.ok(fanEconomyService.createCreatorProfile(request, actor(authentication)));
    }

    @PostMapping("/fan-economy/creators/{creatorProfileId}/content")
    public ResponseEntity<Map<String, Object>> publishCreatorContent(@PathVariable Long creatorProfileId,
                                                                     @RequestBody FanEconomyRequests.CreatorContentRequest request,
                                                                     Authentication authentication) {
        return ResponseEntity.ok(fanEconomyService.publishCreatorContent(creatorProfileId, request, actor(authentication)));
    }

    @PostMapping("/fan-economy/marketplace/listings")
    public ResponseEntity<Map<String, Object>> createMarketplaceListing(@RequestBody FanEconomyRequests.MarketplaceListingRequest request,
                                                                        Authentication authentication) {
        return ResponseEntity.ok(fanEconomyService.createMarketplaceListing(request, actor(authentication)));
    }

    @PostMapping("/fan-economy/marketplace/listings/{listingId}/purchase")
    public ResponseEntity<Map<String, Object>> purchaseMarketplaceListing(@PathVariable Long listingId,
                                                                          @RequestBody FanEconomyRequests.MarketplacePurchaseRequest request,
                                                                          Authentication authentication) {
        return ResponseEntity.ok(fanEconomyService.purchaseMarketplaceListing(listingId, request, actor(authentication)));
    }

    @PostMapping("/fan-economy/watch-parties")
    public ResponseEntity<Map<String, Object>> createWatchParty(@RequestBody FanEconomyRequests.WatchPartyRequest request,
                                                                Authentication authentication) {
        return ResponseEntity.ok(fanEconomyService.createWatchParty(request, actor(authentication)));
    }

    @PostMapping("/fan-economy/watch-parties/{watchPartyId}/join")
    public ResponseEntity<Map<String, Object>> joinWatchParty(@PathVariable Long watchPartyId,
                                                              @RequestBody FanEconomyRequests.JoinWatchPartyRequest request,
                                                              Authentication authentication) {
        return ResponseEntity.ok(fanEconomyService.joinWatchParty(watchPartyId, request, actor(authentication)));
    }

    @GetMapping("/admin/fan-economy/dashboard")
    @PreAuthorize(ADMIN_FAN_ECONOMY_ACCESS)
    public ResponseEntity<Map<String, Object>> adminDashboard() {
        return ResponseEntity.ok(fanEconomyService.adminDashboard());
    }

    @GetMapping("/admin/fan-economy/fans")
    @PreAuthorize(ADMIN_FAN_ECONOMY_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> adminFans() {
        return ResponseEntity.ok(fanEconomyService.fans());
    }

    @PostMapping("/admin/fan-economy/fans/{fanProfileId}/status")
    @PreAuthorize(ADMIN_FAN_ECONOMY_ACCESS)
    public ResponseEntity<Map<String, Object>> updateFanStatus(@PathVariable Long fanProfileId,
                                                               @RequestParam String status,
                                                               Authentication authentication) {
        return ResponseEntity.ok(fanEconomyService.updateFanStatus(fanProfileId, status, actor(authentication)));
    }

    @PostMapping("/admin/fan-economy/collectibles")
    @PreAuthorize(ADMIN_FAN_ECONOMY_ACCESS)
    public ResponseEntity<Map<String, Object>> createCollectible(@RequestBody FanEconomyRequests.CollectibleRequest request,
                                                                 Authentication authentication) {
        return ResponseEntity.ok(fanEconomyService.createCollectible(request, actor(authentication)));
    }

    @PostMapping("/admin/fan-economy/predictions")
    @PreAuthorize(ADMIN_FAN_ECONOMY_ACCESS)
    public ResponseEntity<Map<String, Object>> createPredictionGame(@RequestBody FanEconomyRequests.PredictionGameRequest request,
                                                                    Authentication authentication) {
        return ResponseEntity.ok(fanEconomyService.createPredictionGame(request, actor(authentication)));
    }

    @PostMapping("/admin/fan-economy/prediction-entries/{entryId}/score")
    @PreAuthorize(ADMIN_FAN_ECONOMY_ACCESS)
    public ResponseEntity<Map<String, Object>> scorePrediction(@PathVariable Long entryId,
                                                               @RequestBody FanEconomyRequests.PredictionScoreRequest request,
                                                               Authentication authentication) {
        return ResponseEntity.ok(fanEconomyService.scorePrediction(entryId, request, actor(authentication)));
    }

    @GetMapping("/admin/fan-economy/audit")
    @PreAuthorize(ADMIN_FAN_ECONOMY_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> audit() {
        return ResponseEntity.ok(fanEconomyService.audits());
    }

    private String actor(Authentication authentication) {
        return authentication == null ? "anonymous" : authentication.getName();
    }
}
