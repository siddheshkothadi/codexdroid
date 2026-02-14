package me.siddheshkothadi.codexdroid.data.local

import me.siddheshkothadi.codexdroid.domain.model.Connection

interface LegacyConnectionStore {
    suspend fun readLegacyConnections(): List<Connection>

    suspend fun isRoomMigrationComplete(): Boolean

    suspend fun markRoomMigrationComplete()

    suspend fun clearLegacyConnections()
}
