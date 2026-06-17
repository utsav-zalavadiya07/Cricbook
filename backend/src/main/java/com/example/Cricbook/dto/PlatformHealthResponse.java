package com.example.Cricbook.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformHealthResponse {
    private String status;
    private Integer cacheEntries;
    private Long usedMemoryMb;
    private Long totalMemoryMb;
    private Integer availableProcessors;
}
