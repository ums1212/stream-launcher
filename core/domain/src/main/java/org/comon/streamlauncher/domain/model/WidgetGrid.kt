package org.comon.streamlauncher.domain.model

object WidgetGrid {
    const val CELL_SIZE_DP = 70  // Android 표준 런처 셀 크기

    /** 화면 크기(dp)에서 그리드 열 수 계산 */
    fun computeColumns(areaWidthDp: Int): Int = (areaWidthDp / CELL_SIZE_DP).coerceAtLeast(3)

    /** 화면 크기(dp)에서 그리드 행 수 계산 */
    fun computeRows(areaHeightDp: Int): Int = (areaHeightDp / CELL_SIZE_DP).coerceAtLeast(4)
}
