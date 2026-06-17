package com.example.Cricbook.service;

import com.example.Cricbook.dto.CareerPlatformRequests;
import com.example.Cricbook.entity.*;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CareerPlatformService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Set<String> OPPORTUNITY_TYPES = Set.of("RECRUITMENT", "TRIAL", "SCHOLARSHIP", "ACADEMY_ADMISSION");
    private static final Set<String> APPLICATION_STATUSES = Set.of("APPLIED", "SHORTLISTED", "SELECTED", "REJECTED", "WAITLISTED", "WITHDRAWN", "NEEDS_REVIEW");

    private final CareerProfileRepository profileRepository;
    private final CareerAchievementRepository achievementRepository;
    private final CareerTimelineEventRepository timelineRepository;
    private final CareerHighlightVideoRepository videoRepository;
    private final ScoutProfileRepository scoutRepository;
    private final ScoutActivityRepository scoutActivityRepository;
    private final CareerOpportunityRepository opportunityRepository;
    private final CareerApplicationRepository applicationRepository;
    private final TrialEvaluationRepository evaluationRepository;
    private final CareerRecommendationLetterRepository recommendationRepository;
    private final CareerAuditRepository auditRepository;
    private final TalentProfileRepository talentRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> dashboard() {
        List<CareerProfile> profiles = profileRepository.findAll();
        List<ScoutProfile> scouts = scoutRepository.findAll();
        List<CareerOpportunity> opportunities = opportunityRepository.findAll();
        List<CareerApplication> applications = applicationRepository.findAll();
        List<CareerAchievement> achievements = achievementRepository.findAll();
        List<CareerHighlightVideo> videos = videoRepository.findAll();
        List<TrialEvaluation> evaluations = evaluationRepository.findAll();
        List<CareerRecommendationLetter> recommendations = recommendationRepository.findAll();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("kpis", kpis(profiles, scouts, opportunities, applications, achievements, videos, evaluations));
        response.put("profileLeaderboard", profiles.stream().sorted(byRanking()).limit(12).map(this::mapProfile).toList());
        response.put("scouts", scouts.stream().sorted(byCreatedDesc()).limit(10).map(this::mapScout).toList());
        response.put("opportunities", opportunities.stream().sorted(byCreatedDesc()).limit(12).map(this::mapOpportunity).toList());
        response.put("applications", applications.stream().sorted(byApplicationDesc()).limit(12).map(this::mapApplication).toList());
        response.put("trials", opportunities.stream().filter(item -> "TRIAL".equalsIgnoreCase(item.getOpportunityType())).sorted(byCreatedDesc()).limit(10).map(this::mapOpportunity).toList());
        response.put("scholarships", opportunities.stream().filter(item -> "SCHOLARSHIP".equalsIgnoreCase(item.getOpportunityType())).sorted(byCreatedDesc()).limit(10).map(this::mapOpportunity).toList());
        response.put("pendingVerification", achievements.stream().filter(item -> "PENDING_VERIFICATION".equalsIgnoreCase(item.getStatus())).map(this::mapAchievement).toList());
        response.put("recommendations", recommendations.stream().sorted(byCreatedDesc()).limit(8).map(this::mapRecommendation).toList());
        response.put("careerAnalytics", careerAnalytics(profiles, applications));
        response.put("pipeline", pipeline(applications));
        response.put("ranking", rankings(profiles));
        response.put("governanceControls", governanceControls());
        response.put("priorityActions", priorityActions(profiles, scouts, opportunities, achievements, applications));
        response.put("audits", audits());
        response.put("generatedAt", LocalDateTime.now());
        return response;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> talentMarketplace(String city, String sport, String ageGroup, Integer minScore) {
        List<Map<String, Object>> careerProfiles = profileRepository.findByStatusOrderByRankingScoreDescCreatedAtDesc("ACTIVE").stream()
                .filter(profile -> Boolean.TRUE.equals(profile.getScoutOptIn()))
                .filter(profile -> matches(city, profile.getCity()))
                .filter(profile -> matches(sport, profile.getSport()))
                .filter(profile -> matches(ageGroup, profile.getAgeGroup()))
                .filter(profile -> minScore == null || defaultInt(profile.getRankingScore()) >= minScore)
                .map(this::mapProfile)
                .toList();

        List<Map<String, Object>> academyTalent = talentRepository.findByStatusOrderByTalentScoreDescCreatedAtDesc("DISCOVERABLE").stream()
                .filter(profile -> matches(city, profile.getCity()))
                .filter(profile -> matches(sport, profile.getSport()))
                .filter(profile -> matches(ageGroup, profile.getAgeGroup()))
                .filter(profile -> minScore == null || defaultInt(profile.getTalentScore()) >= minScore)
                .map(this::mapAcademyTalent)
                .toList();

        List<Map<String, Object>> combined = new ArrayList<>();
        combined.addAll(careerProfiles);
        combined.addAll(academyTalent);
        combined.sort((left, right) -> Integer.compare(Number.class.cast(right.getOrDefault("rankingScore", 0)).intValue(), Number.class.cast(left.getOrDefault("rankingScore", 0)).intValue()));
        return combined;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> opportunities(String type, String sport, String location) {
        return opportunityRepository.findByStatusOrderByCreatedAtDesc("PUBLISHED").stream()
                .filter(opportunity -> matches(type, opportunity.getOpportunityType()))
                .filter(opportunity -> matches(sport, opportunity.getSport()))
                .filter(opportunity -> matches(location, opportunity.getLocation()))
                .map(this::mapOpportunity)
                .toList();
    }

    @Transactional
    public Map<String, Object> publicResume(String shareCode) {
        CareerProfile profile = profileRepository.findByShareCode(shareCode)
                .orElseThrow(() -> ApiException.notFound("Sports resume was not found"));
        if (!"PUBLIC".equalsIgnoreCase(profile.getVisibility()) && !Boolean.TRUE.equals(profile.getScoutOptIn())) {
            throw ApiException.forbidden("This sports resume is private");
        }
        profile.setProfileViews(defaultLong(profile.getProfileViews()) + 1);
        profileRepository.save(profile);
        return resumePayload(profile);
    }

    @Transactional
    public Map<String, Object> createProfile(CareerPlatformRequests.ProfileRequest request, String actor) {
        String email = required(request == null ? null : request.getPlayerEmail(), "Player email is required").toLowerCase(Locale.ROOT);
        if (profileRepository.existsByPlayerEmailIgnoreCase(email)) {
            throw ApiException.conflict("Sports profile already exists for this player");
        }
        CareerProfile profile = CareerProfile.builder()
                .playerName(required(request.getPlayerName(), "Player name is required"))
                .playerEmail(email)
                .phone(blankToNull(request.getPhone()))
                .sport(defaultString(request.getSport(), "CRICKET"))
                .specialization(blankToNull(request.getSpecialization()))
                .playerRole(blankToNull(request.getPlayerRole()))
                .city(blankToNull(request.getCity()))
                .ageGroup(defaultString(request.getAgeGroup(), "OPEN"))
                .profilePhotoUrl(validateOptionalUrl(request.getProfilePhotoUrl(), "Profile photo"))
                .bio(blankToNull(request.getBio()))
                .statisticsSummary(blankToNull(request.getStatisticsSummary()))
                .certificatesSummary(blankToNull(request.getCertificatesSummary()))
                .visibility(normalize(request.getVisibility(), Boolean.TRUE.equals(request.getScoutOptIn()) ? "PUBLIC" : "PRIVATE"))
                .scoutOptIn(Boolean.TRUE.equals(request.getScoutOptIn()))
                .shareCode(shareCode())
                .build();
        applyProfileScores(profile);
        CareerProfile saved = profileRepository.save(profile);
        audit("CAREER_PROFILE", saved.getId(), "PROFILE_CREATED", actor, "Sports career profile created for " + saved.getPlayerEmail());
        return mapProfile(saved);
    }

    @Transactional
    public Map<String, Object> updateProfile(Long profileId, CareerPlatformRequests.ProfileRequest request, String actor) {
        CareerProfile profile = profile(profileId);
        profile.setPlayerName(defaultString(request == null ? null : request.getPlayerName(), profile.getPlayerName()));
        profile.setPhone(blankToNull(request == null ? null : request.getPhone()));
        profile.setSport(defaultString(request == null ? null : request.getSport(), profile.getSport()));
        profile.setSpecialization(blankToNull(request == null ? null : request.getSpecialization()));
        profile.setPlayerRole(blankToNull(request == null ? null : request.getPlayerRole()));
        profile.setCity(blankToNull(request == null ? null : request.getCity()));
        profile.setAgeGroup(defaultString(request == null ? null : request.getAgeGroup(), profile.getAgeGroup()));
        profile.setProfilePhotoUrl(validateOptionalUrl(request == null ? null : request.getProfilePhotoUrl(), "Profile photo"));
        profile.setBio(blankToNull(request == null ? null : request.getBio()));
        profile.setStatisticsSummary(blankToNull(request == null ? null : request.getStatisticsSummary()));
        profile.setCertificatesSummary(blankToNull(request == null ? null : request.getCertificatesSummary()));
        profile.setScoutOptIn(Boolean.TRUE.equals(request == null ? profile.getScoutOptIn() : request.getScoutOptIn()));
        profile.setVisibility(normalize(request == null ? null : request.getVisibility(), Boolean.TRUE.equals(profile.getScoutOptIn()) ? "PUBLIC" : "PRIVATE"));
        applyProfileScores(profile);
        CareerProfile saved = profileRepository.save(profile);
        audit("CAREER_PROFILE", saved.getId(), "PROFILE_UPDATED", actor, "Sports career profile updated");
        return mapProfile(saved);
    }

    @Transactional
    public Map<String, Object> generateResume(Long profileId, String actor) {
        CareerProfile profile = profile(profileId);
        profile.setResumeDownloads(defaultLong(profile.getResumeDownloads()) + 1);
        CareerProfile saved = profileRepository.save(profile);
        audit("CAREER_PROFILE", saved.getId(), "RESUME_GENERATED", actor, "Sports resume generated");
        return resumePayload(saved);
    }

    @Transactional
    public Map<String, Object> addAchievement(Long profileId, CareerPlatformRequests.AchievementRequest request, String actor) {
        CareerProfile profile = profile(profileId);
        CareerAchievement achievement = CareerAchievement.builder()
                .profile(profile)
                .title(required(request == null ? null : request.getTitle(), "Achievement title is required"))
                .achievementType(defaultString(request.getAchievementType(), "PERFORMANCE"))
                .issuer(blankToNull(request.getIssuer()))
                .eventDate(request.getEventDate())
                .evidenceUrl(validateOptionalUrl(request.getEvidenceUrl(), "Achievement evidence"))
                .notes(blankToNull(request.getNotes()))
                .build();
        CareerAchievement saved = achievementRepository.save(achievement);
        audit("CAREER_ACHIEVEMENT", saved.getId(), "ACHIEVEMENT_SUBMITTED", actor, "Achievement submitted for verification");
        recalculateAndSave(profile);
        return mapAchievement(saved);
    }

    @Transactional
    public Map<String, Object> verifyAchievement(Long achievementId, CareerPlatformRequests.StatusRequest request, String actor) {
        CareerAchievement achievement = achievementRepository.findById(achievementId)
                .orElseThrow(() -> ApiException.notFound("Achievement not found"));
        String status = normalize(request == null ? null : request.getStatus(), "VERIFIED");
        if (!Set.of("PENDING_VERIFICATION", "VERIFIED", "REJECTED").contains(status)) {
            throw ApiException.badRequest("Unsupported achievement status");
        }
        achievement.setStatus(status);
        achievement.setNotes(blankToNull(request == null ? null : request.getNotes()));
        achievement.setVerifiedBy(actor);
        achievement.setVerifiedAt(LocalDateTime.now());
        CareerAchievement saved = achievementRepository.save(achievement);
        audit("CAREER_ACHIEVEMENT", saved.getId(), "ACHIEVEMENT_" + status, actor, "Achievement verification updated");
        recalculateAndSave(achievement.getProfile());
        return mapAchievement(saved);
    }

    @Transactional
    public Map<String, Object> addTimelineEvent(Long profileId, CareerPlatformRequests.TimelineRequest request, String actor) {
        CareerProfile profile = profile(profileId);
        CareerTimelineEvent event = CareerTimelineEvent.builder()
                .profile(profile)
                .eventType(normalize(request == null ? null : request.getEventType(), "CAREER"))
                .title(required(request == null ? null : request.getTitle(), "Timeline title is required"))
                .eventDate(request.getEventDate())
                .summary(blankToNull(request.getSummary()))
                .build();
        CareerTimelineEvent saved = timelineRepository.save(event);
        audit("CAREER_TIMELINE", saved.getId(), "TIMELINE_EVENT_ADDED", actor, "Career timeline event added");
        return mapTimeline(saved);
    }

    @Transactional
    public Map<String, Object> addHighlightVideo(Long profileId, CareerPlatformRequests.VideoRequest request, String actor) {
        CareerProfile profile = profile(profileId);
        CareerHighlightVideo video = CareerHighlightVideo.builder()
                .profile(profile)
                .category(normalize(request == null ? null : request.getCategory(), "HIGHLIGHT"))
                .title(required(request == null ? null : request.getTitle(), "Video title is required"))
                .videoUrl(required(validateOptionalUrl(request.getVideoUrl(), "Highlight video"), "Video URL is required"))
                .build();
        CareerHighlightVideo saved = videoRepository.save(video);
        audit("CAREER_VIDEO", saved.getId(), "HIGHLIGHT_VIDEO_ADDED", actor, "Highlight video added");
        recalculateAndSave(profile);
        return mapVideo(saved);
    }

    @Transactional
    public Map<String, Object> registerScout(CareerPlatformRequests.ScoutRequest request, String actor) {
        String email = required(request == null ? null : request.getEmail(), "Scout email is required").toLowerCase(Locale.ROOT);
        if (scoutRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("Scout email already exists");
        }
        ScoutProfile scout = ScoutProfile.builder()
                .scoutName(required(request.getScoutName(), "Scout name is required"))
                .email(email)
                .phone(blankToNull(request.getPhone()))
                .organizationName(required(request.getOrganizationName(), "Organization name is required"))
                .clubType(defaultString(request.getClubType(), "CLUB"))
                .sportSpecialization(defaultString(request.getSportSpecialization(), "CRICKET"))
                .region(blankToNull(request.getRegion()))
                .credentialsUrl(validateOptionalUrl(request.getCredentialsUrl(), "Scout credentials"))
                .build();
        ScoutProfile saved = scoutRepository.save(scout);
        audit("SCOUT_PROFILE", saved.getId(), "SCOUT_REGISTERED", actor, "Scout profile registered");
        return mapScout(saved);
    }

    @Transactional
    public Map<String, Object> updateScoutStatus(Long scoutId, CareerPlatformRequests.StatusRequest request, String actor) {
        ScoutProfile scout = scoutRepository.findById(scoutId).orElseThrow(() -> ApiException.notFound("Scout not found"));
        String status = normalize(request == null ? null : request.getStatus(), "VERIFIED");
        if (!Set.of("PENDING_VERIFICATION", "VERIFIED", "REJECTED", "SUSPENDED").contains(status)) {
            throw ApiException.badRequest("Unsupported scout status");
        }
        scout.setStatus(status);
        scout.setVerifiedBadge("VERIFIED".equals(status));
        ScoutProfile saved = scoutRepository.save(scout);
        audit("SCOUT_PROFILE", saved.getId(), "SCOUT_STATUS_" + status, actor, defaultString(request == null ? null : request.getNotes(), "Scout verification updated"));
        return mapScout(saved);
    }

    @Transactional
    public Map<String, Object> recordScoutActivity(Long scoutId, CareerPlatformRequests.ScoutActivityRequest request, String actor) {
        ScoutProfile scout = scoutRepository.findById(scoutId).orElseThrow(() -> ApiException.notFound("Scout not found"));
        if (!"VERIFIED".equalsIgnoreCase(scout.getStatus())) {
            throw ApiException.forbidden("Scout must be verified before marketplace activity");
        }
        CareerProfile profile = profile(request == null ? null : request.getProfileId());
        ScoutActivity activity = ScoutActivity.builder()
                .scout(scout)
                .profile(profile)
                .actionType(normalize(request.getActionType(), "VIEW"))
                .notes(blankToNull(request.getNotes()))
                .build();
        ScoutActivity saved = scoutActivityRepository.save(activity);
        if (Set.of("VIEW", "BOOKMARK", "FOLLOW", "INVITE").contains(saved.getActionType())) {
            profile.setScoutViews(defaultLong(profile.getScoutViews()) + 1);
            profileRepository.save(profile);
        }
        audit("SCOUT_ACTIVITY", saved.getId(), "SCOUT_" + saved.getActionType(), actor, "Scout activity logged");
        return mapScoutActivity(saved);
    }

    @Transactional
    public Map<String, Object> createOpportunity(CareerPlatformRequests.OpportunityRequest request, String actor) {
        String type = normalize(request == null ? null : request.getOpportunityType(), "RECRUITMENT");
        if (!OPPORTUNITY_TYPES.contains(type)) {
            throw ApiException.badRequest("Unsupported career opportunity type");
        }
        Integer capacity = request.getCapacity();
        if (capacity != null && capacity < 0) {
            throw ApiException.badRequest("Opportunity capacity cannot be negative");
        }
        CareerOpportunity opportunity = CareerOpportunity.builder()
                .opportunityType(type)
                .title(required(request.getTitle(), "Opportunity title is required"))
                .organizationName(required(request.getOrganizationName(), "Organization name is required"))
                .sport(defaultString(request.getSport(), "CRICKET"))
                .category(blankToNull(request.getCategory()))
                .location(blankToNull(request.getLocation()))
                .description(blankToNull(request.getDescription()))
                .eligibility(blankToNull(request.getEligibility()))
                .capacity(capacity)
                .deadline(request.getDeadline())
                .startsAt(request.getStartsAt())
                .scholarshipAmount(request.getScholarshipAmount())
                .status(normalize(request.getStatus(), "PUBLISHED"))
                .createdBy(actor)
                .build();
        CareerOpportunity saved = opportunityRepository.save(opportunity);
        audit("CAREER_OPPORTUNITY", saved.getId(), "OPPORTUNITY_CREATED", actor, "Career opportunity created");
        return mapOpportunity(saved);
    }

    @Transactional
    public Map<String, Object> applyForOpportunity(Long opportunityId, CareerPlatformRequests.ApplicationRequest request, String actor) {
        CareerOpportunity opportunity = opportunityRepository.findById(opportunityId)
                .orElseThrow(() -> ApiException.notFound("Career opportunity not found"));
        if (!"PUBLISHED".equalsIgnoreCase(opportunity.getStatus())) {
            throw ApiException.conflict("Career opportunity is not accepting applications");
        }
        if (opportunity.getDeadline() != null && opportunity.getDeadline().isBefore(LocalDate.now())) {
            throw ApiException.conflict("Career opportunity deadline has passed");
        }
        CareerProfile profile = profile(request == null ? null : request.getProfileId());
        applicationRepository.findByOpportunityIdAndProfileId(opportunityId, profile.getId()).ifPresent(existing -> {
            throw ApiException.conflict("Player already applied for this opportunity");
        });
        boolean capacityFull = opportunity.getCapacity() != null && opportunity.getCapacity() > 0 && defaultInt(opportunity.getApplicantCount()) >= opportunity.getCapacity();
        CareerApplication application = CareerApplication.builder()
                .opportunity(opportunity)
                .profile(profile)
                .applicationType(opportunity.getOpportunityType())
                .statement(blankToNull(request.getStatement()))
                .documentsUrl(validateOptionalUrl(request.getDocumentsUrl(), "Application documents"))
                .status(capacityFull ? "WAITLISTED" : "APPLIED")
                .build();
        CareerApplication saved = applicationRepository.save(application);
        opportunity.setApplicantCount(defaultInt(opportunity.getApplicantCount()) + 1);
        opportunityRepository.save(opportunity);
        audit("CAREER_APPLICATION", saved.getId(), "APPLICATION_" + saved.getStatus(), actor, "Career opportunity application submitted");
        return mapApplication(saved);
    }

    @Transactional
    public Map<String, Object> updateApplicationStatus(Long applicationId, CareerPlatformRequests.StatusRequest request, String actor) {
        CareerApplication application = application(applicationId);
        String status = normalize(request == null ? null : request.getStatus(), "SHORTLISTED");
        if (!APPLICATION_STATUSES.contains(status)) {
            throw ApiException.badRequest("Unsupported application status");
        }
        application.setStatus(status);
        application.setDecisionNotes(blankToNull(request == null ? null : request.getNotes()));
        if (request != null && request.getScore() != null) {
            application.setEvaluatorScore(score(request.getScore()));
        }
        CareerApplication saved = applicationRepository.save(application);
        audit("CAREER_APPLICATION", saved.getId(), "APPLICATION_STATUS_" + status, actor, defaultString(saved.getDecisionNotes(), "Application status updated"));
        recalculateAndSave(saved.getProfile());
        return mapApplication(saved);
    }

    @Transactional
    public Map<String, Object> evaluateApplication(Long applicationId, CareerPlatformRequests.EvaluationRequest request, String actor) {
        CareerApplication application = application(applicationId);
        int technique = score(request == null ? null : request.getTechniqueScore());
        int fitness = score(request == null ? null : request.getFitnessScore());
        int discipline = score(request == null ? null : request.getDisciplineScore());
        int awareness = score(request == null ? null : request.getAwarenessScore());
        int overall = Math.round((technique + fitness + discipline + awareness) / 4.0f);
        String decision = normalize(request == null ? null : request.getDecision(), overall >= 80 ? "SHORTLISTED" : "NEEDS_REVIEW");
        TrialEvaluation evaluation = TrialEvaluation.builder()
                .application(application)
                .evaluatorName(defaultString(request == null ? null : request.getEvaluatorName(), actor))
                .techniqueScore(technique)
                .fitnessScore(fitness)
                .disciplineScore(discipline)
                .awarenessScore(awareness)
                .overallScore(overall)
                .decision(decision)
                .notes(blankToNull(request == null ? null : request.getNotes()))
                .build();
        TrialEvaluation saved = evaluationRepository.save(evaluation);
        application.setEvaluatorScore(overall);
        if (APPLICATION_STATUSES.contains(decision)) {
            application.setStatus(decision);
        }
        applicationRepository.save(application);
        audit("TRIAL_EVALUATION", saved.getId(), "EVALUATION_CREATED", actor, "Trial evaluation recorded");
        recalculateAndSave(application.getProfile());
        return mapEvaluation(saved);
    }

    @Transactional
    public Map<String, Object> createRecommendation(CareerPlatformRequests.RecommendationRequest request, String actor) {
        CareerProfile profile = profile(request == null ? null : request.getProfileId());
        CareerRecommendationLetter letter = CareerRecommendationLetter.builder()
                .profile(profile)
                .recommenderName(required(request.getRecommenderName(), "Recommender name is required"))
                .organization(blankToNull(request.getOrganization()))
                .content(required(request.getContent(), "Recommendation content is required"))
                .build();
        CareerRecommendationLetter saved = recommendationRepository.save(letter);
        audit("CAREER_RECOMMENDATION", saved.getId(), "RECOMMENDATION_CREATED", actor, "Recommendation letter submitted");
        return mapRecommendation(saved);
    }

    @Transactional
    public Map<String, Object> approveRecommendation(Long recommendationId, CareerPlatformRequests.StatusRequest request, String actor) {
        CareerRecommendationLetter letter = recommendationRepository.findById(recommendationId)
                .orElseThrow(() -> ApiException.notFound("Recommendation letter not found"));
        String status = normalize(request == null ? null : request.getStatus(), "APPROVED");
        if (!Set.of("PENDING_APPROVAL", "APPROVED", "REJECTED").contains(status)) {
            throw ApiException.badRequest("Unsupported recommendation status");
        }
        letter.setStatus(status);
        letter.setApprovedAt("APPROVED".equals(status) ? LocalDateTime.now() : null);
        CareerRecommendationLetter saved = recommendationRepository.save(letter);
        audit("CAREER_RECOMMENDATION", saved.getId(), "RECOMMENDATION_" + status, actor, defaultString(request == null ? null : request.getNotes(), "Recommendation status updated"));
        recalculateAndSave(saved.getProfile());
        return mapRecommendation(saved);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> profiles() {
        return profileRepository.findAllByOrderByCreatedAtDesc().stream().map(this::mapProfile).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> scouts() {
        return scoutRepository.findAllByOrderByCreatedAtDesc().stream().map(this::mapScout).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> adminOpportunities(String type) {
        return opportunityRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(opportunity -> matches(type, opportunity.getOpportunityType()))
                .map(this::mapOpportunity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> applications() {
        return applicationRepository.findAllByOrderByAppliedAtDesc().stream().map(this::mapApplication).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> evaluations() {
        return evaluationRepository.findAllByOrderByCreatedAtDesc().stream().map(this::mapEvaluation).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> audits() {
        return auditRepository.findTop100ByOrderByCreatedAtDesc().stream().map(this::mapAudit).toList();
    }

    private Map<String, Object> resumePayload(CareerProfile profile) {
        Map<String, Object> response = mapProfile(profile);
        response.put("shareLink", "/career/resumes/" + profile.getShareCode());
        response.put("pdfUrl", "/api/v1/career/resumes/" + profile.getShareCode() + "/pdf");
        response.put("qrCode", "TRUF-CAREER-QR:" + profile.getShareCode());
        response.put("statistics", defaultString(profile.getStatisticsSummary(), "No statistics summary added yet"));
        response.put("certificates", defaultString(profile.getCertificatesSummary(), "No certificate summary added yet"));
        response.put("achievements", achievementRepository.findByProfileIdOrderByCreatedAtDesc(profile.getId()).stream().map(this::mapAchievement).toList());
        response.put("timeline", timelineRepository.findByProfileIdOrderByEventDateDescCreatedAtDesc(profile.getId()).stream().map(this::mapTimeline).toList());
        response.put("highlightVideos", videoRepository.findByProfileIdOrderByCreatedAtDesc(profile.getId()).stream().map(this::mapVideo).toList());
        response.put("recommendations", recommendationRepository.findByProfileIdOrderByCreatedAtDesc(profile.getId()).stream().map(this::mapRecommendation).toList());
        return response;
    }

    private void recalculateAndSave(CareerProfile profile) {
        applyProfileScores(profile);
        profileRepository.save(profile);
    }

    private void applyProfileScores(CareerProfile profile) {
        profile.setProfileCompleteness(profileCompleteness(profile));
        long verifiedAchievements = profile.getId() == null ? 0 : achievementRepository.findByProfileIdOrderByCreatedAtDesc(profile.getId()).stream()
                .filter(item -> "VERIFIED".equalsIgnoreCase(item.getStatus()))
                .count();
        long videos = profile.getId() == null ? 0 : videoRepository.findByProfileIdOrderByCreatedAtDesc(profile.getId()).size();
        int selectedBoost = profile.getId() == null ? 0 : applicationRepository.findByProfileIdOrderByAppliedAtDesc(profile.getId()).stream()
                .filter(item -> Set.of("SHORTLISTED", "SELECTED").contains(item.getStatus()))
                .mapToInt(item -> "SELECTED".equals(item.getStatus()) ? 12 : 6)
                .sum();
        int score = Math.min(100, Math.round(profile.getProfileCompleteness() * 0.45f) + (int) Math.min(25, verifiedAchievements * 8) + (int) Math.min(15, videos * 5) + Math.min(15, selectedBoost));
        profile.setRankingScore(score);
        profile.setRankingBand(rankingBand(score));
    }

    private int profileCompleteness(CareerProfile profile) {
        int total = 12;
        int completed = 0;
        completed += hasValue(profile.getPlayerName()) ? 1 : 0;
        completed += hasValue(profile.getPlayerEmail()) ? 1 : 0;
        completed += hasValue(profile.getSport()) ? 1 : 0;
        completed += hasValue(profile.getSpecialization()) ? 1 : 0;
        completed += hasValue(profile.getPlayerRole()) ? 1 : 0;
        completed += hasValue(profile.getCity()) ? 1 : 0;
        completed += hasValue(profile.getAgeGroup()) ? 1 : 0;
        completed += hasValue(profile.getProfilePhotoUrl()) ? 1 : 0;
        completed += hasValue(profile.getBio()) ? 1 : 0;
        completed += hasValue(profile.getStatisticsSummary()) ? 1 : 0;
        completed += hasValue(profile.getCertificatesSummary()) ? 1 : 0;
        completed += Boolean.TRUE.equals(profile.getScoutOptIn()) ? 1 : 0;
        return Math.round((completed * 100.0f) / total);
    }

    private Map<String, Object> kpis(List<CareerProfile> profiles,
                                     List<ScoutProfile> scouts,
                                     List<CareerOpportunity> opportunities,
                                     List<CareerApplication> applications,
                                     List<CareerAchievement> achievements,
                                     List<CareerHighlightVideo> videos,
                                     List<TrialEvaluation> evaluations) {
        Map<String, Object> kpis = new LinkedHashMap<>();
        kpis.put("careerProfiles", profiles.size());
        kpis.put("discoverableProfiles", profiles.stream().filter(item -> Boolean.TRUE.equals(item.getScoutOptIn())).count());
        kpis.put("verifiedScouts", scouts.stream().filter(item -> "VERIFIED".equalsIgnoreCase(item.getStatus())).count());
        kpis.put("openOpportunities", opportunities.stream().filter(item -> "PUBLISHED".equalsIgnoreCase(item.getStatus())).count());
        kpis.put("trialOpportunities", opportunities.stream().filter(item -> "TRIAL".equalsIgnoreCase(item.getOpportunityType())).count());
        kpis.put("scholarships", opportunities.stream().filter(item -> "SCHOLARSHIP".equalsIgnoreCase(item.getOpportunityType())).count());
        kpis.put("applications", applications.size());
        kpis.put("selectedPlayers", applications.stream().filter(item -> "SELECTED".equalsIgnoreCase(item.getStatus())).count());
        kpis.put("pendingAchievements", achievements.stream().filter(item -> "PENDING_VERIFICATION".equalsIgnoreCase(item.getStatus())).count());
        kpis.put("highlightVideos", videos.size());
        kpis.put("averageEvaluation", Math.round(evaluations.stream().map(TrialEvaluation::getOverallScore).filter(Objects::nonNull).mapToInt(Integer::intValue).average().orElse(0)));
        return kpis;
    }

    private Map<String, Object> careerAnalytics(List<CareerProfile> profiles, List<CareerApplication> applications) {
        Map<String, Object> analytics = new LinkedHashMap<>();
        analytics.put("profileViews", profiles.stream().mapToLong(item -> defaultLong(item.getProfileViews())).sum());
        analytics.put("scoutViews", profiles.stream().mapToLong(item -> defaultLong(item.getScoutViews())).sum());
        analytics.put("resumeDownloads", profiles.stream().mapToLong(item -> defaultLong(item.getResumeDownloads())).sum());
        analytics.put("applicationSuccessRate", applications.isEmpty() ? 0 : Math.round(applications.stream().filter(item -> "SELECTED".equalsIgnoreCase(item.getStatus())).count() * 100.0 / applications.size()));
        analytics.put("averageRankingScore", Math.round(profiles.stream().map(CareerProfile::getRankingScore).filter(Objects::nonNull).mapToInt(Integer::intValue).average().orElse(0)));
        return analytics;
    }

    private Map<String, Object> pipeline(List<CareerApplication> applications) {
        Map<String, Object> pipeline = new LinkedHashMap<>();
        for (String status : APPLICATION_STATUSES) {
            pipeline.put(status.toLowerCase(Locale.ROOT), applications.stream().filter(item -> status.equalsIgnoreCase(item.getStatus())).count());
        }
        return pipeline;
    }

    private List<Map<String, Object>> rankings(List<CareerProfile> profiles) {
        List<CareerProfile> sorted = profiles.stream().sorted(byRanking()).toList();
        List<Map<String, Object>> response = new ArrayList<>();
        for (int index = 0; index < sorted.size(); index++) {
            Map<String, Object> row = mapProfile(sorted.get(index));
            row.put("rank", index + 1);
            response.add(row);
        }
        return response;
    }

    private List<Map<String, Object>> governanceControls() {
        return List.of(
                governance("PROFILE_PRIVACY", "Only public or scout opt-in profiles are discoverable"),
                governance("ACHIEVEMENT_VERIFICATION", "Achievement verification and rejection are audited"),
                governance("SCOUT_VERIFICATION", "Scout activity is blocked until credentials are verified"),
                governance("TRIAL_EVALUATION_AUDIT", "Trial evaluation scores and decisions are retained"),
                governance("SCHOLARSHIP_GOVERNANCE", "Scholarship applications follow explicit status transitions")
        );
    }

    private Map<String, Object> governance(String key, String description) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("key", key);
        item.put("description", description);
        item.put("status", "ENABLED");
        return item;
    }

    private List<Map<String, Object>> priorityActions(List<CareerProfile> profiles,
                                                      List<ScoutProfile> scouts,
                                                      List<CareerOpportunity> opportunities,
                                                      List<CareerAchievement> achievements,
                                                      List<CareerApplication> applications) {
        List<Map<String, Object>> actions = new ArrayList<>();
        if (profiles.stream().noneMatch(item -> Boolean.TRUE.equals(item.getScoutOptIn()))) {
            actions.add(action("Publish scout-ready profiles", "Talent marketplace needs public opt-in profiles.", "HIGH"));
        }
        if (scouts.stream().anyMatch(item -> "PENDING_VERIFICATION".equalsIgnoreCase(item.getStatus()))) {
            actions.add(action("Verify scouts", "Scout credentials must be verified before talent activity.", "HIGH"));
        }
        if (opportunities.stream().noneMatch(item -> "PUBLISHED".equalsIgnoreCase(item.getStatus()))) {
            actions.add(action("Publish opportunities", "Recruitment, trial, and scholarship marketplaces need open opportunities.", "MEDIUM"));
        }
        if (achievements.stream().anyMatch(item -> "PENDING_VERIFICATION".equalsIgnoreCase(item.getStatus()))) {
            actions.add(action("Review achievements", "Verified achievements improve ranking trust.", "HIGH"));
        }
        if (applications.stream().anyMatch(item -> "APPLIED".equalsIgnoreCase(item.getStatus()))) {
            actions.add(action("Shortlist applications", "Applied players are waiting for recruitment decisions.", "MEDIUM"));
        }
        return actions;
    }

    private Map<String, Object> action(String title, String description, String severity) {
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("title", title);
        action.put("description", description);
        action.put("severity", severity);
        return action;
    }

    private Map<String, Object> mapProfile(CareerProfile profile) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("source", "CAREER_PROFILE");
        item.put("id", profile.getId());
        item.put("playerName", profile.getPlayerName());
        item.put("playerEmail", profile.getPlayerEmail());
        item.put("phone", profile.getPhone());
        item.put("sport", profile.getSport());
        item.put("specialization", profile.getSpecialization());
        item.put("playerRole", profile.getPlayerRole());
        item.put("city", profile.getCity());
        item.put("ageGroup", profile.getAgeGroup());
        item.put("profilePhotoUrl", profile.getProfilePhotoUrl());
        item.put("bio", profile.getBio());
        item.put("statisticsSummary", profile.getStatisticsSummary());
        item.put("certificatesSummary", profile.getCertificatesSummary());
        item.put("profileCompleteness", profile.getProfileCompleteness());
        item.put("rankingScore", profile.getRankingScore());
        item.put("rankingBand", profile.getRankingBand());
        item.put("visibility", profile.getVisibility());
        item.put("status", profile.getStatus());
        item.put("shareCode", profile.getShareCode());
        item.put("scoutOptIn", profile.getScoutOptIn());
        item.put("profileViews", profile.getProfileViews());
        item.put("scoutViews", profile.getScoutViews());
        item.put("resumeDownloads", profile.getResumeDownloads());
        item.put("createdAt", profile.getCreatedAt());
        return item;
    }

    private Map<String, Object> mapAcademyTalent(TalentProfile profile) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("source", "ACADEMY_TALENT");
        item.put("id", profile.getId());
        item.put("playerName", profile.getPlayerName());
        item.put("playerEmail", profile.getPlayerEmail());
        item.put("sport", profile.getSport());
        item.put("city", profile.getCity());
        item.put("ageGroup", profile.getAgeGroup());
        item.put("rankingScore", profile.getTalentScore());
        item.put("rankingBand", rankingBand(defaultInt(profile.getTalentScore())));
        item.put("profileCompleteness", 80);
        item.put("scoutOptIn", true);
        item.put("status", profile.getStatus());
        item.put("highlights", profile.getHighlights());
        return item;
    }

    private Map<String, Object> mapAchievement(CareerAchievement achievement) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", achievement.getId());
        item.put("profileId", achievement.getProfile().getId());
        item.put("playerName", achievement.getProfile().getPlayerName());
        item.put("title", achievement.getTitle());
        item.put("achievementType", achievement.getAchievementType());
        item.put("issuer", achievement.getIssuer());
        item.put("eventDate", achievement.getEventDate());
        item.put("evidenceUrl", achievement.getEvidenceUrl());
        item.put("status", achievement.getStatus());
        item.put("verifiedBy", achievement.getVerifiedBy());
        item.put("verifiedAt", achievement.getVerifiedAt());
        item.put("notes", achievement.getNotes());
        item.put("createdAt", achievement.getCreatedAt());
        return item;
    }

    private Map<String, Object> mapTimeline(CareerTimelineEvent event) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", event.getId());
        item.put("profileId", event.getProfile().getId());
        item.put("eventType", event.getEventType());
        item.put("title", event.getTitle());
        item.put("eventDate", event.getEventDate());
        item.put("summary", event.getSummary());
        item.put("createdAt", event.getCreatedAt());
        return item;
    }

    private Map<String, Object> mapVideo(CareerHighlightVideo video) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", video.getId());
        item.put("profileId", video.getProfile().getId());
        item.put("playerName", video.getProfile().getPlayerName());
        item.put("category", video.getCategory());
        item.put("title", video.getTitle());
        item.put("videoUrl", video.getVideoUrl());
        item.put("status", video.getStatus());
        item.put("bookmarks", video.getBookmarks());
        item.put("createdAt", video.getCreatedAt());
        return item;
    }

    private Map<String, Object> mapScout(ScoutProfile scout) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", scout.getId());
        item.put("scoutName", scout.getScoutName());
        item.put("email", scout.getEmail());
        item.put("phone", scout.getPhone());
        item.put("organizationName", scout.getOrganizationName());
        item.put("clubType", scout.getClubType());
        item.put("sportSpecialization", scout.getSportSpecialization());
        item.put("region", scout.getRegion());
        item.put("credentialsUrl", scout.getCredentialsUrl());
        item.put("status", scout.getStatus());
        item.put("verifiedBadge", scout.getVerifiedBadge());
        item.put("createdAt", scout.getCreatedAt());
        return item;
    }

    private Map<String, Object> mapScoutActivity(ScoutActivity activity) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", activity.getId());
        item.put("scoutId", activity.getScout().getId());
        item.put("scoutName", activity.getScout().getScoutName());
        item.put("profileId", activity.getProfile().getId());
        item.put("playerName", activity.getProfile().getPlayerName());
        item.put("actionType", activity.getActionType());
        item.put("notes", activity.getNotes());
        item.put("createdAt", activity.getCreatedAt());
        return item;
    }

    private Map<String, Object> mapOpportunity(CareerOpportunity opportunity) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", opportunity.getId());
        item.put("opportunityType", opportunity.getOpportunityType());
        item.put("title", opportunity.getTitle());
        item.put("organizationName", opportunity.getOrganizationName());
        item.put("sport", opportunity.getSport());
        item.put("category", opportunity.getCategory());
        item.put("location", opportunity.getLocation());
        item.put("description", opportunity.getDescription());
        item.put("eligibility", opportunity.getEligibility());
        item.put("capacity", opportunity.getCapacity());
        item.put("applicantCount", opportunity.getApplicantCount());
        item.put("availableSeats", opportunity.getCapacity() == null ? null : Math.max(0, opportunity.getCapacity() - defaultInt(opportunity.getApplicantCount())));
        item.put("deadline", opportunity.getDeadline());
        item.put("startsAt", opportunity.getStartsAt());
        item.put("scholarshipAmount", opportunity.getScholarshipAmount());
        item.put("status", opportunity.getStatus());
        item.put("createdBy", opportunity.getCreatedBy());
        item.put("createdAt", opportunity.getCreatedAt());
        return item;
    }

    private Map<String, Object> mapApplication(CareerApplication application) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", application.getId());
        item.put("opportunityId", application.getOpportunity().getId());
        item.put("opportunityTitle", application.getOpportunity().getTitle());
        item.put("opportunityType", application.getOpportunity().getOpportunityType());
        item.put("profileId", application.getProfile().getId());
        item.put("playerName", application.getProfile().getPlayerName());
        item.put("playerEmail", application.getProfile().getPlayerEmail());
        item.put("applicationType", application.getApplicationType());
        item.put("statement", application.getStatement());
        item.put("documentsUrl", application.getDocumentsUrl());
        item.put("status", application.getStatus());
        item.put("evaluatorScore", application.getEvaluatorScore());
        item.put("decisionNotes", application.getDecisionNotes());
        item.put("appliedAt", application.getAppliedAt());
        return item;
    }

    private Map<String, Object> mapEvaluation(TrialEvaluation evaluation) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", evaluation.getId());
        item.put("applicationId", evaluation.getApplication().getId());
        item.put("playerName", evaluation.getApplication().getProfile().getPlayerName());
        item.put("opportunityTitle", evaluation.getApplication().getOpportunity().getTitle());
        item.put("evaluatorName", evaluation.getEvaluatorName());
        item.put("techniqueScore", evaluation.getTechniqueScore());
        item.put("fitnessScore", evaluation.getFitnessScore());
        item.put("disciplineScore", evaluation.getDisciplineScore());
        item.put("awarenessScore", evaluation.getAwarenessScore());
        item.put("overallScore", evaluation.getOverallScore());
        item.put("decision", evaluation.getDecision());
        item.put("notes", evaluation.getNotes());
        item.put("createdAt", evaluation.getCreatedAt());
        return item;
    }

    private Map<String, Object> mapRecommendation(CareerRecommendationLetter letter) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", letter.getId());
        item.put("profileId", letter.getProfile().getId());
        item.put("playerName", letter.getProfile().getPlayerName());
        item.put("recommenderName", letter.getRecommenderName());
        item.put("organization", letter.getOrganization());
        item.put("content", letter.getContent());
        item.put("status", letter.getStatus());
        item.put("approvedAt", letter.getApprovedAt());
        item.put("createdAt", letter.getCreatedAt());
        return item;
    }

    private Map<String, Object> mapAudit(CareerAudit audit) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", audit.getId());
        item.put("entityType", audit.getEntityType());
        item.put("entityId", audit.getEntityId());
        item.put("action", audit.getAction());
        item.put("actor", audit.getActor());
        item.put("summary", audit.getSummary());
        item.put("createdAt", audit.getCreatedAt());
        return item;
    }

    private void audit(String entityType, Long entityId, String action, String actor, String summary) {
        auditRepository.save(CareerAudit.builder()
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .actor(defaultString(actor, "system"))
                .summary(summary)
                .build());
    }

    private CareerProfile profile(Long profileId) {
        if (profileId == null) {
            throw ApiException.badRequest("Career profile is required");
        }
        return profileRepository.findById(profileId).orElseThrow(() -> ApiException.notFound("Career profile not found"));
    }

    private CareerApplication application(Long applicationId) {
        if (applicationId == null) {
            throw ApiException.badRequest("Career application is required");
        }
        return applicationRepository.findById(applicationId).orElseThrow(() -> ApiException.notFound("Career application not found"));
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

    private Comparator<CareerProfile> byRanking() {
        return Comparator.comparing(CareerProfile::getRankingScore, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(CareerProfile::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private Comparator<CareerApplication> byApplicationDesc() {
        return Comparator.comparing(CareerApplication::getAppliedAt, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private LocalDateTime createdAt(Object value) {
        if (value instanceof CareerProfile item) return item.getCreatedAt();
        if (value instanceof ScoutProfile item) return item.getCreatedAt();
        if (value instanceof CareerOpportunity item) return item.getCreatedAt();
        if (value instanceof CareerRecommendationLetter item) return item.getCreatedAt();
        return null;
    }

    private String required(String value, String message) {
        String cleaned = blankToNull(value);
        if (cleaned == null) {
            throw ApiException.badRequest(message);
        }
        return cleaned;
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim().replaceAll("[<>]", "");
        return cleaned.isBlank() ? null : cleaned;
    }

    private String defaultString(String value, String fallback) {
        String cleaned = blankToNull(value);
        return cleaned == null ? fallback : cleaned;
    }

    private String normalize(String value, String fallback) {
        return defaultString(value, fallback).toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_").replaceAll("^_+|_+$", "");
    }

    private boolean matches(String expected, String actual) {
        String cleaned = blankToNull(expected);
        return cleaned == null || (actual != null && actual.equalsIgnoreCase(cleaned));
    }

    private boolean hasValue(String value) {
        return blankToNull(value) != null;
    }

    private String validateOptionalUrl(String url, String label) {
        String cleaned = blankToNull(url);
        if (cleaned == null) {
            return null;
        }
        if (!cleaned.startsWith("https://") && !cleaned.startsWith("http://localhost")) {
            throw ApiException.badRequest(label + " must use HTTPS or localhost for development");
        }
        return cleaned;
    }

    private int score(Integer value) {
        if (value == null) {
            return 0;
        }
        return Math.max(0, Math.min(100, value));
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private long defaultLong(Long value) {
        return value == null ? 0L : value;
    }

    private String rankingBand(Integer score) {
        int value = defaultInt(score);
        if (value >= 85) return "ELITE";
        if (value >= 70) return "HIGH_POTENTIAL";
        if (value >= 50) return "DEVELOPING";
        return "FOUNDATION";
    }

    private String shareCode() {
        byte[] buffer = new byte[8];
        RANDOM.nextBytes(buffer);
        return "TRUF-CAREER-" + Base64.getUrlEncoder().withoutPadding().encodeToString(buffer).toUpperCase(Locale.ROOT);
    }
}
