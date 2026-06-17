package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "support_tickets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "ticket_number", unique = true)
    private String ticketNumber;

    @Column(length = 60)
    private String category;

    @Column(length = 40)
    private String priority;

    @Column(name = "assigned_to")
    private String assignedTo;

    @Column(length = 40)
    private String channel;

    @Column(nullable = false)
    private String issue;

    @Column(nullable = false)
    private String status; // OPEN, IN_PROGRESS, RESOLVED

    @Column(name = "internal_notes", columnDefinition = "TEXT")
    private String internalNotes;

    @Column(name = "chat_log", columnDefinition = "TEXT")
    private String chatLog; // JSON or serialized messages: "User: Hello\nSupport: How can I help?"

    @Column(name = "first_response_at")
    private LocalDateTime firstResponseAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
