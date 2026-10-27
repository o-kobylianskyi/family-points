package com.olehkobylianskyi.familypoints.android.network

import com.olehkobylianskyi.familypoints.android.BuildConfig

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    private val BASE_URL = BuildConfig.API_BASE_URL

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
