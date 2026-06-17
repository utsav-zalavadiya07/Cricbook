package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "federation_draft_selections", indexes = {
        @Index(name = "idx_federation_draft_selection_draft", columnList = "draftId"),
        @Index(name = "idx_federation_draft_selection_player", columnList = "playerRegistrationId")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_federation_draft_player", columnNames = {"draftId", "playerRegistrationId"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FederationDraftSelection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long draftId;

    @Column(nullable = false)
    private Long franchiseId;

    @Column(nullable = false)
    private Long playerRegistrationId;

    @Column(nullable = false)
    private Integer pickNumber;

    @Column(nullable = false, length = 40)
    @Builder.Default
    private String selectionType = "MANUAL";

    private String selectedBy;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime selectedAt;
}
