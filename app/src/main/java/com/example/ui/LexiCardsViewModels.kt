package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.CardEntity
import com.example.data.CardWriteResult
import com.example.data.CsvImportOutcome
import com.example.data.DeckEntity
import com.example.data.DeckWithCount
import com.example.data.FlashcardRepository
import com.example.util.TextNormalization
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardUiState(
    val decks: List<DeckWithCount> = emptyList(),
    val totalCards: Int = 0,
    val isBusy: Boolean = false,
    val statusMessage: String? = null,
    val isErrorStatus: Boolean = false,
)

class DashboardViewModel(
    private val repository: FlashcardRepository,
) : ViewModel() {

    private val _busy = MutableStateFlow(false)
    private val _status = MutableStateFlow<Pair<String, Boolean>?>(null)

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.decksWithCount,
        _busy,
        _status,
    ) { decks, busy, status ->
        DashboardUiState(
            decks = decks,
            totalCards = decks.sumOf { it.cardCount },
            isBusy = busy,
            statusMessage = status?.first,
            isErrorStatus = status?.second ?: false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState(),
    )

    fun createDeck(name: String, languageTag: String) {
        viewModelScope.launch {
            repository.createDeck(name, languageTag)
        }
    }

    fun deleteDeck(deckId: Long) {
        viewModelScope.launch {
            repository.deleteDeck(deckId)
        }
    }

    fun clearStatus() {
        _status.value = null
    }

    fun setStatus(message: String, isError: Boolean = false) {
        _status.value = message to isError
    }

    suspend fun exportAllDecksCsvBytes(): ByteArray {
        return repository.exportAllDecksCsvBytes()
    }

    fun importCsvBytes(bytes: ByteArray, fallbackDeckName: String) {
        viewModelScope.launch {
            _busy.value = true
            when (
                val outcome = repository.importCsvBytes(
                    targetDeckId = null,
                    fallbackDeckName = fallbackDeckName,
                    fallbackLanguageTag = "und",
                    bytes = bytes,
                )
            ) {
                is CsvImportOutcome.Error -> {
                    _status.value = outcome.message to true
                }
                is CsvImportOutcome.Success -> {
                    val msg = buildString {
                        append("Imported ${outcome.importedCount} card(s)")
                        if (outcome.skippedDuplicates > 0) {
                            append(", skipped ${outcome.skippedDuplicates} duplicate(s)")
                        }
                        if (outcome.skippedInvalidRows > 0) {
                            append(", skipped ${outcome.skippedInvalidRows} invalid row(s)")
                        }
                        append(".")
                    }
                    _status.value = msg to false
                }
            }
            _busy.value = false
        }
    }

    companion object {
        fun factory(repository: FlashcardRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return DashboardViewModel(repository) as T
                }
            }
    }
}

data class DeckDetailUiState(
    val deck: DeckEntity? = null,
    val allCardsCount: Int = 0,
    val visibleCards: List<CardEntity> = emptyList(),
    val searchQuery: String = "",
    val isMcqEnabled: Boolean = false,
    val isBusy: Boolean = false,
    val statusMessage: String? = null,
    val isErrorStatus: Boolean = false,
)

class DeckViewModel(
    private val deckId: Long,
    private val repository: FlashcardRepository,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _busy = MutableStateFlow(false)
    private val _status = MutableStateFlow<Pair<String, Boolean>?>(null)

    private val sortedCardsFlow = combine(
        repository.observeDeck(deckId),
        repository.observeCards(deckId),
    ) { deck, cards ->
        val languageTag = deck?.languageTag ?: "und"
        deck to TextNormalization.sortCardsWithCollator(cards, languageTag)
    }.flowOn(Dispatchers.Default)

    val uiState: StateFlow<DeckDetailUiState> = combine(
        sortedCardsFlow,
        _searchQuery,
        _busy,
        _status,
    ) { (deck, sortedCards), query, busy, status ->
        val normalizedQuery = TextNormalization.computeSearchKey(query)
        val filtered = if (normalizedQuery.isEmpty()) {
            sortedCards
        } else {
            sortedCards.filter { card ->
                card.frontKey.contains(normalizedQuery) ||
                    TextNormalization.computeSearchKey(card.back).contains(normalizedQuery)
            }
        }
        DeckDetailUiState(
            deck = deck,
            allCardsCount = sortedCards.size,
            visibleCards = filtered,
            searchQuery = query,
            isMcqEnabled = sortedCards.size >= 4,
            isBusy = busy,
            statusMessage = status?.first,
            isErrorStatus = status?.second ?: false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DeckDetailUiState(),
    )

    val allCards: StateFlow<List<CardEntity>> = sortedCardsFlow
        .map { it.second }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    suspend fun saveCard(
        editingCardId: Long?,
        front: String,
        back: String,
    ): CardWriteResult {
        return if (editingCardId == null) {
            repository.addCard(deckId, front, back)
        } else {
            repository.updateCard(editingCardId, deckId, front, back)
        }
    }

    fun deleteCard(cardId: Long) {
        viewModelScope.launch {
            repository.deleteCard(cardId)
        }
    }

    fun clearStatus() {
        _status.value = null
    }

    fun setStatus(message: String, isError: Boolean = false) {
        _status.value = message to isError
    }

    suspend fun exportDeckCsvBytes(): ByteArray {
        return repository.exportDeckCsvBytes(deckId)
    }

    fun importCsvBytes(bytes: ByteArray) {
        viewModelScope.launch {
            _busy.value = true
            val currentDeck = uiState.value.deck
            when (
                val outcome = repository.importCsvBytes(
                    targetDeckId = deckId,
                    fallbackDeckName = currentDeck?.name ?: "Imported Deck",
                    fallbackLanguageTag = currentDeck?.languageTag ?: "und",
                    bytes = bytes,
                )
            ) {
                is CsvImportOutcome.Error -> {
                    _status.value = outcome.message to true
                }
                is CsvImportOutcome.Success -> {
                    val msg = buildString {
                        append("Imported ${outcome.importedCount} card(s)")
                        if (outcome.skippedDuplicates > 0) {
                            append(", skipped ${outcome.skippedDuplicates} duplicate(s)")
                        }
                        if (outcome.skippedInvalidRows > 0) {
                            append(", skipped ${outcome.skippedInvalidRows} invalid row(s)")
                        }
                        append(".")
                    }
                    _status.value = msg to false
                }
            }
            _busy.value = false
        }
    }

    companion object {
        fun factory(deckId: Long, repository: FlashcardRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return DeckViewModel(deckId, repository) as T
                }
            }
    }
}
