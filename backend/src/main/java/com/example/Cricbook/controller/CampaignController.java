package com.example.Cricbook.controller;

import com.example.Cricbook.dto.CampaignResponse;
import com.example.Cricbook.dto.CreateCampaignRequest;
import com.example.Cricbook.entity.NotificationAnalytics;
import com.example.Cricbook.service.CampaignService;
import com.example.Cricbook.service.NotificationAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/campaigns")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'CONTENT_ADMIN', 'OWNER') or hasAnyAuthority('CAMPAIGN_CREATE', 'PUSH_SEND', 'EMAIL_SEND')")
public class CampaignController {

    private final CampaignService campaignService;
    private final NotificationAnalyticsService analyticsService;

    @PostMapping
    public ResponseEntity<CampaignResponse> createCampaign(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody CreateCampaignRequest request) {
        return ResponseEntity.ok(campaignService.createCampaign(userDetails.getUsername(), request));
    }

    @GetMapping
    public ResponseEntity<List<CampaignResponse>> getCampaigns() {
        return ResponseEntity.ok(campaignService.getCampaigns());
    }

    @PostMapping("/{campaignId}/send")
    public ResponseEntity<CampaignResponse> sendCampaign(@PathVariable Long campaignId) {
        return ResponseEntity.ok(campaignService.sendCampaign(campaignId));
    }

    @GetMapping("/analytics/{campaignId}")
    public ResponseEntity<NotificationAnalytics> getCampaignAnalytics(@PathVariable Long campaignId) {
        return ResponseEntity.ok(analyticsService.getCampaignAnalytics(campaignId));
    }
}
