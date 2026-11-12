package com.allen.hearme.navigation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
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
    onRequestMicPermission: () -> Unit = {},
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val openNotificationAccessSettings = {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

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
                onToggleSmartListening = { enable ->
                    if (enable) {
                        val hasMicPermission = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED

                        if (hasMicPermission) {
                            viewModel.toggleSmartListening(true)
                        } else {
                            onRequestMicPermission()
                        }
                    } else {
                        viewModel.toggleSmartListening(false)
                    }
                },
                listeningStatusText = uiState.listeningStatusText,
                earphoneName = uiState.earphoneName,
                isEarphoneConnected = uiState.isEarphoneConnected,
                rmsEnergy = uiState.rmsEnergy,
                isSpeechActive = uiState.isSpeechActive,
                activeMediaTrack = uiState.activeMediaTrack,
                hasNotificationListenerAccess = uiState.hasNotificationListenerAccess,
                onGrantNotificationAccess = openNotificationAccessSettings,
                onSimulateInterruption = { viewModel.simulateMediaInterruption() },
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
                hasNotificationListenerAccess = uiState.hasNotificationListenerAccess,
                onAutoActivationChange = { viewModel.setAutoActivation(it) },
                onSensitivityChange = { viewModel.setSensitivity(it) },
                onBehaviorChange = { viewModel.setInterruptionBehavior(it) },
                onTrainingRemindersChange = { viewModel.setTrainingReminders(it) },
                onAutoRestorePlaybackChange = { viewModel.setAutoRestorePlayback(it) },
                onGrantNotificationAccess = openNotificationAccessSettings,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
