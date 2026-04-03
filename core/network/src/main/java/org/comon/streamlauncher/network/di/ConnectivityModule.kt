package org.comon.streamlauncher.network.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.comon.streamlauncher.network.connectivity.NetworkConnectivityChecker
import org.comon.streamlauncher.network.connectivity.NetworkConnectivityCheckerImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ConnectivityModule {

    @Binds
    @Singleton
    abstract fun bindNetworkConnectivityChecker(
        impl: NetworkConnectivityCheckerImpl,
    ): NetworkConnectivityChecker
}
