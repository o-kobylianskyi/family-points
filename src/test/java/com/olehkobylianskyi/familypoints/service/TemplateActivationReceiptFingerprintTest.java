package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.dto.RewardDefinitionRequest;
import com.olehkobylianskyi.familypoints.dto.TaskDefinitionCreateRequest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/** Regression for real Boot4/Jackson2 LocalDate fingerprint failures. */
class TemplateActivationReceiptFingerprintTest {
    private final TemplateActivationReceiptService service =
            new TemplateActivationReceiptService(null, null, null);

    private TaskDefinitionCreateRequest task(LocalDate start, LocalDate end) {
        var request = new TaskDefinitionCreateRequest();
        request.setTitle("Test task");
        request.setStartDate(start);
        request.setEndDate(end);
        request.setDueTime(LocalTime.of(18, 15));
        request.setInitialSlotTimes(java.util.List.of(LocalTime.of(9, 30)));
        return request;
    }

    @Test
    void localDatesAndTimesFingerprintDeterministicallyForOriginalUuidReplay() {
        var request = task(LocalDate.of(2026, 11, 21), LocalDate.of(2026, 12, 21));
        String first = service.fingerprint("TASK", 43L, request);
        assertNotNull(first);
        assertTrue(first.matches("[a-f0-9]{64}"));
        assertEquals(first, service.fingerprint("TASK", 43L, request));
        assertEquals(first, service.fingerprint("TASK", 43L,
                task(LocalDate.of(2026, 11, 21), LocalDate.of(2026, 12, 21))));
    }

    @Test
    void temporalPayloadAndTemplateChangesConflictByFingerprint() {
        String original = service.fingerprint("TASK", 43L,
                task(LocalDate.of(2026, 11, 21), null));
        assertNotEquals(original, service.fingerprint("TASK", 43L,
                task(LocalDate.of(2026, 11, 22), null)));
        assertNotEquals(original, service.fingerprint("TASK", 43L,
                task(LocalDate.of(2026, 11, 21), LocalDate.of(2026, 11, 30))));
        assertNotEquals(original, service.fingerprint("TASK", 44L,
                task(LocalDate.of(2026, 11, 21), null)));
    }

    @Test
    void rewardFingerprintRemainsDeterministicWithoutTemporalFields() {
        var reward = new RewardDefinitionRequest();
        reward.setTitle("Reward");
        reward.setPointTypeId(2L);
        reward.setPriceAmount(10);
        String first = service.fingerprint("REWARD", 7L, reward);
        assertEquals(first, service.fingerprint("REWARD", 7L, reward));
        assertNotEquals(first, service.fingerprint("REWARD", 8L, reward));
    }
}
