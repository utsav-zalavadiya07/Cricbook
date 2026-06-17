package com.example.Cricbook.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Map;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class AcademyTalentControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    @org.springframework.beans.factory.annotation.Autowired
    private WebApplicationContext context;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void adminAcademyDashboardReturnsTalentLifecycleControls() throws Exception {
        mockMvc.perform(get("/api/v1/admin/academy/dashboard")
                        .with(user("academy_admin@cricbook.com").roles("SUPER_ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kpis.academies", greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.academyCrm").exists())
                .andExpect(jsonPath("$.skillProgression").exists())
                .andExpect(jsonPath("$.certification.qrVerification").value(true))
                .andExpect(jsonPath("$.governanceControls").isArray())
                .andExpect(jsonPath("$.priorityActions").isArray());
    }

    @Test
    void academyCoachProgramEnrollmentAssessmentTalentAndCertificateFlowWorks() throws Exception {
        AcademyFixture fixture = createApprovedAcademyCoachAndProgram();

        JsonNode batch = postAdmin("/api/v1/admin/academy/batches", Map.of(
                "programId", fixture.programId(),
                "name", "Morning Elite Batch",
                "scheduleLabel", "Mon-Wed-Fri 06:00",
                "capacity", 2
        ));

        JsonNode enrollment = postAdmin("/api/v1/admin/academy/enrollments", Map.of(
                "batchId", batch.get("id").asLong(),
                "studentName", "Aarav Talent",
                "studentEmail", "aarav-" + System.nanoTime() + "@example.com",
                "studentAge", 15,
                "amountPaid", 12000
        ));

        postAdmin("/api/v1/admin/academy/attendance", Map.of(
                "enrollmentId", enrollment.get("id").asLong(),
                "status", "PRESENT"
        ));

        JsonNode assessment = postAdmin("/api/v1/admin/academy/assessments", Map.of(
                "enrollmentId", enrollment.get("id").asLong(),
                "coachId", fixture.coachId(),
                "battingScore", 88,
                "bowlingScore", 72,
                "fieldingScore", 80,
                "fitnessScore", 84,
                "disciplineScore", 90,
                "recommendations", "Continue power hitting and fielding drills"
        ));

        postAdmin("/api/v1/admin/academy/videos", Map.of(
                "enrollmentId", enrollment.get("id").asLong(),
                "category", "BATTING",
                "videoUrl", "https://cdn.example.com/batting.mp4",
                "coachComments", "Stable head position",
                "improvementAreas", "Back-foot punch",
                "recommendations", "Repeat 30-minute drill"
        ));

        JsonNode talent = postAdmin("/api/v1/admin/academy/talent", Map.of(
                "enrollmentId", enrollment.get("id").asLong(),
                "city", "Surat",
                "tournamentImpactScore", 82,
                "highlights", "Top-order batter with leadership signals",
                "status", "DISCOVERABLE"
        ));

        JsonNode certificate = postAdmin("/api/v1/admin/academy/certificates", Map.of(
                "enrollmentId", enrollment.get("id").asLong(),
                "certificateType", "COMPLETION",
                "pdfUrl", "https://cdn.example.com/certificate.pdf"
        ));

        mockMvc.perform(get("/api/v1/talent/discovery")
                        .param("city", "Surat")
                        .param("sport", "CRICKET")
                        .param("ageGroup", "U16"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].playerName").value(talent.get("playerName").asText()))
                .andExpect(jsonPath("$[0].talentScore", greaterThanOrEqualTo(1)));

        mockMvc.perform(get("/api/v1/certificates/{code}/verify", certificate.get("certificateCode").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.certificateCode").value(certificate.get("certificateCode").asText()))
                .andExpect(jsonPath("$.playerName").value(enrollment.get("studentName").asText()));

        mockMvc.perform(get("/api/v1/admin/academy/analytics")
                        .with(user("academy_admin@cricbook.com").roles("SUPER_ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.talentRanking").isArray())
                .andExpect(jsonPath("$.skillImprovementTrend.averageOverall", greaterThanOrEqualTo(assessment.get("overallScore").asInt())));
    }

    @Test
    void publicAcademyCoachAndProgramMarketplacesRespectApproval() throws Exception {
        AcademyFixture fixture = createApprovedAcademyCoachAndProgram();

        mockMvc.perform(get("/api/v1/academies/marketplace")
                        .param("city", "Surat")
                        .param("sport", "CRICKET"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(fixture.academyId().intValue()));

        mockMvc.perform(get("/api/v1/coaches/marketplace")
                        .param("city", "Surat")
                        .param("sport", "CRICKET"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(fixture.coachId().intValue()))
                .andExpect(jsonPath("$[0].verifiedBadge").value(true));

        mockMvc.perform(get("/api/v1/training/programs")
                        .param("city", "Surat")
                        .param("sport", "CRICKET"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(fixture.programId().intValue()))
                .andExpect(jsonPath("$[0].status").value("PUBLISHED"));
    }

    private AcademyFixture createApprovedAcademyCoachAndProgram() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        JsonNode academy = postPublic("/api/v1/academies/register", Map.of(
                "name", "Elite Cricket Academy " + suffix,
                "sportsOffered", "CRICKET,FOOTBALL",
                "city", "Surat",
                "email", "academy-" + suffix + "@example.com",
                "website", "https://academy.example.com"
        ));
        postAdmin("/api/v1/admin/academy/academies/" + academy.get("id").asLong() + "/status", Map.of(
                "status", "APPROVED",
                "notes", "Documents verified"
        ));

        JsonNode coach = postPublic("/api/v1/coaches/register", Map.of(
                "academyId", academy.get("id").asLong(),
                "name", "Coach " + suffix,
                "email", "coach-" + suffix + "@example.com",
                "sport", "CRICKET",
                "city", "Surat",
                "experienceYears", 8,
                "pricePerSession", 1500
        ));
        postAdmin("/api/v1/admin/academy/coaches/" + coach.get("id").asLong() + "/status", Map.of(
                "status", "VERIFIED",
                "notes", "Certification validated"
        ));

        JsonNode program = postAdmin("/api/v1/admin/academy/programs", Map.of(
                "academyId", academy.get("id").asLong(),
                "coachId", coach.get("id").asLong(),
                "title", "Professional Cricket Development " + suffix,
                "sport", "CRICKET",
                "level", "PROFESSIONAL",
                "durationWeeks", 12,
                "capacity", 24,
                "price", 12000,
                "status", "PUBLISHED"
        ));
        return new AcademyFixture(academy.get("id").asLong(), coach.get("id").asLong(), program.get("id").asLong());
    }

    private JsonNode postPublic(String url, Map<String, Object> payload) throws Exception {
        MvcResult result = mockMvc.perform(post(url)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode postAdmin(String url, Map<String, Object> payload) throws Exception {
        MvcResult result = mockMvc.perform(post(url)
                        .with(user("academy_admin@cricbook.com").roles("SUPER_ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private record AcademyFixture(Long academyId, Long coachId, Long programId) {
    }
}
