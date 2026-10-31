package com.olehkobylianskyi.familypoints.android.storage

import android.content.Context

/** Call only after Backend confirms completed reset for the authenticated workspace.
 * Pending offline point operations are retained, never dropped or automatically replayed.
 */
class WorkspaceResetCacheCoordinator(context: Context) {
    private val appContext = context.applicationContext

    fun quarantineAndInvalidate(workspaceId: Long) {
        require(workspaceId > 0)
        OfflinePointsQueueStore(appContext).use { it.holdForReset(workspaceId) }
        OfflineBalanceStore(appContext).use { it.clearWorkspace(workspaceId) }
        OfflineMembersStore(appContext).clearWorkspace(workspaceId)
        OfflinePointTypesStore(appContext).clearWorkspace(workspaceId)
        OfflineTasksStore(appContext).clearWorkspace(workspaceId)
        OfflineRewardsStore(appContext).clearWorkspace(workspaceId)
    }
}
