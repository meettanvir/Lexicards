package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DeckDao {
    @Query(
        """
        SELECT d.id, d.name, d.languageTag, d.createdAt, COUNT(c.id) AS cardCount
        FROM decks d
        LEFT JOIN cards c ON c.deckId = d.id
        GROUP BY d.id
        ORDER BY d.createdAt DESC, d.id DESC
        """
    )
    fun observeDecksWithCount(): Flow<List<DeckWithCount>>

    @Query("SELECT * FROM decks ORDER BY createdAt DESC, id DESC")
    suspend fun getAllDecks(): List<DeckEntity>

    @Query("SELECT * FROM decks WHERE id = :deckId LIMIT 1")
    fun observeDeck(deckId: Long): Flow<DeckEntity?>

    @Query("SELECT * FROM decks WHERE id = :deckId LIMIT 1")
    suspend fun getDeckById(deckId: Long): DeckEntity?

    @Query("SELECT * FROM decks WHERE name = :name LIMIT 1")
    suspend fun getDeckByName(name: String): DeckEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertDeck(deck: DeckEntity): Long

    @Query("DELETE FROM decks WHERE id = :deckId")
    suspend fun deleteDeckById(deckId: Long)

    @Query("DELETE FROM decks")
    suspend fun deleteAllDecks()
}

@Dao
interface CardDao {
    @Query("SELECT * FROM cards WHERE deckId = :deckId ORDER BY id ASC")
    fun observeCardsForDeck(deckId: Long): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE deckId = :deckId ORDER BY id ASC")
    suspend fun getCardsForDeck(deckId: Long): List<CardEntity>

    @Query("SELECT * FROM cards ORDER BY deckId ASC, id ASC")
    suspend fun getAllCards(): List<CardEntity>

    @Query("SELECT * FROM cards WHERE deckId = :deckId AND frontKey = :frontKey LIMIT 1")
    suspend fun findByFrontKey(deckId: Long, frontKey: String): CardEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCard(card: CardEntity): Long

    @Update
    suspend fun updateCard(card: CardEntity)

    @Query("DELETE FROM cards WHERE id = :cardId")
    suspend fun deleteCardById(cardId: Long)
}
