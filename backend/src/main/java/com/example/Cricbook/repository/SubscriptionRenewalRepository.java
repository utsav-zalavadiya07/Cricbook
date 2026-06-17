package com.example.Cricbook.repository;

import com.example.Cricbook.entity.SubscriptionRenewal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubscriptionRenewalRepository extends JpaRepository<SubscriptionRenewal, Long> {

    List<SubscriptionRenewal> findBySubscriptionIdOrderByAttemptedAtDesc(Long subscriptionId);
}
