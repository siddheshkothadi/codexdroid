package me.siddheshkothadi.codexdroid.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import me.siddheshkothadi.codexdroid.domain.model.Connection
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "connections")

@Singleton
class ConnectionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cryptoManager: CryptoManager
) : LegacyConnectionStore {
    private val connectionsKey = stringPreferencesKey("connections_list")
    private val roomMigrationCompleteKey = booleanPreferencesKey("connections_room_migration_complete")

    val legacyConnections: Flow<List<Connection>> = context.dataStore.data
        .map { preferences ->
            val json = preferences[connectionsKey] ?: "[]"
            try {
                val list = Json.decodeFromString<List<Connection>>(json)
                list.map { it.copy(secret = decryptSecret(it.secret)) }.sortedByDescending { it.updatedAt }
            } catch (e: Exception) {
                emptyList()
            }
        }

    override suspend fun readLegacyConnections(): List<Connection> = legacyConnections.first()

    suspend fun hasLegacyConnections(): Boolean = readLegacyConnections().isNotEmpty()

    override suspend fun isRoomMigrationComplete(): Boolean {
        return context.dataStore.data.first()[roomMigrationCompleteKey] ?: false
    }

    override suspend fun markRoomMigrationComplete() {
        context.dataStore.edit { preferences ->
            preferences[roomMigrationCompleteKey] = true
        }
    }

    override suspend fun clearLegacyConnections() {
        context.dataStore.edit { preferences ->
            preferences[connectionsKey] = "[]"
        }
    }

    private fun decryptSecret(encryptedSecret: String): String {
        if (encryptedSecret.isEmpty()) return ""
        return try {
            cryptoManager.decrypt(encryptedSecret)
        } catch (e: Exception) {
            ""
        }
    }
}
