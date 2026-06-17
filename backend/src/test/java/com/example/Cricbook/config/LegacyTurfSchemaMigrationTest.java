package com.example.Cricbook.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LegacyTurfSchemaMigrationTest {

    @Test
    void runNormalizesTurfStatusColumnForMysql() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metadata = mock(DatabaseMetaData.class);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metadata);
        when(metadata.getDatabaseProductName()).thenReturn("MySQL");
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(0);

        LegacyTurfSchemaMigration migration = new LegacyTurfSchemaMigration(dataSource, jdbcTemplate);

        migration.run(mock(ApplicationArguments.class));

        var order = inOrder(jdbcTemplate);
        order.verify(jdbcTemplate).execute("ALTER TABLE turfs MODIFY COLUMN pitch_type VARCHAR(32) NOT NULL");
        order.verify(jdbcTemplate).execute("ALTER TABLE turfs MODIFY COLUMN box_type VARCHAR(32) NOT NULL");
        order.verify(jdbcTemplate).execute("ALTER TABLE turfs MODIFY COLUMN status VARCHAR(32) NOT NULL");
        order.verify(jdbcTemplate).queryForObject(anyString(), eq(Integer.class));
        order.verify(jdbcTemplate).execute("ALTER TABLE slots ADD COLUMN archived BOOLEAN NOT NULL DEFAULT FALSE");
    }

    @Test
    void runSkipsNormalizationForNonMysqlDatabase() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metadata = mock(DatabaseMetaData.class);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metadata);
        when(metadata.getDatabaseProductName()).thenReturn("H2");

        LegacyTurfSchemaMigration migration = new LegacyTurfSchemaMigration(dataSource, jdbcTemplate);

        migration.run(mock(ApplicationArguments.class));

        verify(jdbcTemplate, never()).execute(org.mockito.ArgumentMatchers.anyString());
    }
}
