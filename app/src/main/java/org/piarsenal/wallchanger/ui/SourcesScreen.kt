package org.piarsenal.wallchanger.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.piarsenal.wallchanger.data.SourceConfig
import org.piarsenal.wallchanger.data.SourceType
import java.util.UUID

@Composable
fun SourcesScreen(vm: WallpaperViewModel) {
    val s by vm.settings.collectAsState()
    var editing by remember { mutableStateOf<SourceConfig?>(null) }
    var menuOpen by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Text("Sources", style = MaterialTheme.typography.headlineMedium) }

            if (s.sources.isEmpty()) {
                item { Text("No sources yet. Tap + to add one.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }

            items(s.sources, key = { it.id }) { src ->
                Card(Modifier.fillMaxWidth().clickable { editing = src }) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.padding(end = 8.dp)) {
                            Text(src.name.ifBlank { src.type.name }, style = MaterialTheme.typography.titleMedium)
                            Text(src.type.name, style = MaterialTheme.typography.bodySmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(checked = src.enabled, onCheckedChange = { vm.upsertSource(src.copy(enabled = it)) })
                            IconButton(onClick = { vm.removeSource(src.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete")
                            }
                        }
                    }
                }
            }
        }

        Box(Modifier.align(Alignment.BottomEnd).padding(24.dp)) {
            FloatingActionButton(onClick = { menuOpen = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add source")
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                SourceType.entries.forEach { t ->
                    DropdownMenuItem(
                        text = { Text(presetName(t)) },
                        onClick = {
                            menuOpen = false
                            editing = SourceConfig(
                                id = UUID.randomUUID().toString(),
                                type = t,
                                name = presetName(t)
                            )
                        }
                    )
                }
            }
        }
    }

    editing?.let { src ->
        SourceEditorDialog(
            initial = src,
            onDismiss = { editing = null },
            onSave = { vm.upsertSource(it); editing = null }
        )
    }
}

private fun presetName(type: SourceType): String = when (type) {
    SourceType.FOLDER -> "Local folder"
    SourceType.WALLHAVEN -> "Wallhaven"
    SourceType.UNSPLASH -> "Unsplash"
    SourceType.NASA_APOD -> "NASA APOD"
    SourceType.GITHUB -> "GitHub repo"
    SourceType.URL_LIST -> "URL list"
    SourceType.JSON_API -> "JSON API"
}
