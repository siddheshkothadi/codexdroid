package me.siddheshkothadi.codexdroid.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class Connection(
    val id: String,
    val name: String,
    val baseUrl: String,
    val secret: String,
    val updatedAt: Long = System.currentTimeMillis()
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "connections")

@Singleton
class ConnectionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cryptoManager: CryptoManager
) {
    private val connectionsKey = stringPreferencesKey("connections_list")

    val connections: Flow<List<Connection>> = context.dataStore.data
        .map { preferences ->
            val json = preferences[connectionsKey] ?: "[]"
            try {
                val list = Json.decodeFromString<List<Connection>>(json)
                list.map { it.copy(secret = decryptSecret(it.secret)) }.sortedByDescending { it.updatedAt }
            } catch (e: Exception) {
                emptyList()
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

    private fun encryptSecret(plainSecret: String): String {
        if (plainSecret.isEmpty()) return ""
        return cryptoManager.encrypt(plainSecret)
    }

    suspend fun addConnection(name: String, baseUrl: String, secret: String) {
        context.dataStore.edit { preferences ->
            val currentJson = preferences[connectionsKey] ?: "[]"
            val current = try {
                Json.decodeFromString<List<Connection>>(currentJson).toMutableList()
            } catch (e: Exception) {
                mutableListOf()
            }
            
            val newConnection = Connection(
                id = java.util.UUID.randomUUID().toString(),
                name = name,
                baseUrl = baseUrl,
                secret = encryptSecret(secret)
            )
            current.add(newConnection)
            preferences[connectionsKey] = Json.encodeToString(current)
        }
    }

    suspend fun updateConnection(id: String, name: String, baseUrl: String, secret: String) {
        context.dataStore.edit { preferences ->
            val currentJson = preferences[connectionsKey] ?: "[]"
            val current = try {
                Json.decodeFromString<List<Connection>>(currentJson).toMutableList()
            } catch (e: Exception) {
                mutableListOf()
            }
            val index = current.indexOfFirst { it.id == id }
            if (index != -1) {
                current[index] = current[index].copy(
                    name = name,
                    baseUrl = baseUrl,
                    secret = encryptSecret(secret),
                    updatedAt = System.currentTimeMillis()
                )
                preferences[connectionsKey] = Json.encodeToString(current)
            }
        }
    }

    suspend fun deleteConnection(id: String) {
        context.dataStore.edit { preferences ->
            val currentJson = preferences[connectionsKey] ?: "[]"
            val current = try {
                Json.decodeFromString<List<Connection>>(currentJson)
            } catch (e: Exception) {
                emptyList()
            }
            val filtered = current.filter { it.id != id }
            preferences[connectionsKey] = Json.encodeToString(filtered)
        }
    }

    suspend fun updateLastUsed(id: String) {
        context.dataStore.edit { preferences ->
            val currentJson = preferences[connectionsKey] ?: "[]"
            val current = try {
                Json.decodeFromString<List<Connection>>(currentJson).toMutableList()
            } catch (e: Exception) {
                mutableListOf()
            }
            val index = current.indexOfFirst { it.id == id }
            if (index != -1) {
                current[index] = current[index].copy(updatedAt = System.currentTimeMillis())
                preferences[connectionsKey] = Json.encodeToString(current)
            }
        }
    }
}
