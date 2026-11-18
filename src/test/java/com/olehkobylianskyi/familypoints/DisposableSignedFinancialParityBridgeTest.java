package com.olehkobylianskyi.familypoints;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.olehkobylianskyi.familypoints.service.*;
import org.junit.jupiter.api.Test;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FIN-06, disposable read-only acceptance bridge. The real V50 sealed source
 * archive must pass independent source graph verification before target ID
 * mapping parity is considered. Never calls the Restore executor or writes DB.
 */
class DisposableSignedFinancialParityBridgeTest {
    private final ProtectiveBackupArtifactCodec codec = new ProtectiveBackupArtifactCodec();
    private final ProtectiveBackupPortableFile portable = new ProtectiveBackupPortableFile();
    private final ProtectiveBackupImportInspector inspector =
            new ProtectiveBackupImportInspector(portable, codec);
    private final DisposableLedgerSourceGraphVerifier sourceVerifier =
            new DisposableLedgerSourceGraphVerifier(inspector, portable, codec);
    private final ObjectMapper json = new ObjectMapper();

    private record Fixture(byte[] bytes, SecretKey key) {}

    private Fixture sealed(String ledgerContent) throws Exception {
        String body = """
                {"format":"family-points-export-v1","workspaceId":17,"categories":{
                  "POINT_TYPES":[
                    {"id":3,"family_id":17,"code":"POINTS"},
                    {"id":8,"family_id":17,"code":"TIME"}],
                  "TASK_DEFINITIONS":[{"id":77,"family_id":17}],
                  "TASK_EXECUTIONS":[
                    {"id":42,"member_id":4,"task_definition_id":77,
                     "reward_amount":5,"reward_point_type_id":3}],
                  "REWARD_PURCHASES":[
                    {"id":51,"member_id":4,"point_type_id":3,"price_amount":3}],
                  "POINT_LEDGER":__LEDGER__
                }}
                """.replace("__LEDGER__", ledgerContent);
        var generator = KeyGenerator.getInstance("AES");
        generator.init(256);
        SecretKey key = generator.generateKey();
        var encrypted = codec.seal(UUID.randomUUID(), 17, UUID.randomUUID(), 0,
                "a".repeat(64), "V50", body.getBytes(StandardCharsets.UTF_8), key);
        return new Fixture(portable.encode(encrypted), key);
    }

    private static final String VALID_LEDGER = """
            [{"id":1,"member_id":4,"point_type_id":3,"amount":5,
              "type":"EARN","source_type":"TASK","source_id":42},
             {"id":2,"member_id":4,"point_type_id":3,"amount":-3,
              "type":"SPEND","source_type":"REWARD_PURCHASE","source_id":51},
             {"id":3,"member_id":5,"point_type_id":8,"amount":7,
              "type":"ADJUSTMENT","source_type":"MANUAL","source_id":null}]
            """;

    private List<DisposableFinancialMappingParityTest.Row> authenticatedSource(Fixture fixture)
            throws Exception {
        var proof = sourceVerifier.inspect(fixture.bytes(), 17, fixture.key());
        assertEquals(3, proof.ledgerRows());
        assertEquals(Map.of("4:3", 2L, "5:8", 7L),
                proof.signedMemberCurrencyBalances());
        var inspection = inspector.inspect(fixture.bytes(), 17, fixture.key());
        byte[] raw = codec.open(portable.decode(fixture.bytes()),
                inspection.snapshotId(), 17, fixture.key());
        try {
            JsonNode ledger = json.readTree(raw).path("categories").path("POINT_LEDGER");
            assertTrue(ledger.isArray());
            var result = new ArrayList<DisposableFinancialMappingParityTest.Row>();
            for (JsonNode row : ledger) {
                JsonNode parent = row.path("source_id");
                result.add(new DisposableFinancialMappingParityTest.Row(
                        row.path("id").asLong(), row.path("member_id").asLong(),
                        row.path("point_type_id").asLong(), row.path("amount").asInt(),
                        row.path("type").asText(), row.path("source_type").asText(),
                        parent.isNull() ? null : parent.asLong()));
            }
            return List.copyOf(result);
        } finally {
            Arrays.fill(raw, (byte) 0);
        }
    }

    private static DisposableFinancialMappingParityTest.Mapping mapping() {
        return new DisposableFinancialMappingParityTest.Mapping(
                Map.of(1L,101L,2L,102L,3L,103L),
                Map.of(4L,14L,5L,15L),Map.of(3L,23L,8L,28L),
                Map.of(42L,142L),Map.of(51L,151L));
    }

    private static List<DisposableFinancialMappingParityTest.Row> target() {
        return List.of(
                new DisposableFinancialMappingParityTest.Row(
                        101,14,23,5,"EARN","TASK",142L),
                new DisposableFinancialMappingParityTest.Row(
                        102,14,23,-3,"SPEND","REWARD_PURCHASE",151L),
                new DisposableFinancialMappingParityTest.Row(
                        103,15,28,7,"ADJUSTMENT","MANUAL",null));
    }

    @Test void signedSourceAndMappedTargetHaveIdenticalAncestryAndBalances()
            throws Exception {
        var fixture = sealed(VALID_LEDGER);
        var original = authenticatedSource(fixture);
        assertEquals(Map.of("14:23",2L,"15:28",7L),
                DisposableFinancialMappingParityTest.reconcile(
                        original,target(),mapping()));
    }

    @Test void signedSourceCannotConcealDuplicatePurchaseDebit() throws Exception {
        String doubled = VALID_LEDGER.replace(
                "{\"id\":3,\"member_id\":5",
                "{\"id\":4,\"member_id\":4,\"point_type_id\":3,"
                +"\"amount\":-3,\"type\":\"SPEND\","
                +"\"source_type\":\"REWARD_PURCHASE\",\"source_id\":51},"
                +"{\"id\":3,\"member_id\":5");
        var fixture = sealed(doubled);
        assertThrows(SecurityException.class, () -> authenticatedSource(fixture));
    }

    @Test void targetCannotReplaceArchivedDebitWithCompensatingCredit()
            throws Exception {
        var original = authenticatedSource(sealed(VALID_LEDGER));
        var altered = List.of(target().get(0),
                new DisposableFinancialMappingParityTest.Row(
                        102,14,23,-4,"SPEND","REWARD_PURCHASE",151L),
                new DisposableFinancialMappingParityTest.Row(
                        103,15,28,7,"ADJUSTMENT","MANUAL",null));
        assertThrows(SecurityException.class, () ->
                DisposableFinancialMappingParityTest.reconcile(
                        original,altered,mapping()));
    }

    @Test void unrecognizedFinancialOriginIsRejectedBeforeTargetMapping()
            throws Exception {
        var fixture = sealed(VALID_LEDGER.replace(
                "\"source_type\":\"TASK\"",
                "\"source_type\":\"POINT_EXCHANGE\""));
        assertThrows(SecurityException.class, () -> authenticatedSource(fixture));
    }
}
