package com.example.Cricbook.controller;

import com.example.Cricbook.dto.PlatformHealthResponse;
import com.example.Cricbook.dto.PlatformEpicResponse;
import com.example.Cricbook.service.CacheService;
import com.example.Cricbook.service.PlatformEpicService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/platform")
@RequiredArgsConstructor
public class PerformanceController {

    private final CacheService cacheService;
    private final PlatformEpicService platformEpicService;

    @GetMapping("/health")
    public ResponseEntity<PlatformHealthResponse> health() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory() / (1024 * 1024);
        long freeMemory = runtime.freeMemory() / (1024 * 1024);
        return ResponseEntity.ok(PlatformHealthResponse.builder()
                .status("UP")
                .cacheEntries(cacheService.size())
                .usedMemoryMb(totalMemory - freeMemory)
                .totalMemoryMb(totalMemory)
                .availableProcessors(runtime.availableProcessors())
                .build());
    }

    @GetMapping("/epics")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'OWNER', 'USER')")
    public ResponseEntity<java.util.List<PlatformEpicResponse>> epics() {
        return ResponseEntity.ok(platformEpicService.getEpics());
    }

    @GetMapping("/epics/current")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'OWNER', 'USER')")
    public ResponseEntity<PlatformEpicResponse> currentEpic() {
        return ResponseEntity.ok(platformEpicService.getCurrentEpic());
    }

    @DeleteMapping("/cache")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> clearCache() {
        cacheService.clear();
        return ResponseEntity.noContent().build();
    }
}
