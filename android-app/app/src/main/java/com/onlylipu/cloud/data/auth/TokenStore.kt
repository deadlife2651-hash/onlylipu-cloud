package com.onlylipu.cloud.data.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Stores the session token in Android Keystore-backed encrypted preferences.
 * Tokens are never written to plain storage, backups, or logs.
 */
class TokenStore(context: Context) {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "onlylipu_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    var token: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_TOKEN, value).apply()

    var username: String?
        get() = prefs.getString(KEY_USER, null)
        set(value) = prefs.edit().putString(KEY_USER, value).apply()

    var lastEnvironment: String?
        get() = prefs.getString(KEY_LAST_ENV, null)
        set(value) = prefs.edit().putString(KEY_LAST_ENV, value).apply()

    var biometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC, false)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRIC, value).apply()

    var defaultQuality: String
        get() = prefs.getString(KEY_QUALITY, "auto") ?: "auto"
        set(value) = prefs.edit().putString(KEY_QUALITY, value).apply()

    fun isLoggedIn(): Boolean = !token.isNullOrBlank()

    fun clear() = prefs.edit().clear().apply()

    private companion object {
        const val KEY_TOKEN = "session_token"
        const val KEY_USER = "username"
        const val KEY_LAST_ENV = "last_environment"
        const val KEY_BIOMETRIC = "biometric_enabled"
        const val KEY_QUALITY = "default_quality"
    }
}
