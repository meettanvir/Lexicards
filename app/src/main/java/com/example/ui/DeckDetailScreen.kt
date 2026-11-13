package com.example.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.CardEntity
import com.example.data.CardWriteResult
import com.example.data.DeckLanguage
import com.example.util.GraphemeUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckDetailScreen(
    uiState: DeckDetailUiState,
    onBack: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSaveCard: suspend (editingCardId: Long?, front: String, back: String) -> CardWriteResult,
    onDeleteCard: (cardId: Long) -> Unit,
    onStartStudy: () -> Unit,
    onStartMcq: () -> Unit,
    onImportCsvClick: () -> Unit,
    onExportCsvClick: () -> Unit,
    onClearStatus: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showEditor by rememberSaveable { mutableStateOf(false) }
    var editingCard by remember { mutableStateOf<CardEntity?>(null) }

    LaunchedEffect(uiState.statusMessage) {
        val message = uiState.statusMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            onClearStatus()
        }
    }

    val deck = uiState.deck
    val language = deck?.let { DeckLanguage.fromTag(it.languageTag) } ?: DeckLanguage.UNIVERSAL

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("deck_back_button"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = deck?.name ?: "Deck",
                            style = MaterialTheme.typography.titleLarge.copy(
                                textDirection = TextDirection.Content,
                            ),
                        )
                        Text(
                            text = "${language.displayName} • ${uiState.allCardsCount} card(s)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onImportCsvClick,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("deck_import_csv_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FileUpload,
                            contentDescription = stringResource(R.string.action_import_csv),
                        )
                    }
                    IconButton(
                        onClick = onExportCsvClick,
                        enabled = uiState.allCardsCount > 0,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("deck_export_csv_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FileDownload,
                            contentDescription = stringResource(R.string.action_export_csv),
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
                onClick = {
                    editingCard = null
                    showEditor = true
                },
                modifier = Modifier.testTag("add_card_fab"),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.action_add_card),
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
                StudyActionsBar(
                    cardCount = uiState.allCardsCount,
                    isMcqEnabled = uiState.isMcqEnabled,
                    onStartStudy = onStartStudy,
                    onStartMcq = onStartMcq,
                )

                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = {
                        Text(
                            text = stringResource(R.string.search_cards_placeholder),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                        )
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { onSearchQueryChange("") },
                                modifier = Modifier.minimumInteractiveComponentSize(),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Clear search",
                                )
                            }
                        }
                    },
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
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("search_cards_input"),
                )

                if (uiState.visibleCards.isEmpty()) {
                    EmptyCardsState(
                        hasSearchQuery = uiState.searchQuery.isNotBlank(),
                        onAddCardClick = {
                            editingCard = null
                            showEditor = true
                        },
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("card_list"),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(uiState.visibleCards, key = { it.id }) { card ->
                            FlashcardListItem(
                                card = card,
                                onEdit = {
                                    editingCard = card
                                    showEditor = true
                                },
                                onDelete = { onDeleteCard(card.id) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showEditor) {
        CardEditorDialog(
            initialCard = editingCard,
            onDismiss = {
                showEditor = false
                editingCard = null
            },
            onSave = { front, back ->
                onSaveCard(editingCard?.id, front, back)
            },
        )
    }
}

@Composable
private fun StudyActionsBar(
    cardCount: Int,
    isMcqEnabled: Boolean,
    onStartStudy: () -> Unit,
    onStartMcq: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onStartStudy,
                    enabled = cardCount >= 1,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("study_cards_button"),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.action_study_flip))
                }

                Button(
                    onClick = onStartMcq,
                    enabled = isMcqEnabled,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("mcq_quiz_button"),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Quiz,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.action_study_mcq))
                }
            }

            if (!isMcqEnabled) {
                Text(
                    text = "${stringResource(R.string.mcq_disabled_hint)} ($cardCount/4)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("mcq_disabled_hint_text"),
                )
            }
        }
    }
}

@Composable
private fun FlashcardListItem(
    card: CardEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val graphemeCount = remember(card.front) { GraphemeUtils.graphemeCount(card.front) }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .testTag("card_item_${card.id}"),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = card.front,
                    style = MaterialTheme.typography.titleLarge.copy(
                        textDirection = TextDirection.Content,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 2.dp),
                )
                Row {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("edit_card_${card.id}"),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit card",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("delete_card_${card.id}"),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Delete card",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

            Text(
                text = card.back,
                style = MaterialTheme.typography.bodyLarge.copy(
                    textDirection = TextDirection.Content,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
            )

            Text(
                text = "$graphemeCount ${if (graphemeCount == 1) "grapheme" else "graphemes"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            )
        }
    }
}

@Composable
private fun EmptyCardsState(
    hasSearchQuery: Boolean,
    onAddCardClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = if (hasSearchQuery) "No matching cards" else stringResource(R.string.empty_cards_title),
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (hasSearchQuery) {
                "Search matches case-insensitively while keeping accents distinct."
            } else {
                stringResource(R.string.empty_cards_body)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!hasSearchQuery) {
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onAddCardClick,
                modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
                Text(stringResource(R.string.action_add_card))
            }
        }
    }
}

@Composable
private fun CardEditorDialog(
    initialCard: CardEntity?,
    onDismiss: () -> Unit,
    onSave: suspend (front: String, back: String) -> CardWriteResult,
) {
    val scope = rememberCoroutineScope()
    var front by rememberSaveable(initialCard?.id) { mutableStateOf(initialCard?.front ?: "") }
    var back by rememberSaveable(initialCard?.id) { mutableStateOf(initialCard?.back ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    val frontGraphemeCount = remember(front) { GraphemeUtils.graphemeCount(front) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initialCard == null) "Add Card" else "Edit Card")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = front,
                    onValueChange = {
                        front = it
                        errorMessage = null
                    },
                    label = { Text("Front (term / prompt)") },
                    supportingText = {
                        Text("$frontGraphemeCount ${if (frontGraphemeCount == 1) "grapheme" else "graphemes"}")
                    },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        textDirection = TextDirection.Content,
                    ),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_front_input"),
                )

                OutlinedTextField(
                    value = back,
                    onValueChange = {
                        back = it
                        errorMessage = null
                    },
                    label = { Text("Back (translation / answer)") },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        textDirection = TextDirection.Content,
                    ),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_back_input"),
                )

                errorMessage?.let { msg ->
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                textDirection = TextDirection.Content,
                            ),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp)
                                .testTag("card_error_text"),
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        isSaving = true
                        when (val result = onSave(front, back)) {
                            is CardWriteResult.Success -> {
                                isSaving = false
                                onDismiss()
                            }
                            is CardWriteResult.Duplicate -> {
                                isSaving = false
                                errorMessage = "Duplicate front: matches existing \"${result.existingFront}\"."
                            }
                            is CardWriteResult.ValidationError -> {
                                isSaving = false
                                errorMessage = result.message
                            }
                        }
                    }
                },
                enabled = !isSaving,
                modifier = Modifier.testTag("save_card_button"),
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_card_button"),
            ) {
                Text("Cancel")
            }
        },
    )
}
