package org.comon.streamlauncher.domain.repository

import kotlinx.coroutines.flow.Flow
import org.comon.streamlauncher.domain.model.preset.Preset

interface PresetRepository {
    fun getAllPresets(): Flow<List<Preset>>
    suspend fun savePreset(preset: Preset)
    suspend fun deletePreset(preset: Preset)
}
