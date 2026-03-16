package org.comon.streamlauncher.ui.dragdrop

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import org.comon.streamlauncher.domain.model.AppEntity
import org.comon.streamlauncher.domain.model.GridCell
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DragDropStateTest {

    private val sampleApp = AppEntity(
        packageName = "com.example.app",
        label = "앱",
        activityName = "com.example.app.MainActivity",
    )

    @Test
    fun `endDrag 처리 시 마지막 좌표 기준으로 늦게 등록된 셀 bounds를 다시 반영함`() {
        val state = DragDropState()

        state.startDrag(sampleApp, Offset(10f, 10f))
        state.updateDrag(Offset(100f, 100f))
        state.registerCellBounds(
            GridCell.TOP_LEFT,
            Rect(left = 50f, top = 50f, right = 150f, bottom = 150f),
        )

        val result = state.endDrag()

        assertNotNull(result)
        assertEquals(GridCell.TOP_LEFT, result?.targetCell)
    }

    @Test
    fun `endDrag 처리 시 마지막 좌표가 셀 밖이면 null을 반환함`() {
        val state = DragDropState()

        state.startDrag(sampleApp, Offset(10f, 10f))
        state.updateDrag(Offset(20f, 20f))
        state.registerCellBounds(
            GridCell.TOP_LEFT,
            Rect(left = 50f, top = 50f, right = 150f, bottom = 150f),
        )

        val result = state.endDrag()

        assertNull(result)
    }
}
