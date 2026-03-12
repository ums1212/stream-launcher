package org.comon.streamlauncher.domain.repository

import kotlinx.coroutines.flow.Flow
import org.comon.streamlauncher.domain.model.WidgetPlacement

interface WidgetRepository {
    fun getWidgetPlacements(): Flow<List<WidgetPlacement>>
    suspend fun addWidget(placement: WidgetPlacement)
    suspend fun removeWidget(appWidgetId: Int)
    suspend fun updateWidgetPlacement(appWidgetId: Int, column: Int, row: Int)
    suspend fun updateWidgetSize(appWidgetId: Int, columnSpan: Int, rowSpan: Int)
    suspend fun migrateLegacySlots() {}

    companion object {
        const val INVALID_WIDGET_ID = -1
    }
}
