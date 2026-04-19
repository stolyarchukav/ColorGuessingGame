package com.stolyarchuk.colorguessinggame.ui

import com.stolyarchuk.colorguessinggame.model.GameColor
import com.stolyarchuk.colorguessinggame.model.GuessRow
import java.io.Serializable

enum class GameStatus {
    PLAYING, WON, LOST
}

data class GameState(
    val guessRows: List<GuessRow> = emptyList(),
    val currentGuessIndex: Int = 0,
    val selectedPegIndex: Int = 0,
    val secretCode: List<GameColor> = emptyList(),
    val status: GameStatus = GameStatus.PLAYING,
    val config: GameConfig = GameConfig(),
    val startTime: Long = 0,
    val endTime: Long = 0,
    val isNewRecord: Boolean = false,
    val pendingRecord: PendingRecord? = null
)

data class PendingRecord(
    val timeInSeconds: Long,
    val attempts: Int,
    val codeLength: Int
)

data class GameConfig(
    val attempts: Int = 12,
    val codeLength: Int = 5,
    val showTimer: Boolean = false
)

data class RecordEntry(
    val name: String,
    val value: Long, // seconds or attempts
    val timestamp: Long = System.currentTimeMillis()
) : Serializable

data class GameStatistics(
    val timeRecords: Map<Int, List<RecordEntry>> = emptyMap(), // codeLength -> top 10
    val attemptRecords: Map<Int, List<RecordEntry>> = emptyMap() // codeLength -> top 10
)
