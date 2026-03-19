package org.comon.streamlauncher.data.datasource

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import org.comon.streamlauncher.domain.model.preset.MarketUser
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseMarketAuthDataSource @Inject constructor(
    private val auth: FirebaseAuth,
) : MarketAuthDataSource {

    override fun authStateChanges(): Flow<MarketUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser?.let {
                MarketUser(
                    uid = it.uid,
                    displayName = it.displayName ?: "",
                    email = it.email,
                    photoUrl = it.photoUrl?.toString(),
                )
            }
            trySend(user)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override fun getCurrentUser(): MarketUser? {
        val user = auth.currentUser ?: return null
        return MarketUser(
            uid = user.uid,
            displayName = user.displayName ?: "",
            email = user.email,
            photoUrl = user.photoUrl?.toString(),
        )
    }

    override fun getCurrentUserId(): String? = auth.currentUser?.uid

    override suspend fun signInWithGoogle(idToken: String): MarketUser {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(credential).await()
        val user = result.user ?: error("로그인 실패: 유저 정보 없음")
        return MarketUser(
            uid = user.uid,
            displayName = user.displayName ?: "",
            email = user.email,
            photoUrl = user.photoUrl?.toString(),
        )
    }

    override suspend fun signOut() {
        auth.signOut()
    }
}
