package org.comon.streamlauncher.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.comon.streamlauncher.data.repository.LiveWallpaperRepositoryImpl
import org.comon.streamlauncher.domain.repository.LiveWallpaperRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LiveWallpaperModule {

    @Binds
    @Singleton
    abstract fun bindLiveWallpaperRepository(impl: LiveWallpaperRepositoryImpl): LiveWallpaperRepository
}
