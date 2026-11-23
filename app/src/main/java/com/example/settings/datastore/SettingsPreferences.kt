package com.example.settings.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.calculator.engine.AngleMode
import com.example.calculator.engine.NumberFormatStyle
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

private val Context.luxaSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "luxa_calculator_settings"
)

data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val scientificExpandedByDefault: Boolean = false,
    val defaultAngleMode: AngleMode = AngleMode.DEG,
    val numberFormatStyle: NumberFormatStyle = NumberFormatStyle.INTERNATIONAL,
    val keepScreenOn: Boolean = false,
    val buttonSoundEnabled: Boolean = false,
    val hapticFeedbackEnabled: Boolean = true,
)

class SettingsPreferences(private val dataStore: DataStore<Preferences>) {

    constructor(context: Context) : this(context.applicationContext.luxaSettingsDataStore)

    @Volatile
    private var cachedSettings: UserSettings? = null

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val SCIENTIFIC_DEFAULT = booleanPreferencesKey("scientific_default")
        val ANGLE_MODE = stringPreferencesKey("angle_mode")
        val NUMBER_FORMAT = stringPreferencesKey("number_format")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val BUTTON_SOUND = booleanPreferencesKey("button_sound")
        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
    }

    val settingsFlow: Flow<UserSettings> = dataStore.data.map { prefs ->
        UserSettings(
            themeMode = prefs[Keys.THEME_MODE]?.let { name ->
                ThemeMode.entries.firstOrNull { it.name == name }
            } ?: ThemeMode.LIGHT,
            scientificExpandedByDefault = prefs[Keys.SCIENTIFIC_DEFAULT] ?: false,
            defaultAngleMode = prefs[Keys.ANGLE_MODE]?.let { name ->
                AngleMode.entries.firstOrNull { it.name == name }
            } ?: AngleMode.DEG,
            numberFormatStyle = prefs[Keys.NUMBER_FORMAT]?.let { name ->
                NumberFormatStyle.entries.firstOrNull { it.name == name }
            } ?: NumberFormatStyle.INTERNATIONAL,
            keepScreenOn = prefs[Keys.KEEP_SCREEN_ON] ?: false,
            buttonSoundEnabled = prefs[Keys.BUTTON_SOUND] ?: false,
            hapticFeedbackEnabled = prefs[Keys.HAPTIC_FEEDBACK] ?: true,
        )
    }.onEach { settings ->
        cachedSettings = settings
    }

    fun currentSettingsOrNull(): UserSettings? = cachedSettings

    fun warmUp(scope: CoroutineScope) {
        settingsFlow.launchIn(scope)
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setScientificExpandedByDefault(enabled: Boolean) {
        dataStore.edit { it[Keys.SCIENTIFIC_DEFAULT] = enabled }
    }

    suspend fun setDefaultAngleMode(mode: AngleMode) {
        dataStore.edit { it[Keys.ANGLE_MODE] = mode.name }
    }

    suspend fun setNumberFormatStyle(style: NumberFormatStyle) {
        dataStore.edit { it[Keys.NUMBER_FORMAT] = style.name }
    }

    suspend fun setKeepScreenOn(enabled: Boolean) {
        dataStore.edit { it[Keys.KEEP_SCREEN_ON] = enabled }
    }

    suspend fun setButtonSoundEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.BUTTON_SOUND] = enabled }
    }

    suspend fun setHapticFeedbackEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.HAPTIC_FEEDBACK] = enabled }
    }

    suspend fun clearAll() {
        dataStore.edit { it.clear() }
        cachedSettings = UserSettings()
    }
}
