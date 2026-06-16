package com.lingdict.app.util

import java.security.MessageDigest
import java.util.*

/**
 * 有道API签名工具
 */
object YoudaoSignUtil {

    /**
     * 生成有道API签名
     * @param appKey 应用ID
     * @param appSecret 应用密钥
     * @param query 查询文本
     * @param salt 随机数
     * @param curtime 当前UTC时间戳（秒）
     */
    fun generateSign(
        appKey: String,
        appSecret: String,
        query: String,
        salt: String,
        curtime: String
    ): String {
        val input = if (query.length <= 20) {
            query
        } else {
            query.substring(0, 10) + query.length + query.substring(query.length - 10)
        }

        val signStr = "$appKey$input$salt$curtime$appSecret"
        return signStr.sha256()
    }

    /**
     * 生成随机数（UUID）
     */
    fun generateSalt(): String {
        return UUID.randomUUID().toString()
    }

    /**
     * 获取当前UTC时间戳（秒）
     */
    fun getCurrentTime(): String {
        return (System.currentTimeMillis() / 1000).toString()
    }

    /**
     * SHA256加密
     */
    private fun String.sha256(): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
