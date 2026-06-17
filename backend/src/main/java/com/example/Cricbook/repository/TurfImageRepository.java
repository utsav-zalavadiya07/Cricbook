package com.example.Cricbook.repository;

import com.example.Cricbook.entity.TurfImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TurfImageRepository extends JpaRepository<TurfImage, Long> {
}
