package com.example.Cricbook.dto;

import com.example.Cricbook.enums.AttendanceStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TrustAttendanceRequest {
    private AttendanceStatus status;
    private LocalDateTime checkinTime;
}
