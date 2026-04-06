package com.stolyarchuk.colorguessinggame.ui

import com.stolyarchuk.colorguessinggame.model.GameColor
import com.stolyarchuk.colorguessinggame.model.GuessRow

enum class GameStatus {
    PLAYING, WON, LOST
}

data class GameState(
    val guessRows: List<GuessRow> = List(12) { GuessRow() },
    val currentGuessIndex: Int = 0,
    val selectedPegIndex: Int = 0,
    val secretCode: List<GameColor> = emptyList(),
    val status: GameStatus = GameStatus.PLAYING
)
