package hr.ferit.brunodidovic.swish.ui.workouts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hr.ferit.brunodidovic.swish.model.Workout
import hr.ferit.brunodidovic.swish.ui.theme.*
import hr.ferit.brunodidovic.swish.viewmodel.WorkoutDetailUiState
import hr.ferit.brunodidovic.swish.viewmodel.WorkoutDetailViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailScreen(
    workoutId: String,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    viewModel: WorkoutDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // load the workout once when the screen first appears
    LaunchedEffect(workoutId) {
        viewModel.loadWorkout(workoutId)
    }

    val deleted by viewModel.deleted.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(deleted) {
        if (deleted) onBack()
    }

    Scaffold(
        containerColor = Bg,
        topBar = {
            TopAppBar(
                title = {
                    Text("Workout", color = TextPrimary)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onEdit(workoutId) }) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit",
                            tint = Blue
                        )
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Delete",
                            tint = Error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Bg)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is WorkoutDetailUiState.Loading -> {
                    CircularProgressIndicator(
                        color = Orange,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is WorkoutDetailUiState.Error -> {
                    Text(
                        text = state.message,
                        color = Error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is WorkoutDetailUiState.Success -> {
                    WorkoutDetailContent(workout = state.workout)
                }
            }
        }
    }
    // delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete workout?", color = TextPrimary) },
            text = {
                Text(
                    "This workout will be permanently deleted. This can't be undone.",
                    color = TextMuted
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.deleteWorkout(workoutId)
                }) {
                    Text("Delete", color = Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            },
            containerColor = Surface2
        )
    }
}

@Composable
private fun WorkoutDetailContent(workout: Workout) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Text(
            text = formatDate(workout.date),
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Shooting section
        Text(
            text = "Shooting",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ShootingStat("Twos", workout.drills.shooting.twos, Modifier.weight(1f))
            ShootingStat("Threes", workout.drills.shooting.threes, Modifier.weight(1f))
            ShootingStat("FT", workout.drills.shooting.freeThrows, Modifier.weight(1f))
            ShootingStat("Layups", workout.drills.shooting.layups, Modifier.weight(1f))
            ShootingStat("Dunks", workout.drills.shooting.dunks, Modifier.weight(1f))
        }

        // Handling section
        if (workout.drills.handling.isNotEmpty()) {
            SectionDivider()
            Text(
                text = "Handling",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            workout.drills.handling.forEach { drill ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = drill.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                    Text(
                        text = "${drill.count} ${drill.unit}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                }
            }
        }

        // Games section
        if (workout.games.isNotEmpty()) {
            SectionDivider()
            Text(
                text = "Games",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            workout.games.forEachIndexed { index, game ->
                val teamAWins = game.teamA.score > game.teamB.score
                val teamBWins = game.teamB.score > game.teamA.score
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Text(
                        text = "Game ${index + 1} · ${game.playersPerTeam}v${game.playersPerTeam}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${game.teamA.players} vs ${game.teamB.players}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                        Row {
                            Text(
                                text = "${game.teamA.score}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (teamAWins) Orange else TextPrimary
                            )
                            Text(
                                text = " - ",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = "${game.teamB.score}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (teamBWins) Orange else TextPrimary
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ShootingStat(label: String, value: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Surface2)
            .border(1.dp, Border, RoundedCornerShape(10.dp))
            .padding(vertical = 16.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$value",
            style = MaterialTheme.typography.titleLarge,
            color = Orange
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SectionDivider() {
    Spacer(modifier = Modifier.height(20.dp))
    HorizontalDivider(color = Border, thickness = 1.dp)
    Spacer(modifier = Modifier.height(20.dp))
}

private fun formatDate(dateString: String): String {
    return try {
        val date = LocalDate.parse(dateString)
        date.format(DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH))
    } catch (e: Exception) {
        dateString
    }
}