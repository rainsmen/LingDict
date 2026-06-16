package com.lingdict.app.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.lingdict.app.presentation.home.HomeScreen
import com.lingdict.app.presentation.learn.LearnScreen
import com.lingdict.app.presentation.settings.SettingsScreen

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Home : BottomNavItem(
        route = Screen.Home.route,
        title = "首页",
        icon = Icons.Default.Home
    )

    object Learn : BottomNavItem(
        route = Screen.Learn.route,
        title = "学习",
        icon = Icons.Default.School
    )

    object Test : BottomNavItem(
        route = Screen.Test.route,
        title = "测试",
        icon = Icons.Default.Quiz
    )

    object Statistics : BottomNavItem(
        route = Screen.Statistics.route,
        title = "统计",
        icon = Icons.Default.BarChart
    )

    object Settings : BottomNavItem(
        route = Screen.Settings.route,
        title = "设置",
        icon = Icons.Default.Settings
    )
}

@Composable
fun LingDictApp() {
    val navController = rememberNavController()
    val bottomNavItems = listOf(
        BottomNavItem.Home,
        BottomNavItem.Learn,
        BottomNavItem.Test,
        BottomNavItem.Statistics,
        BottomNavItem.Settings
    )

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                navController = navController,
                items = bottomNavItems
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(navController = navController)
            }

            composable(Screen.Learn.route) {
                LearnScreen(navController = navController)
            }

            composable(Screen.Test.route) {
                // TestScreen will be implemented
                Text("Test Screen - Coming Soon")
            }

            composable(Screen.Statistics.route) {
                // StatisticsScreen will be implemented
                Text("Statistics Screen - Coming Soon")
            }

            composable(Screen.Settings.route) {
                SettingsScreen(navController = navController)
            }
        }
    }
}

@Composable
fun BottomNavigationBar(
    navController: NavHostController,
    items: List<BottomNavItem>
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar {
        items.forEach { item ->
            val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true

            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title
                    )
                },
                label = { Text(item.title) },
                selected = selected,
                onClick = {
                    navController.navigate(item.route) {
                        // Pop up to the start destination of the graph to
                        // avoid building up a large stack of destinations
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        // Avoid multiple copies of the same destination when
                        // reselecting the same item
                        launchSingleTop = true
                        // Restore state when reselecting a previously selected item
                        restoreState = true
                    }
                }
            )
        }
    }
}
