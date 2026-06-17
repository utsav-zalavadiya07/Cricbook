package com.example.Cricbook.repository;

import com.example.Cricbook.entity.FamilyAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FamilyAccountRepository extends JpaRepository<FamilyAccount, Long> {
    boolean existsByParentIdentityIdAndChildIdentityId(Long parentIdentityId, Long childIdentityId);

    List<FamilyAccount> findByParentIdentityIdOrderByCreatedAtDesc(Long parentIdentityId);

    List<FamilyAccount> findAllByOrderByCreatedAtDesc();
}
