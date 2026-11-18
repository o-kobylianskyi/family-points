package com.olehkobylianskyi.familypoints;

import com.olehkobylianskyi.familypoints.service.*;
import org.junit.jupiter.api.Test;

import javax.crypto.KeyGenerator;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

/** FIN-16: authenticated historical task currency must never wrap asLong(). */
class DisposableLedgerCurrencyOverflowTest {
    private final ProtectiveBackupArtifactCodec codec=new ProtectiveBackupArtifactCodec();
    private final ProtectiveBackupPortableFile portable=new ProtectiveBackupPortableFile();
    private final ProtectiveBackupImportInspector inspector=
            new ProtectiveBackupImportInspector(portable,codec);
    private final DisposableLedgerSourceGraphVerifier verifier=
            new DisposableLedgerSourceGraphVerifier(inspector,portable,codec);

    private void inspectWithTask(String taskRow) throws Exception {
        String body="{\"format\":\"family-points-export-v1\",\"workspaceId\":17,"
                +"\"categories\":{"
                +"\"POINT_TYPES\":[{\"id\":3,\"family_id\":17,\"code\":\"POINTS\"}],"
                +"\"TASK_DEFINITIONS\":[{\"id\":77,\"family_id\":17}],"
                +"\"TASK_EXECUTIONS\":["+taskRow+"],"
                +"\"POINT_LEDGER\":[{\"id\":100,\"member_id\":4,"
                +"\"point_type_id\":3,\"amount\":5,\"type\":\"EARN\","
                +"\"source_type\":\"TASK\",\"source_id\":42}]}}";
        var generator=KeyGenerator.getInstance("AES");
        generator.init(256);
        var key=generator.generateKey();
        var sealed=codec.seal(UUID.randomUUID(),17,UUID.randomUUID(),0,
                "a".repeat(64),"V50",body.getBytes(StandardCharsets.UTF_8),key);
        verifier.inspect(portable.encode(sealed),17,key);
    }

    @Test void positiveOverflowCannotAliasRealCurrencyOnChargedTask()
            throws Exception {
        // 2^64+3 would truncate to the valid currency id 3.
        assertThrows(RuntimeException.class,()->inspectWithTask(
                "{\"id\":42,\"member_id\":4,\"task_definition_id\":77,"
                +"\"reward_amount\":5,\"reward_point_type_id\":18446744073709551619}"));
    }

    @Test void overflowInUnusedTaskIsStillRejected() throws Exception {
        // Source-ledger row 100 charges task 42; task 99 has no ledger row.
        String tasks="{\"id\":42,\"member_id\":4,\"task_definition_id\":77},"
                +"{\"id\":99,\"member_id\":4,\"task_definition_id\":77,"
                +"\"penalty_amount\":7,\"penalty_point_type_id\":18446744073709551619}";
        assertThrows(RuntimeException.class,()->inspectWithTask(tasks));
    }

    @Test void negativeOverflowCannotAliasRealCurrencyOnChargedTask()
            throws Exception {
        // -2^64+3 also truncates to 3 without explicit range checking.
        assertThrows(RuntimeException.class,()->inspectWithTask(
                "{\"id\":42,\"member_id\":4,\"task_definition_id\":77,"
                +"\"reward_amount\":5,\"reward_point_type_id\":-18446744073709551613}"));
    }
}
