package org.piarsenal.wallpaperd.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UsedImageDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<UsedImage>)

    @Query("SELECT * FROM used_image WHERE sourceId = :sourceId AND consumed = 0 LIMIT 1")
    suspend fun nextUnconsumed(sourceId: String): UsedImage?

    @Query("SELECT COUNT(*) FROM used_image WHERE sourceId = :sourceId AND consumed = 0")
    suspend fun unconsumedCount(sourceId: String): Int

    @Query("UPDATE used_image SET consumed = 1, filePath = '' WHERE key = :key")
    suspend fun markConsumed(key: String)

    @Query("SELECT filePath FROM used_image WHERE filePath != ''")
    suspend fun allFilePaths(): List<String>

    @Query("DELETE FROM used_image")
    suspend fun clear()

    @Query("DELETE FROM used_image WHERE sourceId = :sourceId")
    suspend fun clearSource(sourceId: String)
}
