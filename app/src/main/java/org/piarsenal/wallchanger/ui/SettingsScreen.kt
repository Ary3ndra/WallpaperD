package org.piarsenal.wallchanger.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.piarsenal.wallchanger.R

@Composable
fun SettingsScreen(vm: WallpaperViewModel) {
    val s by vm.settings.collectAsState()
    val cache by vm.cacheBytes.collectAsState()
    val logs by vm.logText.collectAsState()

    LaunchedEffect(Unit) { vm.refreshLogs() }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineMedium)

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.settings_interval), style = MaterialTheme.typography.titleMedium)
                val presets = listOf(15L, 30L, 60L, 180L, 360L, 720L, 1440L)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    presets.forEach { m ->
                        FilterChip(
                            selected = s.intervalMinutes == m,
                            onClick = { vm.setInterval(m) },
                            label = { Text(labelFor(m)) }
                        )
                    }
                }
                Text(
                    stringResource(R.string.settings_interval_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                toggle(stringResource(R.string.settings_only_wifi), s.onlyOnWifi, vm::setWifiOnly)
                toggle(stringResource(R.string.settings_only_charging), s.onlyWhenCharging, vm::setChargingOnly)
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                toggle(stringResource(R.string.settings_apply_home), s.applyToHomeScreen, vm::setApplyHome)
                toggle(stringResource(R.string.settings_apply_lock), s.applyToLockScreen, vm::setApplyLock)
                toggle(stringResource(R.string.settings_notify), s.notifyOnChange, vm::setNotify)
                toggle(stringResource(R.string.settings_avoid_repeats), s.avoidRepeats, vm::setAvoidRepeats)
                toggle(stringResource(R.string.settings_prefetch), s.prefetchEnabled, vm::setPrefetch)
            }
        }

        // Cache: live size, numeric limit in MB/GB, and clear.
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.settings_cache), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.settings_cache_size, "%.1f".format(cache / 1024f / 1024f)))

                var gb by remember { mutableStateOf(s.cacheLimitMb >= 1024 && s.cacheLimitMb % 1024 == 0L) }
                var text by remember { mutableStateOf(if (gb) (s.cacheLimitMb / 1024).toString() else s.cacheLimitMb.toString()) }
                Text(stringResource(R.string.settings_cache_limit), style = MaterialTheme.typography.bodyMedium)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { v ->
                            text = v.filter(Char::isDigit)
                            val n = text.toLongOrNull() ?: 0L
                            vm.setCacheLimitMb(if (gb) n * 1024 else n)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(120.dp)
                    )
                    FilterChip(selected = !gb, onClick = {
                        if (gb) { val mb = (text.toLongOrNull() ?: 0L) * 1024; text = mb.toString(); gb = false }
                    }, label = { Text(stringResource(R.string.settings_cache_unit_mb)) })
                    FilterChip(selected = gb, onClick = {
                        if (!gb) { val mb = text.toLongOrNull() ?: 0L; text = (mb / 1024).toString(); gb = true }
                    }, label = { Text(stringResource(R.string.settings_cache_unit_gb)) })
                }
                OutlinedButton(onClick = { vm.clearCache() }) { Text(stringResource(R.string.settings_delete_cache)) }
            }
        }

        // Diagnostics: shows exactly why a source failed.
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.settings_diagnostics), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.settings_logs_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    Modifier.fillMaxWidth().heightIn(max = 220.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = logs.ifBlank { stringResource(R.string.settings_logs_empty) },
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { vm.refreshLogs() }) { Text(stringResource(R.string.settings_refresh)) }
                    TextButton(onClick = { vm.clearLogs() }) { Text(stringResource(R.string.settings_clear_logs)) }
                }
            }
        }
    }
}

@Composable
private fun toggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

private fun labelFor(min: Long): String = when {
    min < 60 -> "$min m"
    min < 1440 -> "${min / 60} h"
    else -> "${min / 1440} d"
}
