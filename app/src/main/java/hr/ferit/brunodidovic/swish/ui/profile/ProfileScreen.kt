package hr.ferit.brunodidovic.swish.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hr.ferit.brunodidovic.swish.ui.theme.*
import hr.ferit.brunodidovic.swish.viewmodel.*

@Composable
fun ProfileScreen(
    onLoggedOut: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showLogoutDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg),
        contentAlignment = Alignment.Center
    ) {
        when (val s = state) {
            is ProfileUiState.Loading -> CircularProgressIndicator(color = Orange)
            is ProfileUiState.Error -> Text(
                text = s.message,
                color = Error,
                style = MaterialTheme.typography.bodyMedium
            )
            is ProfileUiState.Success -> ProfileContent(
                state = s,
                onLogout = { showLogoutDialog = true }   // <-- now opens the dialog
            )
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = Surface2,
            title = { Text("Log out", color = TextPrimary) },
            text = { Text("Are you sure you want to log out?", color = TextMuted) },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    authViewModel.logout()
                    onLoggedOut()
                }) {
                    Text("Log out", color = Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }
}

@Composable
private fun ProfileContent(
    state: ProfileUiState.Success,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(24.dp))

        // --- header ---
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(Surface2),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = Orange,
                    modifier = Modifier.size(48.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = state.username,
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Member since ${state.memberSince}",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }

        // --- lifetime totals ---
        Spacer(Modifier.height(32.dp))
        SectionTitle("Lifetime totals")
        Spacer(Modifier.height(12.dp))

        val tiles = listOf(
            "Workouts" to state.stats.workouts,
            "Longest streak" to state.stats.longestStreak,
            "Total shots" to state.stats.totalShots,
            "Threes" to state.stats.threes,
            "Twos" to state.stats.twos,
            "Free throws" to state.stats.freeThrows,
            "Layups" to state.stats.layups,
            "Dunks" to state.stats.dunks,
            "Games played" to state.stats.gamesPlayed
        )
        tiles.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { (label, value) ->
                    StatTile(
                        label = label,
                        value = value,
                        modifier = Modifier.weight(1f)
                    )
                }
                // pad a short final row so tiles keep 1/3 width
                repeat(3 - row.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // --- achievements ---
        Spacer(Modifier.height(20.dp))
        SectionTitle("Achievements")
        Spacer(Modifier.height(12.dp))

        state.achievements.forEach { family ->
            AchievementCard(family)
            Spacer(Modifier.height(12.dp))
        }

        // --- logout ---
        Spacer(Modifier.height(20.dp))
        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Error)
        ) {
            Text("Log out", color = Error)
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
}

@Composable
private fun StatTile(label: String, value: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Surface)
            .border(1.dp, Border, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text(
            text = value.formatThousands(),
            style = MaterialTheme.typography.titleLarge,
            color = Orange,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            minLines = 2,
            maxLines = 2
        )
    }
}

@Composable
private fun AchievementCard(family: AchievementFamily) {
    val nextTier = family.tiers.firstOrNull { !it.unlocked }
    val fraction = if (nextTier == null) 1f
    else (family.current.toFloat() / nextTier.threshold).coerceIn(0f, 1f)

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
            Text(family.name, color = TextPrimary, style = MaterialTheme.typography.titleSmall)
            Text(
                text = family.current.formatThousands(),
                color = Orange,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(10.dp))

        // hand-drawn progress bar (track + fill)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Surface2)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (nextTier == null) Success else Orange)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (nextTier == null) "All milestones reached"
            else "Next: ${nextTier.threshold.formatThousands()}",
            style = MaterialTheme.typography.bodySmall,
            color = TextDim
        )

        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            family.tiers.forEach { tier -> TierChip(tier) }
        }
    }
}

@Composable
private fun TierChip(tier: AchievementTier) {
    val bg = if (tier.unlocked) Orange else Surface2
    val fg = if (tier.unlocked) TextPrimary else TextDim
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = tier.threshold.formatThousands(),
            color = fg,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

// 1240 -> "1,240"
private fun Int.formatThousands(): String = "%,d".format(this)