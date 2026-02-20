package org.comon.streamlauncher

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import org.comon.streamlauncher.launcher.HomeIntent
import org.comon.streamlauncher.launcher.HomeSideEffect
import org.comon.streamlauncher.launcher.HomeViewModel
import org.comon.streamlauncher.launcher.ui.HomeScreen
import org.comon.streamlauncher.navigation.AppDrawerScreen
import org.comon.streamlauncher.navigation.CrossPagerNavigation
import org.comon.streamlauncher.ui.theme.StreamLauncherTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: HomeViewModel by viewModels()
    private var resetTrigger by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StreamLauncherTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                LaunchedEffect(Unit) {
                    viewModel.effect.collect { effect ->
                        when (effect) {
                            is HomeSideEffect.NavigateToApp -> {
                                val launchIntent = packageManager.getLaunchIntentForPackage(effect.packageName)
                                if (launchIntent != null) {
                                    try {
                                        startActivity(launchIntent)
                                    } catch (e: ActivityNotFoundException) {
                                        Log.w("MainActivity", "앱 실행 실패: ${effect.packageName}", e)
                                    }
                                } else {
                                    Log.w("MainActivity", "앱 실행 실패: ${effect.packageName}")
                                }
                            }
                            is HomeSideEffect.ShowError ->
                                Log.e("MainActivity", "Error: ${effect.message}")
                        }
                    }
                }

                CrossPagerNavigation(
                    resetTrigger = resetTrigger,
                    appDrawerContent = {
                        AppDrawerScreen(
                            state = uiState,
                            onIntent = viewModel::handleIntent,
                        )
                    },
                ) {
                    HomeScreen(state = uiState, onIntent = viewModel::handleIntent)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME) &&
            lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        ) {
            resetTrigger++
            viewModel.handleIntent(HomeIntent.ResetHome)
        }
    }
}
