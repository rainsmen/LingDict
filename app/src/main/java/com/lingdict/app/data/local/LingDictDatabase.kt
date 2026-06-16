package com.lingdict.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.lingdict.app.data.local.dao.StudyRecordDao
import com.lingdict.app.data.local.dao.UserWordDao
import com.lingdict.app.data.local.dao.WordDao
import com.lingdict.app.data.local.entity.ExampleEntity
import com.lingdict.app.data.local.entity.StudyRecordEntity
import com.lingdict.app.data.local.entity.UserWordEntity
import com.lingdict.app.data.local.entity.WordEntity

/**
 * LingDict应用数据库
 */
@Database(
    entities = [
        WordEntity::class,
        UserWordEntity::class,
        ExampleEntity::class,
        StudyRecordEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class LingDictDatabase : RoomDatabase() {

    abstract fun wordDao(): WordDao
    abstract fun userWordDao(): UserWordDao
    abstract fun studyRecordDao(): StudyRecordDao

    companion object {
        const val DATABASE_NAME = "lingdict.db"
    }
}
