package com.example.auralens.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class PreferenceManager(context: Context) {

    private val sharedPreferences: SharedPreferences

    init {
        sharedPreferences = try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                "secret_shared_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Fallback to regular SharedPreferences if encrypted version fails
            // This can happen on some devices with hardware/keystore issues
            Log.e("PreferenceManager", "Failed to create encrypted prefs, falling back to regular", e)
            context.getSharedPreferences("auralens_prefs_fallback", Context.MODE_PRIVATE)
        }
    }

    companion object {
        private const val KEY_API_KEY = "gemini_api_key"
        private const val KEY_MODEL_NAME = "gemini_model_name"
        private const val DEFAULT_MODEL = "gemini-1.5-flash"
    }

    var apiKey: String
        get() = try {
            sharedPreferences.getString(KEY_API_KEY, "") ?: ""
        } catch (e: Exception) {
            Log.e("PreferenceManager", "Error reading API key", e)
            ""
        }
        set(value) = try {
            sharedPreferences.edit().putString(KEY_API_KEY, value).apply()
        } catch (e: Exception) {
            Log.e("PreferenceManager", "Error saving API key", e)
        }

    var modelName: String
        get() = try {
            sharedPreferences.getString(KEY_MODEL_NAME, DEFAULT_MODEL) ?: DEFAULT_MODEL
        } catch (e: Exception) {
            Log.e("PreferenceManager", "Error reading model name", e)
            DEFAULT_MODEL
        }
        set(value) = try {
            sharedPreferences.edit().putString(KEY_MODEL_NAME, value).apply()
        } catch (e: Exception) {
            Log.e("PreferenceManager", "Error saving model name", e)
        }
}
