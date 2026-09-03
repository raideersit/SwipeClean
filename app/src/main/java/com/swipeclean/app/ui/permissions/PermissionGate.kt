package com.swipeclean.app.ui.permissions

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.swipeclean.app.domain.model.MediaAccessLevel

/**
 * Envuelve el contenido que necesita acceso a la galería:
 *
 * - Sin permiso → muestra [PermissionOnboardingScreen].
 * - Acceso parcial o total → muestra [content], indicándole con `partialAccess`
 *   si debe pintar el `PartialAccessBanner`. En ambos casos `onExpandSelection`
 *   relanza el selector del sistema.
 *
 * La navegación real (rutas, back stack) llega en la etapa 5; esto es solo la
 * pieza reutilizable de gating.
 */
@Composable
fun PermissionGate(
    modifier: Modifier = Modifier,
    includeVideos: Boolean = true,
    content: @Composable (partialAccess: Boolean, onExpandSelection: () -> Unit) -> Unit,
) {
    val permission = rememberMediaPermissionUi(includeVideos)

    when (permission.level) {
        MediaAccessLevel.NONE -> PermissionOnboardingScreen(
            level = permission.level,
            needsSettings = permission.needsSettings,
            onGrant = permission.onRequest,
            onOpenSettings = permission.onOpenSettings,
            onContinuePartial = {},
            modifier = modifier,
        )

        MediaAccessLevel.PARTIAL -> content(true, permission.onRequest)
        MediaAccessLevel.FULL -> content(false, permission.onRequest)
    }
}
