package org.piarsenal.wallpaperd.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppliedWallpaperDao {
    @Insert
    suspend fun insert(item: AppliedWallpaper): Long

    @Query("SELECT * FROM applied_wallpaper ORDER BY appliedAt DESC")
    fun observeAll(): Flow<List<AppliedWallpaper>>

    @Query("SELECT * FROM applied_wallpaper WHERE id = :id")
    suspend fun byId(id: Long): AppliedWallpaper?

    @Query("UPDATE applied_wallpaper SET favourite = :fav WHERE id = :id")
    suspend fun setFavourite(id: Long, fav: Boolean)

    @Query("UPDATE applied_wallpaper SET appliedAt = :ts WHERE id = :id")
    suspend fun touch(id: Long, ts: Long)

    @Query("DELETE FROM applied_wallpaper WHERE id = :id")
    suspend fun delete(id: Long)

    // Prune: keep newest [keep] non-favourites; favourites are never pruned.
    @Query(
        """DELETE FROM applied_wallpaper WHERE favourite = 0 AND id NOT IN (
               SELECT id FROM applied_wallpaper WHERE favourite = 0
               ORDER BY appliedAt DESC LIMIT :keep
           )"""
    )
    suspend fun prune(keep: Int)

    @Query("SELECT filePath FROM applied_wallpaper WHERE favourite = 0")
    suspend fun nonFavouritePaths(): List<String>

    @Query("DELETE FROM applied_wallpaper WHERE favourite = 0")
    suspend fun clearNonFavourites()
}
