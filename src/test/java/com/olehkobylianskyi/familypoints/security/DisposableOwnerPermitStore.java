package com.olehkobylianskyi.familypoints.security;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

import static com.olehkobylianskyi.familypoints.security.WorkspaceDestructivePermitContract.*;

/**
 * QA-ONLY, disposable PostgreSQL owner permit rehearsal. There is NO production bean,
 * HTTP route or Flyway migration and NO reset or restore domain-row mutation.
 * The issuer accepts SYNTHETIC identity: do not use it for real authentication.
 */
public final class DisposableOwnerPermitStore {
    private final JdbcTemplate db;
    private final TransactionTemplate tx;
    private final SecureRandom entropy = new SecureRandom();

    public record Issued(UUID id, String secret) {
        @Override public String toString() { return "Issued[redacted]"; }
    }

    public DisposableOwnerPermitStore(JdbcTemplate db, TransactionTemplate tx) {
        this.db = db;
        this.tx = tx;
    }

    /** Run only after Flyway on disposable PG18. Never create this in production. */
    public void createQaTables() {
        db.execute("""
                CREATE TABLE IF NOT EXISTS qa_c_owner_permits (
                    id uuid PRIMARY KEY,
                    workspace_id bigint NOT NULL REFERENCES families(id) ON DELETE RESTRICT,
                    action varchar(8) NOT NULL CHECK (action IN ('RESET','RESTORE')),
                    operation_id uuid NOT NULL,
                    owner_account_id bigint NOT NULL,
                    owner_member_id bigint NOT NULL,
                    snapshot_id uuid NOT NULL,
                    snapshot_digest char(64) NOT NULL,
                    selection_digest char(64) NOT NULL,
                    member_mapping_digest char(64),
                    data_epoch bigint NOT NULL,
                    issued_at timestamptz NOT NULL,
                    expires_at timestamptz NOT NULL,
                    verifier_hash char(64) NOT NULL CHECK (verifier_hash ~ '^[0-9a-f]{64}$'),
                    consumed_at timestamptz,
                    UNIQUE(workspace_id,action,operation_id),
                    CHECK(expires_at > issued_at AND expires_at <= issued_at + interval '5 minutes'),
                    CHECK((action='RESET' AND member_mapping_digest IS NULL)
                       OR (action='RESTORE' AND member_mapping_digest IS NOT NULL))
                )
                """);
        db.execute("""
                CREATE TABLE IF NOT EXISTS qa_c_owner_permit_receipts (
                    permit_id uuid PRIMARY KEY REFERENCES qa_c_owner_permits(id) ON DELETE RESTRICT,
                    workspace_id bigint NOT NULL REFERENCES families(id) ON DELETE RESTRICT,
                    action varchar(8) NOT NULL,
                    from_epoch bigint NOT NULL,
                    to_epoch bigint NOT NULL
                )
                """);
    }

    public Issued issueSynthetic(Scope scope) {
        byte[] random = new byte[32];
        entropy.nextBytes(random);
        String secret = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        UUID id = UUID.randomUUID();
        tx.executeWithoutResult(status -> {
            Long epoch = db.queryForObject(
                    "SELECT data_epoch FROM families WHERE id=? FOR UPDATE",
                    Long.class, scope.workspaceId());
            if (epoch == null || epoch != scope.dataEpoch()) deny(HttpStatus.CONFLICT);
            int inserted = db.update("""
                    INSERT INTO qa_c_owner_permits
                    (id,workspace_id,action,operation_id,owner_account_id,owner_member_id,
                     snapshot_id,snapshot_digest,selection_digest,member_mapping_digest,
                     data_epoch,issued_at,expires_at,verifier_hash)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                    ON CONFLICT (workspace_id,action,operation_id) DO NOTHING
                    """, id, scope.workspaceId(), scope.action().name(), scope.operationId(),
                    scope.ownerAccountId(), scope.ownerMemberId(), scope.snapshotId(),
                    scope.snapshotDigest(), scope.selectionDigest(), scope.memberMappingDigest(),
                    scope.dataEpoch(), java.sql.Timestamp.from(scope.issuedAt()),
                    java.sql.Timestamp.from(scope.expiresAt()), sha(secret));
            if (inserted != 1) deny(HttpStatus.CONFLICT);
        });
        return new Issued(id, secret);
    }

    /**
     * Simulate a same-transaction permit consume + epoch/receipt write.
     * A real executor must authenticate owner independently before this stage.
     */
    public void consumeSynthetic(Issued issued, Scope attempted, boolean failAfterConsumption) {
        tx.executeWithoutResult(status -> {
            // Canonical order: target family FOR UPDATE -> permit FOR UPDATE -> QA writes.
            Long epoch = db.queryForObject(
                    "SELECT data_epoch FROM families WHERE id=? FOR UPDATE",
                    Long.class, attempted.workspaceId());
            if (epoch == null || epoch != attempted.dataEpoch()) deny(HttpStatus.CONFLICT);
            var rows = db.queryForList("""
                    SELECT action,operation_id,owner_account_id,owner_member_id,snapshot_id,
                           snapshot_digest,selection_digest,member_mapping_digest,data_epoch,
                           issued_at,expires_at,verifier_hash,consumed_at,
                           clock_timestamp() AS server_now
                    FROM qa_c_owner_permits
                    WHERE id=? AND workspace_id=? FOR UPDATE
                    """, issued.id(), attempted.workspaceId());
            if (rows.size() != 1) deny(HttpStatus.NOT_FOUND);
            Map<String,Object> row = rows.get(0);
            if (row.get("consumed_at") != null) deny(HttpStatus.CONFLICT);
            Object serverClock = row.get("server_now");
            if (!(serverClock instanceof java.util.Date)) deny(HttpStatus.CONFLICT);
            Instant now = ((java.util.Date)serverClock).toInstant();
            Instant issuedAt = ((java.util.Date) row.get("issued_at")).toInstant();
            Instant expiresAt = ((java.util.Date) row.get("expires_at")).toInstant();
            Scope stored;
            try {
                stored = new Scope(Action.valueOf((String) row.get("action")),
                        (UUID)row.get("operation_id"), attempted.workspaceId(),
                        ((Number)row.get("owner_account_id")).longValue(),
                        ((Number)row.get("owner_member_id")).longValue(),
                        (UUID)row.get("snapshot_id"),
                        ((String)row.get("snapshot_digest")).trim(),
                        ((String)row.get("selection_digest")).trim(),
                        row.get("member_mapping_digest") == null ? null
                            : ((String)row.get("member_mapping_digest")).trim(),
                        ((Number)row.get("data_epoch")).longValue(), issuedAt, expiresAt);
            } catch (RuntimeException invalid) {
                deny(HttpStatus.CONFLICT);
                return;
            }
            if (!stored.matches(attempted, now)) deny(HttpStatus.CONFLICT);
            String verifier = (String)row.get("verifier_hash");
            if (verifier == null || !verifier.matches("[0-9a-f]{64}")
                    || issued.secret() == null
                    || !MessageDigest.isEqual(
                        verifier.getBytes(StandardCharsets.US_ASCII),
                        sha(issued.secret()).getBytes(StandardCharsets.US_ASCII)))
                deny(HttpStatus.FORBIDDEN);
            if (db.update("""
                    UPDATE qa_c_owner_permits SET consumed_at=clock_timestamp()
                    WHERE id=? AND workspace_id=? AND consumed_at IS NULL
                      AND expires_at>clock_timestamp()
                    """,issued.id(),attempted.workspaceId()) != 1)
                deny(HttpStatus.CONFLICT);
            if (failAfterConsumption) throw new IllegalStateException("QA injected rollback");
            if (db.update("""
                    UPDATE families SET data_epoch=data_epoch+1
                    WHERE id=? AND data_epoch=?
                    """,attempted.workspaceId(),attempted.dataEpoch()) != 1)
                deny(HttpStatus.CONFLICT);
            db.update("""
                    INSERT INTO qa_c_owner_permit_receipts
                    (permit_id,workspace_id,action,from_epoch,to_epoch)
                    VALUES (?,?,?,?,?)
                    """, issued.id(), attempted.workspaceId(),
                    attempted.action().name(), attempted.dataEpoch(), attempted.dataEpoch()+1);
        });
    }

    private static String sha(String secret) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(secret.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void deny(HttpStatus status) {
        throw new ResponseStatusException(status, "QA_OWNER_PERMIT_DENIED");
    }
}
