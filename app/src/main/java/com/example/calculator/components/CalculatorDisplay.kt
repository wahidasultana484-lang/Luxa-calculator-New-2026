package com.example.calculator.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.calculator.engine.AngleMode
import com.example.calculator.engine.MathError
import com.example.ui.theme.LocalLuxaCalculatorColors

@Composable
fun CalculatorDisplay(
    expression: String,
    resultText: String,
    justEvaluated: Boolean,
    error: MathError?,
    angleMode: AngleMode,
    hasMemory: Boolean,
    isCompactHeight: Boolean,
    onToggleAngleMode: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onClearHistory: () -> Unit,
    onShowAbout: () -> Unit,
    onCopyResult: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val calcColors = LocalLuxaCalculatorColors.current
    val exprScrollState = rememberScrollState()
    val resultScrollState = rememberScrollState()
    var menuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(expression) {
        exprScrollState.scrollTo(exprScrollState.maxValue)
    }

    LaunchedEffect(resultText) {
        resultScrollState.scrollTo(0)
    }

    val errorMessage = when (error) {
        MathError.DIVISION_BY_ZERO -> stringResource(R.string.error_divide_by_zero)
        MathError.INVALID_EXPRESSION -> stringResource(R.string.error_invalid_expression)
        MathError.MISSING_PARENTHESIS -> stringResource(R.string.error_missing_parenthesis)
        MathError.OVERFLOW -> stringResource(R.string.error_overflow)
        MathError.UNDERFLOW -> stringResource(R.string.error_underflow)
        MathError.UNDEFINED -> stringResource(R.string.error_undefined)
        null -> null
    }

    val exprFontSize = when {
        justEvaluated -> if (isCompactHeight) 20.sp else 26.sp
        expression.length <= 9 -> if (isCompactHeight) 32.sp else 52.sp
        expression.length <= 14 -> if (isCompactHeight) 26.sp else 40.sp
        expression.length <= 20 -> if (isCompactHeight) 22.sp else 32.sp
        else -> if (isCompactHeight) 20.sp else 26.sp
    }

    val resultFontSize = when {
        errorMessage != null -> if (isCompactHeight) 22.sp else 28.sp
        justEvaluated -> when {
            resultText.length <= 10 -> if (isCompactHeight) 32.sp else 54.sp
            resultText.length <= 15 -> if (isCompactHeight) 28.sp else 40.sp
            else -> if (isCompactHeight) 22.sp else 30.sp
        }
        else -> if (isCompactHeight) 20.sp else 28.sp
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("calculator_display_panel"),
        color = calcColors.displayBackground,
        shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = if (isCompactHeight) 2.dp else 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        onClick = onToggleAngleMode,
                        modifier = Modifier.testTag("angle_mode_toggle"),
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ) {
                        Text(
                            text = if (angleMode == AngleMode.DEG) {
                                stringResource(R.string.angle_mode_deg)
                            } else {
                                stringResource(R.string.angle_mode_rad)
                            },
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    if (hasMemory) {
                        Surface(
                            modifier = Modifier.testTag("memory_indicator"),
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Text(
                                text = stringResource(R.string.memory_active_badge),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onOpenHistory,
                        modifier = Modifier.testTag("history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.History,
                            contentDescription = stringResource(R.string.cd_history_button),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.testTag("overflow_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.cd_more_options),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_history)) },
                                leadingIcon = {
                                    Icon(Icons.Outlined.History, contentDescription = null)
                                },
                                modifier = Modifier.testTag("menu_history"),
                                onClick = {
                                    menuExpanded = false
                                    onOpenHistory()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_settings)) },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Settings, contentDescription = null)
                                },
                                modifier = Modifier.testTag("menu_settings"),
                                onClick = {
                                    menuExpanded = false
                                    onOpenSettings()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_clear_history)) },
                                leadingIcon = {
                                    Icon(Icons.Outlined.DeleteSweep, contentDescription = null)
                                },
                                modifier = Modifier.testTag("menu_clear_history"),
                                onClick = {
                                    menuExpanded = false
                                    onClearHistory()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_about)) },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Info, contentDescription = null)
                                },
                                modifier = Modifier.testTag("menu_about"),
                                onClick = {
                                    menuExpanded = false
                                    onShowAbout()
                                }
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = if (isCompactHeight) 4.dp else 8.dp),
                horizontalAlignment = Alignment.End
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                        .horizontalScroll(exprScrollState),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Text(
                        text = expression.ifEmpty { "0" },
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = exprFontSize,
                            lineHeight = exprFontSize * 1.15f
                        ),
                        color = when {
                            expression.isEmpty() -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            justEvaluated -> MaterialTheme.colorScheme.onSurfaceVariant
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 1,
                        softWrap = false,
                        textAlign = TextAlign.End,
                        modifier = Modifier.testTag("calculator_expression_text")
                    )
                }

                Spacer(modifier = Modifier.height(if (isCompactHeight) 2.dp else 4.dp))

                val displayedBottomText = errorMessage ?: resultText
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                        .height(if (isCompactHeight) 36.dp else 54.dp)
                        .horizontalScroll(resultScrollState)
                        .then(
                            if (resultText.isNotEmpty() && errorMessage == null) {
                                Modifier.clickable { onCopyResult(resultText) }
                            } else {
                                Modifier
                            }
                        ),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Text(
                        text = displayedBottomText,
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontSize = resultFontSize,
                            lineHeight = resultFontSize * 1.15f,
                            fontWeight = if (justEvaluated) FontWeight.Medium else FontWeight.Normal
                        ),
                        color = when {
                            errorMessage != null -> MaterialTheme.colorScheme.error
                            justEvaluated -> MaterialTheme.colorScheme.onSurface
                            else -> calcColors.resultPreviewText
                        },
                        maxLines = 1,
                        softWrap = false,
                        textAlign = TextAlign.End,
                        modifier = Modifier.testTag("calculator_result_text")
                    )
                }
            }
        }
    }
}
