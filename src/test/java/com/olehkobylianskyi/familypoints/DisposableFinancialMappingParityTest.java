package com.olehkobylianskyi.familypoints;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Read-only restore ID mapping checks; never executes a financial write. */
class DisposableFinancialMappingParityTest {
    record Row(long id, long member, long currency, int amount,
               String type, String sourceType, Long sourceId) {}
    record Mapping(Map<Long,Long> ledger, Map<Long,Long> members,
                   Map<Long,Long> currencies, Map<Long,Long> tasks,
                   Map<Long,Long> purchases) {}

    private static long map(Map<Long,Long> ids, Long source) {
        if (ids == null || source == null || source <= 0 || !ids.containsKey(source))
            throw new SecurityException("FIN_MAPPING_MISSING");
        Long target = ids.get(source);
        if (target == null || target <= 0)
            throw new SecurityException("FIN_MAPPING_INVALID");
        return target;
    }

    private static void injective(Map<Long,Long> ids) {
        if (ids == null) throw new SecurityException("FIN_MAPPING_MISSING");
        Set<Long> seen = new HashSet<>();
        for (var entry : ids.entrySet()) {
            if (entry.getKey() == null || entry.getKey() <= 0
                    || entry.getValue() == null || entry.getValue() <= 0
                    || !seen.add(entry.getValue()))
                throw new SecurityException("FIN_MAPPING_NOT_INJECTIVE");
        }
    }

    static Map<String,Long> reconcile(List<Row> source, List<Row> target, Mapping ids) {
        if (source == null || target == null || ids == null)
            throw new SecurityException("FIN_RECONCILIATION_MISSING");
        // Explicit checks preserve a fail-closed error for absent mappings.
        injective(ids.ledger);
        injective(ids.members);
        injective(ids.currencies);
        injective(ids.tasks);
        injective(ids.purchases);
        if (source.size() != target.size() || ids.ledger.size() != source.size())
            throw new SecurityException("FIN_ROW_COUNT");
        Map<Long,Row> targetById = new HashMap<>();
        for (Row item : target) {
            if (item == null || item.id <= 0 || targetById.putIfAbsent(item.id,item) != null)
                throw new SecurityException("FIN_TARGET_ID_INVALID");
        }
        Map<String,Long> balance = new TreeMap<>();
        Set<Long> seenSource = new HashSet<>();
        Set<String> taskEvents = new HashSet<>();
        Set<Long> purchaseCharges = new HashSet<>();
        for (Row original : source) {
            if (original == null || original.id <= 0 || original.member <= 0
                    || original.currency <= 0 || original.type == null
                    || original.sourceType == null || !seenSource.add(original.id))
                throw new SecurityException("FIN_SOURCE_ID_INVALID");
            Long parent = switch (original.sourceType) {
                case "MANUAL" -> {
                    if (original.sourceId != null)
                        throw new SecurityException("FIN_MANUAL_PARENT");
                    yield null;
                }
                case "TASK" -> map(ids.tasks, original.sourceId);
                case "REWARD_PURCHASE" -> map(ids.purchases, original.sourceId);
                default -> throw new SecurityException("FIN_UNSUPPORTED_ORIGIN");
            };
            if ("TASK".equals(original.sourceType)
                    && !(original.amount > 0 && "EARN".equals(original.type)
                      || original.amount < 0 && "PENALTY".equals(original.type)))
                throw new SecurityException("FIN_TASK_ECONOMICS");
            if ("REWARD_PURCHASE".equals(original.sourceType)
                    && (original.amount >= 0 || !"SPEND".equals(original.type)))
                throw new SecurityException("FIN_PURCHASE_ECONOMICS");
            if ("TASK".equals(original.sourceType)
                    && !taskEvents.add(original.sourceId + ":" + original.type))
                throw new SecurityException("FIN_TASK_DUPLICATE_EVENT");
            if ("REWARD_PURCHASE".equals(original.sourceType)
                    && !purchaseCharges.add(original.sourceId))
                throw new SecurityException("FIN_DUPLICATE_PURCHASE_CHARGE");
            Row actual = targetById.get(map(ids.ledger, original.id));
            if (actual == null || actual.member <= 0 || actual.currency <= 0
                    || actual.type == null || actual.sourceType == null
                    || actual.member != map(ids.members,original.member)
                    || actual.currency != map(ids.currencies,original.currency)
                    || actual.amount != original.amount
                    || !original.type.equals(actual.type)
                    || !original.sourceType.equals(actual.sourceType)
                    || !Objects.equals(parent,actual.sourceId))
                throw new SecurityException("FIN_PROVENANCE_MISMATCH");
            try {
                balance.merge(actual.member+":"+actual.currency,
                        (long)original.amount, Math::addExact);
            } catch (ArithmeticException overflow) {
                throw new SecurityException("FIN_BALANCE_OVERFLOW");
            }
        }
        if (!targetById.keySet().equals(new HashSet<>(ids.ledger.values())))
            throw new SecurityException("FIN_UNMAPPED_TARGET");
        return Map.copyOf(balance);
    }

    private static List<Row> source() {
        return List.of(new Row(1,4,3,5,"EARN","TASK",42L),
                new Row(2,4,3,-3,"SPEND","REWARD_PURCHASE",51L),
                new Row(3,5,8,7,"ADJUSTMENT","MANUAL",null));
    }
    private static Mapping mapping() {
        return new Mapping(Map.of(1L,101L,2L,102L,3L,103L),
                Map.of(4L,14L,5L,15L),Map.of(3L,23L,8L,28L),
                Map.of(42L,142L),Map.of(51L,151L));
    }
    private static List<Row> target() {
        return List.of(new Row(101,14,23,5,"EARN","TASK",142L),
                new Row(102,14,23,-3,"SPEND","REWARD_PURCHASE",151L),
                new Row(103,15,28,7,"ADJUSTMENT","MANUAL",null));
    }

    @Test void positiveMappedLedgerPreservesPerMemberAndCurrencyPartition() {
        var result=reconcile(source(),target(),mapping());
        assertEquals(Map.of("14:23",2L,"15:28",7L),result);
    }
    @Test void matchingTotalCannotHideAlteredAncestry() {
        var bad=List.of(new Row(101,14,23,5,"EARN","TASK",142L),
                new Row(102,14,23,-3,"SPEND","REWARD_PURCHASE",999L),
                target().get(2));
        assertThrows(SecurityException.class,()->reconcile(source(),bad,mapping()));
    }
    @Test void balancedCompensationCannotReplaceOriginalLedgerHistory() {
        var bad=List.of(new Row(101,14,23,6,"EARN","TASK",142L),
                new Row(102,14,23,-4,"SPEND","REWARD_PURCHASE",151L),
                target().get(2));
        assertThrows(SecurityException.class,()->reconcile(source(),bad,mapping()));
    }
    @Test void mappingMustBeInjectiveAndAccountForEveryRow() {
        var duplicate=new Mapping(Map.of(1L,101L,2L,101L,3L,103L),
                mapping().members,mapping().currencies,mapping().tasks,mapping().purchases);
        assertThrows(SecurityException.class,()->reconcile(source(),target(),duplicate));
        assertThrows(SecurityException.class,()->reconcile(source(),target().subList(0,2),mapping()));
    }
    @Test void noForeignCurrencyOrMemberCanEnterMappedHistory() {
        var bad=List.of(target().get(0),target().get(1),
                new Row(103,16,28,7,"ADJUSTMENT","MANUAL",null));
        assertThrows(SecurityException.class,()->reconcile(source(),bad,mapping()));
    }
    @Test void repeatedReconciliationIsIdempotentAndDoesNotMutateSnapshots() {
        var from=List.copyOf(source());
        var to=List.copyOf(target());
        var first=reconcile(from,to,mapping());
        var second=reconcile(from,to,mapping());
        assertEquals(first,second);
        assertEquals(source(),from);
        assertEquals(target(),to);
    }

    @Test void retryWithExtraLedgerEntryFailsEvenWhenOriginalRowsRemainValid() {
        var after=new ArrayList<>(target());
        after.add(new Row(104,14,23,-2,"SPEND","REWARD_PURCHASE",151L));
        assertThrows(SecurityException.class,()->reconcile(source(),after,mapping()));
        assertEquals(Map.of("14:23",2L,"15:28",7L),
                reconcile(source(),target(),mapping()));
    }

    @Test void rollbackSnapshotRemainsValidButPartialFinancialWriteFails() {
        var baseline=target();
        var partial=List.of(target().get(0),target().get(2));
        assertThrows(SecurityException.class,()->reconcile(source(),partial,mapping()));
        assertEquals(Map.of("14:23",2L,"15:28",7L),
                reconcile(source(),baseline,mapping()));
    }

    @Test void shiftedTransactionTypeFailsEvenIfAmountAndParentsMatch() {
        var altered=List.of(new Row(101,14,23,5,"ADJUSTMENT","TASK",142L),
                target().get(1),target().get(2));
        assertThrows(SecurityException.class,()->reconcile(source(),altered,mapping()));
    }

    @Test void reversedMappingCannotPassWithIdenticalAggregateBalances() {
        var swapped=new Mapping(Map.of(1L,102L,2L,101L,3L,103L),
                mapping().members,mapping().currencies,mapping().tasks,mapping().purchases);
        assertThrows(SecurityException.class,()->reconcile(source(),target(),swapped));
    }

    @Test void missingCategoryMappingsFailClosedWithSecurityException() {
        var absentCurrencies=new Mapping(mapping().ledger,mapping().members,null,
                mapping().tasks,mapping().purchases);
        assertThrows(SecurityException.class,()->reconcile(source(),target(),absentCurrencies));
        var absentTasks=new Mapping(mapping().ledger,mapping().members,mapping().currencies,
                null,mapping().purchases);
        assertThrows(SecurityException.class,()->reconcile(source(),target(),absentTasks));
    }

    @Test void duplicateTaskEventCannotPassEvenWithDistinctLedgerRowIds() {
        var from=List.of(source().get(0),
                new Row(4,4,3,5,"EARN","TASK",42L),
                source().get(1),source().get(2));
        var to=List.of(target().get(0),
                new Row(104,14,23,5,"EARN","TASK",142L),
                target().get(1),target().get(2));
        var ids=new Mapping(Map.of(1L,101L,4L,104L,2L,102L,3L,103L),
                mapping().members,mapping().currencies,mapping().tasks,mapping().purchases);
        assertThrows(SecurityException.class,()->reconcile(from,to,ids));
    }

    @Test void duplicatePurchaseDebitCannotPassEvenWithDifferentLedgerIds() {
        var from=List.of(source().get(0),source().get(1),
                new Row(4,4,3,-3,"SPEND","REWARD_PURCHASE",51L),source().get(2));
        var to=List.of(target().get(0),target().get(1),
                new Row(104,14,23,-3,"SPEND","REWARD_PURCHASE",151L),target().get(2));
        var ids=new Mapping(Map.of(1L,101L,2L,102L,4L,104L,3L,103L),
                mapping().members,mapping().currencies,mapping().tasks,mapping().purchases);
        assertThrows(SecurityException.class,()->reconcile(from,to,ids));
    }

    @Test void nullTargetTransactionOrOriginIsRejectedWithoutNullPointer() {
        var invalidType=List.of(new Row(101,14,23,5,null,"TASK",142L),
                target().get(1),target().get(2));
        assertThrows(SecurityException.class,()->reconcile(source(),invalidType,mapping()));
        var invalidOrigin=List.of(new Row(101,14,23,5,"EARN",null,142L),
                target().get(1),target().get(2));
        assertThrows(SecurityException.class,()->reconcile(source(),invalidOrigin,mapping()));
    }


    @Test void swappingEveryPairOfLedgerMappingsIsDetected() {
        var originals=List.of(1L,2L,3L);
        for (int i=0;i<originals.size();i++) {
            for (int j=i+1;j<originals.size();j++) {
                var replacement=new HashMap<>(mapping().ledger());
                Long first=originals.get(i),second=originals.get(j);
                Long old=replacement.get(first);
                replacement.put(first,replacement.get(second));
                replacement.put(second,old);
                var mismapped=new Mapping(replacement,mapping().members(),
                        mapping().currencies(),mapping().tasks(),mapping().purchases());
                assertThrows(SecurityException.class,
                        ()->reconcile(source(),target(),mismapped),
                        "A permutation of ledger IDs must not substitute history");
            }
        }
    }

    @Test void everyLedgerFieldIsCheckedNotOnlyTheTotal() {
        var first=target().get(0);
        var variants=List.of(
                new Row(first.id(),15,first.currency(),first.amount(),
                        first.type(),first.sourceType(),first.sourceId()),
                new Row(first.id(),first.member(),28,first.amount(),
                        first.type(),first.sourceType(),first.sourceId()),
                new Row(first.id(),first.member(),first.currency(),6,
                        first.type(),first.sourceType(),first.sourceId()),
                new Row(first.id(),first.member(),first.currency(),first.amount(),
                        "PENALTY",first.sourceType(),first.sourceId()),
                new Row(first.id(),first.member(),first.currency(),first.amount(),
                        first.type(),"MANUAL",null),
                new Row(first.id(),first.member(),first.currency(),first.amount(),
                        first.type(),first.sourceType(),999L));
        for (Row variant:variants) {
            var altered=List.of(variant,target().get(1),target().get(2));
            assertThrows(SecurityException.class,
                    ()->reconcile(source(),altered,mapping()));
        }
    }

    @Test void repeatedIndependentSourceEventsCannotShareOneTaskOrPurchase() {
        var taskDuplicate=List.of(source().get(0),
                new Row(4,4,3,5,"EARN","TASK",42L),
                source().get(1),source().get(2));
        var targetDuplicate=List.of(target().get(0),
                new Row(104,14,23,5,"EARN","TASK",142L),
                target().get(1),target().get(2));
        var ids=new Mapping(Map.of(1L,101L,4L,104L,2L,102L,3L,103L),
                mapping().members(),mapping().currencies(),
                mapping().tasks(),mapping().purchases());
        assertThrows(SecurityException.class,
                ()->reconcile(taskDuplicate,targetDuplicate,ids));
        var purchaseDuplicate=List.of(source().get(0),source().get(1),
                new Row(4,4,3,-3,"SPEND","REWARD_PURCHASE",51L),
                source().get(2));
        var purchasedTarget=List.of(target().get(0),target().get(1),
                new Row(104,14,23,-3,"SPEND","REWARD_PURCHASE",151L),
                target().get(2));
        assertThrows(SecurityException.class,
                ()->reconcile(purchaseDuplicate,purchasedTarget,ids));
    }

    @Test void unknownOriginAndMissingParentRemainFailClosed() {
        var unsupported=List.of(new Row(1,4,3,5,"EARN","POINT_EXCHANGE",42L),
                source().get(1),source().get(2));
        assertThrows(SecurityException.class,()->reconcile(unsupported,target(),mapping()));
        var missing=new Mapping(mapping().ledger,mapping().members,mapping().currencies,
                Map.of(),mapping().purchases);
        assertThrows(SecurityException.class,()->reconcile(source(),target(),missing));
    }
}
