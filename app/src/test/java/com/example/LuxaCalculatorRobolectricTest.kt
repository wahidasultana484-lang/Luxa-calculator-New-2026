package com.example

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.calculator.engine.AngleMode
import com.example.calculator.engine.NumberFormatStyle
import com.example.history.database.LuxaDatabase
import com.example.history.repository.HistoryRepository
import com.example.settings.datastore.SettingsPreferences
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LuxaCalculatorRobolectricTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun basicCalculation_backspace_ac_equals_and_memory() {
        // 2 + 3 × 4 = 14
        composeRule.onNodeWithTag("btn_2").performClick()
        composeRule.onNodeWithTag("btn_add").performClick()
        composeRule.onNodeWithTag("btn_3").performClick()
        composeRule.onNodeWithTag("btn_multiply").performClick()
        composeRule.onNodeWithTag("btn_4").performClick()
        composeRule.onNodeWithTag("btn_equals").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("calculator_result_text", useUnmergedTree = true)
            .assertTextEquals("14")

        // Memory M+, AC, MR -> 14
        composeRule.onNodeWithTag("btn_m_plus").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("memory_indicator").assertIsDisplayed()

        composeRule.onNodeWithTag("btn_ac").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("calculator_expression_text", useUnmergedTree = true)
            .assertTextEquals("0")

        composeRule.onNodeWithTag("btn_mr").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("calculator_expression_text", useUnmergedTree = true)
            .assertTextEquals("14")

        // Backspace removes '4', leaving '1'
        composeRule.onNodeWithTag("btn_backspace").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("calculator_expression_text", useUnmergedTree = true)
            .assertTextEquals("1")

        // M- and MC
        composeRule.onNodeWithTag("btn_m_minus").performClick()
        composeRule.onNodeWithTag("btn_mc").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("memory_indicator").assertDoesNotExist()
    }

    @Test
    fun scientificMode_expandsCollapses_angleModeAndFunctionsWork() {
        // Initially collapsed
        composeRule.onNodeWithTag("btn_sin").assertDoesNotExist()

        // Expand scientific keypad
        composeRule.onNodeWithTag("toggle_scientific_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("btn_sin").assertIsDisplayed()

        // Compute sin(30) in DEG mode -> 0.5
        composeRule.onNodeWithTag("btn_sin").performClick()
        composeRule.onNodeWithTag("btn_3").performClick()
        composeRule.onNodeWithTag("btn_0").performClick()
        composeRule.onNodeWithTag("btn_close_paren").performClick()
        composeRule.onNodeWithTag("btn_equals").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("calculator_result_text", useUnmergedTree = true)
            .assertTextEquals("0.5")

        // Toggle DEG -> RAD
        composeRule.onNodeWithTag("angle_mode_toggle").assertTextEquals("DEG")
        composeRule.onNodeWithTag("angle_mode_toggle").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("angle_mode_toggle").assertTextEquals("RAD")

        // Collapse scientific keypad
        composeRule.onNodeWithTag("toggle_scientific_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("btn_sin").assertDoesNotExist()
    }

    @Test
    fun historyFlow_savesReusesDeletesAndClears() {
        // Perform 25² = 625
        composeRule.onNodeWithTag("toggle_scientific_button").performClick()
        composeRule.onNodeWithTag("btn_2").performClick()
        composeRule.onNodeWithTag("btn_5").performClick()
        composeRule.onNodeWithTag("btn_square").performClick()
        composeRule.onNodeWithTag("btn_equals").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("calculator_result_text", useUnmergedTree = true)
            .assertTextEquals("625")

        // Clear display, then open History screen and tap entry to reuse it
        composeRule.onNodeWithTag("btn_ac").performClick()
        composeRule.onNodeWithTag("history_button").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("25²").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("history_list").assertIsDisplayed()
        composeRule.onNodeWithText("25²").assertIsDisplayed()
        composeRule.onNodeWithText("= 625").assertIsDisplayed()

        // Tap entry to reuse expression in calculator
        composeRule.onNodeWithText("25²").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("calculator_expression_text", useUnmergedTree = true)
            .assertTextEquals("25²")

        // Reopen History and clear all
        composeRule.onNodeWithTag("history_button").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("25²").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("history_clear_all_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("confirm_clear_history_button").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasTestTag("history_empty_state")).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("history_empty_state").assertIsDisplayed()
    }

    @Test
    fun settingsNavigation_displaysAllSections() {
        composeRule.onNodeWithTag("overflow_menu_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("menu_settings").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("settings_theme_row").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_scientific_default_switch").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_angle_row").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_list")
            .performScrollToNode(hasTestTag("settings_number_format_row"))
        composeRule.onNodeWithTag("settings_number_format_row").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_list")
            .performScrollToNode(hasTestTag("settings_about_row"))
        composeRule.onNodeWithTag("settings_about_row").assertIsDisplayed()
    }

    @Test
    fun roomHistoryDatabase_persistsAcrossDatabaseReopenAndSupportsDelete() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val dbName = "test_luxa_persist.db"
            context.deleteDatabase(dbName)

            val db1 = Room.databaseBuilder(context, LuxaDatabase::class.java, dbName)
                .allowMainThreadQueries()
                .build()
            val repo1 = HistoryRepository(db1.calculationHistoryDao())
            val id1 = repo1.saveCalculation("2+3", "5", "5", timestamp = 1000L)
            val id2 = repo1.saveCalculation("10×10", "100", "100", timestamp = 2000L)
            db1.close()

            // Reopen database simulating app restart
            val db2 = Room.databaseBuilder(context, LuxaDatabase::class.java, dbName)
                .allowMainThreadQueries()
                .build()
            val repo2 = HistoryRepository(db2.calculationHistoryDao())
            val loaded = repo2.allHistory.first()
            assertEquals(2, loaded.size)
            assertEquals("10×10", loaded[0].expression)
            assertEquals("2+3", loaded[1].expression)

            // Delete individual entry
            repo2.deleteEntry(id1)
            val afterDelete = repo2.allHistory.first()
            assertEquals(1, afterDelete.size)
            assertEquals(id2, afterDelete[0].id)

            // Clear all
            repo2.clearHistory()
            assertTrue(repo2.allHistory.first().isEmpty())
            db2.close()
            context.deleteDatabase(dbName)
        }
    }

    @Test
    fun settingsPreferences_persistChanges() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val prefs = SettingsPreferences(context)
            try {
                prefs.clearAll()
                prefs.setThemeMode(ThemeMode.DARK)
                prefs.setScientificExpandedByDefault(true)
                prefs.setDefaultAngleMode(AngleMode.RAD)
                prefs.setNumberFormatStyle(NumberFormatStyle.INDIAN)
                prefs.setKeepScreenOn(true)
                prefs.setButtonSoundEnabled(true)
                prefs.setHapticFeedbackEnabled(false)

                val stored = prefs.settingsFlow.first()
                assertEquals(ThemeMode.DARK, stored.themeMode)
                assertTrue(stored.scientificExpandedByDefault)
                assertEquals(AngleMode.RAD, stored.defaultAngleMode)
                assertEquals(NumberFormatStyle.INDIAN, stored.numberFormatStyle)
                assertTrue(stored.keepScreenOn)
                assertTrue(stored.buttonSoundEnabled)
                assertFalse(stored.hapticFeedbackEnabled)
            } finally {
                prefs.clearAll()
            }
        }
    }

    @Test
    @Config(qualifiers = "w411dp-h915dp")
    fun displayPanel_remainsFixedDuringVerticalSwipes_andPreservesControls() {
        // Enter 12 + 34 so both expression ("12+34") and live result ("46") have visible bounds
        composeRule.onNodeWithTag("btn_1").performClick()
        composeRule.onNodeWithTag("btn_2").performClick()
        composeRule.onNodeWithTag("btn_add").performClick()
        composeRule.onNodeWithTag("btn_3").performClick()
        composeRule.onNodeWithTag("btn_4").performClick()
        composeRule.waitForIdle()

        val panelNode = composeRule.onNodeWithTag("calculator_display_panel")
        val exprNode = composeRule.onNodeWithTag("calculator_expression_text", useUnmergedTree = true)
        val resultNode = composeRule.onNodeWithTag("calculator_result_text", useUnmergedTree = true)

        exprNode.assertTextEquals("12+34")
        resultNode.assertTextEquals("46")

        val initialPanelBounds = panelNode.fetchSemanticsNode().boundsInRoot
        val initialExprBounds = exprNode.fetchSemanticsNode().boundsInRoot
        val initialResultBounds = resultNode.fetchSemanticsNode().boundsInRoot
        assertTrue("Initial display panel bottom should be > 0", initialPanelBounds.bottom > 0f)

        // Vertical drag upward held mid-gesture must not move the display panel or its contents
        panelNode.performTouchInput {
            down(Offset(centerX, centerY))
            moveBy(Offset(0f, -120f))
        }
        composeRule.waitForIdle()
        assertEquals(initialPanelBounds.top, panelNode.fetchSemanticsNode().boundsInRoot.top, 0.5f)
        assertEquals(initialPanelBounds.bottom, panelNode.fetchSemanticsNode().boundsInRoot.bottom, 0.5f)
        assertEquals(initialExprBounds.top, exprNode.fetchSemanticsNode().boundsInRoot.top, 0.5f)
        assertEquals(initialResultBounds.top, resultNode.fetchSemanticsNode().boundsInRoot.top, 0.5f)

        panelNode.performTouchInput { up() }
        composeRule.waitForIdle()
        assertEquals(initialPanelBounds.bottom, panelNode.fetchSemanticsNode().boundsInRoot.bottom, 0.5f)

        // Swipe up and swipe down gestures leave the display completely fixed
        panelNode.performTouchInput {
            swipeUp(startY = bottom - 16f, endY = top + 40f, durationMillis = 150)
        }
        composeRule.waitForIdle()
        assertEquals(initialPanelBounds.bottom, panelNode.fetchSemanticsNode().boundsInRoot.bottom, 0.5f)
        assertEquals(initialExprBounds.top, exprNode.fetchSemanticsNode().boundsInRoot.top, 0.5f)

        panelNode.performTouchInput {
            swipeDown(startY = top + 40f, endY = bottom - 16f, durationMillis = 150)
        }
        composeRule.waitForIdle()
        assertEquals(initialPanelBounds.bottom, panelNode.fetchSemanticsNode().boundsInRoot.bottom, 0.5f)
        assertEquals(initialResultBounds.top, resultNode.fetchSemanticsNode().boundsInRoot.top, 0.5f)

        // Memory bar (MC, MR, M+, M-) and side arrow remain visible and functional
        composeRule.onNodeWithTag("btn_mc").assertIsDisplayed()
        composeRule.onNodeWithTag("btn_mr").assertIsDisplayed()
        composeRule.onNodeWithTag("btn_m_plus").assertIsDisplayed()
        composeRule.onNodeWithTag("btn_m_minus").assertIsDisplayed()
        composeRule.onNodeWithTag("toggle_scientific_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("btn_sin").assertIsDisplayed()
        composeRule.onNodeWithTag("toggle_scientific_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("btn_sin").assertDoesNotExist()

        // Calculator buttons and calculation continue working normally
        composeRule.onNodeWithTag("btn_ac").performClick()
        composeRule.onNodeWithTag("btn_7").performClick()
        composeRule.onNodeWithTag("btn_add").performClick()
        composeRule.onNodeWithTag("btn_3").performClick()
        composeRule.onNodeWithTag("btn_equals").performClick()
        composeRule.waitForIdle()
        resultNode.assertTextEquals("10")
    }

    @Test
    @Config(qualifiers = "w411dp-h800dp")
    fun allBasicKeypadOperations_andEdgeCases_workReliably() {
        val exprNode = composeRule.onNodeWithTag("calculator_expression_text", useUnmergedTree = true)
        val resultNode = composeRule.onNodeWithTag("calculator_result_text", useUnmergedTree = true)

        // 2 + 2 = 4
        composeRule.onNodeWithTag("btn_2").performClick()
        composeRule.onNodeWithTag("btn_add").performClick()
        composeRule.onNodeWithTag("btn_2").performClick()
        composeRule.onNodeWithTag("btn_equals").performClick()
        composeRule.waitForIdle()
        resultNode.assertTextEquals("4")

        // Repeated continuous calculation: + 3 = 7, then × 8 = 56
        composeRule.onNodeWithTag("btn_add").performClick()
        composeRule.onNodeWithTag("btn_3").performClick()
        composeRule.onNodeWithTag("btn_equals").performClick()
        composeRule.waitForIdle()
        resultNode.assertTextEquals("7")

        composeRule.onNodeWithTag("btn_multiply").performClick()
        composeRule.onNodeWithTag("btn_8").performClick()
        composeRule.onNodeWithTag("btn_equals").performClick()
        composeRule.waitForIdle()
        resultNode.assertTextEquals("56")

        // 25 ÷ 5 = 5
        composeRule.onNodeWithTag("btn_2").performClick()
        composeRule.onNodeWithTag("btn_5").performClick()
        composeRule.onNodeWithTag("btn_divide").performClick()
        composeRule.onNodeWithTag("btn_5").performClick()
        composeRule.onNodeWithTag("btn_equals").performClick()
        composeRule.waitForIdle()
        resultNode.assertTextEquals("5")

        // 100 − 35 = 65
        composeRule.onNodeWithTag("btn_1").performClick()
        composeRule.onNodeWithTag("btn_0").performClick()
        composeRule.onNodeWithTag("btn_0").performClick()
        composeRule.onNodeWithTag("btn_subtract").performClick()
        composeRule.onNodeWithTag("btn_3").performClick()
        composeRule.onNodeWithTag("btn_5").performClick()
        composeRule.onNodeWithTag("btn_equals").performClick()
        composeRule.waitForIdle()
        resultNode.assertTextEquals("65")

        // 12.5 + 2.5 = 15 (and extra decimal ignored)
        composeRule.onNodeWithTag("btn_1").performClick()
        composeRule.onNodeWithTag("btn_2").performClick()
        composeRule.onNodeWithTag("btn_dot").performClick()
        composeRule.onNodeWithTag("btn_5").performClick()
        composeRule.onNodeWithTag("btn_dot").performClick()
        composeRule.onNodeWithTag("btn_add").performClick()
        composeRule.onNodeWithTag("btn_2").performClick()
        composeRule.onNodeWithTag("btn_dot").performClick()
        composeRule.onNodeWithTag("btn_5").performClick()
        composeRule.onNodeWithTag("btn_equals").performClick()
        composeRule.waitForIdle()
        resultNode.assertTextEquals("15")

        // Operator replacement: 7 + × 8 = 56
        composeRule.onNodeWithTag("btn_7").performClick()
        composeRule.onNodeWithTag("btn_add").performClick()
        composeRule.onNodeWithTag("btn_multiply").performClick()
        composeRule.onNodeWithTag("btn_8").performClick()
        composeRule.onNodeWithTag("btn_equals").performClick()
        composeRule.waitForIdle()
        resultNode.assertTextEquals("56")

        // Parentheses and Percentage: (20 + 30) × 50% = 25
        composeRule.onNodeWithTag("btn_parens").performClick()
        composeRule.onNodeWithTag("btn_2").performClick()
        composeRule.onNodeWithTag("btn_0").performClick()
        composeRule.onNodeWithTag("btn_add").performClick()
        composeRule.onNodeWithTag("btn_3").performClick()
        composeRule.onNodeWithTag("btn_0").performClick()
        composeRule.onNodeWithTag("btn_parens").performClick()
        composeRule.onNodeWithTag("btn_multiply").performClick()
        composeRule.onNodeWithTag("btn_5").performClick()
        composeRule.onNodeWithTag("btn_0").performClick()
        composeRule.onNodeWithTag("btn_percent").performClick()
        composeRule.onNodeWithTag("btn_equals").performClick()
        composeRule.waitForIdle()
        resultNode.assertTextEquals("25")

        // Rapid consecutive digit taps 1..9..0
        composeRule.onNodeWithTag("btn_1").performClick()
        composeRule.onNodeWithTag("btn_2").performClick()
        composeRule.onNodeWithTag("btn_3").performClick()
        composeRule.onNodeWithTag("btn_4").performClick()
        composeRule.onNodeWithTag("btn_5").performClick()
        composeRule.onNodeWithTag("btn_6").performClick()
        composeRule.onNodeWithTag("btn_7").performClick()
        composeRule.onNodeWithTag("btn_8").performClick()
        composeRule.onNodeWithTag("btn_9").performClick()
        composeRule.onNodeWithTag("btn_0").performClick()
        composeRule.waitForIdle()
        exprNode.assertTextEquals("1234567890")

        // Invalid expression: 2 + = -> Format error, then AC recovers cleanly
        composeRule.onNodeWithTag("btn_ac").performClick()
        composeRule.onNodeWithTag("btn_2").performClick()
        composeRule.onNodeWithTag("btn_add").performClick()
        composeRule.onNodeWithTag("btn_equals").performClick()
        composeRule.waitForIdle()
        resultNode.assertTextEquals("Format error")

        // Division by zero -> Can't divide by 0
        composeRule.onNodeWithTag("btn_ac").performClick()
        composeRule.onNodeWithTag("btn_9").performClick()
        composeRule.onNodeWithTag("btn_divide").performClick()
        composeRule.onNodeWithTag("btn_0").performClick()
        composeRule.onNodeWithTag("btn_equals").performClick()
        composeRule.waitForIdle()
        resultNode.assertTextEquals("Can't divide by 0")
    }

    @Test
    @Config(qualifiers = "w411dp-h800dp")
    fun scientificKeypad_allFunctionsWork_andLayoutHasNoOverlap() {
        val exprNode = composeRule.onNodeWithTag("calculator_expression_text", useUnmergedTree = true)
        val resultNode = composeRule.onNodeWithTag("calculator_result_text", useUnmergedTree = true)

        composeRule.onNodeWithTag("toggle_scientific_button").performClick()
        composeRule.waitForIdle()

        // Ensure top action bar, expression, and result do not clip/overlap in scientific mode
        val angleBottom = composeRule.onNodeWithTag("angle_mode_toggle").fetchSemanticsNode().boundsInRoot.bottom
        val exprTop = exprNode.fetchSemanticsNode().boundsInRoot.top
        val panelBottom = composeRule.onNodeWithTag("calculator_display_panel").fetchSemanticsNode().boundsInRoot.bottom
        val mcTop = composeRule.onNodeWithTag("btn_mc").fetchSemanticsNode().boundsInRoot.top
        assertTrue("Expression must stay below top angle toggle ($exprTop >= $angleBottom)", exprTop >= angleBottom)
        assertTrue("Memory bar must stay below display panel ($mcTop >= $panelBottom)", mcTop >= panelBottom)

        // 2 × 10^(3) = 2,000 (tests 10^x digit separation fix)
        composeRule.onNodeWithTag("btn_2").performClick()
        composeRule.onNodeWithTag("btn_ten_pow").performClick()
        composeRule.onNodeWithTag("btn_3").performClick()
        composeRule.onNodeWithTag("btn_close_paren").performClick()
        composeRule.onNodeWithTag("btn_equals").performClick()
        composeRule.waitForIdle()
        exprNode.assertTextEquals("2×10^(3)")
        resultNode.assertTextEquals("2,000")

        // √(144) = 12
        composeRule.onNodeWithTag("btn_ac").performClick()
        composeRule.onNodeWithTag("btn_sqrt").performClick()
        composeRule.onNodeWithTag("btn_1").performClick()
        composeRule.onNodeWithTag("btn_4").performClick()
        composeRule.onNodeWithTag("btn_4").performClick()
        composeRule.onNodeWithTag("btn_close_paren").performClick()
        composeRule.onNodeWithTag("btn_equals").performClick()
        composeRule.waitForIdle()
        resultNode.assertTextEquals("12")

        // log(100) + ln(e) = 3
        composeRule.onNodeWithTag("btn_ac").performClick()
        composeRule.onNodeWithTag("btn_log").performClick()
        composeRule.onNodeWithTag("btn_1").performClick()
        composeRule.onNodeWithTag("btn_0").performClick()
        composeRule.onNodeWithTag("btn_0").performClick()
        composeRule.onNodeWithTag("btn_close_paren").performClick()
        composeRule.onNodeWithTag("btn_add").performClick()
        composeRule.onNodeWithTag("btn_ln").performClick()
        composeRule.onNodeWithTag("btn_e").performClick()
        composeRule.onNodeWithTag("btn_close_paren").performClick()
        composeRule.onNodeWithTag("btn_equals").performClick()
        composeRule.waitForIdle()
        resultNode.assertTextEquals("3")

        // 5! = 120 and cos(60) = 0.5
        composeRule.onNodeWithTag("btn_ac").performClick()
        composeRule.onNodeWithTag("btn_5").performClick()
        composeRule.onNodeWithTag("btn_factorial").performClick()
        composeRule.onNodeWithTag("btn_equals").performClick()
        composeRule.waitForIdle()
        resultNode.assertTextEquals("120")

        composeRule.onNodeWithTag("btn_ac").performClick()
        composeRule.onNodeWithTag("btn_cos").performClick()
        composeRule.onNodeWithTag("btn_6").performClick()
        composeRule.onNodeWithTag("btn_0").performClick()
        composeRule.onNodeWithTag("btn_close_paren").performClick()
        composeRule.onNodeWithTag("btn_equals").performClick()
        composeRule.waitForIdle()
        resultNode.assertTextEquals("0.5")

        // Collapse scientific keypad and verify main keypad still responds
        composeRule.onNodeWithTag("toggle_scientific_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("btn_sin").assertDoesNotExist()
        composeRule.onNodeWithTag("btn_9").performClick()
        composeRule.onNodeWithTag("btn_add").performClick()
        composeRule.onNodeWithTag("btn_1").performClick()
        composeRule.onNodeWithTag("btn_equals").performClick()
        composeRule.waitForIdle()
        resultNode.assertTextEquals("10")
    }

    @Test
    @Config(qualifiers = "w411dp-h800dp")
    fun overflowMenu_allItems_andNavigationBack_leaveKeypadResponsive() {
        val resultNode = composeRule.onNodeWithTag("calculator_result_text", useUnmergedTree = true)

        // Create one saved calculation first: 6 × 7 = 42
        composeRule.onNodeWithTag("btn_6").performClick()
        composeRule.onNodeWithTag("btn_multiply").performClick()
        composeRule.onNodeWithTag("btn_7").performClick()
        composeRule.onNodeWithTag("btn_equals").performClick()
        composeRule.waitForIdle()
        resultNode.assertTextEquals("42")

        // 1. Overflow -> About -> OK -> keypad works immediately
        composeRule.onNodeWithTag("overflow_menu_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("menu_about").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("about_dialog_ok_button").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("btn_3").performClick()
        composeRule.onNodeWithTag("btn_add").performClick()
        composeRule.onNodeWithTag("btn_4").performClick()
        composeRule.onNodeWithTag("btn_equals").performClick()
        composeRule.waitForIdle()
        resultNode.assertTextEquals("7")

        // 1b. Open overflow menu and dismiss via device back without selecting an item -> no invisible overlay blocks keypad
        composeRule.onNodeWithTag("overflow_menu_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("menu_about").assertIsDisplayed()
        composeRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("btn_2").performClick()
        composeRule.onNodeWithTag("btn_add").performClick()
        composeRule.onNodeWithTag("btn_2").performClick()
        composeRule.onNodeWithTag("btn_equals").performClick()
        composeRule.waitForIdle()
        resultNode.assertTextEquals("4")

        // 2. Overflow -> History -> Back -> keypad works immediately
        composeRule.onNodeWithTag("overflow_menu_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("menu_history").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("6×7").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("history_back_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("btn_8").performClick()
        composeRule.onNodeWithTag("btn_add").performClick()
        composeRule.onNodeWithTag("btn_1").performClick()
        composeRule.onNodeWithTag("btn_equals").performClick()
        composeRule.waitForIdle()
        resultNode.assertTextEquals("9")

        // 3. Overflow -> Settings -> change Number Format to Indian -> Back -> keypad works
        composeRule.onNodeWithTag("overflow_menu_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("menu_settings").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_list")
            .performScrollToNode(hasTestTag("settings_number_format_row"))
        composeRule.onNodeWithTag("settings_number_format_row").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("dialog_option_INDIAN").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_back_button").performClick()
        composeRule.waitForIdle()

        // Verify Indian formatting applies and keypad works: 100000 × 1 = 1,00,000
        composeRule.onNodeWithTag("btn_1").performClick()
        repeat(5) { composeRule.onNodeWithTag("btn_0").performClick() }
        composeRule.onNodeWithTag("btn_multiply").performClick()
        composeRule.onNodeWithTag("btn_1").performClick()
        composeRule.onNodeWithTag("btn_equals").performClick()
        composeRule.waitForIdle()
        resultNode.assertTextEquals("1,00,000")

        // 4. Overflow -> Clear history -> Confirm -> History is empty & keypad still works
        composeRule.onNodeWithTag("overflow_menu_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("menu_clear_history").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("confirm_clear_history_button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("history_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("history_empty_state").assertIsDisplayed()
        composeRule.onNodeWithTag("history_back_button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("btn_5").performClick()
        composeRule.onNodeWithTag("btn_add").performClick()
        composeRule.onNodeWithTag("btn_5").performClick()
        composeRule.onNodeWithTag("btn_equals").performClick()
        composeRule.waitForIdle()
        resultNode.assertTextEquals("10")
    }

    @Test
    @Config(qualifiers = "w411dp-h800dp")
    fun navigationTransitions_slideHorizontally_andRapidRepeatedNavigationStaysStable() {
        val calcDisplay = composeRule.onNodeWithTag("calculator_display_panel")
        val resultNode = composeRule.onNodeWithTag("calculator_result_text", useUnmergedTree = true)

        // 1. Calculator -> History: inspect mid-transition horizontal slide positions
        composeRule.mainClock.autoAdvance = false
        composeRule.onNodeWithTag("overflow_menu_button").performClick()
        composeRule.mainClock.advanceTimeBy(100L)
        composeRule.onNodeWithTag("menu_history").performClick()
        composeRule.mainClock.advanceTimeBy(120L)

        val historyMidLeft = composeRule.onNodeWithTag("history_back_button")
            .fetchSemanticsNode().boundsInRoot.left
        val calcExitMidLeft = calcDisplay.fetchSemanticsNode().boundsInRoot.left
        assertTrue("History should slide in from right ($historyMidLeft > 0)", historyMidLeft > 0f)
        assertTrue("Calculator should shift slightly left ($calcExitMidLeft < 0)", calcExitMidLeft < 0f)

        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("history_back_button").assertIsDisplayed()
        assertTrue(composeRule.onNodeWithTag("history_back_button").fetchSemanticsNode().boundsInRoot.left < 24f)
        calcDisplay.assertDoesNotExist()

        // 2. History -> Back to Calculator: inspect mid-transition pop positions
        composeRule.mainClock.autoAdvance = false
        composeRule.onNodeWithTag("history_back_button").performClick()
        composeRule.mainClock.advanceTimeBy(120L)

        val historyPopRight = composeRule.onNodeWithTag("history_back_button")
            .fetchSemanticsNode().boundsInRoot.left
        val calcPopLeft = calcDisplay.fetchSemanticsNode().boundsInRoot.left
        assertTrue("History should slide out toward right ($historyPopRight > 24)", historyPopRight > 24f)
        assertTrue("Calculator should slide in from left ($calcPopLeft < 0)", calcPopLeft < 0f)

        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()
        assertEquals(0f, calcDisplay.fetchSemanticsNode().boundsInRoot.left, 1f)
        composeRule.onNodeWithTag("history_back_button").assertDoesNotExist()

        // 3. Calculator -> Settings: inspect mid-transition horizontal slide positions
        composeRule.mainClock.autoAdvance = false
        composeRule.onNodeWithTag("overflow_menu_button").performClick()
        composeRule.mainClock.advanceTimeBy(100L)
        composeRule.onNodeWithTag("menu_settings").performClick()
        composeRule.mainClock.advanceTimeBy(120L)

        val settingsMidLeft = composeRule.onNodeWithTag("settings_list")
            .fetchSemanticsNode().boundsInRoot.left
        val calcSettingsExitLeft = calcDisplay.fetchSemanticsNode().boundsInRoot.left
        assertTrue("Settings should slide in from right ($settingsMidLeft > 0)", settingsMidLeft > 0f)
        assertTrue("Calculator should shift slightly left ($calcSettingsExitLeft < 0)", calcSettingsExitLeft < 0f)

        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_list").assertIsDisplayed()
        calcDisplay.assertDoesNotExist()

        // 4. Settings -> Back to Calculator: inspect mid-transition pop positions
        composeRule.mainClock.autoAdvance = false
        composeRule.onNodeWithTag("settings_back_button").performClick()
        composeRule.mainClock.advanceTimeBy(120L)

        val settingsPopLeft = composeRule.onNodeWithTag("settings_list")
            .fetchSemanticsNode().boundsInRoot.left
        val calcPopFromSettingsLeft = calcDisplay.fetchSemanticsNode().boundsInRoot.left
        assertTrue("Settings should slide out toward right ($settingsPopLeft > 0)", settingsPopLeft > 0f)
        assertTrue("Calculator should slide in from left ($calcPopFromSettingsLeft < 0)", calcPopFromSettingsLeft < 0f)

        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()
        assertEquals(0f, calcDisplay.fetchSemanticsNode().boundsInRoot.left, 1f)
        composeRule.onNodeWithTag("settings_list").assertDoesNotExist()

        // 5. Repeated multi-hop navigation: Calculator -> Settings -> History -> Back -> Back -> Calculator
        repeat(2) {
            composeRule.onNodeWithTag("overflow_menu_button").performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("menu_settings").performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("settings_list")
                .performScrollToNode(hasTestTag("settings_manage_history_row"))
            composeRule.onNodeWithTag("settings_manage_history_row").performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("history_back_button").assertIsDisplayed()
            composeRule.onNodeWithTag("history_back_button").performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("settings_back_button").performClick()
            composeRule.waitForIdle()
            calcDisplay.assertIsDisplayed()
        }

        // 6. Rapid double-tap on History & Back mid-transition does not crash or pop Calculator
        composeRule.mainClock.autoAdvance = false
        composeRule.onNodeWithTag("history_button").performClick()
        composeRule.mainClock.advanceTimeBy(16L)
        composeRule.onNodeWithTag("history_button").performClick()
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("history_back_button").assertIsDisplayed()

        composeRule.mainClock.autoAdvance = false
        composeRule.onNodeWithTag("history_back_button").performClick()
        composeRule.mainClock.advanceTimeBy(16L)
        composeRule.onNodeWithTag("history_back_button").performClick()
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()
        calcDisplay.assertIsDisplayed()
        composeRule.onNodeWithTag("history_back_button").assertDoesNotExist()

        // 7. Calculator buttons stay fully responsive after all transitions
        composeRule.onNodeWithTag("btn_4").performClick()
        composeRule.onNodeWithTag("btn_multiply").performClick()
        composeRule.onNodeWithTag("btn_5").performClick()
        composeRule.onNodeWithTag("btn_equals").performClick()
        composeRule.waitForIdle()
        resultNode.assertTextEquals("20")
    }
}
