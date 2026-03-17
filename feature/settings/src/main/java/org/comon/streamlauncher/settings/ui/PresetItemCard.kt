package org.comon.streamlauncher.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.comon.streamlauncher.domain.model.preset.Preset
import org.comon.streamlauncher.domain.model.preset.UploadProgress
import org.comon.streamlauncher.settings.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun PresetItemCard(
    preset: Preset,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onShare: (() -> Unit)? = null,
    isPending: Boolean = false,
    uploadProgress: UploadProgress? = null,
    onPauseUpload: () -> Unit = {},
    onResumeUpload: () -> Unit = {},
    onCancelUpload: () -> Unit = {},
) {
    var showCancelUploadDialog by remember { mutableStateOf(false) }

    val dateString = remember(preset.createdAt) {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(preset.createdAt))
    }

    val isUploading = uploadProgress != null || isPending

    if (showCancelUploadDialog) {
        LaunchedEffect(Unit) {
            onPauseUpload()
        }
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.upload_cancel_title)) },
            text = { Text(stringResource(R.string.upload_cancel_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showCancelUploadDialog = false
                    onCancelUpload()
                }) {
                    Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    onResumeUpload()
                    showCancelUploadDialog = false
                }) {
                    Text(stringResource(R.string.upload_cancel_resume))
                }
            },
        )
    }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUploading) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = preset.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dateString,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        if (preset.hasTopLeftImage || preset.hasTopRightImage || preset.hasBottomLeftImage || preset.hasBottomRightImage) PresetTag("Home")
                        if (preset.hasFeedSettings) PresetTag("Feed")
                        if (preset.hasAppDrawerSettings) PresetTag("Drawer")
                        if (preset.hasThemeSettings) PresetTag("Theme")
                        if (preset.hasWallpaperSettings) PresetTag("Wallpaper")
                    }
                }
                if (!isUploading && onShare != null) {
                    IconButton(onClick = onShare) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = stringResource(R.string.preset_share_desc),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // 업로드 진행 표시
            if (isUploading) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 4.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (uploadProgress != null) {
                            LinearProgressIndicator(
                                progress = { uploadProgress.percentage },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Text(
                                text = stringResource(R.string.preset_upload_progress, (uploadProgress.percentage * 100).toInt()),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        } else {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            Text(
                                text = stringResource(R.string.preset_upload_preparing),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    IconButton(onClick = { showCancelUploadDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.preset_upload_cancel_desc),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}