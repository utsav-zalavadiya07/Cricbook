package com.example.Cricbook.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformEpicSurfaceResponse {
    private String surface;
    private String status;
    private String owner;
    private List<String> capabilities;
}
