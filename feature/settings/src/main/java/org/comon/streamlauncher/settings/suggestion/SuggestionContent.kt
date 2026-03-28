package org.comon.streamlauncher.settings.suggestion

import android.content.Intent
import android.os.Build
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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import org.comon.streamlauncher.settings.R

@Composable
fun SuggestionContent(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SuggestionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val appVersion = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
        }.getOrDefault("")
    }
    val deviceInfo = remember {
        "${Build.MANUFACTURER} ${Build.MODEL} (API ${Build.VERSION.SDK_INT})"
    }

    val networkErrorMessage = stringResource(R.string.suggestion_network_error)
    val networkSettingsLabel = stringResource(R.string.suggestion_network_settings)
    val successMessage = stringResource(R.string.suggestion_success)
    val imageFormatErrorMessage = stringResource(R.string.suggestion_image_format_error)

    val allowedMimeTypes = remember { setOf("image/png", "image/jpeg", "image/webp") }

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val mimeType = context.contentResolver.getType(uri)
        if (mimeType !in allowedMimeTypes) {
            viewModel.handleIntent(SuggestionIntent.ShowImageFormatError(imageFormatErrorMessage))
            return@rememberLauncherForActivityResult
        }
        viewModel.handleIntent(SuggestionIntent.SelectImage(uri.toString()))
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is SuggestionSideEffect.SubmitSuccess -> {
                    snackbarHostState.showSnackbar(message = successMessage)
                    onBack()
                }
                is SuggestionSideEffect.ShowError ->
                    snackbarHostState.showSnackbar(message = effect.message)
                is SuggestionSideEffect.ShowNetworkError -> {
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

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 이메일 입력 (선택)
            OutlinedTextField(
                value = state.email,
                onValueChange = { viewModel.handleIntent(SuggestionIntent.UpdateEmail(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.suggestion_email_label)) },
                placeholder = { Text(stringResource(R.string.suggestion_email_hint)) },
                singleLine = true,
                enabled = !state.isSubmitting,
            )

            // 건의 내용 입력
            OutlinedTextField(
                value = state.body,
                onValueChange = { viewModel.handleIntent(SuggestionIntent.UpdateBody(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.suggestion_body_label)) },
                placeholder = { Text(stringResource(R.string.suggestion_body_hint)) },
                minLines = 6,
                enabled = !state.isSubmitting,
            )

            // 스크린샷 첨부 버튼
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
                        stringResource(R.string.suggestion_image_change)
                    } else {
                        stringResource(R.string.suggestion_image_add)
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
                        onClick = { viewModel.handleIntent(SuggestionIntent.RemoveImage) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(28.dp),
                        enabled = !state.isSubmitting,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cancel,
                            contentDescription = stringResource(R.string.suggestion_image_remove),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            // 제출 버튼
            Button(
                onClick = {
                    viewModel.handleIntent(SuggestionIntent.Submit(appVersion, deviceInfo))
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSubmitting && state.body.isNotBlank(),
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(stringResource(R.string.suggestion_submit))
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        ) { snackbarData ->
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
    }
}
