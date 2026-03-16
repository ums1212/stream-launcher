package org.comon.streamlauncher.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest

@Composable
fun AppIcon(packageName: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val appContext = remember(context) { context.applicationContext }
    val painter = rememberAsyncImagePainter(
        model = remember(packageName, appContext) {
            ImageRequest.Builder(appContext)
                .data(AppIconData(packageName)) // Custom Fetcher 전용 모델로 변환하여 로드 요청
                .crossfade(false)
                .build()
        }
    )

    if (painter.state is AsyncImagePainter.State.Error) {
        // 설치되지 않았거나 로드에 실패한 기본 플레이스홀더
        Box(
            modifier = modifier.background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
            ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.AccountBox,
                contentDescription = null,
            )
        }
    } else {
        Image(
            painter = painter,
            contentDescription = null,
            modifier = modifier,
        )
    }
}
