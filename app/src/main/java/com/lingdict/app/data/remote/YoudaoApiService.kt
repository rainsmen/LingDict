package com.lingdict.app.data.remote

import com.lingdict.app.data.remote.dto.YoudaoResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 有道词典API服务
 * API文档：https://ai.youdao.com/DOCSIRMA/html/trans/api/wbfy/index.html
 */
interface YoudaoApiService {

    @GET("api")
    suspend fun translate(
        @Query("q") query: String,              // 要翻译的文本
        @Query("from") from: String = "en",     // 源语言
        @Query("to") to: String = "zh-CHS",     // 目标语言
        @Query("appKey") appKey: String,        // 应用ID
        @Query("salt") salt: String,            // 随机数
        @Query("sign") sign: String,            // 签名
        @Query("signType") signType: String = "v3",
        @Query("curtime") curtime: String       // 当前UTC时间戳（秒）
    ): YoudaoResponse

    companion object {
        const val BASE_URL = "https://openapi.youdao.com/"
    }
}
