package com.stolyarchuk.colorguessinggame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stolyarchuk.colorguessinggame.logic.StatisticsRepository
import com.stolyarchuk.colorguessinggame.ui.GameScreen
import com.stolyarchuk.colorguessinggame.ui.GameViewModel
import com.stolyarchuk.colorguessinggame.ui.theme.ColorGuessingGameTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = StatisticsRepository(applicationContext)
        enableEdgeToEdge()
        setContent {
            ColorGuessingGameTheme {
                val viewModel: GameViewModel = viewModel(
                    factory = GameViewModel.Factory(repository)
                )
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    GameScreen(
                        modifier = Modifier.padding(innerPadding),
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}
