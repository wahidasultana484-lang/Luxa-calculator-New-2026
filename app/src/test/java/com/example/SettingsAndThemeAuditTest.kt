package com.example

import android.content.Context
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.core.view.WindowCompat
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.calculator.CalculatorViewModel
import com.example.calculator.components.CalculatorFeedbackController
import com.example.calculator.components.KeySoundPlayer
import com.example.calculator.components.LocalKeySoundPlayer
import com.example.calculator.engine.MathEngine
import com.example.calculator.ui.CalculatorScreen
import com.example.history.database.LuxaDatabase
import com.example.history.repository.HistoryRepository
import com.example.settings.datastore.SettingsPreferences
import com.example.ui.theme.LuxaCalculatorTheme
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SettingsAndThemeAuditTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun resetAppPreferencesAndHistory() {
        val app = ApplicationProvider.getApplicationContext<LuxaApplication>()
        runBlocking {
            app.container.settingsPreferences.clearAll()
            app.container.historyRepository.clearHistory()
        }
    }

    @After
    fun cleanupAppPreferencesAndHistory() {
        val app = ApplicationProvider.getApplicationContext<LuxaApplication>()
        runBlocking {
            app.container.settingsPreferences.clearAll()
            app.container.historyRepository.clearHistory()
        }
    }

    @Test
    @Config(qualifiers = "w411dp-h800dp-night")
    fun freshInstall_opensInLightTheme_evenWhenSystemIsDark() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val initialSettings = runBlocking {
            (context as LuxaApplication).container.settingsPreferences.settingsFlow.first()
        }
        assertEquals(ThemeMode.LIGHT, initialSettings.themeMode)

        composeRule.waitForIdle()
        var isLightStatusBars = false
        composeRule.activityRule.scenario.onActivity { activity ->
            val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
            isLightStatusBars = controller.isAppearanceLightStatusBars
        }
        assertTrue("Fresh install should open in Light Theme with light status bars", isLightStatusBars)

        // Open Settings and confirm Theme row displays Light
        composeRule.onNodeWithTag("overflow_menu_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("menu_settings").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Light").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Light").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w411dp-h800dp")
    fun themeSelection_switchesToDark_andRestoresAcrossActivityRestart() {
        composeRule.waitForIdle()

        // Navigate to Settings -> Choose theme -> Dark
        composeRule.onNodeWithTag("overflow_menu_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("menu_settings").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Light").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Light").assertIsDisplayed()

        composeRule.onNodeWithTag("settings_theme_row").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("dialog_option_DARK").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Dark").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Dark").assertIsDisplayed()

        var isLightStatusBarsAfterDark = true
        composeRule.activityRule.scenario.onActivity { activity ->
            val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
            isLightStatusBarsAfterDark = controller.isAppearanceLightStatusBars
        }
        assertFalse("Dark theme should use dark status bar style", isLightStatusBarsAfterDark)

        // Navigate back to Calculator before simulating Activity restart
        composeRule.onNodeWithTag("settings_back_button").performClick()
        composeRule.waitForIdle()

        // Simulate app restart via Activity recreate
        composeRule.activityRule.scenario.recreate()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            var light = true
            composeRule.activityRule.scenario.onActivity { activity ->
                light = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
                    .isAppearanceLightStatusBars
            }
            !light
        }

        // Open Settings and verify Dark is still selected, then switch back to Light and verify restore
        composeRule.onNodeWithTag("overflow_menu_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("menu_settings").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Dark").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Dark").assertIsDisplayed()

        composeRule.onNodeWithTag("settings_theme_row").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("dialog_option_LIGHT").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Light").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Light").assertIsDisplayed()

        composeRule.onNodeWithTag("settings_back_button").performClick()
        composeRule.waitForIdle()

        composeRule.activityRule.scenario.recreate()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            var light = false
            composeRule.activityRule.scenario.onActivity { activity ->
                light = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
                    .isAppearanceLightStatusBars
            }
            light
        }
    }

    @Test
    @Config(qualifiers = "w411dp-h800dp")
    fun allSettingsOptions_applyIntendedBehavior_andPersistAcrossRestart() {
        composeRule.waitForIdle()

        // Save one calculation first so History count and Clear History can be audited end-to-end
        composeRule.onNodeWithTag("btn_1").performClick()
        composeRule.onNodeWithTag("btn_add").performClick()
        composeRule.onNodeWithTag("btn_2").performClick()
        composeRule.onNodeWithTag("btn_equals").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("calculator_result_text", useUnmergedTree = true)
            .assertTextEquals("3")

        // Open Settings
        composeRule.onNodeWithTag("overflow_menu_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("menu_settings").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasTestTag("settings_scientific_default_switch"))
                .fetchSemanticsNodes().isNotEmpty()
        }

        // 1. Scientific mode by default -> ON
        composeRule.onNodeWithTag("settings_scientific_default_switch").assertIsOff().performClick()
        waitUntilSwitchState("settings_scientific_default_switch", ToggleableState.On)

        // 2. Default angle unit -> RAD
        composeRule.onNodeWithTag("settings_angle_row").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("dialog_option_RAD").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Radians (RAD)").fetchSemanticsNodes().isNotEmpty()
        }

        // 3. Number formatting -> Indian
        composeRule.onNodeWithTag("settings_list")
            .performScrollToNode(hasTestTag("settings_number_format_row"))
        composeRule.onNodeWithTag("settings_number_format_row").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("dialog_option_INDIAN").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Indian (12,34,567.89)").fetchSemanticsNodes().isNotEmpty()
        }

        // 4. Keep screen on -> ON (applies FLAG_KEEP_SCREEN_ON immediately)
        composeRule.onNodeWithTag("settings_list")
            .performScrollToNode(hasTestTag("settings_keep_screen_on_switch"))
        composeRule.onNodeWithTag("settings_keep_screen_on_switch").assertIsOff().performClick()
        waitUntilSwitchState("settings_keep_screen_on_switch", ToggleableState.On)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            var flagSet = false
            composeRule.activityRule.scenario.onActivity { activity ->
                flagSet = (activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0
            }
            flagSet
        }

        // 5. Button sound -> ON
        composeRule.onNodeWithTag("settings_list")
            .performScrollToNode(hasTestTag("settings_button_sound_switch"))
        composeRule.onNodeWithTag("settings_button_sound_switch").assertIsOff().performClick()
        waitUntilSwitchState("settings_button_sound_switch", ToggleableState.On)

        // 6. Haptic feedback -> OFF (default is ON)
        composeRule.onNodeWithTag("settings_list")
            .performScrollToNode(hasTestTag("settings_haptic_switch"))
        composeRule.onNodeWithTag("settings_haptic_switch").assertIsOn().performClick()
        waitUntilSwitchState("settings_haptic_switch", ToggleableState.Off)

        // 7. Privacy dialog
        composeRule.onNodeWithTag("settings_list")
            .performScrollToNode(hasTestTag("settings_privacy_row"))
        composeRule.onNodeWithTag("settings_privacy_row").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasTestTag("privacy_dialog_ok_button")).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("privacy_dialog_ok_button").performClick()
        composeRule.waitForIdle()

        // 8. About dialog
        composeRule.onNodeWithTag("settings_list")
            .performScrollToNode(hasTestTag("settings_about_row"))
        composeRule.onNodeWithTag("settings_about_row").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasTestTag("about_dialog_ok_button")).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("about_dialog_ok_button").performClick()
        composeRule.waitForIdle()

        // 9. View calculation history row shows count and navigates to History
        composeRule.onNodeWithTag("settings_list")
            .performScrollToNode(hasTestTag("settings_manage_history_row"))
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("1 saved calculations").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("settings_manage_history_row").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasTestTag("history_list")).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("history_list").assertIsDisplayed()
        composeRule.onNodeWithTag("history_back_button").performClick()
        composeRule.waitForIdle()

        // 10. Clear calculation history row clears Room history and shows confirmation snackbar
        composeRule.onNodeWithTag("settings_list")
            .performScrollToNode(hasTestTag("settings_clear_history_row"))
        composeRule.onNodeWithTag("settings_clear_history_row").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("confirm_clear_history_button").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("0 saved calculations").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Calculation history cleared").assertIsDisplayed()

        // Navigate back to Calculator before simulating Activity restart
        composeRule.onNodeWithTag("settings_back_button").performClick()
        composeRule.waitForIdle()

        // Simulate Activity recreation (ViewModelStore preserved) and verify persisted state & window flags
        composeRule.activityRule.scenario.recreate()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasTestTag("angle_mode_toggle")).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.waitForIdle()

        // Angle mode is RAD
        composeRule.onNodeWithTag("angle_mode_toggle").assertTextEquals("RAD")

        // Keep screen on flag is still applied after recreate
        composeRule.waitUntil(timeoutMillis = 5_000) {
            var flagSet = false
            composeRule.activityRule.scenario.onActivity { activity ->
                flagSet = (activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0
            }
            flagSet
        }

        // Indian formatting applies after recreate: 100000 × 1 = 1,00,000
        composeRule.onNodeWithTag("btn_1").performClick()
        repeat(5) { composeRule.onNodeWithTag("btn_0").performClick() }
        composeRule.onNodeWithTag("btn_multiply").performClick()
        composeRule.onNodeWithTag("btn_1").performClick()
        composeRule.onNodeWithTag("btn_equals").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("calculator_result_text", useUnmergedTree = true)
            .assertTextEquals("1,00,000")

        // Reopen Settings to verify switches & choices remain persisted
        composeRule.onNodeWithTag("overflow_menu_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("menu_settings").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Radians (RAD)").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("settings_scientific_default_switch").assertIsOn()
        composeRule.onNodeWithText("Radians (RAD)").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_list")
            .performScrollToNode(hasTestTag("settings_number_format_row"))
        composeRule.onNodeWithText("Indian (12,34,567.89)").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_list")
            .performScrollToNode(hasTestTag("settings_keep_screen_on_switch"))
        composeRule.onNodeWithTag("settings_keep_screen_on_switch").assertIsOn()
        composeRule.onNodeWithTag("settings_list")
            .performScrollToNode(hasTestTag("settings_button_sound_switch"))
        composeRule.onNodeWithTag("settings_button_sound_switch").assertIsOn()
        composeRule.onNodeWithTag("settings_list")
            .performScrollToNode(hasTestTag("settings_haptic_switch"))
        composeRule.onNodeWithTag("settings_haptic_switch").assertIsOff()
    }

    private fun waitUntilSwitchState(tag: String, expected: ToggleableState) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            val nodes = composeRule.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes()
            nodes.isNotEmpty() && nodes.first().config[SemanticsProperties.ToggleableState] == expected
        }
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ButtonSoundFeedbackTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun buttonSoundController_gatesPlaybackStrictlyBySoundEnabled() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = View(context)
        var clickCount = 0
        val player = KeySoundPlayer { clickCount++ }

        val disabledController = CalculatorFeedbackController(
            view = view,
            keySoundPlayer = player,
            hapticEnabled = false,
            soundEnabled = false
        )
        disabledController.performKeyFeedback()
        disabledController.performKeyFeedback()
        assertEquals(0, clickCount)

        val enabledController = CalculatorFeedbackController(
            view = view,
            keySoundPlayer = player,
            hapticEnabled = false,
            soundEnabled = true
        )
        enabledController.performKeyFeedback()
        enabledController.performKeyFeedback()
        assertEquals(2, clickCount)
    }

    @Test
    fun buttonSoundSetting_takesEffectImmediately_ignoresNavigation_andPersists() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val dsFile = File(context.filesDir, "datastore/test_button_sound_${System.nanoTime()}.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(scope = scope) { dsFile }
        val prefs = SettingsPreferences(dataStore)
        val db = Room.inMemoryDatabaseBuilder(context, LuxaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repo = HistoryRepository(db.calculationHistoryDao())
        val viewModel = CalculatorViewModel(MathEngine(), repo, prefs)

        var soundClicks = 0
        val recordingPlayer = KeySoundPlayer { soundClicks++ }

        try {
            composeRule.setContent {
                LuxaCalculatorTheme {
                    CompositionLocalProvider(LocalKeySoundPlayer provides recordingPlayer) {
                        CalculatorScreen(
                            viewModel = viewModel,
                            onOpenHistory = {},
                            onOpenSettings = {},
                            onClearHistory = {}
                        )
                    }
                }
            }
            composeRule.waitForIdle()

            // Default OFF: calculator buttons and navigation buttons produce 0 clicks
            composeRule.onNodeWithTag("btn_7").performClick()
            composeRule.onNodeWithTag("btn_add").performClick()
            composeRule.onNodeWithTag("btn_3").performClick()
            composeRule.onNodeWithTag("btn_equals").performClick()
            composeRule.onNodeWithTag("history_button").performClick()
            composeRule.waitForIdle()
            assertEquals("Button sound OFF must produce 0 sounds", 0, soundClicks)

            // Toggle Button Sound ON -> takes effect immediately
            runBlocking { prefs.setButtonSoundEnabled(true) }
            composeRule.waitUntil(timeoutMillis = 5_000) {
                org.robolectric.shadows.ShadowLooper.idleMainLooper()
                viewModel.uiState.value.buttonSoundEnabled
            }
            composeRule.waitForIdle()

            composeRule.onNodeWithTag("btn_5").performClick()
            composeRule.onNodeWithTag("btn_equals").performClick()
            composeRule.waitForIdle()
            assertEquals("Button sound ON must immediately play sound for calculator buttons", 2, soundClicks)

            // Unrelated top-bar navigation & overflow menu buttons must NOT play calculator button sounds
            composeRule.onNodeWithTag("history_button").performClick()
            composeRule.onNodeWithTag("overflow_menu_button").performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("menu_about").performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("about_dialog_ok_button").performClick()
            composeRule.waitForIdle()
            assertEquals("Navigation/menu controls must not play button sound", 2, soundClicks)

            // Toggle Button Sound OFF -> disables sound immediately
            runBlocking { prefs.setButtonSoundEnabled(false) }
            composeRule.waitUntil(timeoutMillis = 5_000) {
                org.robolectric.shadows.ShadowLooper.idleMainLooper()
                !viewModel.uiState.value.buttonSoundEnabled
            }
            composeRule.waitForIdle()

            composeRule.onNodeWithTag("btn_9").performClick()
            composeRule.waitForIdle()
            assertEquals("Toggling Button sound OFF must immediately silence clicks", 2, soundClicks)

            // Verify persistence in DataStore for restart
            runBlocking { prefs.setButtonSoundEnabled(true) }
            val persisted = runBlocking { prefs.settingsFlow.first() }
            assertTrue(persisted.buttonSoundEnabled)
        } finally {
            db.close()
            scope.cancel()
        }
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ColdAppRestartTest {

    @get:Rule
    val composeRule = androidx.compose.ui.test.junit4.createEmptyComposeRule()

    @Test
    @Config(qualifiers = "w411dp-h800dp")
    fun coldAppRestart_restoresSelectedDarkThemeScientificDefaultAndAllPreferences() {
        val app = ApplicationProvider.getApplicationContext<LuxaApplication>()
        runBlocking {
            app.container.settingsPreferences.clearAll()
            app.container.historyRepository.clearHistory()
        }

        try {
            // Persist settings as a user would, then launch a fresh MainActivity (cold start)
            runBlocking {
                val prefs = app.container.settingsPreferences
                prefs.setThemeMode(ThemeMode.DARK)
                prefs.setScientificExpandedByDefault(true)
                prefs.setDefaultAngleMode(com.example.calculator.engine.AngleMode.RAD)
                prefs.setNumberFormatStyle(com.example.calculator.engine.NumberFormatStyle.INDIAN)
                prefs.setKeepScreenOn(true)
                prefs.setButtonSoundEnabled(true)
                prefs.setHapticFeedbackEnabled(false)
            }

            androidx.test.core.app.ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                composeRule.waitUntil(timeoutMillis = 5_000) {
                    composeRule.onAllNodes(hasTestTag("btn_sin")).fetchSemanticsNodes().isNotEmpty()
                }
                composeRule.waitForIdle()

                // 1. Dark theme restored
                var isLightStatusBars = true
                var keepScreenOnFlag = false
                scenario.onActivity { activity ->
                    isLightStatusBars = WindowCompat.getInsetsController(
                        activity.window,
                        activity.window.decorView
                    ).isAppearanceLightStatusBars
                    keepScreenOnFlag = (activity.window.attributes.flags and
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0
                }
                assertFalse("Cold restart must restore saved Dark Theme", isLightStatusBars)
                assertTrue("Cold restart must restore Keep screen on flag", keepScreenOnFlag)

                // 2. Scientific mode by default opens on cold launch & angle mode is RAD
                composeRule.onNodeWithTag("btn_sin").assertIsDisplayed()
                composeRule.onNodeWithTag("angle_mode_toggle").assertTextEquals("RAD")

                // 3. Indian formatting applies on cold launch
                composeRule.onNodeWithTag("btn_1").performClick()
                repeat(5) { composeRule.onNodeWithTag("btn_0").performClick() }
                composeRule.onNodeWithTag("btn_multiply").performClick()
                composeRule.onNodeWithTag("btn_1").performClick()
                composeRule.onNodeWithTag("btn_equals").performClick()
                composeRule.waitForIdle()
                composeRule.onNodeWithTag("calculator_result_text", useUnmergedTree = true)
                    .assertTextEquals("1,00,000")
            }
        } finally {
            runBlocking {
                app.container.settingsPreferences.clearAll()
                app.container.historyRepository.clearHistory()
            }
        }
    }
}
