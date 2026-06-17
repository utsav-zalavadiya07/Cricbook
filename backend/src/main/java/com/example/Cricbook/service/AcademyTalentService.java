package com.example.Cricbook.service;

import com.example.Cricbook.dto.AcademyTalentRequests;
import com.example.Cricbook.entity.*;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AcademyTalentService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AcademyProfileRepository academyRepository;
    private final CoachProfileRepository coachRepository;
    private final TrainingProgramRepository programRepository;
    private final TrainingBatchRepository batchRepository;
    private final AcademyEnrollmentRepository enrollmentRepository;
    private final TrainingAttendanceRepository attendanceRepository;
    private final PlayerAssessmentRepository assessmentRepository;
    private final TrainingVideoAnalysisRepository videoRepository;
    private final TalentProfileRepository talentRepository;
    private final AcademyCertificateRepository certificateRepository;
    private final AcademyTalentAuditRepository auditRepository;
    private final AuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> dashboard() {
        List<AcademyProfile> academies = academyRepository.findAll();
        List<CoachProfile> coaches = coachRepository.findAll();
        List<TrainingProgram> programs = programRepository.findAll();
        List<TrainingBatch> batches = batchRepository.findAll();
        List<AcademyEnrollment> enrollments = enrollmentRepository.findAll();
        List<TrainingAttendance> attendance = attendanceRepository.findAll();
        List<PlayerAssessment> assessments = assessmentRepository.findAll();
        List<TalentProfile> talent = talentRepository.findAll();
        List<AcademyCertificate> certificates = certificateRepository.findAll();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("kpis", kpis(academies, coaches, programs, batches, enrollments, attendance, assessments, talent, certificates));
        response.put("academies", academies.stream().sorted(byCreatedDesc()).limit(8).map(this::mapAcademy).toList());
        response.put("coaches", coaches.stream().sorted(byCreatedDesc()).limit(8).map(this::mapCoach).toList());
        response.put("programs", programs.stream().sorted(byCreatedDesc()).limit(8).map(this::mapProgram).toList());
        response.put("batches", batches.stream().sorted(byCreatedDesc()).limit(8).map(this::mapBatch).toList());
        response.put("enrollments", enrollments.stream().sorted(byCreatedDesc()).limit(8).map(this::mapEnrollment).toList());
        response.put("talentLeaderboard", talent.stream().sorted(Comparator.comparing(TalentProfile::getTalentScore, Comparator.nullsLast(Comparator.reverseOrder()))).limit(8).map(this::mapTalent).toList());
        response.put("academyCrm", crm(enrollments, attendance));
        response.put("revenue", revenue(enrollments));
        response.put("skillProgression", skillProgression(assessments));
        response.put("certification", certification(certificates));
        response.put("governanceControls", governanceControls());
        response.put("priorityActions", priorityActions(academies, coaches, programs, enrollments, assessments, talent));
        response.put("generatedAt", LocalDateTime.now());
        return response;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> academies() {
        return academyRepository.findAllByOrderByCreatedAtDesc().stream().map(this::mapAcademy).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> academyMarketplace(String city, String sport) {
        return academyRepository.findByStatusOrderByCreatedAtDesc("APPROVED").stream()
                .filter(academy -> matches(city, academy.getCity()))
                .filter(academy -> sport == null || containsIgnoreCase(academy.getSportsOffered(), sport))
                .map(this::mapAcademy)
                .toList();
    }

    @Transactional
    public Map<String, Object> registerAcademy(AcademyTalentRequests.AcademyRequest request, String actor) {
        String name = required(request == null ? null : request.getName(), "Academy name is required");
        if (academyRepository.existsByNameIgnoreCase(name)) {
            throw ApiException.conflict("Academy name already exists");
        }
        AcademyProfile academy = AcademyProfile.builder()
                .name(name)
                .description(blankToNull(request.getDescription()))
                .sportsOffered(required(request.getSportsOffered(), "Sports selection is required"))
                .city(blankToNull(request.getCity()))
                .address(blankToNull(request.getAddress()))
                .website(validateOptionalUrl(request.getWebsite(), "Academy website"))
                .email(blankToNull(request.getEmail()))
                .phone(blankToNull(request.getPhone()))
                .logoUrl(blankToNull(request.getLogoUrl()))
                .documentUrl(blankToNull(request.getDocumentUrl()))
                .status("PENDING_APPROVAL")
                .accreditationLevel("UNVERIFIED")
                .build();
        AcademyProfile saved = academyRepository.save(academy);
        audit(saved.getId(), null, null, "ACADEMY_REGISTERED", actor, "Academy registration submitted: " + saved.getName());
        return mapAcademy(saved);
    }

    @Transactional
    public Map<String, Object> updateAcademyStatus(Long academyId, AcademyTalentRequests.StatusRequest request, String actor) {
        AcademyProfile academy = academyRepository.findById(academyId).orElseThrow(() -> ApiException.notFound("Academy not found"));
        String status = normalize(request == null ? null : request.getStatus(), "PENDING_APPROVAL");
        if (!Set.of("PENDING_APPROVAL", "APPROVED", "REJECTED", "MORE_DOCUMENTS", "SUSPENDED").contains(status)) {
            throw ApiException.badRequest("Unsupported academy status");
        }
        academy.setStatus(status);
        academy.setReviewNotes(blankToNull(request == null ? null : request.getNotes()));
        academy.setAccreditationLevel("APPROVED".equals(status) ? "ACCREDITED" : academy.getAccreditationLevel());
        AcademyProfile saved = academyRepository.save(academy);
        audit(saved.getId(), null, null, "ACADEMY_STATUS_" + status, actor, defaultString(saved.getReviewNotes(), "Academy status updated"));
        return mapAcademy(saved);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> coaches() {
        return coachRepository.findAllByOrderByCreatedAtDesc().stream().map(this::mapCoach).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> coachMarketplace(String sport, String city, Double minRating) {
        return coachRepository.findByStatusOrderByCreatedAtDesc("VERIFIED").stream()
                .filter(coach -> matches(sport, coach.getSport()))
                .filter(coach -> matches(city, coach.getCity()))
                .filter(coach -> minRating == null || defaultNumber(coach.getRating()) >= minRating)
                .map(this::mapCoach)
                .toList();
    }

    @Transactional
    public Map<String, Object> registerCoach(AcademyTalentRequests.CoachRequest request, String actor) {
        String email = required(request == null ? null : request.getEmail(), "Coach email is required").toLowerCase(Locale.ROOT);
        if (coachRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("Coach email already exists");
        }
        Integer experience = request.getExperienceYears() == null ? 0 : request.getExperienceYears();
        if (experience < 0) {
            throw ApiException.badRequest("Coach experience cannot be negative");
        }
        if (request.getAcademyId() != null) {
            academyRepository.findById(request.getAcademyId()).orElseThrow(() -> ApiException.notFound("Academy not found"));
        }
        CoachProfile coach = CoachProfile.builder()
                .academyId(request.getAcademyId())
                .name(required(request.getName(), "Coach name is required"))
                .email(email)
                .phone(blankToNull(request.getPhone()))
                .sport(defaultString(request.getSport(), "CRICKET"))
                .city(blankToNull(request.getCity()))
                .experienceYears(experience)
                .pricePerSession(nonNegative(request.getPricePerSession()))
                .certifications(blankToNull(request.getCertifications()))
                .achievements(blankToNull(request.getAchievements()))
                .profilePhotoUrl(blankToNull(request.getProfilePhotoUrl()))
                .status("PENDING_VERIFICATION")
                .rating(0.0)
                .studentsCoached(0)
                .build();
        CoachProfile saved = coachRepository.save(coach);
        audit(saved.getAcademyId(), saved.getId(), null, "COACH_REGISTERED", actor, "Coach profile created: " + saved.getEmail());
        return mapCoach(saved);
    }

    @Transactional
    public Map<String, Object> updateCoachStatus(Long coachId, AcademyTalentRequests.StatusRequest request, String actor) {
        CoachProfile coach = coachRepository.findById(coachId).orElseThrow(() -> ApiException.notFound("Coach not found"));
        String status = normalize(request == null ? null : request.getStatus(), "PENDING_VERIFICATION");
        if (!Set.of("PENDING_VERIFICATION", "VERIFIED", "REJECTED", "SUSPENDED").contains(status)) {
            throw ApiException.badRequest("Unsupported coach status");
        }
        coach.setStatus(status);
        coach.setReviewNotes(blankToNull(request == null ? null : request.getNotes()));
        CoachProfile saved = coachRepository.save(coach);
        audit(saved.getAcademyId(), saved.getId(), null, "COACH_STATUS_" + status, actor, defaultString(saved.getReviewNotes(), "Coach status updated"));
        return mapCoach(saved);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> programs(String sport, String city) {
        Map<Long, AcademyProfile> academies = academyIndex();
        return programRepository.findByStatusOrderByCreatedAtDesc("PUBLISHED").stream()
                .filter(program -> matches(sport, program.getSport()))
                .filter(program -> city == null || matches(city, academies.get(program.getAcademyId()) == null ? null : academies.get(program.getAcademyId()).getCity()))
                .map(program -> mapProgram(program, academies))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> adminPrograms() {
        return programRepository.findAllByOrderByCreatedAtDesc().stream().map(this::mapProgram).toList();
    }

    @Transactional
    public Map<String, Object> createProgram(AcademyTalentRequests.ProgramRequest request, String actor) {
        Long academyId = request == null ? null : request.getAcademyId();
        Long coachId = request == null ? null : request.getCoachId();
        if (academyId == null || coachId == null) {
            throw ApiException.badRequest("Academy and coach are required");
        }
        AcademyProfile academy = academyRepository.findById(academyId).orElseThrow(() -> ApiException.notFound("Academy not found"));
        CoachProfile coach = coachRepository.findById(coachId).orElseThrow(() -> ApiException.notFound("Coach not found"));
        Integer capacity = positive(request.getCapacity(), "Program capacity must be greater than zero");
        Integer duration = positive(request.getDurationWeeks(), "Program duration must be greater than zero");
        TrainingProgram program = TrainingProgram.builder()
                .academyId(academy.getId())
                .coachId(coach.getId())
                .title(required(request.getTitle(), "Program title is required"))
                .sport(defaultString(request.getSport(), coach.getSport()))
                .level(normalize(request.getLevel(), "BEGINNER"))
                .description(blankToNull(request.getDescription()))
                .durationWeeks(duration)
                .capacity(capacity)
                .price(nonNegative(request.getPrice()))
                .status(normalize(request.getStatus(), "DRAFT"))
                .build();
        TrainingProgram saved = programRepository.save(program);
        audit(academy.getId(), coach.getId(), null, "PROGRAM_CREATED", actor, "Training program created: " + saved.getTitle());
        return mapProgram(saved);
    }

    @Transactional
    public Map<String, Object> updateProgramStatus(Long programId, AcademyTalentRequests.StatusRequest request, String actor) {
        TrainingProgram program = programRepository.findById(programId).orElseThrow(() -> ApiException.notFound("Training program not found"));
        String status = normalize(request == null ? null : request.getStatus(), "DRAFT");
        if (!Set.of("DRAFT", "PUBLISHED", "ARCHIVED", "SUSPENDED").contains(status)) {
            throw ApiException.badRequest("Unsupported program status");
        }
        program.setStatus(status);
        TrainingProgram saved = programRepository.save(program);
        audit(saved.getAcademyId(), saved.getCoachId(), null, "PROGRAM_STATUS_" + status, actor, defaultString(request == null ? null : request.getNotes(), "Program status updated"));
        return mapProgram(saved);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> batches() {
        return batchRepository.findAllByOrderByCreatedAtDesc().stream().map(this::mapBatch).toList();
    }

    @Transactional
    public Map<String, Object> createBatch(AcademyTalentRequests.BatchRequest request, String actor) {
        Long programId = request == null ? null : request.getProgramId();
        TrainingProgram program = programRepository.findById(programId).orElseThrow(() -> ApiException.notFound("Training program not found"));
        TrainingBatch batch = TrainingBatch.builder()
                .programId(program.getId())
                .name(required(request.getName(), "Batch name is required"))
                .scheduleLabel(defaultString(request.getScheduleLabel(), "Flexible schedule"))
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .capacity(positive(request.getCapacity() == null ? program.getCapacity() : request.getCapacity(), "Batch capacity must be greater than zero"))
                .enrolledCount(0)
                .status("ACTIVE")
                .build();
        TrainingBatch saved = batchRepository.save(batch);
        audit(program.getAcademyId(), program.getCoachId(), null, "BATCH_CREATED", actor, "Training batch created: " + saved.getName());
        return mapBatch(saved);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> enrollments() {
        return enrollmentRepository.findAllByOrderByCreatedAtDesc().stream().map(this::mapEnrollment).toList();
    }

    @Transactional
    public Map<String, Object> enrollStudent(AcademyTalentRequests.EnrollmentRequest request, String actor) {
        Long batchId = request == null ? null : request.getBatchId();
        TrainingBatch batch = batchRepository.findById(batchId).orElseThrow(() -> ApiException.notFound("Training batch not found"));
        TrainingProgram program = programRepository.findById(batch.getProgramId()).orElseThrow(() -> ApiException.notFound("Training program not found"));
        String email = required(request.getStudentEmail(), "Student email is required").toLowerCase(Locale.ROOT);
        enrollmentRepository.findByBatchIdAndStudentEmailIgnoreCase(batch.getId(), email)
                .filter(existing -> Set.of("CONFIRMED", "WAITLISTED").contains(existing.getStatus()))
                .ifPresent(existing -> {
                    throw ApiException.conflict("Student already enrolled in this batch");
                });
        int currentCount = batch.getEnrolledCount() == null ? 0 : batch.getEnrolledCount();
        int capacity = batch.getCapacity() == null ? 0 : batch.getCapacity();
        String status = currentCount >= capacity ? "WAITLISTED" : "CONFIRMED";
        AcademyEnrollment enrollment = AcademyEnrollment.builder()
                .academyId(program.getAcademyId())
                .programId(program.getId())
                .batchId(batch.getId())
                .studentName(required(request.getStudentName(), "Student name is required"))
                .studentEmail(email)
                .studentAge(request.getStudentAge())
                .status(status)
                .amountPaid(nonNegative(request.getAmountPaid()))
                .notes(blankToNull(request.getNotes()))
                .build();
        AcademyEnrollment saved = enrollmentRepository.save(enrollment);
        if ("CONFIRMED".equals(status)) {
            batch.setEnrolledCount(currentCount + 1);
            batchRepository.save(batch);
            coachRepository.findById(program.getCoachId()).ifPresent(coach -> {
                coach.setStudentsCoached((coach.getStudentsCoached() == null ? 0 : coach.getStudentsCoached()) + 1);
                coachRepository.save(coach);
            });
        }
        audit(program.getAcademyId(), program.getCoachId(), saved.getId(), "STUDENT_ENROLLED_" + status, actor, "Student enrollment processed: " + saved.getStudentEmail());
        return mapEnrollment(saved);
    }

    @Transactional
    public Map<String, Object> cancelEnrollment(Long enrollmentId, String actor) {
        AcademyEnrollment enrollment = enrollmentRepository.findById(enrollmentId).orElseThrow(() -> ApiException.notFound("Enrollment not found"));
        if ("CANCELLED".equalsIgnoreCase(enrollment.getStatus())) {
            throw ApiException.conflict("Enrollment is already cancelled");
        }
        enrollment.setStatus("CANCELLED");
        AcademyEnrollment saved = enrollmentRepository.save(enrollment);
        batchRepository.findById(saved.getBatchId()).ifPresent(batch -> {
            int count = Math.max(0, (batch.getEnrolledCount() == null ? 0 : batch.getEnrolledCount()) - 1);
            batch.setEnrolledCount(count);
            promoteWaitlist(batch);
            batchRepository.save(batch);
        });
        audit(saved.getAcademyId(), null, saved.getId(), "ENROLLMENT_CANCELLED", actor, "Enrollment cancelled and seat released");
        return mapEnrollment(saved);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> attendance() {
        return attendanceRepository.findAllByOrderByCreatedAtDesc().stream().map(this::mapAttendance).toList();
    }

    @Transactional
    public Map<String, Object> markAttendance(AcademyTalentRequests.AttendanceRequest request, String actor) {
        Long enrollmentId = request == null ? null : request.getEnrollmentId();
        AcademyEnrollment enrollment = enrollmentRepository.findById(enrollmentId).orElseThrow(() -> ApiException.notFound("Enrollment not found"));
        String status = normalize(request.getStatus(), "PRESENT");
        if (!Set.of("PRESENT", "ABSENT", "LATE").contains(status)) {
            throw ApiException.badRequest("Unsupported attendance status");
        }
        TrainingAttendance attendance = TrainingAttendance.builder()
                .enrollmentId(enrollment.getId())
                .batchId(enrollment.getBatchId())
                .sessionDate(request.getSessionDate() == null ? LocalDate.now() : request.getSessionDate())
                .status(status)
                .markedBy(defaultString(actor, "system"))
                .notes(blankToNull(request.getNotes()))
                .build();
        TrainingAttendance saved = attendanceRepository.save(attendance);
        audit(enrollment.getAcademyId(), null, enrollment.getId(), "ATTENDANCE_" + status, actor, "Attendance marked for " + enrollment.getStudentEmail());
        return mapAttendance(saved);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> assessments() {
        return assessmentRepository.findAllByOrderByCreatedAtDesc().stream().map(this::mapAssessment).toList();
    }

    @Transactional
    public Map<String, Object> createAssessment(AcademyTalentRequests.AssessmentRequest request, String actor) {
        Long enrollmentId = request == null ? null : request.getEnrollmentId();
        AcademyEnrollment enrollment = enrollmentRepository.findById(enrollmentId).orElseThrow(() -> ApiException.notFound("Enrollment not found"));
        TrainingProgram program = programRepository.findById(enrollment.getProgramId()).orElseThrow(() -> ApiException.notFound("Training program not found"));
        int batting = score(request.getBattingScore());
        int bowling = score(request.getBowlingScore());
        int fielding = score(request.getFieldingScore());
        int fitness = score(request.getFitnessScore());
        int discipline = score(request.getDisciplineScore());
        int overall = Math.round((batting + bowling + fielding + fitness + discipline) / 5.0f);
        PlayerAssessment assessment = PlayerAssessment.builder()
                .enrollmentId(enrollment.getId())
                .coachId(request.getCoachId() == null ? program.getCoachId() : request.getCoachId())
                .playerName(enrollment.getStudentName())
                .playerEmail(enrollment.getStudentEmail())
                .sport(program.getSport())
                .battingScore(batting)
                .bowlingScore(bowling)
                .fieldingScore(fielding)
                .fitnessScore(fitness)
                .disciplineScore(discipline)
                .overallScore(overall)
                .recommendations(blankToNull(request.getRecommendations()))
                .build();
        PlayerAssessment saved = assessmentRepository.save(assessment);
        audit(enrollment.getAcademyId(), saved.getCoachId(), enrollment.getId(), "ASSESSMENT_CREATED", actor, "Assessment saved with score " + saved.getOverallScore());
        return mapAssessment(saved);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> videos() {
        return videoRepository.findAllByOrderByCreatedAtDesc().stream().map(this::mapVideo).toList();
    }

    @Transactional
    public Map<String, Object> addVideoAnalysis(AcademyTalentRequests.VideoRequest request, String actor) {
        AcademyEnrollment enrollment = enrollmentRepository.findById(request == null ? null : request.getEnrollmentId()).orElseThrow(() -> ApiException.notFound("Enrollment not found"));
        TrainingVideoAnalysis video = TrainingVideoAnalysis.builder()
                .enrollmentId(enrollment.getId())
                .playerEmail(enrollment.getStudentEmail())
                .category(normalize(request.getCategory(), "TRAINING"))
                .videoUrl(required(request.getVideoUrl(), "Video URL is required"))
                .coachComments(blankToNull(request.getCoachComments()))
                .improvementAreas(blankToNull(request.getImprovementAreas()))
                .recommendations(blankToNull(request.getRecommendations()))
                .build();
        TrainingVideoAnalysis saved = videoRepository.save(video);
        audit(enrollment.getAcademyId(), null, enrollment.getId(), "VIDEO_ANALYSIS_ADDED", actor, "Video analysis linked to player " + enrollment.getStudentEmail());
        return mapVideo(saved);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> talentDiscovery(String city, String sport, String ageGroup) {
        return talentRepository.findByStatusOrderByTalentScoreDescCreatedAtDesc("DISCOVERABLE").stream()
                .filter(profile -> matches(city, profile.getCity()))
                .filter(profile -> matches(sport, profile.getSport()))
                .filter(profile -> matches(ageGroup, profile.getAgeGroup()))
                .map(this::mapTalent)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> talentProfiles() {
        return talentRepository.findAllByOrderByTalentScoreDescCreatedAtDesc().stream().map(this::mapTalent).toList();
    }

    @Transactional
    public Map<String, Object> publishTalentProfile(AcademyTalentRequests.TalentProfileRequest request, String actor) {
        AcademyEnrollment enrollment = enrollmentRepository.findById(request == null ? null : request.getEnrollmentId()).orElseThrow(() -> ApiException.notFound("Enrollment not found"));
        TrainingProgram program = programRepository.findById(enrollment.getProgramId()).orElseThrow(() -> ApiException.notFound("Training program not found"));
        Optional<PlayerAssessment> latest = assessmentRepository.findByPlayerEmailIgnoreCaseOrderByCreatedAtDesc(enrollment.getStudentEmail()).stream().findFirst();
        int assessmentScore = latest.map(PlayerAssessment::getOverallScore).orElse(0);
        int tournamentImpact = score(request.getTournamentImpactScore());
        int attendanceScore = attendancePercentage(enrollment.getId());
        int talentScore = Math.round((assessmentScore * 0.55f) + (attendanceScore * 0.20f) + (tournamentImpact * 0.25f));
        TalentProfile profile = TalentProfile.builder()
                .academyId(enrollment.getAcademyId())
                .enrollmentId(enrollment.getId())
                .playerName(enrollment.getStudentName())
                .playerEmail(enrollment.getStudentEmail())
                .age(enrollment.getStudentAge())
                .ageGroup(ageGroup(enrollment.getStudentAge()))
                .sport(program.getSport())
                .city(blankToNull(request.getCity()))
                .assessmentScore(assessmentScore)
                .tournamentImpactScore(tournamentImpact)
                .talentScore(talentScore)
                .status(normalize(request.getStatus(), "DISCOVERABLE"))
                .highlights(blankToNull(request.getHighlights()))
                .build();
        TalentProfile saved = talentRepository.save(profile);
        audit(saved.getAcademyId(), null, enrollment.getId(), "TALENT_PROFILE_PUBLISHED", actor, "Talent profile published with score " + saved.getTalentScore());
        return mapTalent(saved);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> certificates() {
        return certificateRepository.findAllByOrderByIssuedAtDesc().stream().map(this::mapCertificate).toList();
    }

    @Transactional
    public Map<String, Object> generateCertificate(AcademyTalentRequests.CertificateRequest request, String actor) {
        AcademyEnrollment enrollment = enrollmentRepository.findById(request == null ? null : request.getEnrollmentId()).orElseThrow(() -> ApiException.notFound("Enrollment not found"));
        TrainingProgram program = programRepository.findById(enrollment.getProgramId()).orElseThrow(() -> ApiException.notFound("Training program not found"));
        AcademyCertificate certificate = AcademyCertificate.builder()
                .academyId(enrollment.getAcademyId())
                .enrollmentId(enrollment.getId())
                .certificateCode(certificateCode())
                .playerName(enrollment.getStudentName())
                .playerEmail(enrollment.getStudentEmail())
                .certificateType(normalize(request.getCertificateType(), "COMPLETION"))
                .sport(program.getSport())
                .pdfUrl(blankToNull(request.getPdfUrl()))
                .status("ISSUED")
                .build();
        AcademyCertificate saved = certificateRepository.save(certificate);
        audit(saved.getAcademyId(), null, enrollment.getId(), "CERTIFICATE_ISSUED", actor, "Certificate issued: " + saved.getCertificateCode());
        return mapCertificate(saved);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> verifyCertificate(String code) {
        AcademyCertificate certificate = certificateRepository.findByCertificateCodeIgnoreCase(required(code, "Certificate code is required"))
                .orElseThrow(() -> ApiException.notFound("Certificate not found"));
        Map<String, Object> response = mapCertificate(certificate);
        response.put("valid", "ISSUED".equalsIgnoreCase(certificate.getStatus()));
        response.put("verificationMethod", "QR_CODE_OR_CERTIFICATE_CODE");
        return response;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> analytics() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("enrollmentGrowth", enrollmentRepository.findAll().size());
        response.put("attendanceTrend", crm(enrollmentRepository.findAll(), attendanceRepository.findAll()));
        response.put("skillImprovementTrend", skillProgression(assessmentRepository.findAll()));
        response.put("talentRanking", talentRepository.findAllByOrderByTalentScoreDescCreatedAtDesc().stream().limit(20).map(this::mapTalent).toList());
        response.put("revenueGrowth", revenue(enrollmentRepository.findAll()));
        response.put("generatedAt", LocalDateTime.now());
        return response;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> revenueReport() {
        Map<String, Object> response = revenue(enrollmentRepository.findAll());
        response.put("refunds", enrollmentRepository.findByStatusOrderByCreatedAtDesc("CANCELLED").stream().map(AcademyEnrollment::getAmountPaid).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add));
        response.put("generatedAt", LocalDateTime.now());
        return response;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> audits() {
        return auditRepository.findTop100ByOrderByCreatedAtDesc().stream().map(this::mapAudit).toList();
    }

    private void promoteWaitlist(TrainingBatch batch) {
        if (batch.getCapacity() == null || batch.getEnrolledCount() == null || batch.getEnrolledCount() >= batch.getCapacity()) {
            return;
        }
        enrollmentRepository.findByBatchIdOrderByCreatedAtDesc(batch.getId()).stream()
                .filter(enrollment -> "WAITLISTED".equals(enrollment.getStatus()))
                .min(Comparator.comparing(AcademyEnrollment::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .ifPresent(enrollment -> {
                    enrollment.setStatus("CONFIRMED");
                    enrollmentRepository.save(enrollment);
                    batch.setEnrolledCount((batch.getEnrolledCount() == null ? 0 : batch.getEnrolledCount()) + 1);
                });
    }

    private Map<String, Object> kpis(List<AcademyProfile> academies,
                                    List<CoachProfile> coaches,
                                    List<TrainingProgram> programs,
                                    List<TrainingBatch> batches,
                                    List<AcademyEnrollment> enrollments,
                                    List<TrainingAttendance> attendance,
                                    List<PlayerAssessment> assessments,
                                    List<TalentProfile> talent,
                                    List<AcademyCertificate> certificates) {
        Map<String, Object> kpis = new LinkedHashMap<>();
        kpis.put("academies", academies.size());
        kpis.put("approvedAcademies", academies.stream().filter(item -> "APPROVED".equalsIgnoreCase(item.getStatus())).count());
        kpis.put("coaches", coaches.size());
        kpis.put("verifiedCoaches", coaches.stream().filter(item -> "VERIFIED".equalsIgnoreCase(item.getStatus())).count());
        kpis.put("programs", programs.size());
        kpis.put("publishedPrograms", programs.stream().filter(item -> "PUBLISHED".equalsIgnoreCase(item.getStatus())).count());
        kpis.put("batches", batches.size());
        kpis.put("enrollments", enrollments.size());
        kpis.put("confirmedEnrollments", enrollments.stream().filter(item -> "CONFIRMED".equalsIgnoreCase(item.getStatus())).count());
        kpis.put("attendanceRecords", attendance.size());
        kpis.put("assessments", assessments.size());
        kpis.put("discoverableTalent", talent.stream().filter(item -> "DISCOVERABLE".equalsIgnoreCase(item.getStatus())).count());
        kpis.put("certificates", certificates.size());
        kpis.put("attendancePercent", attendancePercent(attendance));
        kpis.put("averageAssessment", averageAssessment(assessments));
        kpis.put("revenue", totalRevenue(enrollments));
        return kpis;
    }

    private Map<String, Object> crm(List<AcademyEnrollment> enrollments, List<TrainingAttendance> attendance) {
        Map<String, Object> crm = new LinkedHashMap<>();
        crm.put("activeStudents", enrollments.stream().filter(item -> "CONFIRMED".equalsIgnoreCase(item.getStatus())).count());
        crm.put("inactiveStudents", enrollments.stream().filter(item -> "CANCELLED".equalsIgnoreCase(item.getStatus())).count());
        crm.put("waitlistedStudents", enrollments.stream().filter(item -> "WAITLISTED".equalsIgnoreCase(item.getStatus())).count());
        crm.put("attendancePercent", attendancePercent(attendance));
        crm.put("retentionSignals", List.of("attendance below 70%", "missed assessment", "payment pending", "program completion due"));
        return crm;
    }

    private Map<String, Object> revenue(List<AcademyEnrollment> enrollments) {
        BigDecimal total = totalRevenue(enrollments);
        Map<String, Object> revenue = new LinkedHashMap<>();
        revenue.put("programRevenue", total);
        revenue.put("enrollmentRevenue", total);
        revenue.put("coachRevenue", total.multiply(BigDecimal.valueOf(0.70)));
        revenue.put("platformRevenue", total.multiply(BigDecimal.valueOf(0.30)));
        revenue.put("paidEnrollments", enrollments.stream().filter(item -> nonNullAmount(item.getAmountPaid()).compareTo(BigDecimal.ZERO) > 0).count());
        return revenue;
    }

    private Map<String, Object> skillProgression(List<PlayerAssessment> assessments) {
        Map<String, Object> skill = new LinkedHashMap<>();
        skill.put("averageOverall", averageAssessment(assessments));
        skill.put("averageBatting", averageScore(assessments.stream().map(PlayerAssessment::getBattingScore).toList()));
        skill.put("averageBowling", averageScore(assessments.stream().map(PlayerAssessment::getBowlingScore).toList()));
        skill.put("averageFielding", averageScore(assessments.stream().map(PlayerAssessment::getFieldingScore).toList()));
        skill.put("averageFitness", averageScore(assessments.stream().map(PlayerAssessment::getFitnessScore).toList()));
        skill.put("trend", assessments.size() > 1 ? "TRACKING" : "BASELINE");
        return skill;
    }

    private Map<String, Object> certification(List<AcademyCertificate> certificates) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("issued", certificates.size());
        data.put("completionCertificates", certificates.stream().filter(item -> "COMPLETION".equalsIgnoreCase(item.getCertificateType())).count());
        data.put("skillCertificates", certificates.stream().filter(item -> "SKILL".equalsIgnoreCase(item.getCertificateType())).count());
        data.put("qrVerification", true);
        return data;
    }

    private List<Map<String, Object>> governanceControls() {
        return List.of(
                governance("ACADEMY_ACCREDITATION", "Academy approval, document request, rejection, and suspension workflow"),
                governance("COACH_VERIFICATION", "Coach certification and achievement verification before marketplace visibility"),
                governance("ASSESSMENT_AUDIT", "Assessment, attendance, certificate, and ranking actions are audited"),
                governance("TALENT_PRIVACY", "Talent discovery only exposes profiles explicitly marked discoverable"),
                governance("CERTIFICATE_VERIFICATION", "Certificates are code and QR-verifiable")
        );
    }

    private Map<String, Object> governance(String key, String description) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("key", key);
        item.put("description", description);
        item.put("status", "ENABLED");
        return item;
    }

    private List<Map<String, Object>> priorityActions(List<AcademyProfile> academies,
                                                      List<CoachProfile> coaches,
                                                      List<TrainingProgram> programs,
                                                      List<AcademyEnrollment> enrollments,
                                                      List<PlayerAssessment> assessments,
                                                      List<TalentProfile> talent) {
        List<Map<String, Object>> actions = new ArrayList<>();
        if (academies.stream().anyMatch(item -> "PENDING_APPROVAL".equalsIgnoreCase(item.getStatus()))) actions.add(action("Review academy approvals", "Pending academies need approval, rejection, or document request.", "HIGH"));
        if (coaches.stream().anyMatch(item -> "PENDING_VERIFICATION".equalsIgnoreCase(item.getStatus()))) actions.add(action("Verify coaches", "Coach marketplace visibility depends on verification.", "HIGH"));
        if (programs.stream().noneMatch(item -> "PUBLISHED".equalsIgnoreCase(item.getStatus()))) actions.add(action("Publish training programs", "Enrollment opens only after a program is published.", "MEDIUM"));
        if (!enrollments.isEmpty() && assessments.isEmpty()) actions.add(action("Run first assessment", "Talent rankings need assessment baselines.", "HIGH"));
        if (talent.stream().noneMatch(item -> "DISCOVERABLE".equalsIgnoreCase(item.getStatus()))) actions.add(action("Publish talent profiles", "Selector discovery requires discoverable talent profiles.", "MEDIUM"));
        actions.add(action("Validate certificate workflow", "Completion and skill certificates should be QR-verifiable.", "MEDIUM"));
        return actions;
    }

    private Map<String, Object> action(String title, String description, String severity) {
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("title", title);
        action.put("description", description);
        action.put("severity", severity);
        return action;
    }

    private Map<String, AcademyProfile> academyById() {
        Map<String, AcademyProfile> index = new LinkedHashMap<>();
        academyRepository.findAll().forEach(academy -> index.put(String.valueOf(academy.getId()), academy));
        return index;
    }

    private Map<Long, AcademyProfile> academyIndex() {
        Map<Long, AcademyProfile> index = new LinkedHashMap<>();
        academyRepository.findAll().forEach(academy -> index.put(academy.getId(), academy));
        return index;
    }

    private Map<Long, CoachProfile> coachIndex() {
        Map<Long, CoachProfile> index = new LinkedHashMap<>();
        coachRepository.findAll().forEach(coach -> index.put(coach.getId(), coach));
        return index;
    }

    private Map<String, Object> mapAcademy(AcademyProfile academy) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", academy.getId());
        item.put("name", academy.getName());
        item.put("description", academy.getDescription());
        item.put("sportsOffered", academy.getSportsOffered());
        item.put("city", academy.getCity());
        item.put("address", academy.getAddress());
        item.put("website", academy.getWebsite());
        item.put("email", academy.getEmail());
        item.put("phone", academy.getPhone());
        item.put("logoUrl", academy.getLogoUrl());
        item.put("documentUrl", academy.getDocumentUrl());
        item.put("status", academy.getStatus());
        item.put("accreditationLevel", academy.getAccreditationLevel());
        item.put("createdAt", academy.getCreatedAt());
        return item;
    }

    private Map<String, Object> mapCoach(CoachProfile coach) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", coach.getId());
        item.put("academyId", coach.getAcademyId());
        item.put("name", coach.getName());
        item.put("email", coach.getEmail());
        item.put("phone", coach.getPhone());
        item.put("sport", coach.getSport());
        item.put("city", coach.getCity());
        item.put("experienceYears", coach.getExperienceYears());
        item.put("pricePerSession", coach.getPricePerSession());
        item.put("certifications", coach.getCertifications());
        item.put("achievements", coach.getAchievements());
        item.put("profilePhotoUrl", coach.getProfilePhotoUrl());
        item.put("status", coach.getStatus());
        item.put("rating", coach.getRating());
        item.put("studentsCoached", coach.getStudentsCoached());
        item.put("verifiedBadge", "VERIFIED".equalsIgnoreCase(coach.getStatus()));
        item.put("createdAt", coach.getCreatedAt());
        return item;
    }

    private Map<String, Object> mapProgram(TrainingProgram program) {
        return mapProgram(program, academyIndex());
    }

    private Map<String, Object> mapProgram(TrainingProgram program, Map<Long, AcademyProfile> academies) {
        CoachProfile coach = coachIndex().get(program.getCoachId());
        AcademyProfile academy = academies.get(program.getAcademyId());
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", program.getId());
        item.put("academyId", program.getAcademyId());
        item.put("academyName", academy == null ? null : academy.getName());
        item.put("coachId", program.getCoachId());
        item.put("coachName", coach == null ? null : coach.getName());
        item.put("title", program.getTitle());
        item.put("sport", program.getSport());
        item.put("level", program.getLevel());
        item.put("description", program.getDescription());
        item.put("durationWeeks", program.getDurationWeeks());
        item.put("capacity", program.getCapacity());
        item.put("price", program.getPrice());
        item.put("status", program.getStatus());
        item.put("createdAt", program.getCreatedAt());
        return item;
    }

    private Map<String, Object> mapBatch(TrainingBatch batch) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", batch.getId());
        item.put("programId", batch.getProgramId());
        item.put("name", batch.getName());
        item.put("scheduleLabel", batch.getScheduleLabel());
        item.put("startDate", batch.getStartDate());
        item.put("endDate", batch.getEndDate());
        item.put("capacity", batch.getCapacity());
        item.put("enrolledCount", batch.getEnrolledCount());
        item.put("availableSeats", Math.max(0, (batch.getCapacity() == null ? 0 : batch.getCapacity()) - (batch.getEnrolledCount() == null ? 0 : batch.getEnrolledCount())));
        item.put("status", batch.getStatus());
        item.put("createdAt", batch.getCreatedAt());
        return item;
    }

    private Map<String, Object> mapEnrollment(AcademyEnrollment enrollment) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", enrollment.getId());
        item.put("academyId", enrollment.getAcademyId());
        item.put("programId", enrollment.getProgramId());
        item.put("batchId", enrollment.getBatchId());
        item.put("studentName", enrollment.getStudentName());
        item.put("studentEmail", enrollment.getStudentEmail());
        item.put("studentAge", enrollment.getStudentAge());
        item.put("status", enrollment.getStatus());
        item.put("amountPaid", enrollment.getAmountPaid());
        item.put("attendancePercent", attendancePercentage(enrollment.getId()));
        item.put("createdAt", enrollment.getCreatedAt());
        return item;
    }

    private Map<String, Object> mapAttendance(TrainingAttendance attendance) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", attendance.getId());
        item.put("enrollmentId", attendance.getEnrollmentId());
        item.put("batchId", attendance.getBatchId());
        item.put("sessionDate", attendance.getSessionDate());
        item.put("status", attendance.getStatus());
        item.put("markedBy", attendance.getMarkedBy());
        item.put("notes", attendance.getNotes());
        item.put("createdAt", attendance.getCreatedAt());
        return item;
    }

    private Map<String, Object> mapAssessment(PlayerAssessment assessment) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", assessment.getId());
        item.put("enrollmentId", assessment.getEnrollmentId());
        item.put("coachId", assessment.getCoachId());
        item.put("playerName", assessment.getPlayerName());
        item.put("playerEmail", assessment.getPlayerEmail());
        item.put("sport", assessment.getSport());
        item.put("battingScore", assessment.getBattingScore());
        item.put("bowlingScore", assessment.getBowlingScore());
        item.put("fieldingScore", assessment.getFieldingScore());
        item.put("fitnessScore", assessment.getFitnessScore());
        item.put("disciplineScore", assessment.getDisciplineScore());
        item.put("overallScore", assessment.getOverallScore());
        item.put("recommendations", assessment.getRecommendations());
        item.put("createdAt", assessment.getCreatedAt());
        return item;
    }

    private Map<String, Object> mapVideo(TrainingVideoAnalysis video) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", video.getId());
        item.put("enrollmentId", video.getEnrollmentId());
        item.put("playerEmail", video.getPlayerEmail());
        item.put("category", video.getCategory());
        item.put("videoUrl", video.getVideoUrl());
        item.put("coachComments", video.getCoachComments());
        item.put("improvementAreas", video.getImprovementAreas());
        item.put("recommendations", video.getRecommendations());
        item.put("createdAt", video.getCreatedAt());
        return item;
    }

    private Map<String, Object> mapTalent(TalentProfile talent) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", talent.getId());
        item.put("academyId", talent.getAcademyId());
        item.put("enrollmentId", talent.getEnrollmentId());
        item.put("playerName", talent.getPlayerName());
        item.put("playerEmail", talent.getPlayerEmail());
        item.put("age", talent.getAge());
        item.put("ageGroup", talent.getAgeGroup());
        item.put("sport", talent.getSport());
        item.put("city", talent.getCity());
        item.put("assessmentScore", talent.getAssessmentScore());
        item.put("tournamentImpactScore", talent.getTournamentImpactScore());
        item.put("talentScore", talent.getTalentScore());
        item.put("rankingBand", rankingBand(talent.getTalentScore()));
        item.put("status", talent.getStatus());
        item.put("highlights", talent.getHighlights());
        item.put("createdAt", talent.getCreatedAt());
        return item;
    }

    private Map<String, Object> mapCertificate(AcademyCertificate certificate) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", certificate.getId());
        item.put("academyId", certificate.getAcademyId());
        item.put("enrollmentId", certificate.getEnrollmentId());
        item.put("certificateCode", certificate.getCertificateCode());
        item.put("playerName", certificate.getPlayerName());
        item.put("playerEmail", certificate.getPlayerEmail());
        item.put("certificateType", certificate.getCertificateType());
        item.put("sport", certificate.getSport());
        item.put("pdfUrl", certificate.getPdfUrl());
        item.put("status", certificate.getStatus());
        item.put("issuedAt", certificate.getIssuedAt());
        return item;
    }

    private Map<String, Object> mapAudit(AcademyTalentAudit audit) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", audit.getId());
        item.put("academyId", audit.getAcademyId());
        item.put("coachId", audit.getCoachId());
        item.put("enrollmentId", audit.getEnrollmentId());
        item.put("action", audit.getAction());
        item.put("actor", audit.getActor());
        item.put("summary", audit.getSummary());
        item.put("createdAt", audit.getCreatedAt());
        return item;
    }

    private void audit(Long academyId, Long coachId, Long enrollmentId, String action, String actor, String summary) {
        auditRepository.save(AcademyTalentAudit.builder()
                .academyId(academyId)
                .coachId(coachId)
                .enrollmentId(enrollmentId)
                .action(action)
                .actor(defaultString(actor, "system"))
                .summary(summary)
                .build());
        auditLogRepository.save(AuditLog.builder()
                .adminId(defaultString(actor, "system"))
                .action(action)
                .entity("AcademyTalent #" + (academyId == null ? "platform" : academyId))
                .build());
    }

    private Comparator<Object> byCreatedDesc() {
        return (left, right) -> {
            LocalDateTime leftTime = createdAt(left);
            LocalDateTime rightTime = createdAt(right);
            if (leftTime == null && rightTime == null) {
                return 0;
            }
            if (leftTime == null) {
                return 1;
            }
            if (rightTime == null) {
                return -1;
            }
            return rightTime.compareTo(leftTime);
        };
    }

    private LocalDateTime createdAt(Object value) {
        if (value instanceof AcademyProfile item) return item.getCreatedAt();
        if (value instanceof CoachProfile item) return item.getCreatedAt();
        if (value instanceof TrainingProgram item) return item.getCreatedAt();
        if (value instanceof TrainingBatch item) return item.getCreatedAt();
        if (value instanceof AcademyEnrollment item) return item.getCreatedAt();
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

    private boolean containsIgnoreCase(String source, String expected) {
        return source != null && expected != null && source.toLowerCase(Locale.ROOT).contains(expected.toLowerCase(Locale.ROOT));
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

    private BigDecimal nonNegative(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw ApiException.badRequest("Amount cannot be negative");
        }
        return value;
    }

    private BigDecimal nonNullAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private Integer positive(Integer value, String message) {
        if (value == null || value <= 0) {
            throw ApiException.badRequest(message);
        }
        return value;
    }

    private int score(Integer value) {
        if (value == null) {
            return 0;
        }
        return Math.max(0, Math.min(100, value));
    }

    private double defaultNumber(Double value) {
        return value == null ? 0.0 : value;
    }

    private BigDecimal totalRevenue(List<AcademyEnrollment> enrollments) {
        return enrollments.stream()
                .filter(item -> !"CANCELLED".equalsIgnoreCase(item.getStatus()))
                .map(AcademyEnrollment::getAmountPaid)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private long attendancePercent(List<TrainingAttendance> attendance) {
        if (attendance.isEmpty()) {
            return 0;
        }
        long present = attendance.stream().filter(item -> Set.of("PRESENT", "LATE").contains(item.getStatus())).count();
        return Math.round((present * 100.0) / attendance.size());
    }

    private int attendancePercentage(Long enrollmentId) {
        List<TrainingAttendance> records = attendanceRepository.findByEnrollmentIdOrderBySessionDateDesc(enrollmentId);
        return (int) attendancePercent(records);
    }

    private long averageAssessment(List<PlayerAssessment> assessments) {
        return Math.round(assessments.stream().map(PlayerAssessment::getOverallScore).filter(Objects::nonNull).mapToInt(Integer::intValue).average().orElse(0));
    }

    private long averageScore(List<Integer> scores) {
        return Math.round(scores.stream().filter(Objects::nonNull).mapToInt(Integer::intValue).average().orElse(0));
    }

    private String ageGroup(Integer age) {
        if (age == null) return "OPEN";
        if (age <= 12) return "U12";
        if (age <= 16) return "U16";
        if (age <= 19) return "U19";
        return "OPEN";
    }

    private String rankingBand(Integer score) {
        int value = score == null ? 0 : score;
        if (value >= 85) return "ELITE";
        if (value >= 70) return "HIGH_POTENTIAL";
        if (value >= 50) return "DEVELOPING";
        return "FOUNDATION";
    }

    private String certificateCode() {
        byte[] buffer = new byte[8];
        RANDOM.nextBytes(buffer);
        return "TRUF-CERT-" + Base64.getUrlEncoder().withoutPadding().encodeToString(buffer).toUpperCase(Locale.ROOT);
    }
}
