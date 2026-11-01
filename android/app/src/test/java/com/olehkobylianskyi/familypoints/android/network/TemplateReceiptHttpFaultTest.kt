package com.olehkobylianskyi.familypoints.android.network

import com.google.gson.JsonParser
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

/** Real HTTP requests on localhost, no DEV server, no family mutations. */
class TemplateReceiptHttpFaultTest {
    private lateinit var server: MockWebServer
    private lateinit var api: TemplateReceiptApi

    @Before fun setup() {
        server = MockWebServer()
        server.start()
        val client = OkHttpClient.Builder()
            .retryOnConnectionFailure(false)
            .readTimeout(250, TimeUnit.MILLISECONDS)
            .build()
        api = Retrofit.Builder().baseUrl(server.url("/"))
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build().create(TemplateReceiptApi::class.java)
    }

    @After fun teardown() { server.shutdown() }

    @Test fun missingReceipt404DoesNotSendMutation() = runBlocking {
        val id = UUID.randomUUID().toString()
        server.enqueue(MockResponse().setResponseCode(404))
        try {
            api.activationReceipt("Bearer synthetic", 21, id)
            fail("Expected HTTP 404")
        } catch (e: HttpException) { assertEquals(404, e.code()) }
        val request = server.takeRequest(2, TimeUnit.SECONDS)!!
        assertEquals("GET", request.method)
        assertEquals("/workspaces/21/templates/activations/receipts/$id", request.path)
        assertEquals(1, server.requestCount)
    }

    @Test fun receiptTimeoutDoesNotSendMutation() = runBlocking {
        val id = UUID.randomUUID().toString()
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        try {
            api.activationReceipt("Bearer synthetic", 21, id)
            fail("Expected transport timeout")
        } catch (_: IOException) { /* original journal must remain UNCERTAIN */ }
        val request = server.takeRequest(2, TimeUnit.SECONDS)!!
        assertEquals("GET", request.method)
        assertEquals("/workspaces/21/templates/activations/receipts/$id", request.path)
        assertEquals(1, server.requestCount)
    }

    @Test fun lostActivationPostResponseRecoveredOnlyByGetWithSameUuid() = runBlocking {
        val id = UUID.randomUUID().toString()
        server.enqueue(MockResponse().setResponseCode(200).setHeadersDelay(750, TimeUnit.MILLISECONDS))
        server.enqueue(MockResponse().setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody("""{"operationId":"$id","templateId":19,"kind":"TASK","createdDefinitionId":77,"replayed":false}"""))
        val json = JsonParser.parseString("""{"name":"Synthetic task"}""").asJsonObject
        try {
            api.activateTask("Bearer synthetic", 21, 19, id, json)
            fail("Expected lost POST response")
        } catch (_: IOException) { /* receipt reconciliation is the only next operation */ }
        val recovered = api.activationReceipt("Bearer synthetic", 21, id)
        assertEquals(id, recovered.operationId)
        assertEquals(77L, recovered.createdDefinitionId)
        val post = server.takeRequest(2, TimeUnit.SECONDS)!!
        val get = server.takeRequest(2, TimeUnit.SECONDS)!!
        assertEquals("POST", post.method)
        assertEquals(id, post.getHeader("Idempotency-Key"))
        assertEquals("GET", get.method)
        assertEquals("/workspaces/21/templates/activations/receipts/$id", get.path)
        assertEquals(2, server.requestCount)
    }
}
