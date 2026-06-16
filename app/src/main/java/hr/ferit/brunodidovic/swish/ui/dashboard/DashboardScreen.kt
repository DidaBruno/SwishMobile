package hr.ferit.brunodidovic.swish.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import hr.ferit.brunodidovic.swish.viewmodel.DashboardUiState
import hr.ferit.brunodidovic.swish.viewmodel.DashboardViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import hr.ferit.brunodidovic.swish.domain.ShootingSuggestion
import hr.ferit.brunodidovic.swish.domain.WorkoutSuggester
import hr.ferit.brunodidovic.swish.util.ShakeDetector

@Composable
fun DashboardScreen(
    onCreateWorkout: () -> Unit,
    onSuggestionContinue: (ShootingSuggestion) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showSuggestion by remember { mutableStateOf(false) }
    var suggestion by remember { mutableStateOf<ShootingSuggestion?>(null) }

    // sensor is live ONLY while the dialog is open, a real shake is the only way to roll
    ShakeDetector(enabled = showSuggestion) {
        suggestion = WorkoutSuggester.random()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
    ) {
        when (val state = uiState) {
            is DashboardUiState.Loading -> {
                CircularProgressIndicator(
                    color = Orange,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            is DashboardUiState.Error -> {
                Text(
                    text = state.message,
                    color = Error,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            is DashboardUiState.Success -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp)
                ) {
                    Spacer(modifier = Modifier.height(32.dp))
                    GreetingHeader(
                        username = state.username,
                        streak = state.streak
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (state.todayWorkout != null) {
                        WorkoutSummaryCard(
                            title = "Today's Workout",
                            workout = state.todayWorkout
                        )
                    } else {
                        NoWorkoutCard(onCreateWorkout = onCreateWorkout)

                        if (state.lastWorkout != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            WorkoutSummaryCard(
                                title = "Last Workout",
                                workout = state.lastWorkout
                            )
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = {
                suggestion = null //force a shake to populate
                showSuggestion = true
            },
            containerColor = Orange,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Casino,
                contentDescription = "Random workout",
                tint = TextPrimary
            )
        }
    }
    if (showSuggestion) {
        SuggestionDialog(
            suggestion = suggestion,
            onContinue = { s ->
                onSuggestionContinue(s)
                showSuggestion = false
                suggestion = null
            },
            onDismiss = {
                showSuggestion = false
                suggestion = null
            }
        )
    }
}

@Composable
private fun GreetingHeader(username: String, streak: Int) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "${greeting()}, $username",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary
        )

        if (streak > 0) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Surface)
                    .border(1.dp, Border, RoundedCornerShape(50))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$streak",
                    style = MaterialTheme.typography.titleSmall,
                    color = Orange
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${if (streak == 1) "workout" else "workouts"} in a row",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            }
        }
    }
}

private fun greeting(): String {
    val hour = java.time.LocalTime.now().hour
    return when (hour) {
        in 5..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        else -> "Good evening"
    }
}

@Composable
private fun NoWorkoutCard(onCreateWorkout: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Surface)
            .border(1.dp, Border, RoundedCornerShape(12.dp))
            .padding(20.dp)
    ) {
        Text(
            text = "No workout today yet",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Time to get back on the court.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(
            onClick = onCreateWorkout,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Surface2,
                contentColor = Blue
            ),
            border = BorderStroke(1.dp, Blue),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Create Today's Workout", color = Blue)
        }
    }
}

@Composable
private fun WorkoutSummaryCard(title: String, workout: Workout) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Surface)
            .border(1.dp, Border, RoundedCornerShape(12.dp))
            .padding(20.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = formatDate(workout.date),
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted
        )

        SectionDivider()
        Text(
            text = "Shooting",
            style = MaterialTheme.typography.titleSmall,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            ShootingStat("Twos", workout.drills.shooting.twos, Modifier.weight(1f))
            ShootingStat("Threes", workout.drills.shooting.threes, Modifier.weight(1f))
            ShootingStat("FT", workout.drills.shooting.freeThrows, Modifier.weight(1f))
            ShootingStat("Layups", workout.drills.shooting.layups, Modifier.weight(1f))
            ShootingStat("Dunks", workout.drills.shooting.dunks, Modifier.weight(1f))
        }

        if (workout.drills.handling.isNotEmpty()) {
            SectionDivider()
            Text(
                text = "Handling",
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            workout.drills.handling.forEach { drill ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
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

        if (workout.games.isNotEmpty()) {
            SectionDivider()
            Text(
                text = "Games",
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            workout.games.forEach { game ->
                val teamAWins = game.teamA.score > game.teamB.score
                val teamBWins = game.teamB.score > game.teamA.score
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${game.teamA.players} vs ${game.teamB.players}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
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
}

@Composable
private fun ShootingStat(label: String, value: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "$value",
            style = MaterialTheme.typography.titleLarge,
            color = Orange
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
        val date = java.time.LocalDate.parse(dateString)
        val formatter = java.time.format.DateTimeFormatter
            .ofPattern("MMMM d, yyyy", java.util.Locale.ENGLISH)
        date.format(formatter)
    } catch (e: Exception) {
        dateString
    }
}

@Composable
private fun SuggestionDialog(
    suggestion: ShootingSuggestion?,
    onContinue: (ShootingSuggestion) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface2,
        title = { Text("Suggested shootaround", color = TextPrimary) },
        text = {
            if (suggestion == null) {
                Text(
                    "Shake your phone to generate a workout.",
                    color = TextMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Column {
                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        SuggestionStat("Twos", suggestion.twos, Modifier.weight(1f))
                        SuggestionStat("Threes", suggestion.threes, Modifier.weight(1f))
                        SuggestionStat("FT", suggestion.freeThrows, Modifier.weight(1f))
                        SuggestionStat("Layups", suggestion.layups, Modifier.weight(1f))
                        SuggestionStat("Dunks", suggestion.dunks, Modifier.weight(1f))
                    }
                    SectionDivider()
                    Text(
                        text = "${suggestion.total} shots total",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Shake again for new numbers.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextDim
                    )
                }
            }
        },
        confirmButton = {
            if (suggestion != null) {
                TextButton(onClick = { onContinue(suggestion) }) {
                    Text("CONTINUE", color = Orange)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted)
            }
        }
    )
}
@Composable
private fun SuggestionStat(label: String, value: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$value",
            style = MaterialTheme.typography.titleLarge,
            color = Orange
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            textAlign = TextAlign.Center
        )
    }
}