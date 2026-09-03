package com.swipeclean.app.ui.permissions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.swipeclean.app.R
import com.swipeclean.app.domain.model.MediaAccessLevel
import com.swipeclean.app.ui.theme.SwipeCleanTheme

/**
 * Pantalla previa al uso de la app: explica por qué se necesita la galería y pide
 * el permiso. Sin lógica: recibe el [level] y [needsSettings] ya resueltos (ver
 * [rememberMediaPermissionUi]) y expone las acciones como lambdas.
 *
 * @param onGrant lanza la solicitud del sistema (o el selector de "ampliar
 *   selección" si el acceso es parcial).
 * @param onOpenSettings abre los ajustes de la app; útil solo con [needsSettings].
 * @param onContinuePartial sigue adelante conservando la selección parcial actual.
 */
@Composable
fun PermissionOnboardingScreen(
    level: MediaAccessLevel,
    needsSettings: Boolean,
    onGrant: () -> Unit,
    onOpenSettings: () -> Unit,
    onContinuePartial: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val bodyModifier = Modifier.widthIn(max = 360.dp)
        when {
            needsSettings -> {
                Text(
                    text = stringResource(R.string.permission_denied_title),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.permission_denied_body),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = bodyModifier,
                )
                Spacer(Modifier.height(24.dp))
                Button(onClick = onOpenSettings) {
                    Text(stringResource(R.string.permission_open_settings))
                }
            }

            level == MediaAccessLevel.PARTIAL -> {
                Text(
                    text = stringResource(R.string.permission_onboarding_title),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.partial_access_message),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = bodyModifier,
                )
                Spacer(Modifier.height(24.dp))
                Button(onClick = onGrant) {
                    Text(stringResource(R.string.partial_access_expand))
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onContinuePartial) {
                    Text(stringResource(R.string.permission_partial_continue))
                }
            }

            else -> {
                Text(
                    text = stringResource(R.string.permission_onboarding_title),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.permission_onboarding_body),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = bodyModifier,
                )
                Spacer(Modifier.height(24.dp))
                Button(onClick = onGrant) {
                    Text(stringResource(R.string.permission_onboarding_grant))
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Sin permiso")
@Composable
private fun PermissionOnboardingScreenPreview() {
    SwipeCleanTheme {
        PermissionOnboardingScreen(
            level = MediaAccessLevel.NONE,
            needsSettings = false,
            onGrant = {},
            onOpenSettings = {},
            onContinuePartial = {},
        )
    }
}

@Preview(showBackground = true, name = "Denegado permanentemente")
@Composable
private fun PermissionOnboardingDeniedPreview() {
    SwipeCleanTheme {
        PermissionOnboardingScreen(
            level = MediaAccessLevel.NONE,
            needsSettings = true,
            onGrant = {},
            onOpenSettings = {},
            onContinuePartial = {},
        )
    }
}

@Preview(showBackground = true, name = "Acceso parcial")
@Composable
private fun PermissionOnboardingPartialPreview() {
    SwipeCleanTheme {
        PermissionOnboardingScreen(
            level = MediaAccessLevel.PARTIAL,
            needsSettings = false,
            onGrant = {},
            onOpenSettings = {},
            onContinuePartial = {},
        )
    }
}
