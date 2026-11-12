package com.allen.hearme.navigation

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Home : Screen("home")
    data object Training : Screen("training")
    data object History : Screen("history")
    data object Settings : Screen("settings")
}
