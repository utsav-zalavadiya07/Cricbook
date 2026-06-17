package com.example.Cricbook.entity;

import com.example.Cricbook.enums.PlayerConnectionStatus;
import com.example.Cricbook.enums.PlayerConnectionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "player_connections", uniqueConstraints = {
        @UniqueConstraint(name = "uk_player_connection_pair", columnNames = {"from_user_id", "to_user_id", "type"})
}, indexes = {
        @Index(name = "idx_player_connection_from", columnList = "from_user_id, status"),
        @Index(name = "idx_player_connection_to", columnList = "to_user_id, status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerConnection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_user_id", nullable = false)
    private User fromUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_user_id", nullable = false)
    private User toUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlayerConnectionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlayerConnectionStatus status;

    @Column(length = 500)
    private String message;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
