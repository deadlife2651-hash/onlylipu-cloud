package com.onlylipu.cloud.data.upload

import com.onlylipu.cloud.data.api.ApiClient
import com.onlylipu.cloud.data.auth.TokenStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import okio.ForwardingSink
import okio.buffer
import org.json.JSONObject
import java.io.File

/**
 * Uploads an APK to the server with live progress reporting.
 * The server validates, then installs it into the single Cloud Android VM.
 */
object ApkUploader {

    sealed class Result {
        data class Progress(val percent: Int) : Result()
        data class Success(val message: String) : Result()
        data class Failure(val message: String) : Result()
    }

    suspend fun upload(
        file: File,
        tokenStore: TokenStore,
        onEvent: (Result) -> Unit
    ) = withContext(Dispatchers.IO) {
        val total = file.length()
        val inner = file.asRequestBody("application/vnd.android.package-archive".toMediaType())
        val countingBody = object : RequestBody() {
            override fun contentType() = inner.contentType()
            override fun contentLength() = total
            override fun writeTo(sink: BufferedSink) {
                var written = 0L
                var lastPercent = -1
                val forwarding = object : ForwardingSink(sink) {
                    override fun write(source: okio.Buffer, byteCount: Long) {
                        super.write(source, byteCount)
                        written += byteCount
                        val pct = (written * 100 / total).toInt()
                        if (pct != lastPercent) {
                            lastPercent = pct
                            onEvent(Result.Progress(pct))
                        }
                    }
                }
                val buffered = forwarding.buffer()
                inner.writeTo(buffered)
                buffered.flush()
            }
        }

        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("apk", file.name, countingBody)
            .build()

        val request = Request.Builder()
            .url(ApiClient.baseUrl() + "/api/apps/install")
            .header("Authorization", "Bearer ${tokenStore.token}")
            .post(body)
            .build()

        try {
            OkHttpClient().newCall(request).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                val json = runCatching { JSONObject(text) }.getOrNull()
                if (resp.isSuccessful) {
                    onEvent(Result.Success(json?.optString("message") ?: "Installed"))
                } else {
                    onEvent(Result.Failure(json?.optString("error") ?: "Upload failed (${resp.code})"))
                }
            }
        } catch (e: Exception) {
            onEvent(Result.Failure("Network error: ${e.message}"))
        }
    }
}
