package com.stolyarchuk.colorguessinggame.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stolyarchuk.colorguessinggame.model.Feedback
import com.stolyarchuk.colorguessinggame.model.GameColor
import com.stolyarchuk.colorguessinggame.model.GuessRow
import com.stolyarchuk.colorguessinggame.ui.theme.ColorGuessingGameTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    modifier: Modifier = Modifier,
    viewModel: GameViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    // Scroll to current row when it changes
    LaunchedEffect(uiState.currentGuessIndex) {
        listState.animateScrollToItem(uiState.currentGuessIndex)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Color Guessing Game", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.startNewGame() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Restart Game")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            BottomControls(
                status = uiState.status,
                onSubmit = { viewModel.submitGuess() },
                onRestart = { viewModel.startNewGame() },
                secretCode = uiState.secretCode
            )
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(uiState.guessRows) { index, row ->
                    GuessRowItem(
                        row = row,
                        isCurrent = index == uiState.currentGuessIndex && uiState.status == GameStatus.PLAYING,
                        selectedPegIndex = uiState.selectedPegIndex,
                        onPegClick = { pegIndex -> viewModel.setSelectedPeg(pegIndex) },
                        rowIndex = index
                    )
                }
            }
            
            AnimatedVisibility(
                visible = uiState.status == GameStatus.PLAYING,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                ColorPalette(
                    onColorSelected = { color -> viewModel.selectColor(color) }
                )
            }
        }
    }

    // Win/Loss Dialogs
    if (uiState.status != GameStatus.PLAYING) {
        GameResultDialog(
            status = uiState.status,
            secretCode = uiState.secretCode,
            onRestart = { viewModel.startNewGame() }
        )
    }
}

@Composable
fun GuessRowItem(
    row: GuessRow,
    isCurrent: Boolean,
    selectedPegIndex: Int,
    onPegClick: (Int) -> Unit,
    rowIndex: Int
) {
    Surface(
        tonalElevation = if (isCurrent) 8.dp else 1.dp,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
        color = if (isCurrent) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
        border = if (isCurrent) borderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${rowIndex + 1}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.width(24.dp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.colors.forEachIndexed { index, color ->
                    Peg(
                        color = color,
                        onClick = { onPegClick(index) },
                        isInteractive = isCurrent,
                        isSelected = isCurrent && index == selectedPegIndex
                    )
                }
            }

            FeedbackDisplay(row.feedback)
        }
    }
}

@Composable
fun borderStroke(width: androidx.compose.ui.unit.Dp, color: Color) = androidx.compose.foundation.BorderStroke(width, color)

@Composable
fun Peg(
    color: GameColor?,
    onClick: () -> Unit = {},
    isInteractive: Boolean = false,
    isSelected: Boolean = false
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color?.color ?: MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    color == null -> MaterialTheme.colorScheme.outlineVariant
                    else -> Color.Transparent
                },
                shape = CircleShape
            )
            .then(if (isInteractive) Modifier.clickable { onClick() } else Modifier)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FeedbackDisplay(feedback: Feedback?) {
    Box(
        modifier = Modifier.width(44.dp),
        contentAlignment = Alignment.Center
    ) {
        if (feedback != null) {
            androidx.compose.foundation.layout.FlowRow(
                maxItemsInEachRow = 3,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(feedback.blackPegs) {
                    FeedbackDot(Color.Black)
                }
                repeat(feedback.whitePegs) {
                    FeedbackDot(Color.White)
                }
            }
        }
    }
}

@Composable
fun FeedbackDot(color: Color) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(color)
            .border(1.dp, Color.Gray, CircleShape)
    )
}

@Composable
fun ColorPalette(onColorSelected: (GameColor) -> Unit) {
    Surface(
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            GameColor.values().forEach { gameColor ->
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(gameColor.color)
                        .clickable { onColorSelected(gameColor) }
                        .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                )
            }
        }
    }
}

@Composable
fun BottomControls(
    status: GameStatus,
    onSubmit: () -> Unit,
    onRestart: () -> Unit,
    secretCode: List<GameColor>
) {
    Surface(
        tonalElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (status == GameStatus.PLAYING) {
                Button(
                    onClick = onSubmit,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("Submit Guess")
                }
            } else {
                Text(
                    text = if (status == GameStatus.WON) "Victory!" else "Game Over",
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (status == GameStatus.WON) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                )
                Button(onClick = onRestart, modifier = Modifier.fillMaxWidth()) {
                    Text("Restart Game")
                }
            }
        }
    }
}

@Composable
fun GameResultDialog(
    status: GameStatus,
    secretCode: List<GameColor>,
    onRestart: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { },
        title = {
            Text(
                text = if (status == GameStatus.WON) "Congratulations!" else "Better luck next time!",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("The secret code was:")
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    secretCode.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color.color)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onRestart) {
                Text("Play Again")
            }
        }
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun GameScreenPreview() {
    ColorGuessingGameTheme {
        GameScreen()
    }
}
