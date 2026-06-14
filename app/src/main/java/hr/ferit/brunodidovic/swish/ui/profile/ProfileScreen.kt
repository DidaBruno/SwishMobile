package hr.ferit.brunodidovic.swish.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import hr.ferit.brunodidovic.swish.ui.theme.Bg
import hr.ferit.brunodidovic.swish.ui.theme.TextPrimary

@Composable
fun ProfileScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Profile coming soon",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary
        )
    }
}