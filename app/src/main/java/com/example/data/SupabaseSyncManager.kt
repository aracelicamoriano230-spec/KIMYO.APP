package com.example.data

import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SupabaseSyncManager {
    private val client = OkHttpClient()
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    // Default connection parameters seeded for out-of-the-box operation on any phone
    const val DEFAULT_URL = "https://qrddbonbduzdwchawcxz.supabase.co"
    const val DEFAULT_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InFyZGRib25iZHV6ZHdjaGF3Y3h6Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3Nzk4MjM2MzMsImV4cCI6MjA5NTM5OTYzM30.hhHEe4wejuaViDbdLsiJ722Fr-2jh3orMcwMpltywIs"

    private fun getCleanEndpoint(supabaseUrl: String, path: String): String {
        val base = if (supabaseUrl.endsWith("/")) supabaseUrl else "$supabaseUrl/"
        return "$base$path"
    }

    suspend fun syncTerminalCommand(
        supabaseUrl: String,
        supabaseKey: String,
        command: TerminalCommandEntity
    ): Boolean = withContext(Dispatchers.IO) {
        if (supabaseUrl.isEmpty() || supabaseKey.isEmpty()) return@withContext false
        val endpoint = getCleanEndpoint(supabaseUrl, "rest/v1/terminal_commands")

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
            .addHeader("Prefer", "return=minimal")
            .post(jsonPayload.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                Log.d("SupabaseSync", "Command sync code: ${response.code}")
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e("SupabaseSync", "Failed to sync command: ${e.message}")
            return@withContext false
        }
    }

    suspend fun syncUser(
        supabaseUrl: String,
        supabaseKey: String,
        user: UserEntity
    ): Boolean = withContext(Dispatchers.IO) {
        if (supabaseUrl.isEmpty() || supabaseKey.isEmpty()) return@withContext false
        val endpoint = getCleanEndpoint(supabaseUrl, "rest/v1/users")

        val jsonPayload = JSONObject().apply {
            put("username", user.username)
            put("mode", user.mode)
            put("passwordHash", user.passwordHash)
            put("isApproved", user.isApproved)
            put("signupTime", user.signupTime)
        }.toString()

        val request = Request.Builder()
            .url(endpoint)
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer $supabaseKey")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "resolution=merge-duplicates") // upsert support
            .post(jsonPayload.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                Log.d("SupabaseSync", "User sync code: ${response.code}")
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e("SupabaseSync", "Failed to sync user: ${e.message}")
            return@withContext false
        }
    }

    suspend fun syncChat(
        supabaseUrl: String,
        supabaseKey: String,
        chat: ChatItemEntity
    ): Boolean = withContext(Dispatchers.IO) {
        if (supabaseUrl.isEmpty() || supabaseKey.isEmpty()) return@withContext false
        val endpoint = getCleanEndpoint(supabaseUrl, "rest/v1/chats")

        val jsonPayload = JSONObject().apply {
            put("username", chat.username)
            put("sender", chat.sender)
            put("text", chat.text)
            put("timestamp", chat.timestamp)
            put("isHeader", chat.isHeader)
        }.toString()

        val request = Request.Builder()
            .url(endpoint)
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer $supabaseKey")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=minimal")
            .post(jsonPayload.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                Log.d("SupabaseSync", "Chat sync code: ${response.code}")
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e("SupabaseSync", "Failed to sync chat item: ${e.message}")
            return@withContext false
        }
    }

    suspend fun fetchRemoteUsers(
        supabaseUrl: String,
        supabaseKey: String
    ): List<UserEntity> = withContext(Dispatchers.IO) {
        if (supabaseUrl.isEmpty() || supabaseKey.isEmpty()) return@withContext emptyList()
        val endpoint = getCleanEndpoint(supabaseUrl, "rest/v1/users?order=signupTime.desc")

        val request = Request.Builder()
            .url(endpoint)
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer $supabaseKey")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val bodyStr = response.body?.string() ?: "[]"
                val jsonArray = JSONArray(bodyStr)
                val users = mutableListOf<UserEntity>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    users.add(
                        UserEntity(
                            username = obj.optString("username", ""),
                            mode = obj.optString("mode", "NORMAL"),
                            passwordHash = obj.optString("passwordHash", ""),
                            isApproved = obj.optBoolean("isApproved", false),
                            signupTime = obj.optLong("signupTime", System.currentTimeMillis())
                        )
                    )
                }
                return@withContext users
            }
        } catch (e: Exception) {
            Log.e("SupabaseSync", "Failed to fetch remote users: ${e.message}")
            return@withContext emptyList()
        }
    }

    suspend fun fetchAllRemoteCommands(
        supabaseUrl: String,
        supabaseKey: String
    ): List<TerminalCommandEntity> = withContext(Dispatchers.IO) {
        if (supabaseUrl.isEmpty() || supabaseKey.isEmpty()) return@withContext emptyList()
        val endpoint = getCleanEndpoint(supabaseUrl, "rest/v1/terminal_commands?order=timestamp.desc&limit=100")

        val request = Request.Builder()
            .url(endpoint)
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer $supabaseKey")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val bodyStr = response.body?.string() ?: "[]"
                val jsonArray = JSONArray(bodyStr)
                val list = mutableListOf<TerminalCommandEntity>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    list.add(
                        TerminalCommandEntity(
                            id = obj.optLong("id", 0L),
                            command = obj.optString("command", ""),
                            output = obj.optString("output", ""),
                            success = obj.optBoolean("success", true),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            username = obj.optString("username", "Offline"),
                            isSynced = true
                        )
                    )
                }
                return@withContext list
            }
        } catch (e: Exception) {
            Log.e("SupabaseSync", "Failed to fetch remote terminal logs: ${e.message}")
            return@withContext emptyList()
        }
    }

    suspend fun updateRemoteUserApproval(
        supabaseUrl: String,
        supabaseKey: String,
        targetUsername: String,
        approved: Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        if (supabaseUrl.isEmpty() || supabaseKey.isEmpty()) return@withContext false
        val endpoint = getCleanEndpoint(supabaseUrl, "rest/v1/users?username=eq.$targetUsername")

        val jsonPayload = JSONObject().apply {
            put("isApproved", approved)
        }.toString()

        val request = Request.Builder()
            .url(endpoint)
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer $supabaseKey")
            .addHeader("Content-Type", "application/json")
            .patch(jsonPayload.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                Log.d("SupabaseSync", "Approve target user code: ${response.code}")
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e("SupabaseSync", "Failed to approve user remote: ${e.message}")
            return@withContext false
        }
    }

    suspend fun testConnection(supabaseUrl: String, supabaseKey: String): Boolean = withContext(Dispatchers.IO) {
        if (supabaseUrl.isEmpty() || supabaseKey.isEmpty()) return@withContext false
        val endpoint = getCleanEndpoint(supabaseUrl, "rest/v1/")

        val request = Request.Builder()
            .url(endpoint)
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer $supabaseKey")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                Log.d("SupabaseSync", "Supabase test connection status: ${response.code}")
                return@withContext response.isSuccessful || response.code == 404
            }
        } catch (e: Exception) {
            Log.e("SupabaseSync", "Failed to contact Supabase API: ${e.message}")
            return@withContext false
        }
    }
}
