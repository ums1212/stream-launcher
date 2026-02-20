package org.comon.streamlauncher

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import org.comon.streamlauncher.launcher.HomeSideEffect
import org.comon.streamlauncher.launcher.HomeViewModel
import org.comon.streamlauncher.navigation.CrossPagerNavigation
import org.comon.streamlauncher.ui.theme.StreamLauncherTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StreamLauncherTheme {
                val viewModel: HomeViewModel = hiltViewModel()
                viewModel.uiState.collectAsStateWithLifecycle()

                LaunchedEffect(Unit) {
                    viewModel.effect.collect { effect ->
                        when (effect) {
                            is HomeSideEffect.NavigateToApp ->
                                Log.d("MainActivity", "Navigate: ${effect.packageName}")
                            is HomeSideEffect.ShowError ->
                                Log.e("MainActivity", "Error: ${effect.message}")
                        }
                    }
                }

                CrossPagerNavigation {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Text("Home", style = MaterialTheme.typography.headlineSmall)
                    }
                }
            }
        }
    }
}
