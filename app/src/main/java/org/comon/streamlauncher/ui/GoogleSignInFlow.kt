package org.comon.streamlauncher.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.comon.streamlauncher.preset_market.ui.GoogleSignInHandler
import org.comon.streamlauncher.ui.component.GoogleSignInRequiredDialog

/**
 * Google 로그인 흐름을 캡슐화한 컴포저블.
 *
 * - RequireSignIn SideEffect → 반환된 람다 호출 → 확인 다이얼로그 표시 → GoogleSignInHandler 실행
 * - onRequireSignIn 직접 호출 경로 (SettingsDetailScreen) → 반환된 람다 호출
 *
 * @return 로그인 다이얼로그를 트리거하는 람다 (`showSettingsSignInDialog = true`)
 */
@Composable
fun GoogleSignInFlow(
    onSignInSuccess: (String) -> Unit,
    onSignInFailure: (String) -> Unit,
): () -> Unit {
    var showSignIn by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        GoogleSignInRequiredDialog(
            onConfirm = {
                showDialog = false
                showSignIn = true
            },
            onDismiss = { showDialog = false },
        )
    }

    if (showSignIn) {
        GoogleSignInHandler(
            onSignInSuccess = onSignInSuccess,
            onSignInFailure = onSignInFailure,
            onDismiss = { showSignIn = false },
        )
    }

    return { showDialog = true }
}
