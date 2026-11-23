package com.example.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.calculator.engine.AngleMode
import com.example.calculator.engine.NumberFormatStyle
import com.example.history.repository.HistoryRepository
import com.example.settings.datastore.SettingsPreferences
import com.example.settings.datastore.UserSettings
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: UserSettings = UserSettings(),
    val historyCount: Int = 0
)

class SettingsViewModel(
    private val settingsPreferences: SettingsPreferences,
    private val historyRepository: HistoryRepository
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsPreferences.settingsFlow,
        historyRepository.historyCount
    ) { settings, count ->
        SettingsUiState(settings = settings, historyCount = count)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsPreferences.setThemeMode(mode) }
    }

    fun setScientificExpandedByDefault(enabled: Boolean) {
        viewModelScope.launch { settingsPreferences.setScientificExpandedByDefault(enabled) }
    }

    fun setDefaultAngleMode(mode: AngleMode) {
        viewModelScope.launch { settingsPreferences.setDefaultAngleMode(mode) }
    }

    fun setNumberFormatStyle(style: NumberFormatStyle) {
        viewModelScope.launch { settingsPreferences.setNumberFormatStyle(style) }
    }

    fun setKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch { settingsPreferences.setKeepScreenOn(enabled) }
    }

    fun setButtonSoundEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsPreferences.setButtonSoundEnabled(enabled) }
    }

    fun setHapticFeedbackEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsPreferences.setHapticFeedbackEnabled(enabled) }
    }

    fun clearHistory() {
        viewModelScope.launch { historyRepository.clearHistory() }
    }

    class Factory(
        private val settingsPreferences: SettingsPreferences,
        private val historyRepository: HistoryRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(settingsPreferences, historyRepository) as T
        }
    }
}
