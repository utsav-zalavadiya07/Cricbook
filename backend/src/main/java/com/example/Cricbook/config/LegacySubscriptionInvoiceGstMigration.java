package com.example.Cricbook.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

@Component
@RequiredArgsConstructor
@Slf4j
public class LegacySubscriptionInvoiceGstMigration implements ApplicationRunner {

    static final String MIGRATION_KEY = "subscription_invoice_gst_inclusive_v1";

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        if (!isMySql()) {
            return;
        }

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS app_schema_migrations (
                    migration_key VARCHAR(100) PRIMARY KEY,
                    applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """);

        Integer applied = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM app_schema_migrations WHERE migration_key = ?",
                Integer.class,
                MIGRATION_KEY
        );
        if (applied != null && applied > 0) {
            return;
        }

        jdbcTemplate.update("""
                UPDATE tax_records tr
                JOIN invoices i ON i.id = tr.invoice_id
                SET tr.gst_amount = ROUND(i.amount * 18 / 118, 2),
                    tr.tax_rate = 0.18
                WHERE i.subscription_id IS NOT NULL
                """);
        int correctedInvoices = jdbcTemplate.update("""
                UPDATE invoices
                SET tax_amount = ROUND(amount * 18 / 118, 2),
                    amount = amount - ROUND(amount * 18 / 118, 2)
                WHERE subscription_id IS NOT NULL
                """);
        jdbcTemplate.update(
                "INSERT INTO app_schema_migrations (migration_key) VALUES (?)",
                MIGRATION_KEY
        );
        log.info("Corrected {} legacy subscription invoices to GST-inclusive pricing.", correctedInvoices);
    }

    private boolean isMySql() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            String product = metadata.getDatabaseProductName();
            return product != null && product.toLowerCase().contains("mysql");
        }
    }
}
