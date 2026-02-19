package org.comon.streamlauncher.data.repository

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.comon.streamlauncher.domain.model.AppEntity
import org.comon.streamlauncher.domain.repository.AppRepository
import javax.inject.Inject

class AppRepositoryImpl @Inject constructor(
    private val packageManager: PackageManager,
) : AppRepository {

    override fun getInstalledApps(): Flow<List<AppEntity>> = flow {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfoList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(intent, 0)
        }
        val apps = resolveInfoList
            .filter { it.activityInfo != null }
            .map { it.toAppEntity() }
        emit(apps)
    }.flowOn(Dispatchers.IO)

    private fun ResolveInfo.toAppEntity(): AppEntity = AppEntity(
        packageName = activityInfo.packageName,
        label = loadLabel(packageManager).toString(),
        activityName = activityInfo.name,
    )
}
