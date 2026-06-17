package com.example.Cricbook.repository;

import com.example.Cricbook.entity.HolidayMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface HolidayMasterRepository extends JpaRepository<HolidayMaster, Long> {

    @Query("SELECT h FROM HolidayMaster h WHERE h.holidayDate = :date AND h.isActive = true " +
           "AND h.country = :country " +
           "AND (h.state IS NULL OR h.state = '' OR h.state = :state) " +
           "AND (h.city IS NULL OR h.city = '' OR h.city = :city)")
    List<HolidayMaster> findMatchingHolidays(@Param("date") LocalDate date,
                                            @Param("country") String country,
                                            @Param("state") String state,
                                            @Param("city") String city);

    @Query("SELECT h FROM HolidayMaster h WHERE h.isActive = true " +
           "AND h.holidayDate >= :startDate " +
           "AND h.country = :country " +
           "AND (h.state IS NULL OR h.state = '' OR h.state = :state) " +
           "AND (h.city IS NULL OR h.city = '' OR h.city = :city) " +
           "ORDER BY h.holidayDate ASC")
    List<HolidayMaster> findUpcomingHolidays(@Param("startDate") LocalDate startDate,
                                            @Param("country") String country,
                                            @Param("state") String state,
                                            @Param("city") String city);
}
