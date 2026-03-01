package org.comon.streamlauncher.preset_market.ui

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import org.comon.streamlauncher.preset_market.BuildConfig

/**
 * Google Sign-In 플로우를 처리하는 컴포저블.
 * Credential Manager API 기반.
 *
 * 1차 시도: GetGoogleIdOption (이전에 승인한 계정 우선, filterByAuthorized=false)
 * 2차 시도: NoCredentialException 발생 시 GetSignInWithGoogleOption으로 fallback
 *           (계정 선택 바텀시트를 강제로 표시)
 *
 * [onSignInSuccess]: 로그인 성공 시 idToken 전달
 * [onSignInFailure]: 취소가 아닌 실패 시 에러 메시지 전달
 * [onDismiss]: 성공/실패/취소 후 호출 (showSignIn 상태 해제용)
 */
@Composable
fun GoogleSignInHandler(
    onSignInSuccess: (idToken: String) -> Unit,
    onSignInFailure: (message: String) -> Unit = {},
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        try {
            val credentialManager = CredentialManager.create(context)
            val idToken = tryGetGoogleIdToken(credentialManager, context)
            if (idToken != null) {
                onSignInSuccess(idToken)
            } else {
                onSignInFailure("지원하지 않는 인증 방식입니다.")
            }
        } catch (e: GetCredentialCancellationException) {
            // 사용자가 직접 취소 — 에러 메시지 불필요
        } catch (e: GetCredentialException) {
            onSignInFailure("로그인에 실패했습니다: ${e.message}")
        } finally {
            onDismiss()
        }
    }
}

/**
 * 1차: GetGoogleIdOption → NoCredentialException 시 GetSignInWithGoogleOption으로 fallback
 */
private suspend fun tryGetGoogleIdToken(
    credentialManager: CredentialManager,
    context: android.content.Context,
): String? {
    return try {
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(
                GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(BuildConfig.WEB_CLIENT_ID)
                    .build()
            )
            .build()
        extractIdToken(credentialManager.getCredential(context, request))
    } catch (e: NoCredentialException) {
        // 계정이 없거나 SHA-1 미등록 등의 이유로 실패 → 계정 선택 화면 강제 표시
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(
                GetSignInWithGoogleOption.Builder(BuildConfig.WEB_CLIENT_ID).build()
            )
            .build()
        extractIdToken(credentialManager.getCredential(context, request))
    }
}

private fun extractIdToken(
    result: androidx.credentials.GetCredentialResponse,
): String? {
    val credential = result.credential
    return if (credential is CustomCredential &&
        credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
    ) {
        GoogleIdTokenCredential.createFrom(credential.data).idToken
    } else {
        null
    }
}
