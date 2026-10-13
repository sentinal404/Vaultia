package com.vaultia.app.ui.navigation

sealed class NavRoutes(val route: String) {
    object Splash : NavRoutes("splash")
    object Welcome : NavRoutes("welcome")
    object Onboarding : NavRoutes("onboarding")
    object Lock : NavRoutes("lock")
    object Dashboard : NavRoutes("dashboard")
    object Library : NavRoutes("library")
    object Transfers : NavRoutes("transfers")
    object Search : NavRoutes("search")
    object Settings : NavRoutes("settings")
    object AutoBackup : NavRoutes("auto_backup")
    object About : NavRoutes("about")

    object VideoPlayer : NavRoutes("player_video/{fileId}") {
        fun createRoute(fileId: String) = "player_video/$fileId"
    }

    object AudioPlayer : NavRoutes("player_audio/{fileId}") {
        fun createRoute(fileId: String) = "player_audio/$fileId"
    }

    object ImageViewer : NavRoutes("viewer_image/{fileId}") {
        fun createRoute(fileId: String) = "viewer_image/$fileId"
    }
}
