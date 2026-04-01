package org.comon.streamlauncher.data.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.comon.streamlauncher.data.local.room.AppDatabase
import org.comon.streamlauncher.data.local.room.livewallpaper.LiveWallpaperDao
import org.comon.streamlauncher.data.local.room.preset.PresetDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "streamlauncher_db"
        )
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6,
                AppDatabase.MIGRATION_6_7,
            )
            .build()
    }

    @Provides
    fun providePresetDao(database: AppDatabase): PresetDao = database.presetDao()

    @Provides
    fun provideLiveWallpaperDao(database: AppDatabase): LiveWallpaperDao = database.liveWallpaperDao()
}
