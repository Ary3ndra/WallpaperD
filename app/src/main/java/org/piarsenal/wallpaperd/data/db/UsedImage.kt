package org.piarsenal.wallpaperd.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Index of downloaded/used wallpapers. Mirrors the GNOME extension behaviour:
 * a downloaded page is tracked and each image is deleted once consumed.
 */
@Entity(tableName = "used_image")
data class UsedImage(
    @PrimaryKey val key: String,   // sourceId + remote id, dedupes a fetched page
    val sourceId: String,
    val filePath: String,          // cached file on disk, "" once deleted
    val consumed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
