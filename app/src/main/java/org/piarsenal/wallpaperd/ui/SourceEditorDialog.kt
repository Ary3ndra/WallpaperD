package org.piarsenal.wallpaperd.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.FlowRow
import org.piarsenal.wallpaperd.data.ImageQuality
import org.piarsenal.wallpaperd.data.SourceConfig
import org.piarsenal.wallpaperd.data.SourceType

@Composable
fun SourceEditorDialog(
    initial: SourceConfig,
    onDismiss: () -> Unit,
    onSave: (SourceConfig) -> Unit
) {
    val context = LocalContext.current
    var cfg by remember { mutableStateOf(initial) }
    var showWhInfo by remember { mutableStateOf(false) }

    // SAF folder picker — grants persistable read access to the chosen tree.
    val folderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            cfg = cfg.copy(folderTreeUri = uri.toString())
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onSave(cfg) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Edit source") },
        text = {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = cfg.name,
                    onValueChange = { cfg = cfg.copy(name = it) },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Type")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SourceType.entries.forEach { t ->
                        FilterChip(
                            selected = cfg.type == t,
                            onClick = { cfg = cfg.copy(type = t) },
                            label = { Text(t.name) }
                        )
                    }
                }

                when (cfg.type) {
                    SourceType.FOLDER -> {
                        OutlinedButton(onClick = { folderLauncher.launch(null) }, modifier = Modifier.fillMaxWidth()) {
                            Text(if (cfg.folderTreeUri == null) "Pick folder" else "Folder selected — change")
                        }
                        cfg.folderTreeUri?.let { AssistChip(onClick = {}, label = { Text(it.takeLast(40)) }) }
                        switchRow("Include subfolders", cfg.folderRecursive) { cfg = cfg.copy(folderRecursive = it) }
                        if (cfg.folderRecursive) {
                            field("Skip folders named (comma separated)", cfg.folderExcludeNames) { cfg = cfg.copy(folderExcludeNames = it) }
                        }
                    }

                    SourceType.WALLHAVEN -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Filters")
                            IconButton(onClick = { showWhInfo = true }) {
                                Icon(Icons.Outlined.Info, contentDescription = "What do these mean?")
                            }
                        }
                        field("Search query (optional)", cfg.whQuery) { cfg = cfg.copy(whQuery = it) }
                        field("Categories bits (general/anime/people)", cfg.whCategories) { cfg = cfg.copy(whCategories = it) }
                        field("Purity bits (sfw/sketchy/nsfw)", cfg.whPurity) { cfg = cfg.copy(whPurity = it) }
                        field("Ratios (e.g. 16x9)", cfg.whRatios) { cfg = cfg.copy(whRatios = it) }
                        field("Sorting (random/toplist/...)", cfg.whSorting) { cfg = cfg.copy(whSorting = it) }
                        field("API key (needed for NSFW)", cfg.whApiKey) { cfg = cfg.copy(whApiKey = it) }
                    }

                    SourceType.UNSPLASH -> {
                        field("Access key (client_id)", cfg.unsplashAccessKey) { cfg = cfg.copy(unsplashAccessKey = it) }
                        field("Query (optional, e.g. mountains)", cfg.unsplashQuery) { cfg = cfg.copy(unsplashQuery = it) }
                        field("Orientation (landscape/portrait/squarish)", cfg.unsplashOrientation) { cfg = cfg.copy(unsplashOrientation = it) }
                        qualityChips(cfg.quality) { cfg = cfg.copy(quality = it) }
                    }

                    SourceType.NASA_APOD -> {
                        field("NASA API key (DEMO_KEY works, rate-limited)", cfg.nasaApiKey) { cfg = cfg.copy(nasaApiKey = it) }
                        Text(
                            "Some days are videos; those are skipped automatically.",
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                        )
                        qualityChips(cfg.quality) { cfg = cfg.copy(quality = it) }
                    }

                    SourceType.GITHUB -> {
                        field("Repo URL (e.g. https://github.com/owner/repo)", cfg.ghUrl) { cfg = cfg.copy(ghUrl = it) }
                        field("Branch (blank = from URL or main)", cfg.ghBranch) { cfg = cfg.copy(ghBranch = it) }
                        field("Subfolder path (blank = repo root)", cfg.ghPath) { cfg = cfg.copy(ghPath = it) }
                        field("Token (optional: private repo / rate limit)", cfg.ghToken) { cfg = cfg.copy(ghToken = it) }
                        switchRow("Include subfolders", cfg.ghRecursive) { cfg = cfg.copy(ghRecursive = it) }
                    }

                    SourceType.URL_LIST -> {
                        field("Image URLs (one per line)", cfg.urls.joinToString("\n")) {
                            cfg = cfg.copy(urls = it.split("\n").map(String::trim).filter(String::isNotBlank))
                        }
                    }

                    SourceType.JSON_API -> {
                        field("JSON endpoint URL", cfg.jsonEndpoint) { cfg = cfg.copy(jsonEndpoint = it) }
                        field("Image field path (e.g. data.0.url)", cfg.jsonImagePath) { cfg = cfg.copy(jsonImagePath = it) }
                        field("Headers (k:v per line)", cfg.jsonHeaders.entries.joinToString("\n") { "${it.key}:${it.value}" }) {
                            cfg = cfg.copy(jsonHeaders = it.split("\n").mapNotNull { line ->
                                val parts = line.split(":", limit = 2)
                                if (parts.size == 2 && parts[0].isNotBlank()) parts[0].trim() to parts[1].trim() else null
                            }.toMap())
                        }
                    }
                }
            }
        }
    )

    if (showWhInfo) {
        AlertDialog(
            onDismissRequest = { showWhInfo = false },
            confirmButton = { TextButton(onClick = { showWhInfo = false }) { Text("Got it") } },
            title = { Text("Wallhaven filters") },
            text = { Text(WH_INFO) }
        )
    }
}

private const val WH_INFO =
    "Wallhaven uses three on/off switches written as digits (1 = include, 0 = skip).\n\n" +
    "Categories — General, Anime, People.\n" +
    "  100 = General only\n" +
    "  010 = Anime only\n" +
    "  111 = everything\n\n" +
    "Purity — SFW, Sketchy, NSFW.\n" +
    "  100 = safe only (default)\n" +
    "  110 = safe + sketchy\n" +
    "The NSFW digit needs a Wallhaven API key.\n\n" +
    "Ratios — preferred shape, e.g. 16x9 for landscape, 9x16 for a phone held upright.\n\n" +
    "Sorting — random, toplist, date_added, views, favorites.\n\n" +
    "API key — optional; only needed for NSFW or higher rate limits."

@Composable
private fun field(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth().padding(top = 0.dp)
    )
}

@Composable
private fun switchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun qualityChips(selected: ImageQuality, onSelect: (ImageQuality) -> Unit) {
    Text("Image quality")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ImageQuality.entries.forEach { q ->
            FilterChip(
                selected = selected == q,
                onClick = { onSelect(q) },
                label = { Text(q.name.lowercase().replaceFirstChar { it.uppercase() }) }
            )
        }
    }
}
