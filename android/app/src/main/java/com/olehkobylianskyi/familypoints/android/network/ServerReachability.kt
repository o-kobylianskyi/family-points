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

    fun httpResponse(code: Int) {
        mutableStatus.value = if (code == 502 || code == 503 || code == 504) {
            ServerStatus.OFFLINE
        } else {
            ServerStatus.ONLINE
        }
    }

    fun transportFailure() {
        mutableStatus.value = ServerStatus.OFFLINE
    }
}
