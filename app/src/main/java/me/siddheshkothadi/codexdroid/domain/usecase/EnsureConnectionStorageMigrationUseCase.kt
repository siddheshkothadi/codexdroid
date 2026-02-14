package me.siddheshkothadi.codexdroid.domain.usecase

import me.siddheshkothadi.codexdroid.data.local.ConnectionStorageMigration
import javax.inject.Inject

class EnsureConnectionStorageMigrationUseCase @Inject constructor(
    private val connectionStorageMigration: ConnectionStorageMigration,
) {
    suspend operator fun invoke() {
        connectionStorageMigration.migrateIfNeeded()
    }
}
