package hr.ferit.brunodidovic.swish.ui.workouts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hr.ferit.brunodidovic.swish.model.Workout
import hr.ferit.brunodidovic.swish.ui.theme.*
import hr.ferit.brunodidovic.swish.viewmodel.WorkoutsUiState
import hr.ferit.brunodidovic.swish.viewmodel.WorkoutsViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutsScreen(
    onWorkoutClick: (String) -> Unit,
    onCreateWorkout: () -> Unit,
    viewModel: WorkoutsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedYear by viewModel.selectedYear.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Bg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateWorkout,
                containerColor = Orange
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Create workout",
                    tint = TextPrimary
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Bg)
                .padding(innerPadding)
                .padding(24.dp)
        ) {
            Text(
                text = "Workouts",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            when (val state = uiState) {
                is WorkoutsUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Orange)
                    }
                }

                is WorkoutsUiState.Error -> {
                    Text(text = state.message, color = Error)
                }

                is WorkoutsUiState.Success -> {
                    val years = viewModel.availableYears()
                    val monthsForYear = viewModel.availableMonthsForYear(selectedYear)

                    if (years.isNotEmpty()) {
                        YearFilterRow(
                            years = years,
                            selectedYear = selectedYear,
                            onYearSelected = { viewModel.selectYear(it) }
                        )

                        if (selectedYear != null && monthsForYear.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            MonthFilterRow(
                                months = monthsForYear,
                                selectedMonth = selectedMonth,
                                onMonthSelected = { viewModel.selectMonth(it) }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    if (state.workouts.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No workouts found",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.workouts) { workout ->
                                WorkoutListCard(
                                    workout = workout,
                                    onClick = { onWorkoutClick(workout.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun YearFilterRow(
    years: List<String>,
    selectedYear: String?,
    onYearSelected: (String?) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            FilterChip(
                label = "All",
                selected = selectedYear == null,
                onClick = { onYearSelected(null) }
            )
        }
        items(years) { year ->
            FilterChip(
                label = year,
                selected = selectedYear == year,
                onClick = { onYearSelected(year) }
            )
        }
    }
}

@Composable
private fun MonthFilterRow(
    months: List<String>,
    selectedMonth: String?,
    onMonthSelected: (String?) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            FilterChip(
                label = "All months",
                selected = selectedMonth == null,
                onClick = { onMonthSelected(null) }
            )
        }
        items(months) { month ->
            FilterChip(
                label = formatMonthShort(month),
                selected = selectedMonth == month,
                onClick = { onMonthSelected(month) }
            )
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) Orange else Surface)
            .border(
                1.dp,
                if (selected) Orange else Border,
                RoundedCornerShape(50)
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) TextPrimary else TextMuted
        )
    }
}

@Composable
private fun WorkoutListCard(
    workout: Workout,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Surface)
            .border(1.dp, Border, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = formatDate(workout.date),
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            val totalShots = workout.drills.shooting.twos +
                    workout.drills.shooting.threes +
                    workout.drills.shooting.freeThrows +
                    workout.drills.shooting.layups +
                    workout.drills.shooting.dunks
            Text(
                text = "$totalShots shots made · ${workout.games.size} games",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }
        Text(
            text = "›",
            style = MaterialTheme.typography.titleLarge,
            color = TextMuted
        )
    }
}

private fun formatDate(dateString: String): String {
    return try {
        val date = LocalDate.parse(dateString)
        date.format(DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH))
    } catch (e: Exception) {
        dateString
    }
}

private fun formatMonthShort(monthString: String): String {
    return try {
        val ym = YearMonth.parse(monthString)
        ym.format(DateTimeFormatter.ofPattern("MMMM", Locale.ENGLISH))
    } catch (e: Exception) {
        monthString
    }
}