package com.stolyarchuk.colorguessinggame

import com.stolyarchuk.colorguessinggame.logic.GameLogic
import com.stolyarchuk.colorguessinggame.model.GameColor
import org.junit.Assert.assertEquals
import org.junit.Test

class GameLogicTest {

    @Test
    fun `test all correct`() {
        val secret = listOf(GameColor.RED, GameColor.GREEN, GameColor.BLUE, GameColor.YELLOW, GameColor.PURPLE)
        val guess = listOf(GameColor.RED, GameColor.GREEN, GameColor.BLUE, GameColor.YELLOW, GameColor.PURPLE)
        val feedback = GameLogic.evaluateGuess(secret, guess)
        assertEquals(5, feedback.blackPegs)
        assertEquals(0, feedback.whitePegs)
    }

    @Test
    fun `test all wrong`() {
        val secret = listOf(GameColor.RED, GameColor.RED, GameColor.RED, GameColor.RED, GameColor.RED)
        val guess = listOf(GameColor.GREEN, GameColor.GREEN, GameColor.GREEN, GameColor.GREEN, GameColor.GREEN)
        val feedback = GameLogic.evaluateGuess(secret, guess)
        assertEquals(0, feedback.blackPegs)
        assertEquals(0, feedback.whitePegs)
    }

    @Test
    fun `test white pegs`() {
        val secret = listOf(GameColor.RED, GameColor.GREEN, GameColor.BLUE, GameColor.YELLOW, GameColor.PURPLE)
        val guess = listOf(GameColor.PURPLE, GameColor.RED, GameColor.GREEN, GameColor.BLUE, GameColor.YELLOW)
        val feedback = GameLogic.evaluateGuess(secret, guess)
        assertEquals(0, feedback.blackPegs)
        assertEquals(5, feedback.whitePegs)
    }

    @Test
    fun `test mix of black and white`() {
        val secret = listOf(GameColor.RED, GameColor.RED, GameColor.BLUE, GameColor.BLUE, GameColor.GREEN)
        val guess = listOf(GameColor.RED, GameColor.BLUE, GameColor.RED, GameColor.BLUE, GameColor.YELLOW)
        // RED[0] -> Black
        // BLUE[1] -> White (matches BLUE[2] or [3])
        // RED[2] -> White (matches RED[1])
        // BLUE[3] -> Black
        // YELLOW[4] -> None
        // Expected: 2 Black, 2 White
        val feedback = GameLogic.evaluateGuess(secret, guess)
        assertEquals(2, feedback.blackPegs)
        assertEquals(2, feedback.whitePegs)
    }

    @Test
    fun `test duplicate colors correctly handled`() {
        val secret = listOf(GameColor.RED, GameColor.GREEN, GameColor.RED, GameColor.GREEN, GameColor.RED)
        val guess = listOf(GameColor.GREEN, GameColor.RED, GameColor.GREEN, GameColor.RED, GameColor.GREEN)
        // None match position.
        // Secret has 3 RED, 2 GREEN.
        // Guess has 2 RED, 3 GREEN.
        // 2 RED from guess can match 2 RED in secret -> 2 White.
        // 2 GREEN from guess can match 2 GREEN in secret -> 2 White.
        // Total 4 White.
        val feedback = GameLogic.evaluateGuess(secret, guess)
        assertEquals(0, feedback.blackPegs)
        assertEquals(4, feedback.whitePegs)
    }
}
