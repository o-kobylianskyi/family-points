package com.olehkobylianskyi.familypoints.android.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Server availability, not Wi-Fi or internet connectivity. */
enum class ServerStatus {
    CHECKING,
    ONLINE,
    OFFLINE
}

/**
 * Shared state for every Android screen. Any backend HTTP response other than
 * a gateway-unavailable response demonstrates that the backend can be reached.
 * In particular, HTTP 401 is an authentication error, not an offline state.
 */
object ServerReachability {
    private val mutableStatus = MutableStateFlow(ServerStatus.CHECKING)
    val status: StateFlow<ServerStatus> = mutableStatus.asStateFlow()

    fun checking() {
        mutableStatus.value = ServerStatus.CHECKING
    }

    /**
     * Ordinary responses (including 200/401/403 from proxies) must never
     * mark the backend ONLINE: only the validated /api/client-health probe
     * is authoritative. A gateway error is evidence of an outage.
     */
    fun httpResponse(code: Int) {
        if (code in 502..504) {
            mutableStatus.value = ServerStatus.OFFLINE
        }
    }

    /** Only a validated response from /api/client-health may confirm the probe. */
    fun verifiedProbe() {
        mutableStatus.value = ServerStatus.ONLINE
    }

    fun transportFailure() {
        mutableStatus.value = ServerStatus.OFFLINE
    }
}
