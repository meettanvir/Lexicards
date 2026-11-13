package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CardEntity

private data class McqQuestion(
    val card: CardEntity,
    val options: List<McqOption>,
)

private data class McqOption(
    val text: String,
    val isCorrect: Boolean,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun McqQuizScreen(
    deckName: String,
    cards: List<CardEntity>,
    onBack: () -> Unit,
) {
    var sessionSeed by remember { mutableIntStateOf(0) }

    val questions = remember(cards, sessionSeed) {
        if (cards.size < 4) {
            emptyList()
        } else {
            val shuffledCards = cards.shuffled()
            shuffledCards.map { target ->
                val others = cards.filter { it.id != target.id }.shuffled()
                val distinctDistractors = others
                    .filter { it.back != target.back }
                    .distinctBy { it.back }
                    .take(3)
                val distractors = if (distinctDistractors.size == 3) {
                    distinctDistractors
                } else {
                    (distinctDistractors + others.filter { it !in distinctDistractors }).take(3)
                }
                val options = (listOf(McqOption(target.back, isCorrect = true)) +
                    distractors.map { McqOption(it.back, isCorrect = false) }).shuffled()
                McqQuestion(card = target, options = options)
            }
        }
    }

    var currentIndex by remember(sessionSeed) { mutableIntStateOf(0) }
    var selectedOptionIndex by remember(sessionSeed, currentIndex) { mutableStateOf<Int?>(null) }
    var score by remember(sessionSeed) { mutableIntStateOf(0) }
    var isCompleted by remember(sessionSeed) { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("mcq_back_button"),
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
                            text = "MCQ • $deckName",
                            style = MaterialTheme.typography.titleLarge.copy(
                                textDirection = TextDirection.Content,
                            ),
                        )
                        if (questions.isNotEmpty() && !isCompleted) {
                            Text(
                                text = "Question ${currentIndex + 1} of ${questions.size} • Score $score",
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
            when {
                cards.size < 4 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = "MCQ Quiz is disabled",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Add at least 4 cards to this deck (${cards.size}/4) to start a multiple-choice quiz.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(20.dp))
                        Button(onClick = onBack) { Text("Back to Deck") }
                    }
                }
                isCompleted -> {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 600.dp)
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = "Quiz Complete",
                            style = MaterialTheme.typography.headlineMedium,
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "$score / ${questions.size} correct",
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = { sessionSeed++ },
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize(),
                        ) {
                            Text("Restart Quiz")
                        }
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = onBack,
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize(),
                        ) {
                            Text("Back to Deck")
                        }
                    }
                }
                else -> {
                    val currentQuestion = questions[currentIndex]
                    Column(
                        modifier = Modifier
                            .widthIn(max = 600.dp)
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        LinearProgressIndicator(
                            progress = { (currentIndex + 1).toFloat() / questions.size.toFloat() },
                            modifier = Modifier.fillMaxWidth(),
                        )

                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight(),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                            ),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = "CHOOSE THE MATCHING BACK",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    text = currentQuestion.card.front,
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontSize = 26.sp,
                                        lineHeight = 44.sp,
                                        textDirection = TextDirection.Content,
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("mcq_question_front"),
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            currentQuestion.options.forEachIndexed { idx, option ->
                                val answered = selectedOptionIndex != null
                                val isSelected = selectedOptionIndex == idx
                                val containerColor = when {
                                    !answered -> MaterialTheme.colorScheme.surface
                                    option.isCorrect -> MaterialTheme.colorScheme.secondaryContainer
                                    isSelected && !option.isCorrect -> MaterialTheme.colorScheme.errorContainer
                                    else -> MaterialTheme.colorScheme.surface
                                }
                                val borderColor = when {
                                    !answered -> MaterialTheme.colorScheme.outline
                                    option.isCorrect -> MaterialTheme.colorScheme.secondary
                                    isSelected && !option.isCorrect -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                }

                                Surface(
                                    onClick = {
                                        if (selectedOptionIndex == null) {
                                            selectedOptionIndex = idx
                                            if (option.isCorrect) score++
                                        }
                                    },
                                    enabled = !answered,
                                    shape = MaterialTheme.shapes.medium,
                                    color = containerColor,
                                    border = BorderStroke(1.5.dp, borderColor),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .minimumInteractiveComponentSize()
                                        .testTag("mcq_option_$idx"),
                                ) {
                                    Text(
                                        text = option.text,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontSize = 18.sp,
                                            lineHeight = 30.sp,
                                            textDirection = TextDirection.Content,
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 14.dp),
                                    )
                                }
                            }
                        }

                        if (selectedOptionIndex != null) {
                            Button(
                                onClick = {
                                    if (currentIndex < questions.lastIndex) {
                                        currentIndex++
                                    } else {
                                        isCompleted = true
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .testTag("mcq_next_button"),
                            ) {
                                Text(if (currentIndex < questions.lastIndex) "Next Question" else "Finish Quiz")
                            }
                        }
                    }
                }
            }
        }
    }
}
