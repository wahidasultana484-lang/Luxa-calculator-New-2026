package com.example.calculator.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.LocalLuxaCalculatorColors

@Composable
fun MemoryUtilityBar(
    hasMemory: Boolean,
    isScientificExpanded: Boolean,
    onMemoryClear: () -> Unit,
    onMemoryRecall: () -> Unit,
    onMemoryAdd: () -> Unit,
    onMemorySubtract: () -> Unit,
    onToggleScientific: () -> Unit,
    feedbackController: CalculatorFeedbackController,
    modifier: Modifier = Modifier,
) {
    val calcColors = LocalLuxaCalculatorColors.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MemoryPillButton(
                label = "MC",
                backgroundColor = calcColors.memoryKeyBackground,
                contentColor = calcColors.memoryKeyContent,
                enabled = hasMemory,
                onClick = onMemoryClear,
                contentDesc = "Memory clear",
                testTag = "btn_mc",
                feedbackController = feedbackController
            )
            MemoryPillButton(
                label = "MR",
                backgroundColor = calcColors.memoryKeyBackground,
                contentColor = calcColors.memoryKeyContent,
                enabled = hasMemory,
                onClick = onMemoryRecall,
                contentDesc = "Memory recall",
                testTag = "btn_mr",
                feedbackController = feedbackController
            )
            MemoryPillButton(
                label = "M+",
                backgroundColor = calcColors.memoryKeyBackground,
                contentColor = calcColors.memoryKeyContent,
                enabled = true,
                onClick = onMemoryAdd,
                contentDesc = "Memory add",
                testTag = "btn_m_plus",
                feedbackController = feedbackController
            )
            MemoryPillButton(
                label = "M−",
                backgroundColor = calcColors.memoryKeyBackground,
                contentColor = calcColors.memoryKeyContent,
                enabled = true,
                onClick = onMemorySubtract,
                contentDesc = "Memory subtract",
                testTag = "btn_m_minus",
                feedbackController = feedbackController
            )
        }

        Surface(
            onClick = {
                feedbackController.performKeyFeedback()
                onToggleScientific()
            },
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("toggle_scientific_button"),
            shape = CircleShape,
            color = if (isScientificExpanded) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                calcColors.memoryKeyBackground
            },
            contentColor = if (isScientificExpanded) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                calcColors.memoryKeyContent
            }
        ) {
            Icon(
                imageVector = if (isScientificExpanded) {
                    Icons.Rounded.KeyboardArrowUp
                } else {
                    Icons.Rounded.KeyboardArrowDown
                },
                contentDescription = stringResource(R.string.cd_toggle_scientific),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}
