package me.siddheshkothadi.codexdroid.domain.usecase

import me.siddheshkothadi.codexdroid.domain.repository.ConnectionMigrationRepository
import javax.inject.Inject

class EnsureConnectionStorageMigrationUseCase @Inject constructor(
    private val connectionMigrationRepository: ConnectionMigrationRepository,
) {
    suspend operator fun invoke() {
        connectionMigrationRepository.migrateIfNeeded()
    }
}


