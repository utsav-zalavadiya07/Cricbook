package com.example.Cricbook.repository;

import com.example.Cricbook.entity.AthleteDevelopmentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AthleteDevelopmentRecordRepository extends JpaRepository<AthleteDevelopmentRecord, Long> {
    List<AthleteDevelopmentRecord> findAllByOrderByCreatedAtDesc();

    @Query("""
            SELECT record FROM AthleteDevelopmentRecord record
            WHERE LOWER(record.athleteName) LIKE LOWER(CONCAT('%', :athleteName, '%'))
               OR LOWER(record.schoolOrInstitution) LIKE LOWER(CONCAT('%', :schoolOrInstitution, '%'))
               OR LOWER(record.districtName) LIKE LOWER(CONCAT('%', :districtName, '%'))
            ORDER BY record.createdAt DESC
            """)
    List<AthleteDevelopmentRecord> findByAthleteNameContainingIgnoreCaseOrSchoolOrInstitutionContainingIgnoreCaseOrDistrictNameContainingIgnoreCaseOrderByCreatedAtDesc(
            @Param("athleteName") String athleteName,
            @Param("schoolOrInstitution") String schoolOrInstitution,
            @Param("districtName") String districtName
    );
}
