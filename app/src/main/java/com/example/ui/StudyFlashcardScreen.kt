package com.example.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CardEntity
import com.example.util.GraphemeUtils
import com.example.util.TextNormalization

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyFlashcardScreen(
    deckName: String,
    cards: List<CardEntity>,
    onBack: () -> Unit,
) {
    var currentIndex by rememberSaveable { mutableIntStateOf(0) }
    var isFlipped by rememberSaveable(currentIndex) { mutableStateOf(false) }
    var hintSteps by rememberSaveable(currentIndex) { mutableIntStateOf(0) }
    var typedAnswer by rememberSaveable(currentIndex) { mutableStateOf("") }
    var checkResult by rememberSaveable(currentIndex) { mutableStateOf<Boolean?>(null) }

    val safeIndex = if (cards.isEmpty()) 0 else currentIndex.coerceIn(0, cards.lastIndex)
    val currentCard = cards.getOrNull(safeIndex)

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("study_back_button"),
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
                            text = deckName,
                            style = MaterialTheme.typography.titleLarge.copy(
                                textDirection = TextDirection.Content,
                            ),
                        )
                        if (cards.isNotEmpty()) {
                            Text(
                                text = "Card ${safeIndex + 1} of ${cards.size}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            if (currentCard == null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "No cards available to study.",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onBack) { Text("Back to Deck") }
                }
            } else {
                val hintText = remember(currentCard.back, hintSteps) {
                    if (hintSteps <= 0) null else GraphemeUtils.progressiveHint(currentCard.back, hintSteps)
                }

                Column(
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    LinearProgressIndicator(
                        progress = { (safeIndex + 1).toFloat() / cards.size.toFloat() },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .testTag("study_flashcard_surface"),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 28.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Text(
                                text = "FRONT",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )

                            Text(
                                text = currentCard.front,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontSize = 28.sp,
                                    lineHeight = 46.sp,
                                    textDirection = TextDirection.Content,
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("study_front_text"),
                            )

                            if (hintText != null && !isFlipped) {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = MaterialTheme.shapes.small,
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Text(
                                            text = "HINT ($hintSteps grapheme${if (hintSteps == 1) "" else "s"})",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        )
                                        Text(
                                            text = hintText,
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontSize = 20.sp,
                                                lineHeight = 34.sp,
                                                textDirection = TextDirection.Content,
                                            ),
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.testTag("study_hint_text"),
                                        )
                                    }
                                }
                            }

                            if (isFlipped) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                                Text(
                                    text = "BACK",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                )
                                Text(
                                    text = currentCard.back,
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontSize = 24.sp,
                                        lineHeight = 40.sp,
                                        textDirection = TextDirection.Content,
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("study_back_text"),
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = typedAnswer,
                        onValueChange = {
                            typedAnswer = it
                            checkResult = null
                        },
                        label = { Text("Type your answer (optional)") },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            textDirection = TextDirection.Content,
                        ),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            autoCorrectEnabled = false,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("study_answer_input"),
                    )

                    checkResult?.let { matched ->
                        Surface(
                            color = if (matched) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.errorContainer
                            },
                            shape = MaterialTheme.shapes.small,
                        ) {
                            Text(
                                text = if (matched) {
                                    "Correct!"
                                } else {
                                    "Expected: ${currentCard.back}"
                                },
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    textDirection = TextDirection.Content,
                                ),
                                color = if (matched) {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onErrorContainer
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                                    .testTag("study_check_result"),
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedButton(
                            onClick = { hintSteps++ },
                            enabled = !isFlipped && hintSteps < GraphemeUtils.graphemeCount(currentCard.back),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("hint_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Lightbulb,
                                contentDescription = null,
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Hint")
                        }

                        OutlinedButton(
                            onClick = {
                                checkResult = TextNormalization.computeFrontKey(typedAnswer) ==
                                    TextNormalization.computeFrontKey(currentCard.back)
                                isFlipped = true
                            },
                            enabled = typedAnswer.isNotBlank(),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("check_answer_button"),
                        ) {
                            Text("Check")
                        }

                        Button(
                            onClick = { isFlipped = !isFlipped },
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("flip_card_button"),
                        ) {
                            Text(if (isFlipped) "Hide" else "Flip")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedButton(
                            onClick = { if (safeIndex > 0) currentIndex = safeIndex - 1 },
                            enabled = safeIndex > 0,
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("prev_card_button"),
                        ) {
                            Text("Previous")
                        }

                        Button(
                            onClick = {
                                if (safeIndex < cards.lastIndex) {
                                    currentIndex = safeIndex + 1
                                } else {
                                    onBack()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("next_card_button"),
                        ) {
                            Text(if (safeIndex < cards.lastIndex) "Next" else "Done")
                        }
                    }
                }
            }
        }
    }
}
