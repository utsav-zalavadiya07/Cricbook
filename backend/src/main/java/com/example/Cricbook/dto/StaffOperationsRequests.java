package com.example.Cricbook.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class StaffOperationsRequests {

    @Data
    public static class CheckInRequest {
        private Long bookingId;
        private String bookingCode;
        private String phone;
        private String qrPayload;
        private String note;
    }

    @Data
    public static class TaskRequest {
        private Long centerId;
        private Long staffId;
        private Long assignmentId;
        private Long bookingId;
        private String title;
        private String description;
        private String category;
        private String priority;
        private LocalDateTime dueAt;
    }

    @Data
    public static class StatusRequest {
        private String status;
        private String note;
    }

    @Data
    public static class MaintenanceTicketRequest {
        private Long centerId;
        private Long turfId;
        private Long assignedStaffId;
        private String title;
        private String description;
        private String category;
        private String priority;
        private List<String> mediaUrls;
    }

    @Data
    public static class AttendanceRequest {
        private Long centerId;
        private Double latitude;
        private Double longitude;
        private String deviceId;
        private String source;
    }

    @Data
    public static class EquipmentRequest {
        private Long centerId;
        private Long turfId;
        private String name;
        private String equipmentType;
        private Integer stockCount;
        private String assignedTo;
        private String note;
    }

    @Data
    public static class ShiftRequest {
        private Long centerId;
        private Long staffId;
        private LocalDate shiftDate;
        private LocalTime startTime;
        private LocalTime endTime;
        private String status;
        private String notes;
    }

    @Data
    public static class EmergencyAlertRequest {
        private Long centerId;
        private Long turfId;
        private String type;
        private String severity;
        private String message;
    }
}
