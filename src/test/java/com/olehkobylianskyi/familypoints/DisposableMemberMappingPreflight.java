package com.olehkobylianskyi.familypoints;

import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;

/**
 * TEST-ONLY proof while BOTH independent original and target PG databases
 * are still available. Not a portable signed member catalog or restore
 * authorization: member names, roles and numeric IDs do not prove identity.
 * It validates only tenant ownership, exact source roster coverage and
 * injective caller-supplied mapping, never guesses a mapping.
 */
final class DisposableMemberMappingPreflight {
    record Member(long id,String type,String roleCode) {}
    record Result(int originalRoster,int mappedMembers) {}

    Result verify(JdbcTemplate sourceDb,JdbcTemplate targetDb,long sourceFamily,
                  long targetFamily,Map<Long,Long> mapping) {
        if(sourceDb==null || targetDb==null || sourceFamily<=0 || targetFamily<=0
                || mapping==null || mapping.isEmpty())
            throw new IllegalArgumentException("MEMBER_MAPPING_INVALID_INPUT");

        Map<Long,Member> originals=roster(sourceDb,sourceFamily);
        Map<Long,Member> targets=roster(targetDb,targetFamily);
        if(originals.isEmpty() || !mapping.keySet().equals(originals.keySet()))
            throw new SecurityException("MEMBER_MAPPING_SOURCE_ROSTER_INCOMPLETE");
        if(mapping.values().stream().anyMatch(Objects::isNull)
                || new HashSet<>(mapping.values()).size()!=mapping.size())
            throw new SecurityException("MEMBER_MAPPING_NONINJECTIVE");
        for(var entry:mapping.entrySet()) {
            Member original=originals.get(entry.getKey());
            Member target=targets.get(entry.getValue());
            if(original==null || target==null)
                throw new SecurityException("MEMBER_MAPPING_FOREIGN_FAMILY");
            if(!original.type().equals(target.type())
                    || !original.roleCode().equals(target.roleCode()))
                throw new SecurityException("MEMBER_MAPPING_ROLE_MISMATCH");
        }
        return new Result(originals.size(),mapping.size());
    }

    private static Map<Long,Member> roster(JdbcTemplate db,long family) {
        var rows=db.query("""
                SELECT m.id,m.member_type,r.code
                FROM family_members m
                JOIN family_roles r ON r.id=m.family_role_id
                WHERE m.family_id=? ORDER BY m.id
                """,(rs,n)->new Member(rs.getLong(1),rs.getString(2),rs.getString(3)),family);
        var result=new TreeMap<Long,Member>();
        for(Member row:rows) {
            if(row.id()<=0 || row.type()==null || row.roleCode()==null
                    || result.putIfAbsent(row.id(),row)!=null)
                throw new SecurityException("MEMBER_MAPPING_BAD_ROSTER");
        }
        return result;
    }
}
