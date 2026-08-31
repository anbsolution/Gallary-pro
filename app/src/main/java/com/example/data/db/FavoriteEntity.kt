package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val mediaUriString: String,
    val dateAdded: Long = System.currentTimeMillis()
)
