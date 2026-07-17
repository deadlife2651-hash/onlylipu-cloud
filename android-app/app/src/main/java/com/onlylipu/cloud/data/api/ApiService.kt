package com.onlylipu.cloud.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {

    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): Response<LoginResponse>

    @POST("api/auth/logout-all")
    suspend fun logoutAll(): Response<ActionResponse>

    @GET("api/status")
    suspend fun status(): Response<ServerStatus>

    @GET("api/apps")
    suspend fun listApps(): Response<AppsResponse>

    @DELETE("api/apps/{packageName}")
    suspend fun uninstallApp(@Path("packageName") packageName: String): Response<ActionResponse>

    @POST("api/apps/{packageName}/clear-data")
    suspend fun clearAppData(@Path("packageName") packageName: String): Response<ActionResponse>

    @POST("api/vm/android/start")
    suspend fun startCloudAndroid(): Response<ActionResponse>

    @POST("api/vm/android/stop")
    suspend fun stopCloudAndroid(): Response<ActionResponse>
}
