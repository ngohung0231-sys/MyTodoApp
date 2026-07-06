package com.hungday.mytodoapp.database

import androidx.room.*
import com.hungday.mytodoapp.model.TrashItem
import kotlinx.coroutines.flow.Flow

@Dao
interface TrashDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrashItem(trashItem: TrashItem): Long

    @Query("SELECT * FROM trash_items ORDER BY deletedAt DESC")
    fun getAllTrashItems(): Flow<List<TrashItem>>

    @Query("DELETE FROM trash_items WHERE trashId = :id")
    suspend fun deleteTrashItem(id: Int)

    @Query("DELETE FROM trash_items WHERE deletedAt < :expiryTime")
    suspend fun deleteExpiredTrash(expiryTime: Long)
}
