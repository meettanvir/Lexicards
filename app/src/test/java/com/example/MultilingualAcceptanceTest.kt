package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.CardWriteResult
import com.example.data.CsvImportOutcome
import com.example.data.FlashcardRepository
import com.example.ui.theme.LexiCardsTheme
import com.example.util.CsvCodec
import com.example.util.CsvParseResult
import com.example.util.GraphemeUtils
import com.example.util.TextNormalization
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.text.Normalizer

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MultilingualAcceptanceTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var database: AppDatabase
    private lateinit var repository: FlashcardRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = FlashcardRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `acceptance - accent different vs case duplicate vs apostrophe vs NBSP`() = runBlocking {
        val deckId = repository.createDeck("Français", "fr")

        // Add "élève", then "eleve": both accepted as different cards
        assertTrue(repository.addCard(deckId, "élève", "student") is CardWriteResult.Success)
        assertTrue(repository.addCard(deckId, "eleve", "raise (imperative)") is CardWriteResult.Success)

        // Add "Élève" after "élève": rejected as duplicate
        assertTrue(repository.addCard(deckId, "Élève", "pupil") is CardWriteResult.Duplicate)

        // Add NFD decomposed "e\u0301lève": matches NFC "élève", rejected as duplicate
        val nfdEleve = Normalizer.normalize("élève", Normalizer.Form.NFD)
        assertTrue(repository.addCard(deckId, nfdEleve, "pupil") is CardWriteResult.Duplicate)

        // Add "l’école", then "l'école": rejected as duplicate, stored "l’école" preserved as-is
        assertTrue(repository.addCard(deckId, "l’école", "school") is CardWriteResult.Success)
        assertTrue(repository.addCard(deckId, "l'école", "school2") is CardWriteResult.Duplicate)

        // Add "Ça va\u00A0?" (NBSP), then "Ça va ?" (normal space) and narrow NBSP: rejected as duplicate
        assertTrue(repository.addCard(deckId, "Ça va\u00A0?", "How are you?") is CardWriteResult.Success)
        assertTrue(repository.addCard(deckId, "Ça va ?", "How goes it?") is CardWriteResult.Duplicate)
        assertTrue(repository.addCard(deckId, "Ça va\u202F?", "How goes it?") is CardWriteResult.Duplicate)

        val storedCards = repository.observeCards(deckId).first()
        assertEquals(4, storedCards.size)
        // Stored text remains unchanged byte-for-byte
        assertTrue(storedCards.any { it.front == "l’école" })
        assertTrue(storedCards.any { it.front == "Ça va\u00A0?" })
    }

    @Test
    fun `acceptance - Bengali conjuncts, ZWJ ZWNJ preservation, and BreakIterator grapheme safety`() = runBlocking {
        val deckId = repository.createDeck("বাংলা", "bn")
        val khomota = "ক্ষমতা"
        val zwjSample = "র\u200D্য" // ZWJ preserved
        val zwnjSample = "ক\u200C্ষ" // ZWNJ preserved

        assertTrue(repository.addCard(deckId, "ঢাকা", "Dhaka") is CardWriteResult.Success)
        assertTrue(repository.addCard(deckId, "বাংলাদেশ", "Bangladesh") is CardWriteResult.Success)
        assertTrue(repository.addCard(deckId, khomota, "Power") is CardWriteResult.Success)
        assertTrue(repository.addCard(deckId, zwjSample, "ZWJ sample") is CardWriteResult.Success)
        assertTrue(repository.addCard(deckId, zwnjSample, "ZWNJ sample") is CardWriteResult.Success)

        val stored = repository.observeCards(deckId).first()
        assertEquals(zwjSample, stored.first { it.back == "ZWJ sample" }.front)
        assertEquals(zwnjSample, stored.first { it.back == "ZWNJ sample" }.front)

        // Grapheme safety: "ক্ষ" is 1 grapheme cluster (not split into ক + ্ + ষ)
        assertEquals("ক্ষ", GraphemeUtils.firstGrapheme(khomota))
        assertEquals(3, GraphemeUtils.graphemeCount(khomota)) // ক্ষ, ম, তা
        assertEquals("ক্ষ__", GraphemeUtils.progressiveHint(khomota, 1))
    }

    @Test
    fun `acceptance - CSV export with BOM, wipe data, and reimport byte-identical`() = runBlocking {
        val frDeck = repository.createDeck("Français", "fr")
        val bnDeck = repository.createDeck("বাংলা + RTL", "bn")

        val expectedPairs = listOf(
            frDeck to ("élève" to "student"),
            frDeck to ("eleve" to "raise"),
            frDeck to ("l’école" to "school"),
            frDeck to ("Ça va\u00A0?" to "How are you?"),
            bnDeck to ("ঢাকা" to "Dhaka"),
            bnDeck to ("বাংলাদেশ" to "Bangladesh"),
            bnDeck to ("ক্ষমতা" to "ক্ষমতা — Power"),
            bnDeck to ("র\u200D্য\u200C" to "ZWJ+ZWNJ"),
            bnDeck to ("العربية" to "Arabic"),
        )
        for ((dId, pair) in expectedPairs) {
            assertTrue(repository.addCard(dId, pair.first, pair.second) is CardWriteResult.Success)
        }

        val exportedBytes = repository.exportAllDecksCsvBytes()
        // Verify UTF-8 BOM (EF BB BF)
        assertEquals(0xEF.toByte(), exportedBytes[0])
        assertEquals(0xBB.toByte(), exportedBytes[1])
        assertEquals(0xBF.toByte(), exportedBytes[2])

        // Wipe all data
        repository.wipeAllData()
        assertEquals(0, repository.decksWithCount.first().size)

        // Re-import
        val outcome = repository.importCsvBytes(
            targetDeckId = null,
            fallbackDeckName = "Restored",
            fallbackLanguageTag = "und",
            bytes = exportedBytes,
        )
        assertTrue(outcome is CsvImportOutcome.Success)
        assertEquals(9, (outcome as CsvImportOutcome.Success).importedCount)

        val restoredDecks = repository.decksWithCount.first()
        assertEquals(2, restoredDecks.size)
        val reExportedBytes = repository.exportAllDecksCsvBytes()
        assertArrayEquals(exportedBytes, reExportedBytes)

        val restoredBnCards = repository.observeCards(restoredDecks.first { it.name == "বাংলা + RTL" }.id).first()
        assertEquals("ক্ষমতা — Power", restoredBnCards.first { it.front == "ক্ষমতা" }.back)
        assertEquals("র\u200D্য\u200C", restoredBnCards.first { it.back == "ZWJ+ZWNJ" }.front)
        assertEquals("العربية", restoredBnCards.first { it.back == "Arabic" }.front)
    }

    @Test
    fun `acceptance - semicolon delimited French CSV and invalid UTF-8 abort`() = runBlocking {
        val deckId = repository.createDeck("French Excel", "fr")
        val semicolonCsv = "front;back\n\"élève\";\"student\"\n\"l’école\";\"school\"\n\"Ça va\u00A0?\";\"How are you?\""
            .toByteArray(Charsets.UTF_8)

        val result = repository.importCsvBytes(deckId, "French Excel", "fr", semicolonCsv)
        assertTrue(result is CsvImportOutcome.Success)
        assertEquals(3, (result as CsvImportOutcome.Success).importedCount)

        // Invalid UTF-8 byte sequence (0xFF 0xFE 0x80) must abort and import nothing
        val invalidBytes = byteArrayOf(0xFF.toByte(), 0xFE.toByte(), 0x80.toByte(), 0x61, 0x3B, 0x62)
        val parseErr = CsvCodec.parseCsvBytes(invalidBytes)
        assertTrue(parseErr is CsvParseResult.Error)

        val outcome = repository.importCsvBytes(deckId, "French Excel", "fr", invalidBytes)
        assertTrue(outcome is CsvImportOutcome.Error)
        assertEquals(3, repository.observeCards(deckId).first().size)
    }

    @Test
    fun `acceptance - accent-sensitive search and Collator sorting`() = runBlocking {
        val deckId = repository.createDeck("SearchTest", "fr")
        repository.addCard(deckId, "élève", "student")
        repository.addCard(deckId, "eleve", "raise")
        repository.addCard(deckId, "l’école", "school")

        val cards = repository.observeCards(deckId).first()
        val sorted = TextNormalization.sortCardsWithCollator(cards, "fr")
        assertEquals(3, sorted.size)

        val queryEleveAccented = TextNormalization.computeSearchKey("ÉLÈVE")
        val matchesAccented = sorted.filter { it.frontKey.contains(queryEleveAccented) }
        assertEquals(listOf("élève"), matchesAccented.map { it.front })

        val queryElevePlain = TextNormalization.computeSearchKey("ELEVE")
        val matchesPlain = sorted.filter { it.frontKey.contains(queryElevePlain) }
        assertEquals(listOf("eleve"), matchesPlain.map { it.front })
    }

    @Test
    fun `ui checklist - duplicate add, accent different add, RTL, Bengali, and MCQ disabled at 3 cards`() {
        composeRule.setContent {
            LexiCardsTheme {
                LexiCardsApp(repository = repository)
            }
        }
        composeRule.waitForIdle()

        // Create a deck
        composeRule.onNodeWithTag("new_deck_fab").performClick()
        composeRule.onNodeWithTag("deck_name_input").performTextInput("Polyglot")
        composeRule.onNodeWithTag("confirm_create_deck_button").performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("Polyglot").fetchSemanticsNodes().isNotEmpty()
        }

        // Open deck
        composeRule.onNodeWithText("Polyglot").performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("add_card_fab").fetchSemanticsNodes().isNotEmpty()
        }

        // Add card 1: "élève"
        addCardViaUi("élève", "student", expectedCount = 1)
        composeRule.onNodeWithTag("card_list").performScrollToNode(hasText("élève"))
        composeRule.onNodeWithText("élève").assertIsDisplayed()

        // Duplicate check: "Élève" must be rejected with inline error
        composeRule.onNodeWithTag("add_card_fab").performClick()
        composeRule.onNodeWithTag("card_front_input").performTextInput("Élève")
        composeRule.onNodeWithTag("card_back_input").performTextInput("pupil")
        composeRule.onNodeWithTag("save_card_button").performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("card_error_text").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("card_error_text").assertIsDisplayed()
        composeRule.onNodeWithTag("cancel_card_button").performClick()
        composeRule.waitForIdle()

        // Add card 2: "eleve" (different accent -> accepted)
        addCardViaUi("eleve", "raise", expectedCount = 2)
        composeRule.onNodeWithTag("card_list").performScrollToNode(hasText("eleve"))
        composeRule.onNodeWithText("eleve").assertIsDisplayed()

        // Add card 3: Bengali "ক্ষমতা"
        addCardViaUi("ক্ষমতা", "ঢাকা বাংলাদেশ", expectedCount = 3)
        composeRule.onNodeWithTag("card_list").performScrollToNode(hasText("ক্ষমতা"))
        composeRule.onNodeWithText("ক্ষমতা").assertIsDisplayed()

        // With exactly 3 cards, MCQ Quiz button MUST be disabled!
        composeRule.onNodeWithTag("mcq_quiz_button").assertIsNotEnabled()
        composeRule.onNodeWithTag("mcq_disabled_hint_text").assertIsDisplayed()

        // Add card 4: Arabic RTL "العربية"
        addCardViaUi("العربية", "Arabic", expectedCount = 4)
        composeRule.onNodeWithTag("card_list").performScrollToNode(hasText("العربية"))
        composeRule.onNodeWithText("العربية").assertIsDisplayed()

        // With 4 cards, MCQ Quiz button MUST now be enabled!
        composeRule.onNodeWithTag("mcq_quiz_button").assertIsEnabled()

        // Open Study screen
        composeRule.onNodeWithTag("study_cards_button").performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("study_front_text").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("study_front_text").assertIsDisplayed()
    }

    private fun addCardViaUi(front: String, back: String, expectedCount: Int) {
        composeRule.onNodeWithTag("add_card_fab").performClick()
        composeRule.onNodeWithTag("card_front_input").performTextInput(front)
        composeRule.onNodeWithTag("card_back_input").performTextInput(back)
        composeRule.onNodeWithTag("save_card_button").performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("card_front_input").fetchSemanticsNodes().isEmpty() &&
                composeRule.onAllNodesWithText("$expectedCount card(s)", substring = true)
                    .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.waitForIdle()
    }
}
