package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.model.ThemeMode
import com.example.ui.screens.ActivityScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.JournalScreen
import com.example.ui.screens.ProfileGoalsScreen
import com.example.ui.screens.ProgressScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.FitTrackTheme
import com.example.ui.viewmodel.FitTrackViewModel

object FitTrackRoutes {
    const val HOME = "home"
    const val ACTIVITY = "activity"
    const val JOURNAL = "journal"
    const val PROGRESS = "progress"
    const val PROFILE_GOALS = "profile_goals"
    const val SETTINGS = "settings"
}

private enum class BottomNavDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME(
        route = FitTrackRoutes.HOME,
        label = "Home",
        selectedIcon = Icons.Rounded.Home,
        unselectedIcon = Icons.Outlined.Home
    ),
    ACTIVITY(
        route = FitTrackRoutes.ACTIVITY,
        label = "Activity",
        selectedIcon = Icons.Rounded.FitnessCenter,
        unselectedIcon = Icons.Outlined.FitnessCenter
    ),
    JOURNAL(
        route = FitTrackRoutes.JOURNAL,
        label = "Journal",
        selectedIcon = Icons.AutoMirrored.Rounded.MenuBook,
        unselectedIcon = Icons.AutoMirrored.Outlined.MenuBook
    ),
    PROGRESS(
        route = FitTrackRoutes.PROGRESS,
        label = "Progress",
        selectedIcon = Icons.Rounded.BarChart,
        unselectedIcon = Icons.Outlined.BarChart
    )
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: FitTrackViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        viewModel.refreshPermissionAndStartSensorIfAllowed()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            val darkTheme = when (uiState.userSettings.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            FitTrackTheme(darkTheme = darkTheme) {
                FitTrackApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun FitTrackApp(viewModel: FitTrackViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        val message = uiState.snackbarMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearSnackbarMessage()
        }
    }

    val showTopLevelNav = currentRoute in setOf(
        FitTrackRoutes.HOME,
        FitTrackRoutes.ACTIVITY,
        FitTrackRoutes.JOURNAL,
        FitTrackRoutes.PROGRESS
    )

    val navigateTopLevel: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (showTopLevelNav && !useNavigationRail) {
                    NavigationBar {
                        BottomNavDestination.entries.forEach { dest ->
                            val selected = currentDestination?.hierarchy?.any {
                                it.route == dest.route
                            } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTopLevel(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label) },
                                modifier = Modifier.testTag("nav_${dest.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showTopLevelNav && useNavigationRail) {
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        BottomNavDestination.entries.forEach { dest ->
                            val selected = currentDestination?.hierarchy?.any {
                                it.route == dest.route
                            } == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopLevel(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label) },
                                modifier = Modifier.testTag("rail_${dest.route}")
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = FitTrackRoutes.HOME
                    ) {
                        composable(FitTrackRoutes.HOME) {
                            HomeScreen(
                                uiState = uiState,
                                onActivityPermissionResult = viewModel::onActivityPermissionResult,
                                onAddManualSteps = viewModel::addManualSteps,
                                onSetTodaySteps = viewModel::setTodaySteps,
                                onNavigateToActivity = { navigateTopLevel(FitTrackRoutes.ACTIVITY) },
                                onNavigateToProfile = { navController.navigate(FitTrackRoutes.PROFILE_GOALS) },
                                onNavigateToSettings = { navController.navigate(FitTrackRoutes.SETTINGS) }
                            )
                        }
                        composable(FitTrackRoutes.ACTIVITY) {
                            ActivityScreen(
                                uiState = uiState,
                                onSelectWorkoutType = viewModel::selectWorkoutType,
                                onStartWorkout = viewModel::startWorkout,
                                onPauseWorkout = viewModel::pauseWorkout,
                                onStopAndSaveWorkout = viewModel::stopAndSaveWorkout,
                                onDiscardWorkout = viewModel::discardWorkout
                            )
                        }
                        composable(FitTrackRoutes.JOURNAL) {
                            JournalScreen(
                                uiState = uiState,
                                onDeleteWorkout = viewModel::deleteWorkout,
                                onNavigateToActivity = { navigateTopLevel(FitTrackRoutes.ACTIVITY) }
                            )
                        }
                        composable(FitTrackRoutes.PROGRESS) {
                            ProgressScreen(uiState = uiState)
                        }
                        composable(FitTrackRoutes.PROFILE_GOALS) {
                            ProfileGoalsScreen(
                                uiState = uiState,
                                onSaveProfile = viewModel::saveUserProfile,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable(FitTrackRoutes.SETTINGS) {
                            SettingsScreen(
                                uiState = uiState,
                                onDistanceUnitChange = viewModel::updateDistanceUnit,
                                onWeightUnitChange = viewModel::updateWeightUnit,
                                onRemindersToggle = viewModel::setRemindersEnabled,
                                onThemeModeChange = viewModel::updateThemeMode,
                                onShowMessage = viewModel::showMessage,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
