package me.siddheshkothadi.codexdroid.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ThreadDao {
    @Query("SELECT * FROM threads WHERE connectionId = :connectionId ORDER BY updatedAt DESC")
    fun getThreadsByConnection(connectionId: String): Flow<List<ThreadEntity>>

    @Query("SELECT * FROM threads WHERE connectionId = :connectionId AND id = :threadId LIMIT 1")
    suspend fun getThreadById(connectionId: String, threadId: String): ThreadEntity?

    @Query("SELECT * FROM threads WHERE connectionId = :connectionId AND id = :threadId LIMIT 1")
    fun observeThreadById(connectionId: String, threadId: String): Flow<ThreadEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertThreads(threads: List<ThreadEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertThread(thread: ThreadEntity)

    @Query("DELETE FROM threads WHERE connectionId = :connectionId")
    suspend fun deleteThreadsByConnection(connectionId: String)
}
