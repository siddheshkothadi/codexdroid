package me.siddheshkothadi.codexdroid.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import me.siddheshkothadi.codexdroid.ui.session.SessionScreen
import me.siddheshkothadi.codexdroid.ui.session.SessionViewModel
import me.siddheshkothadi.codexdroid.ui.setup.SetupScreen
import me.siddheshkothadi.codexdroid.ui.setup.SetupUiState
import me.siddheshkothadi.codexdroid.ui.setup.SetupViewModel
import me.siddheshkothadi.codexdroid.navigation.CodexDroidAppLink

sealed class Screen(val route: String) {
    object Setup : Screen("setup?connectionId={connectionId}") {
        fun createRoute(connectionId: String? = null) = 
            if (connectionId != null) "setup?connectionId=$connectionId" else "setup"
    }
    object Session : Screen("session?connectionId={connectionId}&threadId={threadId}&turnId={turnId}&openLatest={openLatest}") {
        fun createRoute(
            connectionId: String? = null,
            threadId: String? = null,
            turnId: String? = null,
            openLatest: Boolean = false,
        ): String {
            val parts = mutableListOf<String>()
            if (!connectionId.isNullOrBlank()) parts += "connectionId=$connectionId"
            if (!threadId.isNullOrBlank()) parts += "threadId=$threadId"
            if (!turnId.isNullOrBlank()) parts += "turnId=$turnId"
            if (openLatest) parts += "openLatest=true"
            return if (parts.isEmpty()) "session" else "session?" + parts.joinToString("&")
        }
    }
}

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String,
    appLinks: Flow<CodexDroidAppLink>? = null,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(navController, appLinks) {
        appLinks ?: return@LaunchedEffect
        appLinks.collectLatest { link ->
            val route =
                Screen.Session.createRoute(
                    connectionId = link.connectionId,
                    threadId = link.threadId,
                    turnId = link.turnId,
                    openLatest = link.openLatest,
                )
            navController.navigate(route) {
                launchSingleTop = true
                popUpTo("session") { inclusive = false }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(
            route = Screen.Setup.route,
            arguments = listOf(
                navArgument("connectionId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            val viewModel: SetupViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()
            val connectionToEdit by viewModel.connectionToEdit.collectAsState()
            val connections by viewModel.connections.collectAsState()

            LaunchedEffect(uiState) {
                if (uiState is SetupUiState.Success) {
                    navController.navigate(Screen.Session.route) {
                        popUpTo(Screen.Setup.route) { inclusive = true }
                    }
                }
            }

            SetupScreen(
                onSaveClick = { name, url, secret ->
                    viewModel.saveConnection(name, url, secret)
                },
                onBackClick = {
                    navController.popBackStack()
                },
                canNavigateBack = connections.isNotEmpty(),
                initialConnection = connectionToEdit,
                isLoading = uiState is SetupUiState.Loading,
                errorMessage = (uiState as? SetupUiState.Error)?.message
            )
        }
        composable(
            route = Screen.Session.route,
            arguments = listOf(
                navArgument("connectionId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("threadId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("turnId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("openLatest") {
                    type = NavType.BoolType
                    defaultValue = false
                },
            )
        ) {
            val viewModel: SessionViewModel = hiltViewModel()
            val connections by viewModel.connections.collectAsState()
            val connectionId = it.arguments?.getString("connectionId")
            val threadId = it.arguments?.getString("threadId")
            val turnId = it.arguments?.getString("turnId")
            val openLatest = it.arguments?.getBoolean("openLatest") ?: false

            LaunchedEffect(connectionId, threadId, turnId, openLatest) {
                viewModel.handleAppLink(connectionId, threadId, turnId, openLatest)
            }

            SessionScreen(
                viewModel = viewModel,
                onAddConnectionClick = {
                    navController.navigate(Screen.Setup.createRoute())
                },
                onEditConnectionClick = { connectionId ->
                    navController.navigate(Screen.Setup.createRoute(connectionId))
                },
                onNoConnections = {
                    navController.navigate(Screen.Setup.createRoute()) {
                        popUpTo(Screen.Session.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
