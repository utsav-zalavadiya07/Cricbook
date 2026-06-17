package com.example.Cricbook.dto;

import com.example.Cricbook.enums.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {
    private Long id;
    private Long playerId;
    private String playerName;
    private String playerEmail;
    private String playerPhone;
    private Long turfId;
    private String sport;
    private LocalDate bookingDate;
    private BookingStatus status;
    private BigDecimal amount;
    private BigDecimal totalAmount;
    private Long slotId;
    private List<Long> slotIds;
    private Integer slotCount;
    private String turfName;
    private String turfCity;
    private String turfLocation;
    private LocalTime startTime;
    private LocalTime endTime;
    private String paymentStatus;
    private String attendanceStatus;
    private LocalDateTime checkedInAt;
    private LocalDateTime createdAt;
}
