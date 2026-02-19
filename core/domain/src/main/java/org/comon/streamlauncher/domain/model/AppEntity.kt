package org.comon.streamlauncher.domain.model

data class AppEntity(
    val packageName: String,  // 앱 고유 ID (e.g. com.kakao.talk)
    val label: String,        // 사용자 표시명 (e.g. 카카오톡)
    val activityName: String, // 메인 액티비티 (e.g. .MainActivity)
)
