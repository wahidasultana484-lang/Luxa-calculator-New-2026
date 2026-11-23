package com.example

import com.example.calculator.engine.AngleMode
import com.example.calculator.engine.EvalResult
import com.example.calculator.engine.MathEngine
import com.example.calculator.engine.MathError
import com.example.calculator.engine.NumberFormatStyle
import com.example.calculator.engine.NumberFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class MathEngineTest {

    private val engine = MathEngine()

    private fun evalFormatted(
        expr: String,
        angleMode: AngleMode = AngleMode.DEG,
        style: NumberFormatStyle = NumberFormatStyle.PLAIN
    ): String {
        val result = engine.evaluate(expr, angleMode = angleMode)
        assertTrue("Expected Success for '$expr' but got $result", result is EvalResult.Success)
        return NumberFormatter.format((result as EvalResult.Success).value, style)
    }

    @Test
    fun requiredTestCases_evaluateAccurately() {
        assertEquals("14", evalFormatted("2 + 3 × 4"))
        assertEquals("20", evalFormatted("(2 + 3) × 4"))
        assertEquals("230", evalFormatted("200 + 15%"))
        assertEquals("12", evalFormatted("√144"))
        assertEquals("625", evalFormatted("25²"))
        assertEquals("8", evalFormatted("2^3"))
        assertEquals("8", evalFormatted("2³"))
        assertEquals("0.5", evalFormatted("sin(30)", AngleMode.DEG))
        assertEquals("0.5", evalFormatted("cos(60)", AngleMode.DEG))
        assertEquals("2", evalFormatted("log(100)"))
        assertEquals("1", evalFormatted("ln(e)"))
        assertEquals("0.3", evalFormatted("0.1 + 0.2"))
    }

    @Test
    fun percentageVariations_evaluateCorrectly() {
        assertEquals("170", evalFormatted("200 - 15%"))
        assertEquals("30", evalFormatted("200 × 15%"))
        assertEquals("0.5", evalFormatted("50%"))
        assertEquals("132", evalFormatted("100 + 20% + 10%"))
    }

    @Test
    fun scientificFunctions_degAndRad_evaluateCorrectly() {
        assertEquals("1", evalFormatted("tan(45)", AngleMode.DEG))
        assertEquals("0", evalFormatted("sin(π)", AngleMode.RAD))
        assertEquals("-1", evalFormatted("cos(π)", AngleMode.RAD))
        assertEquals("30", evalFormatted("asin(0.5)", AngleMode.DEG))
        assertEquals("60", evalFormatted("acos(0.5)", AngleMode.DEG))
        assertEquals("45", evalFormatted("atan(1)", AngleMode.DEG))
        assertEquals("0", evalFormatted("sinh(0)"))
        assertEquals("1", evalFormatted("cosh(0)"))
        assertEquals("0", evalFormatted("tanh(0)"))
        assertEquals("1000", evalFormatted("10^(3)"))
        assertEquals("1", evalFormatted("e^(0)"))
        assertEquals("120", evalFormatted("5!"))
        assertEquals("1", evalFormatted("0!"))
        assertEquals("42.5", evalFormatted("abs(-42.5)"))
        assertEquals("0.25", evalFormatted("1÷(4)"))
        assertEquals("6", evalFormatted("2√9"))
    }

    @Test
    fun errorHandling_returnsExpectedMathErrors() {
        assertEquals(EvalResult.Error(MathError.DIVISION_BY_ZERO), engine.evaluate("10 ÷ 0"))
        assertEquals(EvalResult.Error(MathError.UNDEFINED), engine.evaluate("√(-9)"))
        assertEquals(EvalResult.Error(MathError.UNDEFINED), engine.evaluate("log(0)"))
        assertEquals(EvalResult.Error(MathError.UNDEFINED), engine.evaluate("ln(-1)"))
        assertEquals(EvalResult.Error(MathError.UNDEFINED), engine.evaluate("asin(2)"))
        assertEquals(EvalResult.Error(MathError.UNDEFINED), engine.evaluate("tan(90)", AngleMode.DEG))
        assertEquals(EvalResult.Error(MathError.UNDEFINED), engine.evaluate("(-3)!"))
        assertEquals(EvalResult.Error(MathError.MISSING_PARENTHESIS), engine.evaluate("2 + 3)"))
        assertEquals(
            EvalResult.Error(MathError.MISSING_PARENTHESIS),
            engine.evaluate("(2 + 3", autoCloseParens = false)
        )
        assertEquals(EvalResult.Error(MathError.INVALID_EXPRESSION), engine.evaluate("2 +"))
        assertEquals(EvalResult.Error(MathError.INVALID_EXPRESSION), engine.evaluate("()"))
        assertEquals(EvalResult.Error(MathError.OVERFLOW), engine.evaluate("171!"))
        assertEquals(EvalResult.Error(MathError.OVERFLOW), engine.evaluate("e^(1000)"))
        assertEquals(EvalResult.Error(MathError.UNDERFLOW), engine.evaluate("e^(-1000)"))
    }

    @Test
    fun numberFormatting_supportsInternationalIndianPlainAndScientific() {
        val large = BigDecimal("1234567.89")
        assertEquals("1,234,567.89", NumberFormatter.format(large, NumberFormatStyle.INTERNATIONAL))
        assertEquals("12,34,567.89", NumberFormatter.format(large, NumberFormatStyle.INDIAN))
        assertEquals("1234567.89", NumberFormatter.format(large, NumberFormatStyle.PLAIN))

        val negative = BigDecimal("-1000000")
        assertEquals("-1,000,000", NumberFormatter.format(negative, NumberFormatStyle.INTERNATIONAL))
        assertEquals("-10,00,000", NumberFormatter.format(negative, NumberFormatStyle.INDIAN))

        val huge = BigDecimal("1500000000000")
        assertEquals("1.5E12", NumberFormatter.format(huge, NumberFormatStyle.INTERNATIONAL))

        val tiny = BigDecimal("0.00000000025")
        assertEquals("2.5E-10", NumberFormatter.format(tiny, NumberFormatStyle.INTERNATIONAL))

        // Sub-unit decimals above 1e-9 must retain full significant digits (up to 12 sig figs)
        val smallDecimal = BigDecimal("0.0000000123456789")
        assertEquals("0.0000000123456789", NumberFormatter.format(smallDecimal, NumberFormatStyle.INTERNATIONAL))
        assertEquals("0.0000000123456789", NumberFormatter.format(smallDecimal, NumberFormatStyle.INDIAN))
        assertEquals("0.0000000123456789", NumberFormatter.format(smallDecimal, NumberFormatStyle.PLAIN))
    }

    @Test
    fun operatorPrecedence_unaryMinus_andPowerAssociativity_areAccurate() {
        assertEquals("-4", evalFormatted("-2^2"))
        assertEquals("4", evalFormatted("(-2)^2"))
        assertEquals("0.125", evalFormatted("2^-3"))
        assertEquals("512", evalFormatted("2^3^2"))
        assertEquals("-8", evalFormatted("(-2)^3"))
        assertEquals("0.25", evalFormatted("(-2)^(-2)"))
        assertEquals("7", evalFormatted("1 + 2 × 3"))
        assertEquals("9", evalFormatted("(1 + 2) × 3"))
        assertEquals("14", evalFormatted("2 + 3 × (4 - 0)"))
        assertEquals("6", evalFormatted("((1 + 2) × (4 - 2))"))
    }

    @Test
    fun trigHighPrecision_largeAnglesAndTinyAngles_evaluateAccurately() {
        // BigDecimal mod 2pi reduction keeps n*pi exact in RAD mode
        assertEquals("0", evalFormatted("sin(1000 × π)", AngleMode.RAD))
        assertEquals("1", evalFormatted("cos(1000 × π)", AngleMode.RAD))
        assertEquals("0", evalFormatted("cos(1000 × π + π ÷ 2)", AngleMode.RAD))
        assertEquals("1", evalFormatted("sin(1000 × π + π ÷ 2)", AngleMode.RAD))
        assertEquals(
            EvalResult.Error(MathError.UNDEFINED),
            engine.evaluate("tan(1000 × π + π ÷ 2)", angleMode = AngleMode.RAD)
        )

        // Large DEG angle must not throw Division impossible
        val sin1e40 = engine.evaluate("sin(1E40)", angleMode = AngleMode.DEG)
        assertTrue("Expected Success for sin(1E40) DEG, got $sin1e40", sin1e40 is EvalResult.Success)
        val expectedSin280 = kotlin.math.sin(280.0 * kotlin.math.PI / 180.0)
        assertEquals(expectedSin280, (sin1e40 as EvalResult.Success).value.toDouble(), 1e-12)

        // Tiny angles must NOT be falsely truncated to 0
        assertEquals("1E-15", evalFormatted("sin(1E-15)", AngleMode.RAD))
        assertEquals("1E-15", evalFormatted("tan(1E-15)", AngleMode.RAD))
        assertEquals("1E-15", evalFormatted("asin(1E-15)", AngleMode.RAD))
        assertEquals("1E-15", evalFormatted("atan(1E-15)", AngleMode.RAD))
        assertEquals("1E-15", evalFormatted("sinh(1E-15)", AngleMode.RAD))
        assertEquals("1E-15", evalFormatted("tanh(1E-15)", AngleMode.RAD))
    }

    @Test
    fun logarithms_powers_roots_andFactorials_handleExtremesAccurately() {
        assertEquals("0", evalFormatted("log(1)"))
        assertEquals("-308", evalFormatted("log(1E-308)"))
        assertEquals("250", evalFormatted("log(1E250)"))
        assertEquals("0", evalFormatted("ln(1)"))
        assertEquals("5", evalFormatted("e^(ln(5))"))
        assertEquals("2", evalFormatted("√(2) × √(2)"))
        assertEquals("1", evalFormatted("(1 ÷ 3) × 3"))
        assertEquals("1", evalFormatted("(1 ÷ 7) × 7"))

        // Powers with ±1 and huge exponents
        assertEquals("1", evalFormatted("1^(1E30)"))
        assertEquals("1", evalFormatted("(-1)^(1E30)"))
        assertEquals("-1", evalFormatted("(-1)^(1000000000000000000000000000001)"))
        assertEquals("1", evalFormatted("0^0"))
        assertEquals(EvalResult.Error(MathError.DIVISION_BY_ZERO), engine.evaluate("0^(-1)"))
        assertEquals(EvalResult.Error(MathError.UNDERFLOW), engine.evaluate("0.5^(1E30)"))
        assertEquals(EvalResult.Error(MathError.UNDERFLOW), engine.evaluate("2^(-1E30)"))
        assertEquals(EvalResult.Error(MathError.OVERFLOW), engine.evaluate("0.5^(-1E30)"))

        // Factorials: 170! is finite, 171! overflows, non-integer factorial is undefined
        val fact170 = engine.evaluate("170!")
        assertTrue("170! should succeed", fact170 is EvalResult.Success)
        assertEquals(EvalResult.Error(MathError.UNDEFINED), engine.evaluate("0.5!"))
        assertEquals(EvalResult.Error(MathError.UNDEFINED), engine.evaluate("(-2)^0.5"))
    }

    @Test
    fun chainedCalculations_andFormatterInput_preservePrecision() {
        val oneThird = (engine.evaluate("1 ÷ 3") as EvalResult.Success).value
        val chainedInput = NumberFormatter.formatForInput(oneThird)
        assertEquals("1", evalFormatted("$chainedInput × 3"))

        val oneSeventh = (engine.evaluate("1 ÷ 7") as EvalResult.Success).value
        val seventhInput = NumberFormatter.formatForInput(oneSeventh)
        assertEquals("1", evalFormatted("$seventhInput × 7"))
    }
}
