package com.example.Cricbook.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

@Component
@RequiredArgsConstructor
@Slf4j
public class LegacyTurfSchemaMigration implements ApplicationRunner {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            String product = metadata.getDatabaseProductName();
            if (product == null || !product.toLowerCase().contains("mysql")) {
                return;
            }
        }

        normalizeTurfEnumColumns();
        ensureSlotArchiveColumn();
    }

    private void normalizeTurfEnumColumns() {
        jdbcTemplate.execute("ALTER TABLE turfs MODIFY COLUMN pitch_type VARCHAR(32) NOT NULL");
        jdbcTemplate.execute("ALTER TABLE turfs MODIFY COLUMN box_type VARCHAR(32) NOT NULL");
        jdbcTemplate.execute("ALTER TABLE turfs MODIFY COLUMN status VARCHAR(32) NOT NULL");
        log.info("Verified turf enum columns use VARCHAR(32).");
    }

    private void ensureSlotArchiveColumn() {
        Integer columnCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                AND TABLE_NAME = 'slots'
                AND COLUMN_NAME = 'archived'
                """, Integer.class);
        if (columnCount != null && columnCount > 0) {
            return;
        }
        jdbcTemplate.execute("ALTER TABLE slots ADD COLUMN archived BOOLEAN NOT NULL DEFAULT FALSE");
        log.info("Added slots.archived column for safe slot configuration replacement.");
    }
}
