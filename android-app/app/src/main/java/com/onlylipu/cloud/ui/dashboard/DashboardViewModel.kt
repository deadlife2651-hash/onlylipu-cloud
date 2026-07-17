package com.onlylipu.cloud.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.onlylipu.cloud.data.api.ApiClient
import com.onlylipu.cloud.data.api.ServerStatus
import com.onlylipu.cloud.data.auth.AuthRepository
import com.onlylipu.cloud.data.auth.TokenStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

data class DashboardUiState(
    val status: ServerStatus = ServerStatus(),
    val pingMs: Int = -1,
    val loading: Boolean = true,
    val error: String? = null,
    val loggedOut: Boolean = false,
    val actionMessage: String? = null
)

class DashboardViewModel(app: Application) : AndroidViewModel(app) {
    private val tokenStore = TokenStore(app)
    private val repo = AuthRepository(tokenStore)

    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state

    val lastEnvironment: String? get() = tokenStore.lastEnvironment

    init {
        startPolling()
    }

    private fun startPolling() {
        viewModelScope.launch {
            while (isActive) {
                refresh()
                delay(5000)
            }
        }
    }

    suspend fun refresh() {
        try {
            val resp = ApiClient.service(tokenStore).status()
            if (resp.isSuccessful) {
                val body = resp.body() ?: ServerStatus()
                _state.value = _state.value.copy(
                    status = body, loading = false, error = null,
                    pingMs = measurePing()
                )
            } else if (resp.code() == 401) {
                repo.logoutLocal()
                _state.value = _state.value.copy(loggedOut = true)
            } else {
                _state.value = _state.value.copy(loading = false, error = "Server error ${resp.code()}")
            }
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                loading = false,
                error = "Server offline or unreachable",
                status = _state.value.status.copy(online = false, state = "offline")
            )
        }
    }

    private suspend fun measurePing(): Int = withContext(Dispatchers.IO) {
        try {
            val host = ApiClient.baseUrl().removePrefix("https://").removePrefix("http://")
                .substringBefore("/").substringBefore(":")
            val start = System.nanoTime()
            Socket().use { it.connect(InetSocketAddress(host, 443), 3000) }
            ((System.nanoTime() - start) / 1_000_000).toInt()
        } catch (e: Exception) { -1 }
    }

    fun startCloudAndroid() {
        viewModelScope.launch {
            try {
                val r = ApiClient.service(tokenStore).startCloudAndroid()
                _state.value = _state.value.copy(
                    actionMessage = if (r.isSuccessful) "Cloud Android starting…"
                    else "Failed to start Cloud Android"
                )
                delay(1500); refresh()
            } catch (e: Exception) {
                _state.value = _state.value.copy(actionMessage = "Server unreachable")
            }
        }
    }

    fun markUsed(mode: String) { tokenStore.lastEnvironment = mode }

    fun clearActionMessage() { _state.value = _state.value.copy(actionMessage = null) }

    fun logout() {
        viewModelScope.launch {
            repo.logoutEverywhere()
            _state.value = _state.value.copy(loggedOut = true)
        }
    }
}
