package org.comon.streamlauncher.network.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.comon.streamlauncher.network.BuildConfig
import org.comon.streamlauncher.network.api.ChzzkService
import org.comon.streamlauncher.network.api.RssFeedApi
import org.comon.streamlauncher.network.api.YouTubeService
import org.comon.streamlauncher.network.converter.XmlConverterFactory
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(
                        HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.BODY
                        }
                    )
                }
            }
            .build()
    }

    @Provides
    @Singleton
    @XmlRetrofit
    fun provideXmlRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://placeholder.invalid/")
            .client(client)
            .addConverterFactory(XmlConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    @JsonRetrofit
    fun provideJsonRetrofit(client: OkHttpClient): Retrofit {
        val json = Json { ignoreUnknownKeys = true }
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl("https://placeholder.invalid/")
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideRssFeedApi(@XmlRetrofit retrofit: Retrofit): RssFeedApi {
        return retrofit.create(RssFeedApi::class.java)
    }

    @Provides
    @Singleton
    fun provideChzzkService(@JsonRetrofit retrofit: Retrofit): ChzzkService {
        return retrofit.create(ChzzkService::class.java)
    }

    @Provides
    @Singleton
    fun provideYouTubeService(@JsonRetrofit retrofit: Retrofit): YouTubeService {
        return retrofit.create(YouTubeService::class.java)
    }
}
