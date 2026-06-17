package com.example.Cricbook.controller;

import com.example.Cricbook.entity.*;
import com.example.Cricbook.repository.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1/admin/support")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('TICKET_VIEW')")
public class AdminSupportController {

    private final SupportTicketRepository supportTicketRepository;
    private final UserRepository userRepository;
    private final SupportChatMessageRepository supportChatMessageRepository;
    private final SlaPolicyRepository slaPolicyRepository;
    private final TicketEscalationRepository ticketEscalationRepository;
    private final SupportAgentRepository supportAgentRepository;
    private final KnowledgeArticleRepository knowledgeArticleRepository;
    private final FaqRepository faqRepository;
    private final CsatFeedbackRepository csatFeedbackRepository;
    private final TicketNoteRepository ticketNoteRepository;
    private final SupportAutomationRepository supportAutomationRepository;
    private final SupportChannelMessageRepository supportChannelMessageRepository;

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard() {
        seedSlaPolicies();
        List<SupportTicket> tickets = supportTicketRepository.findAll();
        long open = tickets.stream().filter(ticket -> !closed(ticket.getStatus())).count();
        long resolved = tickets.stream().filter(ticket -> "RESOLVED".equalsIgnoreCase(ticket.getStatus()) || "CLOSED".equalsIgnoreCase(ticket.getStatus())).count();
        long breaches = tickets.stream().filter(this::slaBreached).count();
        double avgResolutionHours = tickets.stream()
                .filter(ticket -> ticket.getCreatedAt() != null && ticket.getResolvedAt() != null)
                .mapToLong(ticket -> Duration.between(ticket.getCreatedAt(), ticket.getResolvedAt()).toHours())
                .average()
                .orElse(0);
        double avgCsat = csatFeedbackRepository.findAll().stream().map(CsatFeedback::getRating).filter(Objects::nonNull).mapToInt(Integer::intValue).average().orElse(0);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("openTickets", open);
        response.put("resolvedTickets", resolved);
        response.put("averageResponseHours", averageFirstResponseHours(tickets));
        response.put("averageResolutionHours", round(avgResolutionHours));
        response.put("slaBreaches", breaches);
        response.put("liveChats", supportChatMessageRepository.findTop50ByOrderByCreatedAtDesc().size());
        response.put("escalations", ticketEscalationRepository.findByStatusOrderByCreatedAtDesc("OPEN").size());
        response.put("csatScore", round(avgCsat));
        response.put("agentAvailability", supportAgentRepository.findByStatusOrderByCreatedAtDesc("AVAILABLE").size());
        response.put("ticketTrends", ticketTrends(tickets));
        response.put("aiInsights", supportInsights(open, breaches, avgCsat));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/tickets")
    public ResponseEntity<List<Map<String, Object>>> tickets() {
        return ResponseEntity.ok(supportTicketRepository.findAll().stream()
                .sorted(Comparator.comparing(SupportTicket::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::mapTicket)
                .toList());
    }

    @PostMapping("/tickets")
    public ResponseEntity<Map<String, Object>> createTicket(@RequestBody TicketRequest request) {
        Long userId = request.getUserId() != null ? request.getUserId() : userRepository.findAll().stream().findFirst().map(User::getId).orElse(1L);
        SupportTicket ticket = SupportTicket.builder()
                .userId(userId)
                .ticketNumber("SUP-" + System.currentTimeMillis())
                .category(defaultString(request.getCategory(), "TECHNICAL_ISSUE"))
                .priority(defaultString(request.getPriority(), "MEDIUM").toUpperCase(Locale.ROOT))
                .assignedTo(request.getAssignedTo())
                .channel(defaultString(request.getChannel(), "IN_APP").toUpperCase(Locale.ROOT))
                .issue(required(request.getIssue(), "Ticket issue is required"))
                .status("OPEN")
                .chatLog("User: " + request.getIssue())
                .build();
        return ResponseEntity.ok(mapTicket(supportTicketRepository.save(ticket)));
    }

    @PostMapping("/tickets/{id}/assign")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('TICKET_ASSIGN')")
    public ResponseEntity<Map<String, Object>> assignTicket(@PathVariable Long id, @RequestBody AssignRequest request) {
        SupportTicket ticket = supportTicketRepository.findById(id).orElseThrow();
        ticket.setAssignedTo(required(request.getAssignedTo(), "Assigned agent is required"));
        ticket.setStatus("IN_PROGRESS");
        return ResponseEntity.ok(mapTicket(supportTicketRepository.save(ticket)));
    }

    @PostMapping("/tickets/{id}/reply")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('CHAT_ACCESS') or hasAuthority('TICKET_RESOLVE')")
    public ResponseEntity<Map<String, Object>> replyTicket(@PathVariable Long id, @RequestBody ReplyRequest request) {
        SupportTicket ticket = supportTicketRepository.findById(id).orElseThrow();
        String reply = required(request.getReply(), "Reply is required");
        ticket.setChatLog(defaultString(ticket.getChatLog(), "") + "\nSupport (" + actor() + "): " + reply);
        ticket.setStatus(defaultString(request.getStatus(), "IN_PROGRESS").toUpperCase(Locale.ROOT));
        if (ticket.getFirstResponseAt() == null) {
            ticket.setFirstResponseAt(LocalDateTime.now());
        }
        if ("RESOLVED".equalsIgnoreCase(ticket.getStatus()) || "CLOSED".equalsIgnoreCase(ticket.getStatus())) {
            ticket.setResolvedAt(LocalDateTime.now());
        }
        if (request.getInternalNotes() != null && !request.getInternalNotes().isBlank()) {
            ticket.setInternalNotes(request.getInternalNotes());
            ticketNoteRepository.save(TicketNote.builder().ticketId(ticket.getId()).agentId(actor()).note(request.getInternalNotes()).build());
        }
        supportChatMessageRepository.save(SupportChatMessage.builder()
                .ticketId(ticket.getId())
                .senderType("AGENT")
                .message(reply)
                .build());
        return ResponseEntity.ok(mapTicket(supportTicketRepository.save(ticket)));
    }

    @GetMapping("/chat")
    public ResponseEntity<List<SupportChatMessage>> chat() {
        return ResponseEntity.ok(supportChatMessageRepository.findTop50ByOrderByCreatedAtDesc());
    }

    @PostMapping("/chat")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('CHAT_ACCESS')")
    public ResponseEntity<SupportChatMessage> sendChat(@RequestBody ChatRequest request) {
        SupportChatMessage message = supportChatMessageRepository.save(SupportChatMessage.builder()
                .ticketId(request.getTicketId())
                .senderId(request.getSenderId())
                .senderType(defaultString(request.getSenderType(), "AGENT"))
                .message(required(request.getMessage(), "Message is required"))
                .build());
        return ResponseEntity.ok(message);
    }

    @GetMapping("/sla")
    public ResponseEntity<List<SlaPolicy>> slaPolicies() {
        seedSlaPolicies();
        return ResponseEntity.ok(slaPolicyRepository.findAll());
    }

    @PostMapping("/sla")
    public ResponseEntity<SlaPolicy> saveSla(@RequestBody SlaPolicy request) {
        SlaPolicy policy = slaPolicyRepository.findByPriority(request.getPriority()).orElseGet(SlaPolicy::new);
        policy.setPriority(required(request.getPriority(), "Priority is required").toUpperCase(Locale.ROOT));
        policy.setResponseTimeHours(request.getResponseTimeHours() == null ? 24 : request.getResponseTimeHours());
        policy.setResolutionTimeHours(request.getResolutionTimeHours() == null ? 48 : request.getResolutionTimeHours());
        return ResponseEntity.ok(slaPolicyRepository.save(policy));
    }

    @GetMapping("/escalations")
    public ResponseEntity<List<TicketEscalation>> escalations() {
        return ResponseEntity.ok(ticketEscalationRepository.findTop50ByOrderByCreatedAtDesc());
    }

    @PostMapping("/escalations")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('TICKET_ASSIGN')")
    public ResponseEntity<TicketEscalation> escalate(@RequestBody EscalationRequest request) {
        TicketEscalation escalation = ticketEscalationRepository.save(TicketEscalation.builder()
                .ticketId(request.getTicketId())
                .level(defaultString(request.getLevel(), "L2_SUPPORT"))
                .assignedTo(defaultString(request.getAssignedTo(), "support-lead"))
                .status("OPEN")
                .reason(defaultString(request.getReason(), "Escalated for specialist review"))
                .build());
        supportTicketRepository.findById(request.getTicketId()).ifPresent(ticket -> {
            ticket.setStatus("IN_PROGRESS");
            ticket.setAssignedTo(escalation.getAssignedTo());
            supportTicketRepository.save(ticket);
        });
        return ResponseEntity.ok(escalation);
    }

    @GetMapping("/agents")
    public ResponseEntity<List<SupportAgent>> agents() {
        return ResponseEntity.ok(supportAgentRepository.findAllByOrderByCreatedAtDesc());
    }

    @PostMapping("/agents")
    public ResponseEntity<SupportAgent> saveAgent(@RequestBody SupportAgent request) {
        SupportAgent agent = request.getId() == null ? new SupportAgent() : supportAgentRepository.findById(request.getId()).orElseGet(SupportAgent::new);
        agent.setUserId(request.getUserId());
        agent.setName(required(request.getName(), "Agent name is required"));
        agent.setRole(defaultString(request.getRole(), "SUPPORT_AGENT"));
        agent.setStatus(defaultString(request.getStatus(), "AVAILABLE"));
        return ResponseEntity.ok(supportAgentRepository.save(agent));
    }

    @GetMapping("/knowledge-base")
    public ResponseEntity<List<KnowledgeArticle>> knowledgeBase() {
        return ResponseEntity.ok(knowledgeArticleRepository.findAllByOrderByUpdatedAtDesc());
    }

    @PostMapping("/knowledge-base")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('KB_EDIT')")
    public ResponseEntity<KnowledgeArticle> saveArticle(@RequestBody KnowledgeArticle request) {
        KnowledgeArticle article = request.getId() == null ? new KnowledgeArticle() : knowledgeArticleRepository.findById(request.getId()).orElseGet(KnowledgeArticle::new);
        article.setTitle(required(request.getTitle(), "Article title is required"));
        article.setContent(required(request.getContent(), "Article content is required"));
        article.setCategory(defaultString(request.getCategory(), "GENERAL"));
        article.setStatus(defaultString(request.getStatus(), "DRAFT"));
        return ResponseEntity.ok(knowledgeArticleRepository.save(article));
    }

    @GetMapping("/faqs")
    public ResponseEntity<List<Faq>> faqs() {
        return ResponseEntity.ok(faqRepository.findAllByOrderByUpdatedAtDesc());
    }

    @PostMapping("/faqs")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('FAQ_EDIT')")
    public ResponseEntity<Faq> saveFaq(@RequestBody Faq request) {
        Faq faq = request.getId() == null ? new Faq() : faqRepository.findById(request.getId()).orElseGet(Faq::new);
        faq.setQuestion(required(request.getQuestion(), "FAQ question is required"));
        faq.setAnswer(required(request.getAnswer(), "FAQ answer is required"));
        faq.setCategory(defaultString(request.getCategory(), "GENERAL"));
        faq.setStatus(defaultString(request.getStatus(), "PUBLISHED"));
        return ResponseEntity.ok(faqRepository.save(faq));
    }

    @GetMapping("/csat")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('CSAT_VIEW')")
    public ResponseEntity<List<CsatFeedback>> csat() {
        return ResponseEntity.ok(csatFeedbackRepository.findTop50ByOrderByCreatedAtDesc());
    }

    @PostMapping("/csat")
    public ResponseEntity<CsatFeedback> saveCsat(@RequestBody CsatRequest request) {
        return ResponseEntity.ok(csatFeedbackRepository.save(CsatFeedback.builder()
                .ticketId(request.getTicketId())
                .rating(Math.max(1, Math.min(5, request.getRating() == null ? 5 : request.getRating())))
                .feedback(request.getFeedback())
                .build()));
    }

    @GetMapping("/notes")
    public ResponseEntity<List<TicketNote>> notes() {
        return ResponseEntity.ok(ticketNoteRepository.findTop50ByOrderByCreatedAtDesc());
    }

    @PostMapping("/notes")
    public ResponseEntity<TicketNote> saveNote(@RequestBody NoteRequest request) {
        return ResponseEntity.ok(ticketNoteRepository.save(TicketNote.builder()
                .ticketId(request.getTicketId())
                .agentId(actor())
                .note(required(request.getNote(), "Note is required"))
                .build()));
    }

    @GetMapping("/automations")
    public ResponseEntity<List<SupportAutomation>> automations() {
        return ResponseEntity.ok(supportAutomationRepository.findAllByOrderByCreatedAtDesc());
    }

    @PostMapping("/automations")
    public ResponseEntity<SupportAutomation> saveAutomation(@RequestBody SupportAutomation request) {
        SupportAutomation automation = request.getId() == null ? new SupportAutomation() : supportAutomationRepository.findById(request.getId()).orElseGet(SupportAutomation::new);
        automation.setRuleName(required(request.getRuleName(), "Rule name is required"));
        automation.setCondition(required(request.getCondition(), "Condition is required"));
        automation.setAction(required(request.getAction(), "Action is required"));
        automation.setActive(request.getActive() == null || request.getActive());
        return ResponseEntity.ok(supportAutomationRepository.save(automation));
    }

    @GetMapping("/channels")
    public ResponseEntity<List<SupportChannelMessage>> channelMessages() {
        return ResponseEntity.ok(supportChannelMessageRepository.findTop50ByOrderByCreatedAtDesc());
    }

    @PostMapping("/channels")
    public ResponseEntity<SupportChannelMessage> saveChannelMessage(@RequestBody ChannelRequest request) {
        return ResponseEntity.ok(supportChannelMessageRepository.save(SupportChannelMessage.builder()
                .channel(defaultString(request.getChannel(), "IN_APP"))
                .externalId(request.getExternalId())
                .ticketId(request.getTicketId())
                .message(required(request.getMessage(), "Channel message is required"))
                .build()));
    }

    @GetMapping("/analytics")
    public ResponseEntity<Map<String, Object>> analytics() {
        return dashboard();
    }

    private Map<String, Object> mapTicket(SupportTicket ticket) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", ticket.getId());
        item.put("ticketNumber", defaultString(ticket.getTicketNumber(), "SUP-" + ticket.getId()));
        item.put("userId", ticket.getUserId());
        item.put("category", defaultString(ticket.getCategory(), "TECHNICAL_ISSUE"));
        item.put("priority", defaultString(ticket.getPriority(), "MEDIUM"));
        item.put("status", ticket.getStatus());
        item.put("assignedTo", ticket.getAssignedTo());
        item.put("channel", defaultString(ticket.getChannel(), "IN_APP"));
        item.put("issue", ticket.getIssue());
        item.put("internalNotes", ticket.getInternalNotes());
        item.put("chatLog", ticket.getChatLog());
        item.put("slaBreached", slaBreached(ticket));
        item.put("createdAt", ticket.getCreatedAt());
        item.put("updatedAt", ticket.getUpdatedAt());
        return item;
    }

    private void seedSlaPolicies() {
        seedSla("CRITICAL", 1, 4);
        seedSla("HIGH", 4, 12);
        seedSla("MEDIUM", 24, 48);
        seedSla("LOW", 48, 96);
    }

    private void seedSla(String priority, int responseHours, int resolutionHours) {
        if (slaPolicyRepository.findByPriority(priority).isEmpty()) {
            slaPolicyRepository.save(SlaPolicy.builder().priority(priority).responseTimeHours(responseHours).resolutionTimeHours(resolutionHours).build());
        }
    }

    private boolean slaBreached(SupportTicket ticket) {
        if (ticket.getCreatedAt() == null || closed(ticket.getStatus())) {
            return false;
        }
        String priority = defaultString(ticket.getPriority(), "MEDIUM").toUpperCase(Locale.ROOT);
        int hours = slaPolicyRepository.findByPriority(priority).map(SlaPolicy::getResponseTimeHours).orElse(24);
        return ticket.getFirstResponseAt() == null && ticket.getCreatedAt().plusHours(hours).isBefore(LocalDateTime.now());
    }

    private boolean closed(String status) {
        return "RESOLVED".equalsIgnoreCase(status) || "CLOSED".equalsIgnoreCase(status);
    }

    private double averageFirstResponseHours(List<SupportTicket> tickets) {
        return round(tickets.stream()
                .filter(ticket -> ticket.getCreatedAt() != null && ticket.getFirstResponseAt() != null)
                .mapToLong(ticket -> Duration.between(ticket.getCreatedAt(), ticket.getFirstResponseAt()).toHours())
                .average()
                .orElse(0));
    }

    private List<Map<String, Object>> ticketTrends(List<SupportTicket> tickets) {
        Map<String, Long> buckets = new TreeMap<>();
        for (int index = 13; index >= 0; index--) {
            buckets.put(LocalDateTime.now().minusDays(index).toLocalDate().toString(), 0L);
        }
        tickets.stream().filter(ticket -> ticket.getCreatedAt() != null).forEach(ticket ->
                buckets.computeIfPresent(ticket.getCreatedAt().toLocalDate().toString(), (day, count) -> count + 1));
        return buckets.entrySet().stream().map(entry -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", entry.getKey());
            item.put("tickets", entry.getValue());
            return item;
        }).toList();
    }

    private List<Map<String, Object>> supportInsights(long open, long breaches, double csat) {
        List<Map<String, Object>> insights = new ArrayList<>();
        if (breaches > 0) {
            insights.add(insight("SLA breaches need action", "Some tickets missed first-response SLA. Reassign or escalate them.", "HIGH"));
        }
        if (open > 20) {
            insights.add(insight("Ticket backlog rising", "Open ticket volume is high. Add agents or enable automation.", "MEDIUM"));
        }
        if (csat > 0 && csat < 3.5) {
            insights.add(insight("CSAT below target", "Customer satisfaction is below 3.5. Review resolved ticket quality.", "HIGH"));
        }
        if (insights.isEmpty()) {
            insights.add(insight("Support operations stable", "Backlog, SLA, and CSAT are within current thresholds.", "LOW"));
        }
        return insights;
    }

    private Map<String, Object> insight(String title, String message, String severity) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("title", title);
        item.put("message", message);
        item.put("severity", severity);
        return item;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private String actor() {
        return SecurityContextHolder.getContext().getAuthentication() == null ? "support-admin" : SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private String required(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    @Data
    public static class TicketRequest {
        private Long userId;
        private String category;
        private String priority;
        private String assignedTo;
        private String channel;
        private String issue;
    }

    @Data
    public static class AssignRequest {
        private String assignedTo;
    }

    @Data
    public static class ReplyRequest {
        private String reply;
        private String status;
        private String internalNotes;
    }

    @Data
    public static class ChatRequest {
        private Long ticketId;
        private Long senderId;
        private String senderType;
        private String message;
    }

    @Data
    public static class EscalationRequest {
        private Long ticketId;
        private String level;
        private String assignedTo;
        private String reason;
    }

    @Data
    public static class CsatRequest {
        private Long ticketId;
        private Integer rating;
        private String feedback;
    }

    @Data
    public static class NoteRequest {
        private Long ticketId;
        private String note;
    }

    @Data
    public static class ChannelRequest {
        private String channel;
        private String externalId;
        private Long ticketId;
        private String message;
    }
}
