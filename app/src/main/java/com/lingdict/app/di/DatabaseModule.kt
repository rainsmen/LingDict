package com.lingdict.app.di

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
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

        var assetDatabase: SQLiteDatabase? = null
        try {
            database.execSQL("DROP TABLE IF EXISTS migration_words")
            database.execSQL(
                """
                CREATE TEMP TABLE migration_words (
                    word TEXT NOT NULL PRIMARY KEY,
                    phonetic TEXT,
                    phoneticUs TEXT,
                    phoneticUk TEXT,
                    definition TEXT NOT NULL,
                    translation TEXT NOT NULL,
                    level TEXT,
                    frequency INTEGER NOT NULL,
                    exchange TEXT,
                    collins INTEGER,
                    bnc INTEGER,
                    frq INTEGER,
                    tag TEXT,
                    addedTime INTEGER NOT NULL,
                    pos TEXT,
                    oxford INTEGER,
                    detail TEXT,
                    audio TEXT
                )
                """.trimIndent()
            )

            assetDatabase = SQLiteDatabase.openDatabase(
                assetFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY
            )
            assetDatabase.rawQuery(
                """
                SELECT word, phonetic, phoneticUs, phoneticUk, definition, translation, level,
                       frequency, exchange, collins, bnc, frq, tag, addedTime, pos, oxford, detail, audio
                FROM words
                """.trimIndent(),
                null
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    database.execSQL(
                        """
                        INSERT INTO migration_words (
                            word, phonetic, phoneticUs, phoneticUk, definition, translation, level,
                            frequency, exchange, collins, bnc, frq, tag, addedTime, pos, oxford, detail, audio
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """.trimIndent(),
                        arrayOf(
                            cursor.getStringOrNull(0),
                            cursor.getStringOrNull(1),
                            cursor.getStringOrNull(2),
                            cursor.getStringOrNull(3),
                            cursor.getStringOrNull(4).orEmpty(),
                            cursor.getStringOrNull(5).orEmpty(),
                            cursor.getStringOrNull(6),
                            cursor.getIntOrZero(7),
                            cursor.getStringOrNull(8),
                            cursor.getIntOrNull(9),
                            cursor.getIntOrNull(10),
                            cursor.getIntOrNull(11),
                            cursor.getStringOrNull(12),
                            cursor.getLongOrZero(13),
                            cursor.getStringOrNull(14),
                            cursor.getIntOrNull(15),
                            cursor.getStringOrNull(16),
                            cursor.getStringOrNull(17)
                        )
                    )
                }
            }

            database.execSQL(
                """
                UPDATE words
                SET
                    phonetic = (SELECT phonetic FROM migration_words WHERE migration_words.word = words.word),
                    phoneticUs = (SELECT phoneticUs FROM migration_words WHERE migration_words.word = words.word),
                    phoneticUk = (SELECT phoneticUk FROM migration_words WHERE migration_words.word = words.word),
                    definition = (SELECT definition FROM migration_words WHERE migration_words.word = words.word),
                    translation = (SELECT translation FROM migration_words WHERE migration_words.word = words.word),
                    level = (SELECT level FROM migration_words WHERE migration_words.word = words.word),
                    frequency = (SELECT frequency FROM migration_words WHERE migration_words.word = words.word),
                    exchange = (SELECT exchange FROM migration_words WHERE migration_words.word = words.word),
                    collins = (SELECT collins FROM migration_words WHERE migration_words.word = words.word),
                    bnc = (SELECT bnc FROM migration_words WHERE migration_words.word = words.word),
                    frq = (SELECT frq FROM migration_words WHERE migration_words.word = words.word),
                    tag = (SELECT tag FROM migration_words WHERE migration_words.word = words.word),
                    addedTime = (SELECT addedTime FROM migration_words WHERE migration_words.word = words.word),
                    pos = (SELECT pos FROM migration_words WHERE migration_words.word = words.word),
                    oxford = (SELECT oxford FROM migration_words WHERE migration_words.word = words.word),
                    detail = (SELECT detail FROM migration_words WHERE migration_words.word = words.word),
                    audio = COALESCE(words.audio, (SELECT audio FROM migration_words WHERE migration_words.word = words.word))
                WHERE word IN (SELECT word FROM migration_words)
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
                FROM migration_words
                """.trimIndent()
            )
            database.execSQL(
                """
                DELETE FROM words
                WHERE word NOT IN (SELECT word FROM migration_words)
                  AND word NOT IN (SELECT word FROM user_words)
                  AND word NOT IN (SELECT word FROM examples)
                """.trimIndent()
            )
            database.execSQL("DROP TABLE migration_words")
        } finally {
            assetDatabase?.close()
            assetFile.delete()
        }
    }

    private fun Cursor.getStringOrNull(index: Int): String? {
        return if (isNull(index)) null else getString(index)
    }

    private fun Cursor.getIntOrNull(index: Int): Int? {
        return if (isNull(index)) null else getInt(index)
    }

    private fun Cursor.getIntOrZero(index: Int): Int {
        return if (isNull(index)) 0 else getInt(index)
    }

    private fun Cursor.getLongOrZero(index: Int): Long {
        return if (isNull(index)) 0L else getLong(index)
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
