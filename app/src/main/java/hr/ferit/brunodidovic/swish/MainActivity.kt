package hr.ferit.brunodidovic.swish

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import hr.ferit.brunodidovic.swish.ui.theme.Orange
import hr.ferit.brunodidovic.swish.ui.theme.SwishTheme
import hr.ferit.brunodidovic.swish.ui.theme.TextPrimary

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SwishTheme {
                setContent {
                    SwishTheme {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = buildAnnotatedString {
                                        withStyle(style = SpanStyle(color = TextPrimary)) { append("SW") }
                                        withStyle(style = SpanStyle(color = Orange)) { append("I") }
                                        withStyle(style = SpanStyle(color = TextPrimary)) { append("SH") }
                                    },
                                    style = MaterialTheme.typography.titleLarge,
                                    fontSize = 48.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}