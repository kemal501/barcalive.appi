package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.MainAppScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Edge-to-edge system navigation rendering support
        enableEdgeToEdge()
        
        setContent {
            MyApplicationTheme {
                val viewModel: BarcaViewModel = viewModel()
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}
