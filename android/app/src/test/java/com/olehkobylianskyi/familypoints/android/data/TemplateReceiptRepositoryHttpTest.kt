package com.olehkobylianskyi.familypoints.android.data

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
import com.olehkobylianskyi.familypoints.android.network.TemplateReceiptApi
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

/** Tests the actual repository, not merely Retrofit interface or a policy stub. */
class TemplateReceiptRepositoryHttpTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: TemplateReceiptRepository

    @Before fun setup() {
        server = MockWebServer()
        server.start()
        val client = OkHttpClient.Builder()
            .retryOnConnectionFailure(false)
            .readTimeout(250, TimeUnit.MILLISECONDS).build()
        val api = Retrofit.Builder().baseUrl(server.url("/")).client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build().create(TemplateReceiptApi::class.java)
        repository = TemplateReceiptRepository({ "synthetic-token" }, api)
    }

    @After fun teardown() { server.shutdown() }

    @Test fun receipt404DoesNotCreateActivationOrRewriteUuid() = runBlocking {
        val id = UUID.randomUUID()
        server.enqueue(MockResponse().setResponseCode(404))
        try {
            repository.activationReceipt(31, id)
            fail("HTTP 404 expected")
        } catch (e: HttpException) { assertEquals(404, e.code()) }
        val request = server.takeRequest(2, TimeUnit.SECONDS)!!
        assertEquals("GET", request.method)
        assertEquals("/workspaces/31/templates/activations/receipts/$id", request.path)
        assertEquals(1, server.requestCount)
    }

    @Test fun receiptTimeoutNeverIssuesSecondRequest() = runBlocking {
        val id = UUID.randomUUID()
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        try {
            repository.activationReceipt(31, id)
            fail("Timeout expected")
        } catch (_: IOException) { }
        assertEquals("GET", server.takeRequest(2, TimeUnit.SECONDS)!!.method)
        assertEquals(1, server.requestCount)
    }

    @Test fun repositoryValidatesReceiptIdentityAndRejectsFalseSuccess() = runBlocking {
        val id = UUID.randomUUID()
        server.enqueue(MockResponse().setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody("""{"operationId":"${UUID.randomUUID()}","templateId":19,"kind":"TASK","createdDefinitionId":47,"replayed":false}"""))
        try {
            repository.activationReceipt(31, id)
            fail("Mismatched operation must not be confirmed")
        } catch (_: IllegalStateException) { }
        assertEquals(1, server.requestCount)
    }

    @Test fun lostActivationResponseRecoversWithOriginalIdAndNoRepost() = runBlocking {
        val id = UUID.randomUUID()
        server.enqueue(MockResponse().setResponseCode(200).setHeadersDelay(750, TimeUnit.MILLISECONDS))
        server.enqueue(MockResponse().setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody("""{"operationId":"$id","templateId":19,"kind":"TASK","createdDefinitionId":47,"replayed":false}"""))
        try {
            repository.activateConfirmed(31, 19, "TASK", id,
                JsonParser.parseString("""{"name":"test"}""").asJsonObject)
            fail("POST response expected to be lost")
        } catch (_: IOException) { }
        val receipt = repository.activationReceipt(31, id)
        assertEquals(id.toString(), receipt.operationId)
        val post = server.takeRequest(2, TimeUnit.SECONDS)!!
        val get = server.takeRequest(2, TimeUnit.SECONDS)!!
        assertEquals("POST", post.method)
        assertEquals(id.toString(), post.getHeader("Idempotency-Key"))
        assertEquals("GET", get.method)
        assertEquals(2, server.requestCount)
    }
    @Test fun switchedLoginBeforeDispatchCannotSendPreparedActivation() = runBlocking {
        var token = "account-b"
        val client = OkHttpClient.Builder().retryOnConnectionFailure(false).build()
        val api = Retrofit.Builder().baseUrl(server.url("/")).client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build().create(TemplateReceiptApi::class.java)
        val guarded = TemplateReceiptRepository({ token }, api)
        val operation = UUID.randomUUID()
        try {
            guarded.activateConfirmed(31, 19, "TASK", operation,
                JsonParser.parseString("""{"name":"test"}""").asJsonObject,
                expectedAccessToken = "account-a")
            fail("A prepared operation must not use another account's token")
        } catch (_: IllegalStateException) { }
        assertEquals(0, server.requestCount)
        token = "account-a"
    }

    @Test fun pinnedCredentialsUseOriginalAccountAuthorization() = runBlocking {
        val id = UUID.randomUUID()
        server.enqueue(MockResponse().setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody("""{"operationId":"$id","templateId":19,"kind":"TASK","createdDefinitionId":77,"replayed":false}"""))
        val result = repository.activateConfirmed(31, 19, "TASK", id,
            JsonParser.parseString("""{"name":"test"}""").asJsonObject,
            expectedAccessToken = "synthetic-token")
        assertEquals(id.toString(), result.operationId)
        val request = server.takeRequest(2, TimeUnit.SECONDS)!!
        assertEquals("Bearer synthetic-token", request.getHeader("Authorization"))
        assertEquals(id.toString(), request.getHeader("Idempotency-Key"))
        assertEquals(1, server.requestCount)
    }


    @Test fun switchedSessionBeforeRecoveryGetIssuesZeroRequests() = runBlocking {
        val api = Retrofit.Builder().baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build().create(TemplateReceiptApi::class.java)
        val switched = TemplateReceiptRepository({ "new-account-token" }, api)
        try {
            switched.activationReceipt(31, UUID.randomUUID(), expectedToken = "old-account-token")
            fail("Switched account must not read original receipt")
        } catch (_: IllegalStateException) { }
        assertEquals(0, server.requestCount)
    }

    @Test fun recoveryGetUsesPinnedOriginalAuthorization() = runBlocking {
        val id = UUID.randomUUID()
        server.enqueue(MockResponse().setResponseCode(404))
        try {
            repository.activationReceipt(31, id, expectedToken = "synthetic-token")
            fail("HTTP 404 expected")
        } catch (e: HttpException) { assertEquals(404, e.code()) }
        val request = server.takeRequest(2, TimeUnit.SECONDS)!!
        assertEquals("Bearer synthetic-token", request.getHeader("Authorization"))
        assertEquals("GET", request.method)
        assertEquals(1, server.requestCount)
    }

}
