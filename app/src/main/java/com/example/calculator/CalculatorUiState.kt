package com.example.calculator

import com.example.calculator.engine.AngleMode
import com.example.calculator.engine.MathError
import com.example.calculator.engine.NumberFormatStyle
import java.math.BigDecimal

data class CalculatorUiState(
    val expression: String = "",
    val resultText: String = "",
    val lastResultValue: BigDecimal? = null,
    val justEvaluated: Boolean = false,
    val error: MathError? = null,
    val isScientificExpanded: Boolean = false,
    val angleMode: AngleMode = AngleMode.DEG,
    val numberFormatStyle: NumberFormatStyle = NumberFormatStyle.INTERNATIONAL,
    val hasMemory: Boolean = false,
    val memoryValue: BigDecimal = BigDecimal.ZERO,
    val hapticFeedbackEnabled: Boolean = true,
    val buttonSoundEnabled: Boolean = false,
)
