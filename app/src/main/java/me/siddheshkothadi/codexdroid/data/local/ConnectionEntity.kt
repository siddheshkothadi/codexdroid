package me.siddheshkothadi.codexdroid.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "connections")
data class ConnectionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val baseUrl: String,
    val encryptedSecret: String,
    val updatedAt: Long = System.currentTimeMillis()
)
