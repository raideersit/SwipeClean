package com.swipeclean.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.swipeclean.app.R
import com.swipeclean.app.ui.theme.SwipeCleanTheme

/**
 * Aviso reutilizable para el estado de acceso parcial (API 34+): la app solo ve
 * las fotos que el usuario seleccionó a mano. El botón vuelve a abrir el selector
 * del sistema para ampliar esa selección.
 */
@Composable
fun PartialAccessBanner(
    onExpandSelection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.partial_access_message),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onExpandSelection) {
                Text(stringResource(R.string.partial_access_expand))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PartialAccessBannerPreview() {
    SwipeCleanTheme {
        PartialAccessBanner(onExpandSelection = {})
    }
}
