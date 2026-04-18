package com.stolyarchuk.colorguessinggame.logic

import com.stolyarchuk.colorguessinggame.model.Feedback
import com.stolyarchuk.colorguessinggame.model.GameColor

object GameLogic {

    fun generateSecretCode(codeLength: Int): List<GameColor> {
        return List(codeLength) { GameColor.entries.random() }
    }

    fun evaluateGuess(secretCode: List<GameColor>, guess: List<GameColor?>): Feedback {
        val codeLength = secretCode.size
        require(guess.size == codeLength)

        val secretHandled = BooleanArray(codeLength)
        val guessHandled = BooleanArray(codeLength)
        var blackPegs = 0
        var whitePegs = 0

        // Count black pegs
        for (i in 0 until codeLength) {
            if (guess[i] == secretCode[i]) {
                blackPegs++
                secretHandled[i] = true
                guessHandled[i] = true
            }
        }

        // Count white pegs
        for (i in 0 until codeLength) {
            if (!guessHandled[i] && guess[i] != null) {
                for (j in 0 until codeLength) {
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
