package org.comon.streamlauncher.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.comon.streamlauncher.data.slp.PresetPackagerImpl
import org.comon.streamlauncher.domain.repository.PresetPackager

@Module
@InstallIn(SingletonComponent::class)
abstract class PresetPackagerModule {

    @Binds
    abstract fun bindPresetPackager(impl: PresetPackagerImpl): PresetPackager
}
