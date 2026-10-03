package com.estidley.umbra

import android.content.Context

class SpeakerColors(context: Context) {
    private val prefs = context.getSharedPreferences("umbra_speaker_colors", Context.MODE_PRIVATE)

    fun colorFor(explicitId: String, name: String, present: List<ScenePerson>): Int {
        val matched = present.firstOrNull { person ->
            person.id.isNotBlank() && person.name.equals(name, ignoreCase = true)
        }?.id.orEmpty()
        val id = explicitId.trim().ifBlank { matched }
        if (id.isNotBlank()) {
            val alias = nameKey(name)
            if (!prefs.contains(id) && alias.isNotBlank() && prefs.contains(alias)) {
                prefs.edit().putInt(id, prefs.getInt(alias, 0)).apply()
            }
            return stored(id)
        }
        return stored(nameKey(name).ifBlank { "speaker" })
    }

    private fun stored(key: String): Int {
        if (prefs.contains(key)) return prefs.getInt(key, palette[0])
        val color = palette[stableIndex(key)]
        prefs.edit().putInt(key, color).apply()
        return color
    }

    private fun nameKey(name: String): String {
        val cleaned = name.trim().lowercase()
        return if (cleaned.isBlank()) "" else "name:$cleaned"
    }

    private fun stableIndex(key: String): Int {
        var hash = 0
        for (char in key) hash = (hash * 33 + char.code) and 0x7fffffff
        return hash % palette.size
    }

    private companion object {
        val palette = intArrayOf(
            0xFF1F6F8B.toInt(),
            0xFF2F6B4F.toInt(),
            0xFF8A5A2B.toInt(),
            0xFF8C3D4E.toInt(),
            0xFF3E5C3A.toInt(),
            0xFF245C7A.toInt(),
            0xFF6E5428.toInt(),
            0xFF3F6B62.toInt(),
            0xFF7A4032.toInt(),
            0xFF1E5A4A.toInt(),
        )
    }
}
