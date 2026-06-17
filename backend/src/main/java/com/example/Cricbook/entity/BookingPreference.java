package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "booking_preferences", uniqueConstraints = {
        @UniqueConstraint(name = "uk_booking_preferences_user", columnNames = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingPreference {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_day")
    private DayOfWeek preferredDay;

    @Column(name = "preferred_time")
    private LocalTime preferredTime;

    @Column(name = "preferred_duration_minutes")
    private Integer preferredDurationMinutes;

    @Column(name = "preferred_city")
    private String preferredCity;

    @Column(name = "preferred_team_id")
    private Long preferredTeamId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
