package org.comon.streamlauncher

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import org.comon.streamlauncher.launcher.HomeSideEffect
import org.comon.streamlauncher.launcher.HomeState
import org.comon.streamlauncher.launcher.HomeViewModel
import org.comon.streamlauncher.ui.theme.StreamLauncherTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StreamLauncherTheme {
                val viewModel: HomeViewModel = hiltViewModel()
                val state by viewModel.uiState.collectAsStateWithLifecycle()

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

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    TempAppList(state = state, modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun TempAppList(state: HomeState, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier.fillMaxSize().padding(16.dp)) {
        if (state.isLoading) {
            item { Text("Loading...") }
        }
        state.appsInCells.forEach { (cell, apps) ->
            item {
                Text(
                    text = "── ${cell.name} (${apps.size}개) ──",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
            items(apps) { app ->
                Text(
                    text = "${app.label} (${app.packageName})",
                    modifier = Modifier.padding(vertical = 2.dp),
                )
            }
        }
    }
}
