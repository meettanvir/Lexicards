package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.DeckLanguage
import com.example.data.DeckWithCount

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    onCreateDeck: (name: String, languageTag: String) -> Unit,
    onDeleteDeck: (deckId: Long) -> Unit,
    onOpenDeck: (deckId: Long) -> Unit,
    onStartStudy: (deckId: Long) -> Unit,
    onStartMcq: (deckId: Long) -> Unit,
    onImportCsvClick: () -> Unit,
    onExportAllCsvClick: () -> Unit,
    onClearStatus: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var pendingDeleteDeck by remember { mutableStateOf<DeckWithCount?>(null) }

    LaunchedEffect(uiState.statusMessage) {
        val message = uiState.statusMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            onClearStatus()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.dashboard_title),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = stringResource(R.string.dashboard_subtitle),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                modifier = Modifier.testTag("new_deck_fab"),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.action_new_deck),
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxSize(),
            ) {
                DashboardSummaryHeader(
                    deckCount = uiState.decks.size,
                    totalCards = uiState.totalCards,
                    onImportCsvClick = onImportCsvClick,
                    onExportAllCsvClick = onExportAllCsvClick,
                )

                if (uiState.decks.isEmpty()) {
                    EmptyDecksState(
                        onCreateDeckClick = { showCreateDialog = true },
                        onImportCsvClick = onImportCsvClick,
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("deck_list"),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(uiState.decks, key = { it.id }) { deck ->
                            DeckRowCard(
                                deck = deck,
                                onOpenDeck = { onOpenDeck(deck.id) },
                                onStartStudy = { onStartStudy(deck.id) },
                                onStartMcq = { onStartMcq(deck.id) },
                                onDeleteClick = { pendingDeleteDeck = deck },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateDeckDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, lang ->
                onCreateDeck(name, lang)
                showCreateDialog = false
            },
        )
    }

    pendingDeleteDeck?.let { deck ->
        AlertDialog(
            onDismissRequest = { pendingDeleteDeck = null },
            title = { Text("Delete deck?") },
            text = {
                Text(
                    text = "Delete \"${deck.name}\" and all ${deck.cardCount} card(s)?",
                    style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Content),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteDeck(deck.id)
                        pendingDeleteDeck = null
                    },
                    modifier = Modifier.testTag("confirm_delete_deck_button"),
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteDeck = null }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun DashboardSummaryHeader(
    deckCount: Int,
    totalCards: Int,
    onImportCsvClick: () -> Unit,
    onExportAllCsvClick: () -> Unit,
) {
    val gradient = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.secondary,
        ),
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.large,
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier
                .background(gradient)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "$deckCount ${if (deckCount == 1) "Deck" else "Decks"}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Text(
                        text = "$totalCards ${if (totalCards == 1) "Card" else "Cards"} stored locally",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                    )
                }
                Icon(
                    imageVector = Icons.Outlined.Style,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                    modifier = Modifier.size(32.dp),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onImportCsvClick,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("dashboard_import_csv_button"),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                        contentColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FileUpload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.action_import_csv))
                }

                OutlinedButton(
                    onClick = onExportAllCsvClick,
                    enabled = totalCards > 0,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("dashboard_export_csv_button"),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                        contentColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.45f),
                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FileDownload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.action_export_csv))
                }
            }
        }
    }
}

@Composable
private fun DeckRowCard(
    deck: DeckWithCount,
    onOpenDeck: () -> Unit,
    onStartStudy: () -> Unit,
    onStartMcq: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    val language = DeckLanguage.fromTag(deck.languageTag)
    val mcqEnabled = deck.cardCount >= 4

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("deck_card_${deck.id}")
            .clickable(onClick = onOpenDeck),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = deck.name,
                        style = MaterialTheme.typography.titleLarge.copy(textDirection = TextDirection.Content),
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                        ) {
                            Text(
                                text = language.displayName,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                        Text(
                            text = "${deck.cardCount} ${if (deck.cardCount == 1) "card" else "cards"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("delete_deck_${deck.id}"),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete ${deck.name}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onStartStudy,
                    enabled = deck.cardCount >= 1,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("deck_study_${deck.id}"),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Study")
                }

                Button(
                    onClick = onStartMcq,
                    enabled = mcqEnabled,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("deck_mcq_${deck.id}"),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Quiz,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("MCQ")
                }
            }

            if (!mcqEnabled) {
                Text(
                    text = "${stringResource(R.string.mcq_disabled_hint)} (${deck.cardCount}/4)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun EmptyDecksState(
    onCreateDeckClick: () -> Unit,
    onImportCsvClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(72.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.Style,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(36.dp),
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.empty_decks_title),
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.empty_decks_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onCreateDeckClick,
                modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
                Text(stringResource(R.string.action_new_deck))
            }
            OutlinedButton(
                onClick = onImportCsvClick,
                modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
                Text(stringResource(R.string.action_import_csv))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreateDeckDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, languageTag: String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var selectedLang by rememberSaveable { mutableStateOf(DeckLanguage.FRENCH) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.action_new_deck)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Deck name") },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        textDirection = TextDirection.Content,
                    ),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("deck_name_input"),
                )

                Text(
                    text = "Deck language (for sorting)",
                    style = MaterialTheme.typography.labelLarge,
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DeckLanguage.entries.forEach { lang ->
                        FilterChip(
                            selected = selectedLang == lang,
                            onClick = { selectedLang = lang },
                            label = { Text(lang.displayName) },
                            modifier = Modifier.testTag("lang_chip_${lang.tag}"),
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, selectedLang.tag) },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("confirm_create_deck_button"),
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
