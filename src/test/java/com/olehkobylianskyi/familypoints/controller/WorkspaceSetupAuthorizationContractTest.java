package com.olehkobylianskyi.familypoints.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.lang.reflect.Method;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

/** Security metadata regression for all Issue18 write and receipt entrypoints.
 * Real SecurityFilterChain/HTTP role testing remains a separate integration gate.
 */
class WorkspaceSetupAuthorizationContractTest {
    private void managerOnly(Class<?> controller) {
        var access=controller.getAnnotation(PreAuthorize.class);
        assertNotNull(access,controller.getSimpleName()+" must be workspace-scoped");
        assertTrue(access.value().contains("canManageWorkspace(#workspaceId)"),access.value());
    }
    private Method named(Class<?> owner,String name) {
        return Arrays.stream(owner.getDeclaredMethods()).filter(m->m.getName().equals(name))
                .findFirst().orElseThrow();
    }

    @Test void setupControllerRemainsManagerScoped() {
        managerOnly(WorkspaceSetupController.class);
        managerOnly(WorkspaceSetupReviewController.class);
        managerOnly(WorkspaceSetupFinalizeController.class);
        assertNotNull(named(WorkspaceSetupFinalizeController.class,"finalizeImport").getAnnotation(PostMapping.class));
        assertNotNull(named(WorkspaceSetupFinalizeController.class,"receipt").getAnnotation(GetMapping.class));
        assertNotNull(named(WorkspaceSetupReviewController.class,"review").getAnnotation(PostMapping.class));
    }

    @Test void catalogActivationRequiresDistinctTaskAndRewardPermissions() {
        managerOnly(TemplateActivationReceiptController.class);
        var task=named(TemplateActivationReceiptController.class,"task").getAnnotation(PreAuthorize.class);
        var reward=named(TemplateActivationReceiptController.class,"reward").getAnnotation(PreAuthorize.class);
        assertNotNull(task);
        assertNotNull(reward);
        assertTrue(task.value().contains("CREATE_TASKS"));
        assertTrue(reward.value().contains("MANAGE_REWARDS"));
        assertFalse(task.value().contains("MANAGE_REWARDS"));
        assertFalse(reward.value().contains("CREATE_TASKS"));
    }

    @Test void receiptReadsAreAlwaysGuardedByManagerScope() {
        managerOnly(WorkspaceTemplateImportReceiptController.class);
        managerOnly(TemplateActivationReceiptController.class);
        assertNotNull(named(TemplateActivationReceiptController.class,"receipt").getAnnotation(GetMapping.class));
        assertNotNull(named(WorkspaceTemplateImportReceiptController.class,"receipt").getAnnotation(GetMapping.class));
    }
}
