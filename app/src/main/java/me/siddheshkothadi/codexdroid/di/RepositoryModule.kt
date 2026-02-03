package me.siddheshkothadi.codexdroid.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    // Repository logic is handled by @Inject constructor and @Singleton on the repository classes themselves.
}
