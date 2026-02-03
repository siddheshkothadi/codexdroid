package me.siddheshkothadi.codexdroid.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ConnectionDao {
    @Query("SELECT * FROM connections ORDER BY updatedAt DESC")
    fun getAllConnections(): Flow<List<ConnectionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConnection(connection: ConnectionEntity): Long

    @Query("UPDATE connections SET updatedAt = :timestamp WHERE id = :id")
    suspend fun updateLastUsed(id: Long, timestamp: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteConnection(connection: ConnectionEntity)
}
