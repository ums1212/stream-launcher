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
        val isInsert = preset.id == 0 || presetDao.getById(preset.id) == null
        if (isInsert && presetDao.getPresetCount() >= 10) {
            throw IllegalStateException("프리셋은 최대 10개까지 저장할 수 있습니다.")
        }
        presetDao.insertPreset(preset.toEntity())
    }

    override suspend fun deletePreset(preset: Preset) = withContext(Dispatchers.IO) {
        presetDao.deletePreset(preset.toEntity())
    }

    override suspend fun isDownloadedByMarketId(marketPresetId: String): Boolean = withContext(Dispatchers.IO) {
        presetDao.existsByMarketPresetId(marketPresetId)
    }

    override suspend fun updateMarketPresetId(presetId: Int, marketPresetId: String): Int = withContext(Dispatchers.IO) {
        presetDao.updateMarketPresetId(presetId, marketPresetId)
    }
}
