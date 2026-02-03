package me.siddheshkothadi.codexdroid

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import me.siddheshkothadi.codexdroid.navigation.toCodexDroidAppLinkOrNull
import me.siddheshkothadi.codexdroid.ui.navigation.NavGraph
import me.siddheshkothadi.codexdroid.ui.theme.CodexDroidTheme
import android.content.pm.PackageManager
import android.os.Build
import android.content.Intent

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    private val requestNotificationsPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
            // Best-effort: if denied, the keep-alive foreground service may not be able to start reliably.
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        
        splashScreen.setKeepOnScreenCondition {
            viewModel.isLoading.value
        }

        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted =
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                requestNotificationsPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            CodexDroidTheme {
                val navController = rememberNavController()
                val startDestination by viewModel.startDestination.collectAsState()
                val isLoading by viewModel.isLoading.collectAsState()

                if (!isLoading) {
                    NavGraph(
                        navController = navController,
                        startDestination = startDestination,
                        appLinks = viewModel.appLinks
                    )
                }
            }
        }

        intent?.toCodexDroidAppLinkOrNull()?.let { viewModel.onAppLink(it) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.toCodexDroidAppLinkOrNull()?.let { viewModel.onAppLink(it) }
    }
}
