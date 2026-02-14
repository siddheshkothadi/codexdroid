package me.siddheshkothadi.codexdroid.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ConnectionDao {
    @Query("SELECT * FROM connections ORDER BY updatedAt DESC")
    fun getAllConnections(): Flow<List<ConnectionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConnection(connection: ConnectionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConnections(connections: List<ConnectionEntity>)

    @Query("SELECT * FROM connections WHERE id = :id LIMIT 1")
    suspend fun getConnectionById(id: String): ConnectionEntity?

    @Query("UPDATE connections SET updatedAt = :timestamp WHERE id = :id")
    suspend fun updateLastUsed(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM connections WHERE id = :id")
    suspend fun deleteConnectionById(id: String)
}
