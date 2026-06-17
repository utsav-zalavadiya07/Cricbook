package com.example.Cricbook.controller;

import com.example.Cricbook.dto.EnterpriseRequests;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.service.EnterprisePlatformService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class EnterprisePlatformController {

    private final EnterprisePlatformService enterprisePlatformService;

    @GetMapping("/enterprise/dashboard")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> ownerDashboard(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(enterprisePlatformService.ownerDashboard(requireUsername(userDetails)));
    }

    @GetMapping("/enterprise/centers")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> ownerCenters(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(enterprisePlatformService.ownerCenters(requireUsername(userDetails)));
    }

    @GetMapping("/enterprise/staff")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> ownerStaff(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(enterprisePlatformService.ownerStaff(requireUsername(userDetails)));
    }

    @GetMapping("/enterprise/franchises")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> ownerFranchises(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(enterprisePlatformService.ownerFranchises(requireUsername(userDetails)));
    }

    @GetMapping("/enterprise/reports")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> ownerReports(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(enterprisePlatformService.ownerReports(requireUsername(userDetails)));
    }

    @GetMapping("/enterprise/governance")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> ownerGovernance(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(enterprisePlatformService.ownerGovernance(requireUsername(userDetails)));
    }

    @PostMapping("/enterprise/businesses")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> createOwnerBusiness(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody EnterpriseRequests.BusinessRequest request) {
        return ResponseEntity.ok(enterprisePlatformService.createOwnerBusiness(requireUsername(userDetails), request));
    }

    @PostMapping("/enterprise/centers")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> createOwnerCenter(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody EnterpriseRequests.CenterRequest request) {
        return ResponseEntity.ok(enterprisePlatformService.createOwnerCenter(requireUsername(userDetails), request));
    }

    @PostMapping("/enterprise/staff")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> createOwnerStaff(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody EnterpriseRequests.StaffAssignmentRequest request) {
        return ResponseEntity.ok(enterprisePlatformService.createOwnerStaffAssignment(requireUsername(userDetails), request));
    }

    @GetMapping("/admin/enterprise/dashboard")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('ANALYTICS_VIEW') or hasAuthority('OWNER_APPROVE')")
    public ResponseEntity<Map<String, Object>> adminDashboard() {
        return ResponseEntity.ok(enterprisePlatformService.adminDashboard());
    }

    @GetMapping("/admin/enterprise/centers")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('ANALYTICS_VIEW') or hasAuthority('OWNER_APPROVE')")
    public ResponseEntity<List<Map<String, Object>>> adminCenters() {
        return ResponseEntity.ok(enterprisePlatformService.adminCenters());
    }

    @GetMapping("/admin/enterprise/staff")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('ROLE_MANAGE') or hasAuthority('OWNER_APPROVE')")
    public ResponseEntity<List<Map<String, Object>>> adminStaff() {
        return ResponseEntity.ok(enterprisePlatformService.adminStaff());
    }

    @GetMapping("/admin/enterprise/franchises")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('ANALYTICS_VIEW') or hasAuthority('OWNER_APPROVE')")
    public ResponseEntity<List<Map<String, Object>>> adminFranchises() {
        return ResponseEntity.ok(enterprisePlatformService.adminFranchises());
    }

    @GetMapping("/admin/enterprise/reports")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('ANALYTICS_VIEW') or hasAuthority('REPORT_EXPORT')")
    public ResponseEntity<Map<String, Object>> adminReports() {
        return ResponseEntity.ok(enterprisePlatformService.adminReports());
    }

    @GetMapping("/admin/enterprise/governance")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('ANALYTICS_VIEW') or hasAuthority('ROLE_MANAGE')")
    public ResponseEntity<Map<String, Object>> adminGovernance() {
        return ResponseEntity.ok(enterprisePlatformService.adminGovernance());
    }

    @PostMapping("/admin/enterprise/businesses")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('OWNER_APPROVE')")
    public ResponseEntity<Map<String, Object>> createAdminBusiness(@RequestBody EnterpriseRequests.BusinessRequest request) {
        return ResponseEntity.ok(enterprisePlatformService.createAdminBusiness(request));
    }

    @PostMapping("/admin/enterprise/centers")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('OWNER_APPROVE')")
    public ResponseEntity<Map<String, Object>> createAdminCenter(@RequestBody EnterpriseRequests.CenterRequest request) {
        return ResponseEntity.ok(enterprisePlatformService.createAdminCenter(request));
    }

    @PostMapping("/admin/enterprise/staff")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('ROLE_MANAGE')")
    public ResponseEntity<Map<String, Object>> createAdminStaff(@RequestBody EnterpriseRequests.StaffAssignmentRequest request) {
        return ResponseEntity.ok(enterprisePlatformService.createAdminStaffAssignment(request));
    }

    @PostMapping("/admin/enterprise/franchises")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('OWNER_APPROVE')")
    public ResponseEntity<Map<String, Object>> createAdminFranchise(@RequestBody EnterpriseRequests.FranchiseRequest request) {
        return ResponseEntity.ok(enterprisePlatformService.createAdminFranchise(request));
    }

    private String requireUsername(UserDetails userDetails) {
        if (userDetails == null || userDetails.getUsername() == null || userDetails.getUsername().isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTH_REQUIRED", "Please sign in to access enterprise operations.");
        }
        return userDetails.getUsername();
    }
}
