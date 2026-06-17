package com.example.Cricbook.controller;

import com.example.Cricbook.dto.MembershipPlanResponse;
import com.example.Cricbook.dto.SubscribeMembershipRequest;
import com.example.Cricbook.dto.UserMembershipResponse;
import com.example.Cricbook.entity.MembershipPlan;
import com.example.Cricbook.service.MembershipService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/memberships")
@RequiredArgsConstructor
public class MembershipController {

    private final MembershipService membershipService;

    @GetMapping("/plans")
    public ResponseEntity<List<MembershipPlanResponse>> getPlans() {
        return ResponseEntity.ok(membershipService.getPlans());
    }

    @PostMapping("/plans")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MembershipPlanResponse> createPlan(@RequestBody MembershipPlan plan) {
        return ResponseEntity.ok(membershipService.createPlan(plan));
    }

    @PostMapping("/subscribe")
    public ResponseEntity<UserMembershipResponse> subscribe(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody SubscribeMembershipRequest request) {
        return ResponseEntity.ok(membershipService.subscribe(userDetails.getUsername(), request));
    }

    @GetMapping("/my")
    public ResponseEntity<List<UserMembershipResponse>> getMyMemberships(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(membershipService.getMyMemberships(userDetails.getUsername()));
    }

    @GetMapping("/active")
    public ResponseEntity<UserMembershipResponse> getActiveMembership(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(membershipService.getActiveMembership(userDetails.getUsername()));
    }
}
