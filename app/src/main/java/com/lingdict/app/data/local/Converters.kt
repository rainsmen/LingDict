package com.lingdict.app.data.local

import androidx.room.TypeConverter
import com.lingdict.app.data.local.entity.WordStatus

/**
 * Room数据库类型转换器
 */
class Converters {

    @TypeConverter
    fun fromWordStatus(status: WordStatus): String {
        return status.name
    }

    @TypeConverter
    fun toWordStatus(value: String): WordStatus {
        return try {
            WordStatus.valueOf(value)
        } catch (e: IllegalArgumentException) {
            WordStatus.NEW
        }
    }
}
