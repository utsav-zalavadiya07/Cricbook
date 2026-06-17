package com.example.Cricbook.service;

import com.example.Cricbook.dto.SportsOsRequests;
import com.example.Cricbook.entity.*;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SportsOsServiceTest {

    @Mock
    private SportsOsFacilityRepository facilityRepository;
    @Mock
    private SportsCensusSubmissionRepository censusSubmissionRepository;
    @Mock
    private GovernmentGrantApplicationRepository grantApplicationRepository;
    @Mock
    private GovernmentGrantDisbursementRepository grantDisbursementRepository;
    @Mock
    private AthleteDevelopmentRecordRepository athleteDevelopmentRecordRepository;
    @Mock
    private SportsVolunteerProfileRepository volunteerProfileRepository;
    @Mock
    private SportsVolunteerAssignmentRepository volunteerAssignmentRepository;
    @Mock
    private FacilityInspectionReportRepository inspectionReportRepository;
    @Mock
    private SportsOsAuditRepository auditRepository;

    @InjectMocks
    private SportsOsService sportsOsService;

    @BeforeEach
    void setUp() {
        lenient().when(auditRepository.save(any(SportsOsAudit.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void registerFacilityCreatesRegistryRecordAndBlocksBadCoordinates() {
        SportsOsRequests.FacilityRequest request = facilityRequest();

        when(facilityRepository.existsByNameIgnoreCaseAndDistrictNameIgnoreCase("Riverfront Sports Complex", "Ahmedabad")).thenReturn(false);
        when(facilityRepository.existsByFacilityCode(any())).thenReturn(false);
        when(facilityRepository.save(any(SportsOsFacility.class))).thenAnswer(invocation -> {
            SportsOsFacility facility = invocation.getArgument(0);
            facility.setId(1L);
            return facility;
        });

        Map<String, Object> response = sportsOsService.registerFacility(request, "sports-os-admin@example.com");

        assertEquals("Riverfront Sports Complex", response.get("name"));
        assertTrue(String.valueOf(response.get("facilityCode")).startsWith("TSOS-FAC-"));
        assertEquals("MUNICIPAL", response.get("ownershipType"));
        verify(auditRepository).save(any(SportsOsAudit.class));

        request.setLatitude(BigDecimal.valueOf(110));
        assertThrows(ApiException.class, () -> sportsOsService.registerFacility(request, "sports-os-admin@example.com"));
    }

    @Test
    void registerFacilityBlocksDuplicateFacilityInDistrict() {
        SportsOsRequests.FacilityRequest request = facilityRequest();
        when(facilityRepository.existsByNameIgnoreCaseAndDistrictNameIgnoreCase("Riverfront Sports Complex", "Ahmedabad")).thenReturn(true);

        assertThrows(ApiException.class, () -> sportsOsService.registerFacility(request, "sports-os-admin@example.com"));
        verify(facilityRepository, never()).save(any(SportsOsFacility.class));
    }

    @Test
    void grantApprovalAndDisbursementPreventOverFunding() {
        GovernmentGrantApplication grant = GovernmentGrantApplication.builder()
                .id(10L)
                .grantCode("TSOS-GRT-2026-111111")
                .applicantName("Ahmedabad District Sports Office")
                .applicantType("DISTRICT_AUTHORITY")
                .programType("YOUTH_SPORTS")
                .stateName("Gujarat")
                .districtName("Ahmedabad")
                .requestedAmount(BigDecimal.valueOf(10000))
                .approvedAmount(BigDecimal.ZERO)
                .disbursedAmount(BigDecimal.ZERO)
                .status("SUBMITTED")
                .build();
        SportsOsRequests.GrantReviewRequest reviewRequest = new SportsOsRequests.GrantReviewRequest();
        reviewRequest.setAction("APPROVE");
        reviewRequest.setApprovedAmount(BigDecimal.valueOf(6000));
        reviewRequest.setReviewNote("Phase-wise approval");

        when(grantApplicationRepository.findById(10L)).thenReturn(Optional.of(grant));
        when(grantApplicationRepository.save(any(GovernmentGrantApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> reviewed = sportsOsService.reviewGrant(10L, reviewRequest, "finance@example.com");

        assertEquals("APPROVED", reviewed.get("status"));
        assertEquals(0, BigDecimal.valueOf(6000).setScale(2).compareTo((BigDecimal) reviewed.get("approvedAmount")));

        SportsOsRequests.GrantDisbursementRequest overFunding = new SportsOsRequests.GrantDisbursementRequest();
        overFunding.setStageName("STAGE_1");
        overFunding.setAmount(BigDecimal.valueOf(7000));
        assertThrows(ApiException.class, () -> sportsOsService.disburseGrant(10L, overFunding, "finance@example.com"));

        SportsOsRequests.GrantDisbursementRequest firstDisbursement = new SportsOsRequests.GrantDisbursementRequest();
        firstDisbursement.setStageName("STAGE_1");
        firstDisbursement.setAmount(BigDecimal.valueOf(2500));
        when(grantDisbursementRepository.save(any(GovernmentGrantDisbursement.class))).thenAnswer(invocation -> {
            GovernmentGrantDisbursement disbursement = invocation.getArgument(0);
            disbursement.setId(20L);
            return disbursement;
        });

        Map<String, Object> disbursed = sportsOsService.disburseGrant(10L, firstDisbursement, "finance@example.com");
        Map<?, ?> grantResponse = (Map<?, ?>) disbursed.get("grant");

        assertEquals("PARTIALLY_DISBURSED", grantResponse.get("status"));
        assertEquals(0, BigDecimal.valueOf(2500).setScale(2).compareTo((BigDecimal) grantResponse.get("disbursedAmount")));
    }

    @Test
    void dashboardAggregatesCensusFacilitiesFundingAndOperationalSignals() {
        SportsOsFacility facility = SportsOsFacility.builder()
                .id(1L)
                .facilityCode("TSOS-FAC-2026-111111")
                .name("Public Ground")
                .facilityType("CRICKET_GROUND")
                .ownershipType("GOVERNMENT")
                .jurisdictionLevel("DISTRICT")
                .stateName("Gujarat")
                .districtName("Ahmedabad")
                .status("ACTIVE")
                .publicBookingEnabled(true)
                .utilizationPercent(BigDecimal.valueOf(60))
                .build();
        SportsCensusSubmission census = SportsCensusSubmission.builder()
                .id(2L)
                .cycleCode("CENSUS-2026")
                .jurisdictionLevel("DISTRICT")
                .stateName("Gujarat")
                .districtName("Ahmedabad")
                .playerCount(100)
                .coachCount(10)
                .facilityCount(3)
                .build();
        GovernmentGrantApplication grant = GovernmentGrantApplication.builder()
                .id(3L)
                .grantCode("TSOS-GRT-2026-222222")
                .applicantName("District Office")
                .applicantType("DISTRICT_AUTHORITY")
                .programType("SPORTS_INFRASTRUCTURE")
                .stateName("Gujarat")
                .districtName("Ahmedabad")
                .requestedAmount(BigDecimal.valueOf(1000))
                .approvedAmount(BigDecimal.valueOf(800))
                .disbursedAmount(BigDecimal.valueOf(300))
                .status("APPROVED")
                .build();
        AthleteDevelopmentRecord athlete = AthleteDevelopmentRecord.builder()
                .id(4L)
                .athleteName("State Player")
                .sport("CRICKET")
                .stateName("Gujarat")
                .districtName("Ahmedabad")
                .currentLevel("NATIONAL")
                .build();
        SportsVolunteerProfile volunteer = SportsVolunteerProfile.builder()
                .id(5L)
                .volunteerCode("TSOS-VOL-2026-333333")
                .fullName("Volunteer")
                .email("volunteer@example.com")
                .stateName("Gujarat")
                .districtName("Ahmedabad")
                .verificationStatus("VERIFIED")
                .build();
        FacilityInspectionReport inspection = FacilityInspectionReport.builder()
                .id(6L)
                .facilityId(1L)
                .inspectorName("Officer")
                .status("SCHEDULED")
                .build();

        when(facilityRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(facility));
        when(censusSubmissionRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(census));
        when(grantApplicationRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(grant));
        when(athleteDevelopmentRecordRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(athlete));
        when(volunteerProfileRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(volunteer));
        when(inspectionReportRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(inspection));

        Map<String, Object> dashboard = sportsOsService.dashboard();
        Map<?, ?> kpis = (Map<?, ?>) dashboard.get("kpis");

        assertEquals(1, kpis.get("facilities"));
        assertEquals(100, kpis.get("censusPlayers"));
        assertEquals(1L, kpis.get("verifiedVolunteers"));
        assertEquals(1L, kpis.get("openInspections"));
        assertEquals(0, BigDecimal.valueOf(60).setScale(2).compareTo((BigDecimal) kpis.get("averageUtilization")));
    }

    @Test
    void completeInspectionUpdatesFacilityMaintenanceAndSafetyHold() {
        SportsOsFacility facility = SportsOsFacility.builder()
                .id(1L)
                .facilityCode("TSOS-FAC-2026-444444")
                .name("District Stadium")
                .facilityType("STADIUM")
                .ownershipType("GOVERNMENT")
                .jurisdictionLevel("DISTRICT")
                .stateName("Gujarat")
                .districtName("Ahmedabad")
                .status("ACTIVE")
                .maintenanceStatus("OPERATIONAL")
                .build();
        FacilityInspectionReport inspection = FacilityInspectionReport.builder()
                .id(7L)
                .facilityId(1L)
                .inspectorName("Safety Officer")
                .status("SCHEDULED")
                .build();
        SportsOsRequests.InspectionRequest request = new SportsOsRequests.InspectionRequest();
        request.setScore(72);
        request.setSafetyStatus("SAFETY_HOLD");
        request.setMaintenanceStatus("UNDER_REPAIR");
        request.setRecommendation("Repair floodlights before public reopening");

        when(inspectionReportRepository.findById(7L)).thenReturn(Optional.of(inspection));
        when(facilityRepository.findById(1L)).thenReturn(Optional.of(facility));
        when(inspectionReportRepository.save(any(FacilityInspectionReport.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(facilityRepository.save(any(SportsOsFacility.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> response = sportsOsService.completeInspection(7L, request, "inspector@example.com");

        assertEquals("COMPLETED", response.get("status"));
        assertEquals(72, response.get("score"));
        assertEquals("MAINTENANCE", facility.getStatus());
        assertEquals("UNDER_REPAIR", facility.getMaintenanceStatus());
        verify(auditRepository).save(any(SportsOsAudit.class));
    }

    private SportsOsRequests.FacilityRequest facilityRequest() {
        SportsOsRequests.FacilityRequest request = new SportsOsRequests.FacilityRequest();
        request.setName("Riverfront Sports Complex");
        request.setFacilityType("SPORTS_COMPLEX");
        request.setOwnershipType("MUNICIPAL");
        request.setJurisdictionLevel("DISTRICT");
        request.setStateName("Gujarat");
        request.setDistrictName("Ahmedabad");
        request.setCityName("Ahmedabad");
        request.setLatitude(BigDecimal.valueOf(23.0225));
        request.setLongitude(BigDecimal.valueOf(72.5714));
        request.setCapacity(500);
        request.setActiveSlots(12);
        request.setPublicBookingEnabled(true);
        request.setOwnerDepartment("Municipal Sports Department");
        return request;
    }
}
