package com.stolyarchuk.colorguessinggame.model

data class GuessRow(
    val colors: List<GameColor?> = emptyList(),
    val feedback: Feedback? = null,
    val isSubmitted: Boolean = false
)
