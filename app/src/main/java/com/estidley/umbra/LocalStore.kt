package com.estidley.umbra

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class Proposal(
    val kind: String,
    val summary: String,
    val target: String,
    val data: JSONObject,
)

class LocalStore(private val context: Context) {
    private val profile: SharedPreferences =
        context.getSharedPreferences("umbra_profile", Context.MODE_PRIVATE)

    private val secrets: SharedPreferences by lazy { openSecrets() }

    private val characterFile = File(context.filesDir, "character.json")
    private val bookFile = File(context.filesDir, "book.json")

    fun displayName(): String = profile.getString("display_name", "").orEmpty()

    fun setDisplayName(name: String) {
        profile.edit().putString("display_name", name.trim()).apply()
    }

    fun clearDisplayName() {
        profile.edit().remove("display_name").apply()
    }

    fun baseUrl(): String {
        val saved = secrets.getString("base_url", null)
        return if (saved.isNullOrBlank()) BuildConfig.HERMES_BASE_URL else saved
    }

    fun bearer(): String = secrets.getString("bearer", "").orEmpty()

    fun saveBaseUrl(baseUrl: String) {
        secrets.edit()
            .putString("base_url", HermesClient.normalizeBase(baseUrl))
            .remove("api_key")
            .apply()
    }

    /** Later login stores the credential Webb's server returns. Not called until that path exists. */
    fun saveBearer(token: String) {
        secrets.edit().putString("bearer", token.trim()).remove("api_key").apply()
    }

    fun dropApiKey() {
        if (secrets.contains("api_key")) secrets.edit().remove("api_key").apply()
    }

    fun character(): JSONObject {
        if (!characterFile.exists()) {
            val created = emptyCharacter(displayName().ifBlank { "Adventurer" })
            writeCharacter(created)
            return created
        }
        return JSONObject(characterFile.readText())
    }

    fun writeCharacter(doc: JSONObject) {
        characterFile.writeText(doc.toString(2))
    }

    fun book(): JSONObject {
        if (!bookFile.exists()) {
            val seeded = seedBook()
            bookFile.writeText(seeded.toString(2))
            return seeded
        }
        val book = JSONObject(bookFile.readText())
        if (book.optJSONArray("features") == null) {
            book.put("features", entities("features.json").filterKind("feature"))
        }
        return book
    }

    fun writeBook(book: JSONObject) {
        bookFile.writeText(book.toString(2))
    }

    fun applyProposal(proposal: Proposal): String {
        return when (proposal.target) {
            "inventory" -> applyInventory(proposal.data)
            "species" -> if (proposal.kind == "book") addEntity("species", proposal.data, "species")
            else setSpecies(proposal.data)
            "subclass" -> if (proposal.kind == "book") addEntity("subclasses", proposal.data, "subclass")
            else setSubclass(proposal.data)
            "monster" -> addEntity("monsters", proposal.data, "monster")
            "encounter" -> addEncounter(proposal.data)
            "sheet" -> mergeSheet(proposal.data)
            else -> throw IllegalArgumentException("Unknown proposal target ${proposal.target}")
        }
    }

    private fun applyInventory(data: JSONObject): String {
        val name = data.optString("name").ifBlank { data.optString("item") }
        if (name.isBlank()) throw IllegalArgumentException("Inventory proposal needs a name")
        val item = JSONObject()
            .put("name", name.take(160))
            .put("quantity", data.optInt("quantity", 1).coerceAtLeast(1))
            .put("equipped", data.optBoolean("equipped", false))
            .put("attuned", data.optBoolean("attuned", false))
        val itemId = data.optString("itemId")
        if (itemId.isNotBlank()) item.put("itemId", itemId)
        if (data.has("notes")) item.put("notes", data.optString("notes").take(500))
        val doc = character()
        val document = doc.getJSONObject("document")
        val items = document.optJSONArray("items") ?: JSONArray()
        items.put(item)
        document.put("items", items)
        writeCharacter(doc)
        return "Added $name to inventory"
    }

    private fun setSpecies(data: JSONObject): String {
        val id = data.optString("speciesId", data.optString("id"))
        if (id.isBlank()) throw IllegalArgumentException("Species proposal needs speciesId")
        val doc = character()
        doc.getJSONObject("document").put("speciesId", id)
        writeCharacter(doc)
        return "Species set to $id"
    }

    private fun setSubclass(data: JSONObject): String {
        val id = data.optString("subclassId", data.optString("id"))
        if (id.isBlank()) throw IllegalArgumentException("Subclass proposal needs subclassId")
        val doc = character()
        val classes = doc.getJSONObject("document").optJSONArray("classes") ?: JSONArray()
        if (classes.length() == 0) throw IllegalArgumentException("Sheet has no class to attach a subclass")
        classes.getJSONObject(0).put("subclassId", id)
        writeCharacter(doc)
        return "Subclass set to $id"
    }

    private fun addEntity(bucket: String, data: JSONObject, defaultKind: String): String {
        val name = data.optString("name")
        if (name.isBlank()) throw IllegalArgumentException("$defaultKind needs a name")
        val entity = JSONObject(data.toString())
        if (!entity.has("kind")) entity.put("kind", defaultKind)
        if (entity.optString("id").isBlank()) {
            entity.put("id", "umbra.$defaultKind.${slug(name)}")
        }
        if (!entity.has("edition")) entity.put("edition", "2014")
        val book = book()
        val list = book.optJSONArray(bucket) ?: JSONArray()
        list.put(entity)
        book.put(bucket, list)
        writeBook(book)
        return "Added $defaultKind ${entity.getString("name")} to the local book"
    }

    private fun addEncounter(data: JSONObject): String {
        val name = data.optString("name")
        if (name.isBlank()) throw IllegalArgumentException("Encounter needs a name")
        val encounter = JSONObject()
            .put("name", name.take(120))
            .put("notes", data.optString("notes"))
            .put("entries", data.optJSONArray("entries") ?: JSONArray())
        val book = book()
        val list = book.optJSONArray("encounters") ?: JSONArray()
        list.put(encounter)
        book.put("encounters", list)
        writeBook(book)
        return "Added encounter $name"
    }

    private fun mergeSheet(data: JSONObject): String {
        val doc = character()
        if (data.has("name")) doc.put("name", data.getString("name").take(120))
        val document = doc.getJSONObject("document")
        val keys = listOf(
            "abilities", "speciesId", "subspeciesId", "backgroundId",
            "classes", "items", "skillProficiencies", "details", "hp",
        )
        for (key in keys) {
            if (data.has(key)) document.put(key, data.get(key))
        }
        writeCharacter(doc)
        return "Updated the character sheet"
    }

    private fun seedBook(): JSONObject {
        val species = entities("species.json").filterKind("species", "subspecies")
        val classes = entities("classes.json").filterKind("class")
        val features = entities("features.json")
        val subclasses = features.filterKind("subclass")
        val monsters = entities("monsters.json").filterKind("monster")
        return JSONObject()
            .put("species", species)
            .put("classes", classes)
            .put("subclasses", subclasses)
            .put("features", features.filterKind("feature"))
            .put("monsters", monsters)
            .put("items", entities("items.json").filterKind("item"))
            .put("encounters", JSONArray())
    }

    private fun entities(asset: String): JSONArray {
        val text = context.assets.open("srd/$asset").bufferedReader().use { it.readText() }
        val root = JSONObject(text)
        return root.optJSONArray("entities") ?: JSONArray()
    }

    private fun JSONArray.filterKind(vararg kinds: String): JSONArray {
        val out = JSONArray()
        val allow = kinds.toSet()
        for (i in 0 until length()) {
            val obj = optJSONObject(i) ?: continue
            if (obj.optString("kind") in allow) out.put(obj)
        }
        return out
    }

    private fun openSecrets(): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            "umbra_secrets",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    companion object {
        fun emptyCharacter(name: String): JSONObject {
            val abilities = JSONObject()
            for (key in listOf("str", "dex", "con", "int", "wis", "cha")) abilities.put(key, 10)
            val classes = JSONArray().put(
                JSONObject()
                    .put("classId", "srd.class.fighter")
                    .put("level", 1)
                    .put("hitDiceSpent", 0),
            )
            val document = JSONObject()
                .put("version", 1)
                .put("edition", "2014")
                .put("abilities", abilities)
                .put("classes", classes)
                .put("speciesId", "srd.species.human")
                .put("items", JSONArray())
            return JSONObject()
                .put("format", "vtt.character.v1")
                .put("name", name.take(120))
                .put("document", document)
        }

        fun slug(name: String): String {
            val cleaned = name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
            return cleaned.ifBlank { "entry" }.take(40)
        }

        fun proposalsFrom(reply: String): List<Proposal> {
            val pattern = Regex("```json\\s*([\\s\\S]*?)```", RegexOption.IGNORE_CASE)
            val found = mutableListOf<Proposal>()
            for (match in pattern.findAll(reply)) {
                val parsed = try {
                    JSONObject(match.groupValues[1].trim())
                } catch (_: Exception) {
                    continue
                }
                val array = parsed.optJSONArray("proposals") ?: continue
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val kind = obj.optString("kind")
                    val target = obj.optString("target")
                    if (kind !in setOf("sheet", "book") || target.isBlank()) continue
                    found += Proposal(
                        kind = kind,
                        summary = obj.optString("summary").ifBlank { "$kind $target" },
                        target = target,
                        data = obj.optJSONObject("data") ?: JSONObject(),
                    )
                }
            }
            return found
        }
    }
}