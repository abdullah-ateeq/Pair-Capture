package com.example.paircapture.ui.screens

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.paircapture.data.AppSettings
import com.example.paircapture.data.ThemeMode
import com.example.paircapture.game.model.Difficulty

private enum class PendingReset { STATISTICS, SETTINGS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onUpdate: ((AppSettings) -> AppSettings) -> Unit,
    onResetStatistics: () -> Unit,
    onResetSettings: () -> Unit
) {
    var pendingReset by rememberSaveable { mutableStateOf<PendingReset?>(null) }
    val dynamicSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SETTINGS", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SettingsSection("APPEARANCE") {
                    ChipRow(
                        title = "Theme",
                        options = ThemeMode.entries,
                        selected = settings.themeMode,
                        label = { it.label },
                        onSelect = { mode -> onUpdate { it.copy(themeMode = mode) } }
                    )
                    SectionDivider()
                    SwitchRow(
                        title = "Dynamic Color",
                        subtitle = if (dynamicSupported) "Use colors from your wallpaper" else "Requires Android 12 or newer",
                        checked = settings.dynamicColor && dynamicSupported,
                        enabled = dynamicSupported,
                        onChange = { value -> onUpdate { it.copy(dynamicColor = value) } }
                    )
                    SectionDivider()
                    SwitchRow(
                        title = "Animations",
                        subtitle = "Tile flips, pulses and shakes",
                        checked = settings.animations,
                        onChange = { value -> onUpdate { it.copy(animations = value) } }
                    )
                }

                SettingsSection("GAMEPLAY") {
                    ChipRow(
                        title = "CPU Difficulty",
                        subtitle = settings.difficulty.description,
                        options = Difficulty.entries,
                        selected = settings.difficulty,
                        label = { it.label },
                        onSelect = { level -> onUpdate { it.copy(difficulty = level) } }
                    )
                    SectionDivider()
                    SwitchRow(
                        title = "Confirm Restart",
                        subtitle = "Ask before reshuffling a match",
                        checked = settings.confirmRestart,
                        onChange = { value -> onUpdate { it.copy(confirmRestart = value) } }
                    )
                    SectionDivider()
                    SwitchRow(
                        title = "Show Turn Counter",
                        checked = settings.showTurnCounter,
                        onChange = { value -> onUpdate { it.copy(showTurnCounter = value) } }
                    )
                }

                SettingsSection("FEEDBACK") {
                    SwitchRow(
                        title = "Sound",
                        checked = settings.sound,
                        onChange = { value -> onUpdate { it.copy(sound = value) } }
                    )
                    SectionDivider()
                    SwitchRow(
                        title = "Haptic Feedback",
                        checked = settings.haptics,
                        onChange = { value -> onUpdate { it.copy(haptics = value) } }
                    )
                }

                SettingsSection("DATA") {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { pendingReset = PendingReset.STATISTICS },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("RESET STATISTICS") }
                        OutlinedButton(
                            onClick = { pendingReset = PendingReset.SETTINGS },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("RESET SETTINGS") }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    pendingReset?.let { reset ->
        val (title, body) = when (reset) {
            PendingReset.STATISTICS -> "Reset statistics?" to "Games, wins, best score and streaks will be cleared."
            PendingReset.SETTINGS -> "Reset settings?" to "All settings return to their defaults."
        }
        AlertDialog(
            onDismissRequest = { pendingReset = null },
            title = { Text(title) },
            text = { Text(body) },
            confirmButton = {
                TextButton(onClick = {
                    if (reset == PendingReset.STATISTICS) onResetStatistics() else onResetSettings()
                    pendingReset = null
                }) { Text("RESET") }
            },
            dismissButton = { TextButton(onClick = { pendingReset = null }) { Text("CANCEL") } }
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            content()
        }
    }
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(
        Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

@Composable
private fun SwitchRow(
    title: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    subtitle: String? = null,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

@Composable
private fun <T> ChipRow(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    subtitle: String? = null
) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { option ->
                FilterChip(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    label = { Text(label(option)) }
                )
            }
        }
    }
}
