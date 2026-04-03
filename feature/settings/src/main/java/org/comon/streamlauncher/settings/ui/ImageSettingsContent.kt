package org.comon.streamlauncher.settings.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.comon.streamlauncher.domain.model.GridCell
import org.comon.streamlauncher.domain.model.GridCellImage
import org.comon.streamlauncher.settings.R
import org.comon.streamlauncher.settings.image.ImageSettingsIntent
import org.comon.streamlauncher.settings.image.ImageSettingsViewModel
import org.comon.streamlauncher.settings.model.ImageType
import org.comon.streamlauncher.ui.theme.StreamLauncherTheme
import org.comon.streamlauncher.ui.util.calculateIsCompactHeight

@Composable
internal fun ImageSettingsContent(
    viewModel: ImageSettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var selectedCell by remember { mutableStateOf(GridCell.TOP_LEFT) }
    var showResetDialog by remember { mutableStateOf(false) }

    val idleImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
        viewModel.handleIntent(ImageSettingsIntent.SetGridImage(selectedCell, ImageType.IDLE, uri.toString()))
    }

    val expandedImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
        viewModel.handleIntent(ImageSettingsIntent.SetGridImage(selectedCell, ImageType.EXPANDED, uri.toString()))
    }

    if (calculateIsCompactHeight()) {
        ImageSettingsLandscapeLayout(
            selectedCell = selectedCell,
            selectCellEvent = { selectedCell = it },
            gridCellImages = state.gridCellImages,
            idleImageLauncher = idleImageLauncher,
            expandedImageLauncher = expandedImageLauncher,
            showResetDialogEvent = { showResetDialog = true },
        )
    } else {
        ImageSettingsPortraitLayout(
            selectedCell = selectedCell,
            selectCellEvent = { selectedCell = it },
            gridCellImages = state.gridCellImages,
            idleImageLauncher = idleImageLauncher,
            expandedImageLauncher = expandedImageLauncher,
            showResetDialogEvent = { showResetDialog = true },
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            text = { Text(text = stringResource(R.string.settings_image_reset_dialog_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.handleIntent(ImageSettingsIntent.ResetAllGridImages)
                        showResetDialog = false
                    },
                ) {
                    Text(text = stringResource(R.string.preset_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun ImageSettingsPortraitLayout(
    selectedCell: GridCell,
    selectCellEvent: (GridCell) -> Unit,
    gridCellImages: Map<GridCell, GridCellImage>,
    idleImageLauncher: ManagedActivityResultLauncher<PickVisualMediaRequest, Uri?>,
    expandedImageLauncher: ManagedActivityResultLauncher<PickVisualMediaRequest, Uri?>,
    showResetDialogEvent: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        ImageSettingsGrid(
            selectedCell = selectedCell,
            gridCellImages = gridCellImages,
            selectCellEvent = selectCellEvent,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = { idleImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = StreamLauncherTheme.colors.accentPrimary),
            ) {
                Text(text = stringResource(R.string.settings_idle_image))
            }
            Button(
                onClick = { expandedImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = StreamLauncherTheme.colors.accentSecondary),
            ) {
                Text(text = stringResource(R.string.settings_expanded_image))
            }
        }

        OutlinedButton(
            onClick = showResetDialogEvent,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
        ) {
            Text(text = stringResource(R.string.settings_image_reset))
        }
    }
}

@Composable
private fun ImageSettingsLandscapeLayout(
    selectedCell: GridCell,
    selectCellEvent: (GridCell) -> Unit,
    gridCellImages: Map<GridCell, GridCellImage>,
    idleImageLauncher: ManagedActivityResultLauncher<PickVisualMediaRequest, Uri?>,
    expandedImageLauncher: ManagedActivityResultLauncher<PickVisualMediaRequest, Uri?>,
    showResetDialogEvent: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        ImageSettingsGrid(
            selectedCell = selectedCell,
            gridCellImages = gridCellImages,
            selectCellEvent = selectCellEvent,
            modifier = Modifier
                .fillMaxHeight()
                .weight(3f),
        )

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {
            Button(
                onClick = { idleImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = StreamLauncherTheme.colors.accentPrimary),
            ) {
                Text(text = stringResource(R.string.settings_idle_image))
            }

            Button(
                onClick = { expandedImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = StreamLauncherTheme.colors.accentSecondary),
            ) {
                Text(text = stringResource(R.string.settings_expanded_image))
            }

            OutlinedButton(
                onClick = showResetDialogEvent,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(text = stringResource(R.string.settings_image_reset))
            }
        }
    }
}

@Composable
private fun ImageSettingsGrid(
    selectedCell: GridCell,
    gridCellImages: Map<GridCell, GridCellImage>,
    selectCellEvent: (GridCell) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accentPrimary = StreamLauncherTheme.colors.accentPrimary
    val accentSecondary = StreamLauncherTheme.colors.accentSecondary
    val cellShape = RoundedCornerShape(8.dp)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf(
            GridCell.TOP_LEFT to GridCell.TOP_RIGHT,
            GridCell.BOTTOM_LEFT to GridCell.BOTTOM_RIGHT,
        ).forEach { (left, right) ->
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(left, right).forEach { cell ->
                    val isSelected = selectedCell == cell
                    val cellImage = gridCellImages[cell]

                    Surface(
                        shape = cellShape,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) accentPrimary else MaterialTheme.colorScheme.outlineVariant,
                                shape = cellShape,
                            )
                            .clickable { selectCellEvent(cell) },
                    ) {
                        GridCellContent(
                            cell = cell,
                            idleUri = cellImage?.idleImageUri,
                            expandedUri = cellImage?.expandedImageUri,
                            accentPrimary = accentPrimary,
                            accentSecondary = accentSecondary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GridCellContent(
    cell: GridCell,
    idleUri: Any?,
    expandedUri: Any?,
    accentPrimary: Color,
    accentSecondary: Color,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize(),
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            if (idleUri != null) {
                CellImageLayer(
                    imageUri = idleUri,
                    labelText = stringResource(R.string.settings_idle_label),
                    labelColor = accentPrimary.copy(alpha = 0.8f),
                    modifier = Modifier.weight(1f),
                )
            }
            if (expandedUri != null) {
                CellImageLayer(
                    imageUri = expandedUri,
                    labelText = stringResource(R.string.settings_expanded_label),
                    labelColor = accentSecondary.copy(alpha = 0.8f),
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Text(
            text = when (cell) {
                GridCell.TOP_LEFT -> stringResource(R.string.grid_cell_top_left)
                GridCell.TOP_RIGHT -> stringResource(R.string.grid_cell_top_right)
                GridCell.BOTTOM_LEFT -> stringResource(R.string.grid_cell_bottom_left)
                GridCell.BOTTOM_RIGHT -> stringResource(R.string.grid_cell_bottom_right)
            },
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
        )
    }
}

@Composable
private fun CellImageLayer(
    imageUri: Any?,
    labelText: String,
    labelColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxHeight(),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUri)
                .crossfade(300)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f)),
        )
        Text(
            text = labelText,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 4.dp)
                .background(
                    color = labelColor,
                    shape = RoundedCornerShape(4.dp),
                )
                .padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}
