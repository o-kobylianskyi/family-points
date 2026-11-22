package com.olehkobylianskyi.familypoints.android.data

import com.olehkobylianskyi.familypoints.android.network.RewardPurchaseReceiptApi
import com.olehkobylianskyi.familypoints.android.network.WorkspaceEpochApi
import com.olehkobylianskyi.familypoints.android.storage.RewardPurchaseIntentStore
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.UUID

class RewardReceiptTransportMockWebServerTest {
    private val uuid = "c40fc8c0-a894-4665-a87a-1e52b7645084"
    private val original = RewardPurchaseIntentStore.Intent(
        UUID.fromString(uuid), 17, 19, 23, 29, 31, "SELF", "UNCERTAIN"
    )
    private val goodReceipt = """
        {"operationId":"$uuid","replayed":false,
         "purchase":{"id":67,"rewardDefinitionId":29,"memberId":23,
           "memberName":"Member","rewardTitle":"Prize",
           "pointTypeId":4,"pointTypeCode":"POINTS","priceAmount":6,
           "durationMinutes":null,"status":"READY","purchasedAt":null,"resolvedAt":null}}
    """.trimIndent()

    @Test fun onlyThreeReadOnlyGetsAreSentWithOriginalUuid() {
        MockWebServer().use { mock ->
            mock.start()
            mock.enqueue(MockResponse().setResponseCode(200)
                .setBody("""{"workspaceId":19,"dataEpoch":31}"""))
            mock.enqueue(MockResponse().setResponseCode(200).setBody(goodReceipt))
            mock.enqueue(MockResponse().setResponseCode(200)
                .setBody("""{"workspaceId":19,"dataEpoch":31}"""))

            val retrofit = Retrofit.Builder().baseUrl(mock.url("/"))
                .addConverterFactory(GsonConverterFactory.create()).build()
            val epoch = retrofit.create(WorkspaceEpochApi::class.java)
            val receipt = retrofit.create(RewardPurchaseReceiptApi::class.java)
            val reconciler = RewardReceiptReadOnlyReconciler(
                currentToken = { "test-jwt" },
                fetchEpoch = { auth, workspace -> epoch.getEpoch(auth, workspace) },
                fetchReceipt = { auth, workspace, id ->
                    receipt.getReceipt(auth, workspace, id)
                }
            )
            assertEquals(RewardPurchaseRecoveryState.State.CONFIRMED_PURCHASE,
                runBlocking { reconciler.inspect(original, 17, 19) })
            assertEquals(3, mock.requestCount)
            val first = mock.takeRequest()
            val middle = mock.takeRequest()
            val last = mock.takeRequest()
            assertEquals(listOf("GET", "GET", "GET"),
                listOf(first.method, middle.method, last.method))
            assertEquals("/workspaces/19/data-epoch", first.path)
            assertEquals("/workspaces/19/rewards/purchases/receipts/$uuid", middle.path)
            assertEquals("/workspaces/19/data-epoch", last.path)
            assertEquals("Bearer test-jwt", middle.getHeader("Authorization"))
        }
    }

    @Test fun http404IsNotPurchaseAuthorizationOrRetry() {
        MockWebServer().use { mock ->
            mock.start()
            mock.enqueue(MockResponse().setResponseCode(200)
                .setBody("""{"workspaceId":19,"dataEpoch":31}"""))
            mock.enqueue(MockResponse().setResponseCode(404)
                .setBody("""{"message":"ACTIVATION_RECEIPT_NOT_FOUND"}"""))
            val retrofit = Retrofit.Builder().baseUrl(mock.url("/"))
                .addConverterFactory(GsonConverterFactory.create()).build()
            val epoch = retrofit.create(WorkspaceEpochApi::class.java)
            val receipt = retrofit.create(RewardPurchaseReceiptApi::class.java)
            val reconciler = RewardReceiptReadOnlyReconciler(
                { "test-jwt" },
                { auth, workspace -> epoch.getEpoch(auth, workspace) },
                { auth, workspace, id -> receipt.getReceipt(auth, workspace, id) }
            )
            assertEquals(RewardPurchaseRecoveryState.State.NEEDS_REVIEW,
                runBlocking { reconciler.inspect(original, 17, 19) })
            assertEquals(2, mock.requestCount)
            assertTrue(mock.takeRequest().method == "GET")
            assertTrue(mock.takeRequest().method == "GET")
        }
    }
}
