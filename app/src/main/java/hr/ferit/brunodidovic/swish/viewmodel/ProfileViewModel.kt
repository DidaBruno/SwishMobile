package hr.ferit.brunodidovic.swish.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import hr.ferit.brunodidovic.swish.domain.MILESTONES
import hr.ferit.brunodidovic.swish.domain.ProfileStats
import hr.ferit.brunodidovic.swish.domain.StatsCalculator
import hr.ferit.brunodidovic.swish.model.User
import hr.ferit.brunodidovic.swish.model.Workout
import hr.ferit.brunodidovic.swish.repository.AuthRepository
import hr.ferit.brunodidovic.swish.repository.UserRepository
import hr.ferit.brunodidovic.swish.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

data class AchievementTier(
    val id: String,
    val threshold: Int,
    val unlocked: Boolean
)

data class AchievementFamily(
    val name: String,
    val current: Int,
    val tiers: List<AchievementTier>
)

sealed class ProfileUiState {
    object Loading : ProfileUiState()
    data class Success(
        val username: String,
        val memberSince: String,
        val stats: ProfileStats,
        val achievements: List<AchievementFamily>
    ) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val workoutRepository: WorkoutRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        val uid = authRepository.currentUser?.uid
        if (uid == null) {
            _uiState.value = ProfileUiState.Error("Not signed in")
        } else {
            viewModelScope.launch {
                combine(
                    userRepository.getUserById(uid),
                    workoutRepository.getWorkoutsForUser(uid)
                ) { user, workouts -> buildState(user, workouts) }
                    .catch { e ->
                        _uiState.value =
                            ProfileUiState.Error(e.message ?: "Failed to load profile")
                    }
                    .collect { _uiState.value = it }
            }
        }
    }

    private fun buildState(user: User?, workouts: List<Workout>): ProfileUiState {
        val stats = StatsCalculator.computeStats(workouts)
        val families = MILESTONES
            .groupBy { it.family }
            .map { (family, defs) ->
                AchievementFamily(
                    name = family,
                    current = defs.first().valueOf(stats),
                    tiers = defs.map { def ->
                        AchievementTier(
                            id = def.id,
                            threshold = def.threshold,
                            unlocked = def.valueOf(stats) >= def.threshold
                        )
                    }
                )
            }
        return ProfileUiState.Success(
            username = user?.username?.ifBlank { "Unknown" } ?: "Unknown",
            memberSince = formatMemberSince(user?.createdAt),
            stats = stats,
            achievements = families
        )
    }

    private fun formatMemberSince(createdAt: Timestamp?): String {
        if (createdAt == null) return "—"
        val date = Instant.ofEpochMilli(createdAt.toDate().time)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        return date.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH))
    }
}