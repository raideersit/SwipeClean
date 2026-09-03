package com.swipeclean.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.swipeclean.app.R
import com.swipeclean.app.domain.model.ThemeMode
import com.swipeclean.app.ui.theme.SwipeCleanTheme
import com.swipeclean.app.util.formatBytes

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreenContent(
        state = state,
        onBack = onBack,
        onPermanentDeleteChange = viewModel::setPermanentDelete,
        onIncludeVideosChange = viewModel::setIncludeVideos,
        onThemeModeChange = viewModel::setThemeMode,
        onResetHistory = viewModel::resetHistory,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenContent(
    state: SettingsUiState,
    onBack: () -> Unit,
    onPermanentDeleteChange: (Boolean) -> Unit,
    onIncludeVideosChange: (Boolean) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onResetHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showResetDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back_cd),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            SectionTitle(stringResource(R.string.settings_section_delete))
            SwitchRow(
                title = stringResource(R.string.settings_permanent_delete),
                description = if (state.permanentDelete) {
                    stringResource(R.string.settings_permanent_delete_warning)
                } else {
                    stringResource(R.string.settings_trash_desc)
                },
                descriptionIsWarning = state.permanentDelete,
                checked = state.permanentDelete,
                onCheckedChange = onPermanentDeleteChange,
            )
            SwitchRow(
                title = stringResource(R.string.settings_include_videos),
                description = stringResource(R.string.settings_include_videos_desc),
                descriptionIsWarning = false,
                checked = state.includeVideos,
                onCheckedChange = onIncludeVideosChange,
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SectionTitle(stringResource(R.string.settings_section_theme))
            ThemeOptions(selected = state.themeMode, onSelect = onThemeModeChange)

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SectionTitle(stringResource(R.string.settings_section_stats))
            StatRow(
                label = stringResource(R.string.settings_stat_freed),
                value = formatBytes(state.totalFreedBytes),
            )
            StatRow(
                label = stringResource(R.string.settings_stat_deleted),
                value = state.totalDeletedCount.toString(),
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SectionTitle(stringResource(R.string.settings_section_history))
            Text(
                text = stringResource(R.string.settings_reset_history_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            OutlinedButton(
                onClick = { showResetDialog = true },
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth(),
            ) {
                Text(stringResource(R.string.settings_reset_history))
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(R.string.settings_reset_dialog_title)) },
            text = { Text(stringResource(R.string.settings_reset_dialog_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetDialog = false
                        onResetHistory()
                    },
                ) { Text(stringResource(R.string.settings_reset_dialog_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(R.string.settings_reset_dialog_cancel))
                }
            },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun SwitchRow(
    title: String,
    description: String,
    descriptionIsWarning: Boolean,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = if (descriptionIsWarning) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ThemeOptions(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    val options = listOf(
        ThemeMode.LIGHT to stringResource(R.string.settings_theme_light),
        ThemeMode.DARK to stringResource(R.string.settings_theme_dark),
        ThemeMode.SYSTEM to stringResource(R.string.settings_theme_system),
    )
    Column(modifier = Modifier.selectableGroup()) {
        options.forEach { (mode, label) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = mode == selected,
                        role = Role.RadioButton,
                        onClick = { onSelect(mode) },
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                RadioButton(selected = mode == selected, onClick = null)
                Text(text = label, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(showBackground = true, name = "Ajustes")
@Composable
private fun SettingsPreview() {
    SwipeCleanTheme {
        SettingsScreenContent(
            state = SettingsUiState(
                permanentDelete = false,
                includeVideos = true,
                themeMode = ThemeMode.SYSTEM,
                totalFreedBytes = 1_400L * 1024 * 1024,
                totalDeletedCount = 87,
            ),
            onBack = {},
            onPermanentDeleteChange = {},
            onIncludeVideosChange = {},
            onThemeModeChange = {},
            onResetHistory = {},
        )
    }
}

@Preview(showBackground = true, name = "Borrado permanente activo")
@Composable
private fun SettingsPermanentPreview() {
    SwipeCleanTheme {
        SettingsScreenContent(
            state = SettingsUiState(permanentDelete = true, themeMode = ThemeMode.DARK),
            onBack = {},
            onPermanentDeleteChange = {},
            onIncludeVideosChange = {},
            onThemeModeChange = {},
            onResetHistory = {},
        )
    }
}
