package com.estidley.umbra

import android.net.Uri
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Client of Webb's Hermes HTTP API. No backend lives in this app.
 * Assumption: chat body includes "model": "hermes" plus the session messages.
 */
class HermesClient {
    private val http = OkHttpClient.Builder()
        .callTimeout(60, TimeUnit.SECONDS)
        .build()


    fun login(baseUrl: String, username: String, password: String): String {
        val url = normalizeBase(baseUrl) + "/api/login"
        val body = JSONObject()
            .put("username", username)
            .put("password", password)
        val request = Request.Builder()
            .url(url)
            .header("Content-Type", "application/json")
            .post(body.toString().toRequestBody(JSON))
            .build()
        try {
            http.newCall(request).execute().use { response ->
                val text = response.body.string()
                if (response.code == 200) {
                    return sessionTokenFrom(text, 200)
                }
                throw HermesException("HTTP ${response.code}\n${errorSnippet(text)}")
            }
        } catch (e: HermesException) {
            throw e
        } catch (e: Exception) {
            throw HermesException("Network error: ${e.javaClass.simpleName}")
        }
    }

    fun register(baseUrl: String, username: String, password: String): String {
        val url = normalizeBase(baseUrl) + "/api/register"
        val body = JSONObject()
            .put("username", username)
            .put("password", password)
        val request = Request.Builder()
            .url(url)
            .header("Content-Type", "application/json")
            .post(body.toString().toRequestBody(JSON))
            .build()
        try {
            http.newCall(request).execute().use { response ->
                val text = response.body.string()
                if (response.code == 201) {
                    return sessionTokenFrom(text, 201)
                }
                if (registrationClosed(response.code, text)) {
                    throw HermesException("Signup is closed.")
                }
                throw HermesException("HTTP ${response.code}\n${errorSnippet(text)}")
            }
        } catch (e: HermesException) {
            throw e
        } catch (e: Exception) {
            throw HermesException("Network error: ${e.javaClass.simpleName}")
        }
    }

    fun logout(baseUrl: String, sessionToken: String): LogoutResult {
        if (sessionToken.isBlank()) return LogoutResult.Unauthorized
        val url = normalizeBase(baseUrl) + "/api/logout"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $sessionToken")
            .post(ByteArray(0).toRequestBody(null))
            .build()
        try {
            http.newCall(request).execute().use { response ->
                val text = response.body.string()
                return when (response.code) {
                    in 200..299 -> LogoutResult.Success
                    401 -> LogoutResult.Unauthorized
                    else -> LogoutResult.Failed("HTTP ${response.code}\n${errorSnippet(text)}")
                }
            }
        } catch (e: Exception) {
            return LogoutResult.Failed("Network error: ${e.javaClass.simpleName}")
        }
    }

    fun health(baseUrl: String): String {
        val url = normalizeBase(baseUrl) + "/health"
        val request = Request.Builder().url(url).get().build()
        return execute(request)
    }

    fun chat(baseUrl: String, sessionToken: String, messages: JSONArray): String {
        val url = normalizeBase(baseUrl) + "/v1/chat/completions"
        val body = JSONObject()
            .put("model", "hermes")
            .put("messages", messages)
        val builder = Request.Builder()
            .url(url)
            .header("Content-Type", "application/json")
            .post(body.toString().toRequestBody(JSON))
        if (sessionToken.isNotBlank()) builder.header("Authorization", "Bearer $sessionToken")
        val request = builder.build()
        val raw = execute(request)
        return assistantText(raw)
    }

    fun chatHistory(baseUrl: String, sessionToken: String): List<ChatHistoryMessage> {
        val all = mutableListOf<ChatHistoryMessage>()
        var offset = 0
        val limit = 500
        while (true) {
            val page = chatHistoryPage(baseUrl, sessionToken, limit, offset)
            all += page.messages
            if (!page.hasMore) break
            val step = if (page.returned > 0) page.returned else page.messages.size
            if (step <= 0) break
            val next = offset + step
            if (next <= offset) break
            offset = next
        }
        return all
    }

    private fun chatHistoryPage(baseUrl: String, sessionToken: String, limit: Int, offset: Int): ChatHistoryPage {
        val capped = limit.coerceIn(1, 2000)
        val path = "/api/chat/history?limit=$capped&offset=$offset"
        val root = authorizedGet(baseUrl, path, sessionToken)
        val array = root.optJSONArray("messages") ?: JSONArray()
        val messages = mutableListOf<ChatHistoryMessage>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val role = obj.optString("role")
            if (role != "user" && role != "assistant") continue
            val id = jsonLong(obj, "id")
            if (id < 0) continue
            messages += ChatHistoryMessage(
                id = id,
                role = role,
                content = if (obj.has("content") && !obj.isNull("content")) obj.optString("content") else "",
                createdAt = obj.optString("created_at"),
            )
        }
        val returned = if (root.has("returned")) root.optInt("returned") else messages.size
        return ChatHistoryPage(
            messages = messages,
            returned = returned,
            hasMore = root.optBoolean("has_more", false),
        )
    }

    private fun jsonLong(obj: JSONObject, key: String): Long {
        if (!obj.has(key) || obj.isNull(key)) return -1
        return when (val value = obj.opt(key)) {
            is Number -> value.toLong()
            is String -> value.toLongOrNull() ?: -1
            else -> -1
        }
    }

    fun compendiumBooks(baseUrl: String, sessionToken: String): List<CompendiumBook> {
        val root = authorizedGet(baseUrl, "/api/compendium/books?dedupe=true", sessionToken)
        val books = root.optJSONArray("books") ?: JSONArray()
        val out = mutableListOf<CompendiumBook>()
        for (i in 0 until books.length()) {
            val obj = books.optJSONObject(i) ?: continue
            out += CompendiumBook(
                id = obj.optString("id"),
                key = obj.optString("key"),
                name = obj.optString("name"),
                edition = obj.optString("edition"),
                entityCount = obj.optInt("entity_count", 0),
            )
        }
        return out
    }

    fun compendiumKinds(baseUrl: String, sessionToken: String, idOrKey: String): List<CompendiumKind> {
        val root = authorizedGet(baseUrl, "/api/compendium/books/${enc(idOrKey)}", sessionToken)
        val kinds = root.optJSONArray("kinds") ?: JSONArray()
        val out = mutableListOf<CompendiumKind>()
        for (i in 0 until kinds.length()) {
            val obj = kinds.optJSONObject(i) ?: continue
            val kind = obj.optString("kind")
            if (kind.isBlank()) continue
            out += CompendiumKind(kind = kind, count = obj.optInt("count", 0))
        }
        return out
    }

    fun compendiumEntities(
        baseUrl: String,
        sessionToken: String,
        idOrKey: String,
        kind: String,
        query: String,
        offset: Int,
    ): CompendiumPage {
        val path = "/api/compendium/books/${enc(idOrKey)}/entities?kind=${enc(kind)}&q=${enc(query)}&limit=200&offset=$offset&include_data=true"
        val root = authorizedGet(baseUrl, path, sessionToken)
        val entities = root.optJSONArray("entities") ?: JSONArray()
        val hits = mutableListOf<CompendiumHit>()
        for (i in 0 until entities.length()) {
            val obj = entities.optJSONObject(i) ?: continue
            hits += CompendiumHit(id = obj.optString("id"), kind = obj.optString("kind"), name = obj.optString("name"))
        }
        val returned = if (root.has("returned")) root.optInt("returned") else hits.size
        return CompendiumPage(entities = hits, returned = returned, hasMore = root.optBoolean("has_more", false))
    }

    fun compendiumDetail(baseUrl: String, sessionToken: String, entityId: String): String {
        val root = authorizedGet(baseUrl, "/api/compendium/entities/${enc(entityId)}", sessionToken)
        val entity = root.optJSONObject("entity") ?: return "No description stored."
        return visibleDescription(entity.optJSONObject("data"))
    }

    private fun authorizedGet(baseUrl: String, pathAndQuery: String, sessionToken: String): JSONObject {
        if (sessionToken.isBlank()) throw HermesException("The session is missing.")
        val request = Request.Builder()
            .url(normalizeBase(baseUrl) + pathAndQuery)
            .header("Authorization", "Bearer $sessionToken")
            .get()
            .build()
        try {
            http.newCall(request).execute().use { response ->
                val text = response.body.string()
                if (response.code == 401) throw HermesException("HTTP 401")
                if (!response.isSuccessful) throw HermesException("HTTP ${response.code}\n${errorSnippet(text)}")
                return try {
                    JSONObject(text)
                } catch (e: Exception) {
                    throw HermesException("Response was not JSON")
                }
            }
        } catch (e: HermesException) {
            throw e
        } catch (e: Exception) {
            throw HermesException("Network error: ${e.javaClass.simpleName}")
        }
    }

    private fun enc(value: String): String = Uri.encode(value)

    private fun execute(request: Request): String {
        try {
            http.newCall(request).execute().use { response ->
                val text = response.body.string()
                if (!response.isSuccessful) {
                    throw HermesException("HTTP ${response.code}\n${text.take(400)}")
                }
                return text
            }
        } catch (e: HermesException) {
            throw e
        } catch (e: Exception) {
            throw HermesException("Network error: ${e.javaClass.simpleName}")
        }
    }

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()


        private fun sessionTokenFrom(text: String, status: Int): String {
            val root = try {
                JSONObject(text)
            } catch (e: Exception) {
                throw HermesException("HTTP $status\nResponse was not JSON")
            }
            val token = if (root.has("session_token") && !root.isNull("session_token")) {
                root.optString("session_token")
            } else {
                ""
            }
            if (token.isBlank()) throw HermesException("HTTP $status\nMissing session_token")
            return token
        }

        private fun registrationClosed(status: Int, text: String): Boolean {
            if (status == 400) return false
            val error = errorSnippet(text).lowercase()
            val saysClosed = error.contains("signup closed") ||
                error.contains("signup is closed") ||
                error.contains("registration closed") ||
                error.contains("registration is closed")
            if (saysClosed) return true
            // umbra-login handleRegister: the only non-validation rejection that means
            // signup is no longer open is 403 {"error":"Signup closed"}.
            return status == 403
        }

        private fun visibleDescription(data: JSONObject?): String {
            if (data == null || !data.has("description") || data.isNull("description")) {
                return "No description stored."
            }
            return data.optString("description").ifBlank { "No description stored." }
        }

        private fun errorSnippet(text: String): String {
            return try {
                val root = JSONObject(text)
                val error = if (root.has("error") && !root.isNull("error")) root.optString("error") else ""
                if (error.isNotBlank()) error else "Request failed"
            } catch (e: Exception) {
                "Request failed"
            }
        }

        fun normalizeBase(raw: String): String = raw.trim().trimEnd('/')

        fun assistantText(raw: String): String {
            val root = try {
                JSONObject(raw)
            } catch (e: Exception) {
                throw HermesException("Response was not JSON.\n${raw.take(400)}")
            }
            val choices = root.optJSONArray("choices")
                ?: throw HermesException("Response is missing choices[0].message.content")
            if (choices.length() == 0) {
                throw HermesException("Response choices array is empty")
            }
            val message = choices.optJSONObject(0)?.optJSONObject("message")
                ?: throw HermesException("Response is missing choices[0].message")
            if (!message.has("content") || message.isNull("content")) {
                throw HermesException("Response is missing choices[0].message.content")
            }
            return when (val content = message.get("content")) {
                is String -> content
                is JSONArray -> joinParts(content)
                else -> throw HermesException("choices[0].message.content is not text")
            }
        }

        private fun joinParts(parts: JSONArray): String {
            val texts = mutableListOf<String>()
            for (i in 0 until parts.length()) {
                when (val part = parts.get(i)) {
                    is String -> texts += part
                    is JSONObject -> {
                        val text = part.optString("text", part.optString("content", ""))
                        if (text.isNotEmpty()) texts += text
                    }
                }
            }
            if (texts.isEmpty()) {
                throw HermesException("content parts had no text")
            }
            return texts.joinToString("")
        }
    }
}

class HermesException(message: String) : Exception(message)

sealed class LogoutResult {
    data object Success : LogoutResult()
    data object Unauthorized : LogoutResult()
    data class Failed(val message: String) : LogoutResult()
}

data class CompendiumBook(
    val id: String,
    val key: String,
    val name: String,
    val edition: String,
    val entityCount: Int,
)

data class CompendiumKind(val kind: String, val count: Int)

data class CompendiumHit(val id: String, val kind: String, val name: String)

data class CompendiumPage(val entities: List<CompendiumHit>, val returned: Int, val hasMore: Boolean)

data class ChatHistoryMessage(
    val id: Long,
    val role: String,
    val content: String,
    val createdAt: String,
)

data class ChatHistoryPage(
    val messages: List<ChatHistoryMessage>,
    val returned: Int,
    val hasMore: Boolean,
)
