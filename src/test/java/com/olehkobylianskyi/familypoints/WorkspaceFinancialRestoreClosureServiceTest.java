package com.olehkobylianskyi.familypoints;

import com.olehkobylianskyi.familypoints.service.WorkspaceFinancialRestoreClosureService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WorkspaceFinancialRestoreClosureServiceTest {
    @Test void positiveEconomicHistoryProducesExplicitNonExecutableBlockers() {
        var db=mock(JdbcTemplate.class);
        when(db.queryForObject(anyString(),eq(Long.class),eq(17L)))
                .thenAnswer(invocation->{
                    String query=invocation.getArgument(0,String.class);
                    if(query.contains("FROM families"))return 1L;
                    if(query.contains("FROM public.point_transactions"))return 3L;
                    if(query.contains("FROM public.group_point_transactions"))return 2L;
                    if(query.contains("FROM public.reward_purchases"))return 1L;
                    return 0L;
                });
        var result=new WorkspaceFinancialRestoreClosureService(db).assess(17L,
                List.of("POINT_LEDGER","REWARD_PURCHASES"));
        assertTrue(result.financialHistoryPresent());
        assertFalse(result.replaySupported());
        assertEquals(14,result.categories().size());
        assertTrue(result.blockers().contains(
                "RESTORE_DEPENDENCY_NOT_SELECTED:GROUP_POINT_LEDGER"));
        assertTrue(result.blockers().contains(
                "RESTORE_GROUP_LEDGER_REPLAY_UNIMPLEMENTED"));
        assertTrue(result.blockers().contains(
                "RESTORE_FINANCIAL_NATIVE_REPLAY_UNIMPLEMENTED:POINT_LEDGER"));
        assertTrue(result.blockers().contains(
                "RESTORE_FINANCIAL_HISTORY_RECONCILIATION_REQUIRED"));
    }

    @Test void exportableGroupHistoryStillCannotBeNativelyRestored() {
        var db=mock(JdbcTemplate.class);
        when(db.queryForObject(anyString(),eq(Long.class),eq(8L)))
                .thenAnswer(invocation->{
                    String q=invocation.getArgument(0,String.class);
                    if(q.contains("FROM families"))return 1L;
                    return q.contains("FROM public.group_point_transactions")?2L:0L;
                });
        var result=new WorkspaceFinancialRestoreClosureService(db)
                .assess(8L,List.of("GROUP_POINT_LEDGER","MEMBER_GROUPS"));
        assertTrue(result.financialHistoryPresent());
        assertFalse(result.replaySupported());
        assertTrue(result.blockers().contains(
                "RESTORE_GROUP_LEDGER_REPLAY_UNIMPLEMENTED"));
        assertTrue(result.blockers().contains(
                "RESTORE_FINANCIAL_NATIVE_REPLAY_UNIMPLEMENTED:GROUP_POINT_LEDGER"));
        assertFalse(result.blockers().stream().anyMatch(
                b->b.equals("RESTORE_DEPENDENCY_NOT_SELECTED:GROUP_POINT_LEDGER")));
    }

    @Test void zeroEconomicHistoryNeverDeclaresFinancialReplayReady() {
        var db=mock(JdbcTemplate.class);
        when(db.queryForObject(anyString(),eq(Long.class),eq(5L)))
                .thenAnswer(invocation->invocation.getArgument(0,String.class)
                        .contains("FROM families")?1L:0L);
        var result=new WorkspaceFinancialRestoreClosureService(db)
                .assess(5L,List.of("TASK_EXECUTIONS"));
        assertFalse(result.financialHistoryPresent());
        assertFalse(result.replaySupported());
        assertEquals(List.of("RESTORE_FINANCIAL_NATIVE_REPLAY_UNIMPLEMENTED:TASK_EXECUTIONS"),
                result.blockers());
    }

    @Test void unknownFamilyAndInvalidInputsFailClosed() {
        var db=mock(JdbcTemplate.class);
        var service=new WorkspaceFinancialRestoreClosureService(db);
        assertThrows(IllegalArgumentException.class,
                ()->service.assess(0L,List.of()));
        assertThrows(IllegalArgumentException.class,
                ()->service.assess(4L,Arrays.asList((String)null)));
        when(db.queryForObject(anyString(),eq(Long.class),eq(4L))).thenReturn(0L);
        assertThrows(SecurityException.class,
                ()->service.assess(4L,List.of()));
    }
}
