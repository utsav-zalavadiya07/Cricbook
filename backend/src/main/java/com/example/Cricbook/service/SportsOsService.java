package com.example.Cricbook.service;

import com.example.Cricbook.dto.SportsOsRequests;
import com.example.Cricbook.entity.*;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SportsOsService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Set<String> FACILITY_STATUSES = Set.of("ACTIVE", "CLOSED", "ARCHIVED", "MAINTENANCE", "PENDING_APPROVAL");
    private static final Set<String> MAINTENANCE_STATUSES = Set.of("OPERATIONAL", "MAINTENANCE_REQUIRED", "UNDER_REPAIR", "SAFETY_HOLD", "CLOSED", "PENDING");
    private static final Set<String> JURISDICTION_LEVELS = Set.of("MUNICIPAL", "CITY", "DISTRICT", "STATE", "NATIONAL", "SCHOOL", "UNIVERSITY");
    private static final Set<String> OWNERSHIP_TYPES = Set.of("GOVERNMENT", "MUNICIPAL", "UNIVERSITY", "SCHOOL", "PPP", "PRIVATE_PARTNERSHIP");
    private static final Set<String> ATHLETE_LEVELS = Set.of("SCHOOL", "DISTRICT", "STATE", "NATIONAL");
    private static final Set<String> GRANT_REVIEW_ACTIONS = Set.of("APPROVE", "REJECT");

    private final SportsOsFacilityRepository facilityRepository;
    private final SportsCensusSubmissionRepository censusSubmissionRepository;
    private final GovernmentGrantApplicationRepository grantApplicationRepository;
    private final GovernmentGrantDisbursementRepository grantDisbursementRepository;
    private final AthleteDevelopmentRecordRepository athleteDevelopmentRecordRepository;
    private final SportsVolunteerProfileRepository volunteerProfileRepository;
    private final SportsVolunteerAssignmentRepository volunteerAssignmentRepository;
    private final FacilityInspectionReportRepository inspectionReportRepository;
    private final SportsOsAuditRepository auditRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> dashboard() {
        List<SportsOsFacility> facilities = facilityRepository.findAllByOrderByCreatedAtDesc();
        List<SportsCensusSubmission> census = censusSubmissionRepository.findAllByOrderByCreatedAtDesc();
        List<GovernmentGrantApplication> grants = grantApplicationRepository.findAllByOrderByCreatedAtDesc();
        List<AthleteDevelopmentRecord> athletes = athleteDevelopmentRecordRepository.findAllByOrderByCreatedAtDesc();
        List<SportsVolunteerProfile> volunteers = volunteerProfileRepository.findAllByOrderByCreatedAtDesc();
        List<FacilityInspectionReport> inspections = inspectionReportRepository.findAllByOrderByCreatedAtDesc();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("kpis", kpis(facilities, census, grants, athletes, volunteers, inspections));
        response.put("facilities", facilities.stream().limit(12).map(this::mapFacility).toList());
        response.put("census", census.stream().limit(12).map(this::mapCensus).toList());
        response.put("grants", grants.stream().limit(12).map(this::mapGrant).toList());
        response.put("athletes", athletes.stream().limit(12).map(this::mapAthlete).toList());
        response.put("volunteers", volunteers.stream().limit(12).map(this::mapVolunteer).toList());
        response.put("inspections", inspections.stream().limit(12).map(this::mapInspection).toList());
        response.put("stateRollups", stateRollups(facilities, census, grants));
        response.put("priorityControls", priorityControls());
        response.put("generatedAt", LocalDateTime.now());
        return response;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> adminDashboard() {
        Map<String, Object> response = dashboard();
        response.put("grantDisbursements", grantDisbursementRepository.findAllByOrderByCreatedAtDesc().stream().limit(25).map(this::mapGrantDisbursement).toList());
        response.put("volunteerAssignments", volunteerAssignmentRepository.findAllByOrderByAssignedAtDesc().stream().limit(25).map(this::mapVolunteerAssignment).toList());
        response.put("audits", audits());
        response.put("governanceControls", governanceControls());
        return response;
    }

    @Transactional
    public Map<String, Object> registerFacility(SportsOsRequests.FacilityRequest request, String actor) {
        if (request == null) {
            throw ApiException.badRequest("Facility request is required");
        }
        String name = required(request.getName(), "Facility name is required");
        String district = required(request.getDistrictName(), "District is required");
        if (facilityRepository.existsByNameIgnoreCaseAndDistrictNameIgnoreCase(name, district)) {
            throw ApiException.conflict("Facility already exists in this district");
        }
        String jurisdictionLevel = normalize(request.getJurisdictionLevel(), "DISTRICT");
        if (!JURISDICTION_LEVELS.contains(jurisdictionLevel)) {
            throw ApiException.badRequest("Unsupported jurisdiction level");
        }
        String ownershipType = normalize(request.getOwnershipType(), "GOVERNMENT");
        if (!OWNERSHIP_TYPES.contains(ownershipType)) {
            throw ApiException.badRequest("Unsupported facility ownership type");
        }
        validateCoordinates(request.getLatitude(), request.getLongitude());
        SportsOsFacility facility = SportsOsFacility.builder()
                .facilityCode(generateFacilityCode())
                .name(name)
                .facilityType(normalize(request.getFacilityType(), "SPORTS_COMPLEX"))
                .ownershipType(ownershipType)
                .jurisdictionLevel(jurisdictionLevel)
                .stateName(required(request.getStateName(), "State is required"))
                .districtName(district)
                .cityName(blankToNull(request.getCityName()))
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .capacity(nonNegative(request.getCapacity()))
                .activeSlots(nonNegative(request.getActiveSlots()))
                .publicBookingEnabled(Boolean.TRUE.equals(request.getPublicBookingEnabled()))
                .ownerDepartment(blankToNull(request.getOwnerDepartment()))
                .images(blankToNull(request.getImages()))
                .build();
        SportsOsFacility saved = facilityRepository.save(facility);
        audit("SPORTS_OS_FACILITY", saved.getId(), "FACILITY_REGISTERED", actor, saved.getFacilityCode() + " registered");
        return mapFacility(saved);
    }

    @Transactional
    public Map<String, Object> updateFacilityStatus(Long facilityId, SportsOsRequests.FacilityStatusRequest request, String actor) {
        SportsOsFacility facility = facility(facilityId);
        if (request == null) {
            throw ApiException.badRequest("Facility status request is required");
        }
        if (request.getStatus() != null) {
            String status = normalize(request.getStatus(), facility.getStatus());
            if (!FACILITY_STATUSES.contains(status)) {
                throw ApiException.badRequest("Unsupported facility status");
            }
            facility.setStatus(status);
        }
        if (request.getMaintenanceStatus() != null) {
            String maintenanceStatus = normalize(request.getMaintenanceStatus(), facility.getMaintenanceStatus());
            if (!MAINTENANCE_STATUSES.contains(maintenanceStatus)) {
                throw ApiException.badRequest("Unsupported maintenance status");
            }
            facility.setMaintenanceStatus(maintenanceStatus);
        }
        if (request.getUtilizationPercent() != null) {
            facility.setUtilizationPercent(percent(request.getUtilizationPercent(), "Utilization percent"));
        }
        if (request.getPublicBookingEnabled() != null) {
            facility.setPublicBookingEnabled(request.getPublicBookingEnabled());
        }
        SportsOsFacility saved = facilityRepository.save(facility);
        audit("SPORTS_OS_FACILITY", saved.getId(), "FACILITY_STATUS_UPDATED", actor, saved.getStatus() + "/" + saved.getMaintenanceStatus());
        return mapFacility(saved);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> searchFacilities(String query) {
        String normalized = blankToNull(query);
        List<SportsOsFacility> facilities = normalized == null
                ? facilityRepository.findByStatusOrderByCreatedAtDesc("ACTIVE")
                : facilityRepository.findByNameContainingIgnoreCaseOrCityNameContainingIgnoreCaseOrDistrictNameContainingIgnoreCaseOrFacilityTypeContainingIgnoreCaseOrderByCreatedAtDesc(normalized, normalized, normalized, normalized);
        return facilities.stream().map(this::mapFacility).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> facilityDetails(Long facilityId) {
        SportsOsFacility facility = facility(facilityId);
        Map<String, Object> response = mapFacility(facility);
        response.put("inspections", inspectionReportRepository.findByFacilityIdOrderByCreatedAtDesc(facilityId).stream().map(this::mapInspection).toList());
        return response;
    }

    @Transactional
    public Map<String, Object> submitCensus(SportsOsRequests.CensusSubmissionRequest request, String actor) {
        if (request == null) {
            throw ApiException.badRequest("Census submission request is required");
        }
        SportsCensusSubmission submission = SportsCensusSubmission.builder()
                .cycleCode(defaultString(request.getCycleCode(), "CENSUS-" + LocalDateTime.now().getYear()).toUpperCase(Locale.ROOT))
                .jurisdictionLevel(normalize(request.getJurisdictionLevel(), "DISTRICT"))
                .stateName(required(request.getStateName(), "State is required"))
                .districtName(required(request.getDistrictName(), "District is required"))
                .submittedBy(blankToNull(request.getSubmittedBy()))
                .playerCount(nonNegative(request.getPlayerCount()))
                .coachCount(nonNegative(request.getCoachCount()))
                .academyCount(nonNegative(request.getAcademyCount()))
                .facilityCount(nonNegative(request.getFacilityCount()))
                .schoolCount(nonNegative(request.getSchoolCount()))
                .universityCount(nonNegative(request.getUniversityCount()))
                .build();
        if (!JURISDICTION_LEVELS.contains(submission.getJurisdictionLevel())) {
            throw ApiException.badRequest("Unsupported census jurisdiction level");
        }
        SportsCensusSubmission saved = censusSubmissionRepository.save(submission);
        audit("SPORTS_CENSUS", saved.getId(), "CENSUS_SUBMITTED", actor, saved.getCycleCode() + " for " + saved.getDistrictName());
        return mapCensus(saved);
    }

    @Transactional
    public Map<String, Object> applyGrant(SportsOsRequests.GrantApplicationRequest request, String actor) {
        if (request == null) {
            throw ApiException.badRequest("Grant application request is required");
        }
        BigDecimal requestedAmount = money(request.getRequestedAmount(), "Requested grant amount");
        GovernmentGrantApplication grant = GovernmentGrantApplication.builder()
                .grantCode(generateGrantCode())
                .applicantName(required(request.getApplicantName(), "Applicant name is required"))
                .applicantType(normalize(request.getApplicantType(), "DISTRICT_AUTHORITY"))
                .programType(normalize(request.getProgramType(), "SPORTS_INFRASTRUCTURE"))
                .stateName(required(request.getStateName(), "State is required"))
                .districtName(required(request.getDistrictName(), "District is required"))
                .requestedAmount(requestedAmount)
                .supportingDocuments(blankToNull(request.getSupportingDocuments()))
                .submittedAt(LocalDateTime.now())
                .build();
        GovernmentGrantApplication saved = grantApplicationRepository.save(grant);
        audit("GOVERNMENT_GRANT", saved.getId(), "GRANT_APPLIED", actor, saved.getGrantCode() + " requested " + requestedAmount);
        return mapGrant(saved);
    }

    @Transactional
    public Map<String, Object> reviewGrant(Long grantId, SportsOsRequests.GrantReviewRequest request, String actor) {
        GovernmentGrantApplication grant = grant(grantId);
        if (request == null) {
            throw ApiException.badRequest("Grant review request is required");
        }
        if (!"SUBMITTED".equalsIgnoreCase(grant.getStatus())) {
            throw ApiException.badRequest("Only submitted grants can be reviewed");
        }
        String action = normalize(request.getAction(), "APPROVE");
        if (!GRANT_REVIEW_ACTIONS.contains(action)) {
            throw ApiException.badRequest("Unsupported grant review action");
        }
        if ("APPROVE".equals(action)) {
            BigDecimal approved = money(request.getApprovedAmount(), "Approved grant amount");
            if (approved.compareTo(grant.getRequestedAmount()) > 0) {
                throw ApiException.badRequest("Approved grant amount cannot exceed requested amount");
            }
            grant.setApprovedAmount(approved);
            grant.setStatus("APPROVED");
        } else {
            grant.setApprovedAmount(BigDecimal.ZERO);
            grant.setStatus("REJECTED");
        }
        grant.setReviewNote(blankToNull(request.getReviewNote()));
        grant.setReviewedAt(LocalDateTime.now());
        GovernmentGrantApplication saved = grantApplicationRepository.save(grant);
        audit("GOVERNMENT_GRANT", saved.getId(), "GRANT_" + saved.getStatus(), actor, saved.getReviewNote());
        return mapGrant(saved);
    }

    @Transactional
    public Map<String, Object> disburseGrant(Long grantId, SportsOsRequests.GrantDisbursementRequest request, String actor) {
        GovernmentGrantApplication grant = grant(grantId);
        if (request == null) {
            throw ApiException.badRequest("Grant disbursement request is required");
        }
        if (!Set.of("APPROVED", "PARTIALLY_DISBURSED").contains(grant.getStatus())) {
            throw ApiException.badRequest("Only approved grants can be disbursed");
        }
        BigDecimal amount = money(request.getAmount(), "Disbursement amount");
        BigDecimal remaining = grant.getApprovedAmount().subtract(defaultMoney(grant.getDisbursedAmount()));
        if (amount.compareTo(remaining) > 0) {
            throw ApiException.badRequest("Disbursement amount exceeds remaining approved grant balance");
        }
        GovernmentGrantDisbursement disbursement = GovernmentGrantDisbursement.builder()
                .grantApplicationId(grantId)
                .stageName(defaultString(request.getStageName(), "STAGE_1").toUpperCase(Locale.ROOT))
                .amount(amount)
                .note(blankToNull(request.getNote()))
                .disbursedAt(request.getDisbursedAt() == null ? LocalDateTime.now() : request.getDisbursedAt())
                .build();
        GovernmentGrantDisbursement savedDisbursement = grantDisbursementRepository.save(disbursement);
        grant.setDisbursedAmount(defaultMoney(grant.getDisbursedAmount()).add(amount));
        grant.setStatus(grant.getDisbursedAmount().compareTo(grant.getApprovedAmount()) >= 0 ? "DISBURSED" : "PARTIALLY_DISBURSED");
        GovernmentGrantApplication savedGrant = grantApplicationRepository.save(grant);
        audit("GOVERNMENT_GRANT", savedGrant.getId(), "GRANT_DISBURSED", actor, amount + " via " + savedDisbursement.getStageName());
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("grant", mapGrant(savedGrant));
        response.put("disbursement", mapGrantDisbursement(savedDisbursement));
        return response;
    }

    @Transactional
    public Map<String, Object> trackAthlete(SportsOsRequests.AthleteDevelopmentRequest request, String actor) {
        if (request == null) {
            throw ApiException.badRequest("Athlete development request is required");
        }
        String currentLevel = normalize(request.getCurrentLevel(), "SCHOOL");
        if (!ATHLETE_LEVELS.contains(currentLevel)) {
            throw ApiException.badRequest("Unsupported athlete development level");
        }
        AthleteDevelopmentRecord record = AthleteDevelopmentRecord.builder()
                .sportsIdentityId(request.getSportsIdentityId())
                .athleteName(required(request.getAthleteName(), "Athlete name is required"))
                .schoolOrInstitution(blankToNull(request.getSchoolOrInstitution()))
                .sport(defaultString(request.getSport(), "CRICKET").toUpperCase(Locale.ROOT))
                .ageGroup(normalize(request.getAgeGroup(), "OPEN"))
                .districtName(required(request.getDistrictName(), "District is required"))
                .stateName(required(request.getStateName(), "State is required"))
                .currentLevel(currentLevel)
                .selectionStatus(normalize(request.getSelectionStatus(), "TRACKING"))
                .selectionHistory(blankToNull(request.getSelectionHistory()))
                .progressScore(score(request.getProgressScore(), 50))
                .build();
        AthleteDevelopmentRecord saved = athleteDevelopmentRecordRepository.save(record);
        audit("ATHLETE_DEVELOPMENT", saved.getId(), "ATHLETE_TRACKED", actor, saved.getAthleteName() + " at " + saved.getCurrentLevel());
        return mapAthlete(saved);
    }

    @Transactional
    public Map<String, Object> registerVolunteer(SportsOsRequests.VolunteerRequest request, String actor) {
        if (request == null) {
            throw ApiException.badRequest("Volunteer request is required");
        }
        String email = required(request.getEmail(), "Volunteer email is required").toLowerCase(Locale.ROOT);
        if (volunteerProfileRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("Volunteer already exists for this email");
        }
        SportsVolunteerProfile volunteer = SportsVolunteerProfile.builder()
                .volunteerCode(generateVolunteerCode())
                .fullName(required(request.getFullName(), "Volunteer name is required"))
                .email(email)
                .phone(blankToNull(request.getPhone()))
                .stateName(required(request.getStateName(), "State is required"))
                .districtName(required(request.getDistrictName(), "District is required"))
                .category(normalize(request.getCategory(), "EVENT"))
                .verificationStatus(normalize(request.getVerificationStatus(), "PENDING"))
                .build();
        SportsVolunteerProfile saved = volunteerProfileRepository.save(volunteer);
        audit("SPORTS_VOLUNTEER", saved.getId(), "VOLUNTEER_REGISTERED", actor, saved.getVolunteerCode());
        return mapVolunteer(saved);
    }

    @Transactional
    public Map<String, Object> assignVolunteer(Long volunteerId, SportsOsRequests.VolunteerAssignmentRequest request, String actor) {
        SportsVolunteerProfile volunteer = volunteer(volunteerId);
        if (request == null) {
            throw ApiException.badRequest("Volunteer assignment request is required");
        }
        SportsVolunteerAssignment assignment = SportsVolunteerAssignment.builder()
                .volunteerId(volunteerId)
                .assignmentType(normalize(request.getAssignmentType(), "EVENT"))
                .assignmentRefId(request.getAssignmentRefId())
                .title(required(request.getTitle(), "Assignment title is required"))
                .districtName(defaultString(request.getDistrictName(), volunteer.getDistrictName()))
                .build();
        SportsVolunteerAssignment saved = volunteerAssignmentRepository.save(assignment);
        audit("SPORTS_VOLUNTEER_ASSIGNMENT", saved.getId(), "VOLUNTEER_ASSIGNED", actor, volunteer.getVolunteerCode() + " -> " + saved.getTitle());
        return mapVolunteerAssignment(saved);
    }

    @Transactional
    public Map<String, Object> scheduleInspection(SportsOsRequests.InspectionRequest request, String actor) {
        if (request == null) {
            throw ApiException.badRequest("Inspection request is required");
        }
        SportsOsFacility facility = facility(requiredId(request.getFacilityId(), "Facility ID is required"));
        FacilityInspectionReport inspection = FacilityInspectionReport.builder()
                .facilityId(facility.getId())
                .inspectorName(required(request.getInspectorName(), "Inspector name is required"))
                .inspectionType(normalize(request.getInspectionType(), "ROUTINE"))
                .score(score(request.getScore(), 0))
                .safetyStatus(normalize(request.getSafetyStatus(), "PENDING"))
                .maintenanceStatus(normalize(request.getMaintenanceStatus(), "PENDING"))
                .recommendation(blankToNull(request.getRecommendation()))
                .status(normalize(request.getStatus(), "SCHEDULED"))
                .scheduledAt(request.getScheduledAt() == null ? LocalDateTime.now() : request.getScheduledAt())
                .completedAt(request.getCompletedAt())
                .build();
        FacilityInspectionReport saved = inspectionReportRepository.save(inspection);
        audit("FACILITY_INSPECTION", saved.getId(), "INSPECTION_SCHEDULED", actor, facility.getFacilityCode());
        return mapInspection(saved);
    }

    @Transactional
    public Map<String, Object> completeInspection(Long inspectionId, SportsOsRequests.InspectionRequest request, String actor) {
        FacilityInspectionReport inspection = inspectionReportRepository.findById(inspectionId)
                .orElseThrow(() -> ApiException.notFound("Facility inspection not found"));
        SportsOsFacility facility = facility(inspection.getFacilityId());
        if (request != null) {
            if (request.getScore() != null) {
                inspection.setScore(score(request.getScore(), inspection.getScore()));
            }
            if (request.getSafetyStatus() != null) {
                inspection.setSafetyStatus(normalize(request.getSafetyStatus(), inspection.getSafetyStatus()));
            }
            if (request.getMaintenanceStatus() != null) {
                inspection.setMaintenanceStatus(normalize(request.getMaintenanceStatus(), inspection.getMaintenanceStatus()));
            }
            if (request.getRecommendation() != null) {
                inspection.setRecommendation(blankToNull(request.getRecommendation()));
            }
        }
        inspection.setStatus("COMPLETED");
        inspection.setCompletedAt(LocalDateTime.now());
        FacilityInspectionReport saved = inspectionReportRepository.save(inspection);
        facility.setMaintenanceStatus(normalize(saved.getMaintenanceStatus(), facility.getMaintenanceStatus()));
        if ("SAFETY_HOLD".equalsIgnoreCase(saved.getSafetyStatus())) {
            facility.setStatus("MAINTENANCE");
        }
        facilityRepository.save(facility);
        audit("FACILITY_INSPECTION", saved.getId(), "INSPECTION_COMPLETED", actor, facility.getFacilityCode() + " score " + saved.getScore());
        return mapInspection(saved);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> facilities() {
        return facilityRepository.findAllByOrderByCreatedAtDesc().stream().map(this::mapFacility).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> censusSubmissions() {
        return censusSubmissionRepository.findAllByOrderByCreatedAtDesc().stream().map(this::mapCensus).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> grants() {
        return grantApplicationRepository.findAllByOrderByCreatedAtDesc().stream().map(this::mapGrant).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> grantDisbursements() {
        return grantDisbursementRepository.findAllByOrderByCreatedAtDesc().stream().map(this::mapGrantDisbursement).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> athletes() {
        return athleteDevelopmentRecordRepository.findAllByOrderByCreatedAtDesc().stream().map(this::mapAthlete).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> volunteers() {
        return volunteerProfileRepository.findAllByOrderByCreatedAtDesc().stream().map(this::mapVolunteer).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> inspections() {
        return inspectionReportRepository.findAllByOrderByCreatedAtDesc().stream().map(this::mapInspection).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> audits() {
        return auditRepository.findAllByOrderByCreatedAtDesc().stream().limit(50).map(this::mapAudit).toList();
    }

    private Map<String, Object> kpis(List<SportsOsFacility> facilities,
                                     List<SportsCensusSubmission> census,
                                     List<GovernmentGrantApplication> grants,
                                     List<AthleteDevelopmentRecord> athletes,
                                     List<SportsVolunteerProfile> volunteers,
                                     List<FacilityInspectionReport> inspections) {
        Map<String, Object> kpis = new LinkedHashMap<>();
        kpis.put("facilities", facilities.size());
        kpis.put("activeFacilities", facilities.stream().filter(facility -> "ACTIVE".equalsIgnoreCase(facility.getStatus())).count());
        kpis.put("publicBookableFacilities", facilities.stream().filter(facility -> Boolean.TRUE.equals(facility.getPublicBookingEnabled())).count());
        kpis.put("averageUtilization", average(facilities.stream().map(SportsOsFacility::getUtilizationPercent).toList()));
        kpis.put("censusPlayers", census.stream().mapToInt(item -> defaultInt(item.getPlayerCount())).sum());
        kpis.put("censusCoaches", census.stream().mapToInt(item -> defaultInt(item.getCoachCount())).sum());
        kpis.put("censusFacilities", census.stream().mapToInt(item -> defaultInt(item.getFacilityCount())).sum());
        kpis.put("pendingGrants", grants.stream().filter(grant -> "SUBMITTED".equalsIgnoreCase(grant.getStatus())).count());
        kpis.put("approvedGrantValue", grants.stream().map(GovernmentGrantApplication::getApprovedAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
        kpis.put("disbursedGrantValue", grants.stream().map(GovernmentGrantApplication::getDisbursedAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
        kpis.put("athleteRecords", athletes.size());
        kpis.put("nationalAthletes", athletes.stream().filter(athlete -> "NATIONAL".equalsIgnoreCase(athlete.getCurrentLevel())).count());
        kpis.put("volunteers", volunteers.size());
        kpis.put("verifiedVolunteers", volunteers.stream().filter(volunteer -> "VERIFIED".equalsIgnoreCase(volunteer.getVerificationStatus())).count());
        kpis.put("openInspections", inspections.stream().filter(inspection -> !"COMPLETED".equalsIgnoreCase(inspection.getStatus())).count());
        return kpis;
    }

    private List<Map<String, Object>> stateRollups(List<SportsOsFacility> facilities,
                                                   List<SportsCensusSubmission> census,
                                                   List<GovernmentGrantApplication> grants) {
        Map<String, Long> facilitiesByState = facilities.stream()
                .collect(Collectors.groupingBy(facility -> defaultString(facility.getStateName(), "UNKNOWN"), LinkedHashMap::new, Collectors.counting()));
        return facilitiesByState.entrySet().stream().map(entry -> {
            String state = entry.getKey();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("stateName", state);
            row.put("facilities", entry.getValue());
            row.put("players", census.stream().filter(item -> state.equalsIgnoreCase(defaultString(item.getStateName(), ""))).mapToInt(item -> defaultInt(item.getPlayerCount())).sum());
            row.put("approvedFunding", grants.stream().filter(grant -> state.equalsIgnoreCase(defaultString(grant.getStateName(), ""))).map(GovernmentGrantApplication::getApprovedAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
            row.put("disbursedFunding", grants.stream().filter(grant -> state.equalsIgnoreCase(defaultString(grant.getStateName(), ""))).map(GovernmentGrantApplication::getDisbursedAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
            return row;
        }).toList();
    }

    private List<Map<String, Object>> priorityControls() {
        return List.of(
                control("Sports Infrastructure Registry", "ACTIVE", "Facility uniqueness, ownership, coordinates and public booking readiness"),
                control("Grant Governance", "ACTIVE", "Approval amount cannot exceed request and disbursement cannot exceed approved balance"),
                control("Sports Census", "ACTIVE", "District, state and national counts aggregate from submitted records"),
                control("Facility Inspection", "ACTIVE", "Inspection outcome can move facilities into maintenance safety hold"),
                control("Public Accountability", "PLANNED", "Transparency exports and government audit certification remain a platform-wide stream")
        );
    }

    private List<Map<String, Object>> governanceControls() {
        return List.of(
                control("Cross-State Data Isolation", "CONTROLLED", "State and district fields are first-class on every Sports OS record"),
                control("Funding Reconciliation", "CONTROLLED", "Grant ledger records every disbursement stage"),
                control("Inspection Auditability", "CONTROLLED", "Facility lifecycle and inspection actions create immutable audit rows"),
                control("Public Service Usability", "CONTROLLED", "Public dashboard and facility discovery are separated from admin actions")
        );
    }

    private Map<String, Object> control(String name, String status, String detail) {
        Map<String, Object> control = new LinkedHashMap<>();
        control.put("name", name);
        control.put("status", status);
        control.put("detail", detail);
        return control;
    }

    private Map<String, Object> mapFacility(SportsOsFacility facility) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", facility.getId());
        map.put("facilityCode", facility.getFacilityCode());
        map.put("name", facility.getName());
        map.put("facilityType", facility.getFacilityType());
        map.put("ownershipType", facility.getOwnershipType());
        map.put("jurisdictionLevel", facility.getJurisdictionLevel());
        map.put("stateName", facility.getStateName());
        map.put("districtName", facility.getDistrictName());
        map.put("cityName", facility.getCityName());
        map.put("latitude", facility.getLatitude());
        map.put("longitude", facility.getLongitude());
        map.put("capacity", facility.getCapacity());
        map.put("activeSlots", facility.getActiveSlots());
        map.put("publicBookingEnabled", facility.getPublicBookingEnabled());
        map.put("status", facility.getStatus());
        map.put("maintenanceStatus", facility.getMaintenanceStatus());
        map.put("utilizationPercent", facility.getUtilizationPercent());
        map.put("ownerDepartment", facility.getOwnerDepartment());
        map.put("images", facility.getImages());
        map.put("createdAt", facility.getCreatedAt());
        map.put("updatedAt", facility.getUpdatedAt());
        return map;
    }

    private Map<String, Object> mapCensus(SportsCensusSubmission submission) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", submission.getId());
        map.put("cycleCode", submission.getCycleCode());
        map.put("jurisdictionLevel", submission.getJurisdictionLevel());
        map.put("stateName", submission.getStateName());
        map.put("districtName", submission.getDistrictName());
        map.put("submittedBy", submission.getSubmittedBy());
        map.put("playerCount", submission.getPlayerCount());
        map.put("coachCount", submission.getCoachCount());
        map.put("academyCount", submission.getAcademyCount());
        map.put("facilityCount", submission.getFacilityCount());
        map.put("schoolCount", submission.getSchoolCount());
        map.put("universityCount", submission.getUniversityCount());
        map.put("status", submission.getStatus());
        map.put("createdAt", submission.getCreatedAt());
        return map;
    }

    private Map<String, Object> mapGrant(GovernmentGrantApplication grant) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", grant.getId());
        map.put("grantCode", grant.getGrantCode());
        map.put("applicantName", grant.getApplicantName());
        map.put("applicantType", grant.getApplicantType());
        map.put("programType", grant.getProgramType());
        map.put("stateName", grant.getStateName());
        map.put("districtName", grant.getDistrictName());
        map.put("requestedAmount", grant.getRequestedAmount());
        map.put("approvedAmount", grant.getApprovedAmount());
        map.put("disbursedAmount", grant.getDisbursedAmount());
        map.put("status", grant.getStatus());
        map.put("supportingDocuments", grant.getSupportingDocuments());
        map.put("reviewNote", grant.getReviewNote());
        map.put("submittedAt", grant.getSubmittedAt());
        map.put("reviewedAt", grant.getReviewedAt());
        return map;
    }

    private Map<String, Object> mapGrantDisbursement(GovernmentGrantDisbursement disbursement) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", disbursement.getId());
        map.put("grantApplicationId", disbursement.getGrantApplicationId());
        map.put("stageName", disbursement.getStageName());
        map.put("amount", disbursement.getAmount());
        map.put("status", disbursement.getStatus());
        map.put("note", disbursement.getNote());
        map.put("disbursedAt", disbursement.getDisbursedAt());
        map.put("createdAt", disbursement.getCreatedAt());
        return map;
    }

    private Map<String, Object> mapAthlete(AthleteDevelopmentRecord athlete) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", athlete.getId());
        map.put("sportsIdentityId", athlete.getSportsIdentityId());
        map.put("athleteName", athlete.getAthleteName());
        map.put("schoolOrInstitution", athlete.getSchoolOrInstitution());
        map.put("sport", athlete.getSport());
        map.put("ageGroup", athlete.getAgeGroup());
        map.put("districtName", athlete.getDistrictName());
        map.put("stateName", athlete.getStateName());
        map.put("currentLevel", athlete.getCurrentLevel());
        map.put("selectionStatus", athlete.getSelectionStatus());
        map.put("selectionHistory", athlete.getSelectionHistory());
        map.put("progressScore", athlete.getProgressScore());
        map.put("createdAt", athlete.getCreatedAt());
        return map;
    }

    private Map<String, Object> mapVolunteer(SportsVolunteerProfile volunteer) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", volunteer.getId());
        map.put("volunteerCode", volunteer.getVolunteerCode());
        map.put("fullName", volunteer.getFullName());
        map.put("email", volunteer.getEmail());
        map.put("phone", volunteer.getPhone());
        map.put("stateName", volunteer.getStateName());
        map.put("districtName", volunteer.getDistrictName());
        map.put("category", volunteer.getCategory());
        map.put("verificationStatus", volunteer.getVerificationStatus());
        map.put("assignmentsCompleted", volunteer.getAssignmentsCompleted());
        map.put("createdAt", volunteer.getCreatedAt());
        return map;
    }

    private Map<String, Object> mapVolunteerAssignment(SportsVolunteerAssignment assignment) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", assignment.getId());
        map.put("volunteerId", assignment.getVolunteerId());
        map.put("assignmentType", assignment.getAssignmentType());
        map.put("assignmentRefId", assignment.getAssignmentRefId());
        map.put("title", assignment.getTitle());
        map.put("districtName", assignment.getDistrictName());
        map.put("attendanceStatus", assignment.getAttendanceStatus());
        map.put("assignedAt", assignment.getAssignedAt());
        return map;
    }

    private Map<String, Object> mapInspection(FacilityInspectionReport inspection) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", inspection.getId());
        map.put("facilityId", inspection.getFacilityId());
        map.put("inspectorName", inspection.getInspectorName());
        map.put("inspectionType", inspection.getInspectionType());
        map.put("score", inspection.getScore());
        map.put("safetyStatus", inspection.getSafetyStatus());
        map.put("maintenanceStatus", inspection.getMaintenanceStatus());
        map.put("recommendation", inspection.getRecommendation());
        map.put("status", inspection.getStatus());
        map.put("scheduledAt", inspection.getScheduledAt());
        map.put("completedAt", inspection.getCompletedAt());
        map.put("createdAt", inspection.getCreatedAt());
        return map;
    }

    private Map<String, Object> mapAudit(SportsOsAudit audit) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", audit.getId());
        map.put("entityType", audit.getEntityType());
        map.put("entityId", audit.getEntityId());
        map.put("action", audit.getAction());
        map.put("actor", audit.getActor());
        map.put("details", audit.getDetails());
        map.put("createdAt", audit.getCreatedAt());
        return map;
    }

    private SportsOsFacility facility(Long facilityId) {
        return facilityRepository.findById(requiredId(facilityId, "Facility ID is required"))
                .orElseThrow(() -> ApiException.notFound("Sports OS facility not found"));
    }

    private GovernmentGrantApplication grant(Long grantId) {
        return grantApplicationRepository.findById(requiredId(grantId, "Grant ID is required"))
                .orElseThrow(() -> ApiException.notFound("Government grant application not found"));
    }

    private SportsVolunteerProfile volunteer(Long volunteerId) {
        return volunteerProfileRepository.findById(requiredId(volunteerId, "Volunteer ID is required"))
                .orElseThrow(() -> ApiException.notFound("Sports volunteer not found"));
    }

    private void audit(String entityType, Long entityId, String action, String actor, String details) {
        auditRepository.save(SportsOsAudit.builder()
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .actor(defaultString(actor, "system"))
                .details(blankToNull(details))
                .build());
    }

    private String generateFacilityCode() {
        String code;
        do {
            code = "TSOS-FAC-" + LocalDateTime.now().getYear() + "-" + (100000 + RANDOM.nextInt(900000));
        } while (facilityRepository.existsByFacilityCode(code));
        return code;
    }

    private String generateGrantCode() {
        String code;
        do {
            code = "TSOS-GRT-" + LocalDateTime.now().getYear() + "-" + (100000 + RANDOM.nextInt(900000));
        } while (grantApplicationRepository.existsByGrantCode(code));
        return code;
    }

    private String generateVolunteerCode() {
        String code;
        do {
            code = "TSOS-VOL-" + LocalDateTime.now().getYear() + "-" + (100000 + RANDOM.nextInt(900000));
        } while (volunteerProfileRepository.existsByVolunteerCode(code));
        return code;
    }

    private void validateCoordinates(BigDecimal latitude, BigDecimal longitude) {
        if (latitude == null || longitude == null) {
            throw ApiException.badRequest("Facility latitude and longitude are required");
        }
        if (latitude.compareTo(BigDecimal.valueOf(-90)) < 0 || latitude.compareTo(BigDecimal.valueOf(90)) > 0) {
            throw ApiException.badRequest("Invalid facility latitude");
        }
        if (longitude.compareTo(BigDecimal.valueOf(-180)) < 0 || longitude.compareTo(BigDecimal.valueOf(180)) > 0) {
            throw ApiException.badRequest("Invalid facility longitude");
        }
    }

    private BigDecimal money(BigDecimal amount, String label) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw ApiException.badRequest(label + " must be greater than zero");
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal percent(BigDecimal value, String label) {
        if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw ApiException.badRequest(label + " must be between 0 and 100");
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal average(List<BigDecimal> values) {
        List<BigDecimal> usable = values.stream().filter(value -> value != null).toList();
        if (usable.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return usable.stream().reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(usable.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal defaultMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private int score(Integer value, int fallback) {
        int resolved = value == null ? fallback : value;
        if (resolved < 0 || resolved > 100) {
            throw ApiException.badRequest("Score must be between 0 and 100");
        }
        return resolved;
    }

    private int nonNegative(Integer value) {
        int resolved = value == null ? 0 : value;
        if (resolved < 0) {
            throw ApiException.badRequest("Numeric values cannot be negative");
        }
        return resolved;
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private Long requiredId(Long id, String message) {
        if (id == null || id <= 0) {
            throw ApiException.badRequest(message);
        }
        return id;
    }

    private String required(String value, String message) {
        String resolved = blankToNull(value);
        if (resolved == null) {
            throw ApiException.badRequest(message);
        }
        return resolved;
    }

    private String normalize(String value, String fallback) {
        return defaultString(value, fallback).trim().toUpperCase(Locale.ROOT).replace(' ', '_');
    }

    private String defaultString(String value, String fallback) {
        String resolved = blankToNull(value);
        return resolved == null ? fallback : resolved;
    }

    private String blankToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
