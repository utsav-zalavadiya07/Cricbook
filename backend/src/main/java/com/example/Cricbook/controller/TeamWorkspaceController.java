package com.example.Cricbook.controller;

import com.example.Cricbook.dto.*;
import com.example.Cricbook.service.TeamWorkspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/team-workspaces")
@RequiredArgsConstructor
public class TeamWorkspaceController {
    private final TeamWorkspaceService teamWorkspaceService;

    @GetMapping("/{teamId}")
    public ResponseEntity<TeamWorkspaceResponse> getWorkspace(@AuthenticationPrincipal UserDetails userDetails, @PathVariable Long teamId) {
        return ResponseEntity.ok(teamWorkspaceService.getWorkspace(userDetails.getUsername(), teamId));
    }

    @PutMapping("/{teamId}")
    public ResponseEntity<TeamWorkspaceResponse> updateWorkspace(@AuthenticationPrincipal UserDetails userDetails, @PathVariable Long teamId, @RequestBody TeamWorkspaceRequest request) {
        return ResponseEntity.ok(teamWorkspaceService.updateWorkspace(userDetails.getUsername(), teamId, request));
    }

    @PostMapping("/{teamId}/availability")
    public ResponseEntity<TeamAvailabilityResponse> setAvailability(@AuthenticationPrincipal UserDetails userDetails, @PathVariable Long teamId, @RequestBody TeamAvailabilityRequest request) {
        return ResponseEntity.ok(teamWorkspaceService.setAvailability(userDetails.getUsername(), teamId, request));
    }

    @PostMapping("/{teamId}/lineup")
    public ResponseEntity<TeamLineupResponse> saveLineup(@AuthenticationPrincipal UserDetails userDetails, @PathVariable Long teamId, @RequestBody TeamLineupRequest request) {
        return ResponseEntity.ok(teamWorkspaceService.saveLineup(userDetails.getUsername(), teamId, request));
    }

    @PostMapping("/{teamId}/polls")
    public ResponseEntity<TeamPollResponse> createPoll(@AuthenticationPrincipal UserDetails userDetails, @PathVariable Long teamId, @RequestBody TeamPollRequest request) {
        return ResponseEntity.ok(teamWorkspaceService.createPoll(userDetails.getUsername(), teamId, request));
    }

    @PostMapping("/{teamId}/polls/{pollId}/vote")
    public ResponseEntity<TeamPollResponse> vote(@AuthenticationPrincipal UserDetails userDetails, @PathVariable Long teamId, @PathVariable Long pollId, @RequestBody TeamVoteRequest request) {
        return ResponseEntity.ok(teamWorkspaceService.vote(userDetails.getUsername(), teamId, pollId, request));
    }

    @PostMapping("/{teamId}/announcements")
    public ResponseEntity<TeamAnnouncementResponse> createAnnouncement(@AuthenticationPrincipal UserDetails userDetails, @PathVariable Long teamId, @RequestBody TeamAnnouncementRequest request) {
        return ResponseEntity.ok(teamWorkspaceService.createAnnouncement(userDetails.getUsername(), teamId, request));
    }

    @PostMapping("/{teamId}/finances")
    public ResponseEntity<TeamFinanceResponse> addFinance(@AuthenticationPrincipal UserDetails userDetails, @PathVariable Long teamId, @RequestBody TeamFinanceRequest request) {
        return ResponseEntity.ok(teamWorkspaceService.addFinance(userDetails.getUsername(), teamId, request));
    }

    @PostMapping("/{teamId}/attendance")
    public ResponseEntity<TeamAttendanceResponse> recordAttendance(@AuthenticationPrincipal UserDetails userDetails, @PathVariable Long teamId, @RequestBody TeamAttendanceRequest request) {
        return ResponseEntity.ok(teamWorkspaceService.recordAttendance(userDetails.getUsername(), teamId, request));
    }

    @GetMapping("/analytics")
    public ResponseEntity<TeamAnalyticsResponse> analytics(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(teamWorkspaceService.analytics(userDetails.getUsername()));
    }

    @GetMapping("/admin/analytics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TeamAnalyticsResponse> adminAnalytics(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(teamWorkspaceService.analytics(userDetails.getUsername()));
    }
}
