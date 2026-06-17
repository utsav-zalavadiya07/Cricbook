package com.example.Cricbook.service;

import com.example.Cricbook.dto.CorporateEngagementRequests;
import com.example.Cricbook.entity.*;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CorporateEngagementService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final CorporateCompanyRepository companyRepository;
    private final CorporateLocationRepository locationRepository;
    private final CorporateDepartmentRepository departmentRepository;
    private final CorporateEmployeeRepository employeeRepository;
    private final CorporateTeamRepository teamRepository;
    private final CorporateTeamMembershipRepository membershipRepository;
    private final CorporateBookingPlanRepository bookingPlanRepository;
    private final CorporateTournamentRepository tournamentRepository;
    private final CorporateWellnessProgramRepository wellnessProgramRepository;
    private final CorporateWellnessActivityRepository wellnessActivityRepository;
    private final CorporateRewardTransactionRepository rewardRepository;
    private final CorporateInvoiceRepository invoiceRepository;
    private final CorporateContractRepository contractRepository;
    private final CorporateLeagueRepository leagueRepository;
    private final CorporateEngagementAuditRepository corporateAuditRepository;
    private final AuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> dashboard() {
        List<CorporateCompany> companies = companyRepository.findAll();
        List<CorporateEmployee> employees = employeeRepository.findAll();
        List<CorporateTeam> teams = teamRepository.findAll();
        List<CorporateBookingPlan> bookingPlans = bookingPlanRepository.findAll();
        List<CorporateTournament> tournaments = tournamentRepository.findAll();
        List<CorporateWellnessProgram> wellnessPrograms = wellnessProgramRepository.findAll();
        List<CorporateWellnessActivity> activities = wellnessActivityRepository.findAll();
        List<CorporateInvoice> invoices = invoiceRepository.findAll();
        List<CorporateContract> contracts = contractRepository.findAll();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("kpis", kpis(companies, employees, teams, bookingPlans, tournaments, wellnessPrograms, activities, invoices));
        response.put("companies", companies.stream().sorted(byCreatedDesc()).limit(8).map(this::mapCompany).toList());
        response.put("employees", employees.stream().sorted(byCreatedDesc()).limit(8).map(this::mapEmployee).toList());
        response.put("teams", teams.stream().sorted(byCreatedDesc()).limit(8).map(this::mapTeam).toList());
        response.put("bookingPlans", bookingPlans.stream().sorted(byCreatedDesc()).limit(8).map(this::mapBookingPlan).toList());
        response.put("tournaments", tournaments.stream().sorted(byCreatedDesc()).limit(8).map(this::mapTournament).toList());
        response.put("wellnessPrograms", wellnessPrograms.stream().sorted(byCreatedDesc()).limit(8).map(this::mapWellnessProgram).toList());
        response.put("billing", billing(invoices, contracts));
        response.put("analytics", analytics(employees, activities, teams, departmentsById()));
        response.put("crm", crm(companies, employees, contracts, invoices));
        response.put("leaderboards", leaderboards(employees, teams, departmentsById()));
        response.put("governanceControls", governanceControls());
        response.put("priorityActions", priorityActions(companies, employees, invoices, contracts));
        response.put("generatedAt", LocalDateTime.now());
        return response;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> marketplace(String city) {
        return companyRepository.findByStatusOrderByCreatedAtDesc("APPROVED").stream()
                .filter(company -> matches(city, company.getCity()))
                .map(this::mapCompany)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> companies() {
        return companyRepository.findAllByOrderByCreatedAtDesc().stream().map(this::mapCompany).toList();
    }

    @Transactional
    public Map<String, Object> registerCompany(CorporateEngagementRequests.CompanyRequest request, String actor) {
        String name = required(request == null ? null : request.getName(), "Company name is required");
        if (companyRepository.existsByNameIgnoreCase(name)) {
            throw ApiException.conflict("Company name already exists");
        }
        validateOptionalEmail(request.getHrEmail(), "HR email");
        validateOptionalEmail(request.getAdminEmail(), "Admin email");
        validateGst(request.getGstNumber());
        CorporateCompany company = CorporateCompany.builder()
                .name(name)
                .gstNumber(blankToNull(request.getGstNumber()))
                .industry(blankToNull(request.getIndustry()))
                .city(blankToNull(request.getCity()))
                .locations(blankToNull(request.getLocations()))
                .hrContactName(blankToNull(request.getHrContactName()))
                .hrEmail(blankToNull(request.getHrEmail()))
                .hrPhone(blankToNull(request.getHrPhone()))
                .adminEmail(blankToNull(request.getAdminEmail()))
                .logoUrl(blankToNull(request.getLogoUrl()))
                .documentUrl(blankToNull(request.getDocumentUrl()))
                .annualContractValue(nvl(request.getAnnualContractValue()))
                .monthlyBudget(nvl(request.getMonthlyBudget()))
                .status("PENDING_APPROVAL")
                .build();
        CorporateCompany saved = companyRepository.save(company);
        audit(saved.getId(), null, saved.getId(), "COMPANY_REGISTERED", actor, "Corporate registration submitted: " + saved.getName());
        return mapCompany(saved);
    }

    @Transactional
    public Map<String, Object> updateCompanyStatus(Long companyId, CorporateEngagementRequests.StatusRequest request, String actor) {
        CorporateCompany company = company(companyId);
        String status = normalize(request == null ? null : request.getStatus(), "PENDING_APPROVAL");
        if (!Set.of("PENDING_APPROVAL", "APPROVED", "REJECTED", "MORE_DOCUMENTS", "SUSPENDED", "ACTIVE").contains(status)) {
            throw ApiException.badRequest("Unsupported company status");
        }
        company.setStatus(status);
        company.setReviewNotes(blankToNull(request == null ? null : request.getNotes()));
        CorporateCompany saved = companyRepository.save(company);
        audit(saved.getId(), null, saved.getId(), "COMPANY_STATUS_" + status, actor, defaultString(saved.getReviewNotes(), "Company status updated"));
        return mapCompany(saved);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> locations() {
        return locationRepository.findAllByOrderByCreatedAtDesc().stream().map(this::mapLocation).toList();
    }

    @Transactional
    public Map<String, Object> createLocation(CorporateEngagementRequests.LocationRequest request, String actor) {
        CorporateCompany company = company(request == null ? null : request.getCompanyId());
        CorporateLocation location = CorporateLocation.builder()
                .companyId(company.getId())
                .name(required(request.getName(), "Location name is required"))
                .city(blankToNull(request.getCity()))
                .address(blankToNull(request.getAddress()))
                .employeeCount(safeInt(request.getEmployeeCount()))
                .status(normalize(request.getStatus(), "ACTIVE"))
                .build();
        CorporateLocation saved = locationRepository.save(location);
        audit(company.getId(), null, saved.getId(), "LOCATION_CREATED", actor, "Corporate location created: " + saved.getName());
        return mapLocation(saved);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> departments() {
        return departmentRepository.findAllByOrderByCreatedAtDesc().stream().map(this::mapDepartment).toList();
    }

    @Transactional
    public Map<String, Object> createDepartment(CorporateEngagementRequests.DepartmentRequest request, String actor) {
        CorporateCompany company = company(request == null ? null : request.getCompanyId());
        CorporateDepartment department = CorporateDepartment.builder()
                .companyId(company.getId())
                .name(required(request.getName(), "Department name is required"))
                .parentDepartment(blankToNull(request.getParentDepartment()))
                .costCenter(blankToNull(request.getCostCenter()))
                .status("ACTIVE")
                .build();
        CorporateDepartment saved = departmentRepository.save(department);
        audit(company.getId(), null, saved.getId(), "DEPARTMENT_CREATED", actor, "Department created: " + saved.getName());
        return mapDepartment(saved);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> employees() {
        return employeeRepository.findAllByOrderByCreatedAtDesc().stream().map(this::mapEmployee).toList();
    }

    @Transactional
    public Map<String, Object> createEmployee(CorporateEngagementRequests.EmployeeRequest request, String actor) {
        CorporateCompany company = company(request == null ? null : request.getCompanyId());
        String email = required(request.getEmail(), "Employee email is required");
        validateOptionalEmail(email, "Employee email");
        employeeRepository.findByCompanyIdAndEmailIgnoreCase(company.getId(), email).ifPresent(existing -> {
            throw ApiException.conflict("Employee already exists in company");
        });
        if (request.getLocationId() != null) location(request.getLocationId());
        if (request.getDepartmentId() != null) department(request.getDepartmentId());
        CorporateEmployee employee = CorporateEmployee.builder()
                .companyId(company.getId())
                .locationId(request.getLocationId())
                .departmentId(request.getDepartmentId())
                .name(required(request.getName(), "Employee name is required"))
                .email(email)
                .phone(blankToNull(request.getPhone()))
                .employeeCode(blankToNull(request.getEmployeeCode()))
                .role(blankToNull(request.getRole()))
                .status("ACTIVE")
                .wellnessScore(50)
                .participationScore(0)
                .rewardBalance(0)
                .build();
        CorporateEmployee saved = employeeRepository.save(employee);
        audit(company.getId(), saved.getId(), saved.getId(), "EMPLOYEE_CREATED", actor, "Corporate employee created: " + saved.getEmail());
        return mapEmployee(saved);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> teams() {
        return teamRepository.findAllByOrderByCreatedAtDesc().stream().map(this::mapTeam).toList();
    }

    @Transactional
    public Map<String, Object> createTeam(CorporateEngagementRequests.TeamRequest request, String actor) {
        CorporateCompany company = company(request == null ? null : request.getCompanyId());
        if (request.getDepartmentId() != null) department(request.getDepartmentId());
        if (request.getCaptainEmployeeId() != null) employee(request.getCaptainEmployeeId());
        CorporateTeam team = CorporateTeam.builder()
                .companyId(company.getId())
                .departmentId(request.getDepartmentId())
                .name(required(request.getName(), "Team name is required"))
                .sport(required(request.getSport(), "Sport is required"))
                .captainEmployeeId(request.getCaptainEmployeeId())
                .status("ACTIVE")
                .memberCount(0)
                .build();
        CorporateTeam saved = teamRepository.save(team);
        audit(company.getId(), request.getCaptainEmployeeId(), saved.getId(), "TEAM_CREATED", actor, "Corporate team created: " + saved.getName());
        return mapTeam(saved);
    }

    @Transactional
    public Map<String, Object> addTeamMember(CorporateEngagementRequests.TeamMembershipRequest request, String actor) {
        CorporateTeam team = team(request == null ? null : request.getTeamId());
        CorporateEmployee employee = employee(request.getEmployeeId());
        if (!Objects.equals(team.getCompanyId(), employee.getCompanyId())) {
            throw ApiException.badRequest("Employee and team must belong to the same company");
        }
        membershipRepository.findByTeamIdAndEmployeeId(team.getId(), employee.getId()).ifPresent(existing -> {
            throw ApiException.conflict("Employee already belongs to team");
        });
        CorporateTeamMembership membership = CorporateTeamMembership.builder()
                .teamId(team.getId())
                .employeeId(employee.getId())
                .role(normalize(request.getRole(), "MEMBER"))
                .status("ACTIVE")
                .build();
        CorporateTeamMembership saved = membershipRepository.save(membership);
        team.setMemberCount(team.getMemberCount() + 1);
        teamRepository.save(team);
        audit(team.getCompanyId(), employee.getId(), saved.getId(), "TEAM_MEMBER_ADDED", actor, employee.getEmail() + " added to " + team.getName());
        return mapMembership(saved);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> memberships() {
        return membershipRepository.findAllByOrderByCreatedAtDesc().stream().map(this::mapMembership).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> bookingPlans() {
        return bookingPlanRepository.findAllByOrderByCreatedAtDesc().stream().map(this::mapBookingPlan).toList();
    }

    @Transactional
    public Map<String, Object> createBookingPlan(CorporateEngagementRequests.BookingPlanRequest request, String actor) {
        CorporateCompany company = company(request == null ? null : request.getCompanyId());
        if (request.getDepartmentId() != null) department(request.getDepartmentId());
        String recurrence = normalize(request.getRecurrenceType(), "WEEKLY");
        if (!Set.of("ONCE", "WEEKLY", "MONTHLY", "YEARLY").contains(recurrence)) {
            throw ApiException.badRequest("Invalid recurrence pattern");
        }
        CorporateBookingPlan plan = CorporateBookingPlan.builder()
                .companyId(company.getId())
                .departmentId(request.getDepartmentId())
                .sport(required(request.getSport(), "Sport is required"))
                .turfName(blankToNull(request.getTurfName()))
                .city(blankToNull(request.getCity()))
                .recurrenceType(recurrence)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .generatedBookings(generatedBookings(recurrence, request.getStartDate(), request.getEndDate()))
                .monthlySpend(nvl(request.getMonthlySpend()))
                .status("ACTIVE")
                .conflictNotes(blankToNull(request.getConflictNotes()))
                .build();
        CorporateBookingPlan saved = bookingPlanRepository.save(plan);
        audit(company.getId(), null, saved.getId(), "BOOKING_PLAN_CREATED", actor, "Corporate booking plan created: " + saved.getSport());
        return mapBookingPlan(saved);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> tournaments() {
        return tournamentRepository.findAllByOrderByCreatedAtDesc().stream().map(this::mapTournament).toList();
    }

    @Transactional
    public Map<String, Object> createTournament(CorporateEngagementRequests.TournamentRequest request, String actor) {
        CorporateCompany company = company(request == null ? null : request.getCompanyId());
        CorporateTournament tournament = CorporateTournament.builder()
                .companyId(company.getId())
                .name(required(request.getName(), "Tournament name is required"))
                .sport(required(request.getSport(), "Sport is required"))
                .scope(normalize(request.getScope(), "OFFICE"))
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .teamCount(safeInt(request.getTeamCount()))
                .fixturesGenerated(Boolean.TRUE.equals(request.getFixturesGenerated()))
                .status(normalize(request.getStatus(), "DRAFT"))
                .rankingStatus(Boolean.TRUE.equals(request.getFixturesGenerated()) ? "FIXTURES_READY" : "PENDING_FIXTURES")
                .build();
        CorporateTournament saved = tournamentRepository.save(tournament);
        audit(company.getId(), null, saved.getId(), "TOURNAMENT_CREATED", actor, "Corporate tournament created: " + saved.getName());
        return mapTournament(saved);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> wellnessPrograms() {
        return wellnessProgramRepository.findAllByOrderByCreatedAtDesc().stream().map(this::mapWellnessProgram).toList();
    }

    @Transactional
    public Map<String, Object> createWellnessProgram(CorporateEngagementRequests.WellnessProgramRequest request, String actor) {
        CorporateCompany company = company(request == null ? null : request.getCompanyId());
        CorporateWellnessProgram program = CorporateWellnessProgram.builder()
                .companyId(company.getId())
                .title(required(request.getTitle(), "Wellness program title is required"))
                .sport(blankToNull(request.getSport()))
                .programType(normalize(request.getProgramType(), "PARTICIPATION"))
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .rewardPoints(safeInt(request.getRewardPoints()))
                .status(normalize(request.getStatus(), "PUBLISHED"))
                .build();
        CorporateWellnessProgram saved = wellnessProgramRepository.save(program);
        audit(company.getId(), null, saved.getId(), "WELLNESS_PROGRAM_CREATED", actor, "Wellness program created: " + saved.getTitle());
        return mapWellnessProgram(saved);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> wellnessActivities() {
        return wellnessActivityRepository.findAllByOrderByCreatedAtDesc().stream().map(this::mapWellnessActivity).toList();
    }

    @Transactional
    public Map<String, Object> recordWellnessActivity(CorporateEngagementRequests.WellnessActivityRequest request, String actor) {
        CorporateWellnessProgram program = wellnessProgram(request == null ? null : request.getProgramId());
        CorporateEmployee employee = employee(request.getEmployeeId());
        if (!Objects.equals(program.getCompanyId(), employee.getCompanyId())) {
            throw ApiException.badRequest("Employee and wellness program must belong to the same company");
        }
        CorporateWellnessActivity activity = CorporateWellnessActivity.builder()
                .programId(program.getId())
                .employeeId(employee.getId())
                .activityType(normalize(request.getActivityType(), "SPORTS_ACTIVITY"))
                .attendanceStatus(normalize(request.getAttendanceStatus(), "PRESENT"))
                .sportsHours(safeInt(request.getSportsHours()))
                .completed(Boolean.TRUE.equals(request.getCompleted()))
                .build();
        CorporateWellnessActivity saved = wellnessActivityRepository.save(activity);
        int sportsHours = safeInt(request.getSportsHours());
        employee.setParticipationScore(Math.min(100, employee.getParticipationScore() + Math.max(5, sportsHours * 10)));
        employee.setWellnessScore(Math.min(100, employee.getWellnessScore() + Math.max(2, sportsHours * 3)));
        if (Boolean.TRUE.equals(request.getCompleted())) {
            program.setCompletedEmployees(program.getCompletedEmployees() + 1);
        }
        if ("PRESENT".equalsIgnoreCase(activity.getAttendanceStatus())) {
            program.setEnrolledEmployees(program.getEnrolledEmployees() + 1);
        }
        employeeRepository.save(employee);
        wellnessProgramRepository.save(program);
        if (Boolean.TRUE.equals(request.getCompleted()) && program.getRewardPoints() > 0) {
            creditRewardInternal(employee, "WELLNESS_COMPLETION", program.getRewardPoints(), "Completed " + program.getTitle(), actor);
        }
        audit(program.getCompanyId(), employee.getId(), saved.getId(), "WELLNESS_ACTIVITY_RECORDED", actor, "Wellness participation recorded");
        return mapWellnessActivity(saved);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> rewards() {
        return rewardRepository.findAllByOrderByCreatedAtDesc().stream().map(this::mapReward).toList();
    }

    @Transactional
    public Map<String, Object> creditReward(CorporateEngagementRequests.RewardRequest request, String actor) {
        CorporateEmployee employee = employee(request == null ? null : request.getEmployeeId());
        return mapReward(creditRewardInternal(employee, normalize(request.getSourceType(), "ADMIN_REWARD"), safeInt(request.getPoints()), blankToNull(request.getSummary()), actor));
    }

    @Transactional
    public Map<String, Object> redeemReward(CorporateEngagementRequests.RewardRequest request, String actor) {
        CorporateEmployee employee = employee(request == null ? null : request.getEmployeeId());
        int points = safeInt(request.getPoints());
        if (points <= 0) throw ApiException.badRequest("Redemption points must be positive");
        if (employee.getRewardBalance() < points) throw ApiException.badRequest("Insufficient corporate reward balance");
        employee.setRewardBalance(employee.getRewardBalance() - points);
        employeeRepository.save(employee);
        CorporateRewardTransaction reward = CorporateRewardTransaction.builder()
                .companyId(employee.getCompanyId())
                .employeeId(employee.getId())
                .sourceType(normalize(request.getSourceType(), "REDEMPTION"))
                .points(-points)
                .balanceAfter(employee.getRewardBalance())
                .status("REDEEMED")
                .summary(blankToNull(request.getSummary()))
                .build();
        CorporateRewardTransaction saved = rewardRepository.save(reward);
        audit(employee.getCompanyId(), employee.getId(), saved.getId(), "REWARD_REDEEMED", actor, "Corporate reward redeemed");
        return mapReward(saved);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> invoices() {
        return invoiceRepository.findAllByOrderByCreatedAtDesc().stream().map(this::mapInvoice).toList();
    }

    @Transactional
    public Map<String, Object> generateInvoice(CorporateEngagementRequests.InvoiceRequest request, String actor) {
        CorporateCompany company = company(request == null ? null : request.getCompanyId());
        BigDecimal amount = nvl(request.getAmount());
        BigDecimal tax = nvl(request.getTaxAmount());
        BigDecimal gst = request.getGstAmount() == null ? amount.multiply(BigDecimal.valueOf(0.18)).setScale(2, RoundingMode.HALF_UP) : request.getGstAmount();
        CorporateInvoice invoice = CorporateInvoice.builder()
                .companyId(company.getId())
                .invoiceNumber("CORP-INV-" + System.currentTimeMillis() + "-" + RANDOM.nextInt(9000))
                .billingPeriod(required(request.getBillingPeriod(), "Billing period is required"))
                .amount(amount)
                .taxAmount(tax)
                .gstAmount(gst)
                .totalAmount(amount.add(tax).add(gst))
                .dueDate(request.getDueDate())
                .status("GENERATED")
                .build();
        CorporateInvoice saved = invoiceRepository.save(invoice);
        audit(company.getId(), null, saved.getId(), "INVOICE_GENERATED", actor, "Corporate invoice generated: " + saved.getInvoiceNumber());
        return mapInvoice(saved);
    }

    @Transactional
    public Map<String, Object> updateInvoicePayment(Long invoiceId, CorporateEngagementRequests.PaymentRequest request, String actor) {
        CorporateInvoice invoice = invoice(invoiceId);
        invoice.setStatus(normalize(request == null ? null : request.getStatus(), "PAID"));
        CorporateInvoice saved = invoiceRepository.save(invoice);
        audit(saved.getCompanyId(), null, saved.getId(), "INVOICE_STATUS_" + saved.getStatus(), actor, "Corporate invoice payment status updated");
        return mapInvoice(saved);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> contracts() {
        return contractRepository.findAllByOrderByCreatedAtDesc().stream().map(this::mapContract).toList();
    }

    @Transactional
    public Map<String, Object> createContract(CorporateEngagementRequests.ContractRequest request, String actor) {
        CorporateCompany company = company(request == null ? null : request.getCompanyId());
        CorporateContract contract = CorporateContract.builder()
                .companyId(company.getId())
                .contractNumber("CORP-CON-" + System.currentTimeMillis() + "-" + RANDOM.nextInt(9000))
                .planName(blankToNull(request.getPlanName()))
                .annualValue(nvl(request.getAnnualValue()))
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .renewalProbability(safeInt(request.getRenewalProbability()))
                .status(normalize(request.getStatus(), "ACTIVE"))
                .build();
        CorporateContract saved = contractRepository.save(contract);
        company.setAnnualContractValue(saved.getAnnualValue());
        companyRepository.save(company);
        audit(company.getId(), null, saved.getId(), "CONTRACT_CREATED", actor, "Corporate contract created");
        return mapContract(saved);
    }

    @Transactional
    public Map<String, Object> renewContract(Long contractId, CorporateEngagementRequests.ContractRequest request, String actor) {
        CorporateContract contract = contract(contractId);
        contract.setEndDate(request == null || request.getEndDate() == null ? LocalDate.now().plusYears(1) : request.getEndDate());
        contract.setAnnualValue(request == null || request.getAnnualValue() == null ? contract.getAnnualValue() : request.getAnnualValue());
        contract.setRenewalProbability(request == null || request.getRenewalProbability() == null ? 90 : request.getRenewalProbability());
        contract.setStatus("ACTIVE");
        CorporateContract saved = contractRepository.save(contract);
        audit(saved.getCompanyId(), null, saved.getId(), "CONTRACT_RENEWED", actor, "Corporate contract renewed");
        return mapContract(saved);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> leagues() {
        return leagueRepository.findAllByOrderByCreatedAtDesc().stream().map(this::mapLeague).toList();
    }

    @Transactional
    public Map<String, Object> createLeague(CorporateEngagementRequests.LeagueRequest request, String actor) {
        CorporateCompany organizer = company(request == null ? null : request.getOrganizerCompanyId());
        CorporateLeague league = CorporateLeague.builder()
                .name(required(request.getName(), "League name is required"))
                .sport(required(request.getSport(), "Sport is required"))
                .organizerCompanyId(organizer.getId())
                .invitedCompanyIds(blankToNull(request.getInvitedCompanyIds()))
                .registeredCompanies(Math.max(1, safeInt(request.getRegisteredCompanies())))
                .teamCount(safeInt(request.getTeamCount()))
                .fixturesGenerated(Boolean.TRUE.equals(request.getFixturesGenerated()))
                .rankingStatus(Boolean.TRUE.equals(request.getFixturesGenerated()) ? "FIXTURES_READY" : "PENDING")
                .status(normalize(request.getStatus(), "REGISTRATION_OPEN"))
                .build();
        CorporateLeague saved = leagueRepository.save(league);
        audit(organizer.getId(), null, saved.getId(), "LEAGUE_CREATED", actor, "Inter-company league created: " + saved.getName());
        return mapLeague(saved);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> analytics() {
        return analytics(employeeRepository.findAll(), wellnessActivityRepository.findAll(), teamRepository.findAll(), departmentsById());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> billing() {
        return billing(invoiceRepository.findAll(), contractRepository.findAll());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> audits() {
        return corporateAuditRepository.findAllByOrderByCreatedAtDesc().stream().map(this::mapAudit).toList();
    }

    private CorporateRewardTransaction creditRewardInternal(CorporateEmployee employee, String sourceType, int points, String summary, String actor) {
        if (points <= 0) throw ApiException.badRequest("Reward points must be positive");
        employee.setRewardBalance(employee.getRewardBalance() + points);
        employeeRepository.save(employee);
        CorporateRewardTransaction reward = CorporateRewardTransaction.builder()
                .companyId(employee.getCompanyId())
                .employeeId(employee.getId())
                .sourceType(sourceType)
                .points(points)
                .balanceAfter(employee.getRewardBalance())
                .status("CREDITED")
                .summary(summary)
                .build();
        CorporateRewardTransaction saved = rewardRepository.save(reward);
        audit(employee.getCompanyId(), employee.getId(), saved.getId(), "REWARD_CREDITED", actor, "Corporate reward credited");
        return saved;
    }

    private Map<String, Object> kpis(List<CorporateCompany> companies,
                                     List<CorporateEmployee> employees,
                                     List<CorporateTeam> teams,
                                     List<CorporateBookingPlan> bookingPlans,
                                     List<CorporateTournament> tournaments,
                                     List<CorporateWellnessProgram> wellnessPrograms,
                                     List<CorporateWellnessActivity> activities,
                                     List<CorporateInvoice> invoices) {
        long activeEmployees = employees.stream().filter(item -> "ACTIVE".equalsIgnoreCase(item.getStatus())).count();
        long participatingEmployees = employees.stream().filter(item -> item.getParticipationScore() > 0).count();
        BigDecimal monthlySpend = bookingPlans.stream().map(CorporateBookingPlan::getMonthlySpend).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal invoiceSpend = invoices.stream().map(CorporateInvoice::getTotalAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, Object> kpis = new LinkedHashMap<>();
        kpis.put("companies", companies.size());
        kpis.put("approvedCompanies", companies.stream().filter(item -> "APPROVED".equalsIgnoreCase(item.getStatus()) || "ACTIVE".equalsIgnoreCase(item.getStatus())).count());
        kpis.put("employees", employees.size());
        kpis.put("activeEmployees", activeEmployees);
        kpis.put("teams", teams.size());
        kpis.put("upcomingEvents", tournaments.stream().filter(item -> !"COMPLETED".equalsIgnoreCase(item.getStatus())).count() + wellnessPrograms.stream().filter(item -> !"COMPLETED".equalsIgnoreCase(item.getStatus())).count());
        kpis.put("participationRate", percentage(participatingEmployees, Math.max(activeEmployees, 1)));
        kpis.put("monthlySpend", monthlySpend);
        kpis.put("invoiceSpend", invoiceSpend);
        kpis.put("sportsHours", activities.stream().mapToInt(CorporateWellnessActivity::getSportsHours).sum());
        return kpis;
    }

    private Map<String, Object> analytics(List<CorporateEmployee> employees,
                                          List<CorporateWellnessActivity> activities,
                                          List<CorporateTeam> teams,
                                          Map<Long, CorporateDepartment> departments) {
        double wellnessAverage = employees.stream().mapToInt(CorporateEmployee::getWellnessScore).average().orElse(0);
        double engagementAverage = employees.stream().mapToInt(CorporateEmployee::getParticipationScore).average().orElse(0);
        long active = employees.stream().filter(item -> "ACTIVE".equalsIgnoreCase(item.getStatus())).count();
        long adopted = employees.stream().filter(item -> item.getParticipationScore() > 0).count();
        Map<String, Long> byDepartment = employees.stream()
                .filter(item -> item.getDepartmentId() != null)
                .collect(Collectors.groupingBy(item -> departmentName(departments.get(item.getDepartmentId())), LinkedHashMap::new, Collectors.counting()));
        Map<String, Object> analytics = new LinkedHashMap<>();
        analytics.put("monthlyParticipation", adopted);
        analytics.put("wellnessScore", round(wellnessAverage));
        analytics.put("employeeEngagementScore", round(engagementAverage));
        analytics.put("sportsAdoptionRate", percentage(adopted, Math.max(active, 1)));
        analytics.put("departmentParticipation", byDepartment);
        analytics.put("teamCount", teams.size());
        analytics.put("sportsHours", activities.stream().mapToInt(CorporateWellnessActivity::getSportsHours).sum());
        return analytics;
    }

    private Map<String, Object> billing(List<CorporateInvoice> invoices, List<CorporateContract> contracts) {
        BigDecimal invoiceTotal = invoices.stream().map(CorporateInvoice::getTotalAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal arr = contracts.stream().filter(item -> "ACTIVE".equalsIgnoreCase(item.getStatus())).map(CorporateContract::getAnnualValue).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, Object> billing = new LinkedHashMap<>();
        billing.put("invoiceTotal", invoiceTotal);
        billing.put("activeArr", arr);
        billing.put("generatedInvoices", invoices.size());
        billing.put("paidInvoices", invoices.stream().filter(item -> "PAID".equalsIgnoreCase(item.getStatus())).count());
        billing.put("outstandingInvoices", invoices.stream().filter(item -> !"PAID".equalsIgnoreCase(item.getStatus())).count());
        billing.put("activeContracts", contracts.stream().filter(item -> "ACTIVE".equalsIgnoreCase(item.getStatus())).count());
        return billing;
    }

    private Map<String, Object> crm(List<CorporateCompany> companies, List<CorporateEmployee> employees, List<CorporateContract> contracts, List<CorporateInvoice> invoices) {
        Map<Long, Long> employeesByCompany = employees.stream().collect(Collectors.groupingBy(CorporateEmployee::getCompanyId, Collectors.counting()));
        Map<Long, Long> unpaidByCompany = invoices.stream().filter(item -> !"PAID".equalsIgnoreCase(item.getStatus())).collect(Collectors.groupingBy(CorporateInvoice::getCompanyId, Collectors.counting()));
        List<Map<String, Object>> accountHealth = companies.stream().map(company -> {
            long employeeCount = employeesByCompany.getOrDefault(company.getId(), 0L);
            long unpaid = unpaidByCompany.getOrDefault(company.getId(), 0L);
            int score = (int) Math.max(0, Math.min(100, 60 + Math.min(employeeCount, 30) - unpaid * 10));
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("companyId", company.getId());
            item.put("companyName", company.getName());
            item.put("accountHealthScore", score);
            item.put("renewalStatus", score >= 75 ? "STRONG" : score >= 50 ? "WATCH" : "AT_RISK");
            item.put("usageMetrics", employeeCount + " employees onboarded");
            return item;
        }).toList();
        Map<String, Object> crm = new LinkedHashMap<>();
        crm.put("accountHealth", accountHealth);
        crm.put("renewalsDue", contracts.stream().filter(item -> item.getEndDate() != null && item.getEndDate().isBefore(LocalDate.now().plusDays(45))).count());
        crm.put("upsellOpportunities", accountHealth.stream().filter(item -> ((Number) item.get("accountHealthScore")).intValue() >= 80).count());
        return crm;
    }

    private Map<String, Object> leaderboards(List<CorporateEmployee> employees, List<CorporateTeam> teams, Map<Long, CorporateDepartment> departments) {
        Map<String, Object> leaderboards = new LinkedHashMap<>();
        leaderboards.put("topEmployees", employees.stream().sorted(Comparator.comparing(CorporateEmployee::getParticipationScore, Comparator.reverseOrder())).limit(5).map(this::mapEmployee).toList());
        leaderboards.put("topTeams", teams.stream().sorted(Comparator.comparing(CorporateTeam::getMemberCount, Comparator.reverseOrder())).limit(5).map(this::mapTeam).toList());
        Map<String, Integer> departmentScores = new LinkedHashMap<>();
        employees.forEach(employee -> {
            if (employee.getDepartmentId() != null) {
                String name = departmentName(departments.get(employee.getDepartmentId()));
                departmentScores.merge(name, employee.getParticipationScore(), Integer::sum);
            }
        });
        leaderboards.put("topDepartments", departmentScores.entrySet().stream().sorted(Map.Entry.<String, Integer>comparingByValue().reversed()).limit(5).map(entry -> Map.of("department", entry.getKey(), "score", entry.getValue())).toList());
        return leaderboards;
    }

    private List<Map<String, Object>> governanceControls() {
        return List.of(
                action("Employee import validation", "CSV and HRMS import must deduplicate by company and email.", "HIGH"),
                action("Cross-company isolation", "Corporate employees, billing, teams, and analytics are scoped by companyId.", "CRITICAL"),
                action("Billing reconciliation", "Invoices carry GST, tax, total amount, and payment status for finance review.", "HIGH"),
                action("Wellness privacy controls", "Employee engagement and wellness scores are only exposed in admin corporate context.", "HIGH"),
                action("Renewal governance", "Contracts expose renewal probability and renewal due flags for CRM follow-up.", "MEDIUM")
        );
    }

    private List<Map<String, Object>> priorityActions(List<CorporateCompany> companies, List<CorporateEmployee> employees, List<CorporateInvoice> invoices, List<CorporateContract> contracts) {
        List<Map<String, Object>> actions = new ArrayList<>();
        if (companies.stream().anyMatch(item -> "PENDING_APPROVAL".equalsIgnoreCase(item.getStatus()))) actions.add(action("Review corporate approvals", "Pending companies require approval, rejection, or more documents.", "CRITICAL"));
        if (employees.stream().anyMatch(item -> item.getParticipationScore() < 20)) actions.add(action("Boost employee participation", "Low participation users should receive wellness nudges or team invites.", "HIGH"));
        if (invoices.stream().anyMatch(item -> !"PAID".equalsIgnoreCase(item.getStatus()))) actions.add(action("Follow up invoices", "Outstanding corporate invoices need finance follow-up.", "HIGH"));
        if (contracts.stream().anyMatch(item -> item.getEndDate() != null && item.getEndDate().isBefore(LocalDate.now().plusDays(45)))) actions.add(action("Renewal workflow", "Contracts nearing expiry need renewal outreach.", "MEDIUM"));
        if (actions.isEmpty()) actions.add(action("Corporate portfolio healthy", "No immediate corporate governance action required.", "LOW"));
        return actions;
    }

    private Map<String, Object> action(String title, String detail, String severity) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("title", title);
        item.put("detail", detail);
        item.put("severity", severity);
        return item;
    }

    private Map<String, Object> mapCompany(CorporateCompany company) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", company.getId());
        item.put("name", company.getName());
        item.put("gstNumber", company.getGstNumber());
        item.put("industry", company.getIndustry());
        item.put("city", company.getCity());
        item.put("locations", company.getLocations());
        item.put("hrContactName", company.getHrContactName());
        item.put("hrEmail", company.getHrEmail());
        item.put("hrPhone", company.getHrPhone());
        item.put("adminEmail", company.getAdminEmail());
        item.put("logoUrl", company.getLogoUrl());
        item.put("documentUrl", company.getDocumentUrl());
        item.put("status", company.getStatus());
        item.put("annualContractValue", company.getAnnualContractValue());
        item.put("monthlyBudget", company.getMonthlyBudget());
        item.put("reviewNotes", company.getReviewNotes());
        item.put("createdAt", company.getCreatedAt());
        return item;
    }

    private Map<String, Object> mapLocation(CorporateLocation location) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", location.getId());
        item.put("companyId", location.getCompanyId());
        item.put("companyName", companyName(location.getCompanyId()));
        item.put("name", location.getName());
        item.put("city", location.getCity());
        item.put("address", location.getAddress());
        item.put("status", location.getStatus());
        item.put("employeeCount", location.getEmployeeCount());
        item.put("createdAt", location.getCreatedAt());
        return item;
    }

    private Map<String, Object> mapDepartment(CorporateDepartment department) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", department.getId());
        item.put("companyId", department.getCompanyId());
        item.put("companyName", companyName(department.getCompanyId()));
        item.put("name", department.getName());
        item.put("parentDepartment", department.getParentDepartment());
        item.put("costCenter", department.getCostCenter());
        item.put("status", department.getStatus());
        item.put("createdAt", department.getCreatedAt());
        return item;
    }

    private Map<String, Object> mapEmployee(CorporateEmployee employee) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", employee.getId());
        item.put("companyId", employee.getCompanyId());
        item.put("companyName", companyName(employee.getCompanyId()));
        item.put("locationId", employee.getLocationId());
        item.put("departmentId", employee.getDepartmentId());
        item.put("name", employee.getName());
        item.put("email", employee.getEmail());
        item.put("phone", employee.getPhone());
        item.put("employeeCode", employee.getEmployeeCode());
        item.put("role", employee.getRole());
        item.put("status", employee.getStatus());
        item.put("wellnessScore", employee.getWellnessScore());
        item.put("participationScore", employee.getParticipationScore());
        item.put("rewardBalance", employee.getRewardBalance());
        item.put("createdAt", employee.getCreatedAt());
        return item;
    }

    private Map<String, Object> mapTeam(CorporateTeam team) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", team.getId());
        item.put("companyId", team.getCompanyId());
        item.put("companyName", companyName(team.getCompanyId()));
        item.put("departmentId", team.getDepartmentId());
        item.put("name", team.getName());
        item.put("sport", team.getSport());
        item.put("captainEmployeeId", team.getCaptainEmployeeId());
        item.put("status", team.getStatus());
        item.put("memberCount", team.getMemberCount());
        item.put("createdAt", team.getCreatedAt());
        return item;
    }

    private Map<String, Object> mapMembership(CorporateTeamMembership membership) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", membership.getId());
        item.put("teamId", membership.getTeamId());
        item.put("employeeId", membership.getEmployeeId());
        item.put("role", membership.getRole());
        item.put("status", membership.getStatus());
        item.put("createdAt", membership.getCreatedAt());
        return item;
    }

    private Map<String, Object> mapBookingPlan(CorporateBookingPlan plan) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", plan.getId());
        item.put("companyId", plan.getCompanyId());
        item.put("companyName", companyName(plan.getCompanyId()));
        item.put("departmentId", plan.getDepartmentId());
        item.put("sport", plan.getSport());
        item.put("turfName", plan.getTurfName());
        item.put("city", plan.getCity());
        item.put("recurrenceType", plan.getRecurrenceType());
        item.put("startDate", plan.getStartDate());
        item.put("endDate", plan.getEndDate());
        item.put("generatedBookings", plan.getGeneratedBookings());
        item.put("monthlySpend", plan.getMonthlySpend());
        item.put("status", plan.getStatus());
        item.put("conflictNotes", plan.getConflictNotes());
        item.put("createdAt", plan.getCreatedAt());
        return item;
    }

    private Map<String, Object> mapTournament(CorporateTournament tournament) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", tournament.getId());
        item.put("companyId", tournament.getCompanyId());
        item.put("companyName", companyName(tournament.getCompanyId()));
        item.put("name", tournament.getName());
        item.put("sport", tournament.getSport());
        item.put("scope", tournament.getScope());
        item.put("startDate", tournament.getStartDate());
        item.put("endDate", tournament.getEndDate());
        item.put("teamCount", tournament.getTeamCount());
        item.put("fixturesGenerated", tournament.getFixturesGenerated());
        item.put("status", tournament.getStatus());
        item.put("rankingStatus", tournament.getRankingStatus());
        item.put("createdAt", tournament.getCreatedAt());
        return item;
    }

    private Map<String, Object> mapWellnessProgram(CorporateWellnessProgram program) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", program.getId());
        item.put("companyId", program.getCompanyId());
        item.put("companyName", companyName(program.getCompanyId()));
        item.put("title", program.getTitle());
        item.put("sport", program.getSport());
        item.put("programType", program.getProgramType());
        item.put("startDate", program.getStartDate());
        item.put("endDate", program.getEndDate());
        item.put("enrolledEmployees", program.getEnrolledEmployees());
        item.put("completedEmployees", program.getCompletedEmployees());
        item.put("rewardPoints", program.getRewardPoints());
        item.put("status", program.getStatus());
        item.put("createdAt", program.getCreatedAt());
        return item;
    }

    private Map<String, Object> mapWellnessActivity(CorporateWellnessActivity activity) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", activity.getId());
        item.put("programId", activity.getProgramId());
        item.put("employeeId", activity.getEmployeeId());
        item.put("activityType", activity.getActivityType());
        item.put("attendanceStatus", activity.getAttendanceStatus());
        item.put("sportsHours", activity.getSportsHours());
        item.put("completed", activity.getCompleted());
        item.put("createdAt", activity.getCreatedAt());
        return item;
    }

    private Map<String, Object> mapReward(CorporateRewardTransaction reward) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", reward.getId());
        item.put("companyId", reward.getCompanyId());
        item.put("employeeId", reward.getEmployeeId());
        item.put("sourceType", reward.getSourceType());
        item.put("points", reward.getPoints());
        item.put("balanceAfter", reward.getBalanceAfter());
        item.put("status", reward.getStatus());
        item.put("summary", reward.getSummary());
        item.put("createdAt", reward.getCreatedAt());
        return item;
    }

    private Map<String, Object> mapInvoice(CorporateInvoice invoice) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", invoice.getId());
        item.put("companyId", invoice.getCompanyId());
        item.put("companyName", companyName(invoice.getCompanyId()));
        item.put("invoiceNumber", invoice.getInvoiceNumber());
        item.put("billingPeriod", invoice.getBillingPeriod());
        item.put("amount", invoice.getAmount());
        item.put("taxAmount", invoice.getTaxAmount());
        item.put("gstAmount", invoice.getGstAmount());
        item.put("totalAmount", invoice.getTotalAmount());
        item.put("dueDate", invoice.getDueDate());
        item.put("status", invoice.getStatus());
        item.put("createdAt", invoice.getCreatedAt());
        return item;
    }

    private Map<String, Object> mapContract(CorporateContract contract) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", contract.getId());
        item.put("companyId", contract.getCompanyId());
        item.put("companyName", companyName(contract.getCompanyId()));
        item.put("contractNumber", contract.getContractNumber());
        item.put("planName", contract.getPlanName());
        item.put("annualValue", contract.getAnnualValue());
        item.put("startDate", contract.getStartDate());
        item.put("endDate", contract.getEndDate());
        item.put("renewalProbability", contract.getRenewalProbability());
        item.put("status", contract.getStatus());
        item.put("createdAt", contract.getCreatedAt());
        return item;
    }

    private Map<String, Object> mapLeague(CorporateLeague league) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", league.getId());
        item.put("name", league.getName());
        item.put("sport", league.getSport());
        item.put("organizerCompanyId", league.getOrganizerCompanyId());
        item.put("organizerCompanyName", companyName(league.getOrganizerCompanyId()));
        item.put("invitedCompanyIds", league.getInvitedCompanyIds());
        item.put("registeredCompanies", league.getRegisteredCompanies());
        item.put("teamCount", league.getTeamCount());
        item.put("fixturesGenerated", league.getFixturesGenerated());
        item.put("rankingStatus", league.getRankingStatus());
        item.put("status", league.getStatus());
        item.put("createdAt", league.getCreatedAt());
        return item;
    }

    private Map<String, Object> mapAudit(CorporateEngagementAudit audit) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", audit.getId());
        item.put("companyId", audit.getCompanyId());
        item.put("employeeId", audit.getEmployeeId());
        item.put("referenceId", audit.getReferenceId());
        item.put("action", audit.getAction());
        item.put("actor", audit.getActor());
        item.put("summary", audit.getSummary());
        item.put("createdAt", audit.getCreatedAt());
        return item;
    }

    private CorporateCompany company(Long id) {
        if (id == null) throw ApiException.badRequest("Company id is required");
        return companyRepository.findById(id).orElseThrow(() -> ApiException.notFound("Company not found"));
    }

    private CorporateLocation location(Long id) {
        return locationRepository.findById(id).orElseThrow(() -> ApiException.notFound("Location not found"));
    }

    private CorporateDepartment department(Long id) {
        return departmentRepository.findById(id).orElseThrow(() -> ApiException.notFound("Department not found"));
    }

    private CorporateEmployee employee(Long id) {
        if (id == null) throw ApiException.badRequest("Employee id is required");
        return employeeRepository.findById(id).orElseThrow(() -> ApiException.notFound("Employee not found"));
    }

    private CorporateTeam team(Long id) {
        if (id == null) throw ApiException.badRequest("Team id is required");
        return teamRepository.findById(id).orElseThrow(() -> ApiException.notFound("Team not found"));
    }

    private CorporateWellnessProgram wellnessProgram(Long id) {
        if (id == null) throw ApiException.badRequest("Wellness program id is required");
        return wellnessProgramRepository.findById(id).orElseThrow(() -> ApiException.notFound("Wellness program not found"));
    }

    private CorporateInvoice invoice(Long id) {
        if (id == null) throw ApiException.badRequest("Invoice id is required");
        return invoiceRepository.findById(id).orElseThrow(() -> ApiException.notFound("Invoice not found"));
    }

    private CorporateContract contract(Long id) {
        if (id == null) throw ApiException.badRequest("Contract id is required");
        return contractRepository.findById(id).orElseThrow(() -> ApiException.notFound("Contract not found"));
    }

    private Map<Long, CorporateDepartment> departmentsById() {
        return departmentRepository.findAll().stream().collect(Collectors.toMap(CorporateDepartment::getId, Function.identity()));
    }

    private String companyName(Long companyId) {
        if (companyId == null) return null;
        return companyRepository.findById(companyId).map(CorporateCompany::getName).orElse(null);
    }

    private String departmentName(CorporateDepartment department) {
        return department == null ? "Unassigned" : department.getName();
    }

    private void audit(Long companyId, Long employeeId, Long referenceId, String action, String actor, String summary) {
        corporateAuditRepository.save(CorporateEngagementAudit.builder()
                .companyId(companyId)
                .employeeId(employeeId)
                .referenceId(referenceId)
                .action(action)
                .actor(defaultString(actor, "system"))
                .summary(summary)
                .build());
        auditLogRepository.save(AuditLog.builder()
                .adminId(defaultString(actor, "system"))
                .action(action)
                .entity("Corporate #" + (companyId == null ? "platform" : companyId))
                .build());
    }

    private Comparator<Object> byCreatedDesc() {
        return (left, right) -> {
            LocalDateTime leftTime = createdAt(left);
            LocalDateTime rightTime = createdAt(right);
            if (leftTime == null && rightTime == null) return 0;
            if (leftTime == null) return 1;
            if (rightTime == null) return -1;
            return rightTime.compareTo(leftTime);
        };
    }

    private LocalDateTime createdAt(Object object) {
        if (object instanceof CorporateCompany item) return item.getCreatedAt();
        if (object instanceof CorporateLocation item) return item.getCreatedAt();
        if (object instanceof CorporateDepartment item) return item.getCreatedAt();
        if (object instanceof CorporateEmployee item) return item.getCreatedAt();
        if (object instanceof CorporateTeam item) return item.getCreatedAt();
        if (object instanceof CorporateTeamMembership item) return item.getCreatedAt();
        if (object instanceof CorporateBookingPlan item) return item.getCreatedAt();
        if (object instanceof CorporateTournament item) return item.getCreatedAt();
        if (object instanceof CorporateWellnessProgram item) return item.getCreatedAt();
        if (object instanceof CorporateWellnessActivity item) return item.getCreatedAt();
        if (object instanceof CorporateRewardTransaction item) return item.getCreatedAt();
        if (object instanceof CorporateInvoice item) return item.getCreatedAt();
        if (object instanceof CorporateContract item) return item.getCreatedAt();
        if (object instanceof CorporateLeague item) return item.getCreatedAt();
        return null;
    }

    private int generatedBookings(String recurrence, LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) return 1;
        long days = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
        return switch (recurrence) {
            case "WEEKLY" -> (int) Math.max(1, Math.ceil(days / 7.0));
            case "MONTHLY" -> (int) Math.max(1, Math.ceil(days / 30.0));
            case "YEARLY" -> (int) Math.max(1, Math.ceil(days / 365.0));
            default -> 1;
        };
    }

    private String required(String value, String message) {
        String normalized = blankToNull(value);
        if (normalized == null) throw ApiException.badRequest(message);
        return normalized;
    }

    private String normalize(String value, String fallback) {
        String normalized = blankToNull(value);
        return normalized == null ? fallback : normalized.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
    }

    private String blankToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private String defaultString(String value, String fallback) {
        return blankToNull(value) == null ? fallback : value;
    }

    private boolean matches(String filter, String value) {
        return filter == null || filter.isBlank() || (value != null && value.toLowerCase(Locale.ROOT).contains(filter.toLowerCase(Locale.ROOT)));
    }

    private void validateOptionalEmail(String email, String label) {
        if (email != null && !email.isBlank() && !email.contains("@")) {
            throw ApiException.badRequest(label + " is invalid");
        }
    }

    private void validateGst(String gstNumber) {
        if (gstNumber != null && !gstNumber.isBlank() && gstNumber.trim().length() < 10) {
            throw ApiException.badRequest("GST number is invalid");
        }
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : Math.max(0, value);
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private double percentage(long numerator, long denominator) {
        if (denominator <= 0) return 0;
        return round((numerator * 100.0) / denominator);
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
