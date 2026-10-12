package com.olehkobylianskyi.familypoints.security;

/**
 * Trusted, server-created workspace selection attached to Authentication.details.
 * Do not populate this record from a request header or client-supplied JSON.
 * Scoped JWT authentication must first validate the signed workspace claim and
 * the live account membership before constructing this object.
 */
public record WorkspaceAuthenticationDetails(Long accountId, Long workspaceId, Long memberId) {
    public WorkspaceAuthenticationDetails {
        if (accountId == null || accountId <= 0
                || workspaceId == null || workspaceId <= 0
                || memberId == null || memberId <= 0) {
            throw new IllegalArgumentException("Invalid workspace authentication details");
        }
    }
}
