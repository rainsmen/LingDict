package com.lingdict.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class LingDictApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // 初始化日志
        if (BuildConfig.DEBUG) {
            // 开发模式下启用详细日志
        }
    }
}
