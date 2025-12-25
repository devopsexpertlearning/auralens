package com.example.auralens.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class PreferenceManager(context: Context) {

    private val sharedPreferences: SharedPreferences

    init {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        sharedPreferences = EncryptedSharedPreferences.create(
            context,
            "secret_shared_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    companion object {
        private const val KEY_API_KEY = "gemini_api_key"
        private const val KEY_MODEL_NAME = "gemini_model_name"
        private const val DEFAULT_MODEL = "gemini-3-pro-preview"
    }

    var apiKey: String
        get() = sharedPreferences.getString(KEY_API_KEY, "") ?: ""
        set(value) = sharedPreferences.edit().putString(KEY_API_KEY, value).apply()

    var modelName: String
        get() = sharedPreferences.getString(KEY_MODEL_NAME, DEFAULT_MODEL) ?: DEFAULT_MODEL
        set(value) = sharedPreferences.edit().putString(KEY_MODEL_NAME, value).apply()
}
