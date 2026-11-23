package com.example.navigation

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.AppContainer
import com.example.calculator.CalculatorViewModel
import com.example.calculator.ui.CalculatorScreen
import com.example.history.HistoryScreen
import com.example.history.HistoryViewModel
import com.example.settings.SettingsScreen
import com.example.settings.SettingsViewModel

object LuxaRoutes {
    const val CALCULATOR = "calculator"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
}

private const val NAV_TRANSITION_DURATION_MS = 300
private val NavTransitionEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

@Composable
fun LuxaNavHost(container: AppContainer) {
    val navController = rememberNavController()

    val calculatorViewModel: CalculatorViewModel = viewModel(
        factory = CalculatorViewModel.Factory(
            mathEngine = container.mathEngine,
            lazyHistoryRepository = container.lazyHistoryRepository,
            settingsPreferences = container.settingsPreferences
        )
    )

    val slideSpec = remember {
        tween<IntOffset>(
            durationMillis = NAV_TRANSITION_DURATION_MS,
            easing = NavTransitionEasing
        )
    }
    val parallaxAlphaSpec = remember {
        tween<Float>(
            durationMillis = NAV_TRANSITION_DURATION_MS,
            easing = NavTransitionEasing
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        NavHost(
            navController = navController,
            startDestination = LuxaRoutes.CALCULATOR,
            enterTransition = {
                slideInHorizontally(
                    animationSpec = slideSpec,
                    initialOffsetX = { fullWidth -> fullWidth }
                )
            },
            exitTransition = {
                slideOutHorizontally(
                    animationSpec = slideSpec,
                    targetOffsetX = { fullWidth -> -fullWidth / 4 }
                ) + fadeOut(
                    animationSpec = parallaxAlphaSpec,
                    targetAlpha = 0.85f
                )
            },
            popEnterTransition = {
                slideInHorizontally(
                    animationSpec = slideSpec,
                    initialOffsetX = { fullWidth -> -fullWidth / 4 }
                ) + fadeIn(
                    animationSpec = parallaxAlphaSpec,
                    initialAlpha = 0.85f
                )
            },
            popExitTransition = {
                slideOutHorizontally(
                    animationSpec = slideSpec,
                    targetOffsetX = { fullWidth -> fullWidth }
                )
            }
        ) {
            composable(LuxaRoutes.CALCULATOR) {
                CalculatorScreen(
                    viewModel = calculatorViewModel,
                    onOpenHistory = {
                        if (navController.currentBackStackEntry?.destination?.route == LuxaRoutes.CALCULATOR) {
                            navController.navigate(LuxaRoutes.HISTORY) {
                                launchSingleTop = true
                            }
                        }
                    },
                    onOpenSettings = {
                        if (navController.currentBackStackEntry?.destination?.route == LuxaRoutes.CALCULATOR) {
                            navController.navigate(LuxaRoutes.SETTINGS) {
                                launchSingleTop = true
                            }
                        }
                    },
                    onClearHistory = calculatorViewModel::clearHistory
                )
            }

            composable(LuxaRoutes.HISTORY) {
                val historyViewModel: HistoryViewModel = viewModel(
                    factory = HistoryViewModel.Factory(
                        historyRepository = container.historyRepository
                    )
                )
                HistoryScreen(
                    viewModel = historyViewModel,
                    onNavigateBack = {
                        if (navController.currentBackStackEntry?.destination?.route == LuxaRoutes.HISTORY) {
                            navController.popBackStack()
                        }
                    },
                    onSelectEntry = { entry ->
                        calculatorViewModel.loadExpressionFromHistory(entry.expression)
                        if (navController.currentBackStackEntry?.destination?.route == LuxaRoutes.HISTORY) {
                            navController.popBackStack()
                        }
                    }
                )
            }

            composable(LuxaRoutes.SETTINGS) {
                val settingsViewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModel.Factory(
                        settingsPreferences = container.settingsPreferences,
                        historyRepository = container.historyRepository
                    )
                )
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onNavigateBack = {
                        if (navController.currentBackStackEntry?.destination?.route == LuxaRoutes.SETTINGS) {
                            navController.popBackStack()
                        }
                    },
                    onOpenHistory = {
                        if (navController.currentBackStackEntry?.destination?.route == LuxaRoutes.SETTINGS) {
                            navController.navigate(LuxaRoutes.HISTORY) {
                                launchSingleTop = true
                            }
                        }
                    }
                )
            }
        }
    }
}
