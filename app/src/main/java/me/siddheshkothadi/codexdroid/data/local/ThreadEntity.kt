package me.siddheshkothadi.codexdroid.data.local

import androidx.room.Entity

@Entity(
    tableName = "threads",
    primaryKeys = ["connectionId", "id"]
)
data class ThreadEntity(
    // Composite PK: threads are scoped to a connection.
    // Without this, switching connections can overwrite threads with the same id.
    val id: String,
    val preview: String,
    val modelProvider: String,
    val createdAt: Long,
    val updatedAt: Long,
    val path: String,
    val cwd: String,
    val connectionId: String, // Changed to String to match Connection.id from SharedPreferences
    val threadJson: String? = null
)
