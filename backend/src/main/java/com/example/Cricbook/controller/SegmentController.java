package com.example.Cricbook.controller;

import com.example.Cricbook.entity.UserSegment;
import com.example.Cricbook.repository.UserSegmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/segments")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'CONTENT_ADMIN') or hasAnyAuthority('CAMPAIGN_CREATE', 'PUSH_SEND', 'EMAIL_SEND')")
public class SegmentController {

    private final UserSegmentRepository segmentRepository;

    @GetMapping
    public ResponseEntity<List<UserSegment>> getAllSegments() {
        return ResponseEntity.ok(segmentRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<UserSegment> createSegment(@RequestBody UserSegment segment) {
        return ResponseEntity.ok(segmentRepository.save(segment));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSegment(@PathVariable Long id) {
        segmentRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
