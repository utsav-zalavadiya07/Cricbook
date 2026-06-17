package com.example.Cricbook.service;

import com.example.Cricbook.dto.CareerPlatformRequests;
import com.example.Cricbook.entity.*;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CareerPlatformServiceTest {

    @Mock
    private CareerProfileRepository profileRepository;
    @Mock
    private CareerAchievementRepository achievementRepository;
    @Mock
    private CareerTimelineEventRepository timelineRepository;
    @Mock
    private CareerHighlightVideoRepository videoRepository;
    @Mock
    private ScoutProfileRepository scoutRepository;
    @Mock
    private ScoutActivityRepository scoutActivityRepository;
    @Mock
    private CareerOpportunityRepository opportunityRepository;
    @Mock
    private CareerApplicationRepository applicationRepository;
    @Mock
    private TrialEvaluationRepository evaluationRepository;
    @Mock
    private CareerRecommendationLetterRepository recommendationRepository;
    @Mock
    private CareerAuditRepository auditRepository;
    @Mock
    private TalentProfileRepository talentRepository;

    @InjectMocks
    private CareerPlatformService careerPlatformService;

    @Test
    void createProfileCalculatesCompletenessAndRejectsDuplicateEmail() {
        CareerPlatformRequests.ProfileRequest request = new CareerPlatformRequests.ProfileRequest();
        request.setPlayerName("Vir Player");
        request.setPlayerEmail("vir@example.com");
        request.setSport("CRICKET");
        request.setSpecialization("Batting");
        request.setPlayerRole("Opener");
        request.setCity("Ahmedabad");
        request.setAgeGroup("U19");
        request.setBio("Aggressive opener");
        request.setStatisticsSummary("35 matches, 1200 runs");
        request.setCertificatesSummary("Academy certified");
        request.setScoutOptIn(true);
        request.setVisibility("PUBLIC");

        when(profileRepository.existsByPlayerEmailIgnoreCase("vir@example.com")).thenReturn(false);
        when(profileRepository.save(any(CareerProfile.class))).thenAnswer(invocation -> {
            CareerProfile profile = invocation.getArgument(0);
            profile.setId(7L);
            return profile;
        });
        when(auditRepository.save(any(CareerAudit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> response = careerPlatformService.createProfile(request, "player@example.com");

        assertEquals("Vir Player", response.get("playerName"));
        assertEquals("FOUNDATION", response.get("rankingBand"));
        assertTrue((Integer) response.get("profileCompleteness") >= 80);
        assertTrue(String.valueOf(response.get("shareCode")).startsWith("TRUF-CAREER-"));
        verify(auditRepository).save(any(CareerAudit.class));

        when(profileRepository.existsByPlayerEmailIgnoreCase("vir@example.com")).thenReturn(true);
        assertThrows(ApiException.class, () -> careerPlatformService.createProfile(request, "player@example.com"));
    }

    @Test
    void applyForOpportunityWaitlistsWhenCapacityIsFull() {
        CareerProfile profile = CareerProfile.builder()
                .id(10L)
                .playerName("Waitlisted Player")
                .playerEmail("wait@example.com")
                .sport("CRICKET")
                .shareCode("TRUF-CAREER-X")
                .scoutOptIn(true)
                .build();
        CareerOpportunity opportunity = CareerOpportunity.builder()
                .id(20L)
                .opportunityType("TRIAL")
                .title("U19 Trial")
                .organizationName("State Club")
                .sport("CRICKET")
                .capacity(1)
                .applicantCount(1)
                .deadline(LocalDate.now().plusDays(3))
                .status("PUBLISHED")
                .build();

        when(opportunityRepository.findById(20L)).thenReturn(Optional.of(opportunity));
        when(profileRepository.findById(10L)).thenReturn(Optional.of(profile));
        when(applicationRepository.findByOpportunityIdAndProfileId(20L, 10L)).thenReturn(Optional.empty());
        when(applicationRepository.save(any(CareerApplication.class))).thenAnswer(invocation -> {
            CareerApplication application = invocation.getArgument(0);
            application.setId(30L);
            return application;
        });
        when(opportunityRepository.save(any(CareerOpportunity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditRepository.save(any(CareerAudit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CareerPlatformRequests.ApplicationRequest request = new CareerPlatformRequests.ApplicationRequest();
        request.setProfileId(10L);
        request.setStatement("Ready for trials");

        Map<String, Object> response = careerPlatformService.applyForOpportunity(20L, request, "wait@example.com");

        assertEquals("WAITLISTED", response.get("status"));
        assertEquals(2, opportunity.getApplicantCount());
        verify(opportunityRepository).save(opportunity);
        verify(auditRepository).save(any(CareerAudit.class));
    }

    @Test
    void evaluateApplicationStoresAverageAndRecalculatesProfileScore() {
        CareerProfile profile = CareerProfile.builder()
                .id(10L)
                .playerName("Trial Player")
                .playerEmail("trial@example.com")
                .sport("CRICKET")
                .shareCode("TRUF-CAREER-Y")
                .profileCompleteness(75)
                .rankingScore(60)
                .scoutOptIn(true)
                .build();
        CareerOpportunity opportunity = CareerOpportunity.builder()
                .id(20L)
                .opportunityType("TRIAL")
                .title("Elite Trial")
                .organizationName("Pro Club")
                .sport("CRICKET")
                .status("PUBLISHED")
                .build();
        CareerApplication application = CareerApplication.builder()
                .id(30L)
                .profile(profile)
                .opportunity(opportunity)
                .applicationType("TRIAL")
                .status("APPLIED")
                .build();

        when(applicationRepository.findById(30L)).thenReturn(Optional.of(application));
        when(evaluationRepository.save(any(TrialEvaluation.class))).thenAnswer(invocation -> {
            TrialEvaluation evaluation = invocation.getArgument(0);
            evaluation.setId(40L);
            return evaluation;
        });
        when(applicationRepository.save(any(CareerApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(applicationRepository.findByProfileIdOrderByAppliedAtDesc(10L)).thenReturn(List.of(application));
        when(achievementRepository.findByProfileIdOrderByCreatedAtDesc(10L)).thenReturn(List.of());
        when(videoRepository.findByProfileIdOrderByCreatedAtDesc(10L)).thenReturn(List.of());
        when(profileRepository.save(any(CareerProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditRepository.save(any(CareerAudit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CareerPlatformRequests.EvaluationRequest request = new CareerPlatformRequests.EvaluationRequest();
        request.setTechniqueScore(88);
        request.setFitnessScore(80);
        request.setDisciplineScore(84);
        request.setAwarenessScore(92);

        Map<String, Object> response = careerPlatformService.evaluateApplication(30L, request, "scout@example.com");

        assertEquals(86, response.get("overallScore"));
        assertEquals("SHORTLISTED", application.getStatus());
        assertTrue(profile.getRankingScore() > 0);
        verify(evaluationRepository).save(any(TrialEvaluation.class));
        verify(profileRepository).save(profile);
    }
}
