package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trashed_items")
data class TrashEntity(
    @PrimaryKey val mediaUriString: String,
    val displayName: String,
    val size: Long,
    val isVideo: Boolean,
    val originalPath: String,
    val dateTrashed: Long = System.currentTimeMillis()
)
