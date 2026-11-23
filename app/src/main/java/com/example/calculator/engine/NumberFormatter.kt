package com.example.calculator.engine

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

object NumberFormatter {

    private val DISPLAY_MC = MathContext(12, RoundingMode.HALF_UP)
    private val DISPLAY_SCI_MC = MathContext(10, RoundingMode.HALF_UP)
    private val INPUT_MC = MathContext(18, RoundingMode.HALF_UP)
    private val SCIENTIFIC_UPPER_THRESHOLD = BigDecimal("1000000000000") // 1e12
    private val SCIENTIFIC_LOWER_THRESHOLD = BigDecimal("0.000000001")   // 1e-9

    fun format(
        value: BigDecimal,
        style: NumberFormatStyle = NumberFormatStyle.INTERNATIONAL
    ): String {
        val cleaned = normalize(value, DISPLAY_MC)
        if (cleaned.compareTo(BigDecimal.ZERO) == 0) return "0"

        val absVal = cleaned.abs()
        return if (absVal >= SCIENTIFIC_UPPER_THRESHOLD || absVal < SCIENTIFIC_LOWER_THRESHOLD) {
            formatScientific(cleaned, DISPLAY_SCI_MC)
        } else {
            formatDecimal(cleaned, style)
        }
    }

    /**
     * Plain ungrouped representation safe for inserting back into mathematical expressions.
     * Retains higher precision (18 significant digits) so chained operations avoid rounding drift.
     */
    fun formatForInput(value: BigDecimal): String {
        val cleaned = normalize(value, INPUT_MC)
        if (cleaned.compareTo(BigDecimal.ZERO) == 0) return "0"

        val absVal = cleaned.abs()
        return if (absVal >= SCIENTIFIC_UPPER_THRESHOLD || absVal < SCIENTIFIC_LOWER_THRESHOLD) {
            formatScientific(cleaned, INPUT_MC)
        } else {
            formatDecimal(cleaned, NumberFormatStyle.PLAIN)
        }
    }

    private fun normalize(value: BigDecimal, mc: MathContext): BigDecimal {
        return if (value.compareTo(BigDecimal.ZERO) == 0) {
            BigDecimal.ZERO
        } else {
            value.round(mc).stripTrailingZeros()
        }
    }

    private fun formatDecimal(value: BigDecimal, style: NumberFormatStyle): String {
        val finalVal = if (value.compareTo(BigDecimal.ZERO) == 0) BigDecimal.ZERO else value.stripTrailingZeros()
        val plain = finalVal.toPlainString()
        val isNegative = plain.startsWith("-")
        val unsigned = if (isNegative) plain.substring(1) else plain

        val dotIndex = unsigned.indexOf('.')
        val integerPart = if (dotIndex >= 0) unsigned.substring(0, dotIndex) else unsigned
        val fractionalPart = if (dotIndex >= 0) unsigned.substring(dotIndex) else ""

        val groupedInt = when (style) {
            NumberFormatStyle.PLAIN -> integerPart
            NumberFormatStyle.INTERNATIONAL -> groupInternational(integerPart)
            NumberFormatStyle.INDIAN -> groupIndian(integerPart)
        }

        return buildString {
            if (isNegative) append('-')
            append(groupedInt)
            append(fractionalPart)
        }
    }

    private fun formatScientific(value: BigDecimal, mc: MathContext): String {
        val rounded = value.round(mc).stripTrailingZeros()
        val sci = rounded.toString() // e.g., 1.234E+12 or 1E+12
        val eIndex = sci.indexOf('E')
        if (eIndex < 0) return sci

        val mantissaRaw = sci.substring(0, eIndex)
        val exponentRaw = sci.substring(eIndex + 1).removePrefix("+")

        val mantissaClean = if (mantissaRaw.contains('.')) {
            mantissaRaw.trimEnd('0').trimEnd('.')
        } else {
            mantissaRaw
        }
        return "${mantissaClean}E$exponentRaw"
    }

    private fun groupInternational(digits: String): String {
        if (digits.length <= 3) return digits
        val sb = StringBuilder()
        var count = 0
        for (i in digits.length - 1 downTo 0) {
            sb.append(digits[i])
            count++
            if (count == 3 && i != 0) {
                sb.append(',')
                count = 0
            }
        }
        return sb.reverse().toString()
    }

    private fun groupIndian(digits: String): String {
        if (digits.length <= 3) return digits
        val lastThree = digits.substring(digits.length - 3)
        val remaining = digits.substring(0, digits.length - 3)
        val sb = StringBuilder()
        var count = 0
        for (i in remaining.length - 1 downTo 0) {
            sb.append(remaining[i])
            count++
            if (count == 2 && i != 0) {
                sb.append(',')
                count = 0
            }
        }
        return "${sb.reverse()},$lastThree"
    }
}
