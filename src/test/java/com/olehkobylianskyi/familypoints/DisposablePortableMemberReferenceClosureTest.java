package com.olehkobylianskyi.familypoints;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class DisposablePortableMemberReferenceClosureTest {
    private static final long FAMILY=10L;
    private static DisposablePortableMemberSourceCatalog.Catalog catalog() {
        return new DisposablePortableMemberSourceCatalog.Catalog(FAMILY,
                List.of(new DisposablePortableMemberSourceCatalog.Role(5,FAMILY,"PARENT",true)),
                List.of(
                        new DisposablePortableMemberSourceCatalog.Member(7,FAMILY,5,
                                "PARENT",true,null),
                        new DisposablePortableMemberSourceCatalog.Member(8,FAMILY,5,
                                "CHILD",false,LocalDateTime.of(2026,11,1,12,0))));
    }
    private static Map<String,Object> row(Object member,Object assigned) {
        var values=new HashMap<String,Object>();
        values.put("member_id",member);
        values.put("original_assigned_member_id",assigned);
        return values;
    }

    @Test void sourceExecutorAndHistoricalAssigneeRemainDistinct() {
        var input=Map.<String,List<? extends Map<String,?>>>of(
                "TASK_EXECUTIONS",List.of(row(7L,8L),row(8L,null)));
        assertDoesNotThrow(()->DisposablePortableMemberReferenceClosure.verify(catalog(),input));
        assertTrue(DisposablePortableMemberReferenceClosure.coveredEdges()
                .get("TASK_EXECUTIONS").stream().anyMatch(f->
                        f.column().equals("original_assigned_member_id")&&f.nullable()));
    }

    @Test void absentOptionalFieldFailsInsteadOfInventingOriginalAssignee() {
        var input=Map.<String,List<? extends Map<String,?>>>of(
                "TASK_EXECUTIONS",List.of(Map.of("member_id",7L)));
        var e=assertThrows(SecurityException.class,
                ()->DisposablePortableMemberReferenceClosure.verify(catalog(),input));
        assertEquals("SOURCE_MEMBER_EDGE_FIELD_MISSING:TASK_EXECUTIONS.original_assigned_member_id",
                e.getMessage());
    }

    @Test void foreignOrMissingMemberCannotBeSilentlyMatchedByNameOrRole() {
        var input=Map.<String,List<? extends Map<String,?>>>of(
                "TASK_EXECUTIONS",List.of(row(9L,8L)));
        assertEquals("SOURCE_MEMBER_EDGE_UNRESOLVED:TASK_EXECUTIONS.member_id",
                assertThrows(SecurityException.class,()->
                        DisposablePortableMemberReferenceClosure.verify(catalog(),input)).getMessage());
        var bad=Map.<String,List<? extends Map<String,?>>>of(
                "TASK_EXECUTIONS",List.of(row(7L,999L)));
        assertEquals("SOURCE_MEMBER_EDGE_UNRESOLVED:TASK_EXECUTIONS.original_assigned_member_id",
                assertThrows(SecurityException.class,()->
                        DisposablePortableMemberReferenceClosure.verify(catalog(),bad)).getMessage());
    }

    @Test void optionalProcessedByAndRequiredPointLedgerAreCheckedIndependently() {
        var exchange=new HashMap<String,Object>();
        exchange.put("member_id",8L);
        exchange.put("processed_by_member_id",null);
        assertDoesNotThrow(()->DisposablePortableMemberReferenceClosure.verify(catalog(),
                Map.of("POINT_EXCHANGE_REQUESTS",List.of(exchange),
                        "POINT_LEDGER",List.of(Map.of("member_id",7L)))));
        exchange.put("processed_by_member_id",11L);
        assertEquals("SOURCE_MEMBER_EDGE_UNRESOLVED:POINT_EXCHANGE_REQUESTS.processed_by_member_id",
                assertThrows(SecurityException.class,()->
                        DisposablePortableMemberReferenceClosure.verify(catalog(),
                                Map.of("POINT_EXCHANGE_REQUESTS",List.of(exchange)))).getMessage());
    }

    @Test void duplicateMemberSourceIdsAndForeignRolesAreRejected() {
        var parent=catalog().members().get(0);
        var dup=new DisposablePortableMemberSourceCatalog.Catalog(FAMILY,catalog().roles(),
                List.of(parent,parent));
        assertEquals("SOURCE_MEMBER_GRAPH_INVALID",assertThrows(SecurityException.class,()->
                DisposablePortableMemberReferenceClosure.verify(dup,Map.of())).getMessage());
        var foreign=new DisposablePortableMemberSourceCatalog.Catalog(FAMILY,
                List.of(new DisposablePortableMemberSourceCatalog.Role(5,20L,"PARENT",true)),
                List.of(parent));
        assertEquals("SOURCE_ROLE_GRAPH_INVALID",assertThrows(SecurityException.class,()->
                DisposablePortableMemberReferenceClosure.verify(foreign,Map.of())).getMessage());
    }

    @Test void unknownCategoryAndOversizedSourceRowsFailClosed() {
        assertEquals("UNREVIEWED_MEMBER_EDGE_CATEGORY:UNKNOWN",
                assertThrows(IllegalArgumentException.class,()->
                        DisposablePortableMemberReferenceClosure.verify(catalog(),
                                Map.of("UNKNOWN",List.of()))).getMessage());
        var rows=new ArrayList<Map<String,Object>>();
        for(int i=0;i<2001;i++)rows.add(row(7L,null));
        assertEquals("SOURCE_MEMBER_EDGE_ROW_LIMIT",assertThrows(IllegalStateException.class,()->
                DisposablePortableMemberReferenceClosure.verify(catalog(),
                        Map.of("TASK_EXECUTIONS",rows))).getMessage());
    }
    @Test void numericAliasingCannotSmuggleFractionalOrOverflowedSourceIdentity() {
        for(Object malicious:List.of(7.9d,7.1f,java.math.BigDecimal.valueOf(7.01),
                java.math.BigInteger.valueOf(Long.MAX_VALUE).add(java.math.BigInteger.ONE))) {
            var input=Map.<String,List<? extends Map<String,?>>>of(
                    "POINT_LEDGER",List.of(Map.of("member_id",malicious)));
            assertEquals("SOURCE_MEMBER_EDGE_UNRESOLVED:POINT_LEDGER.member_id",
                    assertThrows(SecurityException.class,()->
                            DisposablePortableMemberReferenceClosure.verify(catalog(),input))
                            .getMessage());
        }
    }

    @Test void sourceRoleCodeMustBeUniqueAndMemberTypeSupported() {
        var original=catalog();
        var duplicateCodes=new DisposablePortableMemberSourceCatalog.Catalog(FAMILY,
                List.of(new DisposablePortableMemberSourceCatalog.Role(5,FAMILY,"PARENT",true),
                        new DisposablePortableMemberSourceCatalog.Role(6,FAMILY,"PARENT",false)),
                original.members());
        assertEquals("SOURCE_ROLE_GRAPH_INVALID",
                assertThrows(SecurityException.class,()->
                        DisposablePortableMemberReferenceClosure.verify(duplicateCodes,Map.of()))
                        .getMessage());
        var unsupported=new DisposablePortableMemberSourceCatalog.Catalog(FAMILY,original.roles(),
                List.of(new DisposablePortableMemberSourceCatalog.Member(
                        7,FAMILY,5,"UNRECOGNIZED",true,null)));
        assertEquals("SOURCE_MEMBER_GRAPH_INVALID",
                assertThrows(SecurityException.class,()->
                        DisposablePortableMemberReferenceClosure.verify(unsupported,Map.of()))
                        .getMessage());
    }

    @Test void memberCatalogCannotBeOversizedEvenWithNoSelectedCategory() {
        var many=new ArrayList<DisposablePortableMemberSourceCatalog.Member>();
        for(int id=1;id<=2001;id++)
            many.add(new DisposablePortableMemberSourceCatalog.Member(
                    id,FAMILY,5,"CHILD",false,null));
        var roster=new DisposablePortableMemberSourceCatalog.Catalog(
                FAMILY,catalog().roles(),many);
        assertEquals("SOURCE_MEMBER_CATALOG_ROW_LIMIT",
                assertThrows(IllegalStateException.class,()->
                        DisposablePortableMemberReferenceClosure.verify(roster,Map.of())).getMessage());
    }

    @Test void memberExceptionPeriodsRequireOriginalSourceIdentity() {
        assertDoesNotThrow(()->DisposablePortableMemberReferenceClosure.verify(catalog(),
                Map.of("MEMBER_EXCEPTION_PERIODS",List.of(Map.of("member_id",8L)))));
        assertEquals("SOURCE_MEMBER_EDGE_UNRESOLVED:MEMBER_EXCEPTION_PERIODS.member_id",
                assertThrows(SecurityException.class,()->
                        DisposablePortableMemberReferenceClosure.verify(catalog(),
                                Map.of("MEMBER_EXCEPTION_PERIODS",
                                        List.of(Map.of("member_id",12345L))))).getMessage());
    }

}
