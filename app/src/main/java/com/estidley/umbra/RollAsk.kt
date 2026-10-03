package com.estidley.umbra

import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.floor
import kotlin.random.Random

data class RollAsk(val name: String, val purpose: String, val skillKey: String?) {
    fun bonus(character: JSONObject?): Int {
        val document = character?.optJSONObject("document") ?: JSONObject()
        val abilities = document.optJSONObject("abilities") ?: JSONObject()
        fun mod(key: String): Int = floor((abilities.optInt(key, 10) - 10) / 2.0).toInt()
        val classes = document.optJSONArray("classes")
        val level = if (classes != null && classes.length() > 0) {
            classes.getJSONObject(0).optInt("level", 1)
        } else {
            1
        }
        val prof = 2 + ((level.coerceIn(1, 20) - 1) / 4)
        val key = skillKey ?: return mod("dex")
        val ability = skillAbilities[key] ?: "dex"
        val proficient = stringSet(document.optJSONArray("skillProficiencies"))
        val expert = stringSet(document.optJSONArray("skillExpertise"))
        val extra = when {
            key in expert -> prof * 2
            key in proficient -> prof
            else -> 0
        }
        return mod(ability) + extra
    }
}

fun rollCheck(bonus: Int, mode: String): String {
    fun die() = Random.nextInt(1, 21)
    val rolled = when (mode) {
        "advantage", "disadvantage" -> listOf(die(), die())
        else -> listOf(die())
    }
    val picked = when (mode) {
        "advantage" -> rolled.max()
        "disadvantage" -> rolled.min()
        else -> rolled.first()
    }
    val total = picked + bonus
    val sign = if (bonus >= 0) "+$bonus" else bonus.toString()
    val faces = if (rolled.size == 1) rolled.first().toString() else rolled.joinToString("/") + " -> $picked"
    return "$faces $sign = $total"
}

object RollAsks {
    private data class Skill(val key: String, val name: String, val alias: String)

    private val skills = listOf(
        Skill("animalhandling", "Animal Handling", "animal handling"),
        Skill("sleightofhand", "Sleight of Hand", "sleight of hand"),
        Skill("acrobatics", "Acrobatics", "acrobatics"),
        Skill("athletics", "Athletics", "athletics"),
        Skill("deception", "Deception", "deception"),
        Skill("history", "History", "history"),
        Skill("insight", "Insight", "insight"),
        Skill("intimidation", "Intimidation", "intimidation"),
        Skill("investigation", "Investigation", "investigation"),
        Skill("medicine", "Medicine", "medicine"),
        Skill("nature", "Nature", "nature"),
        Skill("perception", "Perception", "perception"),
        Skill("performance", "Performance", "performance"),
        Skill("persuasion", "Persuasion", "persuasion"),
        Skill("religion", "Religion", "religion"),
        Skill("stealth", "Stealth", "stealth"),
        Skill("survival", "Survival", "survival"),
        Skill("arcana", "Arcana", "arcana"),
    )

    private val initiative = Regex("(?i)\\b(?:roll(?:\\s+(?:for|an|a))?\\s+initiative|initiative\\s+(?:roll|check))\\b")
    private val hide = Regex("(?i)\\b(?:hide\\s+check|(?:make|roll)\\s+(?:an?\\s+|to\\s+|for\\s+)?hide|(?:try|attempt)\\s+to\\s+hide)\\b|^hide\\b")

    fun parse(text: String): RollAsk? {
        val sentences = text.split(Regex("(?<=[.!?])\\s+|\\n+")).map { it.trim() }.filter { it.isNotEmpty() }
        for (sentence in sentences) {
            parseSentence(sentence)?.let { return it }
        }
        return null
    }

    private fun parseSentence(sentence: String): RollAsk? {
        data class Hit(val start: Int, val end: Int, val name: String, val skillKey: String?)
        val hits = mutableListOf<Hit>()
        initiative.find(sentence)?.let { hits += Hit(it.range.first, it.range.last + 1, "Initiative", null) }
        hide.find(sentence)?.let { hits += Hit(it.range.first, it.range.last + 1, "Hide", "stealth") }
        for (skill in skills) {
            skillRegex(skill.alias).find(sentence)?.let {
                hits += Hit(it.range.first, it.range.last + 1, skill.name, skill.key)
            }
        }
        val hit = hits.minByOrNull { it.start } ?: return null
        return RollAsk(hit.name, purposeAfter(sentence, hit.end), hit.skillKey)
    }

    private fun skillRegex(alias: String): Regex {
        val name = Regex.escape(alias)
        return Regex("(?i)\\b(?:$name\\s+check|(?:make|roll)\\s+(?:an?\\s+|for\\s+)?$name(?:\\s+check)?|give\\s+me\\s+(?:an?\\s+)?$name(?:\\s+check)?)\\b")
    }

    private fun purposeAfter(sentence: String, end: Int): String {
        var rest = sentence.substring(end.coerceIn(0, sentence.length)).trim()
        rest = rest.trimStart(',', ':', '-', ' ')
        rest = rest.trimEnd('.', '!', '?', ' ')
        if (rest.length < 3) return ""
        return rest.take(90)
    }
}

private val skillAbilities = mapOf(
    "acrobatics" to "dex",
    "animalhandling" to "wis",
    "arcana" to "int",
    "athletics" to "str",
    "deception" to "cha",
    "history" to "int",
    "insight" to "wis",
    "intimidation" to "cha",
    "investigation" to "int",
    "medicine" to "wis",
    "nature" to "int",
    "perception" to "wis",
    "performance" to "cha",
    "persuasion" to "cha",
    "religion" to "int",
    "sleightofhand" to "dex",
    "stealth" to "dex",
    "survival" to "wis",
)

private fun stringSet(array: JSONArray?): Set<String> {
    if (array == null) return emptySet()
    return (0 until array.length()).mapNotNull { array.optString(it).takeIf { value -> value.isNotBlank() } }.toSet()
}

data class CheckRoll(val dice: List<Int>, val kept: Int, val bonus: Int, val total: Int, val mode: String) {
    fun caption(name: String): String {
        val faces = if (dice.size == 1) dice.first().toString() else dice.joinToString("/") + " -> $kept"
        val sign = if (bonus >= 0) "+$bonus" else bonus.toString()
        return "$name: $faces $sign = $total"
    }
}

fun rollD20Check(bonus: Int, mode: String): CheckRoll {
    fun die() = Random.nextInt(1, 21)
    val safe = when (mode) {
        "advantage", "disadvantage" -> mode
        else -> "normal"
    }
    val rolled = when (safe) {
        "advantage", "disadvantage" -> listOf(die(), die())
        else -> listOf(die())
    }
    val kept = when (safe) {
        "advantage" -> rolled.max()
        "disadvantage" -> rolled.min()
        else -> rolled.first()
    }
    return CheckRoll(rolled, kept, bonus, kept + bonus, safe)
}

fun shownDiceForCheck(roll: CheckRoll): List<ShownDie> {
    val duplicates = roll.dice.count { it == roll.kept } > 1
    return roll.dice.mapIndexed { index, face ->
        val primary = if (duplicates) face == roll.kept else index == roll.dice.indexOf(roll.kept)
        ShownDie(sides = 20, value = face, primary = primary)
    }
}

data class PoolRoll(val dice: List<ShownDie>, val total: Int, val text: String)

fun rollPool(counts: Map<Int, Int>): PoolRoll {
    if (counts.values.sum() == 0) return PoolRoll(emptyList(), 0, "No dice selected")
    val dice = mutableListOf<ShownDie>()
    var total = 0
    val parts = mutableListOf<String>()
    for ((sides, count) in counts.toSortedMap()) {
        if (count <= 0 || sides < 2) continue
        val rolls = List(count) { Random.nextInt(1, sides + 1) }
        total += rolls.sum()
        parts += "${count}d$sides " + rolls.joinToString("+")
        for (value in rolls) dice += shownForSides(sides, value)
    }
    if (parts.isEmpty()) return PoolRoll(emptyList(), 0, "No dice selected")
    return PoolRoll(dice, total, parts.joinToString("  ") + "   =   $total")
}

fun shownForSides(sides: Int, value: Int): List<ShownDie> {
    if (sides == 100) {
        val span = (value - 1).coerceIn(0, 99)
        val tens = (span / 10) * 10
        val ones = span % 10
        return listOf(
            ShownDie(10, tens, if (tens == 0) "00" else tens.toString(), "tens"),
            ShownDie(10, ones, ones.toString(), "units"),
        )
    }
    val known = setOf(4, 6, 8, 10, 12, 20)
    val faceSides = if (sides in known) sides else 20
    val face = if (value in 1..faceSides) value else ((value - 1).mod(faceSides) + 1)
    return listOf(ShownDie(faceSides, face, value.toString()))
}

fun rollExpression(roll: CheckRoll): String {
    val mod = if (roll.bonus >= 0) "+" + roll.bonus else roll.bonus.toString()
    return "1d20" + mod + "=" + roll.total
}

fun rollHistoryLine(character: String, check: String, expression: String): String {
    return character + " rolled " + check + ", " + expression
}

