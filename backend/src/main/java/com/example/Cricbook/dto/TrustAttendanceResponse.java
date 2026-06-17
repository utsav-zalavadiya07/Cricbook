package com.example.Cricbook.dto;

import com.example.Cricbook.enums.AttendanceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrustAttendanceResponse {
    private Long id;
    private Long bookingId;
    private Long userId;
    private String playerName;
    private AttendanceStatus status;
    private Integer delayMinutes;
    private String impact;
    private LocalDateTime checkinTime;
    private LocalDateTime createdAt;
}
