package com.cdg.ordersupport.user;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import db.migration.V5__retire_legacy_ticket_tables;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.flywaydb.core.api.migration.Context;
import org.junit.jupiter.api.Test;

class LegacyTicketMigrationGuardTest {
    @Test void preservesNonemptyLegacyTicketsUntilTheyAreMigrated() throws Exception {
        assertNonemptyLegacyTablePreserved("support_tickets");
    }

    @Test void preservesNonemptyLegacyCommentsUntilTheyAreMigrated() throws Exception {
        assertNonemptyLegacyTablePreserved("ticket_comments");
    }

    private void assertNonemptyLegacyTablePreserved(String populatedTable) throws Exception {
        try (var connection = DriverManager.getConnection(
                "jdbc:h2:mem:legacy_data_" + populatedTable + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE", "sa", "")) {
            try (var statement = connection.createStatement()) {
                statement.execute("CREATE TABLE public.support_tickets (id UUID PRIMARY KEY)");
                statement.execute("CREATE TABLE public.ticket_comments (id UUID PRIMARY KEY)");
                statement.execute("INSERT INTO public." + populatedTable
                        + " (id) VALUES ('00000000-0000-0000-0000-000000000001')");
            }
            Context context = mock(Context.class);
            when(context.getConnection()).thenReturn(connection);
            SQLException failure = assertThrows(SQLException.class,
                    () -> new V5__retire_legacy_ticket_tables().migrate(context));
            assertTrue(failure.getMessage().contains("Migrate legacy ticket data"));
            try (var statement = connection.createStatement();
                    var rows = statement.executeQuery("SELECT count(*) FROM public." + populatedTable)) {
                assertTrue(rows.next());
                assertEquals(1, rows.getInt(1));
            }
        }
    }
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
