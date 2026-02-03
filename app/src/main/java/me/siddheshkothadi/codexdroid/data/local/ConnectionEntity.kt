package me.siddheshkothadi.codexdroid.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "connections")
data class ConnectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val baseUrl: String,
    val secret: String,
    val updatedAt: Long = System.currentTimeMillis()
)
