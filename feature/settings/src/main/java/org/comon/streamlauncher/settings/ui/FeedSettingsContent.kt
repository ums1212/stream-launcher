package org.comon.streamlauncher.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.comon.streamlauncher.settings.R
import org.comon.streamlauncher.settings.SettingsIntent
import org.comon.streamlauncher.settings.SettingsState
import org.comon.streamlauncher.ui.theme.StreamLauncherTheme

@Composable
internal fun FeedSettingsContent(
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit,
) {
    val accentPrimary = StreamLauncherTheme.colors.accentPrimary

    var chzzkChannelId by remember(state.chzzkChannelId) { mutableStateOf(state.chzzkChannelId) }
    var youtubeChannelId by remember(state.youtubeChannelId) { mutableStateOf(state.youtubeChannelId) }

    val chzzkError by remember {
        derivedStateOf {
            chzzkChannelId.any { it.isWhitespace() }
        }
    }
    val youtubeError by remember {
        derivedStateOf {
            youtubeChannelId.any { it.isWhitespace() }
        }
    }
    val isSaveEnabled by remember {
        derivedStateOf { !chzzkError && !youtubeError }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {

        OutlinedTextField(
            value = chzzkChannelId,
            onValueChange = { chzzkChannelId = it },
            label = { Text(stringResource(R.string.settings_chzzk_channel_id)) },
            isError = chzzkError,
            supportingText = if (chzzkError) {
                { Text(stringResource(R.string.settings_no_whitespace)) }
            } else null,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        OutlinedTextField(
            value = youtubeChannelId,
            onValueChange = { youtubeChannelId = it },
            label = { Text(stringResource(R.string.settings_youtube_channel_id)) },
            placeholder = { Text(stringResource(R.string.settings_youtube_placeholder)) },
            isError = youtubeError,
            supportingText = if (youtubeError) {
                { Text(stringResource(R.string.settings_no_whitespace)) }
            } else null,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Button(
            onClick = {
                onIntent(
                    SettingsIntent.SaveFeedSettings(
                        chzzkChannelId = chzzkChannelId.trim(),
                        youtubeChannelId = youtubeChannelId.trim(),
                    ),
                )
            },
            enabled = isSaveEnabled,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = accentPrimary),
        ) {
            Text(text = stringResource(R.string.settings_save))
        }
    }
}
