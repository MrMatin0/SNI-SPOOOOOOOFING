package com.example.snispoofing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.snispoofing.ui.screens.*
import com.example.snispoofing.ui.theme.SNISpoofingTheme
import com.example.snispoofing.ui.viewmodel.SniSpoofViewModel

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object Config : Screen("config", "Config", Icons.Default.Tune)
    object Tunnels : Screen("tunnels", "Tunnels", Icons.Default.AltRoute)
    object Inspector : Screen("inspector", "Inspector", Icons.Default.Code)
    object Diagnostics : Screen("diagnostics", "Diagnostics", Icons.Default.BugReport)
}

class MainActivity : ComponentActivity() {
    private val viewModel: SniSpoofViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SNISpoofingTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Dashboard.route

                val items = listOf(
                    Screen.Dashboard,
                    Screen.Config,
                    Screen.Tunnels,
                    Screen.Inspector,
                    Screen.Diagnostics
                )

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar {
                            items.forEach { screen ->
                                NavigationBarItem(
                                    modifier = Modifier.testTag("nav_${screen.route}"),
                                    icon = { Icon(screen.icon, contentDescription = screen.title) },
                                    label = { Text(screen.title) },
                                    selected = currentRoute == screen.route,
                                    onClick = {
                                        if (currentRoute != screen.route) {
                                            navController.navigate(screen.route) {
                                                popUpTo(navController.graph.startDestinationId) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Dashboard.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(Screen.Dashboard.route) {
                            DashboardScreen(
                                viewModel = viewModel,
                                onNavigateToConfig = { navController.navigate(Screen.Config.route) },
                                onNavigateToInspector = { navController.navigate(Screen.Inspector.route) },
                                onNavigateToDiagnostics = { navController.navigate(Screen.Diagnostics.route) }
                            )
                        }
                        composable(Screen.Config.route) {
                            ConfigScreen(
                                viewModel = viewModel,
                                onNavigateToDashboard = { navController.navigate(Screen.Dashboard.route) }
                            )
                        }
                        composable(Screen.Tunnels.route) {
                            TunnelsScreen(viewModel = viewModel)
                        }
                        composable(Screen.Inspector.route) {
                            InspectorScreen(viewModel = viewModel)
                        }
                        composable(Screen.Diagnostics.route) {
                            DiagnosticsScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}
