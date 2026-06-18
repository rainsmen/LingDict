package com.lingdict.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.lingdict.app.data.local.LingDictDatabase
import com.lingdict.app.data.local.dao.ExampleDao
import com.lingdict.app.data.local.dao.StudyRecordDao
import com.lingdict.app.data.local.dao.UserWordDao
import com.lingdict.app.data.local.dao.WordDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

/**
 * 数据库依赖注入模块
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Version 2 ships the pre-populated dictionary asset. Existing user data must not be dropped.
        }
    }

    private fun migration2To3(context: Context): Migration {
        return object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                refreshPreloadedWords(context, database)
            }
        }
    }

    private fun refreshPreloadedWords(context: Context, database: SupportSQLiteDatabase) {
        val assetFile = File(context.noBackupFilesDir, "words_migration_v3.db")
        assetFile.parentFile?.mkdirs()
        context.assets.open("database/words.db").use { input ->
            assetFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        var attached = false
        try {
            database.execSQL("ATTACH DATABASE '${quoteSqlLiteral(assetFile.absolutePath)}' AS asset_dict")
            attached = true
            database.execSQL(
                """
                UPDATE words
                SET
                    phonetic = (SELECT phonetic FROM asset_dict.words WHERE asset_dict.words.word = words.word),
                    phoneticUs = (SELECT phoneticUs FROM asset_dict.words WHERE asset_dict.words.word = words.word),
                    phoneticUk = (SELECT phoneticUk FROM asset_dict.words WHERE asset_dict.words.word = words.word),
                    definition = (SELECT definition FROM asset_dict.words WHERE asset_dict.words.word = words.word),
                    translation = (SELECT translation FROM asset_dict.words WHERE asset_dict.words.word = words.word),
                    level = (SELECT level FROM asset_dict.words WHERE asset_dict.words.word = words.word),
                    frequency = (SELECT frequency FROM asset_dict.words WHERE asset_dict.words.word = words.word),
                    exchange = (SELECT exchange FROM asset_dict.words WHERE asset_dict.words.word = words.word),
                    collins = (SELECT collins FROM asset_dict.words WHERE asset_dict.words.word = words.word),
                    bnc = (SELECT bnc FROM asset_dict.words WHERE asset_dict.words.word = words.word),
                    frq = (SELECT frq FROM asset_dict.words WHERE asset_dict.words.word = words.word),
                    tag = (SELECT tag FROM asset_dict.words WHERE asset_dict.words.word = words.word),
                    addedTime = (SELECT addedTime FROM asset_dict.words WHERE asset_dict.words.word = words.word),
                    pos = (SELECT pos FROM asset_dict.words WHERE asset_dict.words.word = words.word),
                    oxford = (SELECT oxford FROM asset_dict.words WHERE asset_dict.words.word = words.word),
                    detail = (SELECT detail FROM asset_dict.words WHERE asset_dict.words.word = words.word),
                    audio = COALESCE(words.audio, (SELECT audio FROM asset_dict.words WHERE asset_dict.words.word = words.word))
                WHERE word IN (SELECT word FROM asset_dict.words)
                """.trimIndent()
            )
            database.execSQL(
                """
                INSERT OR IGNORE INTO words (
                    word, phonetic, phoneticUs, phoneticUk, definition, translation, level, frequency,
                    exchange, collins, bnc, frq, tag, addedTime, pos, oxford, detail, audio
                )
                SELECT
                    word, phonetic, phoneticUs, phoneticUk, definition, translation, level, frequency,
                    exchange, collins, bnc, frq, tag, addedTime, pos, oxford, detail, audio
                FROM asset_dict.words
                """.trimIndent()
            )
            database.execSQL(
                """
                DELETE FROM words
                WHERE word NOT IN (SELECT word FROM asset_dict.words)
                  AND word NOT IN (SELECT word FROM user_words)
                  AND word NOT IN (SELECT word FROM examples)
                """.trimIndent()
            )
        } finally {
            if (attached) {
                runCatching { database.execSQL("DETACH DATABASE asset_dict") }
            }
            assetFile.delete()
        }
    }

    private fun quoteSqlLiteral(value: String): String {
        return value.replace("'", "''")
    }

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
            .addMigrations(MIGRATION_1_2, migration2To3(context))
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

    @Provides
    @Singleton
    fun provideExampleDao(database: LingDictDatabase): ExampleDao {
        return database.exampleDao()
    }
}
