package com.cdg.ordersupport.user;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import db.migration.V5__retire_legacy_ticket_tables;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.flywaydb.core.api.migration.Context;
import org.junit.jupiter.api.Test;

class LegacyTicketMigrationGuardTest {
    @Test void refusesToDropLegacyTablesReferencedByAnOrderTable() throws Exception {
        try (var connection = DriverManager.getConnection(
                "jdbc:h2:mem:legacy_guard;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE", "sa", "")) {
            try (var statement = connection.createStatement()) {
                statement.execute("CREATE TABLE public.support_tickets (id UUID PRIMARY KEY)");
                statement.execute("CREATE TABLE public.orders (id UUID PRIMARY KEY, ticket_id UUID REFERENCES public.support_tickets(id))");
            }
            Context context = mock(Context.class);
            when(context.getConnection()).thenReturn(connection);
            SQLException failure = assertThrows(SQLException.class,
                    () -> new V5__retire_legacy_ticket_tables().migrate(context));
            assertTrue(failure.getMessage().contains("orders"));
            try (var statement = connection.createStatement();
                    var remaining = statement.executeQuery("SELECT count(*) FROM public.support_tickets")) {
                assertTrue(remaining.next());
            }
        }
    }
}
