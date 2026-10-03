package com.estidley.umbra

import org.json.JSONArray
import org.json.JSONObject

data class ScenePerson(val id: String, val name: String, val portrait: String?)

data class ParsedTurn(
    val text: String,
    val speakerId: String,
    val speakerName: String,
    val locationName: String?,
    val locationImage: String?,
    val present: List<ScenePerson>?,
    val proposals: List<Proposal>,
)

object SceneJson {
    private val fence = Regex("```json\\s*([\\s\\S]*?)```", RegexOption.IGNORE_CASE)

    const val SYSTEM = """
You are Umbra, the GM for a 5e table. You create characters and then speak as those NPCs.
The player message contains their words and one fenced json block:
{"audience":"umbra|group|area|whisper","whisperTo":null,"text":"what they typed"}
whisperTo is null unless audience is whisper, then {"id":"...","name":"..."}.
Reply with exactly one fenced json block and no other json fence:
{"text":"words to show","speaker":{"id":"","name":"","portrait":null},"location":{"name":"","image":null},"present":[{"id":"","name":"","portrait":null}],"proposals":[]}
portrait and image must be null. text is what the player reads. speaker is who is talking. location is where the player is. present replaces who is in the scene.
proposals stays [] unless you propose a sheet or book change. Each proposal is {"kind":"sheet|book","summary":"human readable","target":"inventory|species|subclass|monster|encounter|sheet","data":{}}. Do not claim the change already happened. The player confirms it.
"""

    fun playerContent(audience: String, whisperTo: ScenePerson?, text: String): String {
        val whisper = if (audience == "whisper" && whisperTo != null) {
            JSONObject().put("id", whisperTo.id).put("name", whisperTo.name)
        } else {
            JSONObject.NULL
        }
        val payload = JSONObject()
            .put("audience", audience)
            .put("whisperTo", whisper)
            .put("text", text)
        return text + "\n\n```json\n" + payload.toString() + "\n```"
    }

    fun parseAssistant(raw: String): ParsedTurn? {
        for (match in fence.findAll(raw)) {
            val obj = try {
                JSONObject(match.groupValues[1].trim())
            } catch (_: Exception) {
                continue
            }
            val looksLikeTurn = obj.has("text") || obj.has("speaker") || obj.has("present") || obj.has("location")
            if (!looksLikeTurn) continue
            val speaker = obj.optJSONObject("speaker")
            val location = obj.optJSONObject("location")
            val present = if (obj.has("present")) people(obj.optJSONArray("present")) else null
            return ParsedTurn(
                text = obj.optString("text"),
                speakerId = speaker?.optString("id").orEmpty(),
                speakerName = speaker?.optString("name").orEmpty().ifBlank { "Umbra" },
                locationName = location?.optString("name")?.takeIf { it.isNotBlank() },
                locationImage = nullableUrl(location?.opt("image")),
                present = present,
                proposals = proposals(obj.optJSONArray("proposals")),
            )
        }
        return null
    }

    private fun people(array: JSONArray?): List<ScenePerson> {
        if (array == null) return emptyList()
        val out = mutableListOf<ScenePerson>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val name = obj.optString("name")
            if (name.isBlank()) continue
            out += ScenePerson(
                id = obj.optString("id").ifBlank { name },
                name = name,
                portrait = nullableUrl(obj.opt("portrait")),
            )
        }
        return out
    }

    private fun proposals(array: JSONArray?): List<Proposal> {
        if (array == null) return emptyList()
        val out = mutableListOf<Proposal>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val kind = obj.optString("kind")
            val target = obj.optString("target")
            if (kind !in setOf("sheet", "book") || target.isBlank()) continue
            out += Proposal(
                kind = kind,
                summary = obj.optString("summary").ifBlank { "$kind $target" },
                target = target,
                data = obj.optJSONObject("data") ?: JSONObject(),
            )
        }
        return out
    }

    private fun nullableUrl(value: Any?): String? {
        if (value == null || value == JSONObject.NULL) return null
        val text = value.toString()
        return text.takeIf { it.isNotBlank() && it != "null" }
    }
}