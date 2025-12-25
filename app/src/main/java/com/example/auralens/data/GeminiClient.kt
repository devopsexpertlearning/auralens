package com.example.auralens.data

import android.graphics.Bitmap
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.GenerationConfig
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiClient(
    private var apiKey: String
) {
    fun updateApiKey(newKey: String) {
        this.apiKey = newKey
    }

    // Default to Gemini 1.5 Flash (fast and multimodal)
    private var currentModelName: String = "gemini-1.5-flash" 
    
    // Configurable settings
    var useDetailedDescriptions: Boolean = false

    private fun getModel(
        modelName: String = currentModelName,
        isJsonMode: Boolean = false
    ): GenerativeModel {
        val config = generationConfig {
            temperature = 0.7f // Balanced creativity and accuracy
            topK = 32
            topP = 0.95f
            maxOutputTokens = 1024
            if (isJsonMode) {
                responseMimeType = "application/json"
            }
        }

        val safetySettings = listOf(
            SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.MEDIUM_AND_ABOVE),
            SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.MEDIUM_AND_ABOVE),
            SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.MEDIUM_AND_ABOVE),
            SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.MEDIUM_AND_ABOVE),
        )

        return GenerativeModel(
            modelName = modelName,
            apiKey = apiKey,
            generationConfig = config,
            safetySettings = safetySettings
        )
    }

    fun setModel(modelName: String) {
        currentModelName = modelName
    }

    suspend fun describeScene(image: Bitmap): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext "API key is not set. Please configure it in settings."
        }
        
        val model = getModel()
        val prompt = if (useDetailedDescriptions) {
            "You are an assistive vision guide for a blind user. Describe the scene in front of the camera in specific detail. Mention objects, their colors, positions, and the general atmosphere. Avoid navigation claims."
        } else {
            "You are an assistive vision guide for a blind user. Briefly describe the scene in front of the camera and mention 3–5 important objects with their rough positions (left/center/right, near/far). Avoid navigation or safety claims."
        }
        
        try {
            val response = model.generateContent(
                content {
                    image(image)
                    text(prompt)
                }
            )
            response.text ?: "I couldn't generate a description."
        } catch (e: Exception) {
            "Error describing scene: ${e.localizedMessage ?: e.message ?: "Unknown error"}"
        }
    }

    suspend fun readText(image: Bitmap): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext "API key is not set. Please configure it in settings."
        }
        
        val model = getModel(modelName = "gemini-1.5-pro") // Prefer high-intelligence model for text
        val prompt = "You are assisting a blind user with reading a document. First, identify the type of document and the key values (like total amount, due date, names). Then read the most important information clearly. Keep numbers and dates accurate. Avoid making up values."
        
        try {
            val response = model.generateContent(
                content {
                    image(image)
                    text(prompt)
                }
            )
            response.text ?: "I found no readable text."
        } catch (e: Exception) {
            "Error reading text: ${e.localizedMessage ?: e.message ?: "Unknown error"}"
        }
    }

    suspend fun findObject(image: Bitmap, objectName: String): String = withContext(Dispatchers.IO) {
        val model = getModel(isJsonMode = true)
        val prompt = """
            You are helping a blind user locate a specific object in the camera frame. 
            The target object is: '$objectName'. 
            If found, reply with a JSON object:
            {
                "found": true,
                "position": "left|center|right",
                "vertical": "top|middle|bottom",
                "confidence": 0-1,
                "explanation": "Brief instruction like 'Move slightly left'"
            }
            If not found, reply with:
            {
                "found": false,
                "explanation": "Object not visible"
            }
        """.trimIndent()

        try {
            val response = model.generateContent(
                content {
                    image(image)
                    text(prompt)
                }
            )
            // Ideally parse JSON here, but for now returning raw text for ViewModel to handle/speak
            // In a real app, return a Data class.
            response.text ?: "{ \"found\": false }" 
        } catch (e: Exception) {
            "{ \"error\": \"${e.localizedMessage}\" }"
        }
    }

    suspend fun askAboutPhoto(image: Bitmap, question: String): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext "API key is not set. Please configure it in settings."
        }
        
        val model = getModel()
        val prompt = "You are assisting a blind user to understand a photo. The user asks: '$question'. Answer their question clearly and concisely based only on the image. If you are not sure, say you are not sure."

        try {
            val response = model.generateContent(
                content {
                    image(image)
                    text(prompt)
                }
            )
            response.text ?: "I couldn't answer that."
        } catch (e: Exception) {
            "Error answering question: ${e.localizedMessage ?: e.message ?: "Unknown error"}"
        }
    }

    suspend fun fetchAvailableModels(): List<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext emptyList()

        try {
            val url = java.net.URL("https://generativelanguage.googleapis.com/v1beta/models?key=$apiKey")
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "GET"

            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonObject = org.json.JSONObject(response)
                val modelsArray = jsonObject.getJSONArray("models")
                val modelList = mutableListOf<String>()

                for (i in 0 until modelsArray.length()) {
                    val modelObj = modelsArray.getJSONObject(i)
                    val name = modelObj.getString("name").removePrefix("models/")
                    // Filter for Gemini models that support generation
                    if (name.contains("gemini", ignoreCase = true) && !name.contains("embedding", ignoreCase = true)) {
                        modelList.add(name)
                    }
                }
                modelList
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
