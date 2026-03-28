package org.comon.streamlauncher.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.comon.streamlauncher.data.repository.SuggestionRepositoryImpl
import org.comon.streamlauncher.domain.repository.SuggestionRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SuggestionModule {
    @Binds
    @Singleton
    abstract fun bindSuggestionRepository(impl: SuggestionRepositoryImpl): SuggestionRepository
}
