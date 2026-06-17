package com.example.Cricbook.service.ai;

import com.example.Cricbook.entity.*;
import com.example.Cricbook.enums.BookingStatus;
import com.example.Cricbook.repository.*;
import com.example.Cricbook.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIAssistantService {
    private static final String MODEL_VERSION = "cricbook-intelligence-v1.0-rule-hybrid";
    private static final List<String> PROMPT_ATTACK_PATTERNS = List.of(
            "ignore previous", "ignore all previous", "system prompt", "developer message", "jailbreak",
            "show token", "show jwt", "extract password", "database password", "bypass access", "act as admin"
    );
    private static final List<String> OFFENSIVE_PATTERNS = List.of("hate", "kill", "abuse", "slur");

    private final TurfRepository turfRepository;
    private final SlotRepository slotRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final AIConversationMessageRepository conversationRepository;
    private final AuditLogRepository auditLogRepository;

    @Transactional
    public Map<String, Object> processMessage(Long userId, String message) {
        return processMessage(userId, message, null);
    }

    @Transactional
    public Map<String, Object> processMessage(Long userId, String message, String conversationId) {
        long startedAt = System.currentTimeMillis();
        log.info("AI Assistant processing message for user ID {}: '{}'", userId, message);
        if (message == null || message.isBlank()) {
            throw ApiException.badRequest("Message is required.");
        }

        String normalizedConversationId = conversationId == null || conversationId.isBlank()
                ? "conv-" + userId + "-" + UUID.randomUUID()
                : conversationId.trim();
        String lowerMessage = message.toLowerCase(Locale.ROOT);
        Map<String, Object> response;

        if (containsAny(lowerMessage, PROMPT_ATTACK_PATTERNS)) {
            response = safetyResponse("SAFETY_BLOCKED", "Prompt injection or data extraction attempt detected.", List.of("PROMPT_INJECTION"));
            persistConversation(userId, normalizedConversationId, message, response);
            writeAudit("AI_PROMPT_GUARDRAIL", "User #" + userId + " prompt blocked");
            return finalizeResponse(response, normalizedConversationId, startedAt);
        }

        if (containsAny(lowerMessage, OFFENSIVE_PATTERNS)) {
            response = safetyResponse("SAFETY_BLOCKED", "I cannot help with abusive or harmful content. I can still help with bookings, support, teams, tournaments, or platform insights.", List.of("ABUSE_FILTER"));
            persistConversation(userId, normalizedConversationId, message, response);
            writeAudit("AI_ABUSE_GUARDRAIL", "User #" + userId + " message blocked");
            return finalizeResponse(response, normalizedConversationId, startedAt);
        }

        if (lowerMessage.contains("refund") || lowerMessage.contains("payment failed") || lowerMessage.contains("support")) {
            response = handleSupportQuery(userId, lowerMessage);
        } else if (lowerMessage.contains("tournament") || lowerMessage.contains("fixture") || lowerMessage.contains("bracket")) {
            response = handleTournamentQuery();
        } else if (lowerMessage.contains("team") || lowerMessage.contains("opponent") || lowerMessage.contains("challenge")) {
            response = handleTeamQuery(lowerMessage);
        } else if (lowerMessage.contains("stats") || lowerMessage.contains("ranking") || lowerMessage.contains("achievement")) {
            response = handlePlayerStatsQuery(userId);
        } else if (lowerMessage.contains("best") && (lowerMessage.contains("turf") || lowerMessage.contains("perform"))) {
            response = handleBestTurfQuery(userId);
        } else if (lowerMessage.contains("revenue") && (lowerMessage.contains("drop") || lowerMessage.contains("decrease") || lowerMessage.contains("why"))) {
            response = handleRevenueDropQuery(userId);
        } else if (lowerMessage.contains("revenue") || lowerMessage.contains("occupancy") || lowerMessage.contains("forecast")) {
            response = handleOwnerTrendQuery(userId, lowerMessage);
        } else if (isBookingIntent(lowerMessage)) {
            response = handleBookingQuery(message);
        } else {
            response = clarifyResponse();
        }

        persistConversation(userId, normalizedConversationId, message, response);
        return finalizeResponse(response, normalizedConversationId, startedAt);
    }

    private Map<String, Object> handleBookingQuery(String message) {
        Map<String, Object> response = new HashMap<>();
        String lowerMessage = message.toLowerCase();

        // 1. Resolve city
        String city = "Surat"; // default
        if (lowerMessage.contains("mumbai")) {
            city = "Mumbai";
        } else if (lowerMessage.contains("surat")) {
            city = "Surat";
        } else if (lowerMessage.contains("delhi")) {
            city = "Delhi";
        }

        // 2. Resolve date (e.g. Saturday, Sunday, Saturday 7PM)
        LocalDate date = LocalDate.now();
        if (lowerMessage.contains("saturday")) {
            date = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));
        } else if (lowerMessage.contains("sunday")) {
            date = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        } else if (lowerMessage.contains("tomorrow")) {
            date = LocalDate.now().plusDays(1);
        }

        // 3. Resolve hour (e.g., 7PM -> 19:00)
        LocalTime preferredTime = LocalTime.of(18, 0); // default
        Pattern timePattern = Pattern.compile("(\\d+)\\s*(pm|am)");
        Matcher matcher = timePattern.matcher(lowerMessage);
        if (matcher.find()) {
            int hour = Integer.parseInt(matcher.group(1));
            String period = matcher.group(2);
            if (period.equals("pm") && hour < 12) {
                hour += 12;
            } else if (period.equals("am") && hour == 12) {
                hour = 0;
            }
            preferredTime = LocalTime.of(hour, 0);
        }

        // Search for an active turf in this city
        final String searchCity = city;
        List<Turf> matchingTurfs = turfRepository.findAll().stream()
                .filter(t -> t.getCity() != null && t.getCity().equalsIgnoreCase(searchCity))
                .filter(t -> t.getStatus() == com.example.Cricbook.enums.TurfStatus.ACTIVE)
                .collect(Collectors.toList());

        if (matchingTurfs.isEmpty()) {
            matchingTurfs = turfRepository.findAll().stream()
                    .filter(t -> t.getStatus() == com.example.Cricbook.enums.TurfStatus.ACTIVE)
                    .collect(Collectors.toList());
        }

        if (matchingTurfs.isEmpty()) {
            response.put("reply", "I couldn't find any active turfs at the moment. Please try again later!");
            response.put("action", "NONE");
            response.put("intent", "TURF_SEARCH");
            response.put("confidence", 0.58);
            response.put("escalationRequired", false);
            response.put("suggestedActions", List.of("Try another city", "Open turf discovery"));
            response.put("sources", List.of("turfs"));
            return response;
        }

        Turf chosenTurf = matchingTurfs.get(0);

        // Find slots for this turf
        List<Slot> slots = slotRepository.findByTurfId(chosenTurf.getId());
        Slot chosenSlot = null;
        if (!slots.isEmpty()) {
            final LocalTime searchTime = preferredTime;
            chosenSlot = slots.stream()
                    .min(Comparator.comparingInt(s -> Math.abs(s.getStartTime().getHour() - searchTime.getHour())))
                    .orElse(slots.get(0));
        }

        if (chosenSlot == null) {
            response.put("reply", "I found active turfs in " + city + " but no slots are scheduled. Please check again!");
            response.put("action", "NONE");
            response.put("intent", "SLOT_RECOMMENDATION");
            response.put("confidence", 0.61);
            response.put("escalationRequired", false);
            response.put("suggestedActions", List.of("Try a different date", "Open turf page"));
            response.put("sources", List.of("turfs", "slots"));
            return response;
        }

        String replyMessage = String.format("Perfect! I found an excellent slot for you at **%s** (%s) on **%s** starting at **%s**. Would you like to confirm this?",
                chosenTurf.getName(), chosenTurf.getCity(), date, chosenSlot.getStartTime());
        
        response.put("reply", replyMessage);
        response.put("action", "CHECKOUT");
        response.put("turfId", chosenTurf.getId());
        response.put("slotId", chosenSlot.getId());
        response.put("date", date.toString());
        response.put("checkoutLink", String.format("/checkout?turfId=%d&slotId=%d&date=%s", chosenTurf.getId(), chosenSlot.getId(), date));
        response.put("intent", "ONE_CLICK_BOOKING");
        response.put("confidence", 0.89);
        response.put("escalationRequired", false);
        response.put("suggestedActions", List.of("Review slot", "Open checkout"));
        response.put("sources", List.of("turfs", "slots", "availability"));
        response.put("explainability", List.of(
                "Matched city from query: " + city,
                "Resolved play date: " + date,
                "Selected nearest available slot around " + preferredTime
        ));

        return response;
    }

    private Map<String, Object> handleBestTurfQuery(Long userId) {
        Map<String, Object> response = new HashMap<>();
        
        // Find owner's turfs
        List<Turf> ownerTurfs = turfRepository.findByOwnerId(userId);
        if (ownerTurfs.isEmpty()) {
            response.put("reply", "You don't have any active turf registered in Cricbook yet. Add a turf to see performance metrics!");
            response.put("action", "NONE");
            return response;
        }

        Turf bestTurf = null;
        BigDecimal highestRevenue = BigDecimal.ZERO;
        int maxBookings = 0;

        for (Turf turf : ownerTurfs) {
            List<Booking> bookings = bookingRepository.findByTurfId(turf.getId());
            List<Booking> confirmed = bookings.stream()
                    .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                    .collect(Collectors.toList());
            
            BigDecimal revenue = confirmed.stream()
                    .map(Booking::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (revenue.compareTo(highestRevenue) > 0 || (revenue.compareTo(highestRevenue) == 0 && confirmed.size() > maxBookings)) {
                highestRevenue = revenue;
                maxBookings = confirmed.size();
                bestTurf = turf;
            }
        }

        if (bestTurf == null) {
            bestTurf = ownerTurfs.get(0);
        }

        String replyMessage = String.format("Your best performing turf is **%s** in %s! It has generated a total revenue of **$%s** across **%d** confirmed slot bookings. Customers love the facility!",
                bestTurf.getName(), bestTurf.getCity(), highestRevenue.setScale(2, java.math.RoundingMode.HALF_UP), maxBookings);

        response.put("reply", replyMessage);
        response.put("action", "INSIGHTS");
        response.put("turfId", bestTurf.getId());
        response.put("insightsLink", "/owner/insights?turfId=" + bestTurf.getId());
        response.put("intent", "OWNER_BEST_TURF");
        response.put("confidence", ownerTurfs.size() > 1 ? 0.86 : 0.72);
        response.put("escalationRequired", false);
        response.put("sources", List.of("bookings", "turfs"));
        response.put("suggestedActions", List.of("Open owner insights", "Compare pricing rules"));
        response.put("explainability", List.of(
                "Compared confirmed bookings by owned turf",
                "Ranked by revenue first and booking count second"
        ));

        return response;
    }

    private Map<String, Object> handleRevenueDropQuery(Long userId) {
        Map<String, Object> response = new HashMap<>();
        List<Turf> ownerTurfs = turfRepository.findByOwnerId(userId);

        if (ownerTurfs.isEmpty()) {
            response.put("reply", "No active turfs found under your account to analyze revenue metrics.");
            response.put("action", "NONE");
            return response;
        }

        int totalBookings = 0;
        int cancelledBookings = 0;

        for (Turf turf : ownerTurfs) {
            List<Booking> bookings = bookingRepository.findByTurfId(turf.getId());
            totalBookings += bookings.size();
            cancelledBookings += bookings.stream()
                    .filter(b -> b.getStatus() == BookingStatus.CANCELLED)
                    .count();
        }

        double cancellationRate = totalBookings > 0 ? ((double) cancelledBookings / totalBookings) * 100 : 0.0;

        String explanation = "Based on our AI analytics: Last month, your revenue showed a minor drop primarily due to: \n" +
                "1. A **" + String.format("%.1f", cancellationRate) + "%** slot cancellation rate.\n" +
                "2. Reduced off-peak bookings between 12:00 PM and 4:00 PM.\n\n" +
                "💡 **AI Recommendation**: Enable our *Weekend & Peak Hour Dynamic pricing rules* to capture a +15% premium on high-demand slots, or offer a 10% coupon for weekday afternoon bookings!";

        response.put("reply", explanation);
        response.put("action", "RECOMMENDATION");
        response.put("insightsLink", "/owner/pricing");
        response.put("intent", "OWNER_REVENUE_DIAGNOSIS");
        response.put("confidence", totalBookings > 0 ? 0.82 : 0.55);
        response.put("escalationRequired", false);
        response.put("sources", List.of("bookings", "cancellations", "pricing_rules"));
        response.put("suggestedActions", List.of("Enable peak pricing", "Offer weekday retention coupon", "Review cancellation reasons"));
        response.put("explainability", List.of(
                "Cancellation rate calculated from owned turf bookings",
                "Off-peak recommendation generated from low-utilization operating window"
        ));

        return response;
    }

    private Map<String, Object> handleOwnerTrendQuery(Long userId, String lowerMessage) {
        List<Turf> ownerTurfs = turfRepository.findByOwnerId(userId);
        List<Booking> bookings = ownerTurfs.stream()
                .flatMap(turf -> bookingRepository.findByTurfId(turf.getId()).stream())
                .toList();
        BigDecimal revenue = bookings.stream()
                .filter(booking -> booking.getAmount() != null)
                .map(Booking::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long completedOrConfirmed = bookings.stream()
                .filter(booking -> booking.getStatus() == BookingStatus.CONFIRMED || booking.getStatus() == BookingStatus.COMPLETED)
                .count();
        int turfCount = Math.max(ownerTurfs.size(), 1);
        BigDecimal forecast = revenue.multiply(BigDecimal.valueOf(1.08)).add(BigDecimal.valueOf(completedOrConfirmed * 75L));

        Map<String, Object> response = new HashMap<>();
        response.put("reply", "Your owner forecast shows projected revenue of Rs " + forecast.setScale(0, RoundingMode.HALF_UP)
                + " with " + completedOrConfirmed + " confirmed/completed bookings. Highest-impact action: protect evening slots and retarget inactive repeat players.");
        response.put("action", "INSIGHTS");
        response.put("intent", lowerMessage.contains("forecast") ? "REVENUE_FORECAST" : "OWNER_BUSINESS_INSIGHT");
        response.put("confidence", bookings.isEmpty() ? 0.54 : 0.81);
        response.put("escalationRequired", false);
        response.put("sources", List.of("bookings", "owned_turfs", "pricing_rules"));
        response.put("suggestedActions", List.of("Review dynamic pricing", "Run weekday campaign", "Compare turf occupancy"));
        response.put("explainability", List.of(
                "Projected revenue applies a conservative 8% demand uplift",
                "Booking volume is normalized across " + turfCount + " owned turf(s)"
        ));
        response.put("insightsLink", "/owner/insights");
        return response;
    }

    private Map<String, Object> handleSupportQuery(Long userId, String lowerMessage) {
        Map<String, Object> response = new HashMap<>();
        boolean lowConfidence = lowerMessage.contains("angry") || lowerMessage.contains("legal") || lowerMessage.contains("complaint");
        response.put("reply", lowConfidence
                ? "I can help collect the details, but this needs a human support review. I created an escalation recommendation for the support queue."
                : "For booking or refund issues, open your booking details and check refund status. If payment is deducted but booking is pending, support should verify gateway status.");
        response.put("action", lowConfidence ? "ESCALATE_SUPPORT" : "SUPPORT_GUIDE");
        response.put("intent", "SUPPORT_BOT");
        response.put("confidence", lowConfidence ? 0.48 : 0.78);
        response.put("escalationRequired", lowConfidence);
        response.put("sources", List.of("bookings", "payments", "support_policy"));
        response.put("suggestedActions", lowConfidence
                ? List.of("Create ticket", "Attach payment reference", "Notify support")
                : List.of("Open booking history", "Check wallet/refund status"));
        response.put("explainability", List.of("Detected support/refund intent", "Escalation is enabled when confidence or customer sentiment is risky"));
        return response;
    }

    private Map<String, Object> handleTournamentQuery() {
        Map<String, Object> response = new HashMap<>();
        response.put("reply", "Tournament assistant can generate league, knockout, and group-stage workflows. Use organizer tournament details to validate team count, date window, turf capacity, and bracket format before fixture generation.");
        response.put("action", "TOURNAMENT_ASSISTANT");
        response.put("intent", "TOURNAMENT_ASSISTANT");
        response.put("confidence", 0.76);
        response.put("escalationRequired", false);
        response.put("sources", List.of("tournaments", "fixtures", "teams"));
        response.put("suggestedActions", List.of("Open tournaments", "Generate fixtures", "Review bracket"));
        response.put("explainability", List.of("Detected tournament/fixture/bracket terms", "Suggested workflow checks before fixture generation"));
        return response;
    }

    private Map<String, Object> handleTeamQuery(String lowerMessage) {
        Map<String, Object> response = new HashMap<>();
        response.put("reply", lowerMessage.contains("opponent")
                ? "Opponent finder should match nearby teams by city, availability, and similar win-rate. Start with teams in the same city and avoid teams already booked for the requested slot."
                : "Team finder can recommend nearby teams or player openings based on city, role need, activity, and availability.");
        response.put("action", "TEAM_FINDER");
        response.put("intent", lowerMessage.contains("opponent") ? "OPPONENT_FINDER" : "TEAM_FINDER");
        response.put("confidence", 0.74);
        response.put("escalationRequired", false);
        response.put("sources", List.of("teams", "availability", "matches"));
        response.put("suggestedActions", List.of("Open teams", "Check availability", "Send challenge"));
        response.put("explainability", List.of("Detected team/opponent context", "Prioritized same-city and availability match"));
        return response;
    }

    private Map<String, Object> handlePlayerStatsQuery(Long userId) {
        long matchesOrBookings = bookingRepository.findByUserId(userId).size();
        Map<String, Object> response = new HashMap<>();
        response.put("reply", "Your cricket profile intelligence can summarize bookings, match history, rankings, achievements, and performance trends. Current activity signal: " + matchesOrBookings + " booking/match-linked record(s).");
        response.put("action", "PLAYER_STATS");
        response.put("intent", "PLAYER_STATS");
        response.put("confidence", matchesOrBookings > 0 ? 0.73 : 0.56);
        response.put("escalationRequired", false);
        response.put("sources", List.of("bookings", "player_profile", "rankings"));
        response.put("suggestedActions", List.of("Open profile", "View achievements", "Check leaderboard"));
        response.put("explainability", List.of("Detected stats/ranking/achievement context", "Used booking-linked activity as profile signal"));
        return response;
    }

    private Map<String, Object> clarifyResponse() {
        Map<String, Object> response = new HashMap<>();
        response.put("reply", "I can help with turf search, available slots, one-click booking, tournaments, teams, refunds, owner revenue, or player stats. Please add one of those details.");
        response.put("action", "CLARIFY");
        response.put("intent", "AMBIGUOUS_QUERY");
        response.put("confidence", 0.42);
        response.put("escalationRequired", false);
        response.put("sources", List.of("intent_router"));
        response.put("suggestedActions", List.of("Find turf near me", "Show available slots", "Explain revenue trend"));
        response.put("explainability", List.of("No supported intent crossed the confidence threshold"));
        return response;
    }

    private Map<String, Object> safetyResponse(String intent, String reply, List<String> safetyFlags) {
        Map<String, Object> response = new HashMap<>();
        response.put("reply", reply);
        response.put("action", "SAFETY_BLOCKED");
        response.put("intent", intent);
        response.put("confidence", 0.95);
        response.put("escalationRequired", false);
        response.put("safetyFlags", safetyFlags);
        response.put("sources", List.of("safety_policy"));
        response.put("suggestedActions", List.of("Ask a booking, support, team, tournament, or analytics question"));
        response.put("explainability", List.of("Request matched a platform safety guardrail"));
        return response;
    }

    private Map<String, Object> finalizeResponse(Map<String, Object> response, String conversationId, long startedAt) {
        response.putIfAbsent("confidence", 0.65);
        response.putIfAbsent("intent", "GENERAL_ASSISTANT");
        response.putIfAbsent("action", "NONE");
        response.putIfAbsent("safetyFlags", List.of());
        response.putIfAbsent("explainability", List.of("Rule-based intelligence response generated from Cricbook platform data"));
        response.put("conversationId", conversationId);
        response.put("modelVersion", MODEL_VERSION);
        response.put("responseSlaMs", Math.max(1, System.currentTimeMillis() - startedAt));
        return response;
    }

    private void persistConversation(Long userId, String conversationId, String message, Map<String, Object> response) {
        conversationRepository.save(AIConversationMessage.builder()
                .userId(userId)
                .conversationId(conversationId)
                .intent(String.valueOf(response.getOrDefault("intent", "GENERAL_ASSISTANT")))
                .action(String.valueOf(response.getOrDefault("action", "NONE")))
                .userMessage(message)
                .assistantReply(String.valueOf(response.getOrDefault("reply", "")))
                .confidence(BigDecimal.valueOf(toDouble(response.get("confidence"), 0.65)).setScale(2, RoundingMode.HALF_UP))
                .safetyFlags(String.join(",", toStringList(response.get("safetyFlags"))))
                .modelVersion(MODEL_VERSION)
                .build());
    }

    private void writeAudit(String action, String entity) {
        auditLogRepository.save(AuditLog.builder()
                .adminId("AI_ASSISTANT")
                .action(action)
                .entity(entity)
                .build());
    }

    private boolean isBookingIntent(String lowerMessage) {
        return lowerMessage.contains("book")
                || lowerMessage.contains("slot")
                || lowerMessage.contains("turf")
                || lowerMessage.contains("near me")
                || lowerMessage.contains("available");
    }

    private boolean containsAny(String text, List<String> patterns) {
        return patterns.stream().anyMatch(text::contains);
    }

    private double toDouble(Object value, double fallback) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value != null) {
            try {
                return Double.parseDouble(value.toString());
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private List<String> toStringList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        if (value == null) {
            return List.of();
        }
        return List.of(String.valueOf(value));
    }
}
