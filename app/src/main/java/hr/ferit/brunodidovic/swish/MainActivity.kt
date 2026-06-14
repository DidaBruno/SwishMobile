package hr.ferit.brunodidovic.swish

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import hr.ferit.brunodidovic.swish.repository.AuthRepository
import hr.ferit.brunodidovic.swish.ui.navigation.NavGraph
import hr.ferit.brunodidovic.swish.ui.navigation.Routes
import hr.ferit.brunodidovic.swish.ui.theme.SwishTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SwishTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val startDestination = if (authRepository.currentUser != null) {
                        Routes.MAIN
                    } else {
                        Routes.LOGIN
                    }
                    NavGraph(startDestination = startDestination)
                }
            }
        }
    }
}