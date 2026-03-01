package org.comon.streamlauncher.data.local.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.comon.streamlauncher.data.local.room.preset.PresetDao
import org.comon.streamlauncher.data.local.room.preset.PresetEntity

@Database(entities = [PresetEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun presetDao(): PresetDao

    companion object {
        // youtubeChannelName 컬럼 제거
        // SQLite는 DROP COLUMN을 지원하지 않으므로 테이블 재생성 방식 사용
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE presets_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        hasTopLeftImage INTEGER NOT NULL,
                        hasTopRightImage INTEGER NOT NULL,
                        hasBottomLeftImage INTEGER NOT NULL,
                        hasBottomRightImage INTEGER NOT NULL,
                        topLeftIdleUri TEXT,
                        topLeftExpandedUri TEXT,
                        topRightIdleUri TEXT,
                        topRightExpandedUri TEXT,
                        bottomLeftIdleUri TEXT,
                        bottomLeftExpandedUri TEXT,
                        bottomRightIdleUri TEXT,
                        bottomRightExpandedUri TEXT,
                        hasFeedSettings INTEGER NOT NULL,
                        useFeed INTEGER NOT NULL,
                        youtubeChannelId TEXT NOT NULL,
                        chzzkChannelId TEXT NOT NULL,
                        hasAppDrawerSettings INTEGER NOT NULL,
                        appDrawerColumns INTEGER NOT NULL,
                        appDrawerRows INTEGER NOT NULL,
                        appDrawerIconSizeRatio REAL NOT NULL,
                        hasWallpaperSettings INTEGER NOT NULL,
                        wallpaperUri TEXT,
                        enableParallax INTEGER NOT NULL,
                        hasThemeSettings INTEGER NOT NULL,
                        themeColorHex TEXT,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO presets_new SELECT
                        id, name,
                        hasTopLeftImage, hasTopRightImage, hasBottomLeftImage, hasBottomRightImage,
                        topLeftIdleUri, topLeftExpandedUri, topRightIdleUri, topRightExpandedUri,
                        bottomLeftIdleUri, bottomLeftExpandedUri, bottomRightIdleUri, bottomRightExpandedUri,
                        hasFeedSettings, useFeed, youtubeChannelId, chzzkChannelId,
                        hasAppDrawerSettings, appDrawerColumns, appDrawerRows, appDrawerIconSizeRatio,
                        hasWallpaperSettings, wallpaperUri, enableParallax,
                        hasThemeSettings, themeColorHex, createdAt
                    FROM presets
                """.trimIndent())
                db.execSQL("DROP TABLE presets")
                db.execSQL("ALTER TABLE presets_new RENAME TO presets")
            }
        }
    }
}
