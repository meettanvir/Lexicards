package com.example.util

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction

data class CsvCardRow(
    val front: String,
    val back: String,
)

data class CsvDeckCardRow(
    val deckName: String,
    val languageTag: String,
    val front: String,
    val back: String,
)

sealed class CsvParseResult {
    data class SingleDeck(
        val rows: List<CsvCardRow>,
        val delimiter: Char,
        val skippedInvalidRows: Int,
    ) : CsvParseResult()

    data class MultiDeck(
        val rows: List<CsvDeckCardRow>,
        val delimiter: Char,
        val skippedInvalidRows: Int,
    ) : CsvParseResult()

    data class Error(val message: String) : CsvParseResult()
}

object CsvCodec {
    private val UTF8_BOM_BYTES = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
    private const val UTF8_BOM_CHAR = '\uFEFF'

    fun exportDeckCsvBytes(rows: List<CsvCardRow>): ByteArray {
        val content = buildString {
            append("front,back\n")
            for (row in rows) {
                append(escapeField(row.front))
                append(',')
                append(escapeField(row.back))
                append('\n')
            }
        }
        return withUtf8Bom(content)
    }

    fun exportAllDecksCsvBytes(rows: List<CsvDeckCardRow>): ByteArray {
        val content = buildString {
            append("deck,language,front,back\n")
            for (row in rows) {
                append(escapeField(row.deckName))
                append(',')
                append(escapeField(row.languageTag))
                append(',')
                append(escapeField(row.front))
                append(',')
                append(escapeField(row.back))
                append('\n')
            }
        }
        return withUtf8Bom(content)
    }

    private fun withUtf8Bom(text: String): ByteArray {
        val textBytes = text.toByteArray(Charsets.UTF_8)
        return ByteArrayOutputStream(UTF8_BOM_BYTES.size + textBytes.size).apply {
            write(UTF8_BOM_BYTES)
            write(textBytes)
        }.toByteArray()
    }

    private fun escapeField(value: String): String {
        val needsQuoting = value.any {
            it == ',' || it == '"' || it == '\n' || it == '\r' || it == ';' || it == '\t'
        }
        if (!needsQuoting) return value
        return buildString(value.length + 2) {
            append('"')
            for (ch in value) {
                if (ch == '"') append("\"\"") else append(ch)
            }
            append('"')
        }
    }

    fun parseCsvBytes(bytes: ByteArray): CsvParseResult {
        if (bytes.isEmpty()) {
            return CsvParseResult.Error("The selected CSV file is empty.")
        }

        val decoded = try {
            val decoder = Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
            decoder.decode(ByteBuffer.wrap(bytes)).toString()
        } catch (_: CharacterCodingException) {
            return CsvParseResult.Error("Invalid UTF-8 file encoding. Import aborted; no cards were added.")
        }

        val withoutBom = if (decoded.startsWith(UTF8_BOM_CHAR)) {
            decoded.drop(1)
        } else {
            decoded
        }

        if (withoutBom.isBlank()) {
            return CsvParseResult.Error("The selected CSV file contains no rows.")
        }

        val delimiter = detectDelimiter(withoutBom)
        val records = parseRecords(withoutBom, delimiter)
            .filter { record -> record.any { it.isNotBlank() } }

        if (records.isEmpty()) {
            return CsvParseResult.Error("No data rows found in CSV.")
        }

        val firstRowNormalized = records.first().map { it.trim().lowercase(java.util.Locale.ROOT) }
        val isMultiDeckHeader = firstRowNormalized.size >= 4 &&
            firstRowNormalized[0] == "deck" &&
            firstRowNormalized[1] == "language" &&
            firstRowNormalized[2] == "front" &&
            firstRowNormalized[3] == "back"

        if (isMultiDeckHeader) {
            var skipped = 0
            val dataRows = mutableListOf<CsvDeckCardRow>()
            for (row in records.drop(1)) {
                if (row.size < 4 || row[2].isBlank() || row[3].isBlank()) {
                    skipped++
                    continue
                }
                val deckName = row[0].ifBlank { "Imported Deck" }
                val lang = row[1].ifBlank { "und" }
                dataRows.add(CsvDeckCardRow(deckName = deckName, languageTag = lang, front = row[2], back = row[3]))
            }
            return CsvParseResult.MultiDeck(rows = dataRows, delimiter = delimiter, skippedInvalidRows = skipped)
        }

        val isTwoColHeader = firstRowNormalized.size >= 2 &&
            ((firstRowNormalized[0] == "front" && firstRowNormalized[1] == "back") ||
                (firstRowNormalized[0] == "recto" && firstRowNormalized[1] == "verso"))

        val dataRecords = if (isTwoColHeader) records.drop(1) else records
        var skipped = 0
        val cardRows = mutableListOf<CsvCardRow>()
        for (row in dataRecords) {
            if (row.size < 2 || row[0].isBlank() || row[1].isBlank()) {
                skipped++
                continue
            }
            cardRows.add(CsvCardRow(front = row[0], back = row[1]))
        }
        return CsvParseResult.SingleDeck(rows = cardRows, delimiter = delimiter, skippedInvalidRows = skipped)
    }

    internal fun detectDelimiter(text: String): Char {
        val candidates = charArrayOf(',', ';', '\t')
        val counts = mutableMapOf(',' to 0, ';' to 0, '\t' to 0)
        var inQuotes = false
        var nonEmptyLinesChecked = 0
        var lineHasContent = false

        var i = 0
        while (i < text.length && nonEmptyLinesChecked < 5) {
            val ch = text[i]
            if (ch == '"') {
                if (inQuotes && i + 1 < text.length && text[i + 1] == '"') {
                    i += 2
                    continue
                }
                inQuotes = !inQuotes
                lineHasContent = true
            } else if (!inQuotes && (ch == '\n' || ch == '\r')) {
                if (lineHasContent) {
                    nonEmptyLinesChecked++
                    lineHasContent = false
                    if (counts.values.any { it > 0 }) break
                }
            } else {
                if (!ch.isWhitespace()) lineHasContent = true
                if (!inQuotes && ch in candidates) {
                    counts[ch] = (counts[ch] ?: 0) + 1
                }
            }
            i++
        }

        val best = counts.maxByOrNull { it.value }
        return if (best != null && best.value > 0) best.key else ','
    }

    private fun parseRecords(text: String, delimiter: Char): List<List<String>> {
        val records = mutableListOf<List<String>>()
        var currentRow = mutableListOf<String>()
        val currentField = StringBuilder()
        var inQuotes = false
        var fieldStarted = false

        var i = 0
        while (i < text.length) {
            val ch = text[i]
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < text.length && text[i + 1] == '"') {
                        currentField.append('"')
                        i += 2
                        continue
                    } else {
                        inQuotes = false
                    }
                } else {
                    currentField.append(ch)
                }
            } else {
                when (ch) {
                    '"' -> {
                        if (!fieldStarted && currentField.isEmpty()) {
                            inQuotes = true
                        } else {
                            currentField.append(ch)
                        }
                    }
                    delimiter -> {
                        currentRow.add(currentField.toString())
                        currentField.clear()
                        fieldStarted = false
                        i++
                        continue
                    }
                    '\r' -> {
                        currentRow.add(currentField.toString())
                        currentField.clear()
                        records.add(currentRow)
                        currentRow = mutableListOf()
                        fieldStarted = false
                        if (i + 1 < text.length && text[i + 1] == '\n') {
                            i += 2
                            continue
                        }
                        i++
                        continue
                    }
                    '\n' -> {
                        currentRow.add(currentField.toString())
                        currentField.clear()
                        records.add(currentRow)
                        currentRow = mutableListOf()
                        fieldStarted = false
                        i++
                        continue
                    }
                    else -> {
                        currentField.append(ch)
                    }
                }
            }
            fieldStarted = true
            i++
        }

        if (fieldStarted || currentField.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(currentField.toString())
            records.add(currentRow)
        }
        return records
    }
}
