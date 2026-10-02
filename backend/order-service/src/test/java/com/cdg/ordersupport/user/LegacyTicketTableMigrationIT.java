package com.cdg.ordersupport.user;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class LegacyTicketTableMigrationIT {
    @Autowired JdbcTemplate jdbc;
    @Test void orderServiceNoLongerOwnsLegacyTicketTables() {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES "
                + "WHERE LOWER(TABLE_SCHEMA) = 'public' AND LOWER(TABLE_NAME) IN ('support_tickets', 'ticket_comments')",
                Integer.class);
        assertEquals(0, count);
    }
}
