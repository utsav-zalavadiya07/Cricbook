package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "player_settings", uniqueConstraints = {
        @UniqueConstraint(name = "uk_player_settings_user", columnNames = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerSettings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Builder.Default
    @Column(name = "profile_visibility", nullable = false)
    private String profileVisibility = "PUBLIC";

    @Builder.Default
    @Column(name = "show_trust_score", nullable = false)
    private Boolean showTrustScore = true;

    @Builder.Default
    @Column(name = "show_stats", nullable = false)
    private Boolean showStats = true;

    @Builder.Default
    @Column(name = "show_availability", nullable = false)
    private Boolean showAvailability = true;

    @Builder.Default
    @Column(name = "show_location", nullable = false)
    private Boolean showLocation = false;

    @Builder.Default
    @Column(name = "allow_team_invites", nullable = false)
    private Boolean allowTeamInvites = true;

    @Builder.Default
    @Column(name = "allow_friend_requests", nullable = false)
    private Boolean allowFriendRequests = true;

    @Builder.Default
    @Column(name = "allow_messages", nullable = false)
    private Boolean allowMessages = true;

    @Builder.Default
    @Column(name = "trusted_only_matchmaking", nullable = false)
    private Boolean trustedOnlyMatchmaking = false;

    @Builder.Default
    @Column(name = "nearby_only_matchmaking", nullable = false)
    private Boolean nearbyOnlyMatchmaking = true;

    @Builder.Default
    @Column(name = "competition_level", nullable = false)
    private String competitionLevel = "CASUAL";

    @Builder.Default
    @Column(name = "preferred_skill_level", nullable = false)
    private String preferredSkillLevel = "ANY";

    @Builder.Default
    @Column(name = "auto_waitlist_enabled", nullable = false)
    private Boolean autoWaitlistEnabled = false;

    @Builder.Default
    @Column(name = "quick_rebook_enabled", nullable = false)
    private Boolean quickRebookEnabled = true;

    @Builder.Default
    @Column(name = "auto_pay_splits", nullable = false)
    private Boolean autoPaySplits = false;

    @Column(name = "default_payment_method")
    private String defaultPaymentMethod;

    @Column(name = "saved_upi_id")
    private String savedUpiId;

    @Builder.Default
    @Column(name = "currency", nullable = false)
    private String currency = "INR";

    @Builder.Default
    @Column(name = "timezone", nullable = false)
    private String timezone = "Asia/Kolkata";

    @Builder.Default
    @Column(name = "region_city", nullable = false)
    private String regionCity = "Ahmedabad";

    @Builder.Default
    @Column(name = "two_factor_enabled", nullable = false)
    private Boolean twoFactorEnabled = false;

    @Builder.Default
    @Column(name = "login_method", nullable = false)
    private String loginMethod = "EMAIL_PHONE_PASSWORD";

    @Builder.Default
    @Column(name = "download_data_requested", nullable = false)
    private Boolean downloadDataRequested = false;

    @Column(name = "last_export_requested_at")
    private LocalDateTime lastExportRequestedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
