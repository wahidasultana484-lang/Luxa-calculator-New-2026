package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.example.calculator.CalculatorViewModel
import com.example.calculator.engine.AngleMode
import com.example.calculator.engine.EvalResult
import com.example.calculator.engine.MathEngine
import com.example.history.database.LuxaDatabase
import com.example.history.repository.HistoryRepository
import com.example.settings.datastore.SettingsPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class StartupPerformanceAuditTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    @Config(qualifiers = "w411dp-h800dp")
    fun coldAndWarmStartup_renderImmediately_withoutMainThreadRoomInitialization() {
        val app = ApplicationProvider.getApplicationContext<LuxaApplication>()
        val container = AppContainer(app)
        assertFalse(
            "Room database must remain uninitialized right after AppContainer construction",
            container.isHistoryDatabaseInitialized
        )

        val coldStartNs = System.nanoTime()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            composeRule.waitUntil(timeoutMillis = 5_000) {
                composeRule.onAllNodes(hasTestTag("btn_equals")).fetchSemanticsNodes().isNotEmpty()
            }
            composeRule.waitForIdle()
            val coldMs = (System.nanoTime() - coldStartNs) / 1_000_000.0

            // Room should still NOT be initialized on the main thread just from launching CalculatorScreen
            assertFalse(
                "Room database must NOT initialize on cold startup before History/Settings/Save",
                app.container.isHistoryDatabaseInitialized
            )

            composeRule.onNodeWithTag("calculator_display_panel").assertIsDisplayed()
            composeRule.onNodeWithTag("btn_equals").assertIsDisplayed()

            // Verify instant interactive calculation on first launch
            composeRule.onNodeWithTag("btn_7").performClick()
            composeRule.onNodeWithTag("btn_multiply").performClick()
            composeRule.onNodeWithTag("btn_6").performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("calculator_result_text", useUnmergedTree = true)
                .assertTextEquals("42")

            // Measure warm restart via Activity recreate
            val warmStartNs = System.nanoTime()
            scenario.recreate()
            composeRule.waitUntil(timeoutMillis = 5_000) {
                composeRule.onAllNodes(hasTestTag("btn_equals")).fetchSemanticsNodes().isNotEmpty()
            }
            composeRule.waitForIdle()
            val warmMs = (System.nanoTime() - warmStartNs) / 1_000_000.0

            println("BENCHMARK_ROBOLECTRIC: cold_startup_ms=$coldMs warm_recreate_ms=$warmMs")
            assertTrue("Cold startup took unexpectedly long: $coldMs ms", coldMs < 10_000.0)
            assertTrue("Warm recreate took unexpectedly long: $warmMs ms", warmMs < 10_000.0)
        }
    }

    @Test
    fun calculatorViewModel_chainedDivisionAndMemoryRecall_evaluateAccurately() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val dsFile = File(context.filesDir, "datastore/test_vm_math_${System.nanoTime()}.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(scope = scope) { dsFile }
        val prefs = SettingsPreferences(dataStore)
        val db = Room.inMemoryDatabaseBuilder(context, LuxaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repo = HistoryRepository(db.calculationHistoryDao())

        try {
            val vmStore = ViewModelStore()
            val vm = ViewModelProvider(
                vmStore,
                object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return CalculatorViewModel(MathEngine(), repo, prefs) as T
                    }
                }
            )[CalculatorViewModel::class.java]

            // Chained calculation: 1 ÷ 3 = then × 3 = -> "1"
            vm.onDigit("1")
            vm.onOperator("÷")
            vm.onDigit("3")
            vm.onEquals()
            vm.onOperator("×")
            vm.onDigit("3")
            vm.onEquals()
            assertEquals("1", vm.uiState.value.resultText)

            // Memory recall after digit inserts implicit multiply: store 5, enter 2, press MR -> "2×5" = "10"
            vm.onClear()
            vm.onDigit("5")
            vm.onMemoryAdd()
            vm.onClear()
            vm.onDigit("2")
            vm.onMemoryRecall()
            assertEquals("2×5", vm.uiState.value.expression)
            vm.onEquals()
            assertEquals("10", vm.uiState.value.resultText)

            // Negative memory recall after digit: store -5, enter 2, MR -> "2×(-5)" = "-10"
            vm.onMemoryClear()
            vm.onClear()
            vm.onDigit("5")
            vm.onMemorySubtract()
            vm.onClear()
            vm.onDigit("2")
            vm.onMemoryRecall()
            assertEquals("2×(-5)", vm.uiState.value.expression)
            vm.onEquals()
            assertEquals("-10", vm.uiState.value.resultText)

            // Ensure background Room saves finish before closing in-memory database
            runBlocking {
                withTimeout(5_000) {
                    repo.allHistory.first { it.size >= 4 }
                }
            }
            vmStore.clear()
        } finally {
            runBlocking { prefs.clearAll() }
            db.close()
            scope.cancel()
        }
    }

    @Test
    fun mathEngine_benchmarkThroughput_1000MixedExpressions() {
        val engine = MathEngine()
        val expressions = listOf(
            "2 + 3 × 4",
            "(20 + 30) × 50%",
            "sin(30) + cos(60) + tan(45)",
            "sin(1000 × π)",
            "log(1000) + ln(e) + √144",
            "2^3^2 - 5! + abs(-42.5)",
            "1 ÷ 3 × 3 + 0.1 + 0.2",
            "e^(ln(5)) + √(1E308)",
            "log(1E-308) + sin(1E40)",
            "170! ÷ 169!"
        )

        // Warm up JIT
        repeat(100) { idx ->
            engine.evaluate(expressions[idx % expressions.size], angleMode = AngleMode.DEG)
        }

        val startNs = System.nanoTime()
        repeat(1000) { idx ->
            val res = engine.evaluate(expressions[idx % expressions.size], angleMode = AngleMode.DEG)
            assertTrue(res is EvalResult.Success)
        }
        val elapsedMs = (System.nanoTime() - startNs) / 1_000_000.0
        val avgUs = (elapsedMs * 1000.0) / 1000.0
        println("BENCHMARK_MATH_ENGINE: 1000_exprs_ms=$elapsedMs avg_us_per_expr=$avgUs")
        assertTrue("1,000 expressions took too long: $elapsedMs ms", elapsedMs < 2_000.0)
    }
}
