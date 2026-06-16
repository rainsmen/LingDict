package com.lingdict.app.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.lingdict.app.presentation.navigation.LingDictApp
import com.lingdict.app.presentation.theme.LingDictTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LingDictTheme {
                // 使用预填充数据库，直接进入主界面
                LingDictApp()
            }
        }
    }
}
