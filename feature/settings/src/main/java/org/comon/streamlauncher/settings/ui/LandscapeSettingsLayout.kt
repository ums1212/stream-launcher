package org.comon.streamlauncher.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.comon.streamlauncher.settings.model.SettingMenuItem
import org.comon.streamlauncher.settings.model.SettingsActionType
import kotlin.collections.chunked
import kotlin.collections.forEach

@Composable
fun LandScapeSettingsScreen(
    settingMenuList: List<SettingMenuItem>,
    onItemClick: (SettingsActionType) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .statusBarsPadding()
            .padding(24.dp),
    ) {
        settingMenuList.chunked(2).forEach { menuItems ->
            Column (
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f).fillMaxHeight(),
            ) {
                menuItems.forEach { item ->
                    GlassSettingsTile(
                        label = stringResource(item.labelId),
                        icon = item.icon,
                        lerpFraction = item.lerpFraction,
                        onClick = { onItemClick(item.actionType) },
                        modifier = Modifier.fillMaxWidth().weight(1f),
                    )
                }
            }
        }
    }
}