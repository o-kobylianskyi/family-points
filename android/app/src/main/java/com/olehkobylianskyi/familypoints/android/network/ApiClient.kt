package com.olehkobylianskyi.familypoints.android.network

import com.olehkobylianskyi.familypoints.android.BuildConfig

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.io.IOException
import java.util.concurrent.TimeUnit
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    private val BASE_URL = BuildConfig.API_BASE_URL

    private val quarantinedEpochs = ConcurrentHashMap.newKeySet<String>()
    private val epochHandshakeClient: OkHttpClient by lazy {
        OkHttpClient.Builder().followRedirects(false)
            .callTimeout(6, TimeUnit.SECONDS).build()
    }

    /**
     * Read authenticated workspace generation immediately before each scoped write.
     * A failed handshake blocks the write; credentials and generation are never logged.
     */
    private val workspaceEpochInterceptor = Interceptor { chain ->
        val request = chain.request()
        val match = Regex("^/workspaces/([0-9]+)(?:/|$)").find(request.url.encodedPath)
        val mutation = request.method in setOf("POST", "PUT", "PATCH", "DELETE")
        val auth = request.header("Authorization")
        if (!mutation || match == null || auth.isNullOrBlank()) {
            chain.proceed(request)
        } else {
            val workspaceId = match.groupValues[1]
            val identity = auth + ":" + workspaceId
            if (identity in quarantinedEpochs) {
                throw IOException("Workspace data changed: sign in again before editing")
            }
            val endpoint = request.url.newBuilder().encodedPath("/workspaces/$workspaceId/data-epoch")
                .query(null).build()
            val handshake = Request.Builder().url(endpoint)
                .header("Authorization", auth).header("Cache-Control", "no-store").get().build()
            val epoch = epochHandshakeClient.newCall(handshake).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Cannot verify workspace data generation")
                val json = try {
                    JSONObject(response.body?.string().orEmpty())
                } catch (_: Exception) {
                    throw IOException("Invalid workspace generation response")
                }
                val serverId = json.optLong("workspaceId", -1L)
                val value = json.optLong("dataEpoch", -1L)
                if (serverId.toString() != workspaceId || value < 0L ||
                    !json.has("dataEpoch")) {
                    throw IOException("Untrusted workspace generation response")
                }
                value
            }
            val existing = request.header("X-Workspace-Data-Epoch")
            if (existing != null && existing != epoch.toString()) {
                quarantinedEpochs.add(identity)
                throw IOException("Workspace generation changed during operation")
            }
            val result = chain.proceed(request.newBuilder()
                .header("X-Workspace-Data-Epoch", epoch.toString()).build())
            if (result.code == 409) {
                // Only quarantine if an authoritative second handshake confirms drift.
                // Generic 409 conflicts remain ordinary application errors.
                try {
                    val check = epochHandshakeClient.newCall(handshake).execute().use { response ->
                        if (!response.isSuccessful) null else
                            JSONObject(response.body?.string().orEmpty()).let { json ->
                                if (json.optLong("workspaceId", -1L).toString() == workspaceId)
                                    json.optLong("dataEpoch", -1L) else null
                            }
                    }
                    if (check != null && check != epoch) quarantinedEpochs.add(identity)
                } catch (_: Exception) { /* Retain original 409 for caller. */ }
            }
            result
        }
    }

    private val httpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val reachabilityInterceptor = Interceptor { chain ->
            try {
                chain.proceed(chain.request()).also { response ->
                    ServerReachability.httpResponse(response.code)
                }
            } catch (error: IOException) {
                if (!chain.call().isCanceled()) ServerReachability.transportFailure()
                throw error
            }
        }

        OkHttpClient.Builder()
            .addInterceptor(workspaceEpochInterceptor)
            .addInterceptor(reachabilityInterceptor)
            .addInterceptor(logging)
            .build()
    }

    // Independent client: an HTTP 200 from a captive portal/proxy is not
    // allowed to mark the actual backend ONLINE before content validation.
    private val probeClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .followRedirects(false)
            .callTimeout(6, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Validate the identity of the actual Spring Boot API instead of trusting
     * an unauthenticated 401 or a generic proxy response.
     */
    suspend fun probeServer(): Boolean = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(BASE_URL + "api/client-health")
            .header("Cache-Control", "no-cache")
            .get()
            .build()
        try {
            probeClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                val valid = response.code == 200 && try {
                    val json = org.json.JSONObject(body)
                    json.optString("service") == "family-points" &&
                        json.optString("status") == "UP"
                } catch (_: Exception) {
                    false
                }
                if (valid) ServerReachability.verifiedProbe()
                else ServerReachability.transportFailure()
                valid
            }
        } catch (_: IOException) {
            ServerReachability.transportFailure()
            false
        }
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val authApi: AuthApi by lazy {
        retrofit.create(AuthApi::class.java)
    }

    val dashboardApi: DashboardApi by lazy {
        retrofit.create(DashboardApi::class.java)
    }

    val taskApi: TaskApi by lazy {
        retrofit.create(TaskApi::class.java)
    }

    val createTaskApi: CreateTaskApi by lazy {
        retrofit.create(CreateTaskApi::class.java)
    }

    val rewardNegotiationApi: RewardNegotiationApi by lazy {
        retrofit.create(RewardNegotiationApi::class.java)
    }

    val rewardsApi: RewardsApi by lazy {
        retrofit.create(RewardsApi::class.java)
    }

    val membersApi: MembersApi by lazy {
        retrofit.create(MembersApi::class.java)
    }

    val roleCatalogApi: RoleCatalogApi by lazy {
        retrofit.create(RoleCatalogApi::class.java)
    }

    val settingsApi: SettingsApi by lazy {
        retrofit.create(SettingsApi::class.java)
    }

    val pointsApi: PointsApi by lazy {
        retrofit.create(PointsApi::class.java)
    }

    val calendarApi: CalendarApi by lazy {
        retrofit.create(CalendarApi::class.java)
    }

    val workspaceEpochApi: WorkspaceEpochApi by lazy {
        retrofit.create(WorkspaceEpochApi::class.java)
    }

    val templateApi: TemplateApi by lazy {
        retrofit.create(TemplateApi::class.java)
    }

    val mediaApi: MediaApi by lazy {
        retrofit.create(MediaApi::class.java)
    }
}
