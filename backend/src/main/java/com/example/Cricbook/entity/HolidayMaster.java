package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "holiday_master", indexes = {
        @Index(name = "idx_holiday_date_country", columnList = "holiday_date, country")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HolidayMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "country", nullable = false, length = 100)
    private String country;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "holiday_name", nullable = false, length = 150)
    private String holidayName;

    @Column(name = "holiday_date", nullable = false)
    private LocalDate holidayDate;

    @Column(name = "holiday_type", nullable = false, length = 50)
    private String holidayType; // e.g. NATIONAL, STATE, FESTIVAL, LOCAL

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;
}
