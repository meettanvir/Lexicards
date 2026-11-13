package com.example.data

import androidx.room.withTransaction
import com.example.util.CsvCardRow
import com.example.util.CsvCodec
import com.example.util.CsvDeckCardRow
import com.example.util.CsvParseResult
import com.example.util.TextNormalization
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

sealed class CardWriteResult {
    data class Success(val cardId: Long) : CardWriteResult()
    data class ValidationError(val message: String) : CardWriteResult()
    data class Duplicate(val existingFront: String) : CardWriteResult()
}

sealed class CsvImportOutcome {
    data class Success(
        val importedCount: Int,
        val skippedDuplicates: Int,
        val skippedInvalidRows: Int,
        val targetDeckId: Long?,
    ) : CsvImportOutcome()

    data class Error(val message: String) : CsvImportOutcome()
}

class FlashcardRepository(
    private val database: AppDatabase,
) {
    private val deckDao = database.deckDao()
    private val cardDao = database.cardDao()

    val decksWithCount: Flow<List<DeckWithCount>> = deckDao.observeDecksWithCount()

    fun observeDeck(deckId: Long): Flow<DeckEntity?> = deckDao.observeDeck(deckId)

    fun observeCards(deckId: Long): Flow<List<CardEntity>> = cardDao.observeCardsForDeck(deckId)

    suspend fun createDeck(name: String, languageTag: String): Long = withContext(Dispatchers.IO) {
        val cleanName = name.trim().ifBlank { "Untitled Deck" }
        deckDao.insertDeck(DeckEntity(name = cleanName, languageTag = languageTag))
    }

    suspend fun deleteDeck(deckId: Long) = withContext(Dispatchers.IO) {
        deckDao.deleteDeckById(deckId)
    }

    suspend fun wipeAllData() = withContext(Dispatchers.IO) {
        deckDao.deleteAllDecks()
    }

    suspend fun addCard(deckId: Long, front: String, back: String): CardWriteResult =
        withContext(Dispatchers.IO) {
            if (front.isBlank() || back.isBlank()) {
                return@withContext CardWriteResult.ValidationError("Both front and back are required.")
            }
            val frontKey = TextNormalization.computeFrontKey(front)
            if (frontKey.isEmpty()) {
                return@withContext CardWriteResult.ValidationError("Front text cannot be empty.")
            }
            database.withTransaction {
                val existing = cardDao.findByFrontKey(deckId, frontKey)
                if (existing != null) {
                    CardWriteResult.Duplicate(existing.front)
                } else {
                    val id = cardDao.insertCard(
                        CardEntity(
                            deckId = deckId,
                            front = front,
                            frontKey = frontKey,
                            back = back,
                        )
                    )
                    CardWriteResult.Success(id)
                }
            }
        }

    suspend fun updateCard(
        cardId: Long,
        deckId: Long,
        front: String,
        back: String,
    ): CardWriteResult = withContext(Dispatchers.IO) {
        if (front.isBlank() || back.isBlank()) {
            return@withContext CardWriteResult.ValidationError("Both front and back are required.")
        }
        val frontKey = TextNormalization.computeFrontKey(front)
        if (frontKey.isEmpty()) {
            return@withContext CardWriteResult.ValidationError("Front text cannot be empty.")
        }
        database.withTransaction {
            val existing = cardDao.findByFrontKey(deckId, frontKey)
            if (existing != null && existing.id != cardId) {
                CardWriteResult.Duplicate(existing.front)
            } else {
                cardDao.updateCard(
                    CardEntity(
                        id = cardId,
                        deckId = deckId,
                        front = front,
                        frontKey = frontKey,
                        back = back,
                    )
                )
                CardWriteResult.Success(cardId)
            }
        }
    }

    suspend fun deleteCard(cardId: Long) = withContext(Dispatchers.IO) {
        cardDao.deleteCardById(cardId)
    }

    suspend fun exportDeckCsvBytes(deckId: Long): ByteArray = withContext(Dispatchers.IO) {
        val deck = deckDao.getDeckById(deckId)
        val cards = cardDao.getCardsForDeck(deckId)
        val sorted = if (deck != null) {
            TextNormalization.sortCardsWithCollator(cards, deck.languageTag)
        } else {
            cards
        }
        CsvCodec.exportDeckCsvBytes(
            sorted.map { CsvCardRow(front = it.front, back = it.back) }
        )
    }

    suspend fun exportAllDecksCsvBytes(): ByteArray = withContext(Dispatchers.IO) {
        val decksById = deckDao.getAllDecks().associateBy { it.id }
        val allCards = cardDao.getAllCards()
        val rows = allCards.mapNotNull { card ->
            val deck = decksById[card.deckId] ?: return@mapNotNull null
            CsvDeckCardRow(
                deckName = deck.name,
                languageTag = deck.languageTag,
                front = card.front,
                back = card.back,
            )
        }
        CsvCodec.exportAllDecksCsvBytes(rows)
    }

    suspend fun importCsvBytes(
        targetDeckId: Long?,
        fallbackDeckName: String,
        fallbackLanguageTag: String,
        bytes: ByteArray,
    ): CsvImportOutcome = withContext(Dispatchers.IO) {
        when (val parsed = CsvCodec.parseCsvBytes(bytes)) {
            is CsvParseResult.Error -> CsvImportOutcome.Error(parsed.message)
            is CsvParseResult.SingleDeck -> {
                if (parsed.rows.isEmpty()) {
                    return@withContext CsvImportOutcome.Error("No valid card rows found in CSV.")
                }
                database.withTransaction {
                    val resolvedDeckId = targetDeckId ?: deckDao.insertDeck(
                        DeckEntity(
                            name = fallbackDeckName.ifBlank { "Imported Deck" },
                            languageTag = fallbackLanguageTag,
                        )
                    )
                    var imported = 0
                    var duplicates = 0
                    for (row in parsed.rows) {
                        val key = TextNormalization.computeFrontKey(row.front)
                        if (key.isEmpty()) continue
                        if (cardDao.findByFrontKey(resolvedDeckId, key) != null) {
                            duplicates++
                        } else {
                            cardDao.insertCard(
                                CardEntity(
                                    deckId = resolvedDeckId,
                                    front = row.front,
                                    frontKey = key,
                                    back = row.back,
                                )
                            )
                            imported++
                        }
                    }
                    CsvImportOutcome.Success(
                        importedCount = imported,
                        skippedDuplicates = duplicates,
                        skippedInvalidRows = parsed.skippedInvalidRows,
                        targetDeckId = resolvedDeckId,
                    )
                }
            }
            is CsvParseResult.MultiDeck -> {
                if (parsed.rows.isEmpty()) {
                    return@withContext CsvImportOutcome.Error("No valid card rows found in CSV.")
                }
                database.withTransaction {
                    var imported = 0
                    var duplicates = 0
                    var firstDeckId: Long? = targetDeckId
                    val deckCache = mutableMapOf<String, Long>()

                    for (row in parsed.rows) {
                        val deckId = if (targetDeckId != null) {
                            targetDeckId
                        } else {
                            deckCache.getOrPut(row.deckName) {
                                val existing = deckDao.getDeckByName(row.deckName)
                                existing?.id ?: deckDao.insertDeck(
                                    DeckEntity(name = row.deckName, languageTag = row.languageTag)
                                )
                            }
                        }
                        if (firstDeckId == null) firstDeckId = deckId

                        val key = TextNormalization.computeFrontKey(row.front)
                        if (key.isEmpty()) continue
                        if (cardDao.findByFrontKey(deckId, key) != null) {
                            duplicates++
                        } else {
                            cardDao.insertCard(
                                CardEntity(
                                    deckId = deckId,
                                    front = row.front,
                                    frontKey = key,
                                    back = row.back,
                                )
                            )
                            imported++
                        }
                    }
                    CsvImportOutcome.Success(
                        importedCount = imported,
                        skippedDuplicates = duplicates,
                        skippedInvalidRows = parsed.skippedInvalidRows,
                        targetDeckId = firstDeckId,
                    )
                }
            }
        }
    }
}
