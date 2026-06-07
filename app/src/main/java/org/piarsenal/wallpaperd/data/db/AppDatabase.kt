package org.piarsenal.wallpaperd.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [UsedImage::class, AppliedWallpaper::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun usedImageDao(): UsedImageDao
    abstract fun appliedWallpaperDao(): AppliedWallpaperDao

    companion object {
        @Volatile private var instance: AppDatabase? = null
        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext, AppDatabase::class.java, "wallpaperd.db"
            )
                // v1 -> v2 only adds the history table; cache index is regenerable, so a
                // destructive bump is acceptable here rather than shipping a migration.
                .fallbackToDestructiveMigration()
                .build().also { instance = it }
        }
    }
}
