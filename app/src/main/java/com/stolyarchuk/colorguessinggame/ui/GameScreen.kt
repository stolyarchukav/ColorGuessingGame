package com.stolyarchuk.colorguessinggame.ui

import com.stolyarchuk.colorguessinggame.R

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
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
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
    val stats by viewModel.statistics.collectAsState()
    val listState = rememberLazyListState()
    var showHelpDialog by remember { mutableStateOf(false) }
    var showRestartConfirmDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showStatsDialog by remember { mutableStateOf(false) }

    // Scroll to current row only if it's not fully visible
    LaunchedEffect(uiState.currentGuessIndex) {
        val layoutInfo = listState.layoutInfo
        val visibleItem = layoutInfo.visibleItemsInfo.find { it.index == uiState.currentGuessIndex }
        
        if (visibleItem == null) {
            // Not visible at all, scroll to it
            listState.animateScrollToItem(uiState.currentGuessIndex)
        } else {
            val viewportStart = layoutInfo.viewportStartOffset
            val viewportEnd = layoutInfo.viewportEndOffset
            val itemStart = visibleItem.offset
            val itemEnd = visibleItem.offset + visibleItem.size
            
            // If item is partially hidden at the bottom or top
            if (itemEnd > viewportEnd || itemStart < viewportStart) {
                listState.animateScrollToItem(uiState.currentGuessIndex)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.game_header_hint),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            textAlign = TextAlign.Center
                        )
                    }
                },
                navigationIcon = {
                    Row {
                        IconButton(onClick = { showSettingsDialog = true }) {
                            Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
                        }
                        IconButton(onClick = { showStatsDialog = true }) {
                            Icon(Icons.Default.Leaderboard, contentDescription = stringResource(R.string.statistics))
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(Icons.Default.HelpOutline, contentDescription = stringResource(R.string.help))
                    }
                    IconButton(onClick = { 
                        if (uiState.status == GameStatus.PLAYING) {
                            showRestartConfirmDialog = true 
                        } else {
                            viewModel.startNewGame()
                        }
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.restart_game))
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
                    val isCurrent = index == uiState.currentGuessIndex && uiState.status == GameStatus.PLAYING
                    val isRecent = index in (uiState.currentGuessIndex - 2) until uiState.currentGuessIndex
                    GuessRowItem(
                        row = row,
                        isCurrent = isCurrent,
                        isRecent = isRecent,
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
        if (uiState.isNewRecord && uiState.pendingRecord != null) {
            NewRecordDialog(
                onSave = { name -> viewModel.savePendingRecord(name) }
            )
        } else {
            GameResultDialog(
                status = uiState.status,
                secretCode = uiState.secretCode,
                onRestart = { viewModel.startNewGame() }
            )
        }
    }

    if (showStatsDialog) {
        StatisticsDialog(
            stats = stats,
            onDismiss = { showStatsDialog = false }
        )
    }

    if (showSettingsDialog) {
        SettingsDialog(
            config = uiState.config,
            onSave = { attempts, codeLength ->
                viewModel.updateConfig(attempts, codeLength)
                showSettingsDialog = false
            },
            onDismiss = { showSettingsDialog = false }
        )
    }

    if (showHelpDialog) {
        GameHelpDialog(onDismiss = { showHelpDialog = false })
    }

    if (showRestartConfirmDialog) {
        RestartConfirmDialog(
            onConfirm = {
                showRestartConfirmDialog = false
                viewModel.startNewGame()
            },
            onDismiss = { showRestartConfirmDialog = false }
        )
    }
}

@Composable
fun RestartConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.confirm_restart_title)) },
        text = { Text(stringResource(R.string.confirm_restart_message)) },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun SettingsDialog(
    config: GameConfig,
    onSave: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var attempts by remember { mutableStateOf(config.attempts.toFloat()) }
    var codeLength by remember { mutableStateOf(config.codeLength.toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column {
                    Text("${stringResource(R.string.attempts_count)} ${attempts.toInt()}")
                    Slider(
                        value = attempts,
                        onValueChange = { attempts = it },
                        valueRange = 5f..20f,
                        steps = 14
                    )
                }
                Column {
                    Text("${stringResource(R.string.code_length)} ${codeLength.toInt()}")
                    Slider(
                        value = codeLength,
                        onValueChange = { codeLength = it },
                        valueRange = 3f..6f,
                        steps = 2
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(attempts.toInt(), codeLength.toInt()) }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun GameHelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.game_rules_title),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Text(
                text = stringResource(R.string.game_rules_text),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    )
}

@Composable
fun GuessRowItem(
    row: GuessRow,
    isCurrent: Boolean,
    isRecent: Boolean,
    selectedPegIndex: Int,
    onPegClick: (Int) -> Unit,
    rowIndex: Int
) {
    val isHighlighted = isCurrent || isRecent
    Surface(
        tonalElevation = if (isHighlighted) 8.dp else 1.dp,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
        color = if (isHighlighted) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
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
            GameColor.entries.forEach { gameColor ->
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
                    Text(stringResource(R.string.submit_guess))
                }
            } else {
                Text(
                    text = if (status == GameStatus.WON) stringResource(R.string.victory) else stringResource(R.string.game_over),
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (status == GameStatus.WON) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                )
                Button(onClick = onRestart, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.restart_game))
                }
            }
        }
    }
}

@Composable
fun NewRecordDialog(onSave: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { },
        title = { Text(stringResource(R.string.new_record)) },
        text = {
            Column {
                Text(stringResource(R.string.enter_name))
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text(stringResource(R.string.name_hint)) },
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onSave(name) },
                enabled = name.isNotBlank()
            ) {
                Text(stringResource(R.string.save))
            }
        }
    )
}

@Composable
fun StatisticsDialog(stats: GameStatistics, onDismiss: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(stringResource(R.string.time_records), stringResource(R.string.attempts_records))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.statistics)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().height(400.dp)) {
                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                
                val currentRecords = if (selectedTab == 0) stats.timeRecords else stats.attemptRecords
                
                LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 16.dp)) {
                    // Group by code length
                    (3..6).forEach { codeLength ->
                        val records = currentRecords[codeLength] ?: emptyList()
                        item {
                            Text(
                                text = "${stringResource(R.string.code_length)} $codeLength",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (records.isEmpty()) {
                            item {
                                Text(
                                    stringResource(R.string.no_records),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                                )
                            }
                        } else {
                            itemsIndexed(records) { index, record ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("${index + 1}. ${record.name}", style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        if (selectedTab == 0) "${record.value}${stringResource(R.string.sec_suffix)}" 
                                        else "${record.value} ${stringResource(R.string.attempts).lowercase()}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    )
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
                text = if (status == GameStatus.WON) stringResource(R.string.congratulations) else stringResource(R.string.better_luck),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.secret_code_was))
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
                Text(stringResource(R.string.play_again))
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
