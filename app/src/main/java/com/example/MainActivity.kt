package com.example

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.AppDatabase
import com.example.data.FlashcardRepository
import com.example.ui.DashboardScreen
import com.example.ui.DashboardViewModel
import com.example.ui.DeckDetailScreen
import com.example.ui.DeckViewModel
import com.example.ui.McqQuizScreen
import com.example.ui.StudyFlashcardScreen
import com.example.ui.theme.LexiCardsTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object LexiRoutes {
    const val ROUTE_DASHBOARD = "dashboard"
    const val ROUTE_DECK = "deck/{deckId}"
    const val ROUTE_STUDY = "study/{deckId}"
    const val ROUTE_MCQ = "mcq/{deckId}"

    fun deckRoute(deckId: Long): String = "deck/$deckId"
    fun studyRoute(deckId: Long): String = "study/$deckId"
    fun mcqRoute(deckId: Long): String = "mcq/$deckId"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = FlashcardRepository(AppDatabase.getInstance(applicationContext))
        setContent {
            LexiCardsTheme {
                LexiCardsApp(repository = repository)
            }
        }
    }
}

@Composable
fun LexiCardsApp(repository: FlashcardRepository) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var pendingImportAction by remember { mutableStateOf<((ByteArray, String) -> Unit)?>(null) }
    var pendingExportBytes by remember { mutableStateOf<ByteArray?>(null) }
    var pendingExportStatusCallback by remember { mutableStateOf<((String, Boolean) -> Unit)?>(null) }

    val openCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        val callback = pendingImportAction
        pendingImportAction = null
        if (uri == null || callback == null) return@rememberLauncherForActivityResult
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                val name = resolveDisplayName(context, uri)
                bytes to name
            }
            val bytes = result.first
            if (bytes != null) {
                callback(bytes, result.second)
            }
        }
    }

    val createCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri: Uri? ->
        val bytes = pendingExportBytes
        val statusCb = pendingExportStatusCallback
        pendingExportBytes = null
        pendingExportStatusCallback = null
        if (uri == null || bytes == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(bytes)
                        out.flush()
                    } ?: error("Cannot open output stream")
                }.isSuccess
            }
            statusCb?.invoke(
                if (ok) "Exported UTF-8 CSV with BOM." else "Failed to export CSV.",
                !ok,
            )
        }
    }

    NavHost(
        navController = navController,
        startDestination = LexiRoutes.ROUTE_DASHBOARD,
    ) {
        composable(LexiRoutes.ROUTE_DASHBOARD) {
            val viewModel: DashboardViewModel = viewModel(
                factory = DashboardViewModel.factory(repository),
            )
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            DashboardScreen(
                uiState = uiState,
                onCreateDeck = viewModel::createDeck,
                onDeleteDeck = viewModel::deleteDeck,
                onOpenDeck = { deckId -> navController.navigate(LexiRoutes.deckRoute(deckId)) },
                onStartStudy = { deckId -> navController.navigate(LexiRoutes.studyRoute(deckId)) },
                onStartMcq = { deckId -> navController.navigate(LexiRoutes.mcqRoute(deckId)) },
                onImportCsvClick = {
                    pendingImportAction = { bytes, displayName ->
                        val fallbackName = displayName.substringBeforeLast('.').ifBlank { "Imported Deck" }
                        viewModel.importCsvBytes(bytes, fallbackName)
                    }
                    openCsvLauncher.launch(arrayOf("text/*", "application/csv", "*/*"))
                },
                onExportAllCsvClick = {
                    scope.launch {
                        val bytes = viewModel.exportAllDecksCsvBytes()
                        pendingExportBytes = bytes
                        pendingExportStatusCallback = { msg, err -> viewModel.setStatus(msg, err) }
                        createCsvLauncher.launch("lexicards_all_decks.csv")
                    }
                },
                onClearStatus = viewModel::clearStatus,
            )
        }

        composable(
            route = LexiRoutes.ROUTE_DECK,
            arguments = listOf(navArgument("deckId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val deckId = backStackEntry.arguments?.getLong("deckId") ?: return@composable
            val viewModel: DeckViewModel = viewModel(
                key = "deck_$deckId",
                factory = DeckViewModel.factory(deckId, repository),
            )
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            DeckDetailScreen(
                uiState = uiState,
                onBack = { navController.popBackStack() },
                onSearchQueryChange = viewModel::updateSearchQuery,
                onSaveCard = viewModel::saveCard,
                onDeleteCard = viewModel::deleteCard,
                onStartStudy = { navController.navigate(LexiRoutes.studyRoute(deckId)) },
                onStartMcq = { navController.navigate(LexiRoutes.mcqRoute(deckId)) },
                onImportCsvClick = {
                    pendingImportAction = { bytes, _ -> viewModel.importCsvBytes(bytes) }
                    openCsvLauncher.launch(arrayOf("text/*", "application/csv", "*/*"))
                },
                onExportCsvClick = {
                    scope.launch {
                        val bytes = viewModel.exportDeckCsvBytes()
                        val safeName = (uiState.deck?.name ?: "deck")
                            .replace(Regex("[/\\\\:*?\"<>|]"), "_")
                            .ifBlank { "deck" }
                        pendingExportBytes = bytes
                        pendingExportStatusCallback = { msg, err -> viewModel.setStatus(msg, err) }
                        createCsvLauncher.launch("${safeName}.csv")
                    }
                },
                onClearStatus = viewModel::clearStatus,
            )
        }

        composable(
            route = LexiRoutes.ROUTE_STUDY,
            arguments = listOf(navArgument("deckId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val deckId = backStackEntry.arguments?.getLong("deckId") ?: return@composable
            val viewModel: DeckViewModel = viewModel(
                key = "study_$deckId",
                factory = DeckViewModel.factory(deckId, repository),
            )
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val cards by viewModel.allCards.collectAsStateWithLifecycle()

            StudyFlashcardScreen(
                deckName = uiState.deck?.name ?: "Study",
                cards = cards,
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = LexiRoutes.ROUTE_MCQ,
            arguments = listOf(navArgument("deckId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val deckId = backStackEntry.arguments?.getLong("deckId") ?: return@composable
            val viewModel: DeckViewModel = viewModel(
                key = "mcq_$deckId",
                factory = DeckViewModel.factory(deckId, repository),
            )
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val cards by viewModel.allCards.collectAsStateWithLifecycle()

            McqQuizScreen(
                deckName = uiState.deck?.name ?: "MCQ",
                cards = cards,
                onBack = { navController.popBackStack() },
            )
        }
    }
}

private fun resolveDisplayName(context: android.content.Context, uri: Uri): String {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) return cursor.getString(idx) ?: "imported.csv"
            }
        }
    return uri.lastPathSegment ?: "imported.csv"
}
