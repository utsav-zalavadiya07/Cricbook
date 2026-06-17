package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "score_events", indexes = {
        @Index(name = "idx_score_event_match", columnList = "live_score_id"),
        @Index(name = "idx_score_event_sequence", columnList = "event_sequence")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScoreEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "live_score_id", nullable = false)
    private LiveMatchScore liveScore;

    @Column(name = "event_sequence", nullable = false)
    private Integer eventSequence;

    @Column(nullable = false)
    private Integer runs;

    @Column(name = "extra_runs", nullable = false)
    private Integer extraRuns;

    @Column(name = "extra_type")
    private String extraType;

    @Column(name = "is_wicket", nullable = false)
    private Boolean wicket;

    @Column(name = "wicket_type")
    private String wicketType;

    private String batter;

    private String bowler;

    @Column(columnDefinition = "TEXT")
    private String note;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
