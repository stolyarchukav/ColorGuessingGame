package com.stolyarchuk.colorguessinggame.ui

import com.stolyarchuk.colorguessinggame.model.GameColor
import com.stolyarchuk.colorguessinggame.model.GuessRow

enum class GameStatus {
    PLAYING, WON, LOST
}

data class GameState(
    val guessRows: List<GuessRow> = emptyList(),
    val currentGuessIndex: Int = 0,
    val selectedPegIndex: Int = 0,
    val secretCode: List<GameColor> = emptyList(),
    val status: GameStatus = GameStatus.PLAYING,
    val config: GameConfig = GameConfig()
)

data class GameConfig(
    val attempts: Int = 12,
    val codeLength: Int = 5
)
