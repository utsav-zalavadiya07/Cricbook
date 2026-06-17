package com.example.Cricbook.repository;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AthleteDevelopmentRecordRepositoryTest {

    @Test
    void searchMethodUsesDeclaredQueryForSchoolOrInstitutionField() throws Exception {
        Method method = AthleteDevelopmentRecordRepository.class.getMethod(
                "findByAthleteNameContainingIgnoreCaseOrSchoolOrInstitutionContainingIgnoreCaseOrDistrictNameContainingIgnoreCaseOrderByCreatedAtDesc",
                String.class,
                String.class,
                String.class
        );

        Query query = method.getAnnotation(Query.class);

        assertNotNull(query);
        assertTrue(query.value().contains("record.schoolOrInstitution"));
    }
}
