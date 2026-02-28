package org.comon.streamlauncher.data.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.comon.streamlauncher.data.local.room.AppDatabase
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
        ).build()
    }
    
    @Provides
    fun providePresetDao(database: AppDatabase): PresetDao {
        return database.presetDao()
    }
}
