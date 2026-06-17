package com.example.Cricbook.repository;

import com.example.Cricbook.entity.MediaAsset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MediaAssetRepository extends JpaRepository<MediaAsset, Long> {
    Optional<MediaAsset> findFirstByOwnerTypeAndOwnerIdAndMediaTypeAndActiveTrueOrderByUploadedAtDesc(String ownerType, Long ownerId, String mediaType);
    List<MediaAsset> findByModerationStatusOrderByUploadedAtDesc(String moderationStatus);
}
