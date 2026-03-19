package org.comon.streamlauncher.data.datasource

import kotlinx.coroutines.flow.Flow
import org.comon.streamlauncher.domain.model.preset.MarketUser

interface MarketAuthDataSource {
    fun authStateChanges(): Flow<MarketUser?>
    fun getCurrentUser(): MarketUser?
    fun getCurrentUserId(): String?
    suspend fun signInWithGoogle(idToken: String): MarketUser
    suspend fun signOut()
}
