package com.olehkobylianskyi.familypoints;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.olehkobylianskyi.familypoints.service.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.crypto.SecretKey;
import java.util.*;

/**
 * Test-only proof of atomic native group + owned-media COMMIT from one encrypted
 * portable file. Never available to production HTTP, reset or restore services.
 *
 * Existing isolated executors accept category-specific files. Here both receive
 * verified projections of ONE authenticated input, not separately exported
 * source snapshots. A single outer JDBC transaction commits or rolls back both.
 */
final class DisposableUnifiedGroupMediaRestoreExecutor {
    record Result(Map<Long,Long> groupIds, Map<UUID,UUID> assetIds,
                  int blobs, int privateBindings, int catalogBindings,
                  Map<Long,Long> templateIds, Map<Long,Long> workNodeIds,
                  Map<Long,Long> taskIds, Map<Long,Long> rewardIds) {}
    private final TransactionTemplate transaction;
    private final DisposableNativeRestoreExecutor groups;
    private final DisposablePrivateMediaRestoreExecutor media;
    private final DisposablePrivateTemplateRestoreExecutor templates;
    private final DisposableWorkNodeRestoreExecutor workNodes;
    private final DisposableDefinitionRestoreExecutor definitions;
    private final ProtectiveBackupPortableFile files;
    private final ProtectiveBackupArtifactCodec codec;
    private final ProtectiveBackupImportInspector inspector;
    private final ObjectMapper json=new ObjectMapper();

    DisposableUnifiedGroupMediaRestoreExecutor(JdbcTemplate db,PlatformTransactionManager manager,
            ProtectiveBackupPortableFile files,ProtectiveBackupArtifactCodec codec,
            ProtectiveBackupImportInspector inspector) {
        this.transaction=new TransactionTemplate(manager);
        this.groups=new DisposableNativeRestoreExecutor(db,manager,files,codec,inspector);
        this.media=new DisposablePrivateMediaRestoreExecutor(db,manager,inspector,files,codec);
        this.templates=new DisposablePrivateTemplateRestoreExecutor(db,manager,inspector,files,codec);
        this.workNodes=new DisposableWorkNodeRestoreExecutor(db,manager,files,codec,inspector);
        this.definitions=new DisposableDefinitionRestoreExecutor(db,manager,files,codec,inspector);
        this.files=files;this.codec=codec;this.inspector=inspector;
    }

    Result apply(byte[] unified,long source,long target,Map<Long,Long> memberIds,SecretKey key) {
        if(source<=0 || target<=0 || source==target)
            throw new IllegalArgumentException("DISPOSABLE_DISTINCT_FAMILIES_REQUIRED");
        var inspected=inspector.inspect(unified,source,key);
        boolean hasTemplates=inspected.availableCategories().contains("PRIVATE_TEMPLATES");
        boolean hasWorkNodes=inspected.availableCategories().contains("WORK_NODES");
        boolean hasTasks=inspected.availableCategories().contains("TASK_DEFINITIONS");
        boolean hasRewards=inspected.availableCategories().contains("REWARD_DEFINITIONS");
        var allowed=new TreeSet<>(List.of("MEDIA_ASSETS","MEDIA_BINDINGS","MEMBER_GROUPS"));
        if(hasTemplates)allowed.add("PRIVATE_TEMPLATES");
        if(hasWorkNodes)allowed.add("WORK_NODES");
        if(hasTasks)allowed.add("TASK_DEFINITIONS");
        if(hasRewards)allowed.add("REWARD_DEFINITIONS");
        if(!"V50".equals(inspected.schemaVersion())
                || !inspected.availableCategories().equals(List.copyOf(allowed)))
            throw new IllegalArgumentException("UNIFIED_RESTORE_SCOPE_UNSUPPORTED");
        var envelope=files.decode(unified);
        byte[] clear=codec.open(envelope,envelope.manifest().snapshotId(),source,key);
        byte[] groupFile;
        byte[] mediaFile;
        byte[] templateFile=null;
        byte[] workNodeFile=null;
        byte[] definitionFile=null;
        try {
            var root=json.readTree(clear);
            var categories=root.path("categories");
            var catalog=((ObjectNode)root.deepCopy());
            var catalogData=catalog.putObject("categories");
            catalogData.set("MEMBER_GROUPS",categories.path("MEMBER_GROUPS").deepCopy());
            var privateData=((ObjectNode)root.deepCopy()).putObject("categories");
            privateData.set("MEDIA_ASSETS",categories.path("MEDIA_ASSETS").deepCopy());
            var catalogBindings=json.createArrayNode();
            var privateBindings=json.createArrayNode();
            for(var binding:categories.path("MEDIA_BINDINGS")) {
                if(!"MEMBER_GROUP".equals(binding.path("target_type").asText()))
                    throw new IllegalArgumentException("UNIFIED_UNSUPPORTED_MEDIA_TARGET");
                if(binding.hasNonNull("asset_id") && binding.hasNonNull("catalog_key"))
                    throw new IllegalArgumentException("UNIFIED_AMBIGUOUS_MEDIA_BINDING");
                if(binding.hasNonNull("asset_id"))privateBindings.add(binding.deepCopy());
                else if(binding.path("catalog_key").isTextual())catalogBindings.add(binding.deepCopy());
                else throw new IllegalArgumentException("UNIFIED_UNRESOLVED_MEDIA_BINDING");
            }
            catalogData.set("MEDIA_BINDINGS",catalogBindings);
            privateData.set("MEDIA_BINDINGS",privateBindings);
            groupFile=project(catalog,envelope,source,key);
            var privateRoot=(ObjectNode)root.deepCopy();
            privateRoot.set("categories",privateData);
            mediaFile=project(privateRoot,envelope,source,key);
            if(hasTemplates) {
                var templateRoot=(ObjectNode)root.deepCopy();
                var templateCats=templateRoot.putObject("categories");
                templateCats.set("PRIVATE_TEMPLATES",categories.path("PRIVATE_TEMPLATES").deepCopy());
                templateFile=project(templateRoot,envelope,source,key);
            }
            if(hasWorkNodes) {
                var workRoot=(ObjectNode)root.deepCopy();
                workRoot.putObject("categories").set(
                        "WORK_NODES",categories.path("WORK_NODES").deepCopy());
                workNodeFile=project(workRoot,envelope,source,key);
            }
            if(hasTasks || hasRewards) {
                var definitionRoot=(ObjectNode)root.deepCopy();
                var definitionCats=definitionRoot.putObject("categories");
                if(hasTasks)definitionCats.set("TASK_DEFINITIONS",categories.path("TASK_DEFINITIONS").deepCopy());
                if(hasRewards)definitionCats.set("REWARD_DEFINITIONS",categories.path("REWARD_DEFINITIONS").deepCopy());
                definitionFile=project(definitionRoot,envelope,source,key);
            }
        } catch(Exception ex) {
            if(ex instanceof RuntimeException runtime)throw runtime;
            throw new IllegalArgumentException("UNIFIED_RESTORE_ARCHIVE_INVALID",ex);
        } finally {Arrays.fill(clear,(byte)0);}
        final byte[] finalTemplateFile=templateFile;
        final byte[] finalWorkNodeFile=workNodeFile;
        final byte[] finalDefinitionFile=definitionFile;
        return transaction.execute(status->{
            var groupsResult=groups.apply(groupFile,source,target,key);
            var mediaResult=media.apply(mediaFile,source,target,memberIds,groupsResult.groupIds(),key);
            var templateResult=finalTemplateFile==null?null:
                    templates.apply(finalTemplateFile,source,target,key);
            var workNodeResult=finalWorkNodeFile==null?null:
                    workNodes.apply(finalWorkNodeFile,source,target,key,hasTasks);
            var definitionResult=finalDefinitionFile==null?null:
                    definitions.apply(finalDefinitionFile,source,target,memberIds,
                            groupsResult.groupIds(),workNodeResult==null?Map.of():workNodeResult.nodeIds(),key);
            return new Result(groupsResult.groupIds(),mediaResult.assetIds(),
                    mediaResult.blobs(),mediaResult.bindings(),groupsResult.bindings(),
                    templateResult==null?Map.of():templateResult.templateIds(),
                    workNodeResult==null?Map.of():workNodeResult.nodeIds(),
                    definitionResult==null?Map.of():definitionResult.taskIds(),
                    definitionResult==null?Map.of():definitionResult.rewardIds());
        });
    }

    private byte[] project(ObjectNode root,ProtectiveBackupArtifactCodec.EncryptedArtifact envelope,
                           long source,SecretKey key) throws Exception {
        byte[] bytes=json.writeValueAsBytes(root);
        try {
            var manifest=envelope.manifest();
            return files.encode(codec.seal(UUID.randomUUID(),source,UUID.randomUUID(),
                    manifest.dataEpoch(),manifest.sourceDigest(),manifest.schemaVersion(),bytes,key));
        } finally {Arrays.fill(bytes,(byte)0);}
    }
}
