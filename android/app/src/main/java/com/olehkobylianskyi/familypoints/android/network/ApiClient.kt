package com.olehkobylianskyi.familypoints.android.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    private const val BASE_URL = "https://api-dev.family-point.com:8443/"

    private val httpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
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

    val pointsApi: PointsApi by lazy {
        retrofit.create(PointsApi::class.java)
    }
}
