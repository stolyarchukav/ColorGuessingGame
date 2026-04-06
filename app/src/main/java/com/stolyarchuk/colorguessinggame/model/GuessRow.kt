package com.stolyarchuk.colorguessinggame.model

data class GuessRow(
    val colors: List<GameColor?> = List(5) { null },
    val feedback: Feedback? = null,
    val isSubmitted: Boolean = false
)
