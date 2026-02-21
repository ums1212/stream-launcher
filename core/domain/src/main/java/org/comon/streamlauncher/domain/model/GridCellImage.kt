package org.comon.streamlauncher.domain.model

/**
 * 그리드 셀별 이미지 URI.
 *
 * - [idleImageUri]: 셀이 축소(기본) 상태일 때 표시할 이미지 URI
 * - [expandedImageUri]: 셀이 확장 상태일 때 표시할 이미지 URI
 *
 * URI 권한 유지는 향후 core:data의 SettingsRepository에서 처리.
 */
data class GridCellImage(
    val cell: GridCell,
    val idleImageUri: String? = null,
    val expandedImageUri: String? = null,
)
