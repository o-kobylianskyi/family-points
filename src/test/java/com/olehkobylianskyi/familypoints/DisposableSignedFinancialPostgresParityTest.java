package com.olehkobylianskyi.familypoints;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.olehkobylianskyi.familypoints.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import javax.crypto.KeyGenerator;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FIN-08 QA ONLY: authenticated V50 financial source -> explicit ID mapping
 * -> isolated PostgreSQL row reads -> exact lineage and balance reconciliation.
 * No native family tables or production Restore services are written.
 */
@Testcontainers
class DisposableSignedFinancialPostgresParityTest {
    @Container
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:18-alpine")
                    .withDatabaseName("fin08_rehearsal")
                    .withUsername("qa").withPassword("qa");

    private JdbcTemplate db;
    private TransactionTemplate transaction;
    private final ProtectiveBackupArtifactCodec codec = new ProtectiveBackupArtifactCodec();
    private final ProtectiveBackupPortableFile portable = new ProtectiveBackupPortableFile();
    private final ProtectiveBackupImportInspector inspector =
            new ProtectiveBackupImportInspector(portable, codec);
    private final ObjectMapper mapper = new ObjectMapper();

    private static final String LEDGER = """
            [{"id":1,"member_id":4,"point_type_id":3,"amount":5,
              "type":"EARN","source_type":"TASK","source_id":42},
             {"id":2,"member_id":4,"point_type_id":3,"amount":-3,
              "type":"SPEND","source_type":"REWARD_PURCHASE","source_id":51},
             {"id":3,"member_id":5,"point_type_id":8,"amount":7,
              "type":"ADJUSTMENT","source_type":"MANUAL","source_id":null}]
            """;

    @BeforeEach
    void setup() {
        var dataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        db = new JdbcTemplate(dataSource);
        transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        db.execute("""
                CREATE TABLE IF NOT EXISTS qa_fin08_ledger (
                    operation_id uuid NOT NULL,
                    id bigint NOT NULL,
                    member_id bigint NOT NULL,
                    point_type_id bigint NOT NULL,
                    amount integer NOT NULL,
                    type text NOT NULL,
                    source_type text NOT NULL,
                    source_id bigint,
                    PRIMARY KEY(operation_id,id)
                )
                """);
    }

    private record SignedSource(List<DisposableFinancialMappingParityTest.Row> rows,
                                Map<String,Long> balances) {}

    private SignedSource authenticatedSource(String ledgerJson) throws Exception {
        String body = """
                {"format":"family-points-export-v1","workspaceId":17,"categories":{
                  "POINT_TYPES":[{"id":3,"family_id":17,"code":"POINTS"},
                                 {"id":8,"family_id":17,"code":"TIME"}],
                  "TASK_DEFINITIONS":[{"id":77,"family_id":17}],
                  "TASK_EXECUTIONS":[{"id":42,"member_id":4,"task_definition_id":77,
                                      "reward_amount":5,"reward_point_type_id":3}],
                  "REWARD_PURCHASES":[{"id":51,"member_id":4,"point_type_id":3,
                                       "price_amount":3}],
                  "POINT_LEDGER":__LEDGER__
                }}
                """.replace("__LEDGER__",ledgerJson);
        var generator = KeyGenerator.getInstance("AES");
        generator.init(256);
        var key = generator.generateKey();
        var encrypted = codec.seal(UUID.randomUUID(),17,UUID.randomUUID(),0,
                "a".repeat(64),"V50",body.getBytes(StandardCharsets.UTF_8),key);
        byte[] archive = portable.encode(encrypted);
        var verifier = new DisposableLedgerSourceGraphVerifier(inspector,portable,codec);
        var sourceProof = verifier.inspect(archive,17,key);
        var inspected = inspector.inspect(archive,17,key);
        byte[] raw = codec.open(portable.decode(archive),inspected.snapshotId(),17,key);
        try {
            var ledger = mapper.readTree(raw).path("categories").path("POINT_LEDGER");
            var entries = new ArrayList<DisposableFinancialMappingParityTest.Row>();
            for (var row : ledger) {
                var origin = row.path("source_id");
                entries.add(new DisposableFinancialMappingParityTest.Row(
                        row.path("id").longValue(),row.path("member_id").longValue(),
                        row.path("point_type_id").longValue(),row.path("amount").intValue(),
                        row.path("type").asText(),row.path("source_type").asText(),
                        origin.isNull() ? null : origin.longValue()));
            }
            assertEquals(sourceProof.ledgerRows(),entries.size());
            return new SignedSource(List.copyOf(entries),
                    sourceProof.signedMemberCurrencyBalances());
        } finally {
            Arrays.fill(raw,(byte)0);
        }
    }

    private static DisposableFinancialMappingParityTest.Mapping mapping() {
        return new DisposableFinancialMappingParityTest.Mapping(
                Map.of(1L,101L,2L,102L,3L,103L),
                Map.of(4L,14L,5L,15L),Map.of(3L,23L,8L,28L),
                Map.of(42L,142L),Map.of(51L,151L));
    }

    private static List<DisposableFinancialMappingParityTest.Row> mappedRows() {
        return List.of(
                new DisposableFinancialMappingParityTest.Row(
                        101,14,23,5,"EARN","TASK",142L),
                new DisposableFinancialMappingParityTest.Row(
                        102,14,23,-3,"SPEND","REWARD_PURCHASE",151L),
                new DisposableFinancialMappingParityTest.Row(
                        103,15,28,7,"ADJUSTMENT","MANUAL",null));
    }

    private void write(UUID operation,List<DisposableFinancialMappingParityTest.Row> rows,
                       boolean interrupt) {
        transaction.executeWithoutResult(status -> {
            for(int i=0;i<rows.size();i++) {
                var row = rows.get(i);
                db.update("""
                        INSERT INTO qa_fin08_ledger
                          (operation_id,id,member_id,point_type_id,amount,
                           type,source_type,source_id)
                        VALUES (?,?,?,?,?,?,?,?)
                        """,operation,row.id(),row.member(),row.currency(),row.amount(),
                        row.type(),row.sourceType(),row.sourceId());
                if(interrupt && i==1)
                    throw new IllegalStateException("FIN08_INJECTED_FAILURE");
            }
        });
    }

    private List<DisposableFinancialMappingParityTest.Row> read(UUID operation) {
        return db.query("""
                SELECT id,member_id,point_type_id,amount,type,source_type,source_id
                FROM qa_fin08_ledger WHERE operation_id=? ORDER BY id
                """,(rs,index) -> new DisposableFinancialMappingParityTest.Row(
                rs.getLong("id"),rs.getLong("member_id"),
                rs.getLong("point_type_id"),rs.getInt("amount"),
                rs.getString("type"),rs.getString("source_type"),
                rs.getObject("source_id",Long.class)),operation);
    }

    private static Map<String,Long> reconcile(SignedSource signed,
            List<DisposableFinancialMappingParityTest.Row> persisted) {
        assertEquals(Map.of("4:3",2L,"5:8",7L),signed.balances());
        return DisposableFinancialMappingParityTest.reconcile(
                signed.rows(),persisted,mapping());
    }

    @Test
    void signedV50SourceMapsExactlyToPersistedPostgresFinancialHistory()
            throws Exception {
        SignedSource signed=authenticatedSource(LEDGER);
        UUID operation=UUID.randomUUID();
        write(operation,mappedRows(),false);
        assertEquals(Map.of("14:23",2L,"15:28",7L),
                reconcile(signed,read(operation)));
    }

    @Test
    void rollbackThenRetryPreservesSignedArchiveProvenance() throws Exception {
        SignedSource signed=authenticatedSource(LEDGER);
        UUID operation=UUID.randomUUID();
        assertThrows(IllegalStateException.class,
                () -> write(operation,mappedRows(),true));
        assertTrue(read(operation).isEmpty());
        write(operation,mappedRows(),false);
        assertEquals(Map.of("14:23",2L,"15:28",7L),
                reconcile(signed,read(operation)));
        assertThrows(org.springframework.dao.DataAccessException.class,
                () -> write(operation,mappedRows(),false));
        assertEquals(3,read(operation).size());
    }

    @Test
    void balancedFraudInDatabaseFailsDespiteEqualAggregate()
            throws Exception {
        SignedSource signed=authenticatedSource(LEDGER);
        UUID operation=UUID.randomUUID();
        var changed=List.of(
                new DisposableFinancialMappingParityTest.Row(
                        101,14,23,6,"EARN","TASK",142L),
                new DisposableFinancialMappingParityTest.Row(
                        102,14,23,-4,"SPEND","REWARD_PURCHASE",151L),
                mappedRows().get(2));
        write(operation,changed,false);
        assertThrows(SecurityException.class,() -> reconcile(signed,read(operation)));
    }

    @Test
    void invalidSignedSourceCannotReachDatabaseWrite() throws Exception {
        String duplicate=LEDGER.replace(
                "{\"id\":3,\"member_id\":5",
                "{\"id\":4,\"member_id\":4,\"point_type_id\":3,\"amount\":-3,"
                +"\"type\":\"SPEND\",\"source_type\":\"REWARD_PURCHASE\","
                +"\"source_id\":51},{\"id\":3,\"member_id\":5");
        assertNotEquals(LEDGER,duplicate,
                "Negative fixture must actually modify the signed source");
        UUID operation=UUID.randomUUID();
        assertThrows(SecurityException.class,() -> {
            SignedSource invalid=authenticatedSource(duplicate);
            write(operation,mappedRows(),false);
            reconcile(invalid,read(operation));
        });
        assertTrue(read(operation).isEmpty());
    }
    @Test
    void signedHistoricalRewardMismatchNeverWritesToDatabase() throws Exception {
        UUID operation=UUID.randomUUID();
        // A newly sealed fixture with a ledger amount inconsistent with its
        // signed task economic snapshot must fail before any DB write.
        String malformed=LEDGER.replace(
                "\"amount\":5",
                "\"amount\":6");
        assertNotEquals(LEDGER,malformed);
        assertThrows(SecurityException.class,()->{
            SignedSource proof=authenticatedSource(malformed);
            write(operation,mappedRows(),false);
            reconcile(proof,read(operation));
        });
        assertTrue(read(operation).isEmpty());
    }

    @Test
    void signedArchiveWithOrphanTaskNeverTouchesPostgres() throws Exception {
        String orphan=LEDGER.replace("\"source_type\":\"TASK\",\"source_id\":42",
                "\"source_type\":\"TASK\",\"source_id\":999");
        assertNotEquals(LEDGER,orphan);
        UUID operation=UUID.randomUUID();
        assertThrows(SecurityException.class,()->{
            SignedSource signed=authenticatedSource(orphan);
            write(operation,mappedRows(),false);
            reconcile(signed,read(operation));
        });
        assertTrue(read(operation).isEmpty());
    }

    @Test
    void signedArchiveWithUnknownCurrencyNeverTouchesPostgres() throws Exception {
        String foreign=LEDGER.replace(
                "\"point_type_id\":8,\"amount\":7",
                "\"point_type_id\":999,\"amount\":7");
        assertNotEquals(LEDGER,foreign);
        UUID operation=UUID.randomUUID();
        assertThrows(SecurityException.class,()->{
            SignedSource signed=authenticatedSource(foreign);
            write(operation,mappedRows(),false);
            reconcile(signed,read(operation));
        });
        assertTrue(read(operation).isEmpty());
    }

    @Test
    void unsupportedSourceTypeCannotBecomeManualFinancialHistory()
            throws Exception {
        String unsupported=LEDGER.replace(
                "\"source_type\":\"MANUAL\",\"source_id\":null",
                "\"source_type\":\"POINT_EXCHANGE\",\"source_id\":999");
        assertNotEquals(LEDGER,unsupported);
        UUID operation=UUID.randomUUID();
        assertThrows(SecurityException.class,()->{
            SignedSource signed=authenticatedSource(unsupported);
            write(operation,mappedRows(),false);
            reconcile(signed,read(operation));
        });
        assertTrue(read(operation).isEmpty());
    }

    @Test
    void validSignedArchiveRejectsForeignTargetMemberWithoutMutation()
            throws Exception {
        SignedSource signed=authenticatedSource(LEDGER);
        UUID operation=UUID.randomUUID();
        var foreign=List.of(
                mappedRows().get(0),mappedRows().get(1),
                new DisposableFinancialMappingParityTest.Row(
                        103,999,28,7,"ADJUSTMENT","MANUAL",null));
        write(operation,foreign,false);
        assertThrows(SecurityException.class,()->reconcile(signed,read(operation)));
    }

    @Test
    void wrongTargetCurrencyAndTaskParentBothFailIndependentLedgerParity()
            throws Exception {
        SignedSource signed=authenticatedSource(LEDGER);
        UUID wrongCurrency=UUID.randomUUID();
        var changedCurrency=List.of(
                new DisposableFinancialMappingParityTest.Row(
                        101,14,28,5,"EARN","TASK",142L),
                mappedRows().get(1),mappedRows().get(2));
        write(wrongCurrency,changedCurrency,false);
        assertThrows(SecurityException.class,()->reconcile(signed,read(wrongCurrency)));
        UUID wrongParent=UUID.randomUUID();
        var changedParent=List.of(
                new DisposableFinancialMappingParityTest.Row(
                        101,14,23,5,"EARN","TASK",999L),
                mappedRows().get(1),mappedRows().get(2));
        write(wrongParent,changedParent,false);
        assertThrows(SecurityException.class,()->reconcile(signed,read(wrongParent)));
    }

}
