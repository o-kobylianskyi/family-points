package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.dto.RewardDefinitionRequest;
import com.olehkobylianskyi.familypoints.dto.TaskDefinitionCreateRequest;
import com.olehkobylianskyi.familypoints.service.TemplateActivationReceiptService;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

/** Additive explicit activation: existing activation routes retain their behavior. */
@RestController
@RequestMapping("/workspaces/{workspaceId}/templates")
@PreAuthorize("@workspaceSecurity.canManageWorkspace(#workspaceId)")
public class TemplateActivationReceiptController {
    private final TemplateActivationReceiptService service;
    public TemplateActivationReceiptController(TemplateActivationReceiptService service) {
        this.service=service;
    }

    @PostMapping("/{templateId}/activate-task/idempotent")
    @PreAuthorize("hasAuthority('CREATE_TASKS')")
    public ResponseEntity<TemplateActivationReceiptService.Receipt> task(
            @PathVariable Long workspaceId,@PathVariable Long templateId,
            @RequestHeader("Idempotency-Key") UUID key,
            @Valid @RequestBody TaskDefinitionCreateRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(service.activateTask(workspaceId,templateId,key,request));
    }

    @PostMapping("/{templateId}/activate-reward/idempotent")
    @PreAuthorize("hasAuthority('MANAGE_REWARDS')")
    public ResponseEntity<TemplateActivationReceiptService.Receipt> reward(
            @PathVariable Long workspaceId,@PathVariable Long templateId,
            @RequestHeader("Idempotency-Key") UUID key,
            @Valid @RequestBody RewardDefinitionRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(service.activateReward(workspaceId,templateId,key,request));
    }

    @GetMapping("/activations/receipts/{operationId}")
    public ResponseEntity<TemplateActivationReceiptService.Receipt> receipt(
            @PathVariable Long workspaceId,@PathVariable UUID operationId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(service.receipt(workspaceId,operationId));
    }
}
