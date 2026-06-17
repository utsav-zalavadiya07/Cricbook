package com.example.Cricbook.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.Cricbook.entity.*;
import com.example.Cricbook.enums.NotificationChannel;
import com.example.Cricbook.repository.NotificationEventRepository;
import com.example.Cricbook.repository.NotificationTemplateRepository;
import com.example.Cricbook.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventBusService {

    private final NotificationEventRepository eventRepository;
    private final NotificationTemplateRepository templateRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final TemplateService templateService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public void publishEvent(String eventName, Map<String, String> payload) {
        log.info("Published event to Bus: {} with payload size: {}", eventName, payload.size());
        
        try {
            // 1. Log in Database
            String jsonPayload = objectMapper.writeValueAsString(payload);
            eventRepository.save(NotificationEvent.builder()
                    .eventName(eventName)
                    .payload(jsonPayload)
                    .build());

            // Seed sample templates if not already present
            seedSampleTemplates();

            // 2. Resolve handlers based on Event types
            if ("BOOKING_CREATED".equalsIgnoreCase(eventName)) {
                handleBookingCreated(payload);
            } else if ("PAYMENT_SUCCESS".equalsIgnoreCase(eventName)) {
                handlePaymentSuccess(payload);
            } else if ("REFUND_COMPLETED".equalsIgnoreCase(eventName)) {
                handleRefundCompleted(payload);
            } else if ("TEAM_INVITE".equalsIgnoreCase(eventName)) {
                handleTeamInvite(payload);
            } else if ("MATCH_STARTED".equalsIgnoreCase(eventName)) {
                handleMatchStarted(payload);
            } else if ("TOURNAMENT_UPDATE".equalsIgnoreCase(eventName)) {
                handleTournamentUpdate(payload);
            } else if ("REVIEW_RECEIVED".equalsIgnoreCase(eventName)) {
                handleReviewReceived(payload);
            } else if ("SOCIAL_MENTION".equalsIgnoreCase(eventName)) {
                handleSocialMention(payload);
            } else {
                log.info("No built-in workflow matched for custom event '{}'", eventName);
            }
        } catch (Exception e) {
            log.error("Failed handling published event: {}", eventName, e);
        }
    }

    private void handleBookingCreated(Map<String, String> payload) {
        String email = payload.get("email");
        String name = payload.getOrDefault("name", "User");
        String bookingId = payload.getOrDefault("bookingId", "N/A");
        String date = payload.getOrDefault("date", "Today");

        User user = findUser(email, payload.get("userId"));
        if (user == null) return;

        Map<String, String> vars = new HashMap<>();
        vars.put("name", name);
        vars.put("booking", "Booking #" + bookingId);
        vars.put("date", date);

        // Send Email Confirmation
        String emailBody = templateService.renderTemplate("booking_confirmation_email", vars);
        notificationService.pushNotification(
                user.getId(),
                user.getEmail(),
                "Booking Confirmed!",
                emailBody,
                NotificationChannel.EMAIL,
                null,
                "/bookings",
                "BOOKING",
                "HIGH"
        );

        // Send WhatsApp Confirmation
        String waBody = templateService.renderTemplate("booking_confirmation_whatsapp", vars);
        notificationService.pushNotification(
                user.getId(),
                user.getEmail(),
                "Cricbook Alerts",
                waBody,
                NotificationChannel.WHATSAPP,
                null,
                "/bookings",
                "BOOKING",
                "HIGH"
        );
    }

    private void handlePaymentSuccess(Map<String, String> payload) {
        String email = payload.get("email");
        String name = payload.getOrDefault("name", "User");
        String amount = payload.getOrDefault("amount", "0.00");
        String txId = payload.getOrDefault("transactionId", "TXN100");

        User user = findUser(email, payload.get("userId"));
        if (user == null) return;

        Map<String, String> vars = new HashMap<>();
        vars.put("name", name);
        vars.put("amount", amount);
        vars.put("txId", txId);

        String emailBody = templateService.renderTemplate("payment_invoice_email", vars);
        notificationService.pushNotification(
                user.getId(),
                user.getEmail(),
                "Payment Receipt - Cricbook",
                emailBody,
                NotificationChannel.EMAIL,
                null,
                "/wallet",
                "PAYMENT",
                "HIGH"
        );
    }

    private void handleRefundCompleted(Map<String, String> payload) {
        User user = findUser(payload.get("email"), payload.get("userId"));
        if (user == null) return;

        String amount = payload.getOrDefault("amount", "0.00");
        String refundId = payload.getOrDefault("refundId", "N/A");
        notificationService.pushNotification(
                user.getId(),
                user.getEmail(),
                "Refund processed",
                "Your refund of Rs. " + amount + " has been processed. Refund reference: " + refundId + ".",
                NotificationChannel.PUSH,
                null,
                "/wallet",
                "PAYMENT",
                "HIGH"
        );
    }

    private void handleTeamInvite(Map<String, String> payload) {
        User user = findUser(payload.get("email"), payload.get("userId"));
        if (user == null) return;

        String teamName = payload.getOrDefault("teamName", "a team");
        notificationService.pushNotification(
                user.getId(),
                user.getEmail(),
                "Team invite received",
                "You have been invited to join " + teamName + ".",
                NotificationChannel.PUSH,
                null,
                "/teams",
                "TEAM",
                "NORMAL"
        );
    }

    private void handleMatchStarted(Map<String, String> payload) {
        // Broadcast push notification to all players
        String matchId = payload.getOrDefault("matchId", "N/A");
        String teamA = payload.getOrDefault("teamA", "Team A");
        String teamB = payload.getOrDefault("teamB", "Team B");

        log.info("Broadcasting live match start notification for match: {}", matchId);
        List<User> players = userRepository.findAll();

        Map<String, String> vars = new HashMap<>();
        vars.put("teamA", teamA);
        vars.put("teamB", teamB);

        String pushBody = templateService.renderTemplate("match_started_push", vars);

        for (User u : players) {
            notificationService.pushNotification(
                    u.getId(),
                    u.getEmail(),
                    "Match Started Live! 🏏",
                    pushBody,
                    NotificationChannel.PUSH,
                    null,
                    "/live-scoring",
                    "MATCH",
                    "HIGH"
            );
        }
    }

    private void handleTournamentUpdate(Map<String, String> payload) {
        String title = payload.getOrDefault("title", "Tournament update");
        String message = payload.getOrDefault("message", "A tournament update is available.");
        String tournamentId = payload.getOrDefault("tournamentId", "");
        String deepLink = tournamentId.isBlank() ? "/tournaments" : "/tournaments/" + tournamentId;

        for (User user : userRepository.findAll()) {
            notificationService.pushNotification(
                    user.getId(),
                    user.getEmail(),
                    title,
                    message,
                    NotificationChannel.PUSH,
                    null,
                    deepLink,
                    "TOURNAMENT",
                    "NORMAL"
            );
        }
    }

    private void handleReviewReceived(Map<String, String> payload) {
        String ownerId = payload.get("ownerId");
        String turfName = payload.getOrDefault("turfName", "Your Turf");
        String stars = payload.getOrDefault("stars", "5");

        if (ownerId == null) return;
        User owner = userRepository.findById(Long.parseLong(ownerId)).orElse(null);
        if (owner == null) return;

        Map<String, String> vars = new HashMap<>();
        vars.put("turfName", turfName);
        vars.put("stars", stars);

        String waBody = templateService.renderTemplate("owner_review_alert_whatsapp", vars);
        notificationService.pushNotification(
                owner.getId(),
                owner.getEmail(),
                "New Review Received",
                waBody,
                NotificationChannel.WHATSAPP,
                null,
                "/owner/reviews",
                "REVIEW",
                "NORMAL"
        );
    }

    private void handleSocialMention(Map<String, String> payload) {
        User user = findUser(payload.get("email"), payload.get("userId"));
        if (user == null) return;

        String actor = payload.getOrDefault("actor", "A player");
        notificationService.pushNotification(
                user.getId(),
                user.getEmail(),
                "You were mentioned",
                actor + " mentioned you in the community feed.",
                NotificationChannel.PUSH,
                null,
                "/feed",
                "SOCIAL",
                "NORMAL"
        );
    }

    private User findUser(String email, String userIdStr) {
        if (userIdStr != null) {
            return userRepository.findById(Long.parseLong(userIdStr)).orElse(null);
        }
        if (email != null) {
            return userRepository.findByEmail(email).orElse(null);
        }
        return null;
    }

    private void seedSampleTemplates() {
        if (templateRepository.findByName("booking_confirmation_email").isEmpty()) {
            templateRepository.save(NotificationTemplate.builder()
                    .name("booking_confirmation_email")
                    .subject("Your Cricbook Booking Confirmation")
                    .body("Hello {{name}}, your booking for {{booking}} on {{date}} has been confirmed successfully! See you on the turf!")
                    .channel(NotificationChannel.EMAIL)
                    .build());
        }
        if (templateRepository.findByName("booking_confirmation_whatsapp").isEmpty()) {
            templateRepository.save(NotificationTemplate.builder()
                    .name("booking_confirmation_whatsapp")
                    .body("Hey {{name}}! 🏏 Your spot for {{booking}} on {{date}} is confirmed. View details at Cricbook.")
                    .channel(NotificationChannel.WHATSAPP)
                    .build());
        }
        if (templateRepository.findByName("payment_invoice_email").isEmpty()) {
            templateRepository.save(NotificationTemplate.builder()
                    .name("payment_invoice_email")
                    .subject("Payment Receipt from Cricbook")
                    .body("Hello {{name}}, we have received your payment of Rs. {{amount}}. Your transaction ID is {{txId}}.")
                    .channel(NotificationChannel.EMAIL)
                    .build());
        }
        if (templateRepository.findByName("match_started_push").isEmpty()) {
            templateRepository.save(NotificationTemplate.builder()
                    .name("match_started_push")
                    .body("The big match between {{teamA}} and {{teamB}} has just started! Click to view live scoring!")
                    .channel(NotificationChannel.PUSH)
                    .build());
        }
        if (templateRepository.findByName("owner_review_alert_whatsapp").isEmpty()) {
            templateRepository.save(NotificationTemplate.builder()
                    .name("owner_review_alert_whatsapp")
                    .body("Hi Partner! A new {{stars}}-star review has been published for {{turfName}}. Log in to reply.")
                    .channel(NotificationChannel.WHATSAPP)
                    .build());
        }
    }
}
