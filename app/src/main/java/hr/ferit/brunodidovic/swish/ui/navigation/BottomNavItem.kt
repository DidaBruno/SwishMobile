package hr.ferit.brunodidovic.swish.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    object Dashboard : BottomNavItem("dashboard", "Home", Icons.Filled.Home)
    object Workouts : BottomNavItem("workouts", "Workouts", Icons.Filled.SportsBasketball)
    object Profile : BottomNavItem("profile", "Profile", Icons.Filled.Person)
}