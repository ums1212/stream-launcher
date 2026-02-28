package org.comon.streamlauncher.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.comon.streamlauncher.data.local.room.preset.PresetDao
import org.comon.streamlauncher.data.local.room.preset.toDomain
import org.comon.streamlauncher.data.local.room.preset.toEntity
import org.comon.streamlauncher.domain.model.preset.Preset
import org.comon.streamlauncher.domain.repository.PresetRepository
import javax.inject.Inject

class PresetRepositoryImpl @Inject constructor(
    private val presetDao: PresetDao
) : PresetRepository {

    override fun getAllPresets(): Flow<List<Preset>> {
        return presetDao.getAllPresets().map { entities -> 
            entities.map { it.toDomain() }
        }
    }

    override suspend fun savePreset(preset: Preset) = withContext(Dispatchers.IO) {
        // Enforce the 10 presets limit
        val currentCount = presetDao.getPresetCount()
        if (currentCount >= 10 && preset.id == 0) { // Only delete if inserting a new one
            presetDao.deleteOldestPreset()
        }
        presetDao.insertPreset(preset.toEntity())
    }

    override suspend fun deletePreset(preset: Preset) = withContext(Dispatchers.IO) {
        presetDao.deletePreset(preset.toEntity())
    }
}
