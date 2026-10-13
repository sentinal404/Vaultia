package com.vaultia.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.vaultia.app.core.theme.VaultiaTheme
import com.vaultia.app.data.repository.ThemeMode
import com.vaultia.app.domain.model.MediaType
import com.vaultia.app.ui.about.AboutScreen
import com.vaultia.app.ui.backup.AutoBackupScreen
import com.vaultia.app.ui.components.VaultiaBottomNav
import com.vaultia.app.ui.dashboard.DashboardScreen
import com.vaultia.app.ui.library.LibraryScreen
import com.vaultia.app.ui.lock.LockScreen
import com.vaultia.app.ui.navigation.NavRoutes
import com.vaultia.app.ui.onboarding.OnboardingScreen
import com.vaultia.app.ui.onboarding.WelcomeOnboardingScreen
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

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as VaultiaApplication
        val vaultViewModel = VaultViewModel(app.vaultRepository, app.cloudSyncManager)
        val transferViewModel = TransferViewModel(app.database, app.uploadManager, app.downloadManager)
        val settingsViewModel = SettingsViewModel(app.vaultRepository, app.settingsRepository)
        val playerViewModel = PlayerViewModel(app.vaultRepository, app.videoPlayerManager, app.downloadManager)

        setContent {
            // Automatically restore/sync cloud files from Telegram if configured
            androidx.compose.runtime.LaunchedEffect(Unit) {
                if (!app.secureStorage.botToken.isNullOrBlank() && !app.secureStorage.channelId.isNullOrBlank()) {
                    app.cloudSyncManager.performFullSync()
                }
            }

            val settings by settingsViewModel.settings.collectAsState()
            val themeModeString = when (settings.themeMode) {
                ThemeMode.DARK -> "DARK"
                ThemeMode.LIGHT -> "LIGHT"
                ThemeMode.SYSTEM -> "SYSTEM"
            }

            VaultiaTheme(themeMode = themeModeString) {
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
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: NavRoutes.Splash.route

    val primaryRoutes = remember {
        setOf(
            NavRoutes.Dashboard.route,
            NavRoutes.Library.route,
            NavRoutes.Transfers.route,
            NavRoutes.Settings.route
        )
    }
    val isPrimaryTab = currentRoute in primaryRoutes

    // Clean, persistent multiple-back-stack tab switching
    val navigateTab: (String) -> Unit = remember(currentRoute, navController) {
        { targetRoute ->
            if (targetRoute != currentRoute) {
                navController.navigate(targetRoute) {
                    popUpTo(NavRoutes.Dashboard.route) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    }

    val isImmersiveMedia = currentRoute.startsWith("player_video") || currentRoute.startsWith("player_audio") || currentRoute.startsWith("viewer_image")

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        bottomBar = {
            AnimatedVisibility(
                visible = isPrimaryTab,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(200)),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(180))
            ) {
                VaultiaBottomNav(
                    currentRoute = currentRoute,
                    onNavigate = navigateTab
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavRoutes.Splash.route,
            modifier = if (isImmersiveMedia) Modifier else Modifier.padding(innerPadding),
            enterTransition = {
                val fromPrimary = initialState.destination.route in primaryRoutes
                val toPrimary = targetState.destination.route in primaryRoutes
                if (fromPrimary && toPrimary) {
                    // Bottom tab switching: subtle fade with slight horizontal movement (220ms)
                    fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                            slideInHorizontally(
                                initialOffsetX = { fullWidth -> (fullWidth * 0.08f).toInt() },
                                animationSpec = tween(220, easing = FastOutSlowInEasing)
                            )
                } else {
                    // Nested screen entry: smooth horizontal slide and fade (260ms)
                    slideInHorizontally(
                        initialOffsetX = { fullWidth -> (fullWidth * 0.40f).toInt() },
                        animationSpec = tween(260, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(260))
                }
            },
            exitTransition = {
                val fromPrimary = initialState.destination.route in primaryRoutes
                val toPrimary = targetState.destination.route in primaryRoutes
                if (fromPrimary && toPrimary) {
                    fadeOut(animationSpec = tween(180, easing = FastOutLinearInEasing)) +
                            slideOutHorizontally(
                                targetOffsetX = { fullWidth -> -(fullWidth * 0.08f).toInt() },
                                animationSpec = tween(180, easing = FastOutLinearInEasing)
                            )
                } else {
                    slideOutHorizontally(
                        targetOffsetX = { fullWidth -> -(fullWidth * 0.25f).toInt() },
                        animationSpec = tween(240, easing = FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(200))
                }
            },
            popEnterTransition = {
                val fromPrimary = initialState.destination.route in primaryRoutes
                val toPrimary = targetState.destination.route in primaryRoutes
                if (fromPrimary && toPrimary) {
                    fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                            slideInHorizontally(
                                initialOffsetX = { fullWidth -> -(fullWidth * 0.08f).toInt() },
                                animationSpec = tween(220, easing = FastOutSlowInEasing)
                            )
                } else {
                    slideInHorizontally(
                        initialOffsetX = { fullWidth -> -(fullWidth * 0.25f).toInt() },
                        animationSpec = tween(260, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(260))
                }
            },
            popExitTransition = {
                val fromPrimary = initialState.destination.route in primaryRoutes
                val toPrimary = targetState.destination.route in primaryRoutes
                if (fromPrimary && toPrimary) {
                    fadeOut(animationSpec = tween(180, easing = FastOutLinearInEasing)) +
                            slideOutHorizontally(
                                targetOffsetX = { fullWidth -> (fullWidth * 0.08f).toInt() },
                                animationSpec = tween(180, easing = FastOutLinearInEasing)
                            )
                } else {
                    slideOutHorizontally(
                        targetOffsetX = { fullWidth -> (fullWidth * 0.40f).toInt() },
                        animationSpec = tween(240, easing = FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(200))
                }
            }
        ) {
            // Splash Screen
            composable(NavRoutes.Splash.route) {
                SplashScreen(
                    secureStorage = app.secureStorage,
                    vaultRepository = app.vaultRepository,
                    onNavigateToWelcome = {
                        navController.navigate(NavRoutes.Welcome.route) {
                            popUpTo(NavRoutes.Splash.route) { inclusive = true }
                        }
                    },
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

            // First-Time Welcome Onboarding Screen (Matching Images 2 & 5)
            composable(NavRoutes.Welcome.route) {
                WelcomeOnboardingScreen(
                    onComplete = {
                        app.secureStorage.isFirstLaunchCompleted = true
                        if (!app.secureStorage.isTelegramConfigured) {
                            navController.navigate(NavRoutes.Onboarding.route) {
                                popUpTo(NavRoutes.Welcome.route) { inclusive = true }
                            }
                        } else {
                            navController.navigate(NavRoutes.Dashboard.route) {
                                popUpTo(NavRoutes.Welcome.route) { inclusive = true }
                            }
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
                    onNavigateTab = navigateTab,
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
                    onNavigateTab = navigateTab
                )
            }

            // Transfers Screen
            composable(NavRoutes.Transfers.route) {
                TransfersScreen(
                    transferViewModel = transferViewModel,
                    onNavigateTab = navigateTab
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
                    onNavigateTab = navigateTab,
                    onNavigateToAutoBackup = { navController.navigate(NavRoutes.AutoBackup.route) },
                    onNavigateToAbout = { navController.navigate(NavRoutes.About.route) },
                    onNavigateToWelcome = { navController.navigate(NavRoutes.Welcome.route) }
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
}
