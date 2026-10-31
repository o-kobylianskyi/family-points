package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.controller.TaskScheduleSlotController;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LedgerEpochBoundaryTest {
    @Test void allLedgerExchangeAndSlotMutationsFenceBeforeDomainAccess() throws Exception {
        var guard = mock(WorkspaceEpochTransactionGuard.class);
        var stale = new IllegalStateException("stale epoch fixture");
        when(guard.requireCurrentRequestEpoch(7L)).thenThrow(stale);
        when(guard.requireCurrentRequestOrLockedEpoch(7L)).thenThrow(stale);
        int checked = 0;
        for (Class<?> type : List.of(PointService.class, PointExchangeService.class,
                ReputationService.class, TaskScheduleSlotController.class)) {
            var constructor = type.getConstructors()[0];
            var dependencies = new ArrayList<Object>();
            var arguments = new ArrayList<Object>();
            for (Class<?> parameter : constructor.getParameterTypes()) {
                Object dependency = parameter == WorkspaceEpochTransactionGuard.class ? guard : mock(parameter);
                arguments.add(dependency);
                if (dependency != guard) dependencies.add(dependency);
            }
            var target = constructor.newInstance(arguments.toArray());
            for (var method : type.getDeclaredMethods()) {
                var annotation = method.getAnnotation(Transactional.class);
                if (annotation == null || annotation.readOnly()) continue;
                Object[] inputs = new Object[method.getParameterCount()];
                inputs[0] = 7L;
                for (int i = 1; i < inputs.length; i++) {
                    var parameter = method.getParameterTypes()[i];
                    if (parameter == int.class) inputs[i] = 0;
                    else if (parameter == boolean.class) inputs[i] = false;
                    else if (parameter == long.class) inputs[i] = 0L;
                }
                var error = assertThrows(InvocationTargetException.class, () -> method.invoke(target, inputs));
                assertSame(stale, error.getCause(), type.getSimpleName() + "." + method.getName());
                checked++;
            }
            for (var dependency : dependencies) verifyNoInteractions(dependency);
        }
        assertEquals(18, checked);
    }
}
