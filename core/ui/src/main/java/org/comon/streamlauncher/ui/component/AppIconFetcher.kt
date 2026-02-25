package org.comon.streamlauncher.ui.component

import android.content.pm.PackageManager
import android.graphics.drawable.BitmapDrawable
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.request.Options
import androidx.core.graphics.drawable.toDrawable
import coil.fetch.FetchResult
import coil.fetch.Fetcher

/** App 아이콘 로드를 요청하기 위한 커스텀 데이터 모델 */
data class AppIconData(val packageName: String)

/**
 * Android PackageManager를 통해 App Icon을 직접 가져와서 
 * Coil 이미지 로딩 파이프라인 (메모리, 디스크 캐시, 하드웨어 비트맵 가속)에 태울 수 있도록 하는 Custom Fetcher.
 */
class AppIconFetcher(
    private val data: AppIconData,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        val packageManager = options.context.packageManager
        
        return try {
            val drawable = packageManager.getApplicationIcon(data.packageName)
            // BitmapDrawable인 경우 그대로 사용, 아닐 경우 (AdaptiveIconDrawable 등) 비트맵으로 변환하여 반환
            val optimizedDrawable = drawable as? BitmapDrawable ?: drawable.toBitmap().toDrawable(options.context.resources)

            DrawableResult(
                drawable = optimizedDrawable,
                isSampled = false,
                dataSource = DataSource.DISK
            )
        } catch (e: PackageManager.NameNotFoundException) {
            // 예외(앱이 지워졌거나 찾을 수 없음)가 발생한 경우
            // Coil 라이브러리는 이를 "이미지 로드 실패(Error)"로 간주하여 기본 아이콘으로 대체할 수 있게
            null
        }
    }

    class Factory() : Fetcher.Factory<AppIconData> {
        override fun create(
            data: AppIconData, 
            options: Options, 
            imageLoader: ImageLoader
        ): Fetcher {
            return AppIconFetcher(data, options)
        }
    }
}
