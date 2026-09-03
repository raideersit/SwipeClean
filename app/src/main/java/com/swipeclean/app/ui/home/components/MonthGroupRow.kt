package com.swipeclean.app.ui.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import com.swipeclean.app.R
import com.swipeclean.app.domain.model.MediaMonthGroup
import com.swipeclean.app.ui.theme.SwipeCleanTheme
import com.swipeclean.app.util.formatBytes
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

private val LOCALE_ES = Locale.forLanguageTag("es")

/** Etiqueta legible de un [MediaMonthGroup], p. ej. "Septiembre 2026". */
fun monthGroupLabel(group: MediaMonthGroup): String {
    val nombreMes = Month.of(group.month).getDisplayName(TextStyle.FULL, LOCALE_ES)
        .replaceFirstChar { it.titlecase(LOCALE_ES) }
    return "$nombreMes ${group.year}"
}

/** Fila de un mes en la sección "Por fecha" de la Home. */
@Composable
fun MonthGroupRow(
    group: MediaMonthGroup,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(text = monthGroupLabel(group), style = MaterialTheme.typography.bodyLarge)
            Text(
                text = stringResource(
                    R.string.home_month_meta,
                    group.cantidad,
                    formatBytes(group.pesoTotalBytes),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MonthGroupRowPreview() {
    SwipeCleanTheme {
        MonthGroupRow(
            group = MediaMonthGroup(
                year = 2026,
                month = 9,
                startMillis = 0L,
                endMillis = 0L,
                cantidad = 42,
                pesoTotalBytes = 1_200L * 1024 * 1024,
            ),
            onClick = {},
        )
    }
}
