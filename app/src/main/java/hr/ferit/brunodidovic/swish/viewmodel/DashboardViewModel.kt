package hr.ferit.brunodidovic.swish.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hr.ferit.brunodidovic.swish.model.Workout
import hr.ferit.brunodidovic.swish.repository.AuthRepository
import hr.ferit.brunodidovic.swish.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

sealed class DashboardUiState {
    object Loading : DashboardUiState()
    data class Success(
        val todayWorkout: Workout?,
        val lastWorkout: Workout?,
        val streak: Int
    ) : DashboardUiState()
    data class Error(val message: String) : DashboardUiState()
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    private fun loadDashboard() {
        val userId = authRepository.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                workoutRepository.getWorkoutsForUser(userId).collect { workouts ->
                    val today = LocalDate.now().toString()

                    val todayWorkout = workouts.find { it.date == today }
                    // workouts are already sorted by date descending, so the most recent
                    // one that isn't today is the first that doesn't match today's date
                    val lastWorkout = workouts.firstOrNull { it.date != today }
                    val streak = calculateStreak(workouts)

                    _uiState.value = DashboardUiState.Success(
                        todayWorkout = todayWorkout,
                        lastWorkout = lastWorkout,
                        streak = streak
                    )
                }
            } catch (e: Exception) {
                _uiState.value = DashboardUiState.Error(e.message ?: "Failed to load workouts")
            }
        }
    }

    private fun calculateStreak(workouts: List<Workout>): Int {
        if (workouts.isEmpty()) return 0

        val workoutDates = workouts.map { it.date }.toSet()
        var streak = 0
        var currentDate = LocalDate.now()

        // if no workout today, start counting from yesterday
        if (!workoutDates.contains(currentDate.toString())) {
            currentDate = currentDate.minusDays(1)
        }

        while (workoutDates.contains(currentDate.toString())) {
            streak++
            currentDate = currentDate.minusDays(1)
        }

        return streak
    }
}