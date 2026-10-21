package com.example.Cricbook.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegacySubscriptionInvoiceGstMigrationTest {

    @Mock private DataSource dataSource;
    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private Connection connection;
    @Mock private DatabaseMetaData metadata;
    @Mock private ApplicationArguments applicationArguments;

    @Test
    void correctsLegacySubscriptionInvoicesOnce() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metadata);
        when(metadata.getDatabaseProductName()).thenReturn("MySQL");
        when(jdbcTemplate.queryForObject(
                contains("app_schema_migrations"),
                eq(Integer.class),
                eq(LegacySubscriptionInvoiceGstMigration.MIGRATION_KEY)
        )).thenReturn(0);
        when(jdbcTemplate.update(anyString())).thenReturn(3);

        new LegacySubscriptionInvoiceGstMigration(dataSource, jdbcTemplate).run(applicationArguments);

        verify(jdbcTemplate).update(contains("UPDATE tax_records"));
        verify(jdbcTemplate).update(contains("UPDATE invoices"));
        verify(jdbcTemplate).update(
                contains("INSERT INTO app_schema_migrations"),
                eq(LegacySubscriptionInvoiceGstMigration.MIGRATION_KEY)
        );
    }

    @Test
    void skipsCorrectionWhenMigrationWasAlreadyApplied() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metadata);
        when(metadata.getDatabaseProductName()).thenReturn("MySQL");
        when(jdbcTemplate.queryForObject(
                contains("app_schema_migrations"),
                eq(Integer.class),
                eq(LegacySubscriptionInvoiceGstMigration.MIGRATION_KEY)
        )).thenReturn(1);

        new LegacySubscriptionInvoiceGstMigration(dataSource, jdbcTemplate).run(applicationArguments);

        verify(jdbcTemplate, never()).update(contains("UPDATE invoices"));
        verify(jdbcTemplate, never()).update(contains("UPDATE tax_records"));
    }
}
