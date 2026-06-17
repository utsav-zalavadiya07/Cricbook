package com.example.Cricbook.entity;

import com.example.Cricbook.enums.TeamMemberRole;
import com.example.Cricbook.enums.TeamMemberStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "team_members", uniqueConstraints = {
        @UniqueConstraint(name = "unique_team_member", columnNames = {"team_id", "user_id"})
}, indexes = {
        @Index(name = "idx_team_member_user", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TeamMemberRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TeamMemberStatus status;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
