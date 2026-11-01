package com.olehkobylianskyi.familypoints.android.data

/**
 * A replay response belongs to the session that originally authenticated the queue.
 * If the user switched accounts while the HTTP request was in flight, retain the
 * durable clientOperationId and do not commit or refresh that session's cache.
 */
object OfflineReplaySessionGate {
    fun matches(originalToken: String?, currentToken: String?): Boolean =
        !originalToken.isNullOrBlank() && originalToken == currentToken

    fun canQuarantine(originalToken: String?, currentToken: String?,
                      originalWorkspace: Long, receivedWorkspace: Long,
                      originalEpoch: Long, receivedEpoch: Long): Boolean =
        matches(originalToken, currentToken) &&
            originalWorkspace > 0 && originalWorkspace == receivedWorkspace &&
            originalEpoch >= 0 && receivedEpoch >= 0 &&
            originalEpoch != receivedEpoch
}
