package com.aryanspatel.moodmatch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.aryanspatel.moodmatch.presentation.navigation.NavGraph
import com.aryanspatel.moodmatch.ui.theme.MoodMatchTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MoodMatchApp()
        }
    }
}

@Composable
fun MoodMatchApp(){
    MoodMatchTheme {
        Box(modifier = Modifier.fillMaxSize()
            .background(
                Brush.linearGradient(listOf(Color(0xFFE8F4F8), Color(0xFFF5E8F8)))
            )
        ) {
            NavGraph()
        }
    }
}
