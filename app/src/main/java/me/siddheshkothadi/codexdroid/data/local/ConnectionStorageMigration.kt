package me.siddheshkothadi.codexdroid.data.local

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.siddheshkothadi.codexdroid.domain.repository.ConnectionMigrationRepository
import me.siddheshkothadi.codexdroid.domain.repository.ConnectionRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectionStorageMigration @Inject constructor(
    private val legacyStore: LegacyConnectionStore,
    private val connectionRepository: ConnectionRepository,
) : ConnectionMigrationRepository {
    private val migrateLock = Mutex()

    override suspend fun migrateIfNeeded() {
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
