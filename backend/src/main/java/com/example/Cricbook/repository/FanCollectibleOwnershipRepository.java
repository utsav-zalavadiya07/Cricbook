package com.example.Cricbook.repository;

import com.example.Cricbook.entity.FanCollectibleOwnership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FanCollectibleOwnershipRepository extends JpaRepository<FanCollectibleOwnership, Long> {
    boolean existsByFanProfileIdAndCollectibleIdAndOwnershipStatus(Long fanProfileId, Long collectibleId, String ownershipStatus);

    List<FanCollectibleOwnership> findByFanProfileIdOrderByAcquiredAtDesc(Long fanProfileId);

    List<FanCollectibleOwnership> findAllByOrderByAcquiredAtDesc();
}
