package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.data.viewmodel.MafiaViewModel
import com.example.ui.screens.MainGameScreen

class MainActivity : ComponentActivity() {
    private val viewModel: MafiaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFF0F0F16) // Sleek luxury back canvas
            ) {
                MainGameScreen(viewModel = viewModel)
            }
        }
    }
}
