package me.siddheshkothadi.codexdroid.data.local

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.siddheshkothadi.codexdroid.data.repository.ConnectionRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectionStorageMigration @Inject constructor(
    private val legacyStore: ConnectionManager,
    private val connectionRepository: ConnectionRepository,
) {
    private val migrateLock = Mutex()

    suspend fun migrateIfNeeded() {
        migrateLock.withLock {
            if (legacyStore.isRoomMigrationComplete()) return

            val legacyConnections = legacyStore.readLegacyConnections()
            if (legacyConnections.isNotEmpty()) {
                connectionRepository.importConnections(legacyConnections)
            }

            legacyStore.clearLegacyConnections()
            legacyStore.markRoomMigrationComplete()
        }
    }
}
