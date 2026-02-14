package me.siddheshkothadi.codexdroid.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import me.siddheshkothadi.codexdroid.domain.model.Connection
import me.siddheshkothadi.codexdroid.domain.repository.ConnectionRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectionStorageMigrationTest {
    @Test
    fun migrateIfNeeded_importsLegacyConnectionsAndMarksMigration() =
        runBlocking {
            val legacyConnection =
                Connection(
                    id = "conn-1",
                    name = "Local",
                    baseUrl = "http://localhost:8080",
                    secret = "secret",
                    updatedAt = 123L,
                )
            val legacyStore =
                FakeLegacyConnectionStore(
                    initialConnections = listOf(legacyConnection),
                    migrationComplete = false,
                )
            val repository = FakeConnectionRepository()
            val migration = ConnectionStorageMigration(legacyStore, repository)

            migration.migrateIfNeeded()

            assertEquals(1, repository.importCalls)
            assertEquals(listOf(legacyConnection), repository.importedConnections)
            assertTrue(legacyStore.clearCalled)
            assertTrue(legacyStore.migrationComplete)
        }

    @Test
    fun migrateIfNeeded_doesNothingWhenAlreadyMigrated() =
        runBlocking {
            val legacyStore =
                FakeLegacyConnectionStore(
                    initialConnections =
                        listOf(
                            Connection(
                                id = "conn-1",
                                name = "Local",
                                baseUrl = "http://localhost:8080",
                                secret = "secret",
                                updatedAt = 123L,
                            )
                        ),
                    migrationComplete = true,
                )
            val repository = FakeConnectionRepository()
            val migration = ConnectionStorageMigration(legacyStore, repository)

            migration.migrateIfNeeded()

            assertEquals(0, repository.importCalls)
            assertTrue(!legacyStore.clearCalled)
            assertTrue(legacyStore.migrationComplete)
        }

    private class FakeLegacyConnectionStore(
        initialConnections: List<Connection>,
        var migrationComplete: Boolean,
    ) : LegacyConnectionStore {
        private var connections: List<Connection> = initialConnections
        var clearCalled: Boolean = false

        override suspend fun readLegacyConnections(): List<Connection> = connections

        override suspend fun isRoomMigrationComplete(): Boolean = migrationComplete

        override suspend fun markRoomMigrationComplete() {
            migrationComplete = true
        }

        override suspend fun clearLegacyConnections() {
            clearCalled = true
            connections = emptyList()
        }
    }

    private class FakeConnectionRepository : ConnectionRepository {
        var importCalls: Int = 0
        var importedConnections: List<Connection> = emptyList()

        override fun observeConnections(): Flow<List<Connection>> = flowOf(emptyList())

        override suspend fun addConnection(name: String, baseUrl: String, secret: String) = Unit

        override suspend fun updateConnection(id: String, name: String, baseUrl: String, secret: String) = Unit

        override suspend fun updateLastUsed(id: String) = Unit

        override suspend fun deleteConnection(id: String) = Unit

        override suspend fun importConnections(connections: List<Connection>) {
            importCalls += 1
            importedConnections = connections
        }
    }
}
