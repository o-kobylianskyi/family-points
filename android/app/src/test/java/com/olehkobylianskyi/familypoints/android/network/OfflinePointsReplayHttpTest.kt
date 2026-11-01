package com.olehkobylianskyi.familypoints.android.network

import com.google.gson.JsonParser
import com.olehkobylianskyi.familypoints.android.data.OfflinePointOperationRequest
import com.olehkobylianskyi.familypoints.android.data.OfflineReplaySessionGate
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

class OfflinePointsReplayHttpTest {
    private lateinit var server: MockWebServer
    private lateinit var api: PointsApi
    @Before fun setUp() {
        server = MockWebServer().apply { start() }
        api = Retrofit.Builder().baseUrl(server.url("/"))
            .client(OkHttpClient.Builder().retryOnConnectionFailure(false)
                .readTimeout(250, TimeUnit.MILLISECONDS).build())
            .addConverterFactory(GsonConverterFactory.create())
            .build().create(PointsApi::class.java)
    }
    @After fun tearDown() { server.shutdown() }

    @Test fun timeoutTransmitsOriginalUuidAndEpochExactlyOnce() = runBlocking {
        val id = UUID.randomUUID().toString()
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        try {
            api.replayOffline("Bearer original", 17, 91, 21, "earn",
                OfflinePointOperationRequest(id, 1L, 3, null))
            fail("Timeout expected")
        } catch (_: IOException) { }
        val request = server.takeRequest(2, TimeUnit.SECONDS)!!
        assertEquals("POST", request.method)
        assertEquals("Bearer original", request.getHeader("Authorization"))
        assertEquals("17", request.getHeader("X-Workspace-Data-Epoch"))
        assertEquals(id, JsonParser.parseString(request.body.readUtf8()).asJsonObject
            .get("clientOperationId").asString)
        assertEquals(1, server.requestCount)
    }

    @Test fun conflictDoesNotAuthorizeQuarantineWithoutVerifiedGeneration() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(409))
        try {
            api.replayOffline("Bearer original", 17, 91, 21, "earn",
                OfflinePointOperationRequest(UUID.randomUUID().toString(), 1L, 3, null))
            fail("409 expected")
        } catch (e: HttpException) { assertEquals(409, e.code()) }
        assertFalse(OfflineReplaySessionGate.canQuarantine("original", "original", 91, 91, 17, 17))
        assertFalse(OfflineReplaySessionGate.canQuarantine("original", "switched", 91, 91, 17, 18))
        assertTrue(OfflineReplaySessionGate.canQuarantine("original", "original", 91, 91, 17, 18))
        assertEquals(1, server.requestCount)
    }
}
