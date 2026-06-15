package hr.ferit.brunodidovic.swish.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hr.ferit.brunodidovic.swish.model.*
import hr.ferit.brunodidovic.swish.repository.AuthRepository
import hr.ferit.brunodidovic.swish.repository.TemplateRepository
import hr.ferit.brunodidovic.swish.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import hr.ferit.brunodidovic.swish.workers.AchievementWorker

// editable form rows — these are UI-side models, separate from the Firestore models
data class HandlingDrillInput(
    val name: String = "",
    val count: String = "",
    val unit: String = ""
)

data class GameInput(
    val playersPerTeam: String = "",
    val teamAPlayers: String = "",
    val teamAScore: String = "",
    val teamBPlayers: String = "",
    val teamBScore: String = ""
)

data class CreateWorkoutForm(
    val date: String = LocalDate.now().toString(),
    val twos: String = "",
    val threes: String = "",
    val freeThrows: String = "",
    val layups: String = "",
    val dunks: String = "",
    val handling: List<HandlingDrillInput> = emptyList(),
    val games: List<GameInput> = emptyList()
)

sealed class SaveState {
    object Idle : SaveState()
    object Saving : SaveState()
    object Saved : SaveState()
    data class Error(val message: String) : SaveState()
}

@HiltViewModel
class CreateWorkoutViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val templateRepository: TemplateRepository,
    private val authRepository: AuthRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _form = MutableStateFlow(CreateWorkoutForm())
    val form: StateFlow<CreateWorkoutForm> = _form.asStateFlow()

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    private val _templates = MutableStateFlow<List<DrillTemplate>>(emptyList())
    val templates: StateFlow<List<DrillTemplate>> = _templates.asStateFlow()

    private var editingWorkoutId: String? = null
    private var editingCreatedAt: String? = null

    // this is for checking if you need a popup when creating/editing workout
    private var initialForm: CreateWorkoutForm? = null

    init {
        loadTemplates()
    }

    private fun loadTemplates() {
        val userId = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            templateRepository.getTemplatesForUser(userId).collect {
                _templates.value = it
            }
        }
    }

    fun loadWorkoutForEditing(workoutId: String) {
        editingWorkoutId = workoutId
        viewModelScope.launch {
            workoutRepository.getWorkoutById(workoutId).collect { workout ->
                if (workout != null) {
                    editingCreatedAt = workout.createdAt
                    val loaded = CreateWorkoutForm(
                        date = workout.date,
                        twos = workout.drills.shooting.twos.toStringOrEmpty(),
                        threes = workout.drills.shooting.threes.toStringOrEmpty(),
                        freeThrows = workout.drills.shooting.freeThrows.toStringOrEmpty(),
                        layups = workout.drills.shooting.layups.toStringOrEmpty(),
                        dunks = workout.drills.shooting.dunks.toStringOrEmpty(),
                        handling = workout.drills.handling.map {
                            HandlingDrillInput(
                                name = it.name,
                                count = it.count.toString(),
                                unit = it.unit
                            )
                        },
                        games = workout.games.map {
                            GameInput(
                                playersPerTeam = it.playersPerTeam.toString(),
                                teamAPlayers = it.teamA.players,
                                teamAScore = it.teamA.score.toString(),
                                teamBPlayers = it.teamB.players,
                                teamBScore = it.teamB.score.toString()
                            )
                        }
                    )
                    _form.value = loaded
                    initialForm = loaded
                }
            }
        }
    }
    fun updateDate(date: String) { _form.value = _form.value.copy(date = date) }

    // --- shooting field updates ---
    fun updateTwos(v: String) { _form.value = _form.value.copy(twos = v.filterDigits()) }
    fun updateThrees(v: String) { _form.value = _form.value.copy(threes = v.filterDigits()) }
    fun updateFreeThrows(v: String) { _form.value = _form.value.copy(freeThrows = v.filterDigits()) }
    fun updateLayups(v: String) { _form.value = _form.value.copy(layups = v.filterDigits()) }
    fun updateDunks(v: String) { _form.value = _form.value.copy(dunks = v.filterDigits()) }

    // --- handling drills ---
    fun addHandlingDrill() {
        _form.value = _form.value.copy(
            handling = _form.value.handling + HandlingDrillInput(unit = "reps")
        )
    }

    fun addHandlingDrillFromTemplate(template: DrillTemplate) {
        _form.value = _form.value.copy(
            handling = _form.value.handling + HandlingDrillInput(
                name = template.name,
                unit = template.defaultUnit
            )
        )
    }

    fun updateHandlingDrill(index: Int, drill: HandlingDrillInput) {
        val updated = _form.value.handling.toMutableList()
        if (index in updated.indices) {
            updated[index] = drill
            _form.value = _form.value.copy(handling = updated)
        }
    }

    fun removeHandlingDrill(index: Int) {
        val updated = _form.value.handling.toMutableList()
        if (index in updated.indices) {
            updated.removeAt(index)
            _form.value = _form.value.copy(handling = updated)
        }
    }

    // --- games ---
    fun addGame() {
        _form.value = _form.value.copy(games = _form.value.games + GameInput())
    }

    fun updateGame(index: Int, game: GameInput) {
        val updated = _form.value.games.toMutableList()
        if (index in updated.indices) {
            updated[index] = game
            _form.value = _form.value.copy(games = updated)
        }
    }

    fun removeGame(index: Int) {
        val updated = _form.value.games.toMutableList()
        if (index in updated.indices) {
            updated.removeAt(index)
            _form.value = _form.value.copy(games = updated)
        }
    }

    fun hasUnsavedChanges(): Boolean {
        val f = _form.value
        return if (editingWorkoutId != null) {
            // edit mode: dirty if the form differs from what we loaded
            initialForm != null && f != initialForm
        } else {
            // create mode: dirty only if real content was entered
            f.hasContent()
        }
    }

    private fun CreateWorkoutForm.hasContent(): Boolean {
        val hasShooting = listOf(twos, threes, freeThrows, layups, dunks)
            .any { (it.toIntOrNull() ?: 0) > 0 }
        val hasHandling = handling.any {
            it.name.isNotBlank() || (it.count.toIntOrNull() ?: 0) > 0
        }
        val hasGames = games.any {
            it.teamAPlayers.isNotBlank() || it.teamBPlayers.isNotBlank() ||
                    (it.teamAScore.toIntOrNull() ?: 0) > 0 || (it.teamBScore.toIntOrNull() ?: 0) > 0
        }
        return hasShooting || hasHandling || hasGames
    }

    // --- save a handling drill as a reusable template ---
    fun saveDrillAsTemplate(name: String, unit: String) {
        val userId = authRepository.currentUser?.uid ?: return
        if (name.isBlank()) return
        viewModelScope.launch {
            templateRepository.saveTemplate(
                DrillTemplate(
                    userId = userId,
                    name = name.trim(),
                    defaultUnit = unit.trim(),
                    createdAt = System.currentTimeMillis().toString()
                )
            )
        }
    }

    // --- save the whole workout ---
    fun saveWorkout() {
        val userId = authRepository.currentUser?.uid ?: return
        val f = _form.value

        // validation — require at least one meaningful entry
        val hasShooting = listOf(f.twos, f.threes, f.freeThrows, f.layups, f.dunks)
            .any { (it.toIntOrNull() ?: 0) > 0 }
        val hasHandling = f.handling.any { it.name.isNotBlank() && (it.count.toIntOrNull() ?: 0) > 0 }
        val hasGames = f.games.any {
            it.teamAPlayers.isNotBlank() || it.teamBPlayers.isNotBlank() ||
                    (it.teamAScore.toIntOrNull() ?: 0) > 0 || (it.teamBScore.toIntOrNull() ?: 0) > 0
        }

        if (!hasShooting && !hasHandling && !hasGames) {
            _saveState.value = SaveState.Error("Add at least one shooting stat, handling drill, or game")
            return
        }

        viewModelScope.launch {
            _saveState.value = SaveState.Saving

            val workout = Workout(
                id = editingWorkoutId ?: "",
                userId = userId,
                date = f.date,
                createdAt = editingCreatedAt ?: java.time.Instant.now().toString(),
                drills = Drills(
                    shooting = ShootingDrills(
                        twos = f.twos.toIntOrNull() ?: 0,
                        threes = f.threes.toIntOrNull() ?: 0,
                        freeThrows = f.freeThrows.toIntOrNull() ?: 0,
                        layups = f.layups.toIntOrNull() ?: 0,
                        dunks = f.dunks.toIntOrNull() ?: 0
                    ),
                    handling = f.handling
                        .filter { it.name.isNotBlank() }
                        .map {
                            HandlingDrill(
                                name = it.name.trim(),
                                count = it.count.toIntOrNull() ?: 0,
                                unit = it.unit.trim()
                            )
                        }
                ),
                games = f.games.map {
                    Game(
                        playersPerTeam = it.playersPerTeam.toIntOrNull() ?: 0,
                        teamA = TeamInfo(
                            players = it.teamAPlayers.trim(),
                            score = it.teamAScore.toIntOrNull() ?: 0
                        ),
                        teamB = TeamInfo(
                            players = it.teamBPlayers.trim(),
                            score = it.teamBScore.toIntOrNull() ?: 0
                        )
                    )
                }
            )

            val result = workoutRepository.saveWorkout(workout)
            _saveState.value = if (result.isSuccess) {
                enqueueAchievementCheck()
                SaveState.Saved
            } else {
                SaveState.Error(result.exceptionOrNull()?.message ?: "Failed to save")
            }
        }
    }

    private fun enqueueAchievementCheck() {
        WorkManager.getInstance(appContext).enqueueUniqueWork(
            "achievement_check",
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<AchievementWorker>().build()
        )
    }
}

// helper: keep only digits so number fields can't get letters
private fun String.filterDigits(): String = this.filter { it.isDigit() }

// helper: make it a string or empty
private fun Int.toStringOrEmpty(): String = if (this == 0) "" else this.toString()