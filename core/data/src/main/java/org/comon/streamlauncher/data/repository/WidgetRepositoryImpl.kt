package org.comon.streamlauncher.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.comon.streamlauncher.domain.model.WidgetPlacement
import org.comon.streamlauncher.domain.repository.WidgetRepository
import org.comon.streamlauncher.domain.repository.WidgetRepository.Companion.INVALID_WIDGET_ID
import javax.inject.Inject

private val Context.widgetDataStore by preferencesDataStore(name = "widget_prefs")

@Serializable
private data class WidgetPlacementDto(
    val id: Int,
    val col: Int,
    val row: Int,
    val colSpan: Int,
    val rowSpan: Int,
    val minColSpan: Int = 1,
    val minRowSpan: Int = 1,
)

class WidgetRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : WidgetRepository {

    private val widgetPlacementsKey = stringPreferencesKey("widget_placements")

    // 레거시 키 (마이그레이션 전용)
    private val legacySlotsKey = stringPreferencesKey("widget_slots")     // 현재 포맷: CSV
    private val legacyPackedKey = stringPreferencesKey("widget_ids")      // Step 14: 팩드 리스트
    private val legacySingleKey = intPreferencesKey("widget_id")          // Step 13: 단일 int

    private val json = Json { ignoreUnknownKeys = true }

    override fun getWidgetPlacements(): Flow<List<WidgetPlacement>> =
        context.widgetDataStore.data.map { prefs ->
            parsePlacements(prefs[widgetPlacementsKey])
        }

    override suspend fun addWidget(placement: WidgetPlacement) {
        context.widgetDataStore.edit { prefs ->
            val current = parsePlacements(prefs[widgetPlacementsKey]).toMutableList()
            current.removeAll { it.appWidgetId == placement.appWidgetId }
            current.add(placement)
            prefs[widgetPlacementsKey] = encodePlacements(current)
        }
    }

    override suspend fun removeWidget(appWidgetId: Int) {
        context.widgetDataStore.edit { prefs ->
            val current = parsePlacements(prefs[widgetPlacementsKey]).toMutableList()
            current.removeAll { it.appWidgetId == appWidgetId }
            prefs[widgetPlacementsKey] = encodePlacements(current)
        }
    }

    override suspend fun updateWidgetPlacement(appWidgetId: Int, column: Int, row: Int) {
        context.widgetDataStore.edit { prefs ->
            val current = parsePlacements(prefs[widgetPlacementsKey]).toMutableList()
            val index = current.indexOfFirst { it.appWidgetId == appWidgetId }
            if (index >= 0) {
                current[index] = current[index].copy(column = column, row = row)
            }
            prefs[widgetPlacementsKey] = encodePlacements(current)
        }
    }

    override suspend fun updateWidgetSize(appWidgetId: Int, columnSpan: Int, rowSpan: Int) {
        context.widgetDataStore.edit { prefs ->
            val current = parsePlacements(prefs[widgetPlacementsKey]).toMutableList()
            val index = current.indexOfFirst { it.appWidgetId == appWidgetId }
            if (index >= 0) {
                current[index] = current[index].copy(columnSpan = columnSpan, rowSpan = rowSpan)
            }
            prefs[widgetPlacementsKey] = encodePlacements(current)
        }
    }

    override suspend fun migrateLegacySlots() {
        context.widgetDataStore.edit { prefs ->
            // 이미 새 포맷이 있으면 마이그레이션 불필요
            if (prefs[widgetPlacementsKey] != null) return@edit

            val dtos = mutableListOf<WidgetPlacementDto>()

            // Step 3: widget_slots CSV → JSON (슬롯 index i → col=(i%2)*2, row=(i/2)*2)
            val slotsStr = prefs[legacySlotsKey]
            if (slotsStr != null) {
                slotsStr.split(",").forEachIndexed { i, raw ->
                    val id = raw.trim().toIntOrNull()
                    if (id != null && id != INVALID_WIDGET_ID) {
                        dtos.add(WidgetPlacementDto(id = id, col = (i % 2) * 2, row = (i / 2) * 2, colSpan = 2, rowSpan = 2))
                    }
                }
                prefs.remove(legacySlotsKey)
            } else {
                // Step 2: widget_ids 팩드 리스트 → JSON
                val packed = prefs[legacyPackedKey]
                if (packed != null) {
                    packed.split(",")
                        .filter { it.isNotBlank() }
                        .mapNotNull { it.trim().toIntOrNull() }
                        .filter { it != INVALID_WIDGET_ID }
                        .forEachIndexed { i, id ->
                            dtos.add(WidgetPlacementDto(id = id, col = (i % 2) * 2, row = (i / 2) * 2, colSpan = 2, rowSpan = 2))
                        }
                    prefs.remove(legacyPackedKey)
                } else {
                    // Step 1: widget_id 단일 int → JSON
                    val singleId = prefs[legacySingleKey]
                    if (singleId != null && singleId != INVALID_WIDGET_ID) {
                        dtos.add(WidgetPlacementDto(id = singleId, col = 0, row = 0, colSpan = 2, rowSpan = 2))
                    }
                    if (singleId != null) prefs.remove(legacySingleKey)
                }
            }

            prefs[widgetPlacementsKey] = json.encodeToString(dtos)
        }
    }

    private fun parsePlacements(raw: String?): List<WidgetPlacement> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString<List<WidgetPlacementDto>>(raw).map { it.toDomain() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun encodePlacements(placements: List<WidgetPlacement>): String =
        json.encodeToString(placements.map { it.toDto() })

    private fun WidgetPlacementDto.toDomain() = WidgetPlacement(
        appWidgetId = id,
        column = col,
        row = row,
        columnSpan = colSpan,
        rowSpan = rowSpan,
        minColumnSpan = minColSpan,
        minRowSpan = minRowSpan,
    )

    private fun WidgetPlacement.toDto() = WidgetPlacementDto(
        id = appWidgetId,
        col = column,
        row = row,
        colSpan = columnSpan,
        rowSpan = rowSpan,
        minColSpan = minColumnSpan,
        minRowSpan = minRowSpan,
    )
}
