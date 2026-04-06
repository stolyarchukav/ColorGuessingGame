package com.stolyarchuk.colorguessinggame.logic

import com.stolyarchuk.colorguessinggame.model.Feedback
import com.stolyarchuk.colorguessinggame.model.GameColor

object GameLogic {
    private const val CODE_LENGTH = 5

    fun generateSecretCode(): List<GameColor> {
        return List(CODE_LENGTH) { GameColor.values().random() }
    }

    fun evaluateGuess(secretCode: List<GameColor>, guess: List<GameColor?>): Feedback {
        require(secretCode.size == CODE_LENGTH)
        require(guess.size == CODE_LENGTH)

        val secretHandled = BooleanArray(CODE_LENGTH)
        val guessHandled = BooleanArray(CODE_LENGTH)
        var blackPegs = 0
        var whitePegs = 0

        // Count black pegs
        for (i in 0 until CODE_LENGTH) {
            if (guess[i] == secretCode[i]) {
                blackPegs++
                secretHandled[i] = true
                guessHandled[i] = true
            }
        }

        // Count white pegs
        for (i in 0 until CODE_LENGTH) {
            if (!guessHandled[i] && guess[i] != null) {
                for (j in 0 until CODE_LENGTH) {
                    if (!secretHandled[j] && guess[i] == secretCode[j]) {
                        whitePegs++
                        secretHandled[j] = true
                        break
                    }
                }
            }
        }

        return Feedback(blackPegs, whitePegs)
    }
}
