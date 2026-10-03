package com.estidley.umbra

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
                    val root = try {
                        JSONObject(text)
                    } catch (e: Exception) {
                        throw HermesException("HTTP 200\nResponse was not JSON")
                    }
                    val key = if (root.has("api_key") && !root.isNull("api_key")) root.optString("api_key") else ""
                    if (key.isBlank()) throw HermesException("HTTP 200\nMissing api_key")
                    return key
                }
                throw HermesException("HTTP ${response.code}\n${errorSnippet(text)}")
            }
        } catch (e: HermesException) {
            throw e
        } catch (e: Exception) {
            throw HermesException("Network error: ${e.message ?: e.javaClass.simpleName}")
        }
    }

    fun health(baseUrl: String): String {
        val url = normalizeBase(baseUrl) + "/health"
        val request = Request.Builder().url(url).get().build()
        return execute(request)
    }

    fun chat(baseUrl: String, bearer: String, messages: JSONArray): String {
        val url = normalizeBase(baseUrl) + "/v1/chat/completions"
        val body = JSONObject()
            .put("model", "hermes")
            .put("messages", messages)
        val builder = Request.Builder()
            .url(url)
            .header("Content-Type", "application/json")
            .post(body.toString().toRequestBody(JSON))
        if (bearer.isNotBlank()) builder.header("Authorization", "Bearer $bearer")
        val request = builder.build()
        val raw = execute(request)
        return assistantText(raw)
    }

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
            throw HermesException("Network error: ${e.message ?: e.javaClass.simpleName}")
        }
    }

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()


        private fun errorSnippet(text: String): String {
            val fallback = text.take(400)
            return try {
                val root = JSONObject(text)
                val error = if (root.has("error") && !root.isNull("error")) root.optString("error") else ""
                if (error.isNotBlank()) error else fallback
            } catch (e: Exception) {
                fallback
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