package com.example.calculator.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.calculator.engine.AngleMode
import com.example.ui.theme.LocalLuxaCalculatorColors

@Composable
fun ScientificKeypad(
    expanded: Boolean,
    angleMode: AngleMode,
    onFunction: (String) -> Unit,
    onSquare: () -> Unit,
    onPower: () -> Unit,
    onTenPowerX: () -> Unit,
    onEPowerX: () -> Unit,
    onPi: () -> Unit,
    onEuler: () -> Unit,
    onFactorial: () -> Unit,
    onReciprocal: () -> Unit,
    onOpenParen: () -> Unit,
    onCloseParen: () -> Unit,
    onToggleAngleMode: () -> Unit,
    feedbackController: CalculatorFeedbackController,
    modifier: Modifier = Modifier,
) {
    val calcColors = LocalLuxaCalculatorColors.current
    val bg = calcColors.scientificKeyBackground
    val fg = calcColors.scientificKeyContent

    AnimatedVisibility(
        visible = expanded,
        enter = expandVertically(animationSpec = tween(220)) + fadeIn(animationSpec = tween(180)),
        exit = shrinkVertically(animationSpec = tween(200)) + fadeOut(animationSpec = tween(150)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Row 1: sin, cos, tan, log, ln, √
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                ScientificKeyButton("sin", bg, fg, { onFunction("sin(") }, Modifier.weight(1f), "sine", testTag = "btn_sin", feedbackController = feedbackController)
                ScientificKeyButton("cos", bg, fg, { onFunction("cos(") }, Modifier.weight(1f), "cosine", testTag = "btn_cos", feedbackController = feedbackController)
                ScientificKeyButton("tan", bg, fg, { onFunction("tan(") }, Modifier.weight(1f), "tangent", testTag = "btn_tan", feedbackController = feedbackController)
                ScientificKeyButton("log", bg, fg, { onFunction("log(") }, Modifier.weight(1f), "logarithm base 10", testTag = "btn_log", feedbackController = feedbackController)
                ScientificKeyButton("ln", bg, fg, { onFunction("ln(") }, Modifier.weight(1f), "natural logarithm", testTag = "btn_ln", feedbackController = feedbackController)
                ScientificKeyButton("√", bg, fg, { onFunction("√(") }, Modifier.weight(1f), "square root", testTag = "btn_sqrt", feedbackController = feedbackController)
            }

            // Row 2: asin, acos, atan, 10ˣ, eˣ, x²
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                ScientificKeyButton("asin", bg, fg, { onFunction("asin(") }, Modifier.weight(1f), "inverse sine", testTag = "btn_asin", feedbackController = feedbackController)
                ScientificKeyButton("acos", bg, fg, { onFunction("acos(") }, Modifier.weight(1f), "inverse cosine", testTag = "btn_acos", feedbackController = feedbackController)
                ScientificKeyButton("atan", bg, fg, { onFunction("atan(") }, Modifier.weight(1f), "inverse tangent", testTag = "btn_atan", feedbackController = feedbackController)
                ScientificKeyButton("10ˣ", bg, fg, onTenPowerX, Modifier.weight(1f), "10 to the power of x", testTag = "btn_ten_pow", feedbackController = feedbackController)
                ScientificKeyButton("eˣ", bg, fg, onEPowerX, Modifier.weight(1f), "e to the power of x", testTag = "btn_e_pow", feedbackController = feedbackController)
                ScientificKeyButton("x²", bg, fg, onSquare, Modifier.weight(1f), "square", testTag = "btn_square", feedbackController = feedbackController)
            }

            // Row 3: sinh, cosh, tanh, xʸ, 1/x, |x|
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                ScientificKeyButton("sinh", bg, fg, { onFunction("sinh(") }, Modifier.weight(1f), "hyperbolic sine", testTag = "btn_sinh", feedbackController = feedbackController)
                ScientificKeyButton("cosh", bg, fg, { onFunction("cosh(") }, Modifier.weight(1f), "hyperbolic cosine", testTag = "btn_cosh", feedbackController = feedbackController)
                ScientificKeyButton("tanh", bg, fg, { onFunction("tanh(") }, Modifier.weight(1f), "hyperbolic tangent", testTag = "btn_tanh", feedbackController = feedbackController)
                ScientificKeyButton("xʸ", bg, fg, onPower, Modifier.weight(1f), "power", testTag = "btn_power", feedbackController = feedbackController)
                ScientificKeyButton("1/x", bg, fg, onReciprocal, Modifier.weight(1f), "reciprocal", testTag = "btn_reciprocal", feedbackController = feedbackController)
                ScientificKeyButton("|x|", bg, fg, { onFunction("abs(") }, Modifier.weight(1f), "absolute value", testTag = "btn_abs", feedbackController = feedbackController)
            }

            // Row 4: π, e, x!, (, ), DEG/RAD
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                ScientificKeyButton("π", bg, fg, onPi, Modifier.weight(1f), "pi", testTag = "btn_pi", feedbackController = feedbackController)
                ScientificKeyButton("e", bg, fg, onEuler, Modifier.weight(1f), "euler number e", testTag = "btn_e", feedbackController = feedbackController)
                ScientificKeyButton("x!", bg, fg, onFactorial, Modifier.weight(1f), "factorial", testTag = "btn_factorial", feedbackController = feedbackController)
                ScientificKeyButton("(", bg, fg, onOpenParen, Modifier.weight(1f), "open parenthesis", testTag = "btn_open_paren", feedbackController = feedbackController)
                ScientificKeyButton(")", bg, fg, onCloseParen, Modifier.weight(1f), "close parenthesis", testTag = "btn_close_paren", feedbackController = feedbackController)
                ScientificKeyButton(
                    label = if (angleMode == AngleMode.DEG) "DEG" else "RAD",
                    backgroundColor = calcColors.operatorKeyBackground,
                    contentColor = calcColors.operatorKeyContent,
                    onClick = onToggleAngleMode,
                    modifier = Modifier.weight(1f),
                    contentDesc = "switch angle mode",
                    isHighlighted = true,
                    testTag = "btn_sci_angle",
                    feedbackController = feedbackController
                )
            }
        }
    }
}
