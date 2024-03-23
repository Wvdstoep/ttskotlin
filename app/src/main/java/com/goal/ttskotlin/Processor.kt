package com.tensorspeech.tensorflowtts.utils

import android.util.Log
import com.goal.ttskotlin.NumberNorm
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

class Processor {
    init {
        SYMBOLS.add(PAD)
        SYMBOLS.add(SPECIAL)
        PUNCTUATION.filterNot { it.isEmpty() }.forEach { SYMBOLS.add(it) }
        LETTERS.filterNot { it.isEmpty() }.forEach { SYMBOLS.add(it) }
        VALID_SYMBOLS.forEach { SYMBOLS.add("@$it") }
        SYMBOLS.add(EOS)
        SYMBOLS.forEachIndexed { index, s -> SYMBOL_TO_ID[s] = index }

        ABBREVIATIONS.apply {
            put("mrs", "misess")
            put("mr", "mister")
            put("dr", "doctor")
            put("st", "saint")
            put("co", "company")
            put("jr", "junior")
            put("maj", "major")
            put("gen", "general")
            put("drs", "doctors")
            put("rev", "reverend")
            put("lt", "lieutenant")
            put("hon", "honorable")
            put("sgt", "sergeant")
            put("capt", "captain")
            put("esq", "esquire")
            put("ltd", "limited")
            put("col", "colonel")
            put("ft", "fort")
        }
    }

    private fun symbolsToSequence(symbols: String): List<Int> =
        symbols.mapNotNull { SYMBOL_TO_ID[it.toString()] }

    private fun arpabetToSequence(symbols: String?): List<Int> =
        symbols?.split(" ")?.mapNotNull { SYMBOL_TO_ID["@${it}"] } ?: listOf()

    private fun convertToAscii(text: String): String =
        text.toByteArray(StandardCharsets.US_ASCII).toString(StandardCharsets.US_ASCII)

    private fun collapseWhitespace(text: String): String =
        text.replace("\\s+".toRegex(), " ")

    private fun expandAbbreviations(text: String): String =
        ABBREVIATIONS.entries.fold(text) { acc, (key, value) ->
            acc.replace("\\b$key\\.".toRegex(), value)
        }
    private fun removeCommasFromNumbers(text: String): String {
        var text = text
        val m = COMMA_NUMBER_RE.matcher(text)
        while (m.find()) {
            val s = m.group().replace(",".toRegex(), "")
            text = text.replaceFirst(m.group().toRegex(), s)
        }
        return text
    }
    private fun expandPounds(text: String): String {
        var text = text
        val m = POUNDS_RE.matcher(text)
        while (m.find()) {
            text = text.replaceFirst(m.group().toRegex(), m.group() + " pounds")
        }
        return text
    }

    private fun expandDollars(text: String): String {
        var text = text
        val m = DOLLARS_RE.matcher(text)
        while (m.find()) {
            var dollars = "0"
            var cents = "0"
            var spelling = ""
            val s = m.group().substring(1)
            val parts = s.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()
            if (!s.startsWith(".")) {
                dollars = parts[0]
            }
            if (!s.endsWith(".") && parts.size > 1) {
                cents = parts[1]
            }
            if ("0" != dollars) {
                spelling += parts[0] + " dollars "
            }
            if ("0" != cents && "00" != cents) {
                spelling += parts[1] + " cents "
            }
            text = text.replaceFirst(("\\" + m.group()).toRegex(), spelling)
        }
        return text
    }

    private fun expandDecimals(text: String): String {
        var text = text
        val m = DECIMAL_RE.matcher(text)
        while (m.find()) {
            val s = m.group().replace("\\.".toRegex(), " point ")
            text = text.replaceFirst(m.group().toRegex(), s)
        }
        return text
    }
    private fun expandOrdinals(text: String): String {
        var text = text
        val m = ORDINAL_RE.matcher(text)
        while (m.find()) {
            val s = m.group().substring(0, m.group().length - 2)
            val l = java.lang.Long.valueOf(s)
            val spelling = NumberNorm.toOrdinal(l)
            text = text.replaceFirst(m.group().toRegex(), spelling)
        }
        return text
    }

    private fun expandCardinals(text: String): String {
        var text = text
        val m = NUMBER_RE.matcher(text)
        while (m.find()) {
            val l = java.lang.Long.valueOf(m.group())
            val spelling = NumberNorm.numToString(l)
            text = text.replaceFirst(m.group().toRegex(), spelling)
        }
        return text
    }
    private fun expandNumbers(text: String): String =
        listOf(
            ::removeCommasFromNumbers,
            ::expandPounds,
            ::expandDollars,
            ::expandDecimals,
            ::expandOrdinals,
            ::expandCardinals
        ).fold(text) { acc, func -> func(acc) }

    private fun cleanTextForEnglish(text: String): String =
        text.toByteArray(StandardCharsets.US_ASCII).toString(StandardCharsets.US_ASCII)
            .lowercase().let(::expandAbbreviations).let(::expandNumbers)
            .replace("\\s+".toRegex(), " ").also { Log.d(TAG, "text preprocessed: $it") }

    fun textToIds(text: String?): IntArray {
        val sequence = mutableListOf<Int>()
        var remainingText = text

        while (!remainingText.isNullOrEmpty()) {
            val matcher = CURLY_RE.matcher(remainingText)
            if (!matcher.find()) {
                sequence += symbolsToSequence(cleanTextForEnglish(remainingText))
                break
            }
            sequence += symbolsToSequence(cleanTextForEnglish(matcher.group(1)))
            sequence += arpabetToSequence(matcher.group(2))
            remainingText = matcher.group(3)
        }

        return sequence.toIntArray()
    }

    companion object {
        private const val TAG = "processor"
        private val VALID_SYMBOLS = arrayOf(
            "AA",
            "AA0",
            "AA1",
            "AA2",
            "AE",
            "AE0",
            "AE1",
            "AE2",
            "AH",
            "AH0",
            "AH1",
            "AH2",
            "AO",
            "AO0",
            "AO1",
            "AO2",
            "AW",
            "AW0",
            "AW1",
            "AW2",
            "AY",
            "AY0",
            "AY1",
            "AY2",
            "B",
            "CH",
            "D",
            "DH",
            "EH",
            "EH0",
            "EH1",
            "EH2",
            "ER",
            "ER0",
            "ER1",
            "ER2",
            "EY",
            "EY0",
            "EY1",
            "EY2",
            "F",
            "G",
            "HH",
            "IH",
            "IH0",
            "IH1",
            "IH2",
            "IY",
            "IY0",
            "IY1",
            "IY2",
            "JH",
            "K",
            "L",
            "M",
            "N",
            "NG",
            "OW",
            "OW0",
            "OW1",
            "OW2",
            "OY",
            "OY0",
            "OY1",
            "OY2",
            "P",
            "R",
            "S",
            "SH",
            "T",
            "TH",
            "UH",
            "UH0",
            "UH1",
            "UH2",
            "UW",
            "UW0",
            "UW1",
            "UW2",
            "V",
            "W",
            "Y",
            "Z",
            "ZH"
        )
        private val CURLY_RE = Pattern.compile("(.*?)\\{(.+?)\\}(.*)")
        private val COMMA_NUMBER_RE = Pattern.compile("([0-9][0-9\\,]+[0-9])")
        private val DECIMAL_RE = Pattern.compile("([0-9]+\\.[0-9]+)")
        private val POUNDS_RE = Pattern.compile("£([0-9\\,]*[0-9]+)")
        private val DOLLARS_RE = Pattern.compile("\\$([0-9.\\,]*[0-9]+)")
        private val ORDINAL_RE = Pattern.compile("[0-9]+(st|nd|rd|th)")
        private val NUMBER_RE = Pattern.compile("[0-9]+")
        private const val PAD = "_"
        private const val EOS = "~"
        private const val SPECIAL = "-"
        private val PUNCTUATION = "!'(),.:;? ".split("".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()
        private val LETTERS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".split("".toRegex())
                .dropLastWhile { it.isEmpty() }
                .toTypedArray()
        private val SYMBOLS: MutableList<String> = ArrayList()
        private val ABBREVIATIONS: MutableMap<String, String> = HashMap()
        private val SYMBOL_TO_ID: MutableMap<String, Int> = HashMap()
    }
}

