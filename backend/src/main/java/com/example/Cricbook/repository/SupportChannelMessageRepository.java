package com.example.Cricbook.repository;

import com.example.Cricbook.entity.SupportChannelMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupportChannelMessageRepository extends JpaRepository<SupportChannelMessage, Long> {
    List<SupportChannelMessage> findTop50ByOrderByCreatedAtDesc();
}
