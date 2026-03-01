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
import org.comon.streamlauncher.data.repository.MarketPresetRepositoryImpl
import org.comon.streamlauncher.domain.repository.AppRepository
import org.comon.streamlauncher.domain.repository.MarketPresetRepository
import javax.inject.Singleton

@Suppress("unused") // Hilt @Binds 함수는 코드에서 직접 호출되지 않고 DI 프레임워크가 사용
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

    @Binds
    @Singleton
    abstract fun bindMarketPresetRepository(impl: MarketPresetRepositoryImpl): MarketPresetRepository

    companion object {
        @Provides
        @Singleton
        fun providePackageManager(
            @ApplicationContext context: Context,
        ): PackageManager = context.packageManager
    }
}
