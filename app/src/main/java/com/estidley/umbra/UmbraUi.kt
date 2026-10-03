package com.estidley.umbra

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class ChatLine(val role: String, val text: String)

data class UmbraUiState(
    val displayName: String = "",
    val character: JSONObject? = null,
    val book: JSONObject? = null,
    val lines: List<ChatLine> = emptyList(),
    val draft: String = "",
    val busy: Boolean = false,
    val banner: String = "",
    val pending: Proposal? = null,
    val baseUrl: String = "",
    val apiKey: String = "",
    val health: String = "",
    val tab: String = "sheet",
    val bookFilter: String = "species",
    val itemName: String = "",
)

private const val GM_SYSTEM = """
You are the GM for a 5e table in Umbra. Talk in plain prose.
When you want to change the character or the book, append one fenced json block and do not apply it yourself:
```json
{"proposals":[{"kind":"sheet","summary":"Pick up a rope","target":"inventory","data":{"name":"Rope","quantity":1}}]}
```
kind is sheet or book. target is inventory, species, subclass, monster, encounter, or sheet.
Sheet species data uses speciesId. Sheet subclass data uses subclassId. Book entries need name and, for subclass, classId.
"""

class UmbraViewModel(app: Application) : AndroidViewModel(app) {
    private val store = LocalStore(app)
    private val hermes = HermesClient()
    private val queue = ArrayDeque<Proposal>()

    private val _state = MutableStateFlow(
        UmbraUiState(
            displayName = store.displayName(),
            baseUrl = store.baseUrl(),
            apiKey = store.apiKey(),
        ),
    )
    val state: StateFlow<UmbraUiState> = _state

    init {
        if (store.displayName().isNotBlank()) refreshLocal()
    }

    fun login(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            _state.update { it.copy(banner = "Enter a display name") }
            return
        }
        store.setDisplayName(trimmed)
        if (!getApplication<Application>().filesDir.resolve("character.json").exists()) {
            store.writeCharacter(LocalStore.emptyCharacter(trimmed))
        }
        refreshLocal()
        _state.update { it.copy(displayName = trimmed, banner = "") }
    }

    fun logout() {
        store.clearDisplayName()
        queue.clear()
        _state.update { it.copy(displayName = "", pending = null, lines = emptyList(), tab = "sheet") }
    }

    fun selectTab(tab: String) = _state.update { it.copy(tab = tab) }

    fun setDraft(value: String) = _state.update { it.copy(draft = value) }

    fun setBaseUrl(value: String) = _state.update { it.copy(baseUrl = value) }

    fun setApiKey(value: String) = _state.update { it.copy(apiKey = value) }

    fun setBookFilter(value: String) = _state.update { it.copy(bookFilter = value) }

    fun setItemName(value: String) = _state.update { it.copy(itemName = value) }

    fun saveConnection() {
        val current = _state.value
        store.saveConnection(current.baseUrl, current.apiKey)
        _state.update { it.copy(baseUrl = store.baseUrl(), banner = "Saved on this device") }
    }

    fun testHealth() {
        saveConnection()
        viewModelScope.launch {
            _state.update { it.copy(busy = true, health = "Checking…") }
            val result = withContext(Dispatchers.IO) {
                try {
                    hermes.health(store.baseUrl()).take(400)
                } catch (e: Exception) {
                    e.message ?: "Health check failed"
                }
            }
            _state.update { it.copy(busy = false, health = result) }
        }
    }

    fun updateAbility(key: String, raw: String) {
        val score = raw.toIntOrNull() ?: return
        val doc = store.character()
        doc.getJSONObject("document").getJSONObject("abilities").put(key, score.coerceIn(1, 30))
        store.writeCharacter(doc)
        refreshLocal()
    }

    fun updateIdentity(name: String, speciesId: String, classId: String, subclassId: String) {
        val doc = store.character()
        doc.put("name", name.take(120))
        val document = doc.getJSONObject("document")
        if (speciesId.isNotBlank()) document.put("speciesId", speciesId)
        val classes = document.optJSONArray("classes") ?: JSONArray()
        val row = if (classes.length() == 0) JSONObject().put("level", 1) else classes.getJSONObject(0)
        if (classId.isNotBlank()) row.put("classId", classId)
        if (subclassId.isBlank()) row.remove("subclassId") else row.put("subclassId", subclassId)
        if (classes.length() == 0) classes.put(row)
        document.put("classes", classes)
        store.writeCharacter(doc)
        refreshLocal()
    }

    fun addInventoryFromField() {
        val name = _state.value.itemName.trim()
        if (name.isEmpty()) return
        val doc = store.character()
        val items = doc.getJSONObject("document").optJSONArray("items") ?: JSONArray()
        items.put(JSONObject().put("name", name).put("quantity", 1).put("equipped", false).put("attuned", false))
        doc.getJSONObject("document").put("items", items)
        store.writeCharacter(doc)
        _state.update { it.copy(itemName = "") }
        refreshLocal()
    }

    fun sendChat() {
        val text = _state.value.draft.trim()
        if (text.isEmpty() || _state.value.busy) return
        val history = _state.value.lines + ChatLine("user", text)
        _state.update { it.copy(lines = history, draft = "", busy = true, banner = "") }
        viewModelScope.launch {
            val reply = withContext(Dispatchers.IO) {
                try {
                    hermes.chat(store.baseUrl(), store.apiKey(), messagesPayload(history))
                } catch (e: Exception) {
                    "ERROR:" + (e.message ?: "Chat failed")
                }
            }
            if (reply.startsWith("ERROR:")) {
                _state.update {
                    it.copy(
                        busy = false,
                        lines = history + ChatLine("error", reply.removePrefix("ERROR:")),
                    )
                }
                return@launch
            }
            val proposals = LocalStore.proposalsFrom(reply)
            queue.clear()
            queue.addAll(proposals)
            _state.update {
                it.copy(
                    busy = false,
                    lines = history + ChatLine("assistant", reply),
                    pending = queue.removeFirstOrNull(),
                )
            }
        }
    }

    fun resolveProposal(accept: Boolean) {
        val proposal = _state.value.pending ?: return
        val note = if (!accept) {
            "Discarded: ${proposal.summary}"
        } else {
            try {
                store.applyProposal(proposal)
            } catch (e: Exception) {
                "Could not apply: ${e.message}"
            }
        }
        refreshLocal()
        _state.update {
            it.copy(
                banner = note,
                pending = queue.removeFirstOrNull(),
            )
        }
    }

    private fun messagesPayload(lines: List<ChatLine>): JSONArray {
        val array = JSONArray()
        array.put(JSONObject().put("role", "system").put("content", GM_SYSTEM.trim()))
        for (line in lines) {
            if (line.role == "user" || line.role == "assistant") {
                array.put(JSONObject().put("role", line.role).put("content", line.text))
            }
        }
        return array
    }

    private fun refreshLocal() {
        _state.update { it.copy(character = store.character(), book = store.book()) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UmbraRoot(model: UmbraViewModel = viewModel()) {
    val state by model.state.collectAsState()
    MaterialTheme {
        if (state.displayName.isBlank()) {
            LoginScreen(state.banner) { model.login(it) }
            return@MaterialTheme
        }
        val pending = state.pending
        if (pending != null) {
            AlertDialog(
                onDismissRequest = { model.resolveProposal(false) },
                title = { Text(if (pending.kind == "book") "Write to the book?" else "Write to the sheet?") },
                text = { Text(pending.summary + "\n\n" + pending.target) },
                confirmButton = { TextButton(onClick = { model.resolveProposal(true) }) { Text("Yes") } },
                dismissButton = { TextButton(onClick = { model.resolveProposal(false) }) { Text("No") } },
            )
        }
        Scaffold(
            topBar = { TopAppBar(title = { Text("Umbra") }) },
            bottomBar = {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    listOf("sheet" to "Sheet", "gm" to "GM", "book" to "Book", "settings" to "Settings").forEach { (id, label) ->
                        TextButton(onClick = { model.selectTab(id) }) {
                            Text(if (state.tab == id) "[$label]" else label)
                        }
                    }
                }
            },
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                if (state.banner.isNotBlank()) {
                    Text(state.banner, color = MaterialTheme.colorScheme.primary)
                }
                when (state.tab) {
                    "sheet" -> SheetScreen(state, model)
                    "gm" -> GmScreen(state, model)
                    "book" -> BookScreen(state, model)
                    else -> SettingsScreen(state, model)
                }
            }
        }
    }
}

@Composable
private fun LoginScreen(banner: String, onLogin: (String) -> Unit) {
    var name by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    Scaffold { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Umbra", style = MaterialTheme.typography.headlineMedium)
            Text("Local display name only. Nothing is sent to a login server.")
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Display name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            if (banner.isNotBlank()) Text(banner)
            Button(onClick = { onLogin(name) }) { Text("Enter") }
        }
    }
}

@Composable
private fun SheetScreen(state: UmbraUiState, model: UmbraViewModel) {
    val doc = state.character ?: return
    val document = doc.getJSONObject("document")
    val abilities = document.getJSONObject("abilities")
    val classes = document.getJSONArray("classes").getJSONObject(0)
    var name by androidx.compose.runtime.remember(doc.toString()) { androidx.compose.runtime.mutableStateOf(doc.optString("name")) }
    var species by androidx.compose.runtime.remember(doc.toString()) {
        androidx.compose.runtime.mutableStateOf(document.optString("speciesId"))
    }
    var classId by androidx.compose.runtime.remember(doc.toString()) {
        androidx.compose.runtime.mutableStateOf(classes.optString("classId"))
    }
    var subclass by androidx.compose.runtime.remember(doc.toString()) {
        androidx.compose.runtime.mutableStateOf(classes.optString("subclassId"))
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text("Character", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(name, { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(species, { species = it }, label = { Text("Species id") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(classId, { classId = it }, label = { Text("Class id") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(subclass, { subclass = it }, label = { Text("Subclass id") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = { model.updateIdentity(name, species, classId, subclass) }) { Text("Save identity") }
        }
        items(listOf("str", "dex", "con", "int", "wis", "cha")) { key ->
            AbilityRow(key, abilities.optInt(key, 10), model)
        }
        item {
            Text("Inventory", style = MaterialTheme.typography.titleMedium)
            val itemsJson = document.optJSONArray("items") ?: JSONArray()
            for (i in 0 until itemsJson.length()) {
                val item = itemsJson.getJSONObject(i)
                Text("${item.optInt("quantity", 1)} × ${item.optString("name")}")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    state.itemName,
                    model::setItemName,
                    label = { Text("Add item") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                Button(onClick = { model.addInventoryFromField() }) { Text("Add") }
            }
        }
    }
}

@Composable
private fun AbilityRow(key: String, score: Int, model: UmbraViewModel) {
    var text by androidx.compose.runtime.remember(key, score) { androidx.compose.runtime.mutableStateOf(score.toString()) }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it.filter { ch -> ch.isDigit() }.take(2)
            model.updateAbility(key, text)
        },
        label = { Text(key.uppercase()) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )
}

@Composable
private fun GmScreen(state: UmbraUiState, model: UmbraViewModel) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 8.dp)) {
            items(state.lines) { line ->
                val label = when (line.role) {
                    "user" -> "You"
                    "assistant" -> "GM"
                    else -> "Error"
                }
                Text("$label: ${line.text}", modifier = Modifier.padding(vertical = 4.dp))
            }
        }
        OutlinedTextField(
            value = state.draft,
            onValueChange = model::setDraft,
            label = { Text("Message") },
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = { model.sendChat() }, enabled = !state.busy) {
            Text(if (state.busy) "Sending…" else "Send")
        }
    }
}

@Composable
private fun BookScreen(state: UmbraUiState, model: UmbraViewModel) {
    val book = state.book ?: return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("species", "classes", "subclasses", "monsters", "encounters").forEach { key ->
                FilterChip(
                    selected = state.bookFilter == key,
                    onClick = { model.setBookFilter(key) },
                    label = { Text(key) },
                )
            }
        }
        val array = book.optJSONArray(state.bookFilter) ?: JSONArray()
        LazyColumn {
            items(array.length()) { index ->
                val obj = array.getJSONObject(index)
                Text(obj.optString("name") + "  " + obj.optString("id"), modifier = Modifier.padding(vertical = 4.dp))
            }
        }
    }
}

@Composable
private fun SettingsScreen(state: UmbraUiState, model: UmbraViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            state.baseUrl,
            model::setBaseUrl,
            label = { Text("Hermes base URL") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            state.apiKey,
            model::setApiKey,
            label = { Text("API key") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { model.saveConnection() }) { Text("Save") }
            Button(onClick = { model.testHealth() }, enabled = !state.busy) { Text("Test /health") }
        }
        if (state.health.isNotBlank()) Text(state.health)
        Text("Image generation and Google API: not connected yet.")
        TextButton(onClick = { model.logout() }) { Text("Log out") }
    }
}