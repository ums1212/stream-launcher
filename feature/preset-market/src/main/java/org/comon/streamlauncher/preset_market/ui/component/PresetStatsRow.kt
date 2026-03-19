package org.comon.streamlauncher.preset_market.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
internal fun PresetStatsRow(
    downloadCount: Int,
    likeCount: Int,
    modifier: Modifier = Modifier,
    iconSize: Dp = 14.dp,
    color: Color = Color.White,
    outerSpacing: Dp = 12.dp,
    innerSpacing: Dp = 4.dp,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(outerSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(innerSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(iconSize),
            )
            Text(
                text = "$downloadCount",
                style = MaterialTheme.typography.labelSmall,
                color = color,
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(innerSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(iconSize),
            )
            Text(
                text = "$likeCount",
                style = MaterialTheme.typography.labelSmall,
                color = color,
            )
        }
    }
}
