package com.example.calculator.engine

import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.cosh
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.tan
import kotlin.math.tanh

class MathEngine {

    private companion object {
        val MC: MathContext = MathContext.DECIMAL128 // 34 digits precision
        val DOUBLE_RESULT_MC: MathContext = MathContext(14, RoundingMode.HALF_UP)
        val CLEAN_RESULT_MC: MathContext = MathContext(24, RoundingMode.HALF_UP)
        val MAX_MAGNITUDE = BigDecimal("1E308")
        val MIN_MAGNITUDE = BigDecimal("1E-308")
        val PI_HIGH = BigDecimal("3.14159265358979323846264338327950288419716939937510")
        val TWO_PI_HIGH: BigDecimal = PI_HIGH.multiply(BigDecimal(2))
        val HALF_PI_HIGH: BigDecimal = PI_HIGH.divide(BigDecimal(2))
        val THREE_HALF_PI_HIGH: BigDecimal = PI_HIGH.multiply(BigDecimal("1.5"))
        val PI_DECIMAL: BigDecimal = PI_HIGH.round(MC)
        val E_DECIMAL = BigDecimal("2.7182818284590452353602874713526625")
        val LN_10_HIGH = BigDecimal("2.3025850929940456840179914546843642")
        val TRIG_ZERO_TOLERANCE = BigDecimal("1E-14")
        val SMALL_ANGLE_CUTOFF = BigDecimal("1E-10")
        val RAD_MAX_ANGLE = BigDecimal("1E35")
        val BD_TWO = BigDecimal(2)
        val BD_THREE = BigDecimal(3)
        val BD_100 = BigDecimal(100)
        val BD_360 = BigDecimal(360)
        val BD_HALF = BigDecimal("0.5")
        val BD_NEG_HALF = BigDecimal("-0.5")
        val BD_NEG_ONE = BigDecimal("-1")
        val DEG_TO_RAD = 180.0 / PI
    }

    private val mc = MC
    private val piDecimal = PI_DECIMAL
    private val eDecimal = E_DECIMAL

    private class MathEvalException(val error: MathError) : RuntimeException()
    private class IncompleteExprException : RuntimeException()

    fun evaluate(
        expression: String,
        angleMode: AngleMode = AngleMode.DEG,
        autoCloseParens: Boolean = true,
        allowIncomplete: Boolean = false
    ): EvalResult {
        val trimmed = expression.trim()
        if (trimmed.isEmpty()) return EvalResult.Empty

        return try {
            val tokens = tokenize(trimmed)
            if (tokens.isEmpty()) return EvalResult.Empty

            val normalizedTokens = prepareTokens(tokens, autoCloseParens, allowIncomplete)
            val parser = Parser(normalizedTokens)
            val ast = parser.parseExpression()
            if (parser.hasMore()) {
                throw MathEvalException(MathError.INVALID_EXPRESSION)
            }

            val value = evalNode(ast, angleMode)
            checkMagnitude(value)
            val cleaned = cleanPrecision(value)
            EvalResult.Success(cleaned)
        } catch (_: IncompleteExprException) {
            if (allowIncomplete) EvalResult.Incomplete else EvalResult.Error(MathError.INVALID_EXPRESSION)
        } catch (e: MathEvalException) {
            EvalResult.Error(e.error)
        } catch (_: ArithmeticException) {
            EvalResult.Error(MathError.UNDEFINED)
        } catch (_: NumberFormatException) {
            EvalResult.Error(MathError.INVALID_EXPRESSION)
        }
    }

    private sealed interface Token {
        data class Number(val value: BigDecimal) : Token
        data class Constant(val name: String) : Token
        data class Func(val name: String) : Token
        data class Op(val op: Char) : Token
        data object LParen : Token
        data object RParen : Token
        data object Percent : Token
        data object Factorial : Token
        data object Square : Token
        data object Cube : Token
    }

    private fun tokenize(input: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var i = 0
        val n = input.length

        while (i < n) {
            val ch = input[i]
            when {
                ch.isWhitespace() || ch == ',' -> i++

                ch.isDigit() || ch == '.' -> {
                    val start = i
                    var hasDigit = false
                    var hasDot = false
                    while (i < n && (input[i].isDigit() || input[i] == '.')) {
                        if (input[i].isDigit()) hasDigit = true
                        if (input[i] == '.') {
                            if (hasDot) throw MathEvalException(MathError.INVALID_EXPRESSION)
                            hasDot = true
                        }
                        i++
                    }
                    if (!hasDigit) {
                        // Standalone "." while typing
                        throw IncompleteExprException()
                    }
                    // Check scientific notation 'E' followed by optional +/- and digits
                    if (i < n && input[i] == 'E') {
                        var expIdx = i + 1
                        if (expIdx < n && (input[expIdx] == '+' || input[expIdx] == '-' || input[expIdx] == '−')) {
                            expIdx++
                        }
                        val expDigitsStart = expIdx
                        while (expIdx < n && input[expIdx].isDigit()) {
                            expIdx++
                        }
                        if (expIdx > expDigitsStart) {
                            val rawNum = input.substring(start, expIdx).replace('−', '-')
                            tokens.add(Token.Number(BigDecimal(rawNum)))
                            i = expIdx
                            continue
                        } else {
                            throw IncompleteExprException()
                        }
                    }
                    val rawNum = input.substring(start, i)
                    val normalizedNum = if (rawNum.startsWith(".")) "0$rawNum" else rawNum
                    tokens.add(Token.Number(BigDecimal(normalizedNum)))
                }

                ch == 'π' -> {
                    tokens.add(Token.Constant("pi"))
                    i++
                }

                ch == 'e' -> {
                    tokens.add(Token.Constant("e"))
                    i++
                }

                ch == '√' -> {
                    tokens.add(Token.Func("sqrt"))
                    i++
                }

                ch == '²' -> {
                    tokens.add(Token.Square)
                    i++
                }

                ch == '³' -> {
                    tokens.add(Token.Cube)
                    i++
                }

                ch == '%' -> {
                    tokens.add(Token.Percent)
                    i++
                }

                ch == '!' -> {
                    tokens.add(Token.Factorial)
                    i++
                }

                ch == '(' -> {
                    tokens.add(Token.LParen)
                    i++
                }

                ch == ')' -> {
                    tokens.add(Token.RParen)
                    i++
                }

                ch == '+' -> {
                    tokens.add(Token.Op('+'))
                    i++
                }

                ch == '-' || ch == '−' -> {
                    tokens.add(Token.Op('-'))
                    i++
                }

                ch == '*' || ch == '×' -> {
                    tokens.add(Token.Op('*'))
                    i++
                }

                ch == '/' || ch == '÷' -> {
                    tokens.add(Token.Op('/'))
                    i++
                }

                ch == '^' -> {
                    tokens.add(Token.Op('^'))
                    i++
                }

                ch.isLetter() -> {
                    val start = i
                    while (i < n && input[i].isLetter()) {
                        i++
                    }
                    val word = input.substring(start, i).lowercase()
                    when (word) {
                        "pi" -> tokens.add(Token.Constant("pi"))
                        "sin", "cos", "tan",
                        "asin", "acos", "atan",
                        "sinh", "cosh", "tanh",
                        "log", "ln", "sqrt", "abs" -> tokens.add(Token.Func(word))
                        else -> throw MathEvalException(MathError.INVALID_EXPRESSION)
                    }
                }

                else -> throw MathEvalException(MathError.INVALID_EXPRESSION)
            }
        }
        return tokens
    }

    private fun prepareTokens(
        tokens: List<Token>,
        autoCloseParens: Boolean,
        allowIncomplete: Boolean
    ): List<Token> {
        var openCount = 0
        for (i in tokens.indices) {
            when (val token = tokens[i]) {
                Token.LParen -> openCount++
                Token.RParen -> {
                    openCount--
                    if (openCount < 0) {
                        throw MathEvalException(MathError.MISSING_PARENTHESIS)
                    }
                    if (i > 0 && tokens[i - 1] == Token.LParen) {
                        throw MathEvalException(MathError.INVALID_EXPRESSION)
                    }
                }
                else -> Unit
            }
        }

        val last = tokens.last()
        val endsIncomplete = last is Token.Op || last is Token.Func || last == Token.LParen
        if (endsIncomplete) {
            if (allowIncomplete) {
                throw IncompleteExprException()
            } else if (last == Token.LParen && openCount > 0) {
                throw MathEvalException(MathError.MISSING_PARENTHESIS)
            } else {
                throw MathEvalException(MathError.INVALID_EXPRESSION)
            }
        }

        if (openCount > 0) {
            if (!autoCloseParens) {
                throw MathEvalException(MathError.MISSING_PARENTHESIS)
            }
            val closed = tokens.toMutableList()
            repeat(openCount) { closed.add(Token.RParen) }
            return closed
        }
        return tokens
    }

    private sealed interface ExprNode {
        data class Num(val value: BigDecimal) : ExprNode
        data class Const(val name: String) : ExprNode
        data class Unary(val op: Char, val inner: ExprNode) : ExprNode
        data class Binary(val op: Char, val left: ExprNode, val right: ExprNode) : ExprNode
        data class Percent(val inner: ExprNode) : ExprNode
        data class Factorial(val inner: ExprNode) : ExprNode
        data class Func(val name: String, val arg: ExprNode) : ExprNode
    }

    private inner class Parser(private val tokens: List<Token>) {
        private var pos = 0

        fun hasMore(): Boolean = pos < tokens.size

        private fun peek(): Token? = tokens.getOrNull(pos)

        private fun consume(): Token = tokens[pos++]

        fun parseExpression(): ExprNode = parseAddSub()

        private fun parseAddSub(): ExprNode {
            var left = parseMulDiv()
            while (true) {
                val next = peek()
                if (next is Token.Op && (next.op == '+' || next.op == '-')) {
                    consume()
                    val right = parseMulDiv()
                    left = ExprNode.Binary(next.op, left, right)
                } else {
                    break
                }
            }
            return left
        }

        private fun parseMulDiv(): ExprNode {
            var left = parseUnary()
            while (true) {
                val next = peek()
                if (next is Token.Op && (next.op == '*' || next.op == '/')) {
                    consume()
                    val right = parseUnary()
                    left = ExprNode.Binary(next.op, left, right)
                } else if (startsImplicitMultiplication(next)) {
                    val right = parseUnary()
                    left = ExprNode.Binary('*', left, right)
                } else {
                    break
                }
            }
            return left
        }

        private fun startsImplicitMultiplication(token: Token?): Boolean {
            return token is Token.Number ||
                token is Token.Constant ||
                token is Token.Func ||
                token == Token.LParen
        }

        private fun parseUnary(): ExprNode {
            val next = peek()
            if (next is Token.Op && (next.op == '+' || next.op == '-')) {
                consume()
                val operand = parseUnary()
                return ExprNode.Unary(next.op, operand)
            }
            return parsePower()
        }

        private fun parsePower(): ExprNode {
            var base = parsePostfix()
            val next = peek()
            if (next is Token.Op && next.op == '^') {
                consume()
                val exponent = parseUnary() // right-associative, supports 2^-3 and 2^3^2
                base = ExprNode.Binary('^', base, exponent)
            }
            return base
        }

        private fun parsePostfix(): ExprNode {
            var node = parsePrimary()
            while (true) {
                when (peek()) {
                    Token.Percent -> {
                        consume()
                        node = ExprNode.Percent(node)
                    }
                    Token.Factorial -> {
                        consume()
                        node = ExprNode.Factorial(node)
                    }
                    Token.Square -> {
                        consume()
                        node = ExprNode.Binary('^', node, ExprNode.Num(BD_TWO))
                    }
                    Token.Cube -> {
                        consume()
                        node = ExprNode.Binary('^', node, ExprNode.Num(BD_THREE))
                    }
                    else -> break
                }
            }
            return node
        }

        private fun parsePrimary(): ExprNode {
            return when (val token = peek()) {
                is Token.Number -> {
                    consume()
                    ExprNode.Num(token.value)
                }
                is Token.Constant -> {
                    consume()
                    ExprNode.Const(token.name)
                }
                Token.LParen -> {
                    consume()
                    val expr = parseExpression()
                    if (peek() != Token.RParen) {
                        throw MathEvalException(MathError.MISSING_PARENTHESIS)
                    }
                    consume()
                    expr
                }
                is Token.Func -> {
                    consume()
                    val arg = if (peek() == Token.LParen) {
                        consume()
                        val inner = parseExpression()
                        if (peek() != Token.RParen) {
                            throw MathEvalException(MathError.MISSING_PARENTHESIS)
                        }
                        consume()
                        inner
                    } else if (token.name == "sqrt") {
                        parseUnary()
                    } else {
                        parsePrimary()
                    }
                    ExprNode.Func(token.name, arg)
                }
                else -> throw MathEvalException(MathError.INVALID_EXPRESSION)
            }
        }
    }

    private fun evalNode(node: ExprNode, angleMode: AngleMode): BigDecimal {
        return when (node) {
            is ExprNode.Num -> node.value
            is ExprNode.Const -> when (node.name) {
                "pi" -> piDecimal
                "e" -> eDecimal
                else -> throw MathEvalException(MathError.INVALID_EXPRESSION)
            }
            is ExprNode.Unary -> {
                val v = evalNode(node.inner, angleMode)
                if (node.op == '-') v.negate() else v
            }
            is ExprNode.Percent -> {
                val inner = evalNode(node.inner, angleMode)
                inner.divide(BD_100, mc)
            }
            is ExprNode.Factorial -> {
                val inner = evalNode(node.inner, angleMode)
                evalFactorial(inner)
            }
            is ExprNode.Binary -> evalBinary(node, angleMode)
            is ExprNode.Func -> evalFunction(node.name, evalNode(node.arg, angleMode), angleMode)
        }
    }

    private fun evalBinary(node: ExprNode.Binary, angleMode: AngleMode): BigDecimal {
        val left = evalNode(node.left, angleMode)

        // Google Calculator percentage semantics for + and - when right operand is a percentage term
        if ((node.op == '+' || node.op == '-') && node.right is ExprNode.Percent) {
            val pctFactor = evalNode(node.right.inner, angleMode).divide(BD_100, mc)
            val delta = left.multiply(pctFactor, mc)
            return if (node.op == '+') left.add(delta, mc) else left.subtract(delta, mc)
        }

        val right = evalNode(node.right, angleMode)
        return when (node.op) {
            '+' -> left.add(right, mc)
            '-' -> left.subtract(right, mc)
            '*' -> left.multiply(right, mc)
            '/' -> {
                if (right.compareTo(BigDecimal.ZERO) == 0) {
                    throw MathEvalException(MathError.DIVISION_BY_ZERO)
                }
                left.divide(right, mc)
            }
            '^' -> evalPower(left, right)
            else -> throw MathEvalException(MathError.INVALID_EXPRESSION)
        }
    }

    private fun evalPower(base: BigDecimal, exponent: BigDecimal): BigDecimal {
        val strippedExp = exponent.stripTrailingZeros()
        val isIntegerExp = strippedExp.scale() <= 0

        if (base.compareTo(BigDecimal.ZERO) == 0) {
            val expSign = exponent.compareTo(BigDecimal.ZERO)
            return when {
                expSign < 0 -> throw MathEvalException(MathError.DIVISION_BY_ZERO)
                expSign == 0 -> BigDecimal.ONE // 0^0 = 1 convention
                else -> BigDecimal.ZERO
            }
        }

        if (base.compareTo(BigDecimal.ONE) == 0) {
            return BigDecimal.ONE
        }

        if (base.compareTo(BD_NEG_ONE) == 0) {
            if (!isIntegerExp) throw MathEvalException(MathError.UNDEFINED)
            val isEven = strippedExp.remainder(BD_TWO).compareTo(BigDecimal.ZERO) == 0
            return if (isEven) BigDecimal.ONE else BD_NEG_ONE
        }

        if (isIntegerExp) {
            val expLong = try {
                strippedExp.longValueExact()
            } catch (_: ArithmeticException) {
                val absCmp = base.abs().compareTo(BigDecimal.ONE)
                val positiveExp = strippedExp.signum() > 0
                if ((absCmp > 0 && positiveExp) || (absCmp < 0 && !positiveExp)) {
                    throw MathEvalException(MathError.OVERFLOW)
                } else {
                    throw MathEvalException(MathError.UNDERFLOW)
                }
            }
            if (abs(expLong) <= 1024L && abs(base.toDouble()) < 1e150) {
                try {
                    val res = base.pow(expLong.toInt(), mc)
                    checkMagnitude(res)
                    return res
                } catch (_: ArithmeticException) {
                    // Fall through to double evaluation / overflow check
                }
            }
        }

        val bDouble = base.toDouble()
        val eDouble = exponent.toDouble()
        if (bDouble < 0 && !isIntegerExp) {
            throw MathEvalException(MathError.UNDEFINED)
        }

        val result = bDouble.pow(eDouble)
        return validateDoubleResult(result, nonZeroExpected = true)
    }

    private fun evalFactorial(value: BigDecimal): BigDecimal {
        val stripped = value.stripTrailingZeros()
        if (stripped.compareTo(BigDecimal.ZERO) < 0 || stripped.scale() > 0) {
            throw MathEvalException(MathError.UNDEFINED)
        }
        val n = try {
            stripped.intValueExact()
        } catch (_: ArithmeticException) {
            throw MathEvalException(MathError.OVERFLOW)
        }
        if (n > 170) {
            throw MathEvalException(MathError.OVERFLOW)
        }
        var acc = BigDecimal.ONE
        for (i in 2..n) {
            acc = acc.multiply(BigDecimal(i), mc)
        }
        return acc
    }

    private fun evalFunction(name: String, arg: BigDecimal, angleMode: AngleMode): BigDecimal {
        return when (name) {
            "abs" -> arg.abs()
            "sqrt" -> {
                if (arg.compareTo(BigDecimal.ZERO) < 0) {
                    throw MathEvalException(MathError.UNDEFINED)
                }
                bigSqrt(arg)
            }
            "log" -> {
                if (arg.compareTo(BigDecimal.ZERO) <= 0) {
                    throw MathEvalException(MathError.UNDEFINED)
                }
                val stripped = arg.stripTrailingZeros()
                if (stripped.unscaledValue() == BigInteger.ONE) {
                    return BigDecimal(-stripped.scale())
                }
                val k = stripped.precision() - 1 - stripped.scale()
                val mantissa = stripped.movePointLeft(k).toDouble()
                val res = BigDecimal.valueOf(log10(mantissa)).add(BigDecimal(k), mc)
                checkMagnitude(res)
                res.round(DOUBLE_RESULT_MC).stripTrailingZeros()
            }
            "ln" -> {
                if (arg.compareTo(BigDecimal.ZERO) <= 0) {
                    throw MathEvalException(MathError.UNDEFINED)
                }
                if (arg.compareTo(BigDecimal.ONE) == 0) {
                    return BigDecimal.ZERO
                }
                if (arg.subtract(eDecimal).abs() < TRIG_ZERO_TOLERANCE) {
                    return BigDecimal.ONE
                }
                val stripped = arg.stripTrailingZeros()
                val k = stripped.precision() - 1 - stripped.scale()
                val mantissa = stripped.movePointLeft(k).toDouble()
                val res = if (k == 0) {
                    BigDecimal.valueOf(ln(mantissa))
                } else {
                    BigDecimal.valueOf(ln(mantissa)).add(LN_10_HIGH.multiply(BigDecimal(k), mc), mc)
                }
                checkMagnitude(res)
                res.round(DOUBLE_RESULT_MC).stripTrailingZeros()
            }
            "sin" -> evalSin(arg, angleMode)
            "cos" -> evalCos(arg, angleMode)
            "tan" -> evalTan(arg, angleMode)
            "asin" -> {
                val d = arg.toDouble()
                if (d < -1.0 || d > 1.0) throw MathEvalException(MathError.UNDEFINED)
                if (angleMode == AngleMode.RAD && arg.abs() < SMALL_ANGLE_CUTOFF) {
                    checkMagnitude(arg)
                    return arg
                }
                val rad = asin(d)
                val out = if (angleMode == AngleMode.DEG) rad * DEG_TO_RAD else rad
                validateDoubleResult(out, nonZeroExpected = false)
            }
            "acos" -> {
                val d = arg.toDouble()
                if (d < -1.0 || d > 1.0) throw MathEvalException(MathError.UNDEFINED)
                val rad = acos(d)
                val out = if (angleMode == AngleMode.DEG) rad * DEG_TO_RAD else rad
                validateDoubleResult(out, nonZeroExpected = false)
            }
            "atan" -> {
                if (angleMode == AngleMode.RAD && arg.abs() < SMALL_ANGLE_CUTOFF) {
                    checkMagnitude(arg)
                    return arg
                }
                val d = arg.toDouble()
                val rad = atan(d)
                val out = if (angleMode == AngleMode.DEG) rad * DEG_TO_RAD else rad
                validateDoubleResult(out, nonZeroExpected = false)
            }
            "sinh" -> {
                if (arg.abs() < SMALL_ANGLE_CUTOFF) {
                    checkMagnitude(arg)
                    return arg
                }
                validateDoubleResult(sinh(arg.toDouble()), nonZeroExpected = false)
            }
            "cosh" -> validateDoubleResult(cosh(arg.toDouble()), nonZeroExpected = true)
            "tanh" -> {
                if (arg.abs() < SMALL_ANGLE_CUTOFF) {
                    checkMagnitude(arg)
                    return arg
                }
                validateDoubleResult(tanh(arg.toDouble()), nonZeroExpected = false)
            }
            else -> throw MathEvalException(MathError.INVALID_EXPRESSION)
        }
    }

    private fun normalizeDegrees(arg: BigDecimal): Double {
        val rem = arg.remainder(BD_360).toDouble()
        return ((rem % 360.0) + 360.0) % 360.0
    }

    private fun reduceRadiansMod2Pi(arg: BigDecimal): BigDecimal {
        if (arg.abs() > RAD_MAX_ANGLE) {
            throw MathEvalException(MathError.UNDEFINED)
        }
        val mod = arg.remainder(TWO_PI_HIGH)
        return if (mod.signum() < 0) mod.add(TWO_PI_HIGH) else mod
    }

    private fun evalSin(arg: BigDecimal, angleMode: AngleMode): BigDecimal {
        if (arg.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO
        if (angleMode == AngleMode.DEG) {
            val normalized = normalizeDegrees(arg)
            when {
                isClose(normalized, 0.0) || isClose(normalized, 180.0) || isClose(normalized, 360.0) -> return BigDecimal.ZERO
                isClose(normalized, 30.0) || isClose(normalized, 150.0) -> return BD_HALF
                isClose(normalized, 210.0) || isClose(normalized, 330.0) -> return BD_NEG_HALF
                isClose(normalized, 90.0) -> return BigDecimal.ONE
                isClose(normalized, 270.0) -> return BD_NEG_ONE
            }
            return validateDoubleResult(sin(normalized * PI / 180.0), nonZeroExpected = false)
        }
        val absArg = arg.abs()
        if (absArg < SMALL_ANGLE_CUTOFF) {
            checkMagnitude(arg)
            return arg
        }
        if (absArg < BigDecimal.ONE) {
            return validateDoubleResult(sin(arg.toDouble()), nonZeroExpected = false)
        }
        val mod = reduceRadiansMod2Pi(arg)
        return when {
            mod < TRIG_ZERO_TOLERANCE ||
                mod.subtract(PI_HIGH).abs() < TRIG_ZERO_TOLERANCE ||
                TWO_PI_HIGH.subtract(mod).abs() < TRIG_ZERO_TOLERANCE -> BigDecimal.ZERO
            mod.subtract(HALF_PI_HIGH).abs() < TRIG_ZERO_TOLERANCE -> BigDecimal.ONE
            mod.subtract(THREE_HALF_PI_HIGH).abs() < TRIG_ZERO_TOLERANCE -> BD_NEG_ONE
            else -> validateDoubleResult(sin(mod.toDouble()), nonZeroExpected = false)
        }
    }

    private fun evalCos(arg: BigDecimal, angleMode: AngleMode): BigDecimal {
        if (arg.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ONE
        if (angleMode == AngleMode.DEG) {
            val normalized = normalizeDegrees(arg)
            when {
                isClose(normalized, 90.0) || isClose(normalized, 270.0) -> return BigDecimal.ZERO
                isClose(normalized, 60.0) || isClose(normalized, 300.0) -> return BD_HALF
                isClose(normalized, 120.0) || isClose(normalized, 240.0) -> return BD_NEG_HALF
                isClose(normalized, 0.0) || isClose(normalized, 360.0) -> return BigDecimal.ONE
                isClose(normalized, 180.0) -> return BD_NEG_ONE
            }
            return validateDoubleResult(cos(normalized * PI / 180.0), nonZeroExpected = false)
        }
        val absArg = arg.abs()
        if (absArg < SMALL_ANGLE_CUTOFF) {
            return BigDecimal.ONE
        }
        if (absArg < BigDecimal.ONE) {
            return validateDoubleResult(cos(arg.toDouble()), nonZeroExpected = false)
        }
        val mod = reduceRadiansMod2Pi(arg)
        return when {
            mod < TRIG_ZERO_TOLERANCE ||
                TWO_PI_HIGH.subtract(mod).abs() < TRIG_ZERO_TOLERANCE -> BigDecimal.ONE
            mod.subtract(PI_HIGH).abs() < TRIG_ZERO_TOLERANCE -> BD_NEG_ONE
            mod.subtract(HALF_PI_HIGH).abs() < TRIG_ZERO_TOLERANCE ||
                mod.subtract(THREE_HALF_PI_HIGH).abs() < TRIG_ZERO_TOLERANCE -> BigDecimal.ZERO
            else -> validateDoubleResult(cos(mod.toDouble()), nonZeroExpected = false)
        }
    }

    private fun evalTan(arg: BigDecimal, angleMode: AngleMode): BigDecimal {
        if (arg.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO
        if (angleMode == AngleMode.DEG) {
            val normalized = normalizeDegrees(arg)
            if (isClose(normalized, 90.0) || isClose(normalized, 270.0)) {
                throw MathEvalException(MathError.UNDEFINED)
            }
            if (isClose(normalized, 0.0) || isClose(normalized, 180.0) || isClose(normalized, 360.0)) {
                return BigDecimal.ZERO
            }
            if (isClose(normalized, 45.0) || isClose(normalized, 225.0)) {
                return BigDecimal.ONE
            }
            if (isClose(normalized, 135.0) || isClose(normalized, 315.0)) {
                return BD_NEG_ONE
            }
            return validateDoubleResult(tan(normalized * PI / 180.0), nonZeroExpected = false)
        }
        val absArg = arg.abs()
        if (absArg < SMALL_ANGLE_CUTOFF) {
            checkMagnitude(arg)
            return arg
        }
        if (absArg < BigDecimal.ONE) {
            return validateDoubleResult(tan(arg.toDouble()), nonZeroExpected = false)
        }
        val mod = reduceRadiansMod2Pi(arg)
        if (mod.subtract(HALF_PI_HIGH).abs() < TRIG_ZERO_TOLERANCE ||
            mod.subtract(THREE_HALF_PI_HIGH).abs() < TRIG_ZERO_TOLERANCE
        ) {
            throw MathEvalException(MathError.UNDEFINED)
        }
        if (mod < TRIG_ZERO_TOLERANCE ||
            mod.subtract(PI_HIGH).abs() < TRIG_ZERO_TOLERANCE ||
            TWO_PI_HIGH.subtract(mod).abs() < TRIG_ZERO_TOLERANCE
        ) {
            return BigDecimal.ZERO
        }
        return validateDoubleResult(tan(mod.toDouble()), nonZeroExpected = false)
    }

    private fun isClose(a: Double, b: Double): Boolean = abs(a - b) < 1e-12

    private fun validateDoubleResult(result: Double, nonZeroExpected: Boolean): BigDecimal {
        if (result.isNaN()) throw MathEvalException(MathError.UNDEFINED)
        if (result.isInfinite()) throw MathEvalException(MathError.OVERFLOW)
        if (nonZeroExpected && result == 0.0) throw MathEvalException(MathError.UNDERFLOW)
        if (result == 0.0) return BigDecimal.ZERO
        val bd = BigDecimal.valueOf(result)
        checkMagnitude(bd)
        return bd.round(DOUBLE_RESULT_MC).stripTrailingZeros()
    }

    private fun checkMagnitude(value: BigDecimal) {
        val absVal = value.abs()
        if (absVal > MAX_MAGNITUDE) {
            throw MathEvalException(MathError.OVERFLOW)
        }
        if (value.compareTo(BigDecimal.ZERO) != 0 && absVal < MIN_MAGNITUDE) {
            throw MathEvalException(MathError.UNDERFLOW)
        }
    }

    private fun cleanPrecision(value: BigDecimal): BigDecimal {
        if (value.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO
        return value.round(CLEAN_RESULT_MC).stripTrailingZeros()
    }

    /**
     * Newton-Raphson BigDecimal square root compatible with API 24+ (BigDecimal.sqrt requires API 33).
     */
    private fun bigSqrt(value: BigDecimal): BigDecimal {
        if (value.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO
        val initial = kotlin.math.sqrt(value.toDouble())
        var x = if (initial.isFinite() && initial > 0.0) {
            BigDecimal.valueOf(initial)
        } else {
            value.divide(BD_TWO, mc)
        }
        var iter = 0
        while (iter < 12) {
            val next = x.add(value.divide(x, mc), mc).divide(BD_TWO, mc)
            if (next.compareTo(x) == 0) {
                return next.stripTrailingZeros()
            }
            x = next
            iter++
        }
        return x.stripTrailingZeros()
    }
}
