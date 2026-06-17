package com.example.Cricbook.repository;

import com.example.Cricbook.entity.TenantSaasSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TenantSaasSubscriptionRepository extends JpaRepository<TenantSaasSubscription, Long> {
    List<TenantSaasSubscription> findAllByOrderByCreatedAtDesc();

    List<TenantSaasSubscription> findByTenantIdOrderByCreatedAtDesc(Long tenantId);

    List<TenantSaasSubscription> findByTenantIdAndStatusOrderByEndDateDesc(Long tenantId, String status);
}
