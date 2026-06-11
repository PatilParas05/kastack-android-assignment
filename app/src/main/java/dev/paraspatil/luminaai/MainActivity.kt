package dev.paraspatil.luminaai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dev.paraspatil.luminaai.data.sync.SyncWorker
import dev.paraspatil.luminaai.presentation.home.HomeScreen
import dev.paraspatil.luminaai.presentation.onboarding.OnboardingScreen
import dev.paraspatil.luminaai.ui.theme.LuminaAITheme
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupSyncWorker()

        setContent {
            LuminaAITheme {
                LuminaAppNavigation()
            }
        }
    }

    private fun setupSyncWorker() {
        // Requirement: "Runs only on network availability (use NetworkType.CONNECTED constraint)"
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Setting it to run periodically
        val syncWorkRequest = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "LuminaSyncWork",
            ExistingPeriodicWorkPolicy.KEEP,
            syncWorkRequest
        )
    }
}

@Composable
fun LuminaAppNavigation() {
    val navController = rememberNavController()

    // A simple NavHost handling the two main flows
    NavHost(navController = navController, startDestination = "onboarding") {

        composable("onboarding") {
            OnboardingScreen(
                onFinishOnboarding = {
                    // Navigate to home and remove onboarding from backstack
                    navController.navigate("home") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }

        composable("home") {
            HomeScreen()
        }
    }
}