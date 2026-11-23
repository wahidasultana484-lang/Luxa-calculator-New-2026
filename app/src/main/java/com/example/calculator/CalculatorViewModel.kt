package com.example.calculator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.calculator.engine.AngleMode
import com.example.calculator.engine.EvalResult
import com.example.calculator.engine.MathEngine
import com.example.calculator.engine.MathError
import com.example.calculator.engine.NumberFormatStyle
import com.example.calculator.engine.NumberFormatter
import com.example.history.repository.HistoryRepository
import com.example.settings.datastore.SettingsPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal

class CalculatorViewModel(
    private val mathEngine: MathEngine,
    private val lazyHistoryRepository: Lazy<HistoryRepository>,
    private val settingsPreferences: SettingsPreferences
) : ViewModel() {

    constructor(
        mathEngine: MathEngine,
        historyRepository: HistoryRepository,
        settingsPreferences: SettingsPreferences
    ) : this(mathEngine, lazyOf(historyRepository), settingsPreferences)

    private val initialSettings = settingsPreferences.currentSettingsOrNull()

    private val _uiState = MutableStateFlow(
        if (initialSettings != null) {
            CalculatorUiState(
                isScientificExpanded = initialSettings.scientificExpandedByDefault,
                angleMode = initialSettings.defaultAngleMode,
                numberFormatStyle = initialSettings.numberFormatStyle,
                hapticFeedbackEnabled = initialSettings.hapticFeedbackEnabled,
                buttonSoundEnabled = initialSettings.buttonSoundEnabled
            )
        } else {
            CalculatorUiState()
        }
    )
    val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

    private var appliedInitialDefaults = initialSettings != null
    private var lastSavedDefaultAngleMode: AngleMode? = initialSettings?.defaultAngleMode
    private var lastSavedScientificDefault: Boolean? = initialSettings?.scientificExpandedByDefault

    init {
        viewModelScope.launch {
            settingsPreferences.settingsFlow.collect { settings ->
                _uiState.update { current ->
                    val scientificChangedInSettings = lastSavedScientificDefault != null &&
                        lastSavedScientificDefault != settings.scientificExpandedByDefault
                    val shouldApplyScientific = !appliedInitialDefaults || scientificChangedInSettings
                    val angleChangedInSettings = lastSavedDefaultAngleMode != null &&
                        lastSavedDefaultAngleMode != settings.defaultAngleMode
                    val nextAngleMode = if (!appliedInitialDefaults || angleChangedInSettings) {
                        settings.defaultAngleMode
                    } else {
                        current.angleMode
                    }
                    appliedInitialDefaults = true
                    lastSavedDefaultAngleMode = settings.defaultAngleMode
                    lastSavedScientificDefault = settings.scientificExpandedByDefault

                    val updated = current.copy(
                        isScientificExpanded = if (shouldApplyScientific) {
                            settings.scientificExpandedByDefault
                        } else {
                            current.isScientificExpanded
                        },
                        angleMode = nextAngleMode,
                        numberFormatStyle = settings.numberFormatStyle,
                        hapticFeedbackEnabled = settings.hapticFeedbackEnabled,
                        buttonSoundEnabled = settings.buttonSoundEnabled
                    )
                    if (updated == current) {
                        current
                    } else if (
                        current.expression.isNotEmpty() &&
                        (updated.angleMode != current.angleMode ||
                            updated.numberFormatStyle != current.numberFormatStyle)
                    ) {
                        recomputeDisplay(updated)
                    } else {
                        updated
                    }
                }
            }
        }
    }

    fun onDigit(digit: String) {
        _uiState.update { state ->
            val baseExpr = if (state.justEvaluated) "" else state.expression
            // Prevent multiple leading zeros like 00
            val nextExpr = if (baseExpr == "0" && digit != ".") {
                digit
            } else if (baseExpr.endsWith("0") && isStandaloneLeadingZero(baseExpr)) {
                baseExpr.dropLast(1) + digit
            } else {
                baseExpr + digit
            }
            recomputeDisplay(
                state.copy(
                    expression = nextExpr,
                    justEvaluated = false,
                    error = null
                )
            )
        }
    }

    fun onDecimalPoint() {
        _uiState.update { state ->
            val baseExpr = if (state.justEvaluated) "" else state.expression
            if (currentNumberHasDecimal(baseExpr)) {
                return@update state
            }
            val lastChar = baseExpr.lastOrNull()
            val nextExpr = when {
                baseExpr.isEmpty() || lastChar == '(' || (lastChar != null && isOperatorChar(lastChar)) -> "${baseExpr}0."
                lastChar == ')' || lastChar == 'π' || lastChar == 'e' ||
                    lastChar == '%' || lastChar == '!' || lastChar == '²' -> "${baseExpr}×0."
                else -> "$baseExpr."
            }
            recomputeDisplay(
                state.copy(
                    expression = nextExpr,
                    justEvaluated = false,
                    error = null
                )
            )
        }
    }

    fun onOperator(op: String) {
        _uiState.update { state ->
            val baseExpr = if (state.justEvaluated && state.lastResultValue != null) {
                NumberFormatter.formatForInput(state.lastResultValue)
            } else {
                state.expression
            }

            if (baseExpr.isEmpty()) {
                return@update if (op == "−" || op == "-") {
                    recomputeDisplay(state.copy(expression = "−", justEvaluated = false, error = null))
                } else {
                    state
                }
            }

            val lastChar = baseExpr.last()
            val normalizedOp = when (op) {
                "-" -> "−"
                "*" -> "×"
                "/" -> "÷"
                else -> op
            }

            val nextExpr = when {
                lastChar == '(' -> {
                    if (normalizedOp == "−") "$baseExpr−" else baseExpr
                }
                isOperatorChar(lastChar) -> {
                    if (normalizedOp == "−" && (lastChar == '×' || lastChar == '÷' || lastChar == '^')) {
                        "$baseExpr−"
                    } else if (baseExpr.length >= 2 && lastChar == '−' && isOperatorChar(baseExpr[baseExpr.length - 2])) {
                        // Replace trailing binary+unary pair like ×− with new operator
                        if (normalizedOp == "−") baseExpr else baseExpr.dropLast(2) + normalizedOp
                    } else if (baseExpr.length == 1 && (lastChar == '−' || lastChar == '-') && normalizedOp != "−") {
                        // Cannot replace standalone unary minus with binary op
                        baseExpr
                    } else {
                        baseExpr.dropLast(1) + normalizedOp
                    }
                }
                else -> baseExpr + normalizedOp
            }

            recomputeDisplay(
                state.copy(
                    expression = nextExpr,
                    justEvaluated = false,
                    error = null
                )
            )
        }
    }

    fun onSmartParenthesis() {
        _uiState.update { state ->
            val baseExpr = if (state.justEvaluated) "" else state.expression
            val openCount = countUnmatchedOpenParens(baseExpr)
            val lastChar = baseExpr.lastOrNull()
            val canClose = openCount > 0 && lastChar != null &&
                (lastChar.isDigit() || lastChar == ')' || lastChar == 'π' ||
                    lastChar == 'e' || lastChar == '%' || lastChar == '!' || lastChar == '²')
            val nextExpr = baseExpr + if (canClose) ")" else "("
            recomputeDisplay(
                state.copy(
                    expression = nextExpr,
                    justEvaluated = false,
                    error = null
                )
            )
        }
    }

    fun onOpenParenthesis() {
        _uiState.update { state ->
            val baseExpr = if (state.justEvaluated) "" else state.expression
            recomputeDisplay(
                state.copy(
                    expression = "$baseExpr(",
                    justEvaluated = false,
                    error = null
                )
            )
        }
    }

    fun onCloseParenthesis() {
        _uiState.update { state ->
            val baseExpr = if (state.justEvaluated) "" else state.expression
            if (baseExpr.isEmpty()) return@update state
            recomputeDisplay(
                state.copy(
                    expression = "$baseExpr)",
                    justEvaluated = false,
                    error = null
                )
            )
        }
    }

    fun onPercent() {
        appendPostfix("%")
    }

    fun onFactorial() {
        appendPostfix("!")
    }

    fun onSquare() {
        appendPostfix("²")
    }

    private fun appendPostfix(symbol: String) {
        _uiState.update { state ->
            val baseExpr = if (state.justEvaluated && state.lastResultValue != null) {
                val raw = NumberFormatter.formatForInput(state.lastResultValue)
                if (raw.startsWith("-") && symbol == "²") "($raw)" else raw
            } else {
                state.expression
            }
            val lastChar = baseExpr.lastOrNull() ?: return@update state
            val validPrev = lastChar.isDigit() || lastChar == ')' || lastChar == 'π' ||
                lastChar == 'e' || lastChar == '%' || lastChar == '!' || lastChar == '²'
            if (!validPrev) return@update state

            recomputeDisplay(
                state.copy(
                    expression = baseExpr + symbol,
                    justEvaluated = false,
                    error = null
                )
            )
        }
    }

    fun onFunction(funcPrefix: String) {
        _uiState.update { state ->
            val baseExpr = if (state.justEvaluated) "" else state.expression
            recomputeDisplay(
                state.copy(
                    expression = baseExpr + funcPrefix,
                    justEvaluated = false,
                    error = null
                )
            )
        }
    }

    fun onConstant(constant: String) {
        _uiState.update { state ->
            val baseExpr = if (state.justEvaluated) "" else state.expression
            recomputeDisplay(
                state.copy(
                    expression = baseExpr + constant,
                    justEvaluated = false,
                    error = null
                )
            )
        }
    }

    fun onTenPowerX() {
        _uiState.update { state ->
            val baseExpr = if (state.justEvaluated) "" else state.expression
            val lastChar = baseExpr.lastOrNull()
            val needsMultiply = lastChar != null && (
                lastChar.isDigit() || lastChar == '.' || lastChar == ')' ||
                    lastChar == 'π' || lastChar == 'e' || lastChar == '%' ||
                    lastChar == '!' || lastChar == '²'
                )
            val token = if (needsMultiply) "×10^(" else "10^("
            recomputeDisplay(
                state.copy(
                    expression = baseExpr + token,
                    justEvaluated = false,
                    error = null
                )
            )
        }
    }

    fun onEPowerX() {
        _uiState.update { state ->
            val baseExpr = if (state.justEvaluated) "" else state.expression
            recomputeDisplay(
                state.copy(
                    expression = "${baseExpr}e^(",
                    justEvaluated = false,
                    error = null
                )
            )
        }
    }

    fun onReciprocal() {
        _uiState.update { state ->
            if (state.justEvaluated && state.lastResultValue != null) {
                val raw = NumberFormatter.formatForInput(state.lastResultValue)
                return@update recomputeDisplay(
                    state.copy(
                        expression = "1÷($raw)",
                        justEvaluated = false,
                        error = null
                    )
                )
            }
            val expr = state.expression
            if (expr.isEmpty() || expr.last() == '(' || isOperatorChar(expr.last())) {
                return@update recomputeDisplay(
                    state.copy(
                        expression = "${expr}1÷(",
                        justEvaluated = false,
                        error = null
                    )
                )
            }
            // Wrap trailing number or constant
            val idx = findTrailingOperandStart(expr)
            val prefix = expr.substring(0, idx)
            val operand = expr.substring(idx)
            val wrapped = if (operand.startsWith("(") && operand.endsWith(")")) {
                "1÷$operand"
            } else {
                "1÷($operand)"
            }
            recomputeDisplay(
                state.copy(
                    expression = prefix + wrapped,
                    justEvaluated = false,
                    error = null
                )
            )
        }
    }

    fun onBackspace() {
        _uiState.update { state ->
            if (state.expression.isEmpty()) {
                return@update state.copy(resultText = "", error = null, justEvaluated = false)
            }
            val nextExpr = deleteLastTokenOrChar(state.expression)
            recomputeDisplay(
                state.copy(
                    expression = nextExpr,
                    justEvaluated = false,
                    error = null
                )
            )
        }
    }

    fun onClear() {
        _uiState.update { state ->
            state.copy(
                expression = "",
                resultText = "",
                lastResultValue = null,
                justEvaluated = false,
                error = null
            )
        }
    }

    fun onEquals() {
        val currentState = _uiState.value
        val expr = currentState.expression
        if (expr.isBlank()) return
        if (currentState.justEvaluated && currentState.error == null) return

        val eval = mathEngine.evaluate(
            expression = expr,
            angleMode = currentState.angleMode,
            autoCloseParens = true,
            allowIncomplete = false
        )

        when (eval) {
            is EvalResult.Success -> {
                val formatted = NumberFormatter.format(eval.value, currentState.numberFormatStyle)
                val plainInput = NumberFormatter.formatForInput(eval.value)
                _uiState.update {
                    it.copy(
                        resultText = formatted,
                        lastResultValue = eval.value,
                        justEvaluated = true,
                        error = null
                    )
                }
                if (shouldSaveToHistory(expr)) {
                    viewModelScope.launch(Dispatchers.IO) {
                        lazyHistoryRepository.value.saveCalculation(
                            expression = expr,
                            result = plainInput,
                            formattedResult = formatted
                        )
                    }
                }
            }
            is EvalResult.Error -> {
                _uiState.update {
                    it.copy(
                        resultText = "",
                        error = eval.error,
                        justEvaluated = false
                    )
                }
            }
            EvalResult.Empty, EvalResult.Incomplete -> Unit
        }
    }

    fun toggleScientificExpanded() {
        _uiState.update { it.copy(isScientificExpanded = !it.isScientificExpanded) }
    }

    fun toggleAngleMode() {
        _uiState.update { state ->
            val nextMode = if (state.angleMode == AngleMode.DEG) AngleMode.RAD else AngleMode.DEG
            recomputeDisplay(state.copy(angleMode = nextMode))
        }
    }

    fun onMemoryClear() {
        _uiState.update {
            it.copy(
                memoryValue = BigDecimal.ZERO,
                hasMemory = false
            )
        }
    }

    fun onMemoryRecall() {
        _uiState.update { state ->
            if (!state.hasMemory) return@update state
            val memStr = NumberFormatter.formatForInput(state.memoryValue)
            val baseExpr = if (state.justEvaluated) "" else state.expression
            val lastChar = baseExpr.lastOrNull()
            val needsMultiply = lastChar != null && (
                lastChar.isDigit() || lastChar == '.' || lastChar == ')' ||
                    lastChar == 'π' || lastChar == 'e' || lastChar == '%' ||
                    lastChar == '!' || lastChar == '²'
                )
            val tokenToAppend = when {
                needsMultiply -> {
                    val rhs = if (memStr.startsWith("-")) "($memStr)" else memStr
                    "×$rhs"
                }
                memStr.startsWith("-") && lastChar != null && isOperatorChar(lastChar) -> "($memStr)"
                else -> memStr
            }
            recomputeDisplay(
                state.copy(
                    expression = baseExpr + tokenToAppend,
                    justEvaluated = false,
                    error = null
                )
            )
        }
    }

    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            lazyHistoryRepository.value.clearHistory()
        }
    }

    fun onMemoryAdd() {
        modifyMemory(add = true)
    }

    fun onMemorySubtract() {
        modifyMemory(add = false)
    }

    private fun modifyMemory(add: Boolean) {
        val state = _uiState.value
        val currentValue = resolveCurrentValue(state) ?: return
        _uiState.update { current ->
            val updatedMem = if (add) {
                current.memoryValue.add(currentValue)
            } else {
                current.memoryValue.subtract(currentValue)
            }
            current.copy(
                memoryValue = updatedMem,
                hasMemory = true
            )
        }
    }

    private fun resolveCurrentValue(state: CalculatorUiState): BigDecimal? {
        if (state.justEvaluated && state.lastResultValue != null) {
            return state.lastResultValue
        }
        if (state.expression.isBlank()) return null
        return when (val eval = mathEngine.evaluate(
            expression = state.expression,
            angleMode = state.angleMode,
            autoCloseParens = true,
            allowIncomplete = false
        )) {
            is EvalResult.Success -> eval.value
            else -> null
        }
    }

    fun loadExpressionFromHistory(expression: String) {
        _uiState.update { state ->
            recomputeDisplay(
                state.copy(
                    expression = expression,
                    justEvaluated = false,
                    error = null
                )
            )
        }
    }

    private fun recomputeDisplay(state: CalculatorUiState): CalculatorUiState {
        if (state.expression.isBlank()) {
            return state.copy(resultText = "", error = null)
        }
        val eval = mathEngine.evaluate(
            expression = state.expression,
            angleMode = state.angleMode,
            autoCloseParens = true,
            allowIncomplete = true
        )
        return when (eval) {
            is EvalResult.Success -> {
                val formatted = NumberFormatter.format(eval.value, state.numberFormatStyle)
                val showPreview = state.justEvaluated || shouldShowLivePreview(state.expression)
                state.copy(
                    resultText = if (showPreview) formatted else "",
                    lastResultValue = eval.value,
                    error = null
                )
            }
            is EvalResult.Incomplete, EvalResult.Empty -> {
                state.copy(resultText = "", error = null)
            }
            is EvalResult.Error -> {
                // During live typing, show errors only on mismatched parens or after equals;
                // keep typing smooth for domain errors until '=' unless justEvaluated.
                if (state.justEvaluated) {
                    state.copy(resultText = "", error = eval.error)
                } else {
                    state.copy(resultText = "", error = null)
                }
            }
        }
    }

    private fun shouldShowLivePreview(expr: String): Boolean {
        // Show live result as soon as expression contains an operator, function, constant, or postfix
        return expr.any {
            it == '+' || it == '−' || it == '-' || it == '×' || it == '÷' ||
                it == '^' || it == '%' || it == '!' || it == '²' || it == '√' ||
                it == 'π' || it == 'e' || it.isLetter()
        }
    }

    private fun shouldSaveToHistory(expr: String): Boolean {
        val trimmed = expr.trim()
        // Save any real expression (operators, functions, constants, parens, postfix)
        return trimmed.any {
            it == '+' || it == '−' || it == '-' || it == '×' || it == '÷' ||
                it == '^' || it == '%' || it == '!' || it == '²' || it == '√' ||
                it == 'π' || it == 'e' || it == '(' || it.isLetter()
        }
    }

    private fun isOperatorChar(ch: Char): Boolean {
        return ch == '+' || ch == '−' || ch == '-' || ch == '×' || ch == '*' ||
            ch == '÷' || ch == '/' || ch == '^'
    }

    private fun countUnmatchedOpenParens(expr: String): Int {
        var count = 0
        for (ch in expr) {
            if (ch == '(') count++
            else if (ch == ')') count--
        }
        return count.coerceAtLeast(0)
    }

    private fun currentNumberHasDecimal(expr: String): Boolean {
        for (i in expr.length - 1 downTo 0) {
            val ch = expr[i]
            if (ch == '.') return true
            if (!ch.isDigit()) return false
        }
        return false
    }

    private fun isStandaloneLeadingZero(expr: String): Boolean {
        if (!expr.endsWith("0")) return false
        val prevIdx = expr.length - 2
        if (prevIdx < 0) return true
        val prev = expr[prevIdx]
        return !prev.isDigit() && prev != '.'
    }

    private fun findTrailingOperandStart(expr: String): Int {
        if (expr.endsWith(")")) {
            var depth = 0
            for (i in expr.length - 1 downTo 0) {
                if (expr[i] == ')') depth++
                else if (expr[i] == '(') {
                    depth--
                    if (depth == 0) {
                        var start = i
                        while (start > 0 && (expr[start - 1].isLetter() || expr[start - 1] == '√')) {
                            start--
                        }
                        return start
                    }
                }
            }
            return 0
        }
        var i = expr.length - 1
        while (i >= 0 && (expr[i].isDigit() || expr[i] == '.' || expr[i] == 'π' || expr[i] == 'e')) {
            i--
        }
        return (i + 1).coerceAtMost(expr.length)
    }

    private fun deleteLastTokenOrChar(expr: String): String {
        for (prefix in FUNCTION_PREFIXES) {
            if (expr.endsWith(prefix)) {
                return expr.dropLast(prefix.length)
            }
        }
        return expr.dropLast(1)
    }

    private companion object {
        val FUNCTION_PREFIXES = listOf(
            "asinh(", "acosh(", "atanh(",
            "sinh(", "cosh(", "tanh(",
            "asin(", "acos(", "atan(",
            "sin(", "cos(", "tan(",
            "sqrt(", "abs(", "log(", "ln(",
            "10^(", "e^(", "√("
        )
    }

    class Factory(
        private val mathEngine: MathEngine,
        private val lazyHistoryRepository: Lazy<HistoryRepository>,
        private val settingsPreferences: SettingsPreferences
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CalculatorViewModel(mathEngine, lazyHistoryRepository, settingsPreferences) as T
        }
    }
}
