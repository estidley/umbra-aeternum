package com.estidley.umbra

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

private val Ink = Color(0xFF0B0D12)
private val Card = Color(0xFF12151C)
private val Field = Color(0xFF1B1F29)
private val Gold = Color(0xFFD97A2B)
private val Muted = Color(0xFFA3A9BA)
private val Faint = Color(0xFF7C8399)
private val Player = Color(0xFF1B1F29)

private val Audiences = listOf("umbra" to "Umbra", "group" to "Group", "area" to "Area", "whisper" to "Whisper")

private val Skills = listOf(
    "acrobatics" to "Acrobatics",
    "animalhandling" to "Animal Handling",
    "arcana" to "Arcana",
    "athletics" to "Athletics",
    "deception" to "Deception",
    "history" to "History",
    "insight" to "Insight",
    "intimidation" to "Intimidation",
    "investigation" to "Investigation",
    "medicine" to "Medicine",
    "nature" to "Nature",
    "perception" to "Perception",
    "performance" to "Performance",
    "persuasion" to "Persuasion",
    "religion" to "Religion",
    "sleightofhand" to "Sleight of Hand",
    "stealth" to "Stealth",
    "survival" to "Survival",
)

data class ChatLine(val role: String, val text: String, val speaker: String, val apiContent: String)

data class UmbraUiState(
    val connected: Boolean = false,
    val busy: Boolean = false,
    val banner: String = "",
    val baseUrl: String = "",
    val username: String = "",
    val password: String = "",
    val accountScreen: String = "login",
    val tab: String = "chat",
    val audience: String = "group",
    val whisperTo: ScenePerson? = null,
    val present: List<ScenePerson> = emptyList(),
    val locationName: String = "Somewhere unlit",
    val lines: List<ChatLine> = emptyList(),
    val draft: String = "",
    val pending: Proposal? = null,
    val character: JSONObject? = null,
    val book: JSONObject? = null,
    val sheetTick: Int = 0,
    val compendiumCategory: String = "",
    val compendiumQuery: String = "",
    val compendiumId: String = "",
    val compendiumBooks: List<CompendiumBook> = emptyList(),
    val compendiumBookKey: String = "",
    val compendiumKinds: List<CompendiumKind> = emptyList(),
    val compendiumHits: List<CompendiumHit> = emptyList(),
    val compendiumDetail: String = "",
    val compendiumHasMore: Boolean = false,
    val compendiumNextOffset: Int = 0,
    val compendiumError: String = "",
)

class UmbraViewModel(app: Application) : AndroidViewModel(app) {
    private val store = LocalStore(app)
    private val hermes = HermesClient()
    private var signingOut = false
    private var compendiumSearch: Job? = null
    private val _state = MutableStateFlow(
        UmbraUiState(baseUrl = store.baseUrl()),
    )
    val state: StateFlow<UmbraUiState> = _state

    fun setBaseUrl(value: String) = _state.update { it.copy(baseUrl = value) }
    fun setUsername(value: String) = _state.update { it.copy(username = value) }
    fun setPassword(value: String) = _state.update { it.copy(password = value) }

    fun showRegister() = _state.update { it.copy(accountScreen = "register", banner = "", password = "") }

    fun showLogin() = _state.update { it.copy(accountScreen = "login", banner = "", password = "") }
    fun setDraft(value: String) = _state.update { it.copy(draft = value) }
    fun selectTab(tab: String) {
        _state.update { it.copy(tab = tab) }
        if (tab == "compendium" && _state.value.compendiumBooks.isEmpty()) loadCompendiumBooks()
    }

    fun setQuery(value: String) {
        _state.update { it.copy(compendiumQuery = value, compendiumId = "", compendiumDetail = "") }
        compendiumSearch?.cancel()
        compendiumSearch = viewModelScope.launch {
            delay(350)
            if (_state.value.compendiumBookKey.isNotBlank()) reloadCompendium(reset = true)
        }
    }

    fun setCategory(value: String) {
        _state.update { it.copy(compendiumCategory = value, compendiumId = "", compendiumDetail = "") }
        viewModelScope.launch { reloadCompendium(reset = true) }
    }

    fun selectEntry(id: String) {
        if (id.isBlank()) return
        _state.update { it.copy(compendiumId = id, compendiumDetail = "") }
        viewModelScope.launch {
            val detail = compendiumCall { hermes.compendiumDetail(store.baseUrl(), store.sessionToken(), id) } ?: return@launch
            _state.update { if (it.compendiumId == id) it.copy(compendiumDetail = detail) else it }
        }
    }

    fun selectCompendiumBook(idOrKey: String) {
        if (idOrKey.isBlank()) return
        compendiumSearch?.cancel()
        _state.update {
            it.copy(
                compendiumBookKey = idOrKey,
                compendiumCategory = "",
                compendiumId = "",
                compendiumDetail = "",
                compendiumHits = emptyList(),
                compendiumKinds = emptyList(),
                compendiumHasMore = false,
                compendiumNextOffset = 0,
                compendiumError = "",
            )
        }
        viewModelScope.launch {
            val kinds = compendiumCall { hermes.compendiumKinds(store.baseUrl(), store.sessionToken(), idOrKey) } ?: return@launch
            if (_state.value.compendiumBookKey != idOrKey) return@launch
            _state.update { it.copy(compendiumKinds = kinds) }
            reloadCompendium(reset = true)
        }
    }

    fun loadMoreCompendium() {
        if (!_state.value.compendiumHasMore) return
        viewModelScope.launch { reloadCompendium(reset = false) }
    }

    private fun loadCompendiumBooks() {
        viewModelScope.launch {
            val books = compendiumCall { hermes.compendiumBooks(store.baseUrl(), store.sessionToken()) } ?: return@launch
            _state.update { it.copy(compendiumBooks = books, compendiumError = "") }
        }
    }

    private suspend fun reloadCompendium(reset: Boolean) {
        val snap = _state.value
        val key = snap.compendiumBookKey
        if (key.isBlank()) return
        val offset = if (reset) 0 else snap.compendiumNextOffset
        val page = compendiumCall {
            hermes.compendiumEntities(
                store.baseUrl(),
                store.sessionToken(),
                key,
                snap.compendiumCategory,
                snap.compendiumQuery.trim(),
                offset,
            )
        } ?: return
        if (_state.value.compendiumBookKey != key) return
        _state.update {
            it.copy(
                compendiumHits = if (reset) page.entities else it.compendiumHits + page.entities,
                compendiumHasMore = page.hasMore,
                compendiumNextOffset = offset + page.returned,
                compendiumError = "",
            )
        }
    }

    private suspend fun <T> compendiumCall(block: () -> T): T? {
        if (store.sessionToken().isBlank()) {
            missingSession()
            return null
        }
        return try {
            withContext(Dispatchers.IO) { block() }
        } catch (e: HermesException) {
            val message = e.message.orEmpty()
            if (message == "HTTP 401" || message == "The session is missing.") missingSession()
            else _state.update { it.copy(compendiumError = message.ifBlank { "Network error" }) }
            null
        }
    }

    private fun missingSession() {
        store.clearSession()
        _state.update {
            it.copy(connected = false, busy = false, password = "", banner = "The session is missing.", pending = null)
        }
    }

    fun setAudience(audience: String) {
        if (audience !in setOf("umbra", "group", "area", "whisper")) return
        _state.update { it.copy(audience = audience) }
    }

    fun selectWhisper(person: ScenePerson) {
        _state.update { it.copy(audience = "whisper", whisperTo = person) }
    }

    fun disconnect() {
        if (signingOut) return
        signingOut = true
        val token = store.sessionToken()
        val base = store.baseUrl()
        viewModelScope.launch {
            _state.update { it.copy(busy = true, banner = "") }
            val result = withContext(Dispatchers.IO) {
                hermes.logout(base, token)
            }
            signingOut = false
            when (result) {
                LogoutResult.Success, LogoutResult.Unauthorized -> {
                    store.clearSession()
                    _state.update {
                        it.copy(
                            busy = false,
                            connected = false,
                            pending = null,
                            banner = "",
                            password = "",
                        )
                    }
                }
                is LogoutResult.Failed -> {
                    _state.update { it.copy(busy = false, banner = result.message) }
                }
            }
        }
    }

    fun checkConnection() = submitAccount(registering = false)

    fun registerAccount() = submitAccount(registering = true)

    private fun submitAccount(registering: Boolean) {
        val current = _state.value
        if (current.username.isBlank() || current.password.isBlank()) {
            _state.update {
                it.copy(
                    busy = false,
                    connected = false,
                    password = "",
                    banner = "Username and password are required.",
                )
            }
            return
        }
        val legacy = "https://hermes-agent-production-76d3.up.railway.app"
        val base = if (HermesClient.normalizeBase(current.baseUrl) == legacy) {
            BuildConfig.HERMES_BASE_URL
        } else {
            current.baseUrl
        }
        store.saveBaseUrl(base)
        viewModelScope.launch {
            _state.update { it.copy(busy = true, banner = "", baseUrl = store.baseUrl()) }
            val outcome = withContext(Dispatchers.IO) {
                try {
                    val token = if (registering) {
                        hermes.register(store.baseUrl(), current.username, current.password)
                    } else {
                        hermes.login(store.baseUrl(), current.username, current.password)
                    }
                    token to null
                } catch (e: Exception) {
                    null to (e.message ?: if (registering) "Register failed" else "Login failed")
                }
            }
            val token = outcome.first
            val error = outcome.second
            if (token.isNullOrBlank()) {
                _state.update {
                    it.copy(
                        busy = false,
                        connected = false,
                        password = "",
                        banner = error ?: "HTTP 200\nMissing session_token",
                    )
                }
            } else {
                store.saveSession(token)
                if (!getApplication<Application>().filesDir.resolve("character.json").exists()) {
                    store.writeCharacter(LocalStore.emptyCharacter("Adventurer"))
                }
                _state.update {
                    it.copy(
                        busy = false,
                        connected = true,
                        accountScreen = "login",
                        banner = "",
                        username = "",
                        password = "",
                        baseUrl = store.baseUrl(),
                        character = store.character(),
                        book = store.book(),
                        sheetTick = it.sheetTick + 1,
                        tab = "chat",
                    )
                }
            }
        }
    }

    fun sendChat() {
        val current = _state.value
        val text = current.draft.trim()
        if (text.isEmpty() || current.busy) return
        if (current.audience == "whisper" && current.whisperTo == null) {
            _state.update { it.copy(banner = "Choose someone to whisper to") }
            return
        }
        val whisper = if (current.audience == "whisper") current.whisperTo else null
        val wire = SceneJson.playerContent(current.audience, whisper, text)
        val history = current.lines + ChatLine("user", text, "You", wire)
        _state.update { it.copy(lines = history, draft = "", busy = true, banner = "") }
        viewModelScope.launch {
            val raw = withContext(Dispatchers.IO) {
                try {
                    hermes.chat(store.baseUrl(), store.sessionToken(), messages(history))
                } catch (e: Exception) {
                    "ERROR:" + (e.message ?: "Chat failed")
                }
            }
            if (!_state.value.connected) {
                _state.update { it.copy(busy = false) }
                return@launch
            }
            if (raw.startsWith("ERROR:")) {
                _state.update {
                    it.copy(busy = false, lines = history + ChatLine("error", raw.removePrefix("ERROR:"), "Error", raw))
                }
                return@launch
            }
            when (val reply = SceneJson.parseAssistant(raw)) {
                AssistantReply.Incomplete -> {
                    _state.update {
                        it.copy(
                            busy = false,
                            lines = history + ChatLine("assistant", SceneJson.INCOMPLETE, "", SceneJson.INCOMPLETE),
                            pending = null,
                        )
                    }
                }
                is AssistantReply.Complete -> {
                    val turn = reply.turn
                    val notes = mutableListOf<String>()
                    for (proposal in turn.proposals) {
                        notes += try {
                            store.applyProposal(proposal)
                        } catch (e: Exception) {
                            "Could not apply"
                        }
                    }
                    _state.update {
                        it.copy(
                            busy = false,
                            lines = history + ChatLine("assistant", turn.text, turn.speakerName, raw),
                            present = turn.present ?: emptyList(),
                            locationName = turn.locationName ?: it.locationName,
                            banner = if (notes.isEmpty()) "" else notes.joinToString("\n"),
                            pending = null,
                            character = store.character(),
                            book = store.book(),
                            sheetTick = it.sheetTick + 1,
                        )
                    }
                }
            }
        }
    }

    fun saveIdentity(name: String, species: String, subspecies: String, background: String, classId: String, subclass: String, level: String, alignment: String) {
        val doc = store.character()
        doc.put("name", name.take(120).ifBlank { "Adventurer" })
        val document = doc.getJSONObject("document")
        if (species.isNotBlank()) document.put("speciesId", species) else document.remove("speciesId")
        if (subspecies.isNotBlank()) document.put("subspeciesId", subspecies) else document.remove("subspeciesId")
        if (background.isNotBlank()) document.put("backgroundId", background) else document.remove("backgroundId")
        val details = document.optJSONObject("details") ?: JSONObject()
        if (alignment.isBlank()) details.remove("alignment") else details.put("alignment", alignment.take(60))
        document.put("details", details)
        val classes = document.optJSONArray("classes") ?: JSONArray()
        val row = if (classes.length() == 0) JSONObject().put("hitDiceSpent", 0) else classes.getJSONObject(0)
        if (classId.isNotBlank()) row.put("classId", classId)
        row.put("level", level.toIntOrNull()?.coerceIn(1, 20) ?: row.optInt("level", 1))
        if (subclass.isBlank()) row.remove("subclassId") else row.put("subclassId", subclass)
        if (classes.length() == 0) classes.put(row)
        document.put("classes", classes)
        store.writeCharacter(doc)
        bump()
    }

    fun saveAbilities(scores: Map<String, String>) {
        val doc = store.character()
        val abilities = doc.getJSONObject("document").optJSONObject("abilities") ?: JSONObject()
        for ((key, raw) in scores) {
            val n = raw.toIntOrNull() ?: continue
            abilities.put(key, n.coerceIn(1, 30))
        }
        doc.getJSONObject("document").put("abilities", abilities)
        store.writeCharacter(doc)
        bump()
    }

    fun toggleSkill(key: String, expertise: Boolean) {
        val doc = store.character()
        val document = doc.getJSONObject("document")
        val field = if (expertise) "skillExpertise" else "skillProficiencies"
        val array = document.optJSONArray(field) ?: JSONArray()
        val values = (0 until array.length()).map { array.getString(it) }.toMutableList()
        if (key in values) values.remove(key) else values.add(key)
        document.put(field, JSONArray(values))
        store.writeCharacter(doc)
        bump()
    }

    fun saveCombat(current: String, temp: String, hitDice: String, inspiration: Boolean, conditions: String) {
        val doc = store.character()
        val document = doc.getJSONObject("document")
        val hp = document.optJSONObject("hp") ?: JSONObject()
        val cur = current.toIntOrNull()
        if (cur == null) hp.remove("current") else hp.put("current", cur)
        hp.put("temp", temp.toIntOrNull()?.coerceAtLeast(0) ?: 0)
        document.put("hp", hp)
        val classes = document.optJSONArray("classes") ?: JSONArray()
        if (classes.length() > 0) {
            classes.getJSONObject(0).put("hitDiceSpent", hitDice.toIntOrNull()?.coerceAtLeast(0) ?: 0)
        }
        document.put("inspiration", inspiration)
        val list = conditions.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        document.put("conditions", JSONArray(list))
        store.writeCharacter(doc)
        bump()
    }

    fun addItem(name: String) {
        if (name.isBlank()) return
        val doc = store.character()
        val items = doc.getJSONObject("document").optJSONArray("items") ?: JSONArray()
        items.put(JSONObject().put("name", name.trim().take(160)).put("quantity", 1).put("equipped", false).put("attuned", false))
        doc.getJSONObject("document").put("items", items)
        store.writeCharacter(doc)
        bump()
    }

    fun saveCoins(cp: String, sp: String, ep: String, gp: String, pp: String) {
        val doc = store.character()
        val currency = JSONObject()
        for ((key, raw) in listOf("cp" to cp, "sp" to sp, "ep" to ep, "gp" to gp, "pp" to pp)) {
            currency.put(key, raw.toIntOrNull()?.coerceAtLeast(0) ?: 0)
        }
        doc.getJSONObject("document").put("currency", currency)
        store.writeCharacter(doc)
        bump()
    }

    fun saveNotes(appearance: String, personality: String, ideals: String, bonds: String, flaws: String, backstory: String, notes: String) {
        val doc = store.character()
        val details = doc.getJSONObject("document").optJSONObject("details") ?: JSONObject()
        fun put(key: String, value: String, max: Int) {
            if (value.isBlank()) details.remove(key) else details.put(key, value.take(max))
        }
        put("appearance", appearance, 4000)
        put("personalityTraits", personality, 2000)
        put("ideals", ideals, 2000)
        put("bonds", bonds, 2000)
        put("flaws", flaws, 2000)
        put("backstory", backstory, 20000)
        put("notes", notes, 20000)
        doc.getJSONObject("document").put("details", details)
        store.writeCharacter(doc)
        bump()
    }

    fun saveFeats(csv: String) {
        val doc = store.character()
        val ids = csv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        doc.getJSONObject("document").put("featIds", JSONArray(ids))
        store.writeCharacter(doc)
        bump()
    }

    fun addSpell(id: String) {
        if (id.isBlank()) return
        val doc = store.character()
        val spells = doc.getJSONObject("document").optJSONArray("spells") ?: JSONArray()
        spells.put(JSONObject().put("spellId", id.trim()).put("prepared", true).put("alwaysPrepared", false))
        doc.getJSONObject("document").put("spells", spells)
        store.writeCharacter(doc)
        bump()
    }

    fun adjustHp(delta: Int) {
        val doc = store.character()
        val document = doc.getJSONObject("document")
        val hp = document.optJSONObject("hp") ?: JSONObject()
        val current = if (hp.has("current")) hp.optInt("current") else 0
        hp.put("current", (current + delta).coerceAtLeast(0))
        document.put("hp", hp)
        store.writeCharacter(doc)
        bump()
    }

    fun toggleInspiration() {
        val doc = store.character()
        val document = doc.getJSONObject("document")
        document.put("inspiration", !document.optBoolean("inspiration", false))
        store.writeCharacter(doc)
        bump()
    }

    fun togglePin(key: String) {
        val doc = store.character()
        val document = doc.getJSONObject("document")
        val choices = document.optJSONObject("choices") ?: JSONObject()
        val array = choices.optJSONArray("umbra:pins") ?: JSONArray()
        val values = (0 until array.length()).map { array.optString(it) }.filter { it.isNotBlank() }.toMutableList()
        if (key in values) values.remove(key) else values.add(key)
        choices.put("umbra:pins", JSONArray(values))
        document.put("choices", choices)
        store.writeCharacter(doc)
        bump()
    }

    private fun bump() {
        _state.update { it.copy(character = store.character(), book = store.book(), sheetTick = it.sheetTick + 1, banner = "Saved on this device") }
    }

    private fun messages(lines: List<ChatLine>): JSONArray {
        val array = JSONArray()
        array.put(JSONObject().put("role", "system").put("content", SceneJson.SYSTEM.trim()))
        for (line in lines) {
            if (line.role == "user" || line.role == "assistant") {
                array.put(JSONObject().put("role", line.role).put("content", line.apiContent))
            }
        }
        return array
    }
}

@Composable
fun UmbraRoot(model: UmbraViewModel = viewModel()) {
    val state by model.state.collectAsState()
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Ink,
            surface = Card,
            primary = Gold,
            onPrimary = Color(0xFF0B0D12),
            onBackground = Color(0xFFE8EAF0),
            onSurface = Color(0xFFE8EAF0),
        ),
    ) {
        if (!state.connected) {
            if (state.accountScreen == "register") RegisterScreen(state, model) else LoginScreen(state, model)
            return@MaterialTheme
        }
        Scaffold(containerColor = Ink) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp, vertical = 12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    listOf("chat" to "Chat", "sheet" to "Sheet", "compendium" to "Compendium").forEach { (id, label) ->
                        TextButton(onClick = { model.selectTab(id) }) {
                            Text(if (state.tab == id) label else label, color = if (state.tab == id) Gold else Muted)
                        }
                    }
                }
                if (state.banner.isNotBlank()) Text(state.banner, color = Gold)
                when (state.tab) {
                    "sheet" -> BeyondSheet(state, model, Modifier.weight(1f))
                    "compendium" -> CompendiumScreen(state, model, Modifier.weight(1f))
                    else -> ChatScreen(state, model, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun LoginScreen(state: UmbraUiState, model: UmbraViewModel) {
    Column(
        Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Umbra", style = MaterialTheme.typography.displaySmall, color = Gold)
        Text("The table is listening.", color = Muted)
        OutlinedTextField(
            state.baseUrl,
            model::setBaseUrl,
            label = { Text("Base URL") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            state.username,
            model::setUsername,
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            state.password,
            model::setPassword,
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
        )
        Button(
            onClick = { model.checkConnection() },
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color(0xFF0B0D12)),
        ) { Text(if (state.busy) "Signing in..." else "Log in") }
        if (state.banner.isNotBlank()) Text(state.banner, color = Color(0xFFE8EAF0))
        Text("POST /api/login. The session stays on this device. Username and password are not saved.", color = Faint, style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = { model.showRegister() }, enabled = !state.busy) { Text("Create an account") }
        Text("Image generation and Google API: not connected yet.", color = Faint, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun RegisterScreen(state: UmbraUiState, model: UmbraViewModel) {
    Column(
        Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Umbra", style = MaterialTheme.typography.displaySmall, color = Gold)
        Text("First account only.", color = Muted)
        OutlinedTextField(
            state.baseUrl,
            model::setBaseUrl,
            label = { Text("Base URL") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            state.username,
            model::setUsername,
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            state.password,
            model::setPassword,
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
        )
        Button(
            onClick = { model.registerAccount() },
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color(0xFF0B0D12)),
        ) { Text(if (state.busy) "Creating account..." else "Create account") }
        if (state.banner.isNotBlank()) Text(state.banner, color = Color(0xFFE8EAF0))
        Text("POST /api/register. The session stays on this device. Username and password are not saved.", color = Faint, style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = { model.showLogin() }, enabled = !state.busy) { Text("Back to sign in") }
    }
}

@Composable
private fun ChatScreen(state: UmbraUiState, model: UmbraViewModel, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).padding(0.dp)) {
            Text(state.locationName, color = Color(0xFFE8EAF0), style = MaterialTheme.typography.titleMedium)
            Box(
                Modifier.fillMaxWidth().height(96.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, Color(0xFF2A2F3D), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) { Text("Scene", color = Faint) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Audiences.forEach { (id, label) ->
                FilterChip(selected = state.audience == id, onClick = { model.setAudience(id) }, label = { Text(label) })
            }
        }
        val speaking = when (state.audience) {
            "umbra" -> "Speaking to Umbra"
            "group" -> "Speaking to Group"
            "area" -> "Speaking to Area"
            "whisper" -> state.whisperTo?.let { "Whispering to ${it.name}" } ?: "Choose someone to whisper to"
            else -> "Speaking"
        }
        Text(speaking, color = Gold, style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            if (state.present.isEmpty()) {
                Text("No one in the scene yet.", color = Faint)
            }
            state.present.forEach { person ->
                val selected = state.whisperTo?.id == person.id && state.audience == "whisper"
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { model.selectWhisper(person) },
                ) {
                    Portrait(if (selected) Gold else Color(0xFF2A2F3D))
                    Text(person.name, color = Color(0xFFE8EAF0), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.lines) { line ->
                if (line.role == "user") {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
                        Text(line.text, color = Color(0xFFE8EAF0), modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(Player).padding(10.dp))
                    }
                } else {
                    Column {
                        if (line.speaker.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Portrait(Color(0xFF2A2F3D), 28)
                                Text(line.speaker, color = Gold, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                        Text(line.text, color = Color(0xFFE8EAF0))
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(state.draft, model::setDraft, label = { Text("Message the table") }, modifier = Modifier.weight(1f))
            Button(onClick = { model.sendChat() }, enabled = !state.busy, colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color(0xFF0B0D12))) {
                Text(if (state.busy) "…" else "Send")
            }
        }
        TextButton(onClick = { model.disconnect() }) { Text("Sign out") }
    }
}

@Composable
private fun Portrait(border: Color, size: Int = 44) {
    Box(
        Modifier.size(size.dp).clip(CircleShape).border(1.dp, border, CircleShape),
        contentAlignment = Alignment.Center,
    ) { Text("Img", color = Faint, style = MaterialTheme.typography.labelSmall) }
}

@Composable
private fun SheetScreen(state: UmbraUiState, model: UmbraViewModel, modifier: Modifier) {
    val doc = state.character ?: return
    val document = doc.getJSONObject("document")
    val details = document.optJSONObject("details") ?: JSONObject()
    val abilities = document.optJSONObject("abilities") ?: JSONObject()
    val classes = document.optJSONArray("classes")
    val first = if (classes != null && classes.length() > 0) classes.getJSONObject(0) else JSONObject()
    val hp = document.optJSONObject("hp") ?: JSONObject()
    val currency = document.optJSONObject("currency") ?: JSONObject()
    val tick = state.sheetTick
    var name by remember(tick) { mutableStateOf(doc.optString("name")) }
    var species by remember(tick) { mutableStateOf(document.optString("speciesId")) }
    var subspecies by remember(tick) { mutableStateOf(document.optString("subspeciesId")) }
    var background by remember(tick) { mutableStateOf(document.optString("backgroundId")) }
    var classId by remember(tick) { mutableStateOf(first.optString("classId")) }
    var subclass by remember(tick) { mutableStateOf(first.optString("subclassId")) }
    var level by remember(tick) { mutableStateOf(first.optInt("level", 1).toString()) }
    var alignment by remember(tick) { mutableStateOf(details.optString("alignment")) }
    val abilityState = remember(tick) {
        listOf("str", "dex", "con", "int", "wis", "cha").associateWith { mutableStateOf(abilities.optInt(it, 10).toString()) }
    }
    var hpCurrent by remember(tick) { mutableStateOf(if (hp.has("current")) hp.optInt("current").toString() else "") }
    var hpTemp by remember(tick) { mutableStateOf(hp.optInt("temp", 0).toString()) }
    var hitDice by remember(tick) { mutableStateOf(first.optInt("hitDiceSpent", 0).toString()) }
    var inspiration by remember(tick) { mutableStateOf(document.optBoolean("inspiration", false)) }
    var conditions by remember(tick) { mutableStateOf(jsonStrings(document.optJSONArray("conditions")).joinToString(", ")) }
    var itemName by remember(tick) { mutableStateOf("") }
    var cp by remember(tick) { mutableStateOf(currency.optInt("cp", 0).toString()) }
    var sp by remember(tick) { mutableStateOf(currency.optInt("sp", 0).toString()) }
    var ep by remember(tick) { mutableStateOf(currency.optInt("ep", 0).toString()) }
    var gp by remember(tick) { mutableStateOf(currency.optInt("gp", 0).toString()) }
    var pp by remember(tick) { mutableStateOf(currency.optInt("pp", 0).toString()) }
    var appearance by remember(tick) { mutableStateOf(details.optString("appearance")) }
    var personality by remember(tick) { mutableStateOf(details.optString("personalityTraits")) }
    var ideals by remember(tick) { mutableStateOf(details.optString("ideals")) }
    var bonds by remember(tick) { mutableStateOf(details.optString("bonds")) }
    var flaws by remember(tick) { mutableStateOf(details.optString("flaws")) }
    var backstory by remember(tick) { mutableStateOf(details.optString("backstory")) }
    var notes by remember(tick) { mutableStateOf(details.optString("notes")) }
    var feats by remember(tick) { mutableStateOf(jsonStrings(document.optJSONArray("featIds")).joinToString(", ")) }
    var spellId by remember(tick) { mutableStateOf("") }
    val proficient = jsonStrings(document.optJSONArray("skillProficiencies")).toSet()
    val expert = jsonStrings(document.optJSONArray("skillExpertise")).toSet()

    Column(modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Character", style = MaterialTheme.typography.headlineMedium, color = Gold)
        Text("Scroll for the full sheet", color = Faint, style = MaterialTheme.typography.bodySmall)
        Section("Identity") {
            SheetField("Name", name) { name = it }
            SheetField("Species", species) { species = it }
            SheetField("Subspecies", subspecies) { subspecies = it }
            SheetField("Class", classId) { classId = it }
            SheetField("Level", level) { level = it.filter { ch -> ch.isDigit() }.take(2) }
            SheetField("Subclass", subclass) { subclass = it }
            SheetField("Background", background) { background = it }
            SheetField("Alignment", alignment) { alignment = it }
            GoldButton("Save identity") { model.saveIdentity(name, species, subspecies, background, classId, subclass, level, alignment) }
        }
        Section("Abilities") {
            listOf("str", "dex", "con", "int", "wis", "cha").forEach { key ->
                var score by abilityState.getValue(key)
                SheetField(key.uppercase(), score) { score = it.filter { ch -> ch.isDigit() }.take(2) }
            }
            GoldButton("Save abilities") { model.saveAbilities(abilityState.mapValues { it.value.value }) }
        }
        Section("Skills") {
            Skills.forEach { (key, label) ->
                val mark = when {
                    key in expert -> "expertise"
                    key in proficient -> "proficient"
                    else -> "none"
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("$label · $mark", color = Color(0xFFE8EAF0))
                    Row {
                        TextButton(onClick = { model.toggleSkill(key, false) }) { Text("Prof") }
                        TextButton(onClick = { model.toggleSkill(key, true) }) { Text("Exp") }
                    }
                }
            }
        }
        Section("Combat") {
            SheetField("Hit points", hpCurrent) { hpCurrent = it }
            SheetField("Temporary HP", hpTemp) { hpTemp = it }
            SheetField("Hit dice spent", hitDice) { hitDice = it }
            SheetField("Conditions", conditions) { conditions = it }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (inspiration) "Inspiration: yes" else "Inspiration: no", color = Color(0xFFE8EAF0))
                TextButton(onClick = { inspiration = !inspiration }) { Text("Toggle") }
            }
            Text("Armor class and speed are computed by the VTT engine and are not stored on the document.", color = Faint, style = MaterialTheme.typography.bodySmall)
            GoldButton("Save combat") { model.saveCombat(hpCurrent, hpTemp, hitDice, inspiration, conditions) }
        }
        Section("Inventory") {
            val itemsJson = document.optJSONArray("items") ?: JSONArray()
            for (i in 0 until itemsJson.length()) {
                val item = itemsJson.getJSONObject(i)
                Text("${item.optInt("quantity", 1)} × ${item.optString("name")}", color = Color(0xFFE8EAF0))
            }
            SheetField("Add item", itemName) { itemName = it }
            GoldButton("Add to inventory") { model.addItem(itemName) }
            SheetField("cp", cp) { cp = it }
            SheetField("sp", sp) { sp = it }
            SheetField("ep", ep) { ep = it }
            SheetField("gp", gp) { gp = it }
            SheetField("pp", pp) { pp = it }
            GoldButton("Save coins") { model.saveCoins(cp, sp, ep, gp, pp) }
        }
        Section("Features") {
            SheetField("Feat ids", feats) { feats = it }
            GoldButton("Save feats") { model.saveFeats(feats) }
            Text("Class features stay on the class in the compendium. The sheet stores the class, subclass, and feat ids.", color = Faint, style = MaterialTheme.typography.bodySmall)
        }
        Section("Spells") {
            val spells = document.optJSONArray("spells") ?: JSONArray()
            for (i in 0 until spells.length()) {
                val spell = spells.getJSONObject(i)
                val prepared = if (spell.optBoolean("prepared")) "prepared" else "known"
                Text("${spell.optString("spellId")} · $prepared", color = Color(0xFFE8EAF0))
            }
            SheetField("Add spell id", spellId) { spellId = it }
            GoldButton("Add spell") { model.addSpell(spellId) }
        }
        Section("Notes") {
            SheetField("Appearance", appearance) { appearance = it }
            SheetField("Personality", personality) { personality = it }
            SheetField("Ideals", ideals) { ideals = it }
            SheetField("Bonds", bonds) { bonds = it }
            SheetField("Flaws", flaws) { flaws = it }
            SheetField("Backstory", backstory) { backstory = it }
            SheetField("Notes", notes) { notes = it }
            GoldButton("Save notes") {
                model.saveNotes(appearance, personality, ideals, bonds, flaws, backstory, notes)
            }
        }
    }
}

@Composable
private fun CompendiumScreen(state: UmbraUiState, model: UmbraViewModel, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Compendium", style = MaterialTheme.typography.headlineMedium, color = Gold)
        Text("Browse only", color = Faint)
        if (state.compendiumError.isNotBlank()) Text(state.compendiumError, color = Color(0xFFE8EAF0))
        OutlinedTextField(state.compendiumQuery, model::setQuery, label = { Text("Search") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            state.compendiumBooks.forEach { book ->
                val ref = book.id.ifBlank { book.key }
                val label = book.name.ifBlank { book.key.ifBlank { book.id } }
                FilterChip(selected = state.compendiumBookKey == ref, onClick = { model.selectCompendiumBook(ref) }, label = { Text(label) })
            }
        }
        if (state.compendiumBookKey.isNotBlank()) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                FilterChip(selected = state.compendiumCategory.isEmpty(), onClick = { model.setCategory("") }, label = { Text("all") })
                state.compendiumKinds.forEach { kind ->
                    FilterChip(selected = state.compendiumCategory == kind.kind, onClick = { model.setCategory(kind.kind) }, label = { Text(kind.kind) })
                }
            }
        }
        LazyColumn(Modifier.weight(1f)) {
            items(state.compendiumHits, key = { it.id.ifBlank { it.name } + it.kind }) { hit ->
                Column(
                    Modifier.fillMaxWidth().clickable { model.selectEntry(hit.id) }.padding(vertical = 6.dp),
                ) {
                    Text(hit.name, color = Color(0xFFE8EAF0))
                    Text(hit.id, color = Faint, style = MaterialTheme.typography.bodySmall)
                    if (state.compendiumId == hit.id && hit.id.isNotBlank()) {
                        Text(state.compendiumDetail.ifBlank { "No description stored." }, color = Muted)
                        Text("Read only. No edit, create, or delete.", color = Faint, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            if (state.compendiumHasMore) {
                item {
                    TextButton(onClick = { model.loadMoreCompendium() }) { Text("More") }
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Card).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(title, color = Gold, style = MaterialTheme.typography.titleMedium)
        content()
    }
}

@Composable
private fun SheetField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(value, onChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun GoldButton(label: String, onClick: () -> Unit) {
    Button(onClick = onClick, colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color(0xFF0B0D12))) {
        Text(label)
    }
}

private fun jsonStrings(array: JSONArray?): List<String> {
    if (array == null) return emptyList()
    return (0 until array.length()).mapNotNull { index -> array.optString(index).takeIf { it.isNotBlank() } }
}
