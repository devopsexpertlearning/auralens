package com.example.auralens.data

import android.graphics.Bitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class VisionRepository(
    private val geminiClient: GeminiClient
) {
    // We could keep track of history here if needed
    
    // Pass-through functions to GeminiClient, but could add caching or logging here
    
    suspend fun describeScene(image: Bitmap): Result<String> {
        return try {
            val result = geminiClient.describeScene(image)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun readText(image: Bitmap): Result<String> {
        return try {
            val result = geminiClient.readText(image)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun findObject(image: Bitmap, objectName: String): Result<String> {
        return try {
            val result = geminiClient.findObject(image, objectName)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun askAboutPhoto(image: Bitmap, question: String): Result<String> {
        return try {
            val result = geminiClient.askAboutPhoto(image, question)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun setModelName(modelName: String) {
        geminiClient.setModel(modelName)
    }

    fun setDetailedDescriptions(enabled: Boolean) {
        geminiClient.useDetailedDescriptions = enabled
    }
}
