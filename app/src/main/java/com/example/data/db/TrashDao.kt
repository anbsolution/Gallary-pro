package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TrashDao {
    @Query("SELECT * FROM trashed_items ORDER BY dateTrashed DESC")
    fun getAllTrashedFlow(): Flow<List<TrashEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrashed(item: TrashEntity)

    @Query("DELETE FROM trashed_items WHERE mediaUriString = :uriString")
    suspend fun deleteTrashed(uriString: String)

    @Query("DELETE FROM trashed_items")
    suspend fun clearAll()
}
