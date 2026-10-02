package db.migration;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V5__retire_legacy_ticket_tables extends BaseJavaMigration {
    private static final Set<String> LEGACY = Set.of("support_tickets", "ticket_comments");
    @Override public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        DatabaseMetaData metadata = connection.getMetaData();
        List<Table> tables = new ArrayList<>();
        for (String name : LEGACY) {
            Table table = findPublicTable(metadata, name);
            if (table != null) tables.add(table);
        }
        for (Table table : tables) {
            try (ResultSet references = metadata.getExportedKeys(null, table.schema(), table.name())) {
                while (references.next()) {
                    String referencing = references.getString("FKTABLE_NAME");
                    if (!LEGACY.contains(referencing.toLowerCase())) {
                        throw new SQLException("Cannot retire legacy ticket table " + table.name()
                                + ": foreign key from " + referencing + " still depends on it");
                    }
                }
            }
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS public.ticket_comments");
            statement.execute("DROP TABLE IF EXISTS public.support_tickets");
        }
    }
    private static Table findPublicTable(DatabaseMetaData metadata, String expected) throws SQLException {
        try (ResultSet tables = metadata.getTables(null, null, null, new String[]{"TABLE"})) {
            while (tables.next()) {
                String schema = tables.getString("TABLE_SCHEM");
                String name = tables.getString("TABLE_NAME");
                if ("public".equalsIgnoreCase(schema) && expected.equalsIgnoreCase(name))
                    return new Table(schema, name);
            }
        }
        return null;
    }
    private record Table(String schema, String name) {}
}
