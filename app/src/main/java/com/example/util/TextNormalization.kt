package com.example.util

import com.example.data.CardEntity
import java.text.Collator
import java.text.Normalizer
import java.util.Locale

object TextNormalization {
    private const val NBSP = '\u00A0'
    private const val NARROW_NBSP = '\u202F'
    private const val RIGHT_SINGLE_QUOTE = '\u2019'
    private const val STRAIGHT_APOSTROPHE = '\''
    private val WHITESPACE_RUN_REGEX = Regex("[ \\t\\n\\r\\u000B\\u000C\\u0085\\u2000-\\u200A\\u2028\\u2029\\u3000]+")

    /**
     * Computes the matching key for a card front or search query:
     * 1. Unicode NFC normalization (preserves accents, ZWJ U+200D, and ZWNJ U+200C).
     * 2. Converts NBSP (U+00A0) and narrow NBSP (U+202F) to normal space.
     * 3. Trims and collapses runs of whitespace to a single ASCII space.
     * 4. Converts typographic apostrophe (’) to straight apostrophe (') for matching only.
     * 5. Lowercases with Locale.ROOT.
     */
    fun computeFrontKey(raw: String): String {
        val nfc = Normalizer.normalize(raw, Normalizer.Form.NFC)
        val normalizedSpaces = buildString(nfc.length) {
            for (ch in nfc) {
                when (ch) {
                    NBSP, NARROW_NBSP -> append(' ')
                    RIGHT_SINGLE_QUOTE -> append(STRAIGHT_APOSTROPHE)
                    else -> append(ch)
                }
            }
        }
        val collapsed = WHITESPACE_RUN_REGEX.replace(normalizedSpaces.trim(), " ")
        return collapsed.lowercase(Locale.ROOT)
    }

    fun computeSearchKey(raw: String): String = computeFrontKey(raw)

    fun sortCardsWithCollator(cards: List<CardEntity>, languageTag: String): List<CardEntity> {
        if (cards.size <= 1) return cards
        val locale = if (languageTag.isBlank() || languageTag == "und") {
            Locale.getDefault()
        } else {
            Locale.forLanguageTag(languageTag)
        }
        val collator = Collator.getInstance(locale).apply {
            strength = Collator.TERTIARY
        }
        return cards.sortedWith { a, b ->
            val cmp = collator.compare(a.front, b.front)
            if (cmp != 0) cmp else a.id.compareTo(b.id)
        }
    }
}
