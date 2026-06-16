package com.lingdict.app.di

import com.lingdict.app.data.repository.PexelsRepositoryImpl
import com.lingdict.app.data.repository.SettingsRepositoryImpl
import com.lingdict.app.data.repository.StudyRecordRepositoryImpl
import com.lingdict.app.data.repository.UserWordRepositoryImpl
import com.lingdict.app.data.repository.WordRepositoryImpl
import com.lingdict.app.domain.repository.PexelsRepository
import com.lingdict.app.domain.repository.SettingsRepository
import com.lingdict.app.domain.repository.StudyRecordRepository
import com.lingdict.app.domain.repository.UserWordRepository
import com.lingdict.app.domain.repository.WordRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 仓库依赖注入模块
 * 绑定接口到实现类
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindWordRepository(
        impl: WordRepositoryImpl
    ): WordRepository

    @Binds
    @Singleton
    abstract fun bindUserWordRepository(
        impl: UserWordRepositoryImpl
    ): UserWordRepository

    @Binds
    @Singleton
    abstract fun bindStudyRecordRepository(
        impl: StudyRecordRepositoryImpl
    ): StudyRecordRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        impl: SettingsRepositoryImpl
    ): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindPexelsRepository(
        impl: PexelsRepositoryImpl
    ): PexelsRepository
}
