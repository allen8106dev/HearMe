package com.allen.hearme.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.allen.hearme.feature.history.ui.HistoryScreen
import com.allen.hearme.feature.home.ui.HomeScreen
import com.allen.hearme.feature.onboarding.ui.OnboardingScreen
import com.allen.hearme.feature.settings.ui.SettingsScreen
import com.allen.hearme.feature.training.ui.TrainingScreen
import com.allen.hearme.ui.HearMeViewModel

@Composable
fun AppNavHost(
    viewModel: HearMeViewModel,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val uiState by viewModel.uiState.collectAsState()

    val startDestination = if (uiState.preferences.onboardingCompleted) {
        Screen.Home.route
    } else {
        Screen.Onboarding.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onCompleteOnboarding = {
                    viewModel.completeOnboarding()
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                isSmartListeningOn = uiState.isSmartListeningOn,
                onToggleSmartListening = { enable -> viewModel.toggleSmartListening(enable) },
                listeningStatusText = uiState.listeningStatusText,
                earphoneName = uiState.earphoneName,
                isEarphoneConnected = uiState.isEarphoneConnected,
                onNavigateToTraining = { navController.navigate(Screen.Training.route) },
                onNavigateToHistory = { navController.navigate(Screen.History.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.Training.route) {
            TrainingScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.History.route) {
            HistoryScreen(
                sessions = uiState.sessions,
                onClearHistory = { viewModel.clearHistory() },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                preferences = uiState.preferences,
                onAutoActivationChange = { viewModel.setAutoActivation(it) },
                onSensitivityChange = { viewModel.setSensitivity(it) },
                onBehaviorChange = { viewModel.setInterruptionBehavior(it) },
                onTrainingRemindersChange = { viewModel.setTrainingReminders(it) },
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
