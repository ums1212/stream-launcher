package org.comon.streamlauncher.preset_market.ui

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import org.comon.streamlauncher.preset_market.R
import org.comon.streamlauncher.preset_market.ReportPresetIntent
import org.comon.streamlauncher.preset_market.ReportPresetSideEffect
import org.comon.streamlauncher.preset_market.ReportPresetViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportPresetScreen(
    onBack: () -> Unit,
    onReportSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReportPresetViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val networkErrorMessage = stringResource(R.string.preset_market_report_network_error)
    val networkSettingsLabel = stringResource(R.string.preset_market_report_network_settings)

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        viewModel.handleIntent(ReportPresetIntent.SelectImage(uri.toString()))
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ReportPresetSideEffect.ReportSuccess -> onReportSuccess()
                is ReportPresetSideEffect.ShowError ->
                    snackbarHostState.showSnackbar(message = effect.message)
                is ReportPresetSideEffect.ShowNetworkError -> {
                    val result = snackbarHostState.showSnackbar(
                        message = networkErrorMessage,
                        actionLabel = networkSettingsLabel,
                        duration = SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        try {
                            context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            })
                        } catch (_: Exception) {}
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.preset_market_report_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { snackbarData ->
                SwipeToDismissBox(
                    state = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value != SwipeToDismissBoxValue.Settled) {
                                snackbarData.dismiss()
                                true
                            } else false
                        }
                    ),
                    backgroundContent = {},
                ) {
                    Snackbar(snackbarData)
                }
            }
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Button(
                    onClick = { viewModel.handleIntent(ReportPresetIntent.Submit) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    enabled = !state.isSubmitting && state.reason.isNotBlank(),
                ) {
                    if (state.isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(stringResource(R.string.preset_market_report_submit))
                }
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // 신고 정보 섹션
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ReportInfoRow(
                        label = stringResource(R.string.preset_market_report_reporter),
                        value = state.reporterDisplayName,
                    )
                    HorizontalDivider()
                    ReportInfoRow(
                        label = stringResource(R.string.preset_market_report_author),
                        value = state.presetAuthorDisplayName,
                    )
                    HorizontalDivider()
                    ReportInfoRow(
                        label = stringResource(R.string.preset_market_report_preset_name),
                        value = state.presetName,
                    )
                }
            }

            // 신고 사유 입력
            OutlinedTextField(
                value = state.reason,
                onValueChange = { viewModel.handleIntent(ReportPresetIntent.UpdateReason(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.preset_market_report_reason_hint)) },
                minLines = 4,
                enabled = !state.isSubmitting,
            )

            // 이미지 첨부 버튼
            OutlinedButton(
                onClick = {
                    imageLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSubmitting,
            ) {
                Icon(
                    imageVector = Icons.Default.AddPhotoAlternate,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (state.selectedImageUri != null) {
                        stringResource(R.string.preset_market_report_image_change)
                    } else {
                        stringResource(R.string.preset_market_report_image_add)
                    }
                )
            }

            // 선택된 이미지 썸네일
            if (state.selectedImageUri != null) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    AsyncImage(
                        model = state.selectedImageUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    IconButton(
                        onClick = { viewModel.handleIntent(ReportPresetIntent.RemoveImage) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(28.dp),
                        enabled = !state.isSubmitting,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cancel,
                            contentDescription = stringResource(R.string.preset_market_report_image_remove),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ReportInfoRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
