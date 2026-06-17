package com.example.Cricbook.controller;

import com.example.Cricbook.dto.CreateTurfRequest;
import com.example.Cricbook.dto.TurfResponse;
import com.example.Cricbook.service.TurfService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/turfs")
@RequiredArgsConstructor
public class TurfController {

    private final TurfService turfService;

    @PostMapping(consumes = {"multipart/form-data"})
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<TurfResponse> createTurf(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestPart("data") @Valid CreateTurfRequest request,
            @RequestPart(value = "images", required = false) java.util.List<org.springframework.web.multipart.MultipartFile> images) {
        String email = userDetails.getUsername();
        TurfResponse response = turfService.createTurf(email, request, images);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<TurfResponse> updateTurf(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestPart("data") @Valid CreateTurfRequest request,
            @RequestPart(value = "images", required = false) java.util.List<org.springframework.web.multipart.MultipartFile> images) {
        String email = userDetails.getUsername();
        TurfResponse response = turfService.updateTurf(id, email, request, images);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TurfResponse> getTurf(@PathVariable Long id) {
        TurfResponse response = turfService.getTurf(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<List<TurfResponse>> getMyTurfs(
            @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        return ResponseEntity.ok(turfService.getTurfsByOwner(email));
    }

    @GetMapping("/amenities")
    public ResponseEntity<List<String>> getAmenityCatalog() {
        return ResponseEntity.ok(turfService.getAmenityCatalog());
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<Void> activateTurf(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        turfService.updateStatus(id, email, "ACTIVE");
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<Void> deactivateTurf(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        turfService.updateStatus(id, email, "INACTIVE");
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/maintenance")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<Void> maintenanceTurf(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        turfService.updateStatus(id, userDetails.getUsername(), "MAINTENANCE");
        return ResponseEntity.ok().build();
    }
}
