package com.onlylipu.cloud.data.api

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.onlylipu.cloud.BuildConfig
import com.onlylipu.cloud.data.auth.TokenStore
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object ApiClient {

    @Volatile
    private var service: ApiService? = null

    fun service(tokenStore: TokenStore): ApiService {
        return service ?: synchronized(this) {
            service ?: build(tokenStore).also { service = it }
        }
    }

    fun baseUrl(): String = BuildConfig.SERVER_BASE_URL.trimEnd('/')

    fun wsSignalingUrl(): String =
        baseUrl().replaceFirst("https://", "wss://").replaceFirst("http://", "ws://") +
            "/ws/signaling"

    private fun build(tokenStore: TokenStore): ApiService {
        val authInterceptor = Interceptor { chain ->
            val original = chain.request()
            val builder = original.newBuilder()
            tokenStore.token?.let { builder.header("Authorization", "Bearer $it") }
            builder.header("Accept", "application/json")
            chain.proceed(builder.build())
        }

        val logging = HttpLoggingInterceptor().apply {
            // Never log headers in release: the Authorization header must not leak.
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
                    else HttpLoggingInterceptor.Level.NONE
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .build()

        val json = Json { ignoreUnknownKeys = true }
        return Retrofit.Builder()
            .baseUrl(baseUrl() + "/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ApiService::class.java)
    }
}
