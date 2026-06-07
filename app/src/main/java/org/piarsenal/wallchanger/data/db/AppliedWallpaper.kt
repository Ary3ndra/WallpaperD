package org.piarsenal.wallchanger.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A wallpaper that was applied. A copy is kept on disk so it can be re-applied or favourited. */
@Entity(tableName = "applied_wallpaper")
data class AppliedWallpaper(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceName: String,
    val label: String,
    val filePath: String,           // copy in the history dir, survives cache wipes
    val favourite: Boolean = false,
    val appliedAt: Long = System.currentTimeMillis()
)
