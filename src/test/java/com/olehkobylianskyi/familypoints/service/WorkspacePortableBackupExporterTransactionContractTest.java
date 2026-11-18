package com.olehkobylianskyi.familypoints.service;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Source-side export transaction contract. This protects standalone Spring-proxied
 * exports, not a caller that has already opened a weaker transaction.
 */
class WorkspacePortableBackupExporterTransactionContractTest {
    @Test
    void standaloneExportDeclaresRepeatableReadWithoutStartingIndependentRestoreTransaction() throws Exception {
        Method method = WorkspacePortableBackupExporter.class.getMethod(
                "export", long.class, UUID.class, UUID.class, long.class,
                Collection.class, String.class, SecretKey.class);
        Transactional annotation = method.getAnnotation(Transactional.class);
        assertNotNull(annotation, "Portable source exporter must retain a transaction boundary");
        assertEquals(Isolation.REPEATABLE_READ, annotation.isolation(),
                "READ_COMMITTED permits mixed-time member/ledger snapshots");
        assertEquals(Propagation.REQUIRED, annotation.propagation(),
                "Export must not detach itself into an independent restore transaction");
    }
}
