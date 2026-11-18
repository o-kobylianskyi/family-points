package com.olehkobylianskyi.familypoints.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * P1 archive-v2 data minimization boundary, orthogonal to PostgreSQL fixture
 * failures. No fields are silently redacted: suspect confidential data blocks
 * the archive before AES-GCM sealing and before manifest root construction.
 *
 * This is NOT the eventual fully reviewed column allowlist. Historical
 * business text remains encrypted and must be governed by owner-controlled
 * key distribution / recipient access before Restore is enabled.
 */
public final class PortableV2HistoryDataPolicy {
    private PortableV2HistoryDataPolicy() {}

    private static final Set<String> HISTORY=Set.of(
            "POINT_TYPES","TASK_DEFINITIONS","REWARD_DEFINITIONS",
            "TASK_EXECUTIONS","REWARD_PURCHASES","POINT_LEDGER");
    private static final Set<String> PRIVATE_KEYS=Set.of(
            "email","email_address","phone","phone_number","password",
            "password_hash","password_salt","access_token","refresh_token",
            "id_token","api_key","secret","credential","credentials",
            "provider_subject","provider_user_id","external_identity_id",
            "oauth_subject","session_id","session_token","jwt",
            "account_id","user_id","ip_address","ip","device_id",
            "authorization","proxy_authorization","cookie","set_cookie",
            "private_key","client_secret","access_key","session_key");
    private static final int MAX_TEXT_BYTES=16_384;
    private static final int MAX_DEPTH=8;
    private static final int MAX_ARRAY_ITEMS=2_000;

    /**
     * Apply to the actual selected source rows AND to authenticated decoded
     * plaintext. Reject disallowed keys at any nesting level, rather than
     * silently dropping data and manufacturing a complete-looking archive.
     */
    public static void requireAcceptable(ObjectNode categories) {
        if(categories==null)
            throw new SecurityException("PORTABLE_V2_PRIVACY_CATEGORIES_MISSING");
        for(String category:HISTORY) {
            JsonNode rows=categories.path(category);
            if(!rows.isArray()||rows.size()>MAX_ARRAY_ITEMS)
                throw new SecurityException("PORTABLE_V2_PRIVACY_CATEGORY_INVALID:"+category);
            for(JsonNode row:rows) {
                if(!row.isObject())
                    throw new SecurityException("PORTABLE_V2_PRIVACY_ROW_INVALID:"+category);
                check(row,category,0);
            }
        }
    }

    private static void check(JsonNode node,String category,int depth) {
        if(depth>MAX_DEPTH)
            throw new SecurityException("PORTABLE_V2_PRIVACY_DEPTH_EXCEEDED:"+category);
        if(node.isObject()) {
            var it=node.fields();
            while(it.hasNext()) {
                var field=it.next();
                String normalized=field.getKey().toLowerCase(Locale.ROOT);
                if(PRIVATE_KEYS.contains(normalized)
                        ||normalized.endsWith("_password")
                        ||normalized.endsWith("_token")
                        ||normalized.endsWith("_secret")
                        ||normalized.startsWith("oauth_")
                        ||normalized.endsWith("_private_key")
                        ||normalized.endsWith("_api_key"))
                    throw new SecurityException("PORTABLE_V2_PRIVACY_FORBIDDEN_FIELD:"
                            +category+":"+field.getKey());
                check(field.getValue(),category,depth+1);
            }
        } else if(node.isArray()) {
            if(node.size()>MAX_ARRAY_ITEMS)
                throw new SecurityException("PORTABLE_V2_PRIVACY_ARRAY_TOO_LARGE:"+category);
            for(JsonNode entry:node)check(entry,category,depth+1);
        } else if(node.isTextual()) {
            if(node.textValue().getBytes(StandardCharsets.UTF_8).length>MAX_TEXT_BYTES)
                throw new SecurityException("PORTABLE_V2_PRIVACY_TEXT_TOO_LARGE:"+category);
        } else if(!node.isValueNode()) {
            throw new SecurityException("PORTABLE_V2_PRIVACY_VALUE_INVALID:"+category);
        }
    }
}
