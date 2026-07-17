package com.onlylipu.cloud.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.onlylipu.cloud.ui.apps.AppManagerScreen
import com.onlylipu.cloud.ui.dashboard.DashboardScreen
import com.onlylipu.cloud.ui.login.LoginScreen
import com.onlylipu.cloud.ui.session.SessionScreen
import com.onlylipu.cloud.ui.settings.SettingsScreen
import com.onlylipu.cloud.ui.splash.SplashScreen

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val DASHBOARD = "dashboard"
    const val SESSION = "session/{mode}"
    const val SETTINGS = "settings"
    const val APPS = "apps"

    fun session(mode: String) = "session/$mode"
}

@Composable
fun AppNavHost() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) { SplashScreen(nav) }
        composable(Routes.LOGIN) { LoginScreen(nav) }
        composable(Routes.DASHBOARD) { DashboardScreen(nav) }
        composable(
            Routes.SESSION,
            arguments = listOf(navArgument("mode") { type = NavType.StringType })
        ) { backStack ->
            SessionScreen(nav, backStack.arguments?.getString("mode") ?: "android")
        }
        composable(Routes.SETTINGS) { SettingsScreen(nav) }
        composable(Routes.APPS) { AppManagerScreen(nav) }
    }
}
