package com.lingdict.app.presentation.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Learn : Screen("learn")
    object Test : Screen("test")
    object Statistics : Screen("statistics")
    object Settings : Screen("settings")
    object WordDetail : Screen("word_detail/{word}") {
        fun createRoute(word: String) = "word_detail/$word"
    }
}
