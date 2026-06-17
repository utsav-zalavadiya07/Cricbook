package com.example.Cricbook.controller;

import com.example.Cricbook.dto.*;
import com.example.Cricbook.service.LiveScoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/v1/live-scores")
@RequiredArgsConstructor
public class LiveScoringController {

    private final LiveScoringService liveScoringService;

    @PostMapping
    public ResponseEntity<LiveScoreResponse> createLiveScore(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody CreateLiveScoreRequest request) {
        return ResponseEntity.ok(liveScoringService.createLiveScore(userDetails.getUsername(), request));
    }

    @PostMapping("/{scoreId}/start")
    public ResponseEntity<LiveScoreResponse> startMatch(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long scoreId) {
        return ResponseEntity.ok(liveScoringService.startMatch(userDetails.getUsername(), scoreId));
    }

    @GetMapping
    public ResponseEntity<List<LiveScoreResponse>> getLiveScores() {
        return ResponseEntity.ok(liveScoringService.getLiveScores());
    }

    @GetMapping("/archive")
    public ResponseEntity<List<LiveScoreResponse>> getScoreArchive() {
        return ResponseEntity.ok(liveScoringService.getScoreArchive());
    }

    @GetMapping("/{scoreId}")
    public ResponseEntity<LiveScoreDetailResponse> getScore(@PathVariable Long scoreId) {
        return ResponseEntity.ok(liveScoringService.getScore(scoreId));
    }

    @GetMapping(value = "/{scoreId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamScore(@PathVariable Long scoreId) {
        return liveScoringService.streamScore(scoreId);
    }

    @PostMapping("/{scoreId}/events")
    public ResponseEntity<LiveScoreDetailResponse> recordBall(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long scoreId,
            @RequestBody ScoreEventRequest request) {
        return ResponseEntity.ok(liveScoringService.recordBall(userDetails.getUsername(), scoreId, request));
    }

    @PostMapping("/{scoreId}/undo")
    public ResponseEntity<LiveScoreDetailResponse> undoBall(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long scoreId) {
        return ResponseEntity.ok(liveScoringService.undoBall(userDetails.getUsername(), scoreId));
    }

    @PutMapping("/{scoreId}/events/{eventId}")
    public ResponseEntity<LiveScoreDetailResponse> editBall(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long scoreId,
            @PathVariable Long eventId,
            @RequestBody ScoreEventRequest request) {
        return ResponseEntity.ok(liveScoringService.editBall(userDetails.getUsername(), scoreId, eventId, request));
    }

    @DeleteMapping("/{scoreId}/events/{eventId}")
    public ResponseEntity<LiveScoreDetailResponse> deleteBall(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long scoreId,
            @PathVariable Long eventId) {
        return ResponseEntity.ok(liveScoringService.deleteBall(userDetails.getUsername(), scoreId, eventId));
    }

    @PostMapping("/{scoreId}/pause")
    public ResponseEntity<LiveScoreResponse> pauseMatch(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long scoreId) {
        return ResponseEntity.ok(liveScoringService.pauseMatch(userDetails.getUsername(), scoreId));
    }

    @PostMapping("/{scoreId}/resume")
    public ResponseEntity<LiveScoreResponse> resumeMatch(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long scoreId) {
        return ResponseEntity.ok(liveScoringService.resumeMatch(userDetails.getUsername(), scoreId));
    }

    @PostMapping("/{scoreId}/cancel")
    public ResponseEntity<LiveScoreResponse> cancelMatch(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long scoreId) {
        return ResponseEntity.ok(liveScoringService.cancelMatch(userDetails.getUsername(), scoreId));
    }

    @PostMapping("/{scoreId}/complete")
    public ResponseEntity<LiveScoreResponse> completeScore(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long scoreId) {
        return ResponseEntity.ok(liveScoringService.completeScore(userDetails.getUsername(), scoreId));
    }

    @PostMapping("/{scoreId}/override")
    public ResponseEntity<LiveScoreResponse> overrideResult(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long scoreId,
            @RequestParam String winnerTeamName) {
        return ResponseEntity.ok(liveScoringService.overrideResult(userDetails.getUsername(), scoreId, winnerTeamName));
    }

    @PostMapping("/{scoreId}/lock")
    public ResponseEntity<LiveScoreResponse> lockMatch(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long scoreId) {
        return ResponseEntity.ok(liveScoringService.lockMatch(userDetails.getUsername(), scoreId));
    }

    @GetMapping("/{scoreId}/analytics")
    public ResponseEntity<MatchAnalyticsResponse> getAnalytics(@PathVariable Long scoreId) {
        return ResponseEntity.ok(liveScoringService.getAnalytics(scoreId));
    }
}
