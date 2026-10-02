package com.cdg.supportservice.ticket;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class SupportSchemaIT {
    @Autowired JdbcTemplate jdbc;
    @Test void supportTablesAndFlywayHistoryBelongToSupportSchema() {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES "
                + "WHERE LOWER(TABLE_SCHEMA) = 'support' AND LOWER(TABLE_NAME) IN "
                + "('support_tickets', 'ticket_comments', 'ticket_outbox', 'support_flyway_schema_history')",
                Integer.class);
        assertEquals(4, count);
    }
}
