package org.comon.streamlauncher.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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

@Composable
fun PortraitSettingsScreen(
    settingMenuList: List<SettingMenuItem>,
    onItemClick: (SettingsActionType) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .navigationBarsPadding()
            .statusBarsPadding()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        settingMenuList.chunked(2).forEach { menuItems ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
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