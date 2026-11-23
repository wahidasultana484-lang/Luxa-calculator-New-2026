package com.example.calculator.engine

enum class AngleMode {
    DEG,
    RAD
}

enum class NumberFormatStyle {
    INTERNATIONAL,
    INDIAN,
    PLAIN
}

enum class MathError {
    DIVISION_BY_ZERO,
    INVALID_EXPRESSION,
    MISSING_PARENTHESIS,
    OVERFLOW,
    UNDERFLOW,
    UNDEFINED
}
