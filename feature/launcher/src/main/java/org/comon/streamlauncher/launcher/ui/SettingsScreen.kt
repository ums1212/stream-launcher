package org.comon.streamlauncher.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import org.comon.streamlauncher.launcher.HomeIntent
import org.comon.streamlauncher.launcher.HomeState
import org.comon.streamlauncher.launcher.model.SettingsTab
import org.comon.streamlauncher.ui.theme.StreamLauncherTheme

@Composable
fun SettingsScreen(
    state: HomeState,
    onIntent: (HomeIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = state.currentSettingsTab != SettingsTab.MAIN) {
        onIntent(HomeIntent.ChangeSettingsTab(SettingsTab.MAIN))
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxSize(),
    ) {
        when (state.currentSettingsTab) {
            SettingsTab.MAIN -> MainSettingsContent(onIntent = onIntent)
            SettingsTab.COLOR -> ColorSettingsContent()
            SettingsTab.IMAGE -> ImageSettingsContent()
        }
    }
}

@Composable
private fun MainSettingsContent(onIntent: (HomeIntent) -> Unit) {
    val haptic = LocalHapticFeedback.current
    val accentPrimary = StreamLauncherTheme.colors.accentPrimary
    val accentSecondary = StreamLauncherTheme.colors.accentSecondary

    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 24.dp),
    ) {
        SettingsButton(
            label = "테마 컬러",
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onIntent(HomeIntent.ChangeSettingsTab(SettingsTab.COLOR))
            },
            containerColor = accentPrimary,
        )
        Spacer(modifier = Modifier.width(16.dp))
        SettingsButton(
            label = "홈 이미지",
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onIntent(HomeIntent.ChangeSettingsTab(SettingsTab.IMAGE))
            },
            containerColor = accentSecondary,
        )
    }
}

@Composable
private fun SettingsButton(
    label: String,
    onClick: () -> Unit,
    containerColor: Color,
) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "buttonScale_$label",
    )

    Button(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor),
        modifier = Modifier
            .defaultMinSize(minHeight = 48.dp)
            .scale(scale),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun ColorSettingsContent() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "테마 컬러 설정",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "준비 중입니다",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun ImageSettingsContent() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "홈 이미지 설정",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "준비 중입니다",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
