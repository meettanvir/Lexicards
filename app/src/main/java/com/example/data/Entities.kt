package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "decks")
data class DeckEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val languageTag: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "cards",
    foreignKeys = [
        ForeignKey(
            entity = DeckEntity::class,
            parentColumns = ["id"],
            childColumns = ["deckId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [
        Index(value = ["deckId"]),
        Index(value = ["deckId", "frontKey"], unique = true),
    ],
)
data class CardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deckId: Long,
    val front: String,
    val frontKey: String,
    val back: String,
    val createdAt: Long = System.currentTimeMillis(),
)

data class DeckWithCount(
    val id: Long,
    val name: String,
    val languageTag: String,
    val createdAt: Long,
    val cardCount: Int,
)

enum class DeckLanguage(val tag: String, val displayName: String) {
    FRENCH("fr", "Français (fr)"),
    BENGALI("bn", "বাংলা (bn)"),
    ENGLISH("en", "English (en)"),
    ARABIC("ar", "العربية (ar)"),
    HEBREW("he", "עברית (he)"),
    URDU("ur", "اردو (ur)"),
    UNIVERSAL("und", "Multilingual"),
    ;

    companion object {
        fun fromTag(tag: String): DeckLanguage =
            entries.firstOrNull { it.tag.equals(tag, ignoreCase = true) } ?: UNIVERSAL
    }
}
