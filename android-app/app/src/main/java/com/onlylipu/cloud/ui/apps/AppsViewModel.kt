package com.onlylipu.cloud.ui.apps

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.onlylipu.cloud.data.api.ApiClient
import com.onlylipu.cloud.data.api.InstalledApp
import com.onlylipu.cloud.data.auth.TokenStore
import com.onlylipu.cloud.data.upload.ApkUploader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

data class AppsUiState(
    val apps: List<InstalledApp> = emptyList(),
    val loading: Boolean = false,
    val uploading: Boolean = false,
    val uploadPercent: Int = 0,
    val message: String? = null,
    val error: String? = null
)

class AppsViewModel(app: Application) : AndroidViewModel(app) {
    private val tokenStore = TokenStore(app)

    private val _state = MutableStateFlow(AppsUiState())
    val state: StateFlow<AppsUiState> = _state

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            try {
                val resp = ApiClient.service(tokenStore).listApps()
                if (resp.isSuccessful) {
                    _state.value = _state.value.copy(
                        apps = resp.body()?.apps ?: emptyList(),
                        loading = false, error = null
                    )
                } else {
                    _state.value = _state.value.copy(loading = false,
                        error = "Failed to load apps (${resp.code()})")
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false,
                    error = "Server unreachable")
            }
        }
    }

    fun installApk(uri: Uri) {
        val context = getApplication<Application>()
        viewModelScope.launch {
            _state.value = _state.value.copy(uploading = true, uploadPercent = 0,
                message = null, error = null)
            try {
                // Copy the picked APK to a private temp file first.
                val tmp = File(context.cacheDir, "upload-${System.currentTimeMillis()}.apk")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tmp).use { output -> input.copyTo(output) }
                } ?: run {
                    _state.value = _state.value.copy(uploading = false,
                        error = "Could not read the selected file")
                    return@launch
                }
                if (!tmp.name.endsWith(".apk") || tmp.length() == 0L || tmp.length() > 500L * 1024 * 1024) {
                    tmp.delete()
                    _state.value = _state.value.copy(uploading = false,
                        error = "Invalid APK file (empty or larger than 500 MB)")
                    return@launch
                }
                ApkUploader.upload(tmp, tokenStore) { event ->
                    when (event) {
                        is ApkUploader.Result.Progress ->
                            _state.value = _state.value.copy(uploadPercent = event.percent)
                        is ApkUploader.Result.Success -> {
                            _state.value = _state.value.copy(uploading = false,
                                message = event.message)
                            refresh()
                        }
                        is ApkUploader.Result.Failure ->
                            _state.value = _state.value.copy(uploading = false,
                                error = event.message)
                    }
                }
                tmp.delete()   // delete uploaded installation file after success
            } catch (e: Exception) {
                _state.value = _state.value.copy(uploading = false,
                    error = "Upload failed: ${e.message}")
            }
        }
    }

    fun uninstall(packageName: String) {
        viewModelScope.launch {
            try {
                val r = ApiClient.service(tokenStore).uninstallApp(packageName)
                _state.value = _state.value.copy(
                    message = if (r.isSuccessful) "Uninstalled $packageName"
                    else "Uninstall failed"
                )
                refresh()
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "Server unreachable")
            }
        }
    }

    fun clearData(packageName: String) {
        viewModelScope.launch {
            try {
                ApiClient.service(tokenStore).clearAppData(packageName)
                _state.value = _state.value.copy(message = "Cleared data for $packageName")
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "Server unreachable")
            }
        }
    }

    fun clearMessage() = { _state.value = _state.value.copy(message = null, error = null) }
}
