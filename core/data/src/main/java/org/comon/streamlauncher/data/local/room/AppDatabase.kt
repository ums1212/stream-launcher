package org.comon.streamlauncher.data.local.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.comon.streamlauncher.data.local.room.livewallpaper.LiveWallpaperDao
import org.comon.streamlauncher.data.local.room.livewallpaper.LiveWallpaperEntity
import org.comon.streamlauncher.data.local.room.preset.PresetDao
import org.comon.streamlauncher.data.local.room.preset.PresetEntity

@Database(
    entities = [PresetEntity::class, LiveWallpaperEntity::class],
    version = 7,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun presetDao(): PresetDao
    abstract fun liveWallpaperDao(): LiveWallpaperDao

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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE presets ADD COLUMN marketPresetId TEXT DEFAULT NULL")
            }
        }

        // presets 테이블에 라이브 배경화면 컬럼 추가
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE presets ADD COLUMN isLiveWallpaper INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE presets ADD COLUMN liveWallpaperUri TEXT DEFAULT NULL")
            }
        }

        // live_wallpapers 테이블 신규 생성
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE live_wallpapers (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        fileUri TEXT NOT NULL,
                        thumbnailUri TEXT,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        // presets 테이블에 landscape 라이브 배경화면 컬럼 추가
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE presets ADD COLUMN isLiveWallpaperLandscape INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE presets ADD COLUMN liveWallpaperLandscapeUri TEXT DEFAULT NULL")
            }
        }

        // presets 테이블에 정적 배경화면 가로 컬럼 추가
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE presets ADD COLUMN staticWallpaperLandscapeUri TEXT DEFAULT NULL")
            }
        }
    }
}
