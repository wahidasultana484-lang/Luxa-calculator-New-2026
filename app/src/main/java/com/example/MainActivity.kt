package com.example

import android.content.res.Configuration
import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.navigation.LuxaNavHost
import com.example.settings.datastore.UserSettings
import com.example.ui.theme.LuxaCalculatorTheme
import com.example.ui.theme.ThemeMode

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val container = (application as LuxaApplication).container
        val initialSettings = container.settingsPreferences.currentSettingsOrNull() ?: UserSettings()
        val initialSystemDark =
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
        val initialIsDark = when (initialSettings.themeMode) {
            ThemeMode.SYSTEM -> initialSystemDark
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }
        applyEdgeToEdge(initialIsDark)

        super.onCreate(savedInstanceState)

        setContent {
            val rememberedInitial = remember { initialSettings }
            val settings by container.settingsPreferences.settingsFlow
                .collectAsStateWithLifecycle(initialValue = rememberedInitial)

            val isDarkTheme = when (settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            val appliedDarkRef = remember { booleanArrayOf(initialIsDark) }
            DisposableEffect(isDarkTheme) {
                if (appliedDarkRef[0] != isDarkTheme) {
                    appliedDarkRef[0] = isDarkTheme
                    applyEdgeToEdge(isDarkTheme)
                }
                onDispose {}
            }

            DisposableEffect(settings.keepScreenOn) {
                if (settings.keepScreenOn) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
                onDispose {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            LuxaCalculatorTheme(themeMode = settings.themeMode) {
                LuxaNavHost(container = container)
            }
        }
    }

    private fun applyEdgeToEdge(isDarkTheme: Boolean) {
        val barStyle = if (isDarkTheme) {
            SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        } else {
            SystemBarStyle.light(
                AndroidColor.TRANSPARENT,
                AndroidColor.TRANSPARENT
            )
        }
        enableEdgeToEdge(
            statusBarStyle = barStyle,
            navigationBarStyle = barStyle
        )
    }
}
