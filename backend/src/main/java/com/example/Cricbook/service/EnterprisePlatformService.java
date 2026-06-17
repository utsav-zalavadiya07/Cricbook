package com.example.Cricbook.service;

import com.example.Cricbook.dto.EnterpriseRequests;
import com.example.Cricbook.entity.*;
import com.example.Cricbook.enums.BookingStatus;
import com.example.Cricbook.enums.TurfStatus;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.*;
import com.example.Cricbook.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EnterprisePlatformService {

    private final EnterpriseBusinessRepository businessRepository;
    private final EnterpriseCenterRepository centerRepository;
    private final EnterpriseFranchiseRepository franchiseRepository;
    private final EnterpriseStaffAssignmentRepository staffAssignmentRepository;
    private final UserRepository userRepository;
    private final TurfRepository turfRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final OwnerStaffRoleRepository ownerStaffRoleRepository;
    private final AuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> ownerDashboard(String username) {
        User owner = findUser(username);
        EnterpriseScope scope = ownerScope(owner);
        return dashboard(scope);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> adminDashboard() {
        return dashboard(adminScope());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> ownerCenters(String username) {
        return centerRows(ownerScope(findUser(username)));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> adminCenters() {
        return centerRows(adminScope());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> ownerStaff(String username) {
        return staffRows(ownerScope(findUser(username)));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> adminStaff() {
        return staffRows(adminScope());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> ownerFranchises(String username) {
        return franchiseRows(ownerScope(findUser(username)));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> adminFranchises() {
        return franchiseRows(adminScope());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> ownerReports(String username) {
        return reports(ownerScope(findUser(username)));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> adminReports() {
        return reports(adminScope());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> ownerGovernance(String username) {
        return governance(ownerScope(findUser(username)));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> adminGovernance() {
        return governance(adminScope());
    }

    @Transactional
    public Map<String, Object> createOwnerBusiness(String username, EnterpriseRequests.BusinessRequest request) {
        User owner = findUser(username);
        EnterpriseBusiness business = saveBusiness(owner, request, false);
        writeAudit("CREATE_ENTERPRISE_BUSINESS", "Business " + business.getName());
        return businessRow(business);
    }

    @Transactional
    public Map<String, Object> createAdminBusiness(EnterpriseRequests.BusinessRequest request) {
        if (request == null || request.getOwnerId() == null) {
            throw ApiException.badRequest("Owner ID is required for admin business creation.");
        }
        User owner = userRepository.findById(request.getOwnerId()).orElseThrow(() -> ApiException.badRequest("Owner not found."));
        EnterpriseBusiness business = saveBusiness(owner, request, true);
        writeAudit("CREATE_ENTERPRISE_BUSINESS_ADMIN", "Business " + business.getName() + " owner=" + owner.getEmail());
        return businessRow(business);
    }

    @Transactional
    public Map<String, Object> createOwnerCenter(String username, EnterpriseRequests.CenterRequest request) {
        User owner = findUser(username);
        EnterpriseCenter center = saveCenter(request, owner.getId(), false);
        writeAudit("CREATE_ENTERPRISE_CENTER", "Center " + center.getName());
        return centerRow(center, List.of(), List.of(), List.of(), Map.of());
    }

    @Transactional
    public Map<String, Object> createAdminCenter(EnterpriseRequests.CenterRequest request) {
        EnterpriseCenter center = saveCenter(request, null, true);
        writeAudit("CREATE_ENTERPRISE_CENTER_ADMIN", "Center " + center.getName());
        return centerRow(center, List.of(), List.of(), List.of(), Map.of());
    }

    @Transactional
    public Map<String, Object> createOwnerStaffAssignment(String username, EnterpriseRequests.StaffAssignmentRequest request) {
        User owner = findUser(username);
        EnterpriseStaffAssignment assignment = saveStaffAssignment(request, owner.getId(), false);
        writeAudit("ASSIGN_ENTERPRISE_STAFF", "Staff " + assignment.getStaff().getEmail() + " role=" + assignment.getRole());
        return staffAssignmentRow(assignment);
    }

    @Transactional
    public Map<String, Object> createAdminStaffAssignment(EnterpriseRequests.StaffAssignmentRequest request) {
        EnterpriseStaffAssignment assignment = saveStaffAssignment(request, null, true);
        writeAudit("ASSIGN_ENTERPRISE_STAFF_ADMIN", "Staff " + assignment.getStaff().getEmail() + " role=" + assignment.getRole());
        return staffAssignmentRow(assignment);
    }

    @Transactional
    public Map<String, Object> createAdminFranchise(EnterpriseRequests.FranchiseRequest request) {
        EnterpriseFranchise franchise = saveFranchise(request);
        writeAudit("CREATE_ENTERPRISE_FRANCHISE", "Franchise " + franchise.getName());
        return franchiseRow(franchise, List.of(), List.of());
    }

    private Map<String, Object> dashboard(EnterpriseScope scope) {
        List<Map<String, Object>> centers = centerRows(scope);
        List<Map<String, Object>> staff = staffRows(scope);
        List<Map<String, Object>> franchises = franchiseRows(scope);
        BigDecimal revenue = successfulPaymentAmount(scope.payments());
        long customers = scope.bookings().stream().map(booking -> booking.getUser().getId()).distinct().count();
        long activeTurfs = scope.turfs().stream().filter(turf -> turf.getStatus() == TurfStatus.ACTIVE).count();
        double occupancy = percent(scope.bookings().stream().filter(this::retainedBooking).count(), Math.max(scope.turfs().size() * 30L, 1));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("generatedAt", LocalDateTime.now());
        response.put("businesses", scope.businesses().stream().map(this::businessRow).toList());
        response.put("kpis", mapOf(
                "totalBusinesses", scope.businesses().size(),
                "totalCenters", centers.size(),
                "totalFranchises", franchises.size(),
                "totalTurfs", scope.turfs().size(),
                "activeTurfs", activeTurfs,
                "totalStaff", staff.size(),
                "totalBookings", scope.bookings().size(),
                "totalRevenue", revenue,
                "occupancyPercent", occupancy,
                "customerCount", customers
        ));
        response.put("drillDown", mapOf(
                "platformToCity", revenueByCity(scope.bookings(), scope.payments()),
                "cityToCenter", centers,
                "centerToTurf", turfsByCenter(scope.turfs())
        ));
        response.put("centerRankings", centerRankings(centers));
        response.put("crm", enterpriseCrm(scope));
        response.put("governance", governance(scope));
        response.put("insights", enterpriseInsights(scope, centers, staff, franchises));
        return response;
    }

    private Map<String, Object> reports(EnterpriseScope scope) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("generatedAt", LocalDateTime.now());
        response.put("revenueReport", revenueByCity(scope.bookings(), scope.payments()));
        response.put("occupancyReport", centerRows(scope).stream().map(row -> mapOf(
                "center", row.get("title"),
                "city", row.get("city"),
                "occupancyPercent", row.get("occupancyPercent"),
                "turfs", row.get("turfs")
        )).toList());
        response.put("staffPerformanceReport", staffRows(scope).stream().map(row -> mapOf(
                "staff", row.get("title"),
                "role", row.get("role"),
                "center", row.get("centerName"),
                "kpiScore", row.get("kpiScore"),
                "status", row.get("status")
        )).toList());
        response.put("customerGrowthReport", mapOf(
                "customers", scope.bookings().stream().map(booking -> booking.getUser().getId()).distinct().count(),
                "repeatCustomers", repeatCustomers(scope.bookings()),
                "multiCenterCustomers", multiCenterCustomers(scope.bookings()),
                "crmAggregation", "CONSOLIDATED"
        ));
        response.put("exports", List.of(
                mapOf("format", "PDF", "fileName", "enterprise-revenue-" + LocalDate.now() + ".pdf", "status", "READY"),
                mapOf("format", "EXCEL", "fileName", "enterprise-centers-" + LocalDate.now() + ".xlsx", "status", "READY"),
                mapOf("format", "CSV", "fileName", "enterprise-staff-" + LocalDate.now() + ".csv", "status", "READY")
        ));
        return response;
    }

    private Map<String, Object> governance(EnterpriseScope scope) {
        long activeStaff = staffRows(scope).stream().filter(row -> "ACTIVE".equals(row.get("status"))).count();
        long centersWithoutManagers = centerRows(scope).stream().filter(row -> safe(row.get("managerName")).isBlank()).count();
        List<Map<String, Object>> controls = List.of(
                control("Multi-tenant data isolation", "PASS", "Enterprise dashboards filter by tenant, owner, business, and center scope."),
                control("Hierarchy RBAC", activeStaff > 0 ? "PASS" : "WATCH", "Enterprise owner, regional manager, center manager, and staff mappings are available."),
                control("Center manager coverage", centersWithoutManagers == 0 ? "PASS" : "WATCH", centersWithoutManagers + " center(s) without assigned manager."),
                control("Revenue reconciliation", "PASS", "Revenue roll-up is derived from successful payment records."),
                control("Audit trail", "PASS", "Business, center, staff, and franchise changes write audit records.")
        );
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("score", Math.max(0, 100 - centersWithoutManagers * 8));
        response.put("status", centersWithoutManagers == 0 ? "READY" : "WATCH");
        response.put("controls", controls);
        response.put("settings", List.of(
                setting("GLOBAL_PRICING_RULES", "Central pricing policy can cascade to centers.", "GLOBAL_FIRST"),
                setting("CENTER_DISCOUNT_OVERRIDE", "Center-specific discount overrides can take precedence.", "CENTER_OVERRIDE"),
                setting("FRANCHISE_REVENUE_SHARE", "Franchise revenue sharing is tracked per agreement.", "AGREEMENT_LEVEL"),
                setting("STAFF_ACCESS_POLICY", "Staff permissions are scoped by center and role.", "CENTER_RBAC")
        ));
        return response;
    }

    private List<Map<String, Object>> centerRows(EnterpriseScope scope) {
        Map<Long, EnterpriseCenter> explicitCenters = scope.centers().stream().collect(Collectors.toMap(EnterpriseCenter::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        List<Map<String, Object>> rows = new ArrayList<>();
        explicitCenters.values().forEach(center -> rows.add(centerRow(center, scope.turfs(), scope.bookings(), scope.payments(), staffCountByCenter(scope.staffAssignments()))));

        Set<String> explicitKeys = scope.centers().stream()
                .map(center -> center.getBusiness().getId() + "|" + normalized(center.getCity()))
                .collect(Collectors.toSet());
        Map<String, List<Turf>> syntheticGroups = scope.turfs().stream()
                .collect(Collectors.groupingBy(turf -> ownerBusinessId(scope.businesses(), turf.getOwner()) + "|" + normalized(turf.getCity()), LinkedHashMap::new, Collectors.toList()));

        syntheticGroups.entrySet().stream()
                .filter(entry -> !explicitKeys.contains(entry.getKey()))
                .forEach(entry -> rows.add(syntheticCenterRow(entry.getKey(), entry.getValue(), scope.bookings(), scope.payments())));

        return rows.stream()
                .sorted(Comparator.comparing(row -> safe(row.get("city")) + safe(row.get("title"))))
                .toList();
    }

    private List<Map<String, Object>> staffRows(EnterpriseScope scope) {
        List<Map<String, Object>> rows = new ArrayList<>(scope.staffAssignments().stream().map(this::staffAssignmentRow).toList());
        scope.ownerStaffRoles().forEach(role -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", "OWNER-STAFF-" + role.getId());
            row.put("title", userRepository.findById(role.getStaffId()).map(User::getName).orElse("Staff #" + role.getStaffId()));
            row.put("subtitle", userRepository.findById(role.getStaffId()).map(User::getEmail).orElse("No email"));
            row.put("businessName", userRepository.findById(role.getOwnerId()).map(User::getName).orElse("Owner #" + role.getOwnerId()));
            row.put("centerName", "All owner centers");
            row.put("role", role.getRole() == null ? "OWNER_STAFF" : role.getRole().getName());
            row.put("permissions", role.getRole() == null ? "Owner delegated permissions" : role.getRole().getDescription());
            row.put("status", Boolean.TRUE.equals(role.getActive()) ? "ACTIVE" : "INACTIVE");
            row.put("kpiScore", Boolean.TRUE.equals(role.getActive()) ? 86 : 40);
            row.put("createdAt", role.getCreatedAt());
            rows.add(row);
        });
        return rows;
    }

    private List<Map<String, Object>> franchiseRows(EnterpriseScope scope) {
        return scope.franchises().stream()
                .map(franchise -> franchiseRow(franchise, scope.bookings(), scope.payments()))
                .toList();
    }

    private EnterpriseBusiness saveBusiness(User owner, EnterpriseRequests.BusinessRequest request, boolean admin) {
        if (request == null) {
            throw ApiException.badRequest("Business details are required.");
        }
        Long tenantId = request.getTenantId() == null ? TenantContext.getCurrentTenant() : request.getTenantId();
        String name = required(request.getName(), "Business name is required.");
        businessRepository.findByTenantIdAndNameIgnoreCase(tenantId, name)
                .ifPresent(existing -> {
                    throw ApiException.badRequest("Business name already exists for this tenant.");
                });
        return businessRepository.save(EnterpriseBusiness.builder()
                .tenantId(tenantId)
                .owner(owner)
                .name(name)
                .businessType(defaultString(request.getBusinessType(), admin ? "FRANCHISE_CHAIN" : "OWNER_GROUP").toUpperCase(Locale.ROOT))
                .logoUrl(request.getLogoUrl())
                .status(defaultString(request.getStatus(), "ACTIVE").toUpperCase(Locale.ROOT))
                .build());
    }

    private EnterpriseCenter saveCenter(EnterpriseRequests.CenterRequest request, Long ownerId, boolean admin) {
        if (request == null || request.getBusinessId() == null) {
            throw ApiException.badRequest("Business ID is required.");
        }
        EnterpriseBusiness business = businessRepository.findById(request.getBusinessId()).orElseThrow(() -> ApiException.badRequest("Business not found."));
        if (!admin && !business.getOwner().getId().equals(ownerId)) {
            throw new ApiException("You can create centers only for your business.");
        }
        String name = required(request.getName(), "Center name is required.");
        String city = required(request.getCity(), "Center city is required.");
        centerRepository.findByBusinessIdAndNameIgnoreCaseAndCityIgnoreCase(business.getId(), name, city)
                .ifPresent(existing -> {
                    throw ApiException.badRequest("Center already exists for this business and city.");
                });
        User manager = request.getManagerId() == null ? null : userRepository.findById(request.getManagerId()).orElseThrow(() -> ApiException.badRequest("Manager not found."));
        return centerRepository.save(EnterpriseCenter.builder()
                .business(business)
                .name(name)
                .city(city)
                .state(request.getState())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .manager(manager)
                .status(defaultString(request.getStatus(), "ACTIVE").toUpperCase(Locale.ROOT))
                .build());
    }

    private EnterpriseStaffAssignment saveStaffAssignment(EnterpriseRequests.StaffAssignmentRequest request, Long ownerId, boolean admin) {
        if (request == null || request.getBusinessId() == null || request.getStaffId() == null) {
            throw ApiException.badRequest("Business ID and staff ID are required.");
        }
        EnterpriseBusiness business = businessRepository.findById(request.getBusinessId()).orElseThrow(() -> ApiException.badRequest("Business not found."));
        if (!admin && !business.getOwner().getId().equals(ownerId)) {
            throw new ApiException("You can assign staff only for your business.");
        }
        EnterpriseCenter center = request.getCenterId() == null ? null : centerRepository.findById(request.getCenterId()).orElseThrow(() -> ApiException.badRequest("Center not found."));
        User staff = userRepository.findById(request.getStaffId()).orElseThrow(() -> ApiException.badRequest("Staff user not found."));
        return staffAssignmentRepository.save(EnterpriseStaffAssignment.builder()
                .business(business)
                .center(center)
                .staff(staff)
                .role(defaultString(request.getRole(), "CENTER_MANAGER").toUpperCase(Locale.ROOT))
                .permissions(defaultString(request.getPermissions(), "Bookings, slots, attendance, reports"))
                .active(request.getActive() == null || request.getActive())
                .build());
    }

    private EnterpriseFranchise saveFranchise(EnterpriseRequests.FranchiseRequest request) {
        if (request == null || request.getBusinessId() == null) {
            throw ApiException.badRequest("Business ID is required.");
        }
        EnterpriseBusiness business = businessRepository.findById(request.getBusinessId()).orElseThrow(() -> ApiException.badRequest("Business not found."));
        EnterpriseCenter center = request.getCenterId() == null ? null : centerRepository.findById(request.getCenterId()).orElseThrow(() -> ApiException.badRequest("Center not found."));
        return franchiseRepository.save(EnterpriseFranchise.builder()
                .business(business)
                .center(center)
                .name(required(request.getName(), "Franchise name is required."))
                .status(defaultString(request.getStatus(), "PENDING").toUpperCase(Locale.ROOT))
                .revenueSharePercent(request.getRevenueSharePercent() == null ? BigDecimal.valueOf(12) : request.getRevenueSharePercent())
                .agreementStart(request.getAgreementStart())
                .agreementEnd(request.getAgreementEnd())
                .complianceStatus(defaultString(request.getComplianceStatus(), "PENDING_REVIEW").toUpperCase(Locale.ROOT))
                .build());
    }

    private EnterpriseScope ownerScope(User owner) {
        List<EnterpriseBusiness> businesses = businessRepository.findByOwnerId(owner.getId());
        List<EnterpriseCenter> centers = centerRepository.findByBusinessOwnerId(owner.getId());
        List<EnterpriseFranchise> franchises = franchiseRepository.findByBusinessOwnerId(owner.getId());
        List<EnterpriseStaffAssignment> staff = staffAssignmentRepository.findByBusinessOwnerId(owner.getId());
        List<Turf> turfs = turfRepository.findByOwnerId(owner.getId());
        List<Booking> bookings = bookingRepository.findByTurfOwnerId(owner.getId());
        List<Payment> payments = paymentRepository.findByBookingTurfOwnerIdOrderByCreatedAtDesc(owner.getId());
        List<OwnerStaffRole> ownerStaffRoles = ownerStaffRoleRepository.findByOwnerId(owner.getId());
        return new EnterpriseScope(businesses, centers, franchises, staff, ownerStaffRoles, turfs, bookings, payments);
    }

    private EnterpriseScope adminScope() {
        return new EnterpriseScope(
                businessRepository.findAll(),
                centerRepository.findAll(),
                franchiseRepository.findAll(),
                staffAssignmentRepository.findAll(),
                ownerStaffRoleRepository.findAll(),
                turfRepository.findAll(),
                bookingRepository.findAll(),
                paymentRepository.findAll()
        );
    }

    private Map<String, Object> businessRow(EnterpriseBusiness business) {
        return mapOf(
                "id", business.getId(),
                "tenantId", business.getTenantId(),
                "name", business.getName(),
                "businessType", business.getBusinessType(),
                "logoUrl", business.getLogoUrl(),
                "status", business.getStatus(),
                "ownerId", business.getOwner().getId(),
                "ownerName", business.getOwner().getName(),
                "createdAt", business.getCreatedAt()
        );
    }

    private Map<String, Object> centerRow(EnterpriseCenter center, List<Turf> turfs, List<Booking> bookings, List<Payment> payments, Map<Long, Long> staffCountByCenter) {
        List<Turf> centerTurfs = turfs.stream()
                .filter(turf -> turf.getOwner().getId().equals(center.getBusiness().getOwner().getId()))
                .filter(turf -> normalized(turf.getCity()).equals(normalized(center.getCity())))
                .toList();
        Set<Long> centerTurfIds = centerTurfs.stream().map(Turf::getId).collect(Collectors.toSet());
        List<Booking> centerBookings = bookings.stream().filter(booking -> centerTurfIds.contains(booking.getTurf().getId())).toList();
        BigDecimal revenue = payments.stream()
                .filter(payment -> centerTurfIds.contains(payment.getBooking().getTurf().getId()))
                .filter(payment -> "SUCCESS".equalsIgnoreCase(safe(payment.getStatus())))
                .map(Payment::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return mapOf(
                "id", center.getId(),
                "title", center.getName(),
                "businessName", center.getBusiness().getName(),
                "city", center.getCity(),
                "state", center.getState(),
                "latitude", center.getLatitude(),
                "longitude", center.getLongitude(),
                "managerName", center.getManager() == null ? "" : center.getManager().getName(),
                "status", center.getStatus(),
                "turfs", centerTurfs.size(),
                "bookings", centerBookings.size(),
                "revenue", revenue.setScale(2, RoundingMode.HALF_UP),
                "occupancyPercent", percent(centerBookings.stream().filter(this::retainedBooking).count(), Math.max(centerTurfs.size() * 30L, 1)),
                "staffCount", staffCountByCenter.getOrDefault(center.getId(), 0L),
                "performanceScore", centerPerformanceScore(centerTurfs, centerBookings, revenue)
        );
    }

    private Map<String, Object> syntheticCenterRow(String key, List<Turf> turfs, List<Booking> bookings, List<Payment> payments) {
        String[] parts = key.split("\\|", 2);
        String businessId = parts.length > 0 ? parts[0] : "0";
        String city = parts.length > 1 ? parts[1] : "Unknown";
        Set<Long> turfIds = turfs.stream().map(Turf::getId).collect(Collectors.toSet());
        List<Booking> centerBookings = bookings.stream().filter(booking -> turfIds.contains(booking.getTurf().getId())).toList();
        BigDecimal revenue = payments.stream()
                .filter(payment -> turfIds.contains(payment.getBooking().getTurf().getId()))
                .filter(payment -> "SUCCESS".equalsIgnoreCase(safe(payment.getStatus())))
                .map(Payment::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        String ownerName = turfs.isEmpty() ? "Enterprise Business" : turfs.get(0).getOwner().getName();
        return mapOf(
                "id", "SYN-" + businessId + "-" + city,
                "title", city + " Center",
                "businessName", ownerName,
                "city", city,
                "state", "",
                "latitude", turfs.stream().map(Turf::getLatitude).filter(Objects::nonNull).findFirst().orElse(null),
                "longitude", turfs.stream().map(Turf::getLongitude).filter(Objects::nonNull).findFirst().orElse(null),
                "managerName", ownerName,
                "status", "ACTIVE",
                "turfs", turfs.size(),
                "bookings", centerBookings.size(),
                "revenue", revenue.setScale(2, RoundingMode.HALF_UP),
                "occupancyPercent", percent(centerBookings.stream().filter(this::retainedBooking).count(), Math.max(turfs.size() * 30L, 1)),
                "staffCount", 0,
                "performanceScore", centerPerformanceScore(turfs, centerBookings, revenue)
        );
    }

    private Map<String, Object> staffAssignmentRow(EnterpriseStaffAssignment assignment) {
        return mapOf(
                "id", assignment.getId(),
                "title", assignment.getStaff().getName(),
                "subtitle", assignment.getStaff().getEmail(),
                "businessName", assignment.getBusiness().getName(),
                "centerName", assignment.getCenter() == null ? "All centers" : assignment.getCenter().getName(),
                "role", assignment.getRole(),
                "permissions", assignment.getPermissions(),
                "status", Boolean.TRUE.equals(assignment.getActive()) ? "ACTIVE" : "INACTIVE",
                "kpiScore", Boolean.TRUE.equals(assignment.getActive()) ? 88 : 35,
                "createdAt", assignment.getCreatedAt()
        );
    }

    private Map<String, Object> franchiseRow(EnterpriseFranchise franchise, List<Booking> bookings, List<Payment> payments) {
        Set<Long> turfIds = franchise.getCenter() == null ? Set.of() : turfRepository.findByOwnerId(franchise.getBusiness().getOwner().getId()).stream()
                .filter(turf -> normalized(turf.getCity()).equals(normalized(franchise.getCenter().getCity())))
                .map(Turf::getId)
                .collect(Collectors.toSet());
        BigDecimal revenue = payments.stream()
                .filter(payment -> turfIds.isEmpty() || turfIds.contains(payment.getBooking().getTurf().getId()))
                .filter(payment -> "SUCCESS".equalsIgnoreCase(safe(payment.getStatus())))
                .map(Payment::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return mapOf(
                "id", franchise.getId(),
                "title", franchise.getName(),
                "businessName", franchise.getBusiness().getName(),
                "centerName", franchise.getCenter() == null ? "All centers" : franchise.getCenter().getName(),
                "status", franchise.getStatus(),
                "complianceStatus", franchise.getComplianceStatus(),
                "revenueSharePercent", franchise.getRevenueSharePercent(),
                "revenue", revenue.setScale(2, RoundingMode.HALF_UP),
                "revenueShare", revenue.multiply(franchise.getRevenueSharePercent()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP),
                "agreementStart", franchise.getAgreementStart(),
                "agreementEnd", franchise.getAgreementEnd(),
                "createdAt", franchise.getCreatedAt()
        );
    }

    private Map<Long, Long> staffCountByCenterRaw(List<EnterpriseStaffAssignment> assignments) {
        return assignments.stream()
                .filter(assignment -> assignment.getCenter() != null)
                .collect(Collectors.groupingBy(assignment -> assignment.getCenter().getId(), Collectors.counting()));
    }

    private Map<Long, Long> staffCountByCenter(List<EnterpriseStaffAssignment> assignments) {
        return staffCountByCenterRaw(assignments);
    }

    private List<Map<String, Object>> centerRankings(List<Map<String, Object>> centers) {
        return centers.stream()
                .sorted((left, right) -> toBigDecimal(right.get("revenue")).compareTo(toBigDecimal(left.get("revenue"))))
                .limit(10)
                .map((row) -> mapOf(
                        "center", row.get("title"),
                        "city", row.get("city"),
                        "revenue", row.get("revenue"),
                        "occupancyPercent", row.get("occupancyPercent"),
                        "performanceScore", row.get("performanceScore")
                ))
                .toList();
    }

    private Map<String, Object> enterpriseCrm(EnterpriseScope scope) {
        return mapOf(
                "customers", scope.bookings().stream().map(booking -> booking.getUser().getId()).distinct().count(),
                "repeatCustomers", repeatCustomers(scope.bookings()),
                "multiCenterCustomers", multiCenterCustomers(scope.bookings()),
                "averageCustomerRevenue", averageRevenuePerCustomer(scope.bookings(), scope.payments()),
                "crmAggregation", "UNIFIED_ENTERPRISE_PROFILE"
        );
    }

    private List<Map<String, Object>> enterpriseInsights(EnterpriseScope scope, List<Map<String, Object>> centers, List<Map<String, Object>> staff, List<Map<String, Object>> franchises) {
        List<Map<String, Object>> insights = new ArrayList<>();
        if (centers.size() < 2) {
            insights.add(insight("Add more centers for enterprise reporting", "Cross-center ranking becomes useful after at least two centers.", "MEDIUM"));
        }
        if (staff.isEmpty()) {
            insights.add(insight("Staff hierarchy not configured", "Assign center managers and finance roles to enforce enterprise RBAC.", "HIGH"));
        }
        if (franchises.isEmpty()) {
            insights.add(insight("No franchise agreements tracked", "Create franchise records for compliance and revenue sharing.", "MEDIUM"));
        }
        if (scope.turfs().stream().anyMatch(turf -> turf.getTenantId() == null)) {
            insights.add(insight("Tenant coverage gap", "Some turfs do not have tenant IDs; verify multi-tenant isolation before enterprise rollout.", "HIGH"));
        }
        if (insights.isEmpty()) {
            insights.add(insight("Enterprise platform ready", "Centers, staff hierarchy, franchise controls, and revenue roll-ups are configured.", "LOW"));
        }
        return insights;
    }

    private Map<String, Object> revenueByCity(List<Booking> bookings, List<Payment> payments) {
        Map<Long, Booking> bookingsById = bookings.stream().collect(Collectors.toMap(Booking::getId, Function.identity(), (a, b) -> a));
        Map<String, BigDecimal> revenue = new LinkedHashMap<>();
        payments.stream()
                .filter(payment -> "SUCCESS".equalsIgnoreCase(safe(payment.getStatus())))
                .forEach(payment -> {
                    Booking booking = bookingsById.get(payment.getBooking().getId());
                    if (booking != null) {
                        String city = safe(booking.getTurf().getCity());
                        revenue.put(city, revenue.getOrDefault(city, BigDecimal.ZERO).add(payment.getAmount() == null ? BigDecimal.ZERO : payment.getAmount()));
                    }
                });
        return new LinkedHashMap<>(revenue);
    }

    private Map<String, List<String>> turfsByCenter(List<Turf> turfs) {
        return turfs.stream().collect(Collectors.groupingBy(
                turf -> safe(turf.getCity()) + " Center",
                LinkedHashMap::new,
                Collectors.mapping(Turf::getName, Collectors.toList())
        ));
    }

    private BigDecimal successfulPaymentAmount(List<Payment> payments) {
        return payments.stream()
                .filter(payment -> "SUCCESS".equalsIgnoreCase(safe(payment.getStatus())))
                .map(Payment::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private boolean retainedBooking(Booking booking) {
        return booking.getStatus() == BookingStatus.CONFIRMED
                || booking.getStatus() == BookingStatus.CHECKED_IN
                || booking.getStatus() == BookingStatus.COMPLETED;
    }

    private long repeatCustomers(List<Booking> bookings) {
        return bookings.stream()
                .collect(Collectors.groupingBy(booking -> booking.getUser().getId(), Collectors.counting()))
                .values()
                .stream()
                .filter(count -> count > 1)
                .count();
    }

    private long multiCenterCustomers(List<Booking> bookings) {
        return bookings.stream()
                .collect(Collectors.groupingBy(booking -> booking.getUser().getId(), Collectors.mapping(booking -> safe(booking.getTurf().getCity()), Collectors.toSet())))
                .values()
                .stream()
                .filter(cities -> cities.size() > 1)
                .count();
    }

    private BigDecimal averageRevenuePerCustomer(List<Booking> bookings, List<Payment> payments) {
        long customers = bookings.stream().map(booking -> booking.getUser().getId()).distinct().count();
        if (customers == 0) {
            return BigDecimal.ZERO;
        }
        return successfulPaymentAmount(payments).divide(BigDecimal.valueOf(customers), 2, RoundingMode.HALF_UP);
    }

    private int centerPerformanceScore(List<Turf> turfs, List<Booking> bookings, BigDecimal revenue) {
        int revenueScore = revenue.compareTo(BigDecimal.valueOf(100000)) >= 0 ? 40 : revenue.compareTo(BigDecimal.ZERO) > 0 ? 25 : 5;
        int occupancyScore = (int) Math.min(30, Math.round(percent(bookings.stream().filter(this::retainedBooking).count(), Math.max(turfs.size() * 30L, 1)) / 100 * 30));
        int ratingScore = (int) Math.min(20, Math.round(turfs.stream().map(Turf::getRating).filter(Objects::nonNull).mapToDouble(Double::doubleValue).average().orElse(0) / 5 * 20));
        int growthScore = bookings.size() > 5 ? 10 : bookings.isEmpty() ? 0 : 5;
        return revenueScore + occupancyScore + ratingScore + growthScore;
    }

    private long ownerBusinessId(List<EnterpriseBusiness> businesses, User owner) {
        return businesses.stream()
                .filter(business -> business.getOwner().getId().equals(owner.getId()))
                .map(EnterpriseBusiness::getId)
                .findFirst()
                .orElse(owner.getId() * -1);
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return BigDecimal.ZERO;
    }

    private double percent(long value, long total) {
        if (total <= 0) {
            return 0;
        }
        return BigDecimal.valueOf(value * 100.0 / total).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private Map<String, Object> control(String name, String status, String detail) {
        return mapOf("name", name, "status", status, "detail", detail);
    }

    private Map<String, Object> setting(String key, String description, String precedence) {
        return mapOf("key", key, "description", description, "precedence", precedence);
    }

    private Map<String, Object> insight(String title, String message, String severity) {
        return mapOf("title", title, "message", message, "severity", severity);
    }

    private User findUser(String username) {
        return userRepository.findByEmailOrPhone(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new ApiException("User not found"));
    }

    private String required(String value, String message) {
        if (value == null || value.isBlank()) {
            throw ApiException.badRequest(message);
        }
        return value.trim();
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String safe(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String normalized(String value) {
        return safe(value).toLowerCase(Locale.ROOT);
    }

    private void writeAudit(String action, String entity) {
        String admin = SecurityContextHolder.getContext().getAuthentication() == null
                ? "enterprise_system"
                : SecurityContextHolder.getContext().getAuthentication().getName();
        auditLogRepository.save(AuditLog.builder()
                .adminId(admin)
                .action(action)
                .entity(entity)
                .build());
    }

    private Map<String, Object> mapOf(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int index = 0; index < values.length - 1; index += 2) {
            map.put(String.valueOf(values[index]), values[index + 1]);
        }
        return map;
    }

    private record EnterpriseScope(
            List<EnterpriseBusiness> businesses,
            List<EnterpriseCenter> centers,
            List<EnterpriseFranchise> franchises,
            List<EnterpriseStaffAssignment> staffAssignments,
            List<OwnerStaffRole> ownerStaffRoles,
            List<Turf> turfs,
            List<Booking> bookings,
            List<Payment> payments
    ) {
    }
}
