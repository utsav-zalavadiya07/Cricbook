package com.example.Cricbook.repository;

import com.example.Cricbook.entity.SeoSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SeoSettingRepository extends JpaRepository<SeoSetting, Long> {
    Optional<SeoSetting> findByPageSlug(String pageSlug);
    List<SeoSetting> findAllByOrderByUpdatedAtDesc();
}
