package com.olehkobylianskyi.familypoints.android.network

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

    private const val BASE_URL = "https://api-dev.family-point.com:8443/"

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

    private val probeClient: OkHttpClient by lazy {
        httpClient.newBuilder()
            .callTimeout(6, TimeUnit.SECONDS)
            .build()
    }

    /**
     * A protected backend endpoint is intentionally queried without credentials.
     * 401/403 confirms that the API responds; 502/503/504 or IO means unavailable.
     * This does not validate an account or extend an authentication session.
     */
    suspend fun probeServer(): Boolean = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(BASE_URL + "api/me").get().build()
        try {
            probeClient.newCall(request).execute().use { response ->
                ServerReachability.httpResponse(response.code)
                ServerReachability.status.value == ServerStatus.ONLINE
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
}
