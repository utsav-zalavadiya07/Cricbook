package com.example.Cricbook.repository;

import com.example.Cricbook.entity.Subscription;
import com.example.Cricbook.enums.MembershipPlanType;
import com.example.Cricbook.enums.MembershipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    List<Subscription> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT s FROM Subscription s WHERE s.user.id = :userId AND s.status = :status AND s.endDate > :now ORDER BY s.endDate DESC")
    List<Subscription> findActiveSubscriptions(
            @Param("userId") Long userId,
            @Param("status") MembershipStatus status,
            @Param("now") LocalDateTime now
    );

    @Query("""
            SELECT s
            FROM Subscription s
            JOIN FETCH s.plan p
            LEFT JOIN FETCH s.turf t
            WHERE s.user.id = :userId AND s.status = :status AND s.endDate > :now
            ORDER BY s.endDate DESC
            """)
    List<Subscription> findDetailedActiveSubscriptions(
            @Param("userId") Long userId,
            @Param("status") MembershipStatus status,
            @Param("now") LocalDateTime now
    );

    @Query("""
            SELECT s
            FROM Subscription s
            JOIN FETCH s.plan p
            LEFT JOIN FETCH s.turf t
            WHERE s.user.id = :userId
              AND s.status = :status
              AND s.endDate > :now
              AND p.type = :type
            ORDER BY s.endDate DESC
            """)
    List<Subscription> findDetailedActiveSubscriptionsByType(
            @Param("userId") Long userId,
            @Param("status") MembershipStatus status,
            @Param("now") LocalDateTime now,
            @Param("type") MembershipPlanType type
    );

    default Optional<Subscription> findFirstActiveSubscription(Long userId, LocalDateTime now) {
        return findActiveSubscriptions(userId, MembershipStatus.ACTIVE, now).stream().findFirst();
    }

    default Optional<Subscription> findFirstDetailedActiveSubscription(Long userId, LocalDateTime now) {
        return findDetailedActiveSubscriptions(userId, MembershipStatus.ACTIVE, now).stream().findFirst();
    }

    default Optional<Subscription> findFirstDetailedActiveSubscriptionByType(Long userId, LocalDateTime now, MembershipPlanType type) {
        return findDetailedActiveSubscriptionsByType(userId, MembershipStatus.ACTIVE, now, type).stream().findFirst();
    }

    List<Subscription> findByStatusAndEndDateBefore(MembershipStatus status, LocalDateTime now);
}
