package hr.ferit.brunodidovic.swish.ui.workouts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hr.ferit.brunodidovic.swish.viewmodel.*
import androidx.compose.foundation.clickable
import hr.ferit.brunodidovic.swish.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateWorkoutScreen(
    workoutId: String? = null,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: CreateWorkoutViewModel = hiltViewModel()
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()
    val templates by viewModel.templates.collectAsStateWithLifecycle()

    LaunchedEffect(saveState) {
        if (saveState is SaveState.Saved) onSaved()
    }
    LaunchedEffect(workoutId) {
        if (workoutId != null) {
            viewModel.loadWorkoutForEditing(workoutId)
        }
    }

    Scaffold(
        containerColor = Bg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (workoutId != null) "Edit Workout" else "New Workout",
                        color = TextPrimary
                    )
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Bg)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            // --- Shooting ---
            SectionTitle("Shooting")
            Spacer(Modifier.height(12.dp))
            NumberField("2-pointers", form.twos, viewModel::updateTwos)
            NumberField("3-pointers", form.threes, viewModel::updateThrees)
            NumberField("Free throws", form.freeThrows, viewModel::updateFreeThrows)
            NumberField("Layups", form.layups, viewModel::updateLayups)
            NumberField("Dunks", form.dunks, viewModel::updateDunks)

            // --- Handling ---
            Spacer(Modifier.height(24.dp))
            SectionTitle("Handling Drills")
            Spacer(Modifier.height(12.dp))

            form.handling.forEachIndexed { index, drill ->
                HandlingDrillRow(
                    drill = drill,
                    onChange = { viewModel.updateHandlingDrill(index, it) },
                    onRemove = { viewModel.removeHandlingDrill(index) },
                    onSaveTemplate = { viewModel.saveDrillAsTemplate(drill.name, drill.unit) }
                )
                Spacer(Modifier.height(12.dp))
            }

            OutlinedButton(
                onClick = { viewModel.addHandlingDrill() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Border)
            ) {
                Text("+ Add handling drill", color = Blue)
            }

            // template quick-add chips
            if (templates.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Saved templates",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
                Spacer(Modifier.height(8.dp))
                FlowRowTemplates(
                    templates = templates.map { it.name },
                    onClick = { name ->
                        templates.find { it.name == name }?.let {
                            viewModel.addHandlingDrillFromTemplate(it)
                        }
                    }
                )
            }

            // --- Games ---
            Spacer(Modifier.height(24.dp))
            SectionTitle("Games")
            Spacer(Modifier.height(12.dp))

            form.games.forEachIndexed { index, game ->
                GameRow(
                    game = game,
                    index = index,
                    onChange = { viewModel.updateGame(index, it) },
                    onRemove = { viewModel.removeGame(index) }
                )
                Spacer(Modifier.height(12.dp))
            }

            OutlinedButton(
                onClick = { viewModel.addGame() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Border)
            ) {
                Text("+ Add game", color = Blue)
            }

            // --- Save ---
            Spacer(Modifier.height(24.dp))

            if (saveState is SaveState.Error) {
                Text(
                    text = (saveState as SaveState.Error).message,
                    color = Error,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(8.dp))
            }

            Button(
                onClick = { viewModel.saveWorkout() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = saveState !is SaveState.Saving,
                colors = ButtonDefaults.buttonColors(containerColor = Orange),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (saveState is SaveState.Saving) {
                    CircularProgressIndicator(color = TextPrimary, modifier = Modifier.size(20.dp))
                } else {
                    Text("Save Workout", color = TextPrimary)
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
}

@Composable
private fun NumberField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, color = TextMuted) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        colors = fieldColors(),
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
private fun HandlingDrillRow(
    drill: HandlingDrillInput,
    onChange: (HandlingDrillInput) -> Unit,
    onRemove: () -> Unit,
    onSaveTemplate: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Surface)
            .border(1.dp, Border, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = drill.name,
                onValueChange = { onChange(drill.copy(name = it)) },
                label = { Text("Drill name", color = TextMuted) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                colors = fieldColors(),
                shape = RoundedCornerShape(12.dp)
            )
            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.Close, contentDescription = "Remove", tint = Error)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row {
            OutlinedTextField(
                value = drill.count,
                onValueChange = { onChange(drill.copy(count = it.filter { c -> c.isDigit() })) },
                label = { Text("Count", color = TextMuted) },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = fieldColors(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.width(8.dp))
            UnitDropdown(
                selected = drill.unit,
                onSelected = { onChange(drill.copy(unit = it)) },
                modifier = Modifier.weight(1f)
            )
        }
        TextButton(onClick = onSaveTemplate) {
            Text("Save as template", color = Blue, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun GameRow(
    game: GameInput,
    index: Int,
    onChange: (GameInput) -> Unit,
    onRemove: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Surface)
            .border(1.dp, Border, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Game ${index + 1}", color = TextPrimary, style = MaterialTheme.typography.titleSmall)
            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.Close, contentDescription = "Remove", tint = Error)
            }
        }
        OutlinedTextField(
            value = game.playersPerTeam,
            onValueChange = { onChange(game.copy(playersPerTeam = it.filter { c -> c.isDigit() })) },
            label = { Text("Players per team", color = TextMuted) },
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            colors = fieldColors(),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text("Team A", color = Orange, style = MaterialTheme.typography.bodySmall)
        Row {
            OutlinedTextField(
                value = game.teamAPlayers,
                onValueChange = { onChange(game.copy(teamAPlayers = it)) },
                label = { Text("Players", color = TextMuted) },
                modifier = Modifier.weight(2f),
                singleLine = true,
                colors = fieldColors(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = game.teamAScore,
                onValueChange = { onChange(game.copy(teamAScore = it.filter { c -> c.isDigit() })) },
                label = { Text("Score", color = TextMuted) },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = fieldColors(),
                shape = RoundedCornerShape(12.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text("Team B", color = Orange, style = MaterialTheme.typography.bodySmall)
        Row {
            OutlinedTextField(
                value = game.teamBPlayers,
                onValueChange = { onChange(game.copy(teamBPlayers = it)) },
                label = { Text("Players", color = TextMuted) },
                modifier = Modifier.weight(2f),
                singleLine = true,
                colors = fieldColors(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = game.teamBScore,
                onValueChange = { onChange(game.copy(teamBScore = it.filter { c -> c.isDigit() })) },
                label = { Text("Score", color = TextMuted) },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = fieldColors(),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowTemplates(templates: List<String>, onClick: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        templates.forEach { name ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Surface)
                    .border(1.dp, Border, RoundedCornerShape(50))
                    .clickable { onClick(name) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text("+ $name", color = Blue, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Orange,
    unfocusedBorderColor = Border,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    cursorColor = Orange,
    focusedContainerColor = Surface,
    unfocusedContainerColor = Surface
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitDropdown(
    selected: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val units = listOf("reps", "seconds", "minutes")
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text("Unit", color = TextMuted) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            singleLine = true,
            colors = fieldColors(),
            shape = RoundedCornerShape(12.dp)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            units.forEach { unit ->
                DropdownMenuItem(
                    text = { Text(unit, color = TextPrimary) },
                    onClick = {
                        onSelected(unit)
                        expanded = false
                    }
                )
            }
        }
    }
}