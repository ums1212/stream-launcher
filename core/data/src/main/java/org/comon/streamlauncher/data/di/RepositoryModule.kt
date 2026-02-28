package org.comon.streamlauncher.data.di

import android.content.Context
import android.content.pm.PackageManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.comon.streamlauncher.data.repository.AppRepositoryImpl
import org.comon.streamlauncher.domain.repository.AppRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAppRepository(impl: AppRepositoryImpl): AppRepository

    @Binds
    @Singleton
    abstract fun bindPresetRepository(impl: org.comon.streamlauncher.data.repository.PresetRepositoryImpl): org.comon.streamlauncher.domain.repository.PresetRepository

    @Binds
    @Singleton
    abstract fun bindWallpaperHelper(impl: org.comon.streamlauncher.data.util.WallpaperHelperImpl): org.comon.streamlauncher.domain.util.WallpaperHelper

    companion object {
        @Provides
        @Singleton
        fun providePackageManager(
            @ApplicationContext context: Context,
        ): PackageManager = context.packageManager
    }
}
