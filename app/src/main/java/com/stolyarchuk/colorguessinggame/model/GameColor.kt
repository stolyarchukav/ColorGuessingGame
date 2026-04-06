package com.stolyarchuk.colorguessinggame.model

import androidx.compose.ui.graphics.Color

enum class GameColor(val color: Color) {
    RED(Color(0xFFFF5252)),
    GREEN(Color(0xFF4CAF50)),
    BLUE(Color(0xFF2196F3)),
    YELLOW(Color(0xFFFFEB3B)),
    PURPLE(Color(0xFF9C27B0));

    companion object {
        fun fromIndex(index: Int): GameColor = values()[index % values().size]
    }
}
