package com.milksummer.tracker.ui.ocr

import com.google.mlkit.vision.text.Text

data class ParsedSlip(
    val quantity: Double? = null,
    val fat: Double? = null,
    val snf: Double? = null,
    val rate: Double? = null,
    val total: Double? = null
) {
    val isEmpty: Boolean get() = listOf(quantity, fat, snf, rate, total).all { it == null }
}

object SlipParser {
    private val labels = linkedMapOf(
        "quantity" to "\\b(?:quantity|qty)\\b|मात्रा",
        "fat" to "\\bfat\\b|फैट|वसा",
        "snf" to "\\bsnf\\b|एस\\.?एन\\.?एफ\\.?",
        "rate" to "\\brate\\b|दर",
        "total" to "\\b(?:total|amount)\\b|कुल|राशि"
    ).mapValues { Regex(it.value, RegexOption.IGNORE_CASE) }
    private val number = Regex("[-+]?(?:\\d{1,3}(?:,\\d{3})+(?:\\.\\d+)?|\\d+(?:[.,]\\d+)?)")

    fun parse(text: Text): ParsedSlip = parse(text.text)

    fun parse(raw: String): ParsedSlip {
        val normalized = raw.map { char ->
            if (char.isDigit()) ('0'.code + Character.digit(char, 10)).toChar() else char
        }.joinToString("")
        val lines = normalized.lines().filter { it.isNotBlank() }
        val values = mutableMapOf<String, Double>()
        lines.forEachIndexed { index, line ->
            val matches = labels.flatMap { (name, regex) -> regex.findAll(line).map { name to it }.toList() }
                .sortedBy { it.second.range.first }
            val next = lines.getOrNull(index + 1).orEmpty()
            // Handle slips with a row of headings followed by a row of numbers.
            if (matches.size > 1 && number.find(line) == null && labels.values.none { it.containsMatchIn(next) }) {
                val numbers = number.findAll(next).toList()
                if (numbers.size == matches.size) matches.zip(numbers).forEach { (label, value) ->
                    parseNumber(value.value)?.let { values.putIfAbsent(label.first, it) }
                }
            }
            matches.forEachIndexed { position, (name, label) ->
                val end = matches.getOrNull(position + 1)?.second?.range?.first ?: line.length
                val adjacent = line.substring(label.range.last + 1, end)
                val value = number.find(adjacent)?.value ?: if (matches.size == 1 && labels.values.none { it.containsMatchIn(next) }) number.find(next)?.value else null
                value?.let(::parseNumber)?.let { values.putIfAbsent(name, it) }
            }
        }
        return ParsedSlip(values["quantity"], values["fat"], values["snf"], values["rate"], values["total"])
    }

    private fun parseNumber(value: String): Double? {
        val normalized = when {
            value.contains('.') -> value.replace(",", "")
            Regex("[-+]?\\d{1,3}(?:,\\d{3})+").matches(value) -> value.replace(",", "")
            else -> value.replace(',', '.')
        }
        return normalized.toDoubleOrNull()?.takeIf { it.isFinite() }
    }
}
