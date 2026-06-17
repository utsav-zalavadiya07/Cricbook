package com.example.Cricbook.controller;

import com.example.Cricbook.dto.TurfSearchRequest;
import com.example.Cricbook.dto.TurfSearchResponse;
import com.example.Cricbook.service.TurfSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class TurfSearchController {

    private final TurfSearchService service;

    @GetMapping
    public ResponseEntity<List<TurfSearchResponse>> search(
            @org.springframework.web.bind.annotation.ModelAttribute TurfSearchRequest request
    ) {
        return ResponseEntity.ok(service.search(request));
    }

    @GetMapping("/suggestions")
    public ResponseEntity<List<String>> suggestions(
            @RequestParam String query,
            @RequestParam(required = false) Integer limit
    ) {
        return ResponseEntity.ok(service.suggestions(query, limit));
    }
}
