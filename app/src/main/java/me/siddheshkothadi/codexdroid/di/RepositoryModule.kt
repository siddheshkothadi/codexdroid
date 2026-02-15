package me.siddheshkothadi.codexdroid.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import me.siddheshkothadi.codexdroid.data.local.ConnectionManager
import me.siddheshkothadi.codexdroid.data.local.ConnectionStorageMigration
import me.siddheshkothadi.codexdroid.data.local.LegacyConnectionStore
import me.siddheshkothadi.codexdroid.data.repository.CodexSessionRepositoryImpl
import me.siddheshkothadi.codexdroid.data.repository.ConnectionRepositoryImpl
import me.siddheshkothadi.codexdroid.data.repository.SessionPreferencesRepositoryImpl
import me.siddheshkothadi.codexdroid.data.repository.SpeechSettingsRepositoryImpl
import me.siddheshkothadi.codexdroid.data.repository.SpeechSynthesisRepositoryImpl
import me.siddheshkothadi.codexdroid.data.repository.ThreadRepositoryImpl
import me.siddheshkothadi.codexdroid.domain.repository.CodexSessionRepository
import me.siddheshkothadi.codexdroid.domain.repository.ConnectionMigrationRepository
import me.siddheshkothadi.codexdroid.domain.repository.ConnectionRepository
import me.siddheshkothadi.codexdroid.domain.repository.SessionPreferencesRepository
import me.siddheshkothadi.codexdroid.domain.repository.SpeechSettingsRepository
import me.siddheshkothadi.codexdroid.domain.repository.SpeechSynthesisRepository
import me.siddheshkothadi.codexdroid.domain.repository.ThreadRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindConnectionRepository(
        impl: ConnectionRepositoryImpl,
    ): ConnectionRepository

    @Binds
    @Singleton
    abstract fun bindThreadRepository(
        impl: ThreadRepositoryImpl,
    ): ThreadRepository

    @Binds
    @Singleton
    abstract fun bindCodexSessionRepository(
        impl: CodexSessionRepositoryImpl,
    ): CodexSessionRepository

    @Binds
    @Singleton
    abstract fun bindConnectionMigrationRepository(
        impl: ConnectionStorageMigration,
    ): ConnectionMigrationRepository

    @Binds
    @Singleton
    abstract fun bindLegacyConnectionStore(
        impl: ConnectionManager,
    ): LegacyConnectionStore

    @Binds
    @Singleton
    abstract fun bindSpeechSettingsRepository(
        impl: SpeechSettingsRepositoryImpl,
    ): SpeechSettingsRepository

    @Binds
    @Singleton
    abstract fun bindSpeechSynthesisRepository(
        impl: SpeechSynthesisRepositoryImpl,
    ): SpeechSynthesisRepository

    @Binds
    @Singleton
    abstract fun bindSessionPreferencesRepository(
        impl: SessionPreferencesRepositoryImpl,
    ): SessionPreferencesRepository
}
