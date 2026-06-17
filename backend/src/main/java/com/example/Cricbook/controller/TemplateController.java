package com.example.Cricbook.controller;

import com.example.Cricbook.entity.NotificationTemplate;
import com.example.Cricbook.repository.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/templates")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'CONTENT_ADMIN') or hasAnyAuthority('CMS_EDIT', 'EMAIL_SEND', 'PUSH_SEND')")
public class TemplateController {

    private final NotificationTemplateRepository templateRepository;

    @GetMapping
    public ResponseEntity<List<NotificationTemplate>> getAllTemplates() {
        return ResponseEntity.ok(templateRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<NotificationTemplate> createTemplate(@RequestBody NotificationTemplate template) {
        return ResponseEntity.ok(templateRepository.save(template));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable Long id) {
        templateRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
