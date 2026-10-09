package com.vaultia.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.vaultia.app.core.theme.VaultiaTheme
import com.vaultia.app.data.repository.ThemeMode
import com.vaultia.app.domain.model.MediaType
import com.vaultia.app.ui.about.AboutScreen
import com.vaultia.app.ui.backup.AutoBackupScreen
import com.vaultia.app.ui.dashboard.DashboardScreen
import com.vaultia.app.ui.library.LibraryScreen
import com.vaultia.app.ui.lock.LockScreen
import com.vaultia.app.ui.navigation.NavRoutes
import com.vaultia.app.ui.onboarding.OnboardingScreen
import com.vaultia.app.ui.player.AudioPlayerScreen
import com.vaultia.app.ui.player.ImageViewerScreen
import com.vaultia.app.ui.player.VideoPlayerScreen
import com.vaultia.app.ui.search.SearchScreen
import com.vaultia.app.ui.settings.SettingsScreen
import com.vaultia.app.ui.splash.SplashScreen
import com.vaultia.app.ui.transfers.TransfersScreen
import com.vaultia.app.ui.viewmodel.PlayerViewModel
import com.vaultia.app.ui.viewmodel.SettingsViewModel
import com.vaultia.app.ui.viewmodel.TransferViewModel
import com.vaultia.app.ui.viewmodel.VaultViewModel
import androidx.fragment.app.FragmentActivity

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as VaultiaApplication
        val vaultViewModel = VaultViewModel(app.vaultRepository)
        val transferViewModel = TransferViewModel(app.database, app.uploadManager, app.downloadManager)
        val settingsViewModel = SettingsViewModel(app.vaultRepository, app.settingsRepository)
        val playerViewModel = PlayerViewModel(app.vaultRepository, app.videoPlayerManager)

        setContent {
            val settings by settingsViewModel.settings.collectAsState()
            val isDarkTheme = when (settings.themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            VaultiaTheme(darkTheme = isDarkTheme) {
                VaultiaAppNavigation(
                    app = app,
                    vaultViewModel = vaultViewModel,
                    transferViewModel = transferViewModel,
                    settingsViewModel = settingsViewModel,
                    playerViewModel = playerViewModel
                )
            }
        }
    }
}

@Composable
fun VaultiaAppNavigation(
    app: VaultiaApplication,
    vaultViewModel: VaultViewModel,
    transferViewModel: TransferViewModel,
    settingsViewModel: SettingsViewModel,
    playerViewModel: PlayerViewModel
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = NavRoutes.Splash.route,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> (fullWidth * 0.35f).toInt() },
                animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
            ) + fadeIn(animationSpec = tween(280))
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> -(fullWidth * 0.25f).toInt() },
                animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
            ) + fadeOut(animationSpec = tween(240))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> -(fullWidth * 0.25f).toInt() },
                animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
            ) + fadeIn(animationSpec = tween(280))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> (fullWidth * 0.35f).toInt() },
                animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
            ) + fadeOut(animationSpec = tween(240))
        }
    ) {
        // Splash Screen
        composable(NavRoutes.Splash.route) {
            SplashScreen(
                secureStorage = app.secureStorage,
                vaultRepository = app.vaultRepository,
                onNavigateToOnboarding = {
                    navController.navigate(NavRoutes.Onboarding.route) {
                        popUpTo(NavRoutes.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToLock = {
                    navController.navigate(NavRoutes.Lock.route) {
                        popUpTo(NavRoutes.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToDashboard = {
                    navController.navigate(NavRoutes.Dashboard.route) {
                        popUpTo(NavRoutes.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        // Onboarding Screen
        composable(NavRoutes.Onboarding.route) {
            OnboardingScreen(
                viewModel = vaultViewModel,
                onSetupComplete = {
                    navController.navigate(NavRoutes.Dashboard.route) {
                        popUpTo(NavRoutes.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        // Lock Screen
        composable(NavRoutes.Lock.route) {
            LockScreen(
                appLockManager = app.appLockManager,
                onUnlocked = {
                    navController.navigate(NavRoutes.Dashboard.route) {
                        popUpTo(NavRoutes.Lock.route) { inclusive = true }
                    }
                }
            )
        }

        // Dashboard Screen
        composable(NavRoutes.Dashboard.route) {
            DashboardScreen(
                vaultViewModel = vaultViewModel,
                transferViewModel = transferViewModel,
                onNavigateToMedia = { file ->
                    when (file.mediaType) {
                        MediaType.VIDEOS -> navController.navigate(NavRoutes.VideoPlayer.createRoute(file.id))
                        MediaType.AUDIO -> navController.navigate(NavRoutes.AudioPlayer.createRoute(file.id))
                        MediaType.IMAGES -> navController.navigate(NavRoutes.ImageViewer.createRoute(file.id))
                        else -> vaultViewModel.downloadFile(file)
                    }
                },
                onNavigateToSearch = { navController.navigate(NavRoutes.Search.route) },
                onNavigateToSettings = { navController.navigate(NavRoutes.Settings.route) },
                onNavigateTab = { route -> navController.navigate(route) },
                onNavigateToAutoBackup = { navController.navigate(NavRoutes.AutoBackup.route) }
            )
        }

        // Library Screen
        composable(NavRoutes.Library.route) {
            LibraryScreen(
                vaultViewModel = vaultViewModel,
                onNavigateToMedia = { file ->
                    when (file.mediaType) {
                        MediaType.VIDEOS -> navController.navigate(NavRoutes.VideoPlayer.createRoute(file.id))
                        MediaType.AUDIO -> navController.navigate(NavRoutes.AudioPlayer.createRoute(file.id))
                        MediaType.IMAGES -> navController.navigate(NavRoutes.ImageViewer.createRoute(file.id))
                        else -> vaultViewModel.downloadFile(file)
                    }
                },
                onNavigateTab = { route -> navController.navigate(route) }
            )
        }

        // Transfers Screen
        composable(NavRoutes.Transfers.route) {
            TransfersScreen(
                transferViewModel = transferViewModel,
                onNavigateTab = { route -> navController.navigate(route) }
            )
        }

        // Search Screen
        composable(NavRoutes.Search.route) {
            SearchScreen(
                vaultViewModel = vaultViewModel,
                onNavigateToMedia = { file ->
                    when (file.mediaType) {
                        MediaType.VIDEOS -> navController.navigate(NavRoutes.VideoPlayer.createRoute(file.id))
                        MediaType.AUDIO -> navController.navigate(NavRoutes.AudioPlayer.createRoute(file.id))
                        MediaType.IMAGES -> navController.navigate(NavRoutes.ImageViewer.createRoute(file.id))
                        else -> vaultViewModel.downloadFile(file)
                    }
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        // Settings Screen
        composable(NavRoutes.Settings.route) {
            SettingsScreen(
                secureStorage = app.secureStorage,
                settingsViewModel = settingsViewModel,
                onLogout = {
                    navController.navigate(NavRoutes.Onboarding.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateTab = { route -> navController.navigate(route) },
                onNavigateToAutoBackup = { navController.navigate(NavRoutes.AutoBackup.route) },
                onNavigateToAbout = { navController.navigate(NavRoutes.About.route) }
            )
        }

        // Auto-Backup Screen
        composable(NavRoutes.AutoBackup.route) {
            AutoBackupScreen(
                autoBackupManager = app.autoBackupManager,
                onBackClick = { navController.popBackStack() }
            )
        }

        // About Screen
        composable(NavRoutes.About.route) {
            AboutScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        // Video Player Screen
        composable(
            route = NavRoutes.VideoPlayer.route,
            arguments = listOf(navArgument("fileId") { type = NavType.StringType })
        ) { backStackEntry ->
            val fileId = backStackEntry.arguments?.getString("fileId") ?: ""
            VideoPlayerScreen(
                fileId = fileId,
                viewModel = playerViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        // Audio Player Screen
        composable(
            route = NavRoutes.AudioPlayer.route,
            arguments = listOf(navArgument("fileId") { type = NavType.StringType })
        ) { backStackEntry ->
            val fileId = backStackEntry.arguments?.getString("fileId") ?: ""
            AudioPlayerScreen(
                fileId = fileId,
                viewModel = playerViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        // Image Viewer Screen
        composable(
            route = NavRoutes.ImageViewer.route,
            arguments = listOf(navArgument("fileId") { type = NavType.StringType })
        ) { backStackEntry ->
            val fileId = backStackEntry.arguments?.getString("fileId") ?: ""
            ImageViewerScreen(
                fileId = fileId,
                vaultViewModel = vaultViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
