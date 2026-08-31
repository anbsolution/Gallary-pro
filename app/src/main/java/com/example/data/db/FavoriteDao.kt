package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT mediaUriString FROM favorites")
    fun getAllFavoriteUrisFlow(): Flow<List<String>>

    @Query("SELECT mediaUriString FROM favorites")
    suspend fun getAllFavoriteUris(): List<String>

    @Query("SELECT COUNT(*) FROM favorites WHERE mediaUriString = :uriString")
    suspend fun isFavorite(uriString: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE mediaUriString = :uriString")
    suspend fun deleteFavorite(uriString: String)
}
