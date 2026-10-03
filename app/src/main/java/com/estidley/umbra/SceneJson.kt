package com.estidley.umbra

import org.json.JSONArray
import org.json.JSONObject

data class ScenePerson(val id: String, val name: String, val portrait: String?)

data class SpeechBeat(val speakerId: String, val speakerName: String, val text: String, val portrait: String? = null)

data class ParsedTurn(
    val text: String,
    val speakerId: String,
    val speakerName: String,
    val locationName: String?,
    val locationImage: String?,
    val present: List<ScenePerson>?,
    val proposals: List<Proposal>,
    val beats: List<SpeechBeat> = emptyList(),
    val speakerPortrait: String? = null,
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
proposals stays [] unless you propose a sheet or book change. Each proposal is {"kind":"sheet|book","summary":"human readable","target":"inventory|species|subclass|monster|encounter|sheet","data":{}}. The app writes those proposals itself. Do not ask the player to confirm.
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

    const val INCOMPLETE = "The reply was incomplete."

    fun parseAssistant(raw: String): AssistantReply {
        for (match in fence.findAll(raw)) {
            val body = match.groupValues[1].trim()
            val listed = beatList(body)
            if (listed != null) return AssistantReply.Complete(beatsOnly(listed))
            val obj = try {
                JSONObject(body)
            } catch (_: Exception) {
                continue
            }
            val looksLikeTurn = obj.has("text") || obj.has("speaker") || obj.has("present") || obj.has("location")
            if (!looksLikeTurn) continue
            val turn = completeTurn(obj) ?: return AssistantReply.Incomplete
            return AssistantReply.Complete(turn)
        }
        return AssistantReply.Incomplete
    }

    private fun completeTurn(obj: JSONObject): ParsedTurn? {
        val speaker = jsonObject(obj, "speaker") ?: return null
        val location = jsonObject(obj, "location") ?: return null
        val speakerId = requiredText(speaker, "id") ?: return null
        val speakerName = requiredText(speaker, "name") ?: return null
        val speakerPortraitField = nullableUrlField(speaker, "portrait") ?: return null
        val locationName = requiredText(location, "name") ?: return null
        val locationImage = nullableUrlField(location, "image") ?: return null
        if (!obj.has("present") || obj.isNull("present") || obj.opt("present") !is JSONArray) return null
        val presentRaw = obj.optJSONArray("present") ?: return null
        val present = mutableListOf<ScenePerson>()
        for (i in 0 until presentRaw.length()) {
            val person = presentRaw.optJSONObject(i) ?: return null
            val id = requiredText(person, "id") ?: return null
            val name = requiredText(person, "name") ?: return null
            val portrait = nullableUrlField(person, "portrait") ?: return null
            present += ScenePerson(id = id, name = name, portrait = portrait.value)
        }
        val beats = beatsIn(obj)
        val prose = if (obj.has("text") && !obj.isNull("text") && obj.opt("text") is String) obj.optString("text") else ""
        val text = if (beats.isNotEmpty()) "" else prose
        val proposals = if (obj.has("proposals")) proposals(obj.optJSONArray("proposals")) else emptyList()
        return ParsedTurn(
            text = text,
            speakerId = speakerId,
            speakerName = speakerName,
            locationName = locationName,
            locationImage = locationImage.value,
            present = present,
            proposals = proposals,
            beats = beats,
            speakerPortrait = speakerPortraitField.value,
        )
    }


    private fun beatsOnly(beats: List<SpeechBeat>): ParsedTurn = ParsedTurn(
        text = "",
        speakerId = "",
        speakerName = "",
        locationName = null,
        locationImage = null,
        present = null,
        proposals = emptyList(),
        beats = beats,
    )

    private fun beatsIn(obj: JSONObject): List<SpeechBeat> {
        beatList(obj.opt("text"))?.let { return it }
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            if (key == "text" || key == "present" || key == "proposals") continue
            beatList(obj.opt(key))?.let { return it }
        }
        return emptyList()
    }

    private fun beatList(value: Any?): List<SpeechBeat>? {
        val array = when (value) {
            is JSONArray -> value
            is String -> {
                val trimmed = value.trim()
                if (!trimmed.startsWith("[")) return null
                try {
                    JSONArray(trimmed)
                } catch (_: Exception) {
                    return null
                }
            }
            else -> return null
        }
        if (array.length() == 0) return null
        val out = mutableListOf<SpeechBeat>()
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: return null
            if (!item.has("text") || item.isNull("text") || item.opt("text") !is String) return null
            val (id, name) = beatSpeaker(item)
            out += SpeechBeat(speakerId = id, speakerName = name, text = item.getString("text"), portrait = beatPortrait(item))
        }
        return out
    }

    private fun beatPortrait(obj: JSONObject): String? {
        val speaker = obj.opt("speaker")
        if (speaker is JSONObject) {
            val field = nullableUrlField(speaker, "portrait")
            if (field != null) return field.value
        }
        return nullableUrlField(obj, "portrait")?.value
    }

    private fun beatSpeaker(obj: JSONObject): Pair<String, String> {
        val speaker = obj.opt("speaker")
        if (speaker is JSONObject) {
            val id = if (speaker.opt("id") is String) speaker.optString("id") else ""
            val name = if (speaker.opt("name") is String) speaker.optString("name") else ""
            return id to name
        }
        if (speaker is String) return "" to speaker
        val id = if (obj.opt("id") is String) obj.optString("id") else ""
        val name = if (obj.opt("name") is String) obj.optString("name") else ""
        return id to name
    }

    private fun jsonObject(obj: JSONObject, key: String): JSONObject? {
        if (!obj.has(key) || obj.isNull(key)) return null
        return obj.optJSONObject(key)
    }

    private fun requiredText(obj: JSONObject, key: String): String? {
        if (!obj.has(key) || obj.isNull(key)) return null
        val value = obj.opt(key)
        if (value !is String || value.isBlank()) return null
        return value
    }

    private class PresentUrl(val value: String?)

    private fun nullableUrlField(obj: JSONObject, key: String): PresentUrl? {
        if (!obj.has(key)) return null
        if (obj.isNull(key)) return PresentUrl(null)
        val value = obj.opt(key)
        if (value !is String) return null
        return PresentUrl(value.takeIf { it.isNotBlank() && it != "null" })
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

sealed class AssistantReply {
    data class Complete(val turn: ParsedTurn) : AssistantReply()
    data object Incomplete : AssistantReply()
}
