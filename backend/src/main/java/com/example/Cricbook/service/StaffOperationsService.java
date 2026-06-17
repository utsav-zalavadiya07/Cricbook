package com.example.Cricbook.service;

import com.example.Cricbook.dto.StaffOperationsRequests;
import com.example.Cricbook.entity.*;
import com.example.Cricbook.enums.AttendanceStatus;
import com.example.Cricbook.enums.BookingStatus;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StaffOperationsService {

    private static final List<BookingStatus> ACTIVE_BOOKING_STATUSES = List.of(BookingStatus.CONFIRMED, BookingStatus.CHECKED_IN);
    private static final Pattern NUMBER_PATTERN = Pattern.compile("(\\d+)");

    private final UserRepository userRepository;
    private final EnterpriseStaffAssignmentRepository assignmentRepository;
    private final EnterpriseCenterRepository centerRepository;
    private final OwnerStaffRoleRepository ownerStaffRoleRepository;
    private final BookingRepository bookingRepository;
    private final TurfRepository turfRepository;
    private final TrustAttendanceRepository trustAttendanceRepository;
    private final StaffTaskRepository taskRepository;
    private final StaffMaintenanceTicketRepository maintenanceRepository;
    private final StaffAttendanceRecordRepository attendanceRepository;
    private final StaffEquipmentItemRepository equipmentRepository;
    private final StaffShiftRepository shiftRepository;
    private final StaffEmergencyAlertRepository emergencyRepository;
    private final AuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> staffDashboard(String username) {
        StaffScope scope = staffScope(username);
        return dashboard(scope);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> adminDashboard() {
        return dashboard(adminScope());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> staffSchedule(String username) {
        StaffScope scope = staffScope(username);
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> rows = new ArrayList<>();
        accessibleBookings(scope).stream()
                .filter(booking -> !booking.getBookingDate().isBefore(today))
                .sorted(Comparator.comparing(Booking::getBookingDate).thenComparing(booking -> booking.getSlot().getStartTime()))
                .limit(30)
                .forEach(booking -> rows.add(mapOf(
                        "type", "BOOKING",
                        "id", booking.getId(),
                        "title", booking.getTurf().getName(),
                        "date", booking.getBookingDate(),
                        "startTime", booking.getSlot().getStartTime(),
                        "endTime", booking.getSlot().getEndTime(),
                        "status", booking.getStatus(),
                        "playerName", booking.getUser().getName()
                )));
        shiftRepository.findByStaffIdAndShiftDateBetweenOrderByShiftDateAscStartTimeAsc(scope.staff().getId(), today, today.plusDays(7))
                .forEach(shift -> rows.add(shiftRow(shift)));
        taskRepository.findByStaffIdOrderByCreatedAtDesc(scope.staff().getId()).stream()
                .filter(task -> !"COMPLETED".equals(task.getStatus()) && !"CANCELLED".equals(task.getStatus()))
                .limit(20)
                .forEach(task -> rows.add(taskRow(task)));
        return rows;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> staffBookings(String username) {
        return accessibleBookings(staffScope(username)).stream()
                .sorted(Comparator.comparing(Booking::getBookingDate).reversed().thenComparing(Booking::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::bookingRow)
                .toList();
    }

    @Transactional
    public Map<String, Object> staffCheckIn(String username, StaffOperationsRequests.CheckInRequest request) {
        StaffScope scope = staffScope(username);
        Booking booking = resolveBooking(request);
        ensureBookingAccess(scope, booking);
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw ApiException.conflict("Only confirmed bookings can be checked in.");
        }
        if (trustAttendanceRepository.findByBookingId(booking.getId()).isPresent()) {
            throw ApiException.conflict("Duplicate check-in blocked for this booking.");
        }

        AttendanceStatus status = LocalDateTime.now().isAfter(LocalDateTime.of(booking.getBookingDate(), booking.getSlot().getStartTime()).plusMinutes(15))
                ? AttendanceStatus.LATE
                : AttendanceStatus.ARRIVED;
        TrustAttendance attendance = trustAttendanceRepository.save(TrustAttendance.builder()
                .booking(booking)
                .user(booking.getUser())
                .status(status)
                .checkinTime(LocalDateTime.now())
                .build());
        booking.setStatus(BookingStatus.CHECKED_IN);
        Booking saved = bookingRepository.save(booking);
        writeAudit(scope.staff().getEmail(), "STAFF_CHECK_IN", "Booking " + saved.getId());
        return mapOf(
                "booking", bookingRow(saved),
                "attendanceStatus", attendance.getStatus(),
                "checkedInAt", attendance.getCheckinTime(),
                "staffId", scope.staff().getId(),
                "staffName", scope.staff().getName()
        );
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> staffTasks(String username) {
        StaffScope scope = staffScope(username);
        return taskRepository.findByStaffIdOrderByCreatedAtDesc(scope.staff().getId()).stream().map(this::taskRow).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> adminTasks() {
        return taskRepository.findAll().stream()
                .sorted(Comparator.comparing(StaffTask::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::taskRow)
                .toList();
    }

    @Transactional
    public Map<String, Object> createTask(String username, StaffOperationsRequests.TaskRequest request) {
        StaffScope scope = staffScope(username);
        StaffTask task = saveTask(scope.staff().getEmail(), request, scope, false);
        return taskRow(task);
    }

    @Transactional
    public Map<String, Object> createAdminTask(StaffOperationsRequests.TaskRequest request) {
        StaffTask task = saveTask("ADMIN", request, adminScope(), true);
        return taskRow(task);
    }

    @Transactional
    public Map<String, Object> updateTaskStatus(String username, Long taskId, StaffOperationsRequests.StatusRequest request) {
        StaffScope scope = staffScope(username);
        StaffTask task = taskRepository.findById(taskId).orElseThrow(() -> ApiException.badRequest("Task not found."));
        ensureTaskAccess(scope, task);
        applyTaskStatus(task, normalizeStatus(request == null ? null : request.getStatus(), "IN_PROGRESS"));
        StaffTask saved = taskRepository.save(task);
        writeAudit(scope.staff().getEmail(), "STAFF_TASK_STATUS", "Task " + saved.getId() + " -> " + saved.getStatus());
        return taskRow(saved);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> staffMaintenanceTickets(String username) {
        StaffScope scope = staffScope(username);
        return maintenanceForScope(scope).stream().map(this::maintenanceRow).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> adminMaintenanceTickets() {
        return maintenanceRepository.findAll().stream()
                .sorted(Comparator.comparing(StaffMaintenanceTicket::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::maintenanceRow)
                .toList();
    }

    @Transactional
    public Map<String, Object> createMaintenanceTicket(String username, StaffOperationsRequests.MaintenanceTicketRequest request) {
        StaffScope scope = staffScope(username);
        StaffMaintenanceTicket ticket = saveMaintenanceTicket(scope.staff(), request, scope, false);
        return maintenanceRow(ticket);
    }

    @Transactional
    public Map<String, Object> createAdminMaintenanceTicket(StaffOperationsRequests.MaintenanceTicketRequest request) {
        StaffMaintenanceTicket ticket = saveMaintenanceTicket(null, request, adminScope(), true);
        return maintenanceRow(ticket);
    }

    @Transactional
    public Map<String, Object> updateMaintenanceStatus(String username, Long ticketId, StaffOperationsRequests.StatusRequest request) {
        StaffScope scope = staffScope(username);
        StaffMaintenanceTicket ticket = maintenanceRepository.findById(ticketId).orElseThrow(() -> ApiException.badRequest("Maintenance ticket not found."));
        ensureMaintenanceAccess(scope, ticket);
        applyMaintenanceStatus(ticket, normalizeStatus(request == null ? null : request.getStatus(), "IN_PROGRESS"));
        StaffMaintenanceTicket saved = maintenanceRepository.save(ticket);
        writeAudit(scope.staff().getEmail(), "STAFF_MAINTENANCE_STATUS", "Maintenance " + saved.getId() + " -> " + saved.getStatus());
        return maintenanceRow(saved);
    }

    @Transactional
    public Map<String, Object> startAttendance(String username, StaffOperationsRequests.AttendanceRequest request) {
        StaffScope scope = staffScope(username);
        EnterpriseCenter center = request == null || request.getCenterId() == null ? scope.centers().stream().findFirst().orElse(null) : requireCenter(request.getCenterId());
        if (center != null && !scope.admin() && scope.centerIds().contains(center.getId()) == false && scope.ownerIds().contains(center.getBusiness().getOwner().getId()) == false) {
            throw ApiException.forbidden("Staff is not assigned to this center.");
        }
        attendanceRepository.findFirstByStaffIdAndWorkDateAndShiftEndIsNullOrderByCreatedAtDesc(scope.staff().getId(), LocalDate.now())
                .ifPresent(existing -> {
                    throw ApiException.conflict("Shift already started for today.");
                });
        StaffAttendanceRecord attendance = attendanceRepository.save(StaffAttendanceRecord.builder()
                .staff(scope.staff())
                .center(center)
                .workDate(LocalDate.now())
                .shiftStart(LocalDateTime.now())
                .startLatitude(request == null ? null : request.getLatitude())
                .startLongitude(request == null ? null : request.getLongitude())
                .deviceId(request == null ? null : request.getDeviceId())
                .source(defaultString(request == null ? null : request.getSource(), "MOBILE"))
                .status("STARTED")
                .build());
        writeAudit(scope.staff().getEmail(), "STAFF_ATTENDANCE_START", "Attendance " + attendance.getId());
        return attendanceRow(attendance);
    }

    @Transactional
    public Map<String, Object> endAttendance(String username, StaffOperationsRequests.AttendanceRequest request) {
        StaffScope scope = staffScope(username);
        StaffAttendanceRecord attendance = attendanceRepository.findFirstByStaffIdAndWorkDateAndShiftEndIsNullOrderByCreatedAtDesc(scope.staff().getId(), LocalDate.now())
                .orElseThrow(() -> ApiException.badRequest("No active shift found for today."));
        attendance.setShiftEnd(LocalDateTime.now());
        attendance.setEndLatitude(request == null ? null : request.getLatitude());
        attendance.setEndLongitude(request == null ? null : request.getLongitude());
        attendance.setStatus("COMPLETED");
        StaffAttendanceRecord saved = attendanceRepository.save(attendance);
        writeAudit(scope.staff().getEmail(), "STAFF_ATTENDANCE_END", "Attendance " + saved.getId());
        return attendanceRow(saved);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> attendanceHistory(String username) {
        return attendanceRepository.findByStaffIdOrderByCreatedAtDesc(staffScope(username).staff().getId()).stream().map(this::attendanceRow).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> adminAttendance() {
        return attendanceRepository.findAll().stream()
                .sorted(Comparator.comparing(StaffAttendanceRecord::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::attendanceRow)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> staffEquipment(String username) {
        StaffScope scope = staffScope(username);
        return equipmentForScope(scope).stream().map(this::equipmentRow).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> adminEquipment() {
        return equipmentRepository.findAll().stream().map(this::equipmentRow).toList();
    }

    @Transactional
    public Map<String, Object> createEquipment(String username, StaffOperationsRequests.EquipmentRequest request) {
        StaffScope scope = staffScope(username);
        StaffEquipmentItem item = saveEquipment(request, scope, false);
        return equipmentRow(item);
    }

    @Transactional
    public Map<String, Object> createAdminEquipment(StaffOperationsRequests.EquipmentRequest request) {
        StaffEquipmentItem item = saveEquipment(request, adminScope(), true);
        return equipmentRow(item);
    }

    @Transactional
    public Map<String, Object> updateEquipmentStatus(String username, Long itemId, String action, StaffOperationsRequests.EquipmentRequest request) {
        StaffScope scope = staffScope(username);
        StaffEquipmentItem item = equipmentRepository.findById(itemId).orElseThrow(() -> ApiException.badRequest("Equipment item not found."));
        ensureEquipmentAccess(scope, item);
        String normalizedAction = normalizeStatus(action, "ISSUE");
        if ("ISSUE".equals(normalizedAction)) {
            item.setInUseCount(item.getInUseCount() + 1);
            item.setStatus("IN_USE");
        } else if ("RETURN".equals(normalizedAction)) {
            item.setInUseCount(Math.max(0, item.getInUseCount() - 1));
            item.setStatus(item.getDamagedCount() > 0 ? "DAMAGED" : "AVAILABLE");
        } else if ("DAMAGE".equals(normalizedAction)) {
            item.setDamagedCount(item.getDamagedCount() + 1);
            item.setStatus("DAMAGED");
        } else {
            item.setStatus(normalizedAction);
        }
        item.setAssignedTo(request == null ? item.getAssignedTo() : defaultString(request.getAssignedTo(), item.getAssignedTo()));
        item.setLastActionAt(LocalDateTime.now());
        StaffEquipmentItem saved = equipmentRepository.save(item);
        writeAudit(scope.staff().getEmail(), "STAFF_EQUIPMENT_" + normalizedAction, "Equipment " + saved.getId());
        return equipmentRow(saved);
    }

    @Transactional
    public Map<String, Object> createShift(String username, StaffOperationsRequests.ShiftRequest request) {
        StaffScope scope = staffScope(username);
        StaffShift shift = saveShift(request, scope, false);
        return shiftRow(shift);
    }

    @Transactional
    public Map<String, Object> createAdminShift(StaffOperationsRequests.ShiftRequest request) {
        StaffShift shift = saveShift(request, adminScope(), true);
        return shiftRow(shift);
    }

    @Transactional
    public Map<String, Object> createEmergency(String username, StaffOperationsRequests.EmergencyAlertRequest request) {
        StaffScope scope = staffScope(username);
        StaffEmergencyAlert alert = saveEmergency(scope.staff(), request, scope, false);
        return emergencyRow(alert);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> staffEmergencies(String username) {
        StaffScope scope = staffScope(username);
        return emergencyForScope(scope).stream().map(this::emergencyRow).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> adminEmergencies() {
        return emergencyRepository.findAll().stream()
                .sorted(Comparator.comparing(StaffEmergencyAlert::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::emergencyRow)
                .toList();
    }

    @Transactional
    public Map<String, Object> resolveEmergency(String username, Long alertId) {
        StaffScope scope = staffScope(username);
        StaffEmergencyAlert alert = emergencyRepository.findById(alertId).orElseThrow(() -> ApiException.badRequest("Emergency alert not found."));
        ensureEmergencyAccess(scope, alert);
        alert.setStatus("RESOLVED");
        alert.setResolvedAt(LocalDateTime.now());
        StaffEmergencyAlert saved = emergencyRepository.save(alert);
        writeAudit(scope.staff().getEmail(), "STAFF_EMERGENCY_RESOLVED", "Emergency " + saved.getId());
        return emergencyRow(saved);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> staffAnalytics(String username) {
        return analytics(staffScope(username));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> adminAnalytics() {
        return analytics(adminScope());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> adminReports() {
        StaffScope scope = adminScope();
        return mapOf(
                "generatedAt", LocalDateTime.now(),
                "attendanceReport", attendanceRepository.findAll().stream().map(this::attendanceRow).toList(),
                "taskReport", adminTasks(),
                "maintenanceReport", adminMaintenanceTickets(),
                "staffPerformanceReport", performanceRows(scope),
                "exports", List.of(
                        mapOf("format", "PDF", "fileName", "workforce-attendance-" + LocalDate.now() + ".pdf", "status", "READY"),
                        mapOf("format", "EXCEL", "fileName", "workforce-tasks-" + LocalDate.now() + ".xlsx", "status", "READY"),
                        mapOf("format", "CSV", "fileName", "workforce-maintenance-" + LocalDate.now() + ".csv", "status", "READY")
                )
        );
    }

    private Map<String, Object> dashboard(StaffScope scope) {
        List<Booking> bookings = accessibleBookings(scope);
        List<StaffTask> tasks = taskForScope(scope);
        List<StaffMaintenanceTicket> maintenance = maintenanceForScope(scope);
        List<StaffEmergencyAlert> emergencies = emergencyForScope(scope);
        long todayBookings = bookings.stream().filter(booking -> booking.getBookingDate().equals(LocalDate.now())).count();
        long pendingCheckIns = bookings.stream().filter(booking -> booking.getBookingDate().equals(LocalDate.now()) && booking.getStatus() == BookingStatus.CONFIRMED).count();
        long openTasks = tasks.stream().filter(task -> !"COMPLETED".equals(task.getStatus()) && !"CANCELLED".equals(task.getStatus())).count();
        long openMaintenance = maintenance.stream().filter(ticket -> !"RESOLVED".equals(ticket.getStatus()) && !"CLOSED".equals(ticket.getStatus())).count();
        return mapOf(
                "generatedAt", LocalDateTime.now(),
                "staff", scope.staff() == null ? null : mapOf("id", scope.staff().getId(), "name", scope.staff().getName(), "email", scope.staff().getEmail()),
                "centers", scope.centers().stream().map(this::centerRow).toList(),
                "kpis", mapOf(
                        "todayBookings", todayBookings,
                        "pendingCheckIns", pendingCheckIns,
                        "openTasks", openTasks,
                        "maintenanceIssues", openMaintenance,
                        "emergencyAlerts", emergencies.stream().filter(alert -> !"RESOLVED".equals(alert.getStatus())).count(),
                        "equipmentDamaged", equipmentForScope(scope).stream().mapToInt(StaffEquipmentItem::getDamagedCount).sum()
                ),
                "schedule", scope.staff() == null ? List.of() : staffScheduleRows(scope),
                "tasks", tasks.stream().limit(8).map(this::taskRow).toList(),
                "maintenance", maintenance.stream().limit(8).map(this::maintenanceRow).toList(),
                "announcements", List.of(
                        mapOf("title", "Shift readiness", "message", "Verify check-ins, equipment, and maintenance tickets before peak hours.", "priority", "MEDIUM"),
                        mapOf("title", "Emergency SOP", "message", "Escalate medical, fire, power, and security incidents immediately.", "priority", "HIGH")
                ),
                "analytics", analytics(scope)
        );
    }

    private List<Map<String, Object>> staffScheduleRows(StaffScope scope) {
        if (scope.staff() == null) {
            return List.of();
        }
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> rows = new ArrayList<>();
        shiftRepository.findByStaffIdAndShiftDateBetweenOrderByShiftDateAscStartTimeAsc(scope.staff().getId(), today, today.plusDays(7))
                .forEach(shift -> rows.add(shiftRow(shift)));
        accessibleBookings(scope).stream()
                .filter(booking -> !booking.getBookingDate().isBefore(today))
                .limit(8)
                .forEach(booking -> rows.add(mapOf("type", "BOOKING", "id", booking.getId(), "title", booking.getTurf().getName(), "date", booking.getBookingDate(), "status", booking.getStatus())));
        return rows;
    }

    private Map<String, Object> analytics(StaffScope scope) {
        List<StaffTask> tasks = taskForScope(scope);
        List<StaffMaintenanceTicket> maintenance = maintenanceForScope(scope);
        List<StaffAttendanceRecord> attendance = scope.admin()
                ? attendanceRepository.findAll()
                : scope.staff() == null ? List.of() : attendanceRepository.findByStaffIdOrderByCreatedAtDesc(scope.staff().getId());
        double attendancePercent = percent(attendance.stream().filter(record -> "COMPLETED".equals(record.getStatus()) || "STARTED".equals(record.getStatus())).count(), Math.max(attendance.size(), 1));
        double taskCompletion = percent(tasks.stream().filter(task -> "COMPLETED".equals(task.getStatus())).count(), Math.max(tasks.size(), 1));
        double maintenanceResolution = percent(maintenance.stream().filter(ticket -> "RESOLVED".equals(ticket.getStatus()) || "CLOSED".equals(ticket.getStatus())).count(), Math.max(maintenance.size(), 1));
        return mapOf(
                "attendancePercent", attendancePercent,
                "taskCompletionPercent", taskCompletion,
                "issueResolutionPercent", maintenanceResolution,
                "utilizationPercent", Math.min(100, Math.round((attendancePercent + taskCompletion + maintenanceResolution) / 3)),
                "performanceScore", Math.min(100, Math.round(taskCompletion * 0.45 + attendancePercent * 0.35 + maintenanceResolution * 0.20)),
                "topPerformers", performanceRows(scope).stream().limit(5).toList()
        );
    }

    private List<Map<String, Object>> performanceRows(StaffScope scope) {
        List<User> staffUsers = scope.admin()
                ? assignmentRepository.findAll().stream().map(EnterpriseStaffAssignment::getStaff).distinct().toList()
                : scope.staff() == null ? List.of() : List.of(scope.staff());
        return staffUsers.stream().map(user -> {
            List<StaffTask> tasks = taskRepository.findByStaffIdOrderByCreatedAtDesc(user.getId());
            List<StaffAttendanceRecord> attendance = attendanceRepository.findByStaffIdOrderByCreatedAtDesc(user.getId());
            double completion = percent(tasks.stream().filter(task -> "COMPLETED".equals(task.getStatus())).count(), Math.max(tasks.size(), 1));
            double attendanceScore = percent(attendance.stream().filter(record -> "COMPLETED".equals(record.getStatus()) || "STARTED".equals(record.getStatus())).count(), Math.max(attendance.size(), 1));
            long score = Math.round(completion * 0.6 + attendanceScore * 0.4);
            return mapOf("staffId", user.getId(), "staffName", user.getName(), "tasks", tasks.size(), "attendanceRecords", attendance.size(), "performanceScore", score);
        }).sorted((left, right) -> Long.compare(toLong(right.get("performanceScore")), toLong(left.get("performanceScore")))).toList();
    }

    private StaffTask saveTask(String actor, StaffOperationsRequests.TaskRequest request, StaffScope scope, boolean admin) {
        if (request == null || isBlank(request.getTitle())) {
            throw ApiException.badRequest("Task title is required.");
        }
        EnterpriseCenter center = request.getCenterId() == null ? scope.centers().stream().findFirst().orElse(null) : requireCenter(request.getCenterId());
        if (!admin && center != null && !canAccessCenter(scope, center)) {
            throw ApiException.forbidden("Staff cannot create tasks for this center.");
        }
        User staff = request.getStaffId() == null ? scope.staff() : userRepository.findById(request.getStaffId()).orElseThrow(() -> ApiException.badRequest("Staff user not found."));
        EnterpriseStaffAssignment assignment = request.getAssignmentId() == null ? null : assignmentRepository.findById(request.getAssignmentId()).orElseThrow(() -> ApiException.badRequest("Staff assignment not found."));
        Booking booking = request.getBookingId() == null ? null : bookingRepository.findById(request.getBookingId()).orElseThrow(() -> ApiException.badRequest("Booking not found."));
        StaffTask task = taskRepository.save(StaffTask.builder()
                .center(center)
                .assignment(assignment)
                .staff(staff)
                .booking(booking)
                .title(request.getTitle().trim())
                .description(defaultString(request.getDescription(), ""))
                .category(defaultString(request.getCategory(), "OPERATIONS"))
                .priority(defaultString(request.getPriority(), "MEDIUM").toUpperCase(Locale.ROOT))
                .status("PENDING")
                .createdBy(actor)
                .dueAt(request.getDueAt())
                .build());
        writeAudit(actor, "STAFF_TASK_CREATED", "Task " + task.getId());
        return task;
    }

    private StaffMaintenanceTicket saveMaintenanceTicket(User reporter, StaffOperationsRequests.MaintenanceTicketRequest request, StaffScope scope, boolean admin) {
        if (request == null || isBlank(request.getTitle())) {
            throw ApiException.badRequest("Maintenance title is required.");
        }
        EnterpriseCenter center = request.getCenterId() == null ? scope.centers().stream().findFirst().orElse(null) : requireCenter(request.getCenterId());
        if (!admin && center != null && !canAccessCenter(scope, center)) {
            throw ApiException.forbidden("Staff cannot create maintenance tickets for this center.");
        }
        Turf turf = request.getTurfId() == null ? null : turfRepository.findById(request.getTurfId()).orElseThrow(() -> ApiException.badRequest("Turf not found."));
        User assigned = request.getAssignedStaffId() == null ? reporter : userRepository.findById(request.getAssignedStaffId()).orElseThrow(() -> ApiException.badRequest("Assigned staff not found."));
        StaffMaintenanceTicket ticket = maintenanceRepository.save(StaffMaintenanceTicket.builder()
                .center(center)
                .turf(turf)
                .assignedStaff(assigned)
                .reporter(reporter)
                .title(request.getTitle().trim())
                .description(defaultString(request.getDescription(), ""))
                .category(defaultString(request.getCategory(), "GROUND"))
                .priority(defaultString(request.getPriority(), "MEDIUM").toUpperCase(Locale.ROOT))
                .status("OPEN")
                .mediaUrls(request.getMediaUrls() == null ? "" : String.join(",", request.getMediaUrls()))
                .slaDueAt(LocalDateTime.now().plusHours(slaHours(defaultString(request.getPriority(), "MEDIUM"))))
                .build());
        writeAudit(reporter == null ? "ADMIN" : reporter.getEmail(), "STAFF_MAINTENANCE_CREATED", "Maintenance " + ticket.getId());
        return ticket;
    }

    private StaffEquipmentItem saveEquipment(StaffOperationsRequests.EquipmentRequest request, StaffScope scope, boolean admin) {
        if (request == null || isBlank(request.getName())) {
            throw ApiException.badRequest("Equipment name is required.");
        }
        EnterpriseCenter center = request.getCenterId() == null ? scope.centers().stream().findFirst().orElse(null) : requireCenter(request.getCenterId());
        if (!admin && center != null && !canAccessCenter(scope, center)) {
            throw ApiException.forbidden("Staff cannot update equipment for this center.");
        }
        Turf turf = request.getTurfId() == null ? null : turfRepository.findById(request.getTurfId()).orElse(null);
        StaffEquipmentItem item = equipmentRepository.save(StaffEquipmentItem.builder()
                .center(center)
                .turf(turf)
                .name(request.getName().trim())
                .equipmentType(defaultString(request.getEquipmentType(), "CRICKET_KIT"))
                .status("AVAILABLE")
                .stockCount(request.getStockCount() == null ? 1 : Math.max(0, request.getStockCount()))
                .inUseCount(0)
                .damagedCount(0)
                .assignedTo(request.getAssignedTo())
                .lastActionAt(LocalDateTime.now())
                .build());
        writeAudit(scope.staff() == null ? "ADMIN" : scope.staff().getEmail(), "STAFF_EQUIPMENT_CREATED", "Equipment " + item.getId());
        return item;
    }

    private StaffShift saveShift(StaffOperationsRequests.ShiftRequest request, StaffScope scope, boolean admin) {
        if (request == null || request.getStaffId() == null) {
            throw ApiException.badRequest("Staff ID is required for shift assignment.");
        }
        User staff = userRepository.findById(request.getStaffId()).orElseThrow(() -> ApiException.badRequest("Staff user not found."));
        EnterpriseCenter center = request.getCenterId() == null ? null : requireCenter(request.getCenterId());
        if (!admin && center != null && !canAccessCenter(scope, center)) {
            throw ApiException.forbidden("Staff cannot create shifts for this center.");
        }
        StaffShift shift = shiftRepository.save(StaffShift.builder()
                .staff(staff)
                .center(center)
                .shiftDate(request.getShiftDate() == null ? LocalDate.now() : request.getShiftDate())
                .startTime(request.getStartTime() == null ? LocalTime.of(9, 0) : request.getStartTime())
                .endTime(request.getEndTime() == null ? LocalTime.of(18, 0) : request.getEndTime())
                .status(defaultString(request.getStatus(), "ASSIGNED"))
                .notes(defaultString(request.getNotes(), ""))
                .build());
        writeAudit(scope.staff() == null ? "ADMIN" : scope.staff().getEmail(), "STAFF_SHIFT_CREATED", "Shift " + shift.getId());
        return shift;
    }

    private StaffEmergencyAlert saveEmergency(User staff, StaffOperationsRequests.EmergencyAlertRequest request, StaffScope scope, boolean admin) {
        if (request == null || isBlank(request.getType())) {
            throw ApiException.badRequest("Emergency type is required.");
        }
        EnterpriseCenter center = request.getCenterId() == null ? scope.centers().stream().findFirst().orElse(null) : requireCenter(request.getCenterId());
        if (!admin && center != null && !canAccessCenter(scope, center)) {
            throw ApiException.forbidden("Staff cannot raise emergency alerts for this center.");
        }
        Turf turf = request.getTurfId() == null ? null : turfRepository.findById(request.getTurfId()).orElse(null);
        StaffEmergencyAlert alert = emergencyRepository.save(StaffEmergencyAlert.builder()
                .staff(staff)
                .center(center)
                .turf(turf)
                .type(request.getType().toUpperCase(Locale.ROOT))
                .severity(defaultString(request.getSeverity(), "HIGH").toUpperCase(Locale.ROOT))
                .message(defaultString(request.getMessage(), "Emergency alert raised from staff operations."))
                .status("OPEN")
                .escalationLevel("CENTER_MANAGER")
                .build());
        writeAudit(staff == null ? "ADMIN" : staff.getEmail(), "STAFF_EMERGENCY_CREATED", "Emergency " + alert.getId());
        return alert;
    }

    private StaffScope staffScope(String username) {
        User staff = findUser(username);
        boolean admin = isAdmin(staff);
        if (admin) {
            StaffScope scope = adminScope();
            return new StaffScope(staff, true, scope.assignments(), scope.centers(), scope.ownerIds(), scope.centerIds());
        }
        List<EnterpriseStaffAssignment> assignments = assignmentRepository.findByStaffIdAndActiveTrue(staff.getId());
        Set<Long> ownerIds = assignments.stream().map(assignment -> assignment.getBusiness().getOwner().getId()).collect(Collectors.toCollection(LinkedHashSet::new));
        ownerStaffRoleRepository.findByStaffIdAndActiveTrue(staff.getId()).forEach(role -> ownerIds.add(role.getOwnerId()));
        List<EnterpriseCenter> centers = assignments.stream().map(EnterpriseStaffAssignment::getCenter).filter(Objects::nonNull).distinct().toList();
        Set<Long> centerIds = centers.stream().map(EnterpriseCenter::getId).collect(Collectors.toCollection(LinkedHashSet::new));
        return new StaffScope(staff, false, assignments, centers, ownerIds, centerIds);
    }

    private StaffScope adminScope() {
        List<EnterpriseStaffAssignment> assignments = assignmentRepository.findAll();
        List<EnterpriseCenter> centers = centerRepository.findAll();
        Set<Long> ownerIds = assignments.stream().map(assignment -> assignment.getBusiness().getOwner().getId()).collect(Collectors.toCollection(LinkedHashSet::new));
        ownerStaffRoleRepository.findAll().forEach(role -> ownerIds.add(role.getOwnerId()));
        Set<Long> centerIds = centers.stream().map(EnterpriseCenter::getId).collect(Collectors.toCollection(LinkedHashSet::new));
        return new StaffScope(null, true, assignments, centers, ownerIds, centerIds);
    }

    private List<Booking> accessibleBookings(StaffScope scope) {
        return bookingRepository.findAll().stream()
                .filter(booking -> scope.admin() || scope.ownerIds().contains(booking.getTurf().getOwner().getId()) || scope.centers().stream().anyMatch(center -> same(center.getCity(), booking.getTurf().getCity())))
                .filter(booking -> ACTIVE_BOOKING_STATUSES.contains(booking.getStatus()) || booking.getBookingDate().isAfter(LocalDate.now().minusDays(1)))
                .toList();
    }

    private List<StaffTask> taskForScope(StaffScope scope) {
        if (scope.admin()) {
            return taskRepository.findAll().stream()
                    .sorted(Comparator.comparing(StaffTask::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                    .toList();
        }
        List<StaffTask> rows = new ArrayList<>(scope.staff() == null ? List.of() : taskRepository.findByStaffIdOrderByCreatedAtDesc(scope.staff().getId()));
        if (!scope.centerIds().isEmpty()) {
            rows.addAll(taskRepository.findByCenterIdInOrderByCreatedAtDesc(new ArrayList<>(scope.centerIds())));
        }
        return rows.stream().collect(Collectors.toMap(StaffTask::getId, task -> task, (a, b) -> a, LinkedHashMap::new)).values().stream().toList();
    }

    private List<StaffMaintenanceTicket> maintenanceForScope(StaffScope scope) {
        if (scope.admin()) {
            return maintenanceRepository.findAll().stream()
                    .sorted(Comparator.comparing(StaffMaintenanceTicket::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                    .toList();
        }
        List<StaffMaintenanceTicket> rows = new ArrayList<>(scope.staff() == null ? List.of() : maintenanceRepository.findByAssignedStaffIdOrderByCreatedAtDesc(scope.staff().getId()));
        if (!scope.centerIds().isEmpty()) {
            rows.addAll(maintenanceRepository.findByCenterIdInOrderByCreatedAtDesc(new ArrayList<>(scope.centerIds())));
        }
        return rows.stream().collect(Collectors.toMap(StaffMaintenanceTicket::getId, ticket -> ticket, (a, b) -> a, LinkedHashMap::new)).values().stream().toList();
    }

    private List<StaffEquipmentItem> equipmentForScope(StaffScope scope) {
        if (scope.admin()) {
            return equipmentRepository.findAll().stream().sorted(Comparator.comparing(StaffEquipmentItem::getName)).toList();
        }
        if (!scope.centerIds().isEmpty()) {
            return equipmentRepository.findByCenterIdInOrderByNameAsc(new ArrayList<>(scope.centerIds()));
        }
        return List.of();
    }

    private List<StaffEmergencyAlert> emergencyForScope(StaffScope scope) {
        if (scope.admin()) {
            return emergencyRepository.findAll().stream()
                    .sorted(Comparator.comparing(StaffEmergencyAlert::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                    .toList();
        }
        List<StaffEmergencyAlert> rows = new ArrayList<>(scope.staff() == null ? List.of() : emergencyRepository.findByStaffIdOrderByCreatedAtDesc(scope.staff().getId()));
        if (!scope.centerIds().isEmpty()) {
            rows.addAll(emergencyRepository.findByCenterIdInOrderByCreatedAtDesc(new ArrayList<>(scope.centerIds())));
        }
        return rows.stream().collect(Collectors.toMap(StaffEmergencyAlert::getId, alert -> alert, (a, b) -> a, LinkedHashMap::new)).values().stream().toList();
    }

    private Booking resolveBooking(StaffOperationsRequests.CheckInRequest request) {
        if (request == null) {
            throw ApiException.badRequest("Booking identifier is required.");
        }
        Long bookingId = firstPresent(request.getBookingId(), parseId(request.getBookingCode()), parseId(request.getQrPayload()));
        if (bookingId != null) {
            return bookingRepository.findById(bookingId).orElseThrow(() -> ApiException.badRequest("Booking not found."));
        }
        if (!isBlank(request.getPhone())) {
            return bookingRepository.findAll().stream()
                    .filter(booking -> request.getPhone().equals(booking.getUser().getPhone()))
                    .filter(booking -> booking.getBookingDate().equals(LocalDate.now()))
                    .filter(booking -> booking.getStatus() == BookingStatus.CONFIRMED)
                    .findFirst()
                    .orElseThrow(() -> ApiException.badRequest("No confirmed booking found for this phone today."));
        }
        throw ApiException.badRequest("Booking ID, QR payload, or phone is required.");
    }

    private void applyTaskStatus(StaffTask task, String status) {
        task.setStatus(status);
        if ("ACCEPTED".equals(status)) {
            task.setAcceptedAt(LocalDateTime.now());
        } else if ("IN_PROGRESS".equals(status)) {
            task.setStartedAt(LocalDateTime.now());
        } else if ("COMPLETED".equals(status)) {
            task.setCompletedAt(LocalDateTime.now());
        } else if ("ESCALATED".equals(status)) {
            task.setEscalatedAt(LocalDateTime.now());
        }
    }

    private void applyMaintenanceStatus(StaffMaintenanceTicket ticket, String status) {
        ticket.setStatus(status);
        if ("IN_PROGRESS".equals(status)) {
            ticket.setStartedAt(LocalDateTime.now());
        } else if ("RESOLVED".equals(status)) {
            ticket.setResolvedAt(LocalDateTime.now());
        } else if ("CLOSED".equals(status)) {
            ticket.setClosedAt(LocalDateTime.now());
        }
    }

    private void ensureBookingAccess(StaffScope scope, Booking booking) {
        if (scope.admin()) {
            return;
        }
        if (scope.ownerIds().contains(booking.getTurf().getOwner().getId())) {
            return;
        }
        if (scope.centers().stream().anyMatch(center -> same(center.getCity(), booking.getTurf().getCity()))) {
            return;
        }
        throw ApiException.forbidden("Staff cannot access this booking.");
    }

    private void ensureTaskAccess(StaffScope scope, StaffTask task) {
        if (scope.admin()) {
            return;
        }
        if (task.getStaff() != null && scope.staff() != null && Objects.equals(task.getStaff().getId(), scope.staff().getId())) {
            return;
        }
        if (task.getCenter() != null && canAccessCenter(scope, task.getCenter())) {
            return;
        }
        throw ApiException.forbidden("Staff cannot access this task.");
    }

    private void ensureMaintenanceAccess(StaffScope scope, StaffMaintenanceTicket ticket) {
        if (scope.admin()) {
            return;
        }
        if (ticket.getAssignedStaff() != null && scope.staff() != null && Objects.equals(ticket.getAssignedStaff().getId(), scope.staff().getId())) {
            return;
        }
        if (ticket.getCenter() != null && canAccessCenter(scope, ticket.getCenter())) {
            return;
        }
        throw ApiException.forbidden("Staff cannot access this maintenance ticket.");
    }

    private void ensureEquipmentAccess(StaffScope scope, StaffEquipmentItem item) {
        if (!scope.admin() && item.getCenter() != null && !canAccessCenter(scope, item.getCenter())) {
            throw ApiException.forbidden("Staff cannot access this equipment item.");
        }
    }

    private void ensureEmergencyAccess(StaffScope scope, StaffEmergencyAlert alert) {
        if (scope.admin()) {
            return;
        }
        if (alert.getStaff() != null && scope.staff() != null && Objects.equals(alert.getStaff().getId(), scope.staff().getId())) {
            return;
        }
        if (alert.getCenter() != null && canAccessCenter(scope, alert.getCenter())) {
            return;
        }
        throw ApiException.forbidden("Staff cannot access this emergency alert.");
    }

    private boolean canAccessCenter(StaffScope scope, EnterpriseCenter center) {
        return scope.admin()
                || scope.centerIds().contains(center.getId())
                || scope.ownerIds().contains(center.getBusiness().getOwner().getId());
    }

    private EnterpriseCenter requireCenter(Long centerId) {
        return centerRepository.findById(centerId).orElseThrow(() -> ApiException.badRequest("Center not found."));
    }

    private User findUser(String username) {
        if (isBlank(username)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTH_REQUIRED", "Please sign in to access staff operations.");
        }
        return userRepository.findByEmailOrPhone(username).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "AUTH_REQUIRED", "Staff user not found."));
    }

    private boolean isAdmin(User user) {
        return user.getRoles().stream().anyMatch(role -> role.getName().name().contains("ADMIN"));
    }

    private Map<String, Object> bookingRow(Booking booking) {
        Optional<TrustAttendance> attendance = trustAttendanceRepository.findByBookingId(booking.getId());
        return mapOf(
                "id", booking.getId(),
                "bookingCode", "B-" + booking.getId(),
                "playerId", booking.getUser().getId(),
                "playerName", booking.getUser().getName(),
                "playerPhone", booking.getUser().getPhone(),
                "turfId", booking.getTurf().getId(),
                "turfName", booking.getTurf().getName(),
                "city", booking.getTurf().getCity(),
                "bookingDate", booking.getBookingDate(),
                "startTime", booking.getSlot().getStartTime(),
                "endTime", booking.getSlot().getEndTime(),
                "status", booking.getStatus(),
                "amount", booking.getAmount(),
                "attendanceStatus", attendance.map(item -> item.getStatus().name()).orElse(null),
                "checkedInAt", attendance.map(TrustAttendance::getCheckinTime).orElse(null)
        );
    }

    private Map<String, Object> taskRow(StaffTask task) {
        return mapOf(
                "id", task.getId(),
                "title", task.getTitle(),
                "description", task.getDescription(),
                "category", task.getCategory(),
                "priority", task.getPriority(),
                "status", task.getStatus(),
                "centerId", task.getCenter() == null ? null : task.getCenter().getId(),
                "centerName", task.getCenter() == null ? "All centers" : task.getCenter().getName(),
                "staffId", task.getStaff() == null ? null : task.getStaff().getId(),
                "staffName", task.getStaff() == null ? "Unassigned" : task.getStaff().getName(),
                "bookingId", task.getBooking() == null ? null : task.getBooking().getId(),
                "dueAt", task.getDueAt(),
                "createdAt", task.getCreatedAt()
        );
    }

    private Map<String, Object> maintenanceRow(StaffMaintenanceTicket ticket) {
        return mapOf(
                "id", ticket.getId(),
                "title", ticket.getTitle(),
                "description", ticket.getDescription(),
                "category", ticket.getCategory(),
                "priority", ticket.getPriority(),
                "status", ticket.getStatus(),
                "centerId", ticket.getCenter() == null ? null : ticket.getCenter().getId(),
                "centerName", ticket.getCenter() == null ? "All centers" : ticket.getCenter().getName(),
                "turfName", ticket.getTurf() == null ? null : ticket.getTurf().getName(),
                "assignedStaffName", ticket.getAssignedStaff() == null ? "Unassigned" : ticket.getAssignedStaff().getName(),
                "mediaUrls", ticket.getMediaUrls(),
                "slaDueAt", ticket.getSlaDueAt(),
                "createdAt", ticket.getCreatedAt()
        );
    }

    private Map<String, Object> attendanceRow(StaffAttendanceRecord attendance) {
        long durationMinutes = attendance.getShiftStart() == null ? 0 : Duration.between(attendance.getShiftStart(), attendance.getShiftEnd() == null ? LocalDateTime.now() : attendance.getShiftEnd()).toMinutes();
        return mapOf(
                "id", attendance.getId(),
                "staffId", attendance.getStaff().getId(),
                "staffName", attendance.getStaff().getName(),
                "centerName", attendance.getCenter() == null ? "Unassigned" : attendance.getCenter().getName(),
                "workDate", attendance.getWorkDate(),
                "shiftStart", attendance.getShiftStart(),
                "shiftEnd", attendance.getShiftEnd(),
                "durationMinutes", durationMinutes,
                "source", attendance.getSource(),
                "status", attendance.getStatus()
        );
    }

    private Map<String, Object> equipmentRow(StaffEquipmentItem item) {
        return mapOf(
                "id", item.getId(),
                "name", item.getName(),
                "equipmentType", item.getEquipmentType(),
                "status", item.getStatus(),
                "centerName", item.getCenter() == null ? "All centers" : item.getCenter().getName(),
                "turfName", item.getTurf() == null ? null : item.getTurf().getName(),
                "stockCount", item.getStockCount(),
                "inUseCount", item.getInUseCount(),
                "damagedCount", item.getDamagedCount(),
                "assignedTo", item.getAssignedTo(),
                "lastActionAt", item.getLastActionAt()
        );
    }

    private Map<String, Object> shiftRow(StaffShift shift) {
        return mapOf(
                "type", "SHIFT",
                "id", shift.getId(),
                "staffId", shift.getStaff().getId(),
                "staffName", shift.getStaff().getName(),
                "centerName", shift.getCenter() == null ? "All centers" : shift.getCenter().getName(),
                "date", shift.getShiftDate(),
                "startTime", shift.getStartTime(),
                "endTime", shift.getEndTime(),
                "status", shift.getStatus(),
                "notes", shift.getNotes()
        );
    }

    private Map<String, Object> emergencyRow(StaffEmergencyAlert alert) {
        return mapOf(
                "id", alert.getId(),
                "type", alert.getType(),
                "severity", alert.getSeverity(),
                "message", alert.getMessage(),
                "status", alert.getStatus(),
                "centerName", alert.getCenter() == null ? "All centers" : alert.getCenter().getName(),
                "turfName", alert.getTurf() == null ? null : alert.getTurf().getName(),
                "staffName", alert.getStaff() == null ? "Admin" : alert.getStaff().getName(),
                "escalationLevel", alert.getEscalationLevel(),
                "resolvedAt", alert.getResolvedAt(),
                "createdAt", alert.getCreatedAt()
        );
    }

    private Map<String, Object> centerRow(EnterpriseCenter center) {
        return mapOf("id", center.getId(), "name", center.getName(), "city", center.getCity(), "status", center.getStatus());
    }

    private void writeAudit(String actor, String action, String entity) {
        auditLogRepository.save(AuditLog.builder()
                .adminId(defaultString(actor, "SYSTEM"))
                .action(action)
                .entity(entity)
                .build());
    }

    private long slaHours(String priority) {
        return switch (priority.toUpperCase(Locale.ROOT)) {
            case "CRITICAL" -> 1L;
            case "HIGH" -> 4L;
            case "LOW" -> 48L;
            default -> 24L;
        };
    }

    private double percent(long numerator, long denominator) {
        if (denominator <= 0) {
            return 0;
        }
        return Math.round((numerator * 10000.0) / denominator) / 100.0;
    }

    private Long parseId(String value) {
        if (isBlank(value)) {
            return null;
        }
        Matcher matcher = NUMBER_PATTERN.matcher(value);
        return matcher.find() ? Long.parseLong(matcher.group(1)) : null;
    }

    @SafeVarargs
    private final <T> T firstPresent(T... values) {
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String normalizeStatus(String value, String fallback) {
        return defaultString(value, fallback).trim().toUpperCase(Locale.ROOT).replace(' ', '_');
    }

    private String defaultString(String value, String fallback) {
        return isBlank(value) ? fallback : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean same(String left, String right) {
        return !isBlank(left) && !isBlank(right) && left.equalsIgnoreCase(right);
    }

    private long toLong(Object value) {
        return value instanceof Number number ? number.longValue() : 0;
    }

    private Map<String, Object> mapOf(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < values.length; i += 2) {
            map.put(String.valueOf(values[i]), values[i + 1]);
        }
        return map;
    }

    private record StaffScope(
            User staff,
            boolean admin,
            List<EnterpriseStaffAssignment> assignments,
            List<EnterpriseCenter> centers,
            Set<Long> ownerIds,
            Set<Long> centerIds
    ) {
    }
}
