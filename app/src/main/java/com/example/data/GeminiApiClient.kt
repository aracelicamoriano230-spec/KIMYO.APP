package com.example.data

import android.util.Log
import com.example.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

object GeminiApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    suspend fun generateContent(
        prompt: String,
        systemInstruction: String = "You are KimYo 5V, an advanced AI hacking assistant and study companion.",
        useProModel: Boolean = false,
        customApiKey: String = ""
    ): String = withContext(Dispatchers.IO) {
        // Evaluate the API key to use
        val apiKey = customApiKey.ifEmpty { BuildConfig.GEMINI_API_KEY }
        
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Error: API Key not configured. Please add your GEMINI_API_KEY in the application settings or the build config secrets."
        }

        // Determine model: gemini-3.1-pro-preview for advanced, gemini-3.5-flash for normal
        val modelName = if (useProModel) "gemini-3.1-pro-preview" else "gemini-3.5-flash"
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"

        try {
            // Build request payload using standard JSONObject to avoid Moshi schema mismatch
            val contentJson = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    val partsArray = JSONArray().apply {
                        put(JSONObject().put("text", prompt))
                    }
                    put(JSONObject().put("parts", partsArray))
                }
                put("contents", contentsArray)

                // Add System Instruction if present
                if (systemInstruction.isNotEmpty()) {
                    val sysPart = JSONObject().put("parts", JSONArray().apply {
                        put(JSONObject().put("text", systemInstruction))
                    })
                    put("systemInstruction", sysPart)
                }

                // Add config parameters
                val config = JSONObject().apply {
                    put("temperature", 0.7)
                }
                put("generationConfig", config)
            }

            val request = Request.Builder()
                .url(url)
                .post(contentJson.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                Log.d("GeminiApi", "Response Code: ${response.code}")
                
                if (!response.isSuccessful) {
                    val errorMsg = try {
                        val errObj = JSONObject(bodyStr)
                        errObj.getJSONObject("error").getString("message")
                    } catch (e: Exception) {
                        "HTTP error ${response.code}"
                    }
                    return@withContext "Error call from Gemini API ($modelName): $errorMsg"
                }

                try {
                    val respJson = JSONObject(bodyStr)
                    val candidates = respJson.getJSONArray("candidates")
                    val firstCandidate = candidates.getJSONObject(0)
                    val contentObj = firstCandidate.getJSONObject("content")
                    val parts = contentObj.getJSONArray("parts")
                    val firstPart = parts.getJSONObject(0)
                    return@withContext firstPart.getString("text")
                } catch (e: Exception) {
                    Log.e("GeminiApi", "Failed to parse API response: $bodyStr", e)
                    return@withContext "Error: Failed to parse Gemini response payload."
                }
            }
        } catch (e: Exception) {
            Log.e("GeminiApi", "Failed during Gemini generateContent call", e)
            return@withContext "Network Error: ${e.message ?: "Unknown"}. Please check your connection."
        }
    }
}
