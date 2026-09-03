package com.swipeclean.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.swipeclean.app.domain.model.ThemeMode
import com.swipeclean.app.ui.app.AppViewModel
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

// Sin @Preview: el root depende de Hilt (AppViewModel y, más abajo, HomeViewModel),
// que no está disponible en el renderer de previews. Las pantallas reales sí lo tienen.
@Composable
private fun SwipeCleanRoot(appViewModel: AppViewModel = hiltViewModel()) {
    val themeMode by appViewModel.themeMode.collectAsStateWithLifecycle()
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    SwipeCleanTheme(darkTheme = darkTheme) {
        SwipeCleanNavHost(modifier = Modifier.fillMaxSize())
    }
}
