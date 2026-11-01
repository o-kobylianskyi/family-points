package com.olehkobylianskyi.familypoints.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.olehkobylianskyi.familypoints.controller.WorkspaceTemplateController;
import com.olehkobylianskyi.familypoints.dto.RewardDefinitionRequest;
import com.olehkobylianskyi.familypoints.dto.TaskDefinitionCreateRequest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;

/** Durable activation identity, scoped to a family and the original client epoch. */
@Service
public class TemplateActivationReceiptService {
    private final JdbcTemplate db;
    private final WorkspaceEpochTransactionGuard epoch;
    private final WorkspaceTemplateController templates;
    private final ObjectMapper mapper=new ObjectMapper().findAndRegisterModules();

    public TemplateActivationReceiptService(JdbcTemplate db,
            WorkspaceEpochTransactionGuard epoch,WorkspaceTemplateController templates) {
        this.db=db;this.epoch=epoch;this.templates=templates;
    }
    public record Receipt(UUID operationId,Long templateId,String kind,
            Long createdDefinitionId,boolean replayed) {}
    private record Stored(Long templateId,String kind,String fingerprint,Long definitionId) {}

    @Transactional
    public Receipt activateTask(long family,long template,UUID key,TaskDefinitionCreateRequest request) {
        return activate(family,template,key,"TASK",request,
                () -> {
                    var response=templates.activateTask(family,template,key,request).getBody();
                    if(response==null || response.getId()==null)
                        throw new IllegalStateException("Missing created task identifier");
                    return response.getId();
                });
    }
    @Transactional
    public Receipt activateReward(long family,long template,UUID key,RewardDefinitionRequest request) {
        return activate(family,template,key,"REWARD",request,
                () -> {
                    var response=templates.activateReward(family,template,key,request);
                    if(response==null || response.getId()==null)
                        throw new IllegalStateException("Missing created reward identifier");
                    return response.getId();
                });
    }
    private Receipt activate(long family,long template,UUID key,String kind,Object request,
            java.util.function.Supplier<Long> creator) {
        if(key==null||request==null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"ACTIVATION_RECEIPT_INVALID");
        String fingerprint=fingerprint(kind,template,request);
        epoch.requireCurrentRequestEpoch(family);
        Stored previous=load(family,key);
        if(previous!=null) {
            if(!previous.templateId().equals(template)||!previous.kind().equals(kind)
                    ||!previous.fingerprint().equals(fingerprint))
                throw new ResponseStatusException(HttpStatus.CONFLICT,"ACTIVATION_RECEIPT_CONFLICT");
            return new Receipt(key,template,kind,previous.definitionId(),true);
        }
        // The legacy V40 claim and new V48 receipt participate in this same transaction.
        // Any failure rolls back both claims and the created definition.
        Long created=creator.get();
        db.update("""
            INSERT INTO template_activation_receipts
              (workspace_id,operation_id,template_id,kind,request_fingerprint,created_definition_id)
            VALUES (?,?,?,?,?,?)
            """,family,key,template,kind,fingerprint,created);
        return new Receipt(key,template,kind,created,false);
    }
    @Transactional(readOnly=true)
    public Receipt receipt(long family,UUID key) {
        if(key==null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"ACTIVATION_RECEIPT_INVALID");
        Stored found=load(family,key);
        if(found==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"ACTIVATION_RECEIPT_NOT_FOUND");
        return new Receipt(key,found.templateId(),found.kind(),found.definitionId(),true);
    }
    private Stored load(long family,UUID key) {
        var rows=db.query("""
            SELECT template_id,kind,request_fingerprint,created_definition_id
            FROM template_activation_receipts WHERE workspace_id=? AND operation_id=?
            """,(rs,i)->new Stored(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getLong(4)),
            family,key);
        return rows.isEmpty()?null:rows.get(0);
    }
    private String fingerprint(String kind,long template,Object request) {
        try {
            byte[] payload=mapper.writeValueAsBytes(request);
            MessageDigest digest=MessageDigest.getInstance("SHA-256");
            digest.update(kind.getBytes(StandardCharsets.UTF_8));
            digest.update((byte)0);
            digest.update(Long.toString(template).getBytes(StandardCharsets.UTF_8));
            digest.update((byte)0);
            return HexFormat.of().formatHex(digest.digest(payload));
        } catch(Exception ex) {throw new IllegalStateException("Cannot fingerprint activation request",ex);}
    }
}
