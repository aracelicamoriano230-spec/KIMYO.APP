package com.example.data

import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SupabaseSyncManager {
    private val client = OkHttpClient()
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    suspend fun syncTerminalCommand(
        supabaseUrl: String,
        supabaseKey: String,
        command: TerminalCommandEntity
    ): Boolean = withContext(Dispatchers.IO) {
        if (supabaseUrl.isEmpty() || supabaseKey.isEmpty()) {
            return@withContext false
        }

        // Standard Supabase REST POST endpoint: url + /rest/v1/terminal_commands
        val cleanUrl = if (supabaseUrl.endsWith("/")) supabaseUrl else "$supabaseUrl/"
        val endpoint = "${cleanUrl}rest/v1/terminal_commands"

        // Build Row json matching Supabase table structure
        val jsonPayload = JSONObject().apply {
            put("command", command.command)
            put("output", command.output)
            put("success", command.success)
            put("timestamp", command.timestamp)
            put("username", command.username)
        }.toString()

        val request = Request.Builder()
            .url(endpoint)
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer $supabaseKey")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=minimal") // returns status 201 Created
            .post(jsonPayload.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val code = response.code
                Log.d("SupabaseSync", "Command synced to Supabase (code: $code)")
                return@withContext code in 200..299
            }
        } catch (e: Exception) {
            Log.e("SupabaseSync", "Failed to sync command: ${e.message}", e)
            return@withContext false
        }
    }

    suspend fun testConnection(supabaseUrl: String, supabaseKey: String): Boolean = withContext(Dispatchers.IO) {
        if (supabaseUrl.isEmpty() || supabaseKey.isEmpty()) {
            return@withContext false
        }
        val cleanUrl = if (supabaseUrl.endsWith("/")) supabaseUrl else "$supabaseUrl/"
        // Try getting schema config or a simple REST health-check info
        val endpoint = "${cleanUrl}rest/v1/"

        val request = Request.Builder()
            .url(endpoint)
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer $supabaseKey")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                Log.d("SupabaseSync", "Supabase test connection status: ${response.code}")
                return@withContext response.isSuccessful || response.code == 404 // 404 sometimes means table not visible but API works
            }
        } catch (e: Exception) {
            Log.e("SupabaseSync", "Failed to contact Supabase API: ${e.message}")
            return@withContext false
        }
    }
}
