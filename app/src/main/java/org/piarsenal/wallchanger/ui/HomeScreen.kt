package org.piarsenal.wallchanger.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.piarsenal.wallchanger.R

@Composable
fun HomeScreen(vm: WallpaperViewModel) {
    val s by vm.settings.collectAsState()
    val changing by vm.isChanging.collectAsState()
    val enabledSources = s.sources.count { it.enabled }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(stringResource(R.string.home_title), style = MaterialTheme.typography.headlineMedium)

        ElevatedCard(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(stringResource(R.string.home_auto), style = MaterialTheme.typography.titleMedium)
                    val sub = when {
                        enabledSources == 0 -> stringResource(R.string.home_add_source_first)
                        s.enabled -> stringResource(R.string.home_every_minutes, s.intervalMinutes.coerceAtLeast(15))
                        else -> stringResource(R.string.home_paused)
                    }
                    Text(sub, style = MaterialTheme.typography.bodyMedium)
                }
                Switch(
                    checked = s.enabled && enabledSources > 0,
                    onCheckedChange = { vm.setEnabled(it) },
                    enabled = enabledSources > 0
                )
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.home_status), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.home_active_sources, enabledSources))
                Text(
                    stringResource(R.string.home_battery_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Button(
            onClick = { vm.changeNow() },
            enabled = enabledSources > 0 && !changing,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (changing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text("  " + stringResource(R.string.home_changing))
            } else {
                Icon(Icons.Filled.Refresh, contentDescription = null)
                Text("  " + stringResource(R.string.home_change_now))
            }
        }
    }
}
