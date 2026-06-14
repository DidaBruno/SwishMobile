package hr.ferit.brunodidovic.swish.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
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
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import com.google.firebase.Timestamp

// --- lifetime totals computed from all workouts ---
data class ProfileStats(
    val workouts: Int = 0,
    val longestStreak: Int = 0,
    val twos: Int = 0,
    val threes: Int = 0,
    val freeThrows: Int = 0,
    val layups: Int = 0,
    val dunks: Int = 0,
    val totalShots: Int = 0,
    val gamesPlayed: Int = 0
)

// --- one unlockable tier within a family, e.g. "threes_1000" ---
data class AchievementTier(
    val id: String,
    val threshold: Int,
    val unlocked: Boolean
)

// --- a group of tiers for one stat, e.g. "Threes made" ---
data class AchievementFamily(
    val name: String,
    val current: Int,
    val tiers: List<AchievementTier>
)

// --- static milestone definitions. IDs are HARD-CODED and must never change
//     (Phase 10 stores them in notifiedAchievements to avoid re-notifying). ---
private data class MilestoneDef(
    val id: String,
    val family: String,
    val threshold: Int,
    val valueOf: (ProfileStats) -> Int
)

private val MILESTONES = listOf(
    MilestoneDef("workouts_100",  "Workouts logged",  100)   { it.workouts },
    MilestoneDef("workouts_500",  "Workouts logged",  500)   { it.workouts },
    MilestoneDef("workouts_1000", "Workouts logged",  1000)  { it.workouts },
    MilestoneDef("streak_10",     "Day streak",       10)    { it.longestStreak },
    MilestoneDef("streak_50",     "Day streak",       50)    { it.longestStreak },
    MilestoneDef("streak_100",    "Day streak",       100)   { it.longestStreak },
    MilestoneDef("twos_100",      "Twos made",        100)   { it.twos },
    MilestoneDef("twos_1000",     "Twos made",        1000)  { it.twos },
    MilestoneDef("twos_10000",    "Twos made",        10000) { it.twos },
    MilestoneDef("threes_100",    "Threes made",      100)   { it.threes },
    MilestoneDef("threes_1000",   "Threes made",      1000)  { it.threes },
    MilestoneDef("threes_10000",  "Threes made",      10000) { it.threes },
    MilestoneDef("ft_100",        "Free throws made", 100)   { it.freeThrows },
    MilestoneDef("ft_1000",       "Free throws made", 1000)  { it.freeThrows },
    MilestoneDef("ft_10000",      "Free throws made", 10000) { it.freeThrows },
    MilestoneDef("layups_100",    "Layups made",      100)   { it.layups },
    MilestoneDef("layups_1000",   "Layups made",      1000)  { it.layups },
    MilestoneDef("layups_10000",  "Layups made",      10000) { it.layups },
    MilestoneDef("dunks_10",      "Dunks made",       10)    { it.dunks },
    MilestoneDef("dunks_100",     "Dunks made",       100)   { it.dunks },
    MilestoneDef("dunks_1000",    "Dunks made",       1000)  { it.dunks },
    MilestoneDef("shots_1000",    "Total shots made", 1000)  { it.totalShots },
    MilestoneDef("shots_10000",   "Total shots made", 10000) { it.totalShots },
    MilestoneDef("shots_50000",   "Total shots made", 50000) { it.totalShots },
    MilestoneDef("games_10",      "Games played",     10)    { it.gamesPlayed },
    MilestoneDef("games_50",      "Games played",     50)    { it.gamesPlayed },
    MilestoneDef("games_100",     "Games played",     100)   { it.gamesPlayed }
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
        val stats = computeStats(workouts)
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

    private fun computeStats(workouts: List<Workout>): ProfileStats {
        val twos = workouts.sumOf { it.drills.shooting.twos }
        val threes = workouts.sumOf { it.drills.shooting.threes }
        val freeThrows = workouts.sumOf { it.drills.shooting.freeThrows }
        val layups = workouts.sumOf { it.drills.shooting.layups }
        val dunks = workouts.sumOf { it.drills.shooting.dunks }
        return ProfileStats(
            workouts = workouts.size,
            longestStreak = longestStreak(workouts),
            twos = twos,
            threes = threes,
            freeThrows = freeThrows,
            layups = layups,
            dunks = dunks,
            totalShots = twos + threes + freeThrows + layups + dunks,
            gamesPlayed = workouts.sumOf { it.games.size }
        )
    }

    private fun longestStreak(workouts: List<Workout>): Int {
        // distinct calendar days, sorted ascending (multiple workouts on one day = one day)
        val days = workouts
            .mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }
            .toSortedSet()
        var longest = 0
        var run = 0
        var prev: LocalDate? = null
        for (day in days) {
            run = if (prev != null && prev.plusDays(1) == day) run + 1 else 1
            if (run > longest) longest = run
            prev = day
        }
        return longest
    }

    private fun formatMemberSince(createdAt: Timestamp?): String {
        if (createdAt == null) return "—"
        val date = Instant.ofEpochMilli(createdAt.toDate().time)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        return date.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
    }
}