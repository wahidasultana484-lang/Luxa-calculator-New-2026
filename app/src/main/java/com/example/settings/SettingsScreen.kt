package com.example.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Brightness6
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Functions
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.calculator.engine.AngleMode
import com.example.calculator.engine.NumberFormatStyle
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onOpenHistory: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = uiState.settings
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showThemeDialog by remember { mutableStateOf(false) }
    var showAngleDialog by remember { mutableStateOf(false) }
    var showFormatDialog by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    val clearedMsg = stringResource(R.string.history_cleared_snackbar)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back_button)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 560.dp)
                    .testTag("settings_list"),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                // A. Appearance
                item {
                    SettingsSectionHeader(stringResource(R.string.settings_section_appearance))
                }
                item {
                    val themeLabel = when (settings.themeMode) {
                        ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
                        ThemeMode.LIGHT -> stringResource(R.string.theme_light)
                        ThemeMode.DARK -> stringResource(R.string.theme_dark)
                    }
                    SettingsActionRow(
                        icon = Icons.Outlined.Brightness6,
                        title = stringResource(R.string.settings_theme),
                        subtitle = themeLabel,
                        onClick = { showThemeDialog = true },
                        testTag = "settings_theme_row"
                    )
                }

                item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

                // B. Calculator
                item {
                    SettingsSectionHeader(stringResource(R.string.settings_section_calculator))
                }
                item {
                    SettingsSwitchRow(
                        icon = Icons.Outlined.Functions,
                        title = stringResource(R.string.settings_scientific_default),
                        subtitle = stringResource(R.string.settings_scientific_default_summary),
                        checked = settings.scientificExpandedByDefault,
                        onCheckedChange = viewModel::setScientificExpandedByDefault,
                        testTag = "settings_scientific_default_switch"
                    )
                }
                item {
                    val angleLabel = when (settings.defaultAngleMode) {
                        AngleMode.DEG -> stringResource(R.string.angle_degree)
                        AngleMode.RAD -> stringResource(R.string.angle_radian)
                    }
                    SettingsActionRow(
                        icon = Icons.Outlined.Straighten,
                        title = stringResource(R.string.settings_angle_mode),
                        subtitle = angleLabel,
                        onClick = { showAngleDialog = true },
                        testTag = "settings_angle_row"
                    )
                }
                item {
                    val formatLabel = when (settings.numberFormatStyle) {
                        NumberFormatStyle.INTERNATIONAL -> stringResource(R.string.format_international)
                        NumberFormatStyle.INDIAN -> stringResource(R.string.format_indian)
                        NumberFormatStyle.PLAIN -> stringResource(R.string.format_plain)
                    }
                    SettingsActionRow(
                        icon = Icons.Outlined.Numbers,
                        title = stringResource(R.string.settings_number_format),
                        subtitle = formatLabel,
                        onClick = { showFormatDialog = true },
                        testTag = "settings_number_format_row"
                    )
                }
                item {
                    SettingsSwitchRow(
                        icon = Icons.Outlined.PhoneAndroid,
                        title = stringResource(R.string.settings_keep_screen_on),
                        subtitle = stringResource(R.string.settings_keep_screen_on_summary),
                        checked = settings.keepScreenOn,
                        onCheckedChange = viewModel::setKeepScreenOn,
                        testTag = "settings_keep_screen_on_switch"
                    )
                }

                item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

                // C. Feedback
                item {
                    SettingsSectionHeader(stringResource(R.string.settings_section_feedback))
                }
                item {
                    SettingsSwitchRow(
                        icon = Icons.Outlined.VolumeUp,
                        title = stringResource(R.string.settings_button_sound),
                        subtitle = stringResource(R.string.settings_button_sound_summary),
                        checked = settings.buttonSoundEnabled,
                        onCheckedChange = viewModel::setButtonSoundEnabled,
                        testTag = "settings_button_sound_switch"
                    )
                }
                item {
                    SettingsSwitchRow(
                        icon = Icons.Outlined.Vibration,
                        title = stringResource(R.string.settings_haptic_feedback),
                        subtitle = stringResource(R.string.settings_haptic_feedback_summary),
                        checked = settings.hapticFeedbackEnabled,
                        onCheckedChange = viewModel::setHapticFeedbackEnabled,
                        testTag = "settings_haptic_switch"
                    )
                }

                item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

                // D. History
                item {
                    SettingsSectionHeader(stringResource(R.string.settings_section_history))
                }
                item {
                    SettingsActionRow(
                        icon = Icons.Outlined.History,
                        title = stringResource(R.string.settings_manage_history),
                        subtitle = stringResource(R.string.settings_history_count_format, uiState.historyCount),
                        onClick = onOpenHistory,
                        testTag = "settings_manage_history_row"
                    )
                }
                item {
                    SettingsActionRow(
                        icon = Icons.Outlined.DeleteSweep,
                        title = stringResource(R.string.settings_clear_history),
                        subtitle = stringResource(R.string.settings_clear_history_summary),
                        onClick = { showClearHistoryDialog = true },
                        testTag = "settings_clear_history_row"
                    )
                }

                item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

                // E. General
                item {
                    SettingsSectionHeader(stringResource(R.string.settings_section_general))
                }
                item {
                    SettingsActionRow(
                        icon = Icons.Outlined.PrivacyTip,
                        title = stringResource(R.string.settings_privacy),
                        subtitle = stringResource(R.string.settings_privacy_summary),
                        onClick = { showPrivacyDialog = true },
                        testTag = "settings_privacy_row"
                    )
                }
                item {
                    SettingsActionRow(
                        icon = Icons.Outlined.Info,
                        title = stringResource(R.string.settings_about),
                        subtitle = "${stringResource(R.string.app_name)} • ${stringResource(R.string.app_version_label)}",
                        onClick = { showAboutDialog = true },
                        testTag = "settings_about_row"
                    )
                }
            }
        }
    }

    if (showThemeDialog) {
        SingleChoiceDialog(
            title = stringResource(R.string.settings_theme),
            options = listOf(
                ThemeMode.SYSTEM to stringResource(R.string.theme_system),
                ThemeMode.LIGHT to stringResource(R.string.theme_light),
                ThemeMode.DARK to stringResource(R.string.theme_dark)
            ),
            selected = settings.themeMode,
            onSelect = {
                viewModel.setThemeMode(it)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }

    if (showAngleDialog) {
        SingleChoiceDialog(
            title = stringResource(R.string.settings_angle_mode),
            options = listOf(
                AngleMode.DEG to stringResource(R.string.angle_degree),
                AngleMode.RAD to stringResource(R.string.angle_radian)
            ),
            selected = settings.defaultAngleMode,
            onSelect = {
                viewModel.setDefaultAngleMode(it)
                showAngleDialog = false
            },
            onDismiss = { showAngleDialog = false }
        )
    }

    if (showFormatDialog) {
        SingleChoiceDialog(
            title = stringResource(R.string.settings_number_format),
            options = listOf(
                NumberFormatStyle.INTERNATIONAL to stringResource(R.string.format_international),
                NumberFormatStyle.INDIAN to stringResource(R.string.format_indian),
                NumberFormatStyle.PLAIN to stringResource(R.string.format_plain)
            ),
            selected = settings.numberFormatStyle,
            onSelect = {
                viewModel.setNumberFormatStyle(it)
                showFormatDialog = false
            },
            onDismiss = { showFormatDialog = false }
        )
    }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text(stringResource(R.string.history_clear_dialog_title)) },
            text = { Text(stringResource(R.string.history_clear_dialog_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearHistory()
                        showClearHistoryDialog = false
                        scope.launch { snackbarHostState.showSnackbar(clearedMsg) }
                    },
                    modifier = Modifier.testTag("confirm_clear_history_button")
                ) {
                    Text(stringResource(R.string.action_clear))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text(stringResource(R.string.settings_privacy_dialog_title)) },
            text = { Text(stringResource(R.string.settings_privacy_dialog_body)) },
            confirmButton = {
                TextButton(
                    onClick = { showPrivacyDialog = false },
                    modifier = Modifier.testTag("privacy_dialog_ok_button")
                ) {
                    Text(stringResource(R.string.action_ok))
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

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
    )
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun <T> SingleChoiceDialog(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                options.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_option_$value")
                            .clickable { onSelect(value) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = value == selected,
                            onClick = { onSelect(value) }
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
