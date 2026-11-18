package com.olehkobylianskyi.familypoints;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class DisposablePortableFinancialSourceClosureTest {
    private static final long FAMILY=17;
    private static Map<String,Object> parent(long id) {
        return Map.of("id",id,"family_id",FAMILY);
    }
    private static Map<String,Object> row(Object... values) {
        var m=new HashMap<String,Object>();
        for(int i=0;i<values.length;i+=2)m.put((String)values[i],values[i+1]);
        return m;
    }

    @Test void acceptsCurrencyDenominatedLedgerAndGroupAncestry() {
        var rows=Map.<String,List<? extends Map<String,?>>>of(
                "POINT_TYPES",List.of(parent(1)),
                "MEMBER_GROUPS",List.of(parent(2)),
                "GROUP_POINT_LEDGER",List.of(row("group_id",2L,"point_type_id",1L)),
                "POINT_LEDGER",List.of(row("point_type_id",1L)),
                "GROUP_MEMBERSHIPS",List.of(row("member_group_id",2L)));
        assertDoesNotThrow(()->DisposablePortableFinancialSourceClosure.verify(FAMILY,rows));
    }

    @Test void deniesCrossFamilyParentEvenWhenIdsLookValid() {
        var foreign=Map.<String,List<? extends Map<String,?>>>of(
                "POINT_TYPES",List.of(Map.of("id",1L,"family_id",19L)),
                "POINT_LEDGER",List.of(row("point_type_id",1L)));
        assertEquals("SOURCE_FINANCE_PARENT_FOREIGN_WORKSPACE:POINT_TYPES",
                assertThrows(SecurityException.class,()->
                        DisposablePortableFinancialSourceClosure.verify(FAMILY,foreign)).getMessage());
    }

    @Test void missingCurrencyOrGroupFailsClosed() {
        var missingCurrency=Map.<String,List<? extends Map<String,?>>>of(
                "GROUP_POINT_LEDGER",List.of(row("group_id",2L,"point_type_id",1L)),
                "MEMBER_GROUPS",List.of(parent(2)));
        assertEquals("SOURCE_FINANCE_CATEGORY_MISSING:POINT_TYPES",
                assertThrows(SecurityException.class,()->
                        DisposablePortableFinancialSourceClosure.verify(FAMILY,missingCurrency))
                        .getMessage());
        var missingGroup=Map.<String,List<? extends Map<String,?>>>of(
                "POINT_TYPES",List.of(parent(1)),
                "GROUP_POINT_LEDGER",List.of(row("group_id",999L,"point_type_id",1L)),
                "MEMBER_GROUPS",List.of(parent(2)));
        assertEquals("SOURCE_FINANCE_PARENT_MISSING:GROUP_POINT_LEDGER.group_id",
                assertThrows(SecurityException.class,()->
                        DisposablePortableFinancialSourceClosure.verify(FAMILY,missingGroup))
                        .getMessage());
    }

    @Test void historicalTaskCurrencyNullIsExplicitAndNeverInvented() {
        var good=Map.<String,List<? extends Map<String,?>>>of(
                "TASK_DEFINITIONS",List.of(row("id",3L,"family_id",FAMILY,
                        "reward_point_type_id",null,"penalty_point_type_id",null)),
                "TASK_EXECUTIONS",List.of(row("task_definition_id",3L,
                        "reward_point_type_id",null,"penalty_point_type_id",null)));
        assertDoesNotThrow(()->DisposablePortableFinancialSourceClosure.verify(FAMILY,good));
        var absent=Map.<String,List<? extends Map<String,?>>>of(
                "TASK_DEFINITIONS",List.of(row("id",3L,"family_id",FAMILY,
                        "reward_point_type_id",null,"penalty_point_type_id",null)),
                "TASK_EXECUTIONS",List.of(row("task_definition_id",3L,
                        "reward_point_type_id",null)));
        assertEquals("SOURCE_FINANCE_FIELD_MISSING:TASK_EXECUTIONS.penalty_point_type_id",
                assertThrows(SecurityException.class,()->
                        DisposablePortableFinancialSourceClosure.verify(FAMILY,absent))
                        .getMessage());
    }

    @Test void historicalPurchaseMustRetainBothProductAndCurrencyParents() {
        var good=Map.<String,List<? extends Map<String,?>>>of(
                "POINT_TYPES",List.of(parent(1)),
                "REWARD_DEFINITIONS",List.of(row("id",4L,"family_id",FAMILY,"point_type_id",1L)),
                "REWARD_PURCHASES",List.of(row("reward_definition_id",4L,"point_type_id",1L)));
        assertDoesNotThrow(()->DisposablePortableFinancialSourceClosure.verify(FAMILY,good));
        var missing=Map.<String,List<? extends Map<String,?>>>of(
                "POINT_TYPES",List.of(parent(1)),
                "REWARD_DEFINITIONS",List.of(row("id",4L,"family_id",FAMILY,"point_type_id",1L)),
                "REWARD_PURCHASES",List.of(row("reward_definition_id",4L,"point_type_id",999L)));
        assertEquals("SOURCE_FINANCE_PARENT_MISSING:REWARD_PURCHASES.point_type_id",
                assertThrows(SecurityException.class,()->
                        DisposablePortableFinancialSourceClosure.verify(FAMILY,missing)).getMessage());
    }

    @Test void malformedOrFractionalReferencesAndUnknownCategoryDenied() {
        var wrong=Map.<String,List<? extends Map<String,?>>>of(
                "POINT_TYPES",List.of(parent(1)),
                "POINT_LEDGER",List.of(row("point_type_id",1.9)));
        assertEquals("SOURCE_FINANCE_ID_INVALID:POINT_LEDGER.point_type_id",
                assertThrows(SecurityException.class,()->
                        DisposablePortableFinancialSourceClosure.verify(FAMILY,wrong)).getMessage());
        assertEquals("UNREVIEWED_FINANCE_SOURCE_CATEGORY:UNKNOWN_LEDGER",
                assertThrows(IllegalArgumentException.class,()->
                        DisposablePortableFinancialSourceClosure.verify(FAMILY,
                                Map.of("UNKNOWN_LEDGER",List.of()))).getMessage());
    }

    @Test void duplicateSourceParentIdsAreRejected() {
        var rows=Map.<String,List<? extends Map<String,?>>>of(
                "POINT_TYPES",List.of(parent(1),parent(1)));
        assertEquals("SOURCE_FINANCE_PARENT_DUPLICATE:POINT_TYPES",
                assertThrows(SecurityException.class,()->
                        DisposablePortableFinancialSourceClosure.verify(FAMILY,rows)).getMessage());
    }
    @Test void nullablePhysicalPurchaseParentCannotBypassStrictSourceGraph() {
        // PostgreSQL may permit nullable reward_definition_id, but a portable
        // replay cannot infer the historical product from its current title.
        var missing=new HashMap<String,Object>();
        missing.put("reward_definition_id",null);
        missing.put("point_type_id",1L);
        var rows=Map.<String,List<? extends Map<String,?>>>of(
                "POINT_TYPES",List.of(parent(1)),
                "REWARD_DEFINITIONS",List.of(
                        row("id",4L,"family_id",FAMILY,"point_type_id",1L)),
                "REWARD_PURCHASES",List.of(missing));
        assertEquals("SOURCE_FINANCE_ID_INVALID:REWARD_PURCHASES.reward_definition_id",
                assertThrows(SecurityException.class,()->
                        DisposablePortableFinancialSourceClosure.verify(FAMILY,rows)).getMessage());
    }

    @Test void pointCurrencyParentIsRequiredEvenWhenNoOriginalCurrencyCatalogProvided() {
        var rows=Map.<String,List<? extends Map<String,?>>>of(
                "POINT_LEDGER",List.of(Map.of("point_type_id",1L)));
        assertEquals("SOURCE_FINANCE_CATEGORY_MISSING:POINT_TYPES",
                assertThrows(SecurityException.class,()->
                        DisposablePortableFinancialSourceClosure.verify(FAMILY,rows)).getMessage());
    }

}
