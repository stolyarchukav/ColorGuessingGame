package com.stolyarchuk.colorguessinggame.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stolyarchuk.colorguessinggame.logic.GameLogic
import com.stolyarchuk.colorguessinggame.logic.StatisticsRepository
import com.stolyarchuk.colorguessinggame.model.GameColor
import com.stolyarchuk.colorguessinggame.model.GuessRow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class GameViewModel(private val repository: StatisticsRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(GameState())
    val uiState: StateFlow<GameState> = _uiState.asStateFlow()

    private val _statistics = MutableStateFlow(GameStatistics())
    val statistics: StateFlow<GameStatistics> = _statistics.asStateFlow()

    val lastName: StateFlow<String> = repository.lastNameFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    private val _timer = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1000)
        }
    }

    val elapsedTime: StateFlow<Long> = combine(_uiState, _timer) { state, now ->
        if (state.status == GameStatus.PLAYING) {
            (now - state.startTime).coerceAtLeast(0) / 1000
        } else {
            (state.endTime - state.startTime).coerceAtLeast(0) / 1000
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    init {
        viewModelScope.launch {
            repository.statsFlow.collect { stats ->
                _statistics.value = stats
            }
        }
        startNewGame()
    }

    fun startNewGame() {
        _uiState.update { state ->
            val config = state.config
            state.copy(
                guessRows = List(config.attempts) { GuessRow(colors = List(config.codeLength) { null }) },
                currentGuessIndex = 0,
                selectedPegIndex = 0,
                secretCode = GameLogic.generateSecretCode(config.codeLength),
                status = GameStatus.PLAYING,
                startTime = System.currentTimeMillis(),
                endTime = 0,
                isNewRecord = false,
                pendingRecord = null
            )
        }
    }

    fun savePendingRecord(name: String) {
        val pending = _uiState.value.pendingRecord ?: return
        viewModelScope.launch {
            repository.saveRecord(name, pending.timeInSeconds, pending.attempts, pending.codeLength)
            _uiState.update { it.copy(pendingRecord = null, isNewRecord = false) }
        }
    }

    fun updateConfig(attempts: Int, codeLength: Int, showTimer: Boolean) {
        _uiState.update { it.copy(config = GameConfig(attempts, codeLength, showTimer)) }
        startNewGame()
    }

    fun setSelectedPeg(index: Int) {
        if (_uiState.value.status != GameStatus.PLAYING) return
        _uiState.update { it.copy(selectedPegIndex = index) }
    }

    fun selectColor(color: GameColor) {
        val currentState = _uiState.value
        if (currentState.status != GameStatus.PLAYING) return

        _uiState.update { state ->
            val updatedRows = state.guessRows.toMutableList()
            val currentRow = updatedRows[state.currentGuessIndex]
            val updatedColors = currentRow.colors.toMutableList()
            
            updatedColors[state.selectedPegIndex] = color
            updatedRows[state.currentGuessIndex] = currentRow.copy(colors = updatedColors)
            
            // Auto-advance to next peg if available
            val nextPegIndex = (state.selectedPegIndex + 1) % state.config.codeLength
            
            state.copy(
                guessRows = updatedRows,
                selectedPegIndex = nextPegIndex
            )
        }
    }

    fun submitGuess() {
        val currentState = _uiState.value
        if (currentState.status != GameStatus.PLAYING) return

        val currentRow = currentState.guessRows[currentState.currentGuessIndex]
        
        // Ensure all pegs are filled
        if (currentRow.colors.any { it == null }) return

        val feedback = GameLogic.evaluateGuess(currentState.secretCode, currentRow.colors)
        
        _uiState.update { state ->
            val updatedRows = state.guessRows.toMutableList()
            updatedRows[state.currentGuessIndex] = currentRow.copy(
                feedback = feedback,
                isSubmitted = true
            )

            val won = feedback.blackPegs == state.config.codeLength
            val lost = !won && state.currentGuessIndex == state.config.attempts - 1
            
            val newStatus = when {
                won -> GameStatus.WON
                lost -> GameStatus.LOST
                else -> GameStatus.PLAYING
            }

            val endTime = if (newStatus != GameStatus.PLAYING) System.currentTimeMillis() else 0L

            var isRecord = false
            var pending: PendingRecord? = null
            if (won) {
                val timeSeconds = (endTime - state.startTime) / 1000
                val attempts = state.currentGuessIndex + 1
                if (repository.isNewRecord(timeSeconds, attempts, state.config.codeLength, _statistics.value)) {
                    isRecord = true
                    pending = PendingRecord(timeSeconds, attempts, state.config.codeLength)
                }
            }

            state.copy(
                guessRows = updatedRows,
                currentGuessIndex = if (newStatus == GameStatus.PLAYING) state.currentGuessIndex + 1 else state.currentGuessIndex,
                selectedPegIndex = if (newStatus == GameStatus.PLAYING) 0 else state.selectedPegIndex,
                status = newStatus,
                endTime = endTime,
                isNewRecord = isRecord,
                pendingRecord = pending
            )
        }
    }

    fun giveUp() {
        if (_uiState.value.status != GameStatus.PLAYING) return
        _uiState.update { it.copy(status = GameStatus.LOST, endTime = System.currentTimeMillis()) }
    }

    class Factory(private val repository: StatisticsRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GameViewModel(repository) as T
        }
    }
}
