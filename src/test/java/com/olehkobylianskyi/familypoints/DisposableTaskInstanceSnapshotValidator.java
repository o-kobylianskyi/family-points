package com.olehkobylianskyi.familypoints;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Backend B / B-B-06: test-only historical task-instance snapshot contract.
 * Neither reconstructs missing fields nor authorizes restore. Rejects
 * ambiguous legacy source rows; producer-side vNext contract belongs to A.
 */
final class DisposableTaskInstanceSnapshotValidator {
    record Snapshot(long sourceId,long sourceTask,long sourceMember,Long originalAssignedMember,
                    LocalDate scheduledDate,String slotKey,String title,String description,
                    boolean mandatory,LocalTime dueTime,String status,LocalDateTime claimedAt,
                    LocalDateTime startedAt,LocalDateTime completedAt,
                    String excuseReason,String excuseComment,
                    Integer rewardAmount,Long rewardPointType,
                    Integer penaltyAmount,Long penaltyPointType,
                    Integer rewardReputationAmount,Integer penaltyReputationAmount,
                    boolean rewardProcessed,boolean penaltyProcessed) {}
    private static final Set<String> STATUSES=Set.of("PENDING","IN_PROGRESS","PAUSED",
            "WAITING_APPROVAL","COMPLETED","MISSED","EXCUSED","RELEASED","CANCELLED");
    private static final Set<String> EXCUSES=Set.of("ILLNESS","FAMILY_EVENT",
            "TRAVEL","PARENT_DECISION","OTHER","EXCEPTION_PERIOD");
    private DisposableTaskInstanceSnapshotValidator() {}

    static Snapshot parse(JsonNode row,Map<Long,Long> members,Map<Long,Long> tasks,
                          Map<Long,Long> currencies) {
        long id=positiveId(row,"id");
        long task=positiveId(row,"task_definition_id");
        long member=positiveId(row,"member_id");
        if(!tasks.containsKey(task)||!members.containsKey(member))
            throw new SecurityException("RESTORE_TASK_SNAPSHOT_PARENT_MISSING");
        Long assigned=optionalId(row,"original_assigned_member_id");
        if(assigned!=null&&!members.containsKey(assigned))
            throw new SecurityException("RESTORE_TASK_ASSIGNEE_MISSING");
        String date=requiredText(row,"scheduled_date",10);
        LocalDate scheduled;
        try {scheduled=LocalDate.parse(date);}
        catch(DateTimeParseException e){throw new SecurityException("RESTORE_TASK_BAD_DATE",e);}
        String slot=requiredText(row,"slot_key",40);
        if(!slot.matches("[a-zA-Z0-9_-]{1,40}"))
            throw new SecurityException("RESTORE_TASK_BAD_SLOT");
        String title=requiredText(row,"title",200);
        String description=optionalText(row,"description",1000);
        if(!row.path("mandatory").isBoolean()
                || !row.path("reward_processed").isBoolean()
                || !row.path("penalty_processed").isBoolean())
            throw new SecurityException("RESTORE_TASK_SNAPSHOT_FLAGS_MISSING");
        String status=requiredText(row,"status",30);
        if(!STATUSES.contains(status))
            throw new SecurityException("RESTORE_TASK_BAD_STATUS");
        // vNext source contract: explicit nullable fields, not omission that
        // might silently reset a historic attribute during target replay.
        for(String field:List.of("original_assigned_member_id","description",
                "due_time","claimed_at","started_at","completed_at",
                "excuse_reason","excuse_comment",
                "reward_amount","reward_point_type_id","penalty_amount",
                "penalty_point_type_id","reward_reputation_amount",
                "penalty_reputation_amount"))
            if(!row.has(field))
                throw new SecurityException("RESTORE_TASK_SNAPSHOT_FIELD_MISSING:"+field);
        var due=optionalTime(row,"due_time");
        var claimed=optionalDateTime(row,"claimed_at");
        var started=optionalDateTime(row,"started_at");
        var completed=optionalDateTime(row,"completed_at");
        String excuse=optionalText(row,"excuse_reason",30);
        String comment=optionalText(row,"excuse_comment",500);
        if(excuse!=null&&!EXCUSES.contains(excuse)
                ||"EXCUSED".equals(status)&&excuse==null)
            throw new SecurityException("RESTORE_TASK_EXCUSE_INVALID");
        if("COMPLETED".equals(status)&&completed==null)
            throw new SecurityException("RESTORE_TASK_COMPLETION_TIMESTAMP_MISSING");
        if(completed!=null && started!=null && completed.isBefore(started))
            throw new SecurityException("RESTORE_TASK_TIME_ORDER");
        Integer reward=optionalPositive(row,"reward_amount");
        Long rewardType=optionalId(row,"reward_point_type_id");
        Integer penalty=optionalPositive(row,"penalty_amount");
        Long penaltyType=optionalId(row,"penalty_point_type_id");
        if((reward==null)!=(rewardType==null)||(penalty==null)!=(penaltyType==null))
            throw new SecurityException("RESTORE_TASK_ECONOMY_PAIR_INCOMPLETE");
        if(rewardType!=null&&!currencies.containsKey(rewardType)
                ||penaltyType!=null&&!currencies.containsKey(penaltyType))
            throw new SecurityException("RESTORE_TASK_ECONOMY_CURRENCY_MISSING");
        Integer rewardRep=optionalPositive(row,"reward_reputation_amount");
        Integer penaltyRep=optionalPositive(row,"penalty_reputation_amount");
        if(row.path("reward_processed").asBoolean()&&reward==null&&rewardRep==null
                ||row.path("penalty_processed").asBoolean()&&penalty==null&&penaltyRep==null)
            throw new SecurityException("RESTORE_TASK_PROCESSED_WITHOUT_ECONOMY");
        return new Snapshot(id,task,member,assigned,scheduled,slot,title,description,
                row.path("mandatory").asBoolean(),due,status,claimed,started,completed,
                excuse,comment,reward,rewardType,penalty,penaltyType,
                rewardRep,penaltyRep,row.path("reward_processed").asBoolean(),
                row.path("penalty_processed").asBoolean());
    }

    static void verifyLedgerEconomy(JsonNode rows,Map<Long,Snapshot> snapshots) {
        var paid=new HashSet<String>();
        for(JsonNode row:rows) {
            if(!"TASK".equals(row.path("source_type").asText()))continue;
            long source=positiveId(row,"source_id");
            Snapshot snapshot=snapshots.get(source);
            if(snapshot==null ||positiveId(row,"member_id")!=snapshot.sourceMember())
                throw new SecurityException("RESTORE_TASK_LEDGER_OWNER_OR_SOURCE");
            String kind=row.path("type").asText();
            if(!Set.of("EARN","PENALTY").contains(kind)
                    ||!paid.add(source+":"+kind)
                    ||!row.path("amount").canConvertToInt())
                throw new SecurityException("RESTORE_TASK_LEDGER_KIND_OR_DUPLICATE");
            int amount=row.path("amount").asInt();
            long currency=positiveId(row,"point_type_id");
            if("EARN".equals(kind)
                    ? !snapshot.rewardProcessed()||snapshot.rewardAmount()==null
                        ||amount!=snapshot.rewardAmount()
                        ||currency!=snapshot.rewardPointType()
                    : !snapshot.penaltyProcessed()||snapshot.penaltyAmount()==null
                        ||amount!=-snapshot.penaltyAmount()
                        ||currency!=snapshot.penaltyPointType())
                throw new SecurityException("RESTORE_TASK_LEDGER_HISTORIC_MISMATCH");
        }
        for(Snapshot snapshot:snapshots.values()) {
            if(snapshot.rewardProcessed()&&snapshot.rewardAmount()!=null
                    &&!paid.contains(snapshot.sourceId()+":EARN")
                    ||snapshot.penaltyProcessed()&&snapshot.penaltyAmount()!=null
                    &&!paid.contains(snapshot.sourceId()+":PENALTY"))
                throw new SecurityException("RESTORE_TASK_PROCESSED_LEDGER_MISSING");
        }
    }

    private static long positiveId(JsonNode row,String name) {
        JsonNode value=row.path(name);
        if(!value.isIntegralNumber()||value.asLong()<=0)
            throw new SecurityException("RESTORE_TASK_INVALID_ID:"+name);
        return value.asLong();
    }
    private static Long optionalId(JsonNode row,String name) {
        if(!row.hasNonNull(name))return null;
        return positiveId(row,name);
    }
    private static String requiredText(JsonNode row,String name,int max) {
        var node=row.path(name);
        if(!node.isTextual()||node.asText().isBlank()||node.asText().length()>max)
            throw new SecurityException("RESTORE_TASK_BAD_TEXT:"+name);
        return node.asText();
    }
    private static String optionalText(JsonNode row,String name,int max) {
        if(!row.hasNonNull(name))return null;
        if(!row.path(name).isTextual()||row.path(name).asText().length()>max)
            throw new SecurityException("RESTORE_TASK_BAD_OPTIONAL_TEXT:"+name);
        return row.path(name).asText();
    }
    private static LocalDateTime optionalDateTime(JsonNode row,String name) {
        if(!row.hasNonNull(name))return null;
        try{return LocalDateTime.parse(requiredText(row,name,40));}
        catch(DateTimeParseException e){throw new SecurityException("RESTORE_TASK_BAD_DATETIME:"+name,e);}
    }
    private static LocalTime optionalTime(JsonNode row,String name) {
        if(!row.hasNonNull(name))return null;
        try{return LocalTime.parse(requiredText(row,name,18));}
        catch(DateTimeParseException e){throw new SecurityException("RESTORE_TASK_BAD_TIME:"+name,e);}
    }
    private static Integer optionalPositive(JsonNode row,String name) {
        if(!row.hasNonNull(name))return null;
        if(!row.path(name).canConvertToInt()||row.path(name).asInt()<=0)
            throw new SecurityException("RESTORE_TASK_INVALID_AMOUNT:"+name);
        return row.path(name).asInt();
    }
}
