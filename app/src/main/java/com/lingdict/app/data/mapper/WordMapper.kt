package com.lingdict.app.data.mapper

import com.lingdict.app.data.local.entity.WordEntity
import com.lingdict.app.data.remote.dto.YoudaoResponse

/**
 * 数据映射扩展函数
 */

/**
 * 将有道API响应转换为WordEntity
 */
fun YoudaoResponse.toWordEntity(): WordEntity? {
    if (errorCode != "0" || query == null || translation.isNullOrEmpty()) {
        return null
    }

    val explains = basic?.explains?.joinToString("; ") ?: ""
    val translationText = translation.joinToString("; ")

    return WordEntity(
        word = query,
        phonetic = basic?.phonetic,
        phoneticUs = basic?.usPhonetic,
        phoneticUk = basic?.ukPhonetic,
        definition = explains.ifEmpty { translationText },
        translation = translationText,
        level = null, // 有道API不提供等级信息
        frequency = 0,
        addedTime = System.currentTimeMillis()
    )
}
