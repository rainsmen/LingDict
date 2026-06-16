package com.lingdict.app.di

import android.content.Context
import com.lingdict.app.util.TTSManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UtilModule {

    @Provides
    @Singleton
    fun provideTTSManager(
        @ApplicationContext context: Context
    ): TTSManager {
        return TTSManager(context)
    }
}
