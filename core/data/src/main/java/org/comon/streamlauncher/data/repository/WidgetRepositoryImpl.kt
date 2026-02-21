package org.comon.streamlauncher.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.comon.streamlauncher.domain.repository.WidgetRepository
import org.comon.streamlauncher.domain.repository.WidgetRepository.Companion.INVALID_WIDGET_ID
import org.comon.streamlauncher.domain.repository.WidgetRepository.Companion.MAX_WIDGETS
import javax.inject.Inject

private val Context.widgetDataStore by preferencesDataStore(name = "widget_prefs")

class WidgetRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : WidgetRepository {

    // 현재 포맷: "id0,id1,-1,-1,-1,-1" — 고정 MAX_WIDGETS 칸, -1 = 빈 슬롯
    private val widgetSlotsKey = stringPreferencesKey("widget_slots")

    // 레거시 키 (마이그레이션 전용)
    private val legacyPackedKey = stringPreferencesKey("widget_ids")   // Step 14: 팩드 리스트
    private val legacySingleKey = intPreferencesKey("widget_id")        // Step 13: 단일 int

    override fun getWidgetSlots(): Flow<List<Int?>> =
        context.widgetDataStore.data.map { prefs ->
            parseSlots(prefs[widgetSlotsKey])
        }

    override suspend fun setWidgetAtSlot(slot: Int, id: Int) {
        if (slot !in 0 until MAX_WIDGETS) return
        context.widgetDataStore.edit { prefs ->
            val current = parseSlots(prefs[widgetSlotsKey]).toMutableList()
            current[slot] = id
            prefs[widgetSlotsKey] = encodeSlots(current)
        }
    }

    override suspend fun clearSlot(slot: Int) {
        if (slot !in 0 until MAX_WIDGETS) return
        context.widgetDataStore.edit { prefs ->
            val current = parseSlots(prefs[widgetSlotsKey]).toMutableList()
            current[slot] = null
            prefs[widgetSlotsKey] = encodeSlots(current)
        }
    }

    override suspend fun migrateLegacyData() {
        context.widgetDataStore.edit { prefs ->
            // 이미 새 포맷이 있으면 마이그레이션 불필요
            if (prefs[widgetSlotsKey] != null) return@edit

            val slots = MutableList<Int?>(MAX_WIDGETS) { null }

            // Step 14 포맷: 팩드 콤마 리스트 ("id1,id2")
            val packed = prefs[legacyPackedKey]
            if (packed != null) {
                packed.split(",")
                    .filter { it.isNotBlank() }
                    .mapNotNull { it.trim().toIntOrNull() }
                    .filter { it != INVALID_WIDGET_ID }
                    .take(MAX_WIDGETS)
                    .forEachIndexed { i, id -> slots[i] = id }
                prefs.remove(legacyPackedKey)
            }

            // Step 13 포맷: 단일 int
            val singleId = prefs[legacySingleKey]
            if (singleId != null && singleId != INVALID_WIDGET_ID && slots.all { it == null }) {
                slots[0] = singleId
            }
            if (singleId != null) prefs.remove(legacySingleKey)

            prefs[widgetSlotsKey] = encodeSlots(slots)
        }
    }

    // "id0,id1,-1,-1,-1,-1" → List<Int?> (size = MAX_WIDGETS)
    private fun parseSlots(raw: String?): List<Int?> {
        val parts = raw?.split(",") ?: return List(MAX_WIDGETS) { null }
        return List(MAX_WIDGETS) { i ->
            val v = parts.getOrNull(i)?.trim()?.toIntOrNull()
            if (v == null || v == INVALID_WIDGET_ID) null else v
        }
    }

    // List<Int?> → "id0,-1,-1,-1,-1,id5"
    private fun encodeSlots(slots: List<Int?>): String =
        slots.joinToString(",") { it?.toString() ?: "$INVALID_WIDGET_ID" }
}
