package org.comon.streamlauncher.data.repository

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import org.comon.streamlauncher.domain.model.AppEntity
import org.comon.streamlauncher.domain.repository.AppRepository
import javax.inject.Inject

class AppRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val packageManager: PackageManager,
) : AppRepository {

    override fun getInstalledApps(): Flow<List<AppEntity>> = callbackFlow {
        // 현재 설치된 앱 목록 조회
        fun queryApps(): List<AppEntity> {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfoList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                packageManager.queryIntentActivities(intent, 0)
            }
            return resolveInfoList
                .filter { it.activityInfo != null }
                .map { it.toAppEntity() }
        }

        // 초기 앱 목록 emit
        trySend(queryApps())

        // 패키지 설치/삭제/업데이트 이벤트 수신 → 앱 목록 재조회
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                trySend(queryApps())
            }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }

        context.registerReceiver(receiver, filter)

        awaitClose {
            context.unregisterReceiver(receiver)
        }
    }.flowOn(Dispatchers.IO)

    private fun ResolveInfo.toAppEntity(): AppEntity = AppEntity(
        packageName = activityInfo.packageName,
        label = loadLabel(packageManager).toString(),
        activityName = activityInfo.name,
    )
}
