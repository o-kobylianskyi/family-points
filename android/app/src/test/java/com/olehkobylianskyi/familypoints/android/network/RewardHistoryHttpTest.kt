package com.olehkobylianskyi.familypoints.android.network

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** GET-only paginated purchase history, with backend-authored states. */
class RewardHistoryHttpTest {
    private lateinit var server: MockWebServer
    private lateinit var api: RewardsApi

    @Before fun setup() {
        server = MockWebServer().apply { start() }
        api = Retrofit.Builder().baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build().create(RewardsApi::class.java)
    }
    @After fun teardown() { server.shutdown() }

    @Test fun pagesPreservePurchaseSnapshotsAndDoNotWrite() = runBlocking {
        server.enqueue(MockResponse().setHeader("Content-Type", "application/json")
            .setBody("""{"items":[{"id":1,"memberId":7,"memberName":"Child","rewardTitle":"Old reward name","priceAmount":15,"pointTypeCode":"POINTS","status":"READY","purchasedAt":"2026-11-01T12:00:00"}],"nextCursor":"cursor-next","hasMore":true}"""))
        server.enqueue(MockResponse().setHeader("Content-Type", "application/json")
            .setBody("""{"items":[{"id":2,"memberId":7,"rewardTitle":"Second reward","priceAmount":20,"status":"REDEEMED"}],"nextCursor":null,"hasMore":false}"""))
        val first = api.getPurchaseHistory("Bearer session-a", 91L, 7L, null, 30)
        assertEquals("Old reward name", first.items.single().rewardTitle)
        assertEquals("READY", first.items.single().status)
        assertTrue(first.hasMore)
        val second = api.getPurchaseHistory("Bearer session-a", 91L, 7L, first.nextCursor, 30)
        assertEquals("REDEEMED", second.items.single().status)
        assertFalse(second.hasMore)
        val a = server.takeRequest(2, TimeUnit.SECONDS)!!
        val b = server.takeRequest(2, TimeUnit.SECONDS)!!
        assertEquals("GET", a.method)
        assertEquals("GET", b.method)
        assertTrue(a.path!!.contains("memberId=7"))
        assertTrue(b.path!!.contains("cursor=cursor-next"))
        assertEquals("Bearer session-a", a.getHeader("Authorization"))
        assertEquals(2, server.requestCount)
    }
    @Test fun memberPickerReadsAuthenticatedWorkspaceWithoutMutations() = runBlocking {
        server.enqueue(MockResponse().setHeader("Content-Type", "application/json")
            .setBody("""[{"id":7,"name":"Child A"},{"id":8,"name":"Child B"}]"""))
        val membersApi = Retrofit.Builder().baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build().create(TaskApi::class.java)
        val members = membersApi.getWorkspaceMembers("Bearer manager", 91L)
        assertEquals(2, members.size)
        assertEquals(7L, members[0].id)
        assertEquals("Child B", members[1].name)
        val request = server.takeRequest(2, TimeUnit.SECONDS)!!
        assertEquals("GET", request.method)
        assertEquals("/workspaces/91/members", request.path)
        assertEquals("Bearer manager", request.getHeader("Authorization"))
        assertEquals(1, server.requestCount)
    }

    @Test fun forbiddenMemberHistoryDoesNotWriteOrFallbackToForeignFamily() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(403)
            .setBody("""{"code":"FORBIDDEN"}"""))
        try {
            api.getPurchaseHistory("Bearer child", 91L, 999L, null, 30)
            fail("Backend must deny unauthorized member history")
        } catch (e: HttpException) {
            assertEquals(403, e.code())
        }
        val request = server.takeRequest(2, TimeUnit.SECONDS)!!
        assertEquals("GET", request.method)
        assertEquals("Bearer child", request.getHeader("Authorization"))
        assertTrue(request.path!!.contains("memberId=999"))
        assertEquals(1, server.requestCount)
    }

    @Test fun persistedStatusAndCursorAreTransportedAsReadOnlyFilters() = runBlocking {
        server.enqueue(MockResponse().setHeader("Content-Type", "application/json")
            .setBody("""{"items":[],"nextCursor":null,"hasMore":false}"""))
        val page = api.getPurchaseHistory(
            "Bearer manager", 91L, 7L, "cursor-2", 20, "CANCELLED"
        )
        assertTrue(page.items.isEmpty())
        val request = server.takeRequest(2, TimeUnit.SECONDS)!!
        assertEquals("GET", request.method)
        assertTrue(request.path!!.contains("cursor=cursor-2"))
        assertTrue(request.path!!.contains("status=CANCELLED"))
        assertTrue(request.path!!.contains("limit=20"))
        assertEquals(1, server.requestCount)
    }

}
