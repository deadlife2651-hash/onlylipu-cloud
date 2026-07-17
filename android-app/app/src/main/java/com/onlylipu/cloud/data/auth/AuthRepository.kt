package com.onlylipu.cloud.data.auth

import com.onlylipu.cloud.data.api.ApiClient
import com.onlylipu.cloud.data.api.LoginRequest

sealed class AuthResult {
    data object Success : AuthResult()
    data class Failure(val message: String) : AuthResult()
}

class AuthRepository(private val tokenStore: TokenStore) {

    suspend fun login(username: String, password: String): AuthResult {
        return try {
            val response = ApiClient.service(tokenStore).login(LoginRequest(username, password))
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.token.isNotBlank()) {
                    tokenStore.token = body.token
                    tokenStore.username = username
                    AuthResult.Success
                } else {
                    AuthResult.Failure("Empty response from server")
                }
            } else {
                when (response.code()) {
                    401 -> AuthResult.Failure("Invalid username or password")
                    423 -> AuthResult.Failure("Account locked. Try again later.")
                    else -> AuthResult.Failure("Server error (${response.code()})")
                }
            }
        } catch (e: Exception) {
            AuthResult.Failure("Cannot reach server. Check your connection.")
        }
    }

    suspend fun logoutEverywhere() {
        try {
            ApiClient.service(tokenStore).logoutAll()
        } catch (_: Exception) { /* best effort */ }
        tokenStore.clear()
    }

    fun logoutLocal() = tokenStore.clear()
}
