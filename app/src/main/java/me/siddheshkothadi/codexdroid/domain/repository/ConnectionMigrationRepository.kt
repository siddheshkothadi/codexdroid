package me.siddheshkothadi.codexdroid.domain.repository

interface ConnectionMigrationRepository {
    suspend fun migrateIfNeeded()
}
