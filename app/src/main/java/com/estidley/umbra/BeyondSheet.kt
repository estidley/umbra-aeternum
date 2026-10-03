package com.estidley.umbra

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

private val Navy = Color(0xFF151922)
private val Panel = Color(0xFF232833)
private val Line = Color(0xFF3A4254)
private val InkText = Color(0xFFF4F6FA)
private val Ash = Color(0xFF9AA3B5)
private val Blue = Color(0xFF5B9CFF)
private val Red = Color(0xFF8E2A2A)
private val DiceRed = Color(0xFFE23B3B)

private enum class SheetPage {
    Main, Skills, Actions, Inventory, Spells, Speed, Features, Training, Background, Notes, Creatures,
}

private data class SheetFacts(
    val name: String,
    val subtitle: String,
    val scores: Map<String, Int>,
    val level: Int,
    val prof: Int,
    val ac: Int,
    val initiative: Int,
    val hpCurrent: Int,
    val hpMax: Int,
    val inspiration: Boolean,
    val conditions: List<String>,
    val saveProf: Set<String>,
    val skillProf: Set<String>,
    val skillExpert: Set<String>,
    val pins: Set<String>,
    val speed: Int,
    val vision: String,
    val items: JSONArray,
    val currencyLabel: String,
    val spells: JSONArray,
    val spellAbility: String,
    val feats: List<String>,
    val background: String,
    val notes: String,
    val otherProf: List<String>,
)

private val SkillRows = listOf(
    Triple("acrobatics", "Acrobatics", "dex"),
    Triple("animalhandling", "Animal Handling", "wis"),
    Triple("arcana", "Arcana", "int"),
    Triple("athletics", "Athletics", "str"),
    Triple("deception", "Deception", "cha"),
    Triple("history", "History", "int"),
    Triple("insight", "Insight", "wis"),
    Triple("intimidation", "Intimidation", "cha"),
    Triple("investigation", "Investigation", "int"),
    Triple("medicine", "Medicine", "wis"),
    Triple("nature", "Nature", "int"),
    Triple("perception", "Perception", "wis"),
    Triple("performance", "Performance", "cha"),
    Triple("persuasion", "Persuasion", "cha"),
    Triple("religion", "Religion", "int"),
    Triple("sleightofhand", "Sleight of Hand", "dex"),
    Triple("stealth", "Stealth", "dex"),
    Triple("survival", "Survival", "wis"),
)

private val AbilityOrder = listOf(
    "str" to "STRENGTH",
    "dex" to "DEXTERITY",
    "con" to "CONSTITUTION",
    "int" to "INTELLIGENCE",
    "wis" to "WISDOM",
    "cha" to "CHARISMA",
)

private val CombatActions = listOf(
    "Attack", "Dash", "Disengage", "Dodge", "Grapple", "Help", "Hide",
    "Improvise", "Influence", "Magic", "Ready", "Search", "Shove", "Study", "Utilize",
)

@Composable
fun BeyondSheet(state: UmbraUiState, model: UmbraViewModel, modifier: Modifier) {
    val facts = remember(state.sheetTick, state.character, state.book) {
        readFacts(state.character, state.book)
    }
    var page by remember { mutableStateOf(SheetPage.Main) }
    var menu by remember { mutableStateOf(false) }
    var dice by remember { mutableStateOf(false) }
    var counts by remember { mutableStateOf(mapOf(20 to 1)) }
    var result by remember { mutableStateOf<String?>(null) }
    var spellQuery by remember { mutableStateOf("") }
    var inventoryMine by remember { mutableStateOf(true) }
    var creatureHp by remember { mutableIntStateOf(0) }
    var creatureMax by remember { mutableIntStateOf(0) }

    Box(modifier.fillMaxSize().background(Navy)) {
        Column(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 8.dp)) {
            SheetTop(
                name = facts.name,
                subtitle = facts.subtitle,
                onBack = {
                    if (page == SheetPage.Main && !menu) model.selectTab("chat") else {
                        menu = false
                        page = SheetPage.Main
                    }
                },
            )
            if (menu) {
                SectionMenu(onPick = {
                    page = it
                    menu = false
                })
            } else {
                when (page) {
                    SheetPage.Main -> MainPage(facts, model, { menu = true }, { dice = true }, Modifier.weight(1f))
                    SheetPage.Skills -> SkillsPage(facts, model, { menu = true }, Modifier.weight(1f))
                    SheetPage.Actions -> ActionsPage(facts, creatureHp, creatureMax, { creatureHp = it }, { creatureMax = it }, { menu = true }, Modifier.weight(1f))
                    SheetPage.Inventory -> InventoryPage(facts, inventoryMine, { inventoryMine = it }, { menu = true }, Modifier.weight(1f))
                    SheetPage.Spells -> SpellsPage(facts, spellQuery, { spellQuery = it }, { menu = true }, Modifier.weight(1f))
                    SheetPage.Speed -> SimplePage("Speed, Defenses", listOf("Speed ${facts.speed} ft.", "Armor class ${facts.ac}", "No resistances are stored on this character."), { menu = true }, Modifier.weight(1f))
                    SheetPage.Features -> SimplePage("Features & Traits", facts.feats.ifEmpty { listOf("No features are stored on this character.") }, { menu = true }, Modifier.weight(1f))
                    SheetPage.Training -> SimplePage("Proficiencies & Training", (facts.otherProf + facts.skillProf.map { it }).ifEmpty { listOf("No proficiencies are stored.") }, { menu = true }, Modifier.weight(1f))
                    SheetPage.Background -> SimplePage("Background", listOf(facts.background.ifBlank { "No background is stored." }, facts.notes).filter { it.isNotBlank() }, { menu = true }, Modifier.weight(1f))
                    SheetPage.Notes -> SimplePage("Notes", listOf(facts.notes.ifBlank { "No notes are stored." }), { menu = true }, Modifier.weight(1f))
                    SheetPage.Creatures -> CreaturePage(creatureHp, creatureMax, { creatureHp = it }, { creatureMax = it }, { menu = true }, Modifier.weight(1f))
                }
            }
        }
        if (!menu && page == SheetPage.Main) {
            Box(
                Modifier.align(Alignment.BottomEnd).padding(16.dp).size(56.dp).clip(CircleShape).background(DiceRed).clickable { dice = true },
                contentAlignment = Alignment.Center,
            ) { Text("d20", color = InkText, fontWeight = FontWeight.SemiBold) }
        }
        if (dice) {
            DiceSheet(
                counts = counts,
                result = result,
                onCount = { sides ->
                    val next = counts.toMutableMap()
                    next[sides] = (next[sides] ?: 0) + 1
                    counts = next
                },
                onReset = { counts = mapOf(20 to 1) },
                onClear = { counts = emptyMap(); result = null },
                onRoll = { result = rollDice(counts) },
                onClose = { dice = false },
            )
        }
    }
}

@Composable
private fun SheetTop(name: String, subtitle: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onBack() }.padding(bottom = 8.dp)) {
        Text("<", color = InkText, fontSize = 20.sp, modifier = Modifier.padding(end = 8.dp))
        Column {
            Text(name, color = InkText, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Ash, fontSize = 12.sp)
        }
    }
}

@Composable
private fun SectionBar(title: String, onMenu: () -> Unit, onGrid: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Panel).padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = InkText, fontWeight = FontWeight.SemiBold)
        Row {
            if (onGrid != null) Text("grid", color = DiceRed, modifier = Modifier.clickable { onGrid() }.padding(end = 10.dp))
            Text("list", color = DiceRed, modifier = Modifier.clickable { onMenu() })
        }
    }
}

@Composable
private fun MainPage(facts: SheetFacts, model: UmbraViewModel, onMenu: () -> Unit, onDice: () -> Unit, modifier: Modifier) {
    Column(modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        StatRow(facts, model)
        SectionBar("Abilities, Saves, Senses", onMenu)
        AbilityGrid(facts.scores)
        Text("Saving Throws", color = InkText, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        AbilityOrder.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                pair.forEach { (key, label) ->
                    val bonus = saveBonus(facts, key)
                    val on = key in facts.saveProf
                    Row(
                        Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).background(Panel).border(1.dp, Line, RoundedCornerShape(20.dp)).padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text((if (on) "* " else "o ") + label, color = InkText, fontSize = 11.sp)
                        Text(signed(bonus), color = InkText, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        Text("Senses", color = InkText, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        SenseRow(passive(facts, "perception", "wis"), "PASSIVE PERCEPTION")
        SenseRow(passive(facts, "investigation", "int"), "PASSIVE INVESTIGATION")
        SenseRow(passive(facts, "insight", "wis"), "PASSIVE INSIGHT")
        Text(facts.vision, color = InkText, fontSize = 12.sp)
        TextButton(onClick = onDice) { Text("Roll dice", color = Blue) }
    }
}

@Composable
private fun StatRow(facts: SheetFacts, model: UmbraViewModel) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        StatBlock("ARMOR\nCLASS", facts.ac.toString())
        StatBlock("INITIATIVE", signed(facts.initiative))
        Box(
            Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF2C3342)).border(1.dp, Line, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) { Text("Img", color = Ash, fontSize = 11.sp) }
        Column(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(Panel).padding(8.dp)) {
            Text("HIT POINTS", color = Ash, fontSize = 9.sp)
            Text("${facts.hpCurrent}/${facts.hpMax}", color = InkText, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
            Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF2A3142))) {
                val ratio = if (facts.hpMax <= 0) 0f else (facts.hpCurrent.toFloat() / facts.hpMax).coerceIn(0f, 1f)
                Box(Modifier.fillMaxWidth(ratio).height(4.dp).background(Color(0xFF3D7DFF)))
            }
            Row {
                Text("-", color = InkText, modifier = Modifier.clickable { model.adjustHp(-1) }.padding(end = 8.dp))
                Text("+", color = InkText, modifier = Modifier.clickable { model.adjustHp(1) })
            }
        }
    }
    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(Panel).clickable {
            model.toggleInspiration()
        }.padding(8.dp), contentAlignment = Alignment.Center) {
            Text(if (facts.inspiration) "Inspiration on" else "Heroic inspiration", color = InkText, fontSize = 11.sp)
        }
        Box(Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(Panel).padding(8.dp), contentAlignment = Alignment.Center) {
            Text(if (facts.conditions.isEmpty()) "CONDITIONS" else facts.conditions.joinToString(), color = InkText, fontSize = 11.sp)
        }
    }
}

@Composable
private fun StatBlock(label: String, value: String) {
    Column(
        Modifier.clip(RoundedCornerShape(12.dp)).background(Panel).padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, color = Ash, fontSize = 9.sp)
        Text(value, color = InkText, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
    }
}

@Composable
private fun AbilityGrid(scores: Map<String, Int>) {
    AbilityOrder.chunked(3).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            row.forEach { (key, label) ->
                val score = scores[key] ?: 10
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(Color(0xFF1E2430)).border(1.dp, Line, RoundedCornerShape(18.dp)).padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(label, color = Ash, fontSize = 8.sp)
                    Text(signed(abilityMod(score)), color = InkText, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Text(score.toString(), color = InkText, fontSize = 12.sp, modifier = Modifier.border(1.dp, Line, CircleShape).padding(horizontal = 8.dp, vertical = 2.dp))
                }
            }
        }
    }
}

@Composable
private fun SenseRow(value: Int, label: String) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Panel).border(1.dp, Line, RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(value.toString(), color = InkText, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, modifier = Modifier.padding(end = 10.dp))
        Text(label, color = InkText, fontSize = 12.sp)
    }
}

@Composable
private fun SkillsPage(facts: SheetFacts, model: UmbraViewModel, onMenu: () -> Unit, modifier: Modifier) {
    Column(modifier.verticalScroll(rememberScrollState())) {
        SectionBar("Skills", onMenu)
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("PIN   PROF   MOD   SKILL", color = Ash, fontSize = 10.sp)
            Text("BONUS", color = Ash, fontSize = 10.sp)
        }
        SkillRows.forEach { (key, label, ability) ->
            val proficient = key in facts.skillProf || key in facts.skillExpert
            val bonus = skillBonus(facts, key, ability)
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (key in facts.pins) "pin" else "o", color = if (key in facts.pins) Blue else Ash, fontSize = 11.sp, modifier = Modifier.width(28.dp).clickable { model.togglePin(key) })
                Text(if (proficient) "*" else "o", color = InkText, modifier = Modifier.width(24.dp).clickable { model.toggleSkill(key, false) })
                Text(ability.uppercase(), color = Ash, fontSize = 11.sp, modifier = Modifier.width(36.dp))
                Text(label, color = InkText, modifier = Modifier.weight(1f), fontSize = 13.sp)
                Text(signed(bonus), color = InkText, fontWeight = FontWeight.SemiBold, modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Panel).border(1.dp, Line, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp))
            }
        }
    }
}

@Composable
private fun ActionsPage(
    facts: SheetFacts,
    creatureHp: Int,
    creatureMax: Int,
    onHp: (Int) -> Unit,
    onMax: (Int) -> Unit,
    onMenu: () -> Unit,
    modifier: Modifier,
) {
    val str = abilityMod(facts.scores["str"] ?: 10)
    val hit = str + facts.prof
    val damage = 1 + str
    Column(modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SectionBar("Actions", onMenu)
        Text("ACTIONS  ·  Attacks per Action: 1", color = Blue, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        Text("RANGE                         HIT/DC          DAMAGE", color = Ash, fontSize = 10.sp)
        Text("Unarmed Strike", color = InkText, fontWeight = FontWeight.SemiBold)
        Text("MELEE ATTACK    5 FT. REACH", color = Ash, fontSize = 11.sp)
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(signed(hit), color = InkText, modifier = Modifier.background(Panel).padding(horizontal = 8.dp, vertical = 4.dp))
            Text("$damage bludgeoning", color = InkText, modifier = Modifier.background(Panel).padding(horizontal = 8.dp, vertical = 4.dp))
        }
        Text("Actions in Combat", color = InkText, fontWeight = FontWeight.SemiBold)
        Text(CombatActions.joinToString(", "), color = Color(0xFFC5CDD8), fontSize = 12.sp)
        Text("BONUS ACTIONS", color = Blue, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        Text("No bonus actions are stored.", color = Ash, fontSize = 12.sp)
        Text("REACTIONS", color = Blue, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        Text("Opportunity Attack", color = Color(0xFFC5CDD8), fontSize = 13.sp)
        Text("OTHER", color = Blue, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        Text("Interact with an Object", color = Color(0xFFC5CDD8), fontSize = 13.sp)
        Text("Limited uses", color = InkText, fontWeight = FontWeight.SemiBold)
        Text("No limited-use features are stored. Boxes appear when a feature has uses.", color = Ash, fontSize = 12.sp)
        CreatureBlock(creatureHp, creatureMax, onHp, onMax, showEmpty = true)
    }
}

@Composable
private fun CreaturePage(hp: Int, max: Int, onHp: (Int) -> Unit, onMax: (Int) -> Unit, onMenu: () -> Unit, modifier: Modifier) {
    Column(modifier.verticalScroll(rememberScrollState())) {
        SectionBar("Extras: Creatures", onMenu)
        CreatureBlock(hp, max, onHp, onMax, showEmpty = true)
    }
}

@Composable
private fun CreatureBlock(hp: Int, max: Int, onHp: (Int) -> Unit, onMax: (Int) -> Unit, showEmpty: Boolean) {
    if (max <= 0 && hp <= 0 && showEmpty) {
        Text("No creatures are stored.", color = Ash, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
        TextButton(onClick = { onMax(1); onHp(1) }) { Text("Track a creature", color = Blue) }
        return
    }
    Text("Summoned creature", color = InkText, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
    Text("AC ${10}", color = InkText)
    Text("Hit Points", color = InkText, fontSize = 12.sp)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        HpButton("-", Red) { onHp((hp - 1).coerceAtLeast(0)) }
        Text("$hp/$max", color = InkText, modifier = Modifier.background(Panel).padding(horizontal = 10.dp, vertical = 6.dp))
        HpButton("+", Panel) { onHp(hp + 1); if (hp + 1 > max) onMax(hp + 1) }
        HpButton("Apply", Color(0xFF1E3A5F)) { }
    }
    Text("Resets on Special", color = Ash, fontSize = 11.sp)
}

@Composable
private fun HpButton(label: String, color: Color, onClick: () -> Unit) {
    Text(label, color = InkText, modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(color).clickable { onClick() }.padding(horizontal = 8.dp, vertical = 6.dp))
}

@Composable
private fun InventoryPage(facts: SheetFacts, mine: Boolean, onMine: (Boolean) -> Unit, onMenu: () -> Unit, modifier: Modifier) {
    Column(modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionBar("Inventory", onMenu)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("MY INVENTORY", color = if (mine) InkText else Ash, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, modifier = Modifier.clickable { onMine(true) })
            Text("PARTY INVENTORY", color = if (!mine) InkText else Ash, fontSize = 12.sp, modifier = Modifier.clickable { onMine(false) })
        }
        if (!mine) {
            Text("Party inventory is not stored on this character.", color = Ash)
            return@Column
        }
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Panel).padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("WEIGHT CARRIED", color = Ash, fontSize = 10.sp)
                Text("Not stored", color = InkText, fontWeight = FontWeight.SemiBold)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("TOTAL CURRENCY", color = Ash, fontSize = 10.sp)
                Text(facts.currencyLabel, color = InkText, fontWeight = FontWeight.SemiBold)
            }
        }
        val equipped = (0 until facts.items.length()).mapNotNull { facts.items.optJSONObject(it) }.filter { it.optBoolean("equipped") }
        val carried = (0 until facts.items.length()).mapNotNull { facts.items.optJSONObject(it) }
        Text("EQUIPMENT (${equipped.size})", color = InkText, fontWeight = FontWeight.SemiBold)
        if (equipped.isEmpty()) Text("There are no items in this container", color = Ash, fontSize = 12.sp)
        equipped.forEach { Text(it.optString("name"), color = InkText) }
        Text("ATTUNED ITEMS", color = InkText, fontWeight = FontWeight.SemiBold)
        val attuned = carried.filter { it.optBoolean("attuned") }
        repeat(3) { index ->
            val label = attuned.getOrNull(index)?.optString("name")?.ifBlank { null } ?: "Item not attuned"
            Text(label, color = if (label == "Item not attuned") Ash else InkText, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).border(1.dp, Line, RoundedCornerShape(16.dp)).padding(12.dp))
        }
        Text("ITEMS REQUIRING ATTUNEMENT", color = InkText, fontWeight = FontWeight.SemiBold)
        Text("Items that you can attune to will display here as you make them active.", color = Ash, fontSize = 12.sp)
        Text("INFUSIONS", color = InkText, fontWeight = FontWeight.SemiBold)
        Text("No item infused", color = Ash, fontSize = 12.sp)
        Text("OTHER POSSESSIONS", color = InkText, fontWeight = FontWeight.SemiBold)
        val other = carried.filter { !it.optBoolean("equipped") }
        if (other.isEmpty()) Text("No other possessions are stored.", color = Ash, fontSize = 12.sp)
        other.forEach { Text("${it.optInt("quantity", 1)} × ${it.optString("name")}", color = InkText) }
    }
}

@Composable
private fun SpellsPage(facts: SheetFacts, query: String, onQuery: (String) -> Unit, onMenu: () -> Unit, modifier: Modifier) {
    val mod = abilityMod(facts.scores[facts.spellAbility] ?: 10)
    val attack = mod + facts.prof
    val dc = 8 + facts.prof + mod
    val shown = (0 until facts.spells.length()).mapNotNull { facts.spells.optJSONObject(it) }.filter {
        val id = it.optString("spellId")
        query.isBlank() || id.contains(query, ignoreCase = true)
    }
    Column(modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionBar("Spells", onMenu)
        androidx.compose.material3.OutlinedTextField(query, onQuery, label = { Text("Search") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("All", "0", "1", "2", "3").forEach { chip ->
                Text(chip, color = InkText, modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(if (chip == "All") Color(0xFF3D7DFF) else Panel).padding(horizontal = 8.dp, vertical = 4.dp))
            }
        }
        Text("MOD ${facts.spellAbility.uppercase()}    SPELL ATTACK ${signed(attack)}    SAVE DC $dc", color = InkText, fontSize = 11.sp)
        Text("SPELLS", color = Blue, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        Text("TIME    RANGE                 HIT/DC    EFFECT", color = Ash, fontSize = 10.sp)
        if (shown.isEmpty()) Text("No spells are stored.", color = Ash)
        shown.forEach { spell ->
            Text(spell.optString("spellId"), color = InkText, fontWeight = FontWeight.SemiBold)
            val prepared = if (spell.optBoolean("prepared")) "prepared" else "known"
            Text("—    —    ${signed(attack)}    $prepared", color = Color(0xFFC5CDD8), fontSize = 12.sp)
        }
        Text("SPELL SLOTS", color = Blue, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        Text("Slot totals are not stored. Spent slots stay empty until the document has them.", color = Ash, fontSize = 12.sp)
    }
}

@Composable
private fun SimplePage(title: String, lines: List<String>, onMenu: () -> Unit, modifier: Modifier) {
    Column(modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionBar(title, onMenu)
        lines.forEach { Text(it, color = InkText) }
    }
}

@Composable
private fun SectionMenu(onPick: (SheetPage) -> Unit) {
    val rows = listOf(
        "Abilities, Saves, Senses" to SheetPage.Main,
        "Skills" to SheetPage.Skills,
        "Actions" to SheetPage.Actions,
        "Inventory" to SheetPage.Inventory,
        "Spells" to SheetPage.Spells,
        "Speed, Defenses" to SheetPage.Speed,
        "Features & Traits" to SheetPage.Features,
        "Proficiencies & Training" to SheetPage.Training,
        "Background" to SheetPage.Background,
        "Notes" to SheetPage.Notes,
        "Extras: Creatures" to SheetPage.Creatures,
    )
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Reorder", color = Ash, modifier = Modifier.align(Alignment.End))
        rows.forEachIndexed { index, (label, dest) ->
            Text(
                label,
                color = InkText,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Panel).border(if (index == 0) 2.dp else 1.dp, if (index == 0) DiceRed else Line, RoundedCornerShape(10.dp)).clickable { onPick(dest) }.padding(12.dp),
            )
        }
    }
}

@Composable
private fun DiceSheet(
    counts: Map<Int, Int>,
    result: String?,
    onCount: (Int) -> Unit,
    onReset: () -> Unit,
    onClear: () -> Unit,
    onRoll: () -> Unit,
    onClose: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().background(Color(0xCC000000)).clickable { onClose() },
        verticalArrangement = Arrangement.Bottom,
    ) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)).background(Color(0xFF1B2130)).border(1.dp, Line, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)).clickable { }.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Roll Dice", color = InkText, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Text("X", color = InkText, modifier = Modifier.clickable { onClose() })
            }
            val selected = counts.entries.filter { it.value > 0 }.joinToString(" ") { "${it.value}d${it.key}" }.ifBlank { "d20" }
            Text(selected, color = InkText, fontWeight = FontWeight.SemiBold)
            Text("Change Dice", color = Blue, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                listOf(20, 12, 100, 10, 8, 6, 4).forEach { sides ->
                    val on = (counts[sides] ?: 0) > 0
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(if (on) Color(0xFF3A2030) else Panel).clickable { onCount(sides) }.padding(8.dp)) {
                        Text("d$sides", color = if (sides == 20) DiceRed else InkText, fontWeight = FontWeight.SemiBold)
                        Text("${counts[sides] ?: 0}", color = Ash, fontSize = 10.sp)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Text("RESET", color = InkText, modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(Line).clickable { onReset() }.padding(vertical = 10.dp), fontWeight = FontWeight.SemiBold)
                Text("ROLL", color = InkText, modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(Red).clickable { onRoll() }.padding(vertical = 10.dp), fontWeight = FontWeight.SemiBold)
            }
            Text("CLEAR DICE", color = Color(0xFFE0B15A), fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.End).clickable { onClear() })
            if (result != null) {
                Text(result, color = InkText, modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF5B8CFF), RoundedCornerShape(12.dp)).background(Color(0xFF243044)).padding(10.dp))
            }
        }
    }
}

private fun abilityMod(score: Int): Int = (score - 10).floorDiv(2)

private fun profBonus(level: Int): Int = 2 + ((level.coerceIn(1, 20) - 1) / 4)

private fun signed(n: Int): String = if (n >= 0) "+$n" else n.toString()

private fun saveBonus(facts: SheetFacts, key: String): Int {
    val base = abilityMod(facts.scores[key] ?: 10)
    return if (key in facts.saveProf) base + facts.prof else base
}

private fun skillBonus(facts: SheetFacts, key: String, ability: String): Int {
    val base = abilityMod(facts.scores[ability] ?: 10)
    val extra = when {
        key in facts.skillExpert -> facts.prof * 2
        key in facts.skillProf -> facts.prof
        else -> 0
    }
    return base + extra
}

private fun passive(facts: SheetFacts, skill: String, ability: String): Int = 10 + skillBonus(facts, skill, ability)

private fun rollDice(counts: Map<Int, Int>): String {
    if (counts.values.sum() == 0) return "No dice selected"
    var total = 0
    val parts = mutableListOf<String>()
    for ((sides, count) in counts.toSortedMap()) {
        if (count <= 0) continue
        val rolls = List(count) { Random.nextInt(1, sides + 1) }
        total += rolls.sum()
        parts += "${count}d$sides ${rolls.joinToString("+")}"
    }
    return parts.joinToString("  ") + "   =   $total"
}

private fun readFacts(character: JSONObject?, book: JSONObject?): SheetFacts {
    val doc = character ?: JSONObject()
    val document = doc.optJSONObject("document") ?: JSONObject()
    val abilities = document.optJSONObject("abilities") ?: JSONObject()
    val scores = AbilityOrder.associate { (key, _) -> key to abilities.optInt(key, 10) }
    val classes = document.optJSONArray("classes")
    val first = if (classes != null && classes.length() > 0) classes.getJSONObject(0) else JSONObject()
    val level = first.optInt("level", 1).coerceIn(1, 20)
    val classId = first.optString("classId")
    val speciesId = document.optString("speciesId")
    val classEntity = findEntity(book, "classes", classId)
    val speciesEntity = findEntity(book, "species", speciesId)
    val className = classEntity?.optString("name").orEmpty().ifBlank { classId.ifBlank { "Class" } }
    val speciesName = speciesEntity?.optString("name").orEmpty().ifBlank { speciesId.ifBlank { "Species" } }
    val hp = document.optJSONObject("hp") ?: JSONObject()
    val current = if (hp.has("current")) hp.optInt("current") else 0
    val max = when {
        hp.has("rolledMax") -> hp.optInt("rolledMax")
        else -> current.coerceAtLeast(1)
    }
    val saveProf = mutableSetOf<String>()
    classEntity?.optJSONArray("savingThrows")?.let { array ->
        for (i in 0 until array.length()) saveProf += array.optString(i)
    }
    val skillProf = jsonSet(document.optJSONArray("skillProficiencies"))
    val skillExpert = jsonSet(document.optJSONArray("skillExpertise"))
    val pins = jsonSet(document.optJSONObject("choices")?.optJSONArray("umbra:pins"))
    val speed = speciesEntity?.optInt("speed", 30) ?: 30
    val description = speciesEntity?.optString("description").orEmpty()
    val vision = if (description.contains("darkvision", ignoreCase = true)) {
        "Darkvision is mentioned on the species."
    } else {
        "No special vision is stored."
    }
    val currency = document.optJSONObject("currency") ?: JSONObject()
    val coins = listOf("cp", "sp", "ep", "gp", "pp").joinToString("  ") { "$it ${currency.optInt(it, 0)}" }
    val details = document.optJSONObject("details") ?: JSONObject()
    val notes = listOf("appearance", "personalityTraits", "ideals", "bonds", "flaws", "backstory", "notes")
        .mapNotNull { key -> details.optString(key).takeIf { it.isNotBlank() } }
        .joinToString("\n\n")
    val ac = if (document.has("ac")) document.optInt("ac") else 10 + abilityMod(scores["dex"] ?: 10)
    val spellAbility = classEntity?.optJSONObject("spellcasting")?.optString("ability").orEmpty().ifBlank { "int" }
    return SheetFacts(
        name = doc.optString("name").ifBlank { "Adventurer" },
        subtitle = "$speciesName $className $level",
        scores = scores,
        level = level,
        prof = profBonus(level),
        ac = ac,
        initiative = abilityMod(scores["dex"] ?: 10),
        hpCurrent = current,
        hpMax = max.coerceAtLeast(current.coerceAtLeast(1)),
        inspiration = document.optBoolean("inspiration", false),
        conditions = jsonList(document.optJSONArray("conditions")),
        saveProf = saveProf,
        skillProf = skillProf,
        skillExpert = skillExpert,
        pins = pins,
        speed = speed,
        vision = vision,
        items = document.optJSONArray("items") ?: JSONArray(),
        currencyLabel = coins,
        spells = document.optJSONArray("spells") ?: JSONArray(),
        spellAbility = spellAbility,
        feats = jsonList(document.optJSONArray("featIds")),
        background = document.optString("backgroundId").ifBlank { details.optString("background") },
        notes = notes,
        otherProf = jsonList(document.optJSONArray("otherProficiencies")),
    )
}

private fun findEntity(book: JSONObject?, bucket: String, id: String): JSONObject? {
    if (id.isBlank()) return null
    val array = book?.optJSONArray(bucket) ?: return null
    for (i in 0 until array.length()) {
        val obj = array.optJSONObject(i) ?: continue
        if (obj.optString("id") == id) return obj
    }
    return null
}

private fun jsonSet(array: JSONArray?): Set<String> = jsonList(array).toSet()

private fun jsonList(array: JSONArray?): List<String> {
    if (array == null) return emptyList()
    return (0 until array.length()).mapNotNull { array.optString(it).takeIf { value -> value.isNotBlank() } }
}
