package com.stolyarchuk.colorguessinggame.ui

import androidx.lifecycle.ViewModel
import com.stolyarchuk.colorguessinggame.logic.GameLogic
import com.stolyarchuk.colorguessinggame.model.GameColor
import com.stolyarchuk.colorguessinggame.model.GuessRow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class GameViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(GameState())
    val uiState: StateFlow<GameState> = _uiState.asStateFlow()

    init {
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
                status = GameStatus.PLAYING
            )
        }
    }

    fun updateConfig(attempts: Int, codeLength: Int) {
        _uiState.update { it.copy(config = GameConfig(attempts, codeLength)) }
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

            state.copy(
                guessRows = updatedRows,
                currentGuessIndex = if (newStatus == GameStatus.PLAYING) state.currentGuessIndex + 1 else state.currentGuessIndex,
                selectedPegIndex = if (newStatus == GameStatus.PLAYING) 0 else state.selectedPegIndex,
                status = newStatus
            )
        }
    }
}
