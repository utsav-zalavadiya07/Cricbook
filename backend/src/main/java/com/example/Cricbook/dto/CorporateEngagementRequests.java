package com.example.Cricbook.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class CorporateEngagementRequests {

    private CorporateEngagementRequests() {
    }

    @Data
    public static class CompanyRequest {
        private String name;
        private String gstNumber;
        private String industry;
        private String city;
        private String locations;
        private String hrContactName;
        private String hrEmail;
        private String hrPhone;
        private String adminEmail;
        private String logoUrl;
        private String documentUrl;
        private BigDecimal annualContractValue;
        private BigDecimal monthlyBudget;
    }

    @Data
    public static class StatusRequest {
        private String status;
        private String notes;
    }

    @Data
    public static class LocationRequest {
        private Long companyId;
        private String name;
        private String city;
        private String address;
        private Integer employeeCount;
        private String status;
    }

    @Data
    public static class DepartmentRequest {
        private Long companyId;
        private String name;
        private String parentDepartment;
        private String costCenter;
    }

    @Data
    public static class EmployeeRequest {
        private Long companyId;
        private Long locationId;
        private Long departmentId;
        private String name;
        private String email;
        private String phone;
        private String employeeCode;
        private String role;
    }

    @Data
    public static class TeamRequest {
        private Long companyId;
        private Long departmentId;
        private String name;
        private String sport;
        private Long captainEmployeeId;
    }

    @Data
    public static class TeamMembershipRequest {
        private Long teamId;
        private Long employeeId;
        private String role;
    }

    @Data
    public static class BookingPlanRequest {
        private Long companyId;
        private Long departmentId;
        private String sport;
        private String turfName;
        private String city;
        private String recurrenceType;
        private LocalDate startDate;
        private LocalDate endDate;
        private BigDecimal monthlySpend;
        private String conflictNotes;
    }

    @Data
    public static class TournamentRequest {
        private Long companyId;
        private String name;
        private String sport;
        private String scope;
        private LocalDate startDate;
        private LocalDate endDate;
        private Integer teamCount;
        private Boolean fixturesGenerated;
        private String status;
    }

    @Data
    public static class WellnessProgramRequest {
        private Long companyId;
        private String title;
        private String sport;
        private String programType;
        private LocalDate startDate;
        private LocalDate endDate;
        private Integer rewardPoints;
        private String status;
    }

    @Data
    public static class WellnessActivityRequest {
        private Long programId;
        private Long employeeId;
        private String activityType;
        private String attendanceStatus;
        private Integer sportsHours;
        private Boolean completed;
    }

    @Data
    public static class RewardRequest {
        private Long employeeId;
        private String sourceType;
        private Integer points;
        private String summary;
    }

    @Data
    public static class InvoiceRequest {
        private Long companyId;
        private String billingPeriod;
        private BigDecimal amount;
        private BigDecimal taxAmount;
        private BigDecimal gstAmount;
        private LocalDate dueDate;
    }

    @Data
    public static class PaymentRequest {
        private String status;
    }

    @Data
    public static class ContractRequest {
        private Long companyId;
        private String planName;
        private BigDecimal annualValue;
        private LocalDate startDate;
        private LocalDate endDate;
        private Integer renewalProbability;
        private String status;
    }

    @Data
    public static class LeagueRequest {
        private String name;
        private String sport;
        private Long organizerCompanyId;
        private String invitedCompanyIds;
        private Integer registeredCompanies;
        private Integer teamCount;
        private Boolean fixturesGenerated;
        private String status;
    }
}
