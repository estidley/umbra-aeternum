package com.estidley.umbra

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.ui.text.TextStyle
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val Navy = Color(0xFF0B0D12)
private val Panel = Color(0xFF171B24)
private val Line = Color(0xFF2A2F3D)
private val InkText = Color(0xFFE8EAF0)
private val Ash = Color(0xFF7C8399)
private val Blue = Color(0xFFF0944A)
private val Red = Color(0xFFB8651F)
private val DiceRed = Color(0xFFD97A2B)

private enum class SheetPage {
    Header, Saves, Dice, Skills, Actions, Reactions, Inventory, Spells, Features, Training, Background, Notes, Creatures,
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
    val traits: List<TraitBlock>,
    val background: String,
    val alignment: String,
    val speciesSize: String,
    val personality: String,
    val ideals: String,
    val bonds: String,
    val flaws: String,
    val appearance: String,
    val backstory: String,
    val otherNotes: String,
    val armor: List<String>,
    val weapons: List<String>,
    val tools: List<String>,
    val languages: List<String>,
    val otherProf: List<String>,
    val slotsSpent: Map<String, Int>,
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
    var page by remember { mutableStateOf(SheetPage.Header) }
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
                    if (page == SheetPage.Header && !menu) model.selectTab("chat") else {
                        menu = false
                        page = SheetPage.Header
                    }
                },
            )
            if (menu) {
                SectionMenu(Modifier.weight(1f), onPick = {
                    page = it
                    menu = false
                })
            } else {
                when (page) {
                    SheetPage.Header -> HeaderPage(facts, model, { menu = true }, Modifier.weight(1f))
                    SheetPage.Saves -> SavesPage(facts, model, { menu = true }, Modifier.weight(1f))
                    SheetPage.Dice -> DicePage(counts, result, { sides ->
                        val next = counts.toMutableMap()
                        next[sides] = (next[sides] ?: 0) + 1
                        counts = next
                    }, { counts = mapOf(20 to 1); result = null }, { counts = emptyMap(); result = null }, {
                        val active = if (counts.values.sum() == 0) mapOf(20 to 1) else counts
                        val pool = rollPool(active)
                        result = pool.text
                        model.showPool(pool)
                    }, { menu = true }, Modifier.weight(1f))
                    SheetPage.Skills -> SkillsPage(facts, model, { menu = true }, Modifier.weight(1f))
                    SheetPage.Actions -> ActionsPage(facts, model, creatureHp, creatureMax, { creatureHp = it }, { creatureMax = it }, { menu = true }, Modifier.weight(1f))
                    SheetPage.Reactions -> ReactionsPage({ menu = true }, Modifier.weight(1f))
                    SheetPage.Inventory -> InventoryPage(facts, inventoryMine, { inventoryMine = it }, { menu = true }, Modifier.weight(1f))
                    SheetPage.Spells -> SpellsPage(facts, spellQuery, { spellQuery = it }, { menu = true }, Modifier.weight(1f))
                    SheetPage.Features -> FeaturesPage(facts, { menu = true }, Modifier.weight(1f))
                    SheetPage.Training -> TrainingPage(facts, { menu = true }, Modifier.weight(1f))
                    SheetPage.Background -> BackgroundPage(facts, { menu = true }, Modifier.weight(1f))
                    SheetPage.Notes -> NotesPage(facts, { menu = true }, Modifier.weight(1f))
                    SheetPage.Creatures -> CompanionsPage(creatureHp, creatureMax, { creatureHp = it }, { creatureMax = it }, { menu = true }, Modifier.weight(1f))
                }
            }
        }
        if (!menu) {
            EmberDie(onClick = { page = SheetPage.Dice; menu = false }, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp))
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
                onRoll = {
                    val active = if (counts.values.sum() == 0) mapOf(20 to 1) else counts
                    val pool = rollPool(active)
                    result = pool.text
                    model.showPool(pool)
                },
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
private fun HeaderPage(facts: SheetFacts, model: UmbraViewModel, onMenu: () -> Unit, modifier: Modifier) {
    Column(modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionBar("Header", onMenu)
        StatRow(facts, model)
        Text(facts.subtitle, color = Ash, fontSize = 12.sp)
        if (facts.inspiration) Text("Inspiration", color = InkText)
        if (facts.conditions.isNotEmpty()) Text(facts.conditions.joinToString(", "), color = InkText, fontSize = 12.sp)
        AbilityGrid(facts.scores) { label, bonus ->
            model.rollNamedCheck(label, bonus)
        }
    }
}

@Composable
private fun SavesPage(facts: SheetFacts, model: UmbraViewModel, onMenu: () -> Unit, modifier: Modifier) {
    Column(modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionBar("Saves", onMenu)
        Text("Saving Throws", color = InkText, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        AbilityOrder.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                pair.forEach { (key, label) ->
                    val bonus = saveBonus(facts, key)
                    val on = key in facts.saveProf
                    Row(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Panel)
                            .border(1.dp, Line, RoundedCornerShape(20.dp))
                            .clickable { model.rollNamedCheck("$label save", bonus) }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
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
    }
}

@Composable
private fun DicePage(
    counts: Map<Int, Int>,
    result: String?,
    onCount: (Int) -> Unit,
    onReset: () -> Unit,
    onClear: () -> Unit,
    onRoll: () -> Unit,
    onMenu: () -> Unit,
    modifier: Modifier,
) {
    Column(modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionBar("Dice", onMenu)
        DiceControls(counts, result, onCount, onReset, onClear, onRoll)
    }
}

@Composable
private fun StatRow(facts: SheetFacts, model: UmbraViewModel) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        StatBlock("ARMOR\nCLASS", facts.ac.toString())
        StatBlock("INITIATIVE", signed(facts.initiative))
        Box(
            Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF2A2F3D)).border(1.dp, Line, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) { Text("Img", color = Ash, fontSize = 11.sp) }
        Column(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(Panel).padding(8.dp)) {
            Text("HIT POINTS", color = Ash, fontSize = 9.sp)
            Text("${facts.hpCurrent}/${facts.hpMax}", color = InkText, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
            Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF2A2F3D))) {
                val ratio = if (facts.hpMax <= 0) 0f else (facts.hpCurrent.toFloat() / facts.hpMax).coerceIn(0f, 1f)
                Box(Modifier.fillMaxWidth(ratio).height(4.dp).background(Color(0xFFD97A2B)))
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
private fun AbilityGrid(scores: Map<String, Int>, onRoll: (String, Int) -> Unit) {
    AbilityOrder.chunked(3).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            row.forEach { (key, label) ->
                val score = scores[key] ?: 10
                val bonus = abilityMod(score)
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(Color(0xFF1B1F29)).border(1.dp, Line, RoundedCornerShape(18.dp)).clickable { onRoll(label, bonus) }.padding(vertical = 8.dp),
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
                Text(
                    signed(bonus),
                    color = InkText,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Panel)
                        .border(1.dp, Line, RoundedCornerShape(8.dp))
                        .clickable { model.rollNamedCheck(label, bonus) }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun ActionsPage(
    facts: SheetFacts,
    model: UmbraViewModel,
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
            Text(
                signed(hit),
                color = InkText,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Panel).clickable { model.rollNamedCheck("Attack", hit) }.padding(horizontal = 8.dp, vertical = 4.dp),
            )
            Text(
                "$damage bludgeoning",
                color = InkText,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Panel).clickable { model.showDamage("Damage $damage bludgeoning", damage) }.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
        Text("Actions in Combat", color = InkText, fontWeight = FontWeight.SemiBold)
        Text(CombatActions.joinToString(", "), color = Color(0xFFA3A9BA), fontSize = 12.sp)
        Text("BONUS ACTIONS", color = Blue, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        Text("No bonus actions are stored.", color = Ash, fontSize = 12.sp)
        Text("OTHER", color = Blue, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        Text("Interact with an Object", color = Color(0xFFA3A9BA), fontSize = 13.sp)
        Text("Limited uses", color = InkText, fontWeight = FontWeight.SemiBold)
        Text("No limited-use features are stored. Boxes appear when a feature has uses.", color = Ash, fontSize = 12.sp)
        CreatureBlock(creatureHp, creatureMax, onHp, onMax, showEmpty = true)
    }
}

@Composable
private fun CreaturePage(hp: Int, max: Int, onHp: (Int) -> Unit, onMax: (Int) -> Unit, onMenu: () -> Unit, modifier: Modifier) {
    Column(modifier.verticalScroll(rememberScrollState())) {
        SectionBar("Companions", onMenu)
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
        HpButton("Apply", Color(0xFF1B1F29)) { }
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
    var chip by remember { mutableStateOf("all") }
    val mod = abilityMod(facts.scores[facts.spellAbility] ?: 10)
    val attack = mod + facts.prof
    val dc = 8 + facts.prof + mod
    val spells = (0 until facts.spells.length()).mapNotNull { facts.spells.optJSONObject(it) }
    val shown = spells.filter { spell ->
        val id = spell.optString("spellId")
        val source = spell.optString("source")
        val matchesQuery = query.isBlank() || id.contains(query, ignoreCase = true) || source.contains(query, ignoreCase = true)
        val level = if (spell.has("level")) spell.optInt("level") else null
        val matchesChip = when (chip) {
            "0" -> level == 0
            "1" -> level == 1
            else -> true
        }
        matchesQuery && matchesChip
    }
    Column(modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Spells",
                color = InkText,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(Panel).padding(horizontal = 12.dp, vertical = 12.dp),
            )
            Text(
                "::::",
                color = DiceRed,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Panel).padding(horizontal = 10.dp, vertical = 12.dp),
            )
            Text(
                "\u2630",
                color = InkText,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Panel).clickable { onMenu() }.padding(horizontal = 12.dp, vertical = 12.dp),
            )
        }
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xFF12151C)).border(1.dp, Line, RoundedCornerShape(10.dp)).padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = query,
                onValueChange = onQuery,
                singleLine = true,
                textStyle = TextStyle(color = InkText, fontSize = 14.sp),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    if (query.isEmpty()) Text("Search in Spells", color = Ash, fontSize = 14.sp)
                    inner()
                },
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("all" to "All", "0" to "-  0  -", "1" to "1st").forEach { (id, label) ->
                val selected = chip == id
                Text(
                    label,
                    color = if (selected) Color(0xFF0B0D12) else InkText,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) Color(0xFFD97A2B) else Color(0xFF2A2F3D))
                        .clickable { chip = id }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
        Row(Modifier.fillMaxWidth()) {
            SpellStat(signed(mod), "MODIFIER", Modifier.weight(1f))
            SpellStat(signed(attack), "SPELL ATTACK", Modifier.weight(1f))
            SpellStat(dc.toString(), "SAVE DC", Modifier.weight(1f))
        }
        if (shown.isEmpty()) {
            Text(
                when {
                    query.isNotBlank() -> "No spells match."
                    chip == "all" -> "No spells are stored."
                    else -> "No spells at this level are stored."
                },
                color = Ash,
                fontSize = 13.sp,
            )
        } else {
            shown.groupBy { if (it.has("level")) it.optInt("level") else -1 }.toSortedMap().forEach { (level, rows) ->
                SpellLevelHeader(
                    title = spellLevelTitle(level),
                    spent = if (level == 1) facts.slotsSpent["1"] else null,
                    showSlots = level == 1,
                )
                SpellColumns()
                rows.forEach { SpellEntry(it) }
            }
        }
        val showedFirst = shown.any { it.has("level") && it.optInt("level") == 1 }
        if ((chip == "all" || chip == "1") && !showedFirst) {
            SpellLevelHeader("1st Level", spent = facts.slotsSpent["1"], showSlots = true)
        }
    }
}

@Composable
private fun SpellStat(value: String, label: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = InkText, fontWeight = FontWeight.SemiBold, fontSize = 24.sp)
        Text(label, color = Ash, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SpellColumns() {
    Row(Modifier.fillMaxWidth()) {
        Text("TIME", color = Ash, fontSize = 10.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(0.22f))
        Text("RANGE", color = Ash, fontSize = 10.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(0.22f))
        Text("HIT/DC", color = Ash, fontSize = 10.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(0.28f))
        Text("EFFECT", color = Ash, fontSize = 10.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(0.28f))
    }
}

@Composable
private fun SpellEntry(spell: JSONObject) {
    val source = spell.optString("source")
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(spellLabel(spell.optString("spellId")), color = InkText, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            if (source.isNotBlank()) {
                Text(source.uppercase(), color = Ash, fontSize = 10.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 8.dp))
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 8.dp)) {
            Text("\u2014", color = InkText, fontSize = 13.sp, modifier = Modifier.weight(0.22f))
            Text("\u2014", color = InkText, fontSize = 13.sp, modifier = Modifier.weight(0.22f))
            Text("\u2014", color = InkText, fontSize = 13.sp, modifier = Modifier.weight(0.28f))
            Text("\u2014", color = InkText, fontSize = 13.sp, modifier = Modifier.weight(0.28f))
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
    }
}

@Composable
private fun SpellLevelHeader(title: String, spent: Int?, showSlots: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = InkText, fontWeight = FontWeight.SemiBold, fontSize = 22.sp)
        if (showSlots) {
            Text(
                if (spent == null) "SLOTS  Not stored" else "SLOTS  Spent $spent",
                color = Ash,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

private fun spellLevelTitle(level: Int): String = when (level) {
    -1 -> "Spells"
    0 -> "Cantrip"
    1 -> "1st Level"
    2 -> "2nd Level"
    3 -> "3rd Level"
    else -> "${level}th Level"
}

private fun spellLabel(id: String): String {
    val tail = id.substringAfterLast('.').substringAfterLast(':').ifBlank { id }
    return tail.replace('-', ' ').replace('_', ' ').replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}


@Composable
private fun EmberDie(onClick: () -> Unit, modifier: Modifier) {
    Box(modifier.size(56.dp).clickable { onClick() }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Color(0xFFB8651F))
            val face = Path()
            val cx = size.width / 2f
            val cy = size.height / 2f
            val radius = size.minDimension * 0.34f
            for (i in 0 until 6) {
                val angle = PI / 3.0 * i - PI / 2.0
                val x = cx + (radius * cos(angle)).toFloat()
                val y = cy + (radius * sin(angle) * 0.92).toFloat()
                if (i == 0) face.moveTo(x, y) else face.lineTo(x, y)
            }
            face.close()
            drawPath(face, Color(0xFFD97A2B))
            val highlight = Path()
            highlight.moveTo(cx, cy - radius * 0.62f)
            highlight.lineTo(cx - radius * 0.48f, cy + radius * 0.12f)
            highlight.lineTo(cx + radius * 0.08f, cy + radius * 0.02f)
            highlight.close()
            drawPath(highlight, Color(0xFFF6B47E))
        }
        Text("20", color = Color(0xFF0B0D12), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
    }
}

@Composable
private fun FeaturesPage(facts: SheetFacts, onMenu: () -> Unit, modifier: Modifier) {
    Column(modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionBar("Features & Traits", onMenu)
        if (facts.traits.isEmpty()) Text("No traits are stored on this character.", color = Ash)
        facts.traits.forEach { block ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(block.title, color = InkText, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                if (block.source.isNotBlank()) {
                    Text("  \u2022  " + block.source, color = Ash, fontSize = 12.sp)
                }
            }
            if (block.body.isNotBlank()) Text(block.body, color = InkText, fontSize = 14.sp)
            block.choices.forEach { choice ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.padding(end = 8.dp).width(2.dp).height(16.dp).background(Ash))
                    Text(choice, color = InkText)
                }
            }
            if (block.useBoxes > 0) {
                Text("Used Charges:", color = InkText, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(block.useBoxes) {
                        Box(Modifier.size(22.dp, 18.dp).border(1.5.dp, Ash, RoundedCornerShape(4.dp)))
                    }
                }
            }
            if (block.recharge.isNotBlank()) Text(block.recharge, color = Ash, fontSize = 12.sp)
        }
        Text("FEATS", color = Color(0xFFF0944A), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        if (facts.feats.isEmpty()) {
            Text("There are no feats for this character.", color = Ash)
        } else {
            facts.feats.forEach { Text(it, color = InkText) }
        }
    }
}

@Composable
private fun TrainingPage(facts: SheetFacts, onMenu: () -> Unit, modifier: Modifier) {
    Column(modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionBar("Proficiencies & Training", onMenu)
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("+" + facts.prof, color = InkText, fontWeight = FontWeight.SemiBold, fontSize = 28.sp)
            Text("Proficiency Bonus", color = InkText, fontWeight = FontWeight.SemiBold)
        }
        ProficiencyGroup("Armor", facts.armor)
        ProficiencyGroup("Weapons", facts.weapons)
        ProficiencyGroup("Tools", facts.tools)
        ProficiencyGroup("Languages", facts.languages)
    }
}

@Composable
private fun ProficiencyGroup(title: String, lines: List<String>) {
    Text(title, color = InkText, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
    if (lines.isEmpty()) Text("None stored.", color = Ash)
    lines.forEach { Text(it, color = InkText) }
    Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
}

@Composable
private fun BackgroundPage(facts: SheetFacts, onMenu: () -> Unit, modifier: Modifier) {
    Column(modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SectionBar("Background", onMenu)
        Text("BACKGROUND", color = Color(0xFFF0944A), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        Text(facts.background.ifBlank { "No background is stored." }, color = InkText, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        Text("Feature text is not stored on this character.", color = Ash)
        Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
        Text("CHARACTERISTICS", color = Color(0xFFF0944A), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        Characteristic("Alignment", facts.alignment)
        Characteristic("Gender", "")
        Characteristic("Eyes", "")
        Characteristic("Size", facts.speciesSize.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() })
        Characteristic("Height", "")
        Characteristic("Faith", "")
        Characteristic("Hair", "")
        Characteristic("Skin", "")
        Characteristic("Age", "")
        Characteristic("Weight", "")
        TraitLine("Personality Traits", facts.personality, "No Personality Traits")
        TraitLine("Ideals", facts.ideals, "No Ideals")
        TraitLine("Bonds", facts.bonds, "No Bonds")
        TraitLine("Flaws", facts.flaws, "No Flaws")
        Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
        Text("APPEARANCE", color = Color(0xFFF0944A), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        Text(
            facts.appearance.ifBlank { "You do not have any appearance traits now." },
            color = if (facts.appearance.isBlank()) Ash else InkText,
        )
    }
}

@Composable
private fun Characteristic(label: String, value: String) {
    Text(label + ": " + value.ifBlank { "--" }, color = InkText, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun TraitLine(label: String, value: String, empty: String) {
    Text(label, color = InkText, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    Text(value.ifBlank { empty }, color = if (value.isBlank()) Ash else InkText)
}

@Composable
private fun NotesPage(facts: SheetFacts, onMenu: () -> Unit, modifier: Modifier) {
    Column(modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionBar("Notes", onMenu)
        NoteBlock("Organizations", "")
        NoteBlock("Allies", "")
        NoteBlock("Enemies", "")
        NoteBlock("Backstory", facts.backstory)
        NoteBlock("Other", facts.otherNotes)
    }
}

@Composable
private fun NoteBlock(title: String, value: String) {
    Text(title, color = InkText, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
    Text(if (value.isBlank()) "+ Add $title" else value, color = if (value.isBlank()) Ash else InkText)
    Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
}

@Composable
private fun CompanionsPage(
    hp: Int,
    max: Int,
    onHp: (Int) -> Unit,
    onMax: (Int) -> Unit,
    onMenu: () -> Unit,
    modifier: Modifier,
) {
    Column(modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionBar("Companions", onMenu)
        CreatureBlock(hp, max, onHp, onMax, showEmpty = true)
    }
}

@Composable
private fun ReactionsPage(onMenu: () -> Unit, modifier: Modifier) {
    Column(modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionBar("Reactions", onMenu)
        Text("Opportunity Attack", color = InkText, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Text("A creature you can see leaves your reach.", color = Color(0xFFA3A9BA), fontSize = 13.sp)
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
private fun SectionMenu(modifier: Modifier, onPick: (SheetPage) -> Unit) {
    val rows = listOf(
        "Header" to SheetPage.Header,
        "Saves" to SheetPage.Saves,
        "Dice" to SheetPage.Dice,
        "Skills" to SheetPage.Skills,
        "Actions" to SheetPage.Actions,
        "Reactions" to SheetPage.Reactions,
        "Inventory" to SheetPage.Inventory,
        "Spells" to SheetPage.Spells,
        "Features" to SheetPage.Features,
        "Proficiencies" to SheetPage.Training,
        "Background" to SheetPage.Background,
        "Notes" to SheetPage.Notes,
        "Companions" to SheetPage.Creatures,
    )
    Column(modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Sections", color = Ash, modifier = Modifier.align(Alignment.End))
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
            Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)).background(Color(0xFF12151C)).border(1.dp, Line, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)).clickable { }.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DiceControls(counts, result, onCount, onReset, onClear, onRoll, onClose)
        }
    }
}

@Composable
private fun DiceControls(
    counts: Map<Int, Int>,
    result: String?,
    onCount: (Int) -> Unit,
    onReset: () -> Unit,
    onClear: () -> Unit,
    onRoll: () -> Unit,
    onClose: (() -> Unit)? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Roll Dice", color = InkText, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            onClose?.let { close -> Text("X", color = InkText, modifier = Modifier.clickable { close() }.padding(8.dp)) }
        }
        val selected = counts.entries.filter { it.value > 0 }.joinToString(" ") { "${it.value}d${it.key}" }.ifBlank { "d20" }
        Text(selected, color = InkText, fontWeight = FontWeight.SemiBold)
        Text("Change Dice", color = Blue, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            listOf(20, 12, 100, 10, 8, 6, 4).forEach { sides ->
                val on = (counts[sides] ?: 0) > 0
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(if (on) Color(0xFF2A2F3D) else Panel).clickable { onCount(sides) }.padding(8.dp),
                ) {
                    Text("d$sides", color = if (sides == 20) DiceRed else InkText, fontWeight = FontWeight.SemiBold)
                    Text("${counts[sides] ?: 0}", color = Ash, fontSize = 10.sp)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            RollChip("RESET", Line, Modifier.weight(1f), onReset)
            RollChip("ROLL", Red, Modifier.weight(1f), onRoll)
        }
        Text("CLEAR DICE", color = Color(0xFFF6B47E), fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.End).clickable { onClear() }.padding(8.dp))
        if (result != null) {
            Text(result, color = InkText, modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFF0944A), RoundedCornerShape(12.dp)).background(Color(0xFF171B24)).padding(10.dp))
        }
    }
}

@Composable
private fun RollChip(label: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.height(48.dp).clip(RoundedCornerShape(12.dp)).background(color).clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = InkText, fontWeight = FontWeight.SemiBold)
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

private data class TraitBlock(
    val title: String,
    val source: String,
    val body: String,
    val choices: List<String> = emptyList(),
    val useBoxes: Int = 0,
    val recharge: String = "",
)

private fun collectTraits(
    book: JSONObject?,
    document: JSONObject,
    species: JSONObject?,
    classId: String,
    className: String,
    level: Int,
): List<TraitBlock> {
    val out = mutableListOf<TraitBlock>()
    val speciesSource = sourceLabel(species)
    if (species != null) {
        val size = species.optString("size")
        if (size.isNotBlank()) {
            out += TraitBlock("Size", speciesSource, "You are ${size.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }}.")
        }
        val speed = species.optInt("speed", 0)
        if (speed > 0) out += TraitBlock("Speed", speciesSource, "Your walking speed is $speed ft.")
        sentenceContaining(species.optString("description"), "darkvision")?.let { sentence ->
            out += TraitBlock("Darkvision", speciesSource, sentence)
        }
        val effects = species.optJSONArray("effects")
        if (effects != null) {
            for (i in 0 until effects.length()) {
                val effect = effects.optJSONObject(i) ?: continue
                if (effect.optString("type") == "descriptive") {
                    val text = effect.optString("text")
                    if (text.isNotBlank()) out += TraitBlock("Trait", speciesSource, text)
                }
            }
        }
        val featureIds = species.optJSONArray("featureIds")
        if (featureIds != null) {
            for (i in 0 until featureIds.length()) {
                val feature = findEntity(book, "features", featureIds.optString(i)) ?: continue
                out += traitFromFeature(feature, document)
            }
        }
    }
    val features = book?.optJSONArray("features")
    if (features != null && classId.isNotBlank()) {
        for (i in 0 until features.length()) {
            val feature = features.optJSONObject(i) ?: continue
            if (feature.optString("parentId") != classId) continue
            if (feature.optInt("level", 1) > level) continue
            out += traitFromFeature(feature, document)
        }
    }
    if (className.isBlank()) return out
    return out
}

private fun traitFromFeature(feature: JSONObject, document: JSONObject): TraitBlock {
    val uses = feature.optJSONObject("uses")
    val maxRaw = if (uses != null && uses.has("max")) uses.get("max") else null
    val boxes = if (maxRaw is Number) maxRaw.toInt().coerceIn(0, 8) else 0
    val rechargeKind = uses?.optJSONObject("recharge")?.optString("kind").orEmpty()
    val recharge = when (rechargeKind) {
        "longRest" -> "Resets on Long Rest"
        "shortRest" -> "Resets on Short Rest"
        else -> ""
    }
    return TraitBlock(
        title = feature.optString("name").ifBlank { feature.optString("id") },
        source = sourceLabel(feature),
        body = feature.optString("description"),
        choices = choicesFor(document, feature.optString("id")),
        useBoxes = boxes,
        recharge = recharge,
    )
}

private fun choicesFor(document: JSONObject, featureId: String): List<String> {
    if (featureId.isBlank()) return emptyList()
    val choices = document.optJSONObject("choices") ?: return emptyList()
    val picked = mutableListOf<String>()
    val keys = choices.keys()
    while (keys.hasNext()) {
        val key = keys.next()
        if (!key.contains(featureId)) continue
        when (val value = choices.get(key)) {
            is String -> if (value.isNotBlank()) picked += value
            is JSONArray -> {
                for (i in 0 until value.length()) {
                    val item = value.optString(i)
                    if (item.isNotBlank()) picked += item
                }
            }
        }
    }
    return picked
}

private fun sourceLabel(entity: JSONObject?): String {
    if (entity == null) return ""
    val source = entity.opt("source")
    return when (source) {
        is JSONObject -> source.optString("name")
        is String -> source
        else -> ""
    }
}

private fun sentenceContaining(text: String, word: String): String? {
    if (!text.contains(word, ignoreCase = true)) return null
    return text.split(Regex("(?<=[.!?])\\s+")).firstOrNull { it.contains(word, ignoreCase = true) } ?: text
}

private fun proficiencyLines(classEntity: JSONObject?, key: String, className: String, document: JSONObject): List<String> {
    val fromClass = classEntity?.optJSONObject("proficiencies")?.optJSONArray(key)
    val lines = mutableListOf<String>()
    if (fromClass != null) {
        for (i in 0 until fromClass.length()) {
            val name = titleWords(fromClass.optString(i))
            if (name.isBlank()) continue
            lines += if (className.isBlank()) name else "$name ($className)"
        }
    }
    val bucket = when (key) {
        "armor" -> "armor"
        "weapons" -> "weapon"
        else -> "tool"
    }
    for (extra in jsonList(document.optJSONArray("otherProficiencies"))) {
        val low = extra.lowercase()
        val matches = when (bucket) {
            "armor" -> "armor" in low || "shield" in low
            "weapon" -> "weapon" in low
            else -> "tool" in low || "kit" in low
        }
        if (matches) lines += extra
    }
    return lines.distinct()
}

private fun languageLines(document: JSONObject): List<String> {
    return jsonList(document.optJSONArray("otherProficiencies")).filter { extra ->
        val low = extra.lowercase()
        "armor" !in low && "shield" !in low && "weapon" !in low && "tool" !in low && "kit" !in low
    }
}

private fun titleWords(value: String): String =
    value.split(' ').joinToString(" ") { word -> word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } }

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
        slotsSpent = readSlots(document.optJSONObject("slotsSpent")),
        spellAbility = spellAbility,
        feats = jsonList(document.optJSONArray("featIds")).map { id ->
            findEntity(book, "features", id)?.optString("name").orEmpty().ifBlank { id }
        },
        traits = collectTraits(book, document, speciesEntity, classId, className, level),
        background = document.optString("backgroundId").ifBlank { details.optString("background") },
        alignment = details.optString("alignment"),
        speciesSize = speciesEntity?.optString("size").orEmpty(),
        personality = details.optString("personalityTraits"),
        ideals = details.optString("ideals"),
        bonds = details.optString("bonds"),
        flaws = details.optString("flaws"),
        appearance = details.optString("appearance"),
        backstory = details.optString("backstory"),
        otherNotes = details.optString("notes"),
        armor = proficiencyLines(classEntity, "armor", className, document),
        weapons = proficiencyLines(classEntity, "weapons", className, document),
        tools = proficiencyLines(classEntity, "tools", className, document),
        languages = languageLines(document),
        otherProf = jsonList(document.optJSONArray("otherProficiencies")),
    )
}


private fun readSlots(obj: JSONObject?): Map<String, Int> {
    if (obj == null) return emptyMap()
    val out = mutableMapOf<String, Int>()
    val keys = obj.keys()
    while (keys.hasNext()) {
        val key = keys.next()
        out[key] = obj.optInt(key, 0)
    }
    return out
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
