package me.bmax.apatch.ui.theme

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.MutableLiveData
import me.bmax.apatch.ui.webui.MonetColorsProvider

// Retained for the upstream settings screen's theme refresh hook.
val refreshTheme = MutableLiveData(false)

@Composable
fun APatchTheme(content: @Composable () -> Unit) {
    val activity = LocalContext.current as ComponentActivity
    SideEffect {
        // Keep the paper UI and its system-bar icons readable even when Android
        // uses dark mode. Transparent bars show the same white page background.
        activity.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.Transparent.toArgb(), Color.Transparent.toArgb()),
            navigationBarStyle = SystemBarStyle.light(Color.Transparent.toArgb(), Color.Transparent.toArgb()),
        )
    }
    MaterialTheme(
        colorScheme = EinkLightScheme,
        typography = Typography,
    ) {
        MonetColorsProvider.UpdateCss()
        content()
    }
}
