package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.repository.NexCloudRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.dashboard.DashboardViewModel
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.NexCloudTheme
import com.example.ui.theme.NexDarkBg

enum class CurrentScreen {
    DASHBOARD,
    SETTINGS
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = NexCloudRepository.create(this)

        setContent {
            NexCloudTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = NexDarkBg
                ) {
                    val dashboardViewModel: DashboardViewModel = viewModel(
                        factory = DashboardViewModel.provideFactory(application, repository)
                    )
                    val uiState by dashboardViewModel.uiState.collectAsState()
                    var currentScreen by remember { mutableStateOf(CurrentScreen.DASHBOARD) }

                    if (!uiState.isLoggedIn) {
                        AuthScreen(
                            repository = repository,
                            onLoginSuccess = {
                                dashboardViewModel.checkSession()
                                currentScreen = CurrentScreen.DASHBOARD
                            }
                        )
                    } else {
                        when (currentScreen) {
                            CurrentScreen.DASHBOARD -> {
                                DashboardScreen(
                                    viewModel = dashboardViewModel,
                                    onOpenSettings = {
                                        currentScreen = CurrentScreen.SETTINGS
                                    }
                                )
                            }
                            CurrentScreen.SETTINGS -> {
                                SettingsScreen(
                                    repository = repository,
                                    usedBytes = uiState.storageUsedBytes,
                                    quotaBytes = uiState.quotaBytes,
                                    onBack = {
                                        currentScreen = CurrentScreen.DASHBOARD
                                        dashboardViewModel.calculateStorageUsage()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
