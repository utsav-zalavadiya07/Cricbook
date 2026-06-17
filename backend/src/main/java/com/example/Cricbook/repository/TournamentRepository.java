package com.example.Cricbook.repository;

import com.example.Cricbook.entity.Tournament;
import com.example.Cricbook.enums.TournamentFormat;
import com.example.Cricbook.enums.TournamentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDate;

@Repository
public interface TournamentRepository extends JpaRepository<Tournament, Long> {
    List<Tournament> findByStatusOrderByStartDateAsc(TournamentStatus status);
    List<Tournament> findByOrganizerIdOrderByStartDateDesc(Long organizerId);

    @Query("SELECT t FROM Tournament t WHERE t.status IN :statuses ORDER BY t.startDate ASC")
    List<Tournament> findByStatusesOrderByStartDateAsc(@Param("statuses") List<TournamentStatus> statuses);

    boolean existsByNameIgnoreCaseAndCityIgnoreCase(String name, String city);

    @Query("""
            SELECT t FROM Tournament t
            WHERE (:query IS NULL OR LOWER(t.name) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(t.organizer.name) LIKE LOWER(CONCAT('%', :query, '%')))
              AND (:city IS NULL OR LOWER(t.city) = LOWER(:city))
              AND (:format IS NULL OR t.format = :format)
              AND (:status IS NULL OR t.status = :status)
              AND (:maxEntryFee IS NULL OR t.entryFee <= :maxEntryFee)
              AND (:fromDate IS NULL OR t.startDate >= :fromDate)
              AND (:toDate IS NULL OR t.startDate <= :toDate)
            ORDER BY t.startDate ASC
            """)
    List<Tournament> searchTournaments(
            @Param("query") String query,
            @Param("city") String city,
            @Param("format") TournamentFormat format,
            @Param("status") TournamentStatus status,
            @Param("maxEntryFee") BigDecimal maxEntryFee,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );
}
