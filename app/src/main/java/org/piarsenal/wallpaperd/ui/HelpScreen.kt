package org.piarsenal.wallpaperd.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.piarsenal.wallpaperd.R

@Composable
fun HelpScreen() {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(stringResource(R.string.help_title), style = MaterialTheme.typography.headlineMedium)

        section(R.string.help_what_t, R.string.help_what_b)
        section(R.string.help_sources_t, R.string.help_sources_b)
        section(R.string.help_change_t, R.string.help_change_b)
        section(R.string.help_now_t, R.string.help_now_b)
        section(R.string.help_data_t, R.string.help_data_b)
        section(R.string.help_history_t, R.string.help_history_b)
        section(R.string.help_cache_t, R.string.help_cache_b)
        section(R.string.help_privacy_t, R.string.help_privacy_b)
        section(R.string.help_perms_t, R.string.help_perms_b)
    }
}

@Composable
private fun section(@StringRes title: Int, @StringRes body: Int) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(body), style = MaterialTheme.typography.bodyMedium)
        }
    }
}
