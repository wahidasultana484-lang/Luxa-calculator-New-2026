package com.example.calculator.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.calculator.CalculatorViewModel
import com.example.calculator.components.CalculatorDisplay
import com.example.calculator.components.CalculatorKeyButton
import com.example.calculator.components.MemoryUtilityBar
import com.example.calculator.components.ScientificKeypad
import com.example.calculator.components.rememberCalculatorFeedback
import com.example.ui.theme.LocalLuxaCalculatorColors
import kotlinx.coroutines.launch

@Composable
fun CalculatorScreen(
    viewModel: CalculatorViewModel,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onClearHistory: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val calcColors = LocalLuxaCalculatorColors.current
    val feedback = rememberCalculatorFeedback(
        hapticEnabled = state.hapticFeedbackEnabled,
        soundEnabled = state.buttonSoundEnabled
    )
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    val copiedMsg = stringResource(R.string.copied_to_clipboard)
    val clearedMsg = stringResource(R.string.history_cleared_snackbar)

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { _ ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
            ) {
                val isCompactHeight = maxHeight < 700.dp || state.isScientificExpanded
                val keySpacing = if (state.isScientificExpanded && maxHeight < 720.dp) 8.dp else 10.dp
                val digitFontSize = if (state.isScientificExpanded) 24.sp else 28.sp
                val opFontSize = if (state.isScientificExpanded) 24.sp else 28.sp

                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    CalculatorDisplay(
                        expression = state.expression,
                        resultText = state.resultText,
                        justEvaluated = state.justEvaluated,
                        error = state.error,
                        angleMode = state.angleMode,
                        hasMemory = state.hasMemory,
                        isCompactHeight = isCompactHeight,
                        onToggleAngleMode = viewModel::toggleAngleMode,
                        onOpenHistory = onOpenHistory,
                        onOpenSettings = onOpenSettings,
                        onClearHistory = { showClearHistoryDialog = true },
                        onShowAbout = { showAboutDialog = true },
                        onCopyResult = { result ->
                            clipboardManager.setText(AnnotatedString(result))
                            scope.launch { snackbarHostState.showSnackbar(copiedMsg) }
                        },
                        modifier = Modifier
                            .weight(if (state.isScientificExpanded) 0.27f else 0.34f)
                            .fillMaxWidth()
                    )

                    MemoryUtilityBar(
                        hasMemory = state.hasMemory,
                        isScientificExpanded = state.isScientificExpanded,
                        onMemoryClear = viewModel::onMemoryClear,
                        onMemoryRecall = viewModel::onMemoryRecall,
                        onMemoryAdd = viewModel::onMemoryAdd,
                        onMemorySubtract = viewModel::onMemorySubtract,
                        onToggleScientific = viewModel::toggleScientificExpanded,
                        feedbackController = feedback,
                    )

                    ScientificKeypad(
                        expanded = state.isScientificExpanded,
                        angleMode = state.angleMode,
                        onFunction = viewModel::onFunction,
                        onSquare = viewModel::onSquare,
                        onPower = { viewModel.onOperator("^") },
                        onTenPowerX = viewModel::onTenPowerX,
                        onEPowerX = viewModel::onEPowerX,
                        onPi = { viewModel.onConstant("π") },
                        onEuler = { viewModel.onConstant("e") },
                        onFactorial = viewModel::onFactorial,
                        onReciprocal = viewModel::onReciprocal,
                        onOpenParen = viewModel::onOpenParenthesis,
                        onCloseParen = viewModel::onCloseParenthesis,
                        onToggleAngleMode = viewModel::toggleAngleMode,
                        feedbackController = feedback
                    )

                    // Main 4x5 Google Calculator Keypad
                    Column(
                        modifier = Modifier
                            .weight(if (state.isScientificExpanded) 0.48f else 0.60f)
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(keySpacing)
                    ) {
                        // Row 1: AC, ( ), %, ÷
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(keySpacing)
                        ) {
                            CalculatorKeyButton(
                                label = "AC",
                                backgroundColor = calcColors.clearKeyBackground,
                                contentColor = calcColors.clearKeyContent,
                                onClick = viewModel::onClear,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                contentDesc = "All clear",
                                fontSize = if (state.isScientificExpanded) 20.sp else 24.sp,
                                fontWeight = FontWeight.Medium,
                                testTag = "btn_ac",
                                feedbackController = feedback
                            )
                            CalculatorKeyButton(
                                label = "( )",
                                backgroundColor = calcColors.operatorKeyBackground,
                                contentColor = calcColors.operatorKeyContent,
                                onClick = viewModel::onSmartParenthesis,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                contentDesc = "Parentheses",
                                fontSize = if (state.isScientificExpanded) 20.sp else 24.sp,
                                testTag = "btn_parens",
                                feedbackController = feedback
                            )
                            CalculatorKeyButton(
                                label = "%",
                                backgroundColor = calcColors.operatorKeyBackground,
                                contentColor = calcColors.operatorKeyContent,
                                onClick = viewModel::onPercent,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                contentDesc = "Percent",
                                fontSize = opFontSize,
                                testTag = "btn_percent",
                                feedbackController = feedback
                            )
                            CalculatorKeyButton(
                                label = "÷",
                                backgroundColor = calcColors.operatorKeyBackground,
                                contentColor = calcColors.operatorKeyContent,
                                onClick = { viewModel.onOperator("÷") },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                contentDesc = "Divide",
                                fontSize = opFontSize,
                                testTag = "btn_divide",
                                feedbackController = feedback
                            )
                        }

                        // Row 2: 7, 8, 9, ×
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(keySpacing)
                        ) {
                            CalculatorKeyButton("7", calcColors.digitKeyBackground, calcColors.digitKeyContent, { viewModel.onDigit("7") }, Modifier.weight(1f).fillMaxHeight(), fontSize = digitFontSize, testTag = "btn_7", feedbackController = feedback)
                            CalculatorKeyButton("8", calcColors.digitKeyBackground, calcColors.digitKeyContent, { viewModel.onDigit("8") }, Modifier.weight(1f).fillMaxHeight(), fontSize = digitFontSize, testTag = "btn_8", feedbackController = feedback)
                            CalculatorKeyButton("9", calcColors.digitKeyBackground, calcColors.digitKeyContent, { viewModel.onDigit("9") }, Modifier.weight(1f).fillMaxHeight(), fontSize = digitFontSize, testTag = "btn_9", feedbackController = feedback)
                            CalculatorKeyButton("×", calcColors.operatorKeyBackground, calcColors.operatorKeyContent, { viewModel.onOperator("×") }, Modifier.weight(1f).fillMaxHeight(), contentDesc = "Multiply", fontSize = opFontSize, testTag = "btn_multiply", feedbackController = feedback)
                        }

                        // Row 3: 4, 5, 6, −
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(keySpacing)
                        ) {
                            CalculatorKeyButton("4", calcColors.digitKeyBackground, calcColors.digitKeyContent, { viewModel.onDigit("4") }, Modifier.weight(1f).fillMaxHeight(), fontSize = digitFontSize, testTag = "btn_4", feedbackController = feedback)
                            CalculatorKeyButton("5", calcColors.digitKeyBackground, calcColors.digitKeyContent, { viewModel.onDigit("5") }, Modifier.weight(1f).fillMaxHeight(), fontSize = digitFontSize, testTag = "btn_5", feedbackController = feedback)
                            CalculatorKeyButton("6", calcColors.digitKeyBackground, calcColors.digitKeyContent, { viewModel.onDigit("6") }, Modifier.weight(1f).fillMaxHeight(), fontSize = digitFontSize, testTag = "btn_6", feedbackController = feedback)
                            CalculatorKeyButton("−", calcColors.operatorKeyBackground, calcColors.operatorKeyContent, { viewModel.onOperator("−") }, Modifier.weight(1f).fillMaxHeight(), contentDesc = "Subtract", fontSize = opFontSize, testTag = "btn_subtract", feedbackController = feedback)
                        }

                        // Row 4: 1, 2, 3, +
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(keySpacing)
                        ) {
                            CalculatorKeyButton("1", calcColors.digitKeyBackground, calcColors.digitKeyContent, { viewModel.onDigit("1") }, Modifier.weight(1f).fillMaxHeight(), fontSize = digitFontSize, testTag = "btn_1", feedbackController = feedback)
                            CalculatorKeyButton("2", calcColors.digitKeyBackground, calcColors.digitKeyContent, { viewModel.onDigit("2") }, Modifier.weight(1f).fillMaxHeight(), fontSize = digitFontSize, testTag = "btn_2", feedbackController = feedback)
                            CalculatorKeyButton("3", calcColors.digitKeyBackground, calcColors.digitKeyContent, { viewModel.onDigit("3") }, Modifier.weight(1f).fillMaxHeight(), fontSize = digitFontSize, testTag = "btn_3", feedbackController = feedback)
                            CalculatorKeyButton("+", calcColors.operatorKeyBackground, calcColors.operatorKeyContent, { viewModel.onOperator("+") }, Modifier.weight(1f).fillMaxHeight(), contentDesc = "Add", fontSize = opFontSize, testTag = "btn_add", feedbackController = feedback)
                        }

                        // Row 5: 0, ., ⌫, =
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(keySpacing)
                        ) {
                            CalculatorKeyButton("0", calcColors.digitKeyBackground, calcColors.digitKeyContent, { viewModel.onDigit("0") }, Modifier.weight(1f).fillMaxHeight(), fontSize = digitFontSize, testTag = "btn_0", feedbackController = feedback)
                            CalculatorKeyButton(".", calcColors.digitKeyBackground, calcColors.digitKeyContent, viewModel::onDecimalPoint, Modifier.weight(1f).fillMaxHeight(), contentDesc = "Decimal point", fontSize = digitFontSize, testTag = "btn_dot", feedbackController = feedback)
                            CalculatorKeyButton(
                                label = "⌫",
                                backgroundColor = calcColors.digitKeyBackground,
                                contentColor = calcColors.digitKeyContent,
                                onClick = viewModel::onBackspace,
                                onLongClick = viewModel::onClear,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                icon = Icons.AutoMirrored.Outlined.Backspace,
                                contentDesc = stringResource(R.string.cd_backspace),
                                testTag = "btn_backspace",
                                feedbackController = feedback
                            )
                            CalculatorKeyButton(
                                label = "=",
                                backgroundColor = calcColors.equalsKeyBackground,
                                contentColor = calcColors.equalsKeyContent,
                                onClick = viewModel::onEquals,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                contentDesc = "Equals",
                                fontSize = opFontSize,
                                fontWeight = FontWeight.Medium,
                                testTag = "btn_equals",
                                feedbackController = feedback
                            )
                        }
                    }
                }
            }
        }
    }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text(stringResource(R.string.history_clear_dialog_title)) },
            text = { Text(stringResource(R.string.history_clear_dialog_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearHistoryDialog = false
                        onClearHistory()
                        scope.launch { snackbarHostState.showSnackbar(clearedMsg) }
                    },
                    modifier = Modifier.testTag("confirm_clear_history_button")
                ) {
                    Text(stringResource(R.string.action_clear))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearHistoryDialog = false },
                    modifier = Modifier.testTag("cancel_clear_history_button")
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text(stringResource(R.string.settings_about_dialog_title)) },
            text = { Text(stringResource(R.string.settings_about_dialog_body)) },
            confirmButton = {
                TextButton(
                    onClick = { showAboutDialog = false },
                    modifier = Modifier.testTag("about_dialog_ok_button")
                ) {
                    Text(stringResource(R.string.action_ok))
                }
            }
        )
    }
}

