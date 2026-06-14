package hr.ferit.brunodidovic.swish.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import hr.ferit.brunodidovic.swish.ui.dashboard.DashboardScreen
import hr.ferit.brunodidovic.swish.ui.profile.ProfileScreen
import hr.ferit.brunodidovic.swish.ui.theme.*
import hr.ferit.brunodidovic.swish.ui.workouts.WorkoutDetailScreen
import hr.ferit.brunodidovic.swish.ui.workouts.WorkoutsScreen
import hr.ferit.brunodidovic.swish.ui.workouts.CreateWorkoutScreen

private const val WORKOUT_DETAIL_ROUTE = "workout_detail"
private const val CREATE_WORKOUT_ROUTE = "create_workout"

@Composable
fun MainScreen(
    onLoggedOut: () -> Unit
) {
    val navController = rememberNavController()

    val items = listOf(
        BottomNavItem.Dashboard,
        BottomNavItem.Workouts,
        BottomNavItem.Profile
    )

    Scaffold(
        containerColor = Bg,
        bottomBar = {
            NavigationBar(
                containerColor = Surface2
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                items.forEach { item ->
                    val selected = currentDestination?.hierarchy?.any {
                        it.route == item.route
                    } == true

                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label
                            )
                        },
                        label = { Text(item.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Orange,
                            selectedTextColor = Orange,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted,
                            indicatorColor = Surface2
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Dashboard.route) {
                DashboardScreen(
                    onCreateWorkout = {
                        navController.navigate(CREATE_WORKOUT_ROUTE)
                    }
                )
            }
            composable(BottomNavItem.Workouts.route) {
                WorkoutsScreen(
                    onWorkoutClick = { workoutId ->
                        navController.navigate("$WORKOUT_DETAIL_ROUTE/$workoutId")
                    },
                    onCreateWorkout = {
                        navController.navigate(CREATE_WORKOUT_ROUTE)
                    }
                )
            }
            composable(
                route = "$WORKOUT_DETAIL_ROUTE/{workoutId}",
                arguments = listOf(navArgument("workoutId") { type = NavType.StringType })
            ) { backStackEntry ->
                val workoutId = backStackEntry.arguments?.getString("workoutId") ?: ""
                WorkoutDetailScreen(
                    workoutId = workoutId,
                    onBack = { navController.popBackStack() },
                    onEdit = { id ->
                        navController.navigate("$CREATE_WORKOUT_ROUTE?workoutId=$id")
                    }
                )
            }
            composable(
                route = "$CREATE_WORKOUT_ROUTE?workoutId={workoutId}",
                arguments = listOf(
                    navArgument("workoutId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val workoutId = backStackEntry.arguments?.getString("workoutId")
                CreateWorkoutScreen(
                    workoutId = workoutId,
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }
            composable(BottomNavItem.Profile.route) {
                ProfileScreen(
                    onLoggedOut = onLoggedOut
                )
            }
        }
    }
}