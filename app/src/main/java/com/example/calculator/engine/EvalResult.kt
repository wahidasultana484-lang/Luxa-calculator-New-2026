package com.example.calculator.engine

import java.math.BigDecimal

sealed class EvalResult {
    data class Success(val value: BigDecimal) : EvalResult()
    data class Error(val error: MathError) : EvalResult()
    data object Empty : EvalResult()
    data object Incomplete : EvalResult()
}
