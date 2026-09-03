package com.swipeclean.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.swipeclean.app.R
import com.swipeclean.app.domain.model.MediaSortOrder
import com.swipeclean.app.domain.model.MediaTypeFilter
import com.swipeclean.app.ui.home.HomeChip
import com.swipeclean.app.ui.home.HomeScreen
import com.swipeclean.app.ui.home.HomeUiState
import com.swipeclean.app.ui.home.HomeViewModel
import com.swipeclean.app.ui.permissions.PermissionGate
import com.swipeclean.app.ui.permissions.rememberMediaPermissionUi
import com.swipeclean.app.ui.theme.SwipeCleanTheme

/**
 * Grafo de navegación de la app. El punto de entrada es [Onboarding]: si ya hay
 * permiso concedido salta a [Home] de inmediato, sin mostrar nada intermedio.
 *
 * [Swipe]/[Summary]/[Settings] son esqueletos vacíos: su contenido llega en las
 * etapas 6 y 7.
 */
@Composable
fun SwipeCleanNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(navController = navController, startDestination = Onboarding, modifier = modifier) {
        composable<Onboarding> {
            PermissionGate { _, _ ->
                LaunchedEffect(Unit) {
                    navController.navigate(Home) {
                        popUpTo(Onboarding) { inclusive = true }
                    }
                }
            }
        }

        composable<Home> {
            val viewModel: HomeViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val permissionUi = rememberMediaPermissionUi()

            // Los títulos de los chips se resuelven acá (contexto @Composable) para
            // poder usarlos dentro del lambda de clic, que no lo es.
            val screenshotsTitle = stringResource(R.string.home_chip_screenshots)
            val heaviestTitle = stringResource(R.string.home_chip_heaviest)
            val videosTitle = stringResource(R.string.home_chip_videos)

            // Si el permiso se revoca mientras la app está en Home (p. ej. desde
            // ajustes del sistema), se vuelve a pedir en vez de mostrar la Home vacía.
            LaunchedEffect(uiState) {
                if (uiState is HomeUiState.NoPermission) {
                    navController.navigate(Onboarding) {
                        popUpTo(Home) { inclusive = true }
                    }
                }
            }

            HomeScreen(
                uiState = uiState,
                onRefreshPermission = viewModel::refreshPermission,
                onExpandSelection = permissionUi.onRequest,
                onOpenSettings = { navController.navigate(Settings) },
                onOpenBucket = { bucket ->
                    navController.navigate(Swipe(bucketId = bucket.id, titulo = bucket.nombre))
                },
                onOpenChip = { chip ->
                    val route = when (chip) {
                        HomeChip.SCREENSHOTS -> Swipe(
                            mediaType = MediaTypeFilter.IMAGES,
                            screenshotsOnly = true,
                            titulo = screenshotsTitle,
                        )

                        HomeChip.HEAVIEST_PHOTOS -> Swipe(
                            mediaType = MediaTypeFilter.IMAGES,
                            sortOrder = MediaSortOrder.SIZE_DESC,
                            titulo = heaviestTitle,
                        )

                        HomeChip.VIDEOS -> Swipe(mediaType = MediaTypeFilter.VIDEOS, titulo = videosTitle)
                    }
                    navController.navigate(route)
                },
                onOpenMonth = { mes, titulo ->
                    navController.navigate(
                        Swipe(dateFromMillis = mes.startMillis, dateToMillis = mes.endMillis, titulo = titulo),
                    )
                },
                onResetHistory = viewModel::resetHistory,
            )
        }

        composable<Swipe> {
            PlaceholderScreen(titulo = stringResource(R.string.placeholder_swipe))
        }
        composable<Summary> {
            PlaceholderScreen(titulo = stringResource(R.string.placeholder_summary))
        }
        composable<Settings> {
            PlaceholderScreen(titulo = stringResource(R.string.placeholder_settings))
        }
    }
}

/** Relleno visual para rutas cuya pantalla real todavía no existe. */
@Composable
private fun PlaceholderScreen(titulo: String) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = titulo, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PlaceholderScreenPreview() {
    SwipeCleanTheme {
        PlaceholderScreen(titulo = "Swipe")
    }
}
