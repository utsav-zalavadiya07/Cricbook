package com.example.Cricbook.controller;

import com.example.Cricbook.dto.*;
import com.example.Cricbook.enums.TournamentFormat;
import com.example.Cricbook.enums.TournamentStatus;
import com.example.Cricbook.service.TournamentService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/tournaments")
@RequiredArgsConstructor
public class TournamentController {

    private final TournamentService tournamentService;

    @PostMapping
    public ResponseEntity<TournamentResponse> createTournament(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody CreateTournamentRequest request) {
        return ResponseEntity.ok(tournamentService.createTournament(userDetails.getUsername(), request));
    }

    @GetMapping
    public ResponseEntity<List<TournamentResponse>> getOpenTournaments(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) TournamentFormat format,
            @RequestParam(required = false) TournamentStatus status,
            @RequestParam(required = false) BigDecimal maxEntryFee,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        if (query != null || city != null || format != null || status != null || maxEntryFee != null || fromDate != null || toDate != null) {
            return ResponseEntity.ok(tournamentService.searchTournaments(query, city, format, status, maxEntryFee, fromDate, toDate));
        }
        return ResponseEntity.ok(tournamentService.getOpenTournaments());
    }

    @GetMapping("/mine")
    public ResponseEntity<List<TournamentResponse>> getMyTournaments(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(tournamentService.getMyTournaments(userDetails.getUsername()));
    }

    @GetMapping("/{tournamentId}")
    public ResponseEntity<TournamentResponse> getTournament(@PathVariable Long tournamentId) {
        return ResponseEntity.ok(tournamentService.getTournament(tournamentId));
    }

    @PostMapping("/{tournamentId}/status")
    public ResponseEntity<TournamentResponse> updateTournamentStatus(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long tournamentId,
            @RequestBody UpdateTournamentStatusRequest request) {
        return ResponseEntity.ok(tournamentService.updateTournamentStatus(userDetails.getUsername(), tournamentId, request));
    }

    @PostMapping("/{tournamentId}/registrations")
    public ResponseEntity<TournamentRegistrationResponse> registerTeam(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long tournamentId,
            @RequestBody TournamentRegistrationRequest request) {
        return ResponseEntity.ok(tournamentService.registerTeam(userDetails.getUsername(), tournamentId, request));
    }

    @GetMapping("/{tournamentId}/registrations")
    public ResponseEntity<List<TournamentRegistrationResponse>> getRegistrations(@PathVariable Long tournamentId) {
        return ResponseEntity.ok(tournamentService.getRegistrations(tournamentId));
    }

    @PostMapping("/{tournamentId}/registrations/{registrationId}/approve")
    public ResponseEntity<TournamentRegistrationResponse> approveRegistration(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long tournamentId,
            @PathVariable Long registrationId,
            @RequestBody(required = false) RegistrationDecisionRequest request) {
        return ResponseEntity.ok(tournamentService.approveRegistration(userDetails.getUsername(), tournamentId, registrationId, request));
    }

    @PostMapping("/{tournamentId}/registrations/{registrationId}/reject")
    public ResponseEntity<TournamentRegistrationResponse> rejectRegistration(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long tournamentId,
            @PathVariable Long registrationId,
            @RequestBody(required = false) RegistrationDecisionRequest request) {
        return ResponseEntity.ok(tournamentService.rejectRegistration(userDetails.getUsername(), tournamentId, registrationId, request));
    }

    @PostMapping("/{tournamentId}/registrations/{registrationId}/cancel")
    public ResponseEntity<TournamentRegistrationResponse> cancelRegistration(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long tournamentId,
            @PathVariable Long registrationId) {
        return ResponseEntity.ok(tournamentService.cancelRegistration(userDetails.getUsername(), tournamentId, registrationId));
    }

    @PostMapping("/{tournamentId}/fixtures/generate")
    public ResponseEntity<List<TournamentFixtureResponse>> generateFixtures(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long tournamentId) {
        return ResponseEntity.ok(tournamentService.generateFixtures(userDetails.getUsername(), tournamentId));
    }

    @PostMapping("/{tournamentId}/fixtures/regenerate")
    public ResponseEntity<List<TournamentFixtureResponse>> regenerateFixtures(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long tournamentId) {
        return ResponseEntity.ok(tournamentService.regenerateFixtures(userDetails.getUsername(), tournamentId));
    }

    @GetMapping("/{tournamentId}/fixtures")
    public ResponseEntity<List<TournamentFixtureResponse>> getFixtures(@PathVariable Long tournamentId) {
        return ResponseEntity.ok(tournamentService.getFixtures(tournamentId));
    }

    @PostMapping("/fixtures/{fixtureId}/result")
    public ResponseEntity<TournamentFixtureResponse> recordFixtureResult(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long fixtureId,
            @RequestBody FixtureResultRequest request) {
        return ResponseEntity.ok(tournamentService.recordFixtureResult(userDetails.getUsername(), fixtureId, request));
    }

    @PostMapping("/fixtures/{fixtureId}/schedule")
    public ResponseEntity<TournamentFixtureResponse> scheduleFixture(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long fixtureId,
            @RequestBody TournamentFixtureScheduleRequest request) {
        return ResponseEntity.ok(tournamentService.scheduleFixture(userDetails.getUsername(), fixtureId, request));
    }

    @GetMapping("/{tournamentId}/standings")
    public ResponseEntity<List<TournamentStandingResponse>> getStandings(@PathVariable Long tournamentId) {
        return ResponseEntity.ok(tournamentService.getStandings(tournamentId));
    }

    @PostMapping("/{tournamentId}/rewards")
    public ResponseEntity<TournamentRewardResponse> addReward(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long tournamentId,
            @RequestBody CreateTournamentRewardRequest request) {
        return ResponseEntity.ok(tournamentService.addReward(userDetails.getUsername(), tournamentId, request));
    }

    @GetMapping("/{tournamentId}/rewards")
    public ResponseEntity<List<TournamentRewardResponse>> getRewards(@PathVariable Long tournamentId) {
        return ResponseEntity.ok(tournamentService.getRewards(tournamentId));
    }

    @GetMapping("/{tournamentId}/dashboard")
    public ResponseEntity<TournamentDashboardResponse> getDashboard(@PathVariable Long tournamentId) {
        return ResponseEntity.ok(tournamentService.getDashboard(tournamentId));
    }
}
