package com.swipeclean.app.ui.permissions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.shouldShowRationale
import com.swipeclean.app.domain.model.MediaAccessLevel
import com.swipeclean.app.domain.permission.MediaPermissions

/**
 * Estado de permisos de medios listo para consumir desde una pantalla. Concentra
 * aquí toda la plomería de `Activity` (solicitud, rationale, vuelta de ajustes)
 * para que ViewModels y composables de contenido no la toquen.
 *
 * @property level nivel de acceso vigente ([MediaAccessLevel]).
 * @property needsSettings el permiso fue denegado de forma permanente: pedirlo de
 *   nuevo no muestra diálogo, hay que ir a los ajustes de la app.
 * @property onRequest lanza la solicitud del sistema. En API 34+ con acceso
 *   parcial, vuelve a abrir el selector para ampliar la selección.
 * @property onOpenSettings abre los ajustes de la app.
 */
data class MediaPermissionUi(
    val level: MediaAccessLevel,
    val needsSettings: Boolean,
    val onRequest: () -> Unit,
    val onOpenSettings: () -> Unit,
)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun rememberMediaPermissionUi(includeVideos: Boolean = true): MediaPermissionUi {
    val context = LocalContext.current
    val permissions = remember(includeVideos) { MediaPermissions.required(includeVideos).toList() }

    val state = rememberMultiplePermissionsState(permissions)

    // Solo tiene sentido hablar de "denegado permanentemente" tras haber pedido.
    var hasRequested by rememberSaveable { mutableStateOf(false) }

    // Se relee el nivel real del sistema cada vez que cambia algún permiso concedido.
    val grantSignature = state.permissions.map { it.permission to it.status.isGranted }
    val level = remember(grantSignature) { MediaPermissions.accessLevel(context) }

    val needsSettings = hasRequested &&
        level == MediaAccessLevel.NONE &&
        !state.shouldShowRationale

    return MediaPermissionUi(
        level = level,
        needsSettings = needsSettings,
        onRequest = {
            hasRequested = true
            state.launchMultiplePermissionRequest()
        },
        onOpenSettings = { context.openAppSettings() },
    )
}
