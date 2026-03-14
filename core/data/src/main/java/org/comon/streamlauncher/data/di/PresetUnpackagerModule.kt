package org.comon.streamlauncher.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.comon.streamlauncher.data.slp.PresetUnpackagerImpl
import org.comon.streamlauncher.domain.repository.PresetUnpackager

@Module
@InstallIn(SingletonComponent::class)
abstract class PresetUnpackagerModule {

    @Binds
    abstract fun bindPresetUnpackager(impl: PresetUnpackagerImpl): PresetUnpackager
}
