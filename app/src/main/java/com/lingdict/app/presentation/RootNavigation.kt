package com.lingdict.app.presentation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lingdict.app.presentation.navigation.LingDictApp
import com.lingdict.app.presentation.navigation.Screen
import com.lingdict.app.presentation.splash.SplashScreen

/**
 * 应用根导航
 * 包含Splash Screen和主应用
 */
@Composable
fun RootNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        // Splash Screen
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToMain = {
                    navController.navigate(Screen.Home.route) {
                        // 清除Splash Screen，防止返回
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        // Main App (包含底部导航的所有页面)
        composable(Screen.Home.route) {
            LingDictApp()
        }
    }
}
