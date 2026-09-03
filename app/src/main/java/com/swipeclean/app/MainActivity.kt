package com.swipeclean.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.swipeclean.app.ui.navigation.SwipeCleanNavHost
import com.swipeclean.app.ui.theme.SwipeCleanTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SwipeCleanRoot()
        }
    }
}

// Sin @Preview: el root depende de Hilt (vía HomeViewModel dentro del NavHost),
// que no está disponible en el renderer de previews de Compose. Las pantallas
// reales (HomeScreen y sus componentes) sí tienen su preview.
@Composable
private fun SwipeCleanRoot() {
    SwipeCleanTheme {
        SwipeCleanNavHost(modifier = Modifier.fillMaxSize())
    }
}
