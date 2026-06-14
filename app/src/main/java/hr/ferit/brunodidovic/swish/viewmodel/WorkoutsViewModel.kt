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
import javax.inject.Inject

sealed class WorkoutsUiState {
    object Loading : WorkoutsUiState()
    data class Success(val workouts: List<Workout>) : WorkoutsUiState()
    data class Error(val message: String) : WorkoutsUiState()
}

@HiltViewModel
class WorkoutsViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<WorkoutsUiState>(WorkoutsUiState.Loading)
    val uiState: StateFlow<WorkoutsUiState> = _uiState.asStateFlow()

    // the full unfiltered list, filter without re-fetching
    private var allWorkouts: List<Workout> = emptyList()

    private val _selectedYear = MutableStateFlow<String?>(null)
    val selectedYear: StateFlow<String?> = _selectedYear.asStateFlow()

    private val _selectedMonth = MutableStateFlow<String?>(null)
    val selectedMonth: StateFlow<String?> = _selectedMonth.asStateFlow()

    init {
        loadWorkouts()
    }

    private fun loadWorkouts() {
        val userId = authRepository.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                workoutRepository.getWorkoutsForUser(userId).collect { workouts ->
                    allWorkouts = workouts
                    applyFilter()
                }
            } catch (e: Exception) {
                _uiState.value = WorkoutsUiState.Error(e.message ?: "Failed to load workouts")
            }
        }
    }

    fun selectYear(year: String?) {
        _selectedYear.value = year
        // reset month when year changes so you don't keep a month from another year
        _selectedMonth.value = null
        applyFilter()
    }

    fun selectMonth(month: String?) {
        _selectedMonth.value = month
        applyFilter()
    }

    private fun applyFilter() {
        val year = _selectedYear.value
        val month = _selectedMonth.value

        val filtered = allWorkouts.filter { workout ->
            val matchesYear = year == null || workout.date.startsWith(year)
            val matchesMonth = month == null || workout.date.startsWith(month)
            matchesYear && matchesMonth
        }
        _uiState.value = WorkoutsUiState.Success(filtered)
    }

    // distinct years that have workouts, e.g. ["2026", "2025"]
    fun availableYears(): List<String> {
        return allWorkouts
            .map { it.date.take(4) }   // "2026-06-09" → "2026"
            .distinct()
            .sortedDescending()
    }

    // months within the selected year, e.g. ["2026-06", "2026-05"]
    fun availableMonthsForYear(year: String?): List<String> {
        if (year == null) return emptyList()
        return allWorkouts
            .filter { it.date.startsWith(year) }
            .map { it.date.take(7) }   // "2026-06-09" → "2026-06"
            .distinct()
            .sortedDescending()
    }
}