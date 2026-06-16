package com.lingdict.app.di

import com.lingdict.app.data.repository.PexelsRepositoryImpl
import com.lingdict.app.data.repository.StudyRecordRepositoryImpl
import com.lingdict.app.data.repository.UserWordRepositoryImpl
import com.lingdict.app.data.repository.WordRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * 仓库依赖注入模块
 *
 * 注意：Repository已经使用@Singleton注解，这里只需要暴露接口绑定
 * 实际上由于使用了@Inject构造函数，Hilt会自动提供这些实例
 * 这个模块主要用于显式声明依赖关系
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    // Hilt会通过@Inject构造函数自动提供以下Repository
    // 这里不需要额外的@Provides方法

    // WordRepositoryImpl
    // UserWordRepositoryImpl
    // StudyRecordRepositoryImpl
    // PexelsRepositoryImpl
}
