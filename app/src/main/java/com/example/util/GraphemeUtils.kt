package com.example.util

import java.text.BreakIterator
import java.util.Locale

/**
 * Grapheme-cluster-safe text utilities backed by java.text.BreakIterator.
 * Ensures Bengali conjuncts (e.g., ক্ষ) and combining marks are never split.
 */
object GraphemeUtils {
    private const val BENGALI_VIRAMA = '\u09CD'
    private const val ZWJ = '\u200D'
    private const val ZWNJ = '\u200C'

    fun splitGraphemes(text: String, locale: Locale = Locale.ROOT): List<String> {
        if (text.isEmpty()) return emptyList()
        val iterator = BreakIterator.getCharacterInstance(locale)
        iterator.setText(text)
        val rawClusters = ArrayList<String>()
        var start = iterator.first()
        var end = iterator.next()
        while (end != BreakIterator.DONE) {
            rawClusters.add(text.substring(start, end))
            start = end
            end = iterator.next()
        }
        if (rawClusters.size <= 1) return rawClusters

        // Merge Indic virama-linked conjunct clusters (e.g. ক + ্ + ষ -> ক্ষ) when
        // BreakIterator breaks after virama, while respecting ZWNJ (U+200C) as an explicit breaker.
        val merged = ArrayList<String>(rawClusters.size)
        var i = 0
        while (i < rawClusters.size) {
            val sb = StringBuilder(rawClusters[i])
            while (i + 1 < rawClusters.size && shouldJoinConjunct(sb.toString(), rawClusters[i + 1])) {
                i++
                sb.append(rawClusters[i])
            }
            merged.add(sb.toString())
            i++
        }
        return merged
    }

    private fun shouldJoinConjunct(current: String, next: String): Boolean {
        if (next.isEmpty() || next.first() == ZWNJ) return false
        val endsWithVirama = current.endsWith(BENGALI_VIRAMA)
        val endsWithViramaZwj = current.length >= 2 &&
            current[current.length - 1] == ZWJ &&
            current[current.length - 2] == BENGALI_VIRAMA
        if (!endsWithVirama && !endsWithViramaZwj) return false
        val firstNext = next.first()
        return firstNext in '\u0980'..'\u09FF' || firstNext == ZWJ
    }

    fun graphemeCount(text: String, locale: Locale = Locale.ROOT): Int {
        return splitGraphemes(text, locale).size
    }

    fun firstGrapheme(text: String, locale: Locale = Locale.ROOT): String {
        return splitGraphemes(text, locale).firstOrNull().orEmpty()
    }

    fun truncateGraphemes(
        text: String,
        maxGraphemes: Int,
        ellipsis: String = "…",
        locale: Locale = Locale.ROOT,
    ): String {
        if (maxGraphemes <= 0 || text.isEmpty()) return ""
        val clusters = splitGraphemes(text, locale)
        if (clusters.size <= maxGraphemes) return text
        return clusters.take(maxGraphemes).joinToString("") + ellipsis
    }

    /**
     * Builds a progressive hint revealing [revealedNonSpaceCount] non-whitespace grapheme clusters
     * while masking remaining non-whitespace grapheme clusters with '_'.
     */
    fun progressiveHint(
        text: String,
        revealedNonSpaceCount: Int,
        locale: Locale = Locale.ROOT,
    ): String {
        val clusters = splitGraphemes(text, locale)
        if (clusters.isEmpty()) return ""
        var remainingToReveal = revealedNonSpaceCount.coerceAtLeast(0)
        return buildString {
            for (cluster in clusters) {
                val isSpace = cluster.all { it.isWhitespace() || it == '\u00A0' || it == '\u202F' }
                when {
                    isSpace -> append(cluster)
                    remainingToReveal > 0 -> {
                        append(cluster)
                        remainingToReveal--
                    }
                    else -> append('_')
                }
            }
        }
    }
}
