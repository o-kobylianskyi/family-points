package com.olehkobylianskyi.familypoints;

import com.olehkobylianskyi.familypoints.service.*;
import org.junit.jupiter.api.Test;

import javax.crypto.KeyGenerator;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class DisposableLedgerSourceGraphVerifierTest {
    private final ProtectiveBackupArtifactCodec codec=new ProtectiveBackupArtifactCodec();
    private final ProtectiveBackupPortableFile file=new ProtectiveBackupPortableFile();
    private final ProtectiveBackupImportInspector inspector=
            new ProtectiveBackupImportInspector(file,codec);
    private final DisposableLedgerSourceGraphVerifier verifier=
            new DisposableLedgerSourceGraphVerifier(inspector,file,codec);

    private DisposableLedgerSourceGraphVerifier.Result verify(String extraRows,String sourceType,
            Long sourceId,long ledgerMember,long taskMember) throws Exception {
        return verifyEdited(extraRows,sourceType,sourceId,ledgerMember,taskMember,x->x);
    }
    private DisposableLedgerSourceGraphVerifier.Result verifyEdited(String extraRows,
            String sourceType,Long sourceId,long ledgerMember,long taskMember,
            java.util.function.UnaryOperator<String> edit) throws Exception {
        var pointLedger="{\"id\":100,\"member_id\":"+ledgerMember+
                ",\"point_type_id\":3,\"amount\":5,\"source_type\":\""+sourceType+"\""+
                (sourceId==null?",\"source_id\":null":",\"source_id\":"+sourceId)+"}";
        String body="{\"format\":\"family-points-export-v1\",\"workspaceId\":17,\"categories\":{"+
                "\"POINT_TYPES\":[{\"id\":3,\"family_id\":17,\"code\":\"POINTS\"}],"+
                "\"TASK_EXECUTIONS\":[{\"id\":42,\"member_id\":"+taskMember+
                ",\"task_definition_id\":77}],"+
                "\"TASK_DEFINITIONS\":[{\"id\":77,\"family_id\":17}],"+
                "\"POINT_LEDGER\":["+pointLedger+extraRows+"]}}";
        body=edit.apply(body);
        var generator=KeyGenerator.getInstance("AES");generator.init(256);
        var key=generator.generateKey();
        var encrypted=codec.seal(UUID.randomUUID(),17,UUID.randomUUID(),0,
                "a".repeat(64),"V50",body.getBytes(StandardCharsets.UTF_8),key);
        return verifier.inspect(file.encode(encrypted),17,key);
    }
    @Test void taskSourceRequiresSameMemberAndArchivedTask() throws Exception {
        var accepted=verify("","TASK",42L,4,4);
        assertEquals(1,accepted.taskRows());
        assertEquals(0,accepted.manualRows());
        assertEquals(42L,accepted.ledgerToTaskSource().get(100L));
        assertThrows(SecurityException.class,()->verify("","TASK",42L,4,5));
        assertThrows(SecurityException.class,()->verify("","TASK",99L,4,4));
        assertThrows(SecurityException.class,()->verify("","TASK",null,4,4));
    }
    @Test void manualSourceMustNotCarrySourceIdAndUnknownTypesRemainBlocked()
            throws Exception {
        var accepted=verify("","MANUAL",null,4,4);
        assertEquals(1,accepted.manualRows());
        assertEquals(0,accepted.taskRows());
        assertThrows(SecurityException.class,()->verify("","MANUAL",42L,4,4));
        assertThrows(SecurityException.class,()->verify("","REWARD_PURCHASE",42L,4,4));
        assertThrows(SecurityException.class,()->verify("","REFUND",42L,4,4));
    }
    @Test void taskDefinitionMustBePresentInSameFamilyAndAmountsAreBounded()
            throws Exception {
        assertThrows(SecurityException.class,()->verifyEdited(
                "","TASK",42L,4,4,
                s->s.replace("\\"task_definition_id\\":77","\\"task_definition_id\\":999")));
        assertThrows(SecurityException.class,()->verifyEdited(
                "","TASK",42L,4,4,
                s->s.replace("\\"id\\":77,\\"family_id\\":17",
                        "\\"id\\":77,\\"family_id\\":18")));
        assertThrows(SecurityException.class,()->verifyEdited(
                "","TASK",42L,4,4,
                s->s.replace("\\"amount\\":5","\\"amount\\":2147483648")));
    }

}
