package org.comon.streamlauncher.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.comon.streamlauncher.data.datasource.FirebaseMarketAuthDataSource
import org.comon.streamlauncher.data.datasource.FirebaseMarketPresetRemoteDataSource
import org.comon.streamlauncher.data.datasource.FirebaseMarketStorageDataSource
import org.comon.streamlauncher.data.datasource.MarketAuthDataSource
import org.comon.streamlauncher.data.datasource.MarketPresetRemoteDataSource
import org.comon.streamlauncher.data.datasource.MarketStorageDataSource
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataSourceModule {

    @Binds
    @Singleton
    abstract fun bindMarketAuthDataSource(
        impl: FirebaseMarketAuthDataSource,
    ): MarketAuthDataSource

    @Binds
    @Singleton
    abstract fun bindMarketPresetRemoteDataSource(
        impl: FirebaseMarketPresetRemoteDataSource,
    ): MarketPresetRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindMarketStorageDataSource(
        impl: FirebaseMarketStorageDataSource,
    ): MarketStorageDataSource
}
