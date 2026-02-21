package org.comon.streamlauncher.domain.repository

import kotlinx.coroutines.flow.Flow

interface WidgetRepository {
    /** 고정 MAX_WIDGETS 크기. null = 빈 슬롯, Int = 위젯 ID */
    fun getWidgetSlots(): Flow<List<Int?>>
    suspend fun setWidgetAtSlot(slot: Int, id: Int)
    suspend fun clearSlot(slot: Int)
    suspend fun migrateLegacyData() {}

    companion object {
        const val INVALID_WIDGET_ID = -1
        const val MAX_WIDGETS = 6
    }
}
