package com.olehkobylianskyi.familypoints.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/**
 * Read-only content fingerprint under the family fence. Directly scoped tables
 * and their FK descendants are hashed, not just counted. This is NOT a deletion
 * plan: protected identities, financial policy and FK closure still gate execute.
 * Global credential tables and reset protocol state are deliberately excluded.
 * Immutable media bytes are represented by their scoped media_assets hashes.
 */
@Service
public class WorkspaceResetSnapshotService {
    private final JdbcTemplate db;
    private final WorkspaceEpochTransactionGuard guard;
    private static final Set<String> EXCLUDED = Set.of("user_accounts", "account_external_identities",
            "account_email_tokens", "workspace_reset_operations", "workspace_reset_challenges",
            "workspace_reset_reauth_limits", "media_blobs", "flyway_schema_history");
    private static final Set<String> CATEGORIES = Set.of("PRIVATE_TEMPLATES","TASK_DEFINITIONS",
            "TASK_EXECUTIONS","REWARD_DEFINITIONS","REWARD_REQUESTS","REWARD_PURCHASES",
            "POINT_LEDGER","REPUTATION_LEDGER","MEMBER_GROUPS","WORK_NODES","TASK_SCHEDULE_SLOTS",
            "MEDIA_ASSETS","MEDIA_BINDINGS");

    public WorkspaceResetSnapshotService(JdbcTemplate db, WorkspaceEpochTransactionGuard guard) {
        this.db = db; this.guard = guard;
    }

    private static class Table {
        final String name;
        final List<String> columns = new ArrayList<>();
        final Map<String, Edge> edges = new TreeMap<>();
        Table(String name) { this.name = name; }
    }
    private static class Edge {
        final String parent;
        final List<String> childColumns = new ArrayList<>(), parentColumns = new ArrayList<>();
        final String deleteAction;
        Edge(String parent, String deleteAction) { this.parent = parent; this.deleteAction = deleteAction; }
    }
    private record Scope(String sql, int parameters) {}
    private static String quoted(String value) { return "\"" + value.replace("\"", "\"\"") + "\""; }
    private static void token(MessageDigest hash, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        hash.update((bytes.length + ":").getBytes(StandardCharsets.UTF_8));
        hash.update(bytes);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public String digest(long workspaceId, Collection<String> selected) {
        if (selected == null || selected.stream().anyMatch(Objects::isNull) || !CATEGORIES.containsAll(selected))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid reset selection");
        var categories = selected.stream().sorted().distinct().toList();
        long epoch = guard.lockCurrentEpoch(workspaceId);
        try {
            MessageDigest hash = MessageDigest.getInstance("SHA-256");
            token(hash, "reset-content-v2");
            token(hash, Long.toString(workspaceId));
            token(hash, Long.toString(epoch));
            token(hash, new ObjectMapper().writeValueAsString(categories));
            Map<String, Table> tables = discoverTables();
            for (var table : tables.values()) {
                if (EXCLUDED.contains(table.name)) continue;
                Scope scope = scope(tables, table.name, "r", Set.of(), new int[]{0});
                if (scope == null) continue;
                token(hash, table.name);
                token(hash, table.columns.toString());
                token(hash, scope.sql());
                for (var entry : table.edges.entrySet()) {
                    var edge = entry.getValue();
                    token(hash, entry.getKey()+":"+edge.parent+":"+edge.childColumns+":"+edge.parentColumns+":"+edge.deleteAction);
                }
                String sql = "SELECT to_jsonb(r)::text AS row_value FROM public."+quoted(table.name)
                        +" r WHERE "+scope.sql()+" ORDER BY row_value";
                db.query(connection -> {
                    var statement = connection.prepareStatement(sql);
                    for (int i=1; i<=scope.parameters(); i++) statement.setLong(i, workspaceId);
                    statement.setFetchSize(128);
                    return statement;
                }, (RowCallbackHandler) row -> token(hash, row.getString("row_value")));
            }
            return HexFormat.of().formatHex(hash.digest());
        } catch (java.security.NoSuchAlgorithmException | java.io.IOException exception) {
            throw new IllegalStateException("Unable to fingerprint reset scope", exception);
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void requireStoredSnapshot(long workspaceId, String selectedJson, String expected) {
        try {
            var tree = new ObjectMapper().readTree(selectedJson);
            if (tree == null || !tree.isArray()) throw new IllegalArgumentException();
            var selected = new ArrayList<String>();
            for (var item : tree) {
                if (!item.isTextual()) throw new IllegalArgumentException();
                selected.add(item.textValue());
            }
            if (!selected.equals(selected.stream().sorted().distinct().toList())
                    || !CATEGORIES.containsAll(selected))
                throw new IllegalArgumentException();
            if (!digest(workspaceId, selected).equals(expected))
                throw new ResponseStatusException(HttpStatus.CONFLICT, "RESET_SNAPSHOT_STALE");
        } catch (java.io.IOException | IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "RESET_SNAPSHOT_SELECTION_INVALID");
        }
    }

    private Map<String, Table> discoverTables() {
        Map<String, Table> tables = new TreeMap<>();
        db.query("""
                SELECT c.relname AS table_name,a.attname AS column_name
                FROM pg_catalog.pg_class c
                JOIN pg_catalog.pg_namespace n ON n.oid=c.relnamespace
                JOIN pg_catalog.pg_attribute a ON a.attrelid=c.oid AND a.attnum>0 AND NOT a.attisdropped
                WHERE n.nspname='public' AND c.relkind IN ('r','p')
                ORDER BY c.relname,a.attnum
                """, (RowCallbackHandler) row -> tables.computeIfAbsent(row.getString("table_name"), Table::new)
                .columns.add(row.getString("column_name")));
        db.query("""
                SELECT c.conname,child.relname AS child_table,parent.relname AS parent_table,
                       ca.attname AS child_column,pa.attname AS parent_column,c.confdeltype AS delete_action
                FROM pg_catalog.pg_constraint c
                JOIN pg_catalog.pg_class child ON child.oid=c.conrelid
                JOIN pg_catalog.pg_class parent ON parent.oid=c.confrelid
                JOIN pg_catalog.pg_namespace cn ON cn.oid=child.relnamespace AND cn.nspname='public'
                JOIN pg_catalog.pg_namespace pn ON pn.oid=parent.relnamespace AND pn.nspname='public'
                CROSS JOIN LATERAL unnest(c.conkey,c.confkey) WITH ORDINALITY AS k(child_att,parent_att,idx)
                JOIN pg_catalog.pg_attribute ca ON ca.attrelid=child.oid AND ca.attnum=k.child_att
                JOIN pg_catalog.pg_attribute pa ON pa.attrelid=parent.oid AND pa.attnum=k.parent_att
                WHERE c.contype='f' ORDER BY child.relname,c.conname,k.idx
                """, (RowCallbackHandler) row -> {
            Table child = tables.get(row.getString("child_table"));
            if (child == null) return;
            Edge edge = child.edges.computeIfAbsent(row.getString("conname"),
                    key -> new Edge(uncheckedString(row,"parent_table"),uncheckedString(row,"delete_action")));
            edge.childColumns.add(row.getString("child_column"));
            edge.parentColumns.add(row.getString("parent_column"));
        });
        return tables;
    }

    private static String uncheckedString(java.sql.ResultSet row, String column) {
        try { return row.getString(column); }
        catch (java.sql.SQLException exception) { throw new IllegalStateException(exception); }
    }

    private Scope scope(Map<String, Table> tables, String name, String alias, Set<String> visited, int[] sequence) {
        if (EXCLUDED.contains(name) || visited.contains(name)) return null;
        if (visited.size()>16 || sequence[0]>5000)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "RESET_SNAPSHOT_SCOPE_TOO_COMPLEX");
        Table table = tables.get(name);
        if (table == null) return null;
        if (name.equals("families")) return new Scope(alias+".id=?",1);
        var direct = table.columns.stream().filter(c -> c.equals("family_id") || c.equals("workspace_id")).toList();
        if (!direct.isEmpty()) return new Scope("("+String.join(" OR ", direct.stream()
                .map(c -> alias+"."+quoted(c)+"=?").toList())+")",direct.size());
        var path = new HashSet<>(visited); path.add(name);
        var clauses = new ArrayList<String>(); int parameters = 0;
        for (var edge : table.edges.values()) {
            String parentAlias = "j"+sequence[0]++;
            Scope parent = scope(tables,edge.parent,parentAlias,path,sequence);
            if (parent == null) continue;
            var joins = new ArrayList<String>();
            for(int i=0;i<edge.childColumns.size();i++)
                joins.add(alias+"."+quoted(edge.childColumns.get(i))+"="+parentAlias+"."+quoted(edge.parentColumns.get(i)));
            clauses.add("EXISTS (SELECT 1 FROM public."+quoted(edge.parent)+" "+parentAlias
                    +" WHERE "+String.join(" AND ",joins)+" AND "+parent.sql()+")");
            parameters += parent.parameters();
        }
        return clauses.isEmpty() ? null : new Scope("("+String.join(" OR ",clauses)+")",parameters);
    }
}
