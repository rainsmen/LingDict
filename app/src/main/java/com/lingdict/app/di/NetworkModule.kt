package com.lingdict.app.di

import com.lingdict.app.data.remote.PexelsApiService
import com.lingdict.app.data.remote.YoudaoApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * 网络请求依赖注入模块
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class YoudaoRetrofit

    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class PexelsRetrofit

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
            )
            .build()
    }

    @Provides
    @Singleton
    @YoudaoRetrofit
    fun provideYoudaoRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(YoudaoApiService.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    @PexelsRetrofit
    fun providePexelsRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(PexelsApiService.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideYoudaoApiService(@YoudaoRetrofit retrofit: Retrofit): YoudaoApiService {
        return retrofit.create(YoudaoApiService::class.java)
    }

    @Provides
    @Singleton
    fun providePexelsApiService(@PexelsRetrofit retrofit: Retrofit): PexelsApiService {
        return retrofit.create(PexelsApiService::class.java)
    }
}
