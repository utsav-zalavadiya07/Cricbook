package com.example.Cricbook.controller;

import com.example.Cricbook.dto.StaffOperationsRequests;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.service.StaffOperationsService;
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
public class StaffOperationsController {

    private final StaffOperationsService staffOperationsService;

    @GetMapping("/staff-ops/dashboard")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> staffDashboard(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(staffOperationsService.staffDashboard(requireUsername(userDetails)));
    }

    @GetMapping("/staff-ops/schedule")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Map<String, Object>>> staffSchedule(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(staffOperationsService.staffSchedule(requireUsername(userDetails)));
    }

    @GetMapping("/staff-ops/bookings")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Map<String, Object>>> staffBookings(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(staffOperationsService.staffBookings(requireUsername(userDetails)));
    }

    @PostMapping("/staff-ops/check-ins")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> staffCheckIn(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody StaffOperationsRequests.CheckInRequest request) {
        return ResponseEntity.ok(staffOperationsService.staffCheckIn(requireUsername(userDetails), request));
    }

    @GetMapping("/staff-ops/tasks")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Map<String, Object>>> staffTasks(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(staffOperationsService.staffTasks(requireUsername(userDetails)));
    }

    @PostMapping("/staff-ops/tasks")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> createStaffTask(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody StaffOperationsRequests.TaskRequest request) {
        return ResponseEntity.ok(staffOperationsService.createTask(requireUsername(userDetails), request));
    }

    @PatchMapping("/staff-ops/tasks/{id}/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> updateStaffTaskStatus(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestBody StaffOperationsRequests.StatusRequest request) {
        return ResponseEntity.ok(staffOperationsService.updateTaskStatus(requireUsername(userDetails), id, request));
    }

    @GetMapping("/staff-ops/maintenance")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Map<String, Object>>> staffMaintenance(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(staffOperationsService.staffMaintenanceTickets(requireUsername(userDetails)));
    }

    @PostMapping("/staff-ops/maintenance")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> createStaffMaintenance(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody StaffOperationsRequests.MaintenanceTicketRequest request) {
        return ResponseEntity.ok(staffOperationsService.createMaintenanceTicket(requireUsername(userDetails), request));
    }

    @PatchMapping("/staff-ops/maintenance/{id}/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> updateStaffMaintenanceStatus(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestBody StaffOperationsRequests.StatusRequest request) {
        return ResponseEntity.ok(staffOperationsService.updateMaintenanceStatus(requireUsername(userDetails), id, request));
    }

    @PostMapping("/staff-ops/attendance/start")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> startStaffAttendance(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody(required = false) StaffOperationsRequests.AttendanceRequest request) {
        return ResponseEntity.ok(staffOperationsService.startAttendance(requireUsername(userDetails), request));
    }

    @PostMapping("/staff-ops/attendance/end")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> endStaffAttendance(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody(required = false) StaffOperationsRequests.AttendanceRequest request) {
        return ResponseEntity.ok(staffOperationsService.endAttendance(requireUsername(userDetails), request));
    }

    @GetMapping("/staff-ops/attendance")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Map<String, Object>>> staffAttendance(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(staffOperationsService.attendanceHistory(requireUsername(userDetails)));
    }

    @GetMapping("/staff-ops/equipment")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Map<String, Object>>> staffEquipment(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(staffOperationsService.staffEquipment(requireUsername(userDetails)));
    }

    @PostMapping("/staff-ops/equipment")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> createStaffEquipment(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody StaffOperationsRequests.EquipmentRequest request) {
        return ResponseEntity.ok(staffOperationsService.createEquipment(requireUsername(userDetails), request));
    }

    @PostMapping("/staff-ops/equipment/{id}/{action}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> updateStaffEquipment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @PathVariable String action,
            @RequestBody(required = false) StaffOperationsRequests.EquipmentRequest request) {
        return ResponseEntity.ok(staffOperationsService.updateEquipmentStatus(requireUsername(userDetails), id, action, request));
    }

    @PostMapping("/staff-ops/shifts")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> createStaffShift(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody StaffOperationsRequests.ShiftRequest request) {
        return ResponseEntity.ok(staffOperationsService.createShift(requireUsername(userDetails), request));
    }

    @GetMapping("/staff-ops/emergencies")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Map<String, Object>>> staffEmergencies(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(staffOperationsService.staffEmergencies(requireUsername(userDetails)));
    }

    @PostMapping("/staff-ops/emergencies")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> createStaffEmergency(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody StaffOperationsRequests.EmergencyAlertRequest request) {
        return ResponseEntity.ok(staffOperationsService.createEmergency(requireUsername(userDetails), request));
    }

    @PatchMapping("/staff-ops/emergencies/{id}/resolve")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> resolveStaffEmergency(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        return ResponseEntity.ok(staffOperationsService.resolveEmergency(requireUsername(userDetails), id));
    }

    @GetMapping("/staff-ops/analytics")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> staffAnalytics(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(staffOperationsService.staffAnalytics(requireUsername(userDetails)));
    }

    @GetMapping("/admin/workforce/dashboard")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('ROLE_MANAGE') or hasAuthority('OWNER_APPROVE')")
    public ResponseEntity<Map<String, Object>> adminWorkforceDashboard() {
        return ResponseEntity.ok(staffOperationsService.adminDashboard());
    }

    @GetMapping("/admin/workforce/tasks")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('ROLE_MANAGE') or hasAuthority('OWNER_APPROVE')")
    public ResponseEntity<List<Map<String, Object>>> adminTasks() {
        return ResponseEntity.ok(staffOperationsService.adminTasks());
    }

    @PostMapping("/admin/workforce/tasks")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('ROLE_MANAGE') or hasAuthority('OWNER_APPROVE')")
    public ResponseEntity<Map<String, Object>> createAdminTask(@RequestBody StaffOperationsRequests.TaskRequest request) {
        return ResponseEntity.ok(staffOperationsService.createAdminTask(request));
    }

    @GetMapping("/admin/workforce/maintenance")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('ROLE_MANAGE') or hasAuthority('OWNER_APPROVE')")
    public ResponseEntity<List<Map<String, Object>>> adminMaintenance() {
        return ResponseEntity.ok(staffOperationsService.adminMaintenanceTickets());
    }

    @PostMapping("/admin/workforce/maintenance")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('ROLE_MANAGE') or hasAuthority('OWNER_APPROVE')")
    public ResponseEntity<Map<String, Object>> createAdminMaintenance(@RequestBody StaffOperationsRequests.MaintenanceTicketRequest request) {
        return ResponseEntity.ok(staffOperationsService.createAdminMaintenanceTicket(request));
    }

    @GetMapping("/admin/workforce/attendance")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('ROLE_MANAGE') or hasAuthority('OWNER_APPROVE')")
    public ResponseEntity<List<Map<String, Object>>> adminAttendance() {
        return ResponseEntity.ok(staffOperationsService.adminAttendance());
    }

    @GetMapping("/admin/workforce/equipment")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('ROLE_MANAGE') or hasAuthority('OWNER_APPROVE')")
    public ResponseEntity<List<Map<String, Object>>> adminEquipment() {
        return ResponseEntity.ok(staffOperationsService.adminEquipment());
    }

    @PostMapping("/admin/workforce/equipment")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('ROLE_MANAGE') or hasAuthority('OWNER_APPROVE')")
    public ResponseEntity<Map<String, Object>> createAdminEquipment(@RequestBody StaffOperationsRequests.EquipmentRequest request) {
        return ResponseEntity.ok(staffOperationsService.createAdminEquipment(request));
    }

    @PostMapping("/admin/workforce/shifts")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('ROLE_MANAGE') or hasAuthority('OWNER_APPROVE')")
    public ResponseEntity<Map<String, Object>> createAdminShift(@RequestBody StaffOperationsRequests.ShiftRequest request) {
        return ResponseEntity.ok(staffOperationsService.createAdminShift(request));
    }

    @GetMapping("/admin/workforce/emergencies")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('ROLE_MANAGE') or hasAuthority('OWNER_APPROVE')")
    public ResponseEntity<List<Map<String, Object>>> adminEmergencies() {
        return ResponseEntity.ok(staffOperationsService.adminEmergencies());
    }

    @GetMapping("/admin/workforce/analytics")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('ANALYTICS_VIEW') or hasAuthority('ROLE_MANAGE')")
    public ResponseEntity<Map<String, Object>> adminAnalytics() {
        return ResponseEntity.ok(staffOperationsService.adminAnalytics());
    }

    @GetMapping("/admin/workforce/reports")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('REPORT_EXPORT') or hasAuthority('ROLE_MANAGE')")
    public ResponseEntity<Map<String, Object>> adminReports() {
        return ResponseEntity.ok(staffOperationsService.adminReports());
    }

    private String requireUsername(UserDetails userDetails) {
        if (userDetails == null || userDetails.getUsername() == null || userDetails.getUsername().isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTH_REQUIRED", "Please sign in to access staff operations.");
        }
        return userDetails.getUsername();
    }
}
