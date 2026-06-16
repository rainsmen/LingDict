package com.lingdict.app.di

import android.content.Context
import androidx.room.Room
import com.lingdict.app.data.local.LingDictDatabase
import com.lingdict.app.data.local.dao.StudyRecordDao
import com.lingdict.app.data.local.dao.UserWordDao
import com.lingdict.app.data.local.dao.WordDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 数据库依赖注入模块
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): LingDictDatabase {
        return Room.databaseBuilder(
            context,
            LingDictDatabase::class.java,
            LingDictDatabase.DATABASE_NAME
        )
            .createFromAsset("database/words.db") // 从assets预填充词库
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideWordDao(database: LingDictDatabase): WordDao {
        return database.wordDao()
    }

    @Provides
    @Singleton
    fun provideUserWordDao(database: LingDictDatabase): UserWordDao {
        return database.userWordDao()
    }

    @Provides
    @Singleton
    fun provideStudyRecordDao(database: LingDictDatabase): StudyRecordDao {
        return database.studyRecordDao()
    }
}
