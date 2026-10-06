package com.qoody.app.ui.settings

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qoody.app.BackupExport
import com.qoody.app.BuildConfig
import com.qoody.app.LedgerExport
import com.qoody.app.ProjectLinks
import com.qoody.app.R
import com.qoody.app.data.BackupService
import com.qoody.app.ui.components.HairlineDivider
import com.qoody.app.ui.components.LoadingIndicator
import com.qoody.app.ui.components.QoodyCard
import com.qoody.app.ui.components.QoodyIcon
import com.qoody.app.ui.components.QoodyInset
import com.qoody.app.ui.components.QoodySwitch
import com.qoody.app.ui.components.QoodyTextField
import com.qoody.app.ui.components.QoodyTopBar
import com.qoody.app.ui.components.ScreenContainer
import com.qoody.app.ui.components.SectionLabel
import com.qoody.app.ui.components.SegmentedControl
import com.qoody.app.ui.components.SheetHandle
import com.qoody.app.ui.components.StatusPill
import com.qoody.app.ui.components.uppercaseForLocale
import com.qoody.app.ui.theme.QoodyTheme
import com.qoody.shared.domain.model.AppTheme
import com.qoody.shared.feature.settings.SettingsUiState
import com.qoody.shared.feature.settings.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/** Everything configurable: automation, appearance, privacy and export. */
@Composable
fun SettingsScreen(
    onSearchClick: () -> Unit,
    onOpenNotificationAccessSettings: () -> Unit,
    onOpenUnparsedCaptures: () -> Unit,
    onOpenExcludedEntries: () -> Unit,
    onOpenBudgets: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHost = remember { SnackbarHostState() }
    val uriHandler = LocalUriHandler.current
    val backupService: BackupService = koinInject()
    var backupAction by remember { mutableStateOf<BackupAction?>(null) }
    var backupPassword by remember { mutableStateOf("") }
    var showImportWarning by remember { mutableStateOf(false) }

    val noBrowser = stringResource(R.string.settings_no_browser)
    val exportDone = stringResource(R.string.settings_export_done)
    val exportFailed = stringResource(R.string.settings_export_failed)
    val backupDone = stringResource(R.string.settings_backup_done)
    val backupFailed = stringResource(R.string.settings_backup_failed)

    val exportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(LedgerExport.MIME_TYPE)) { uri: Uri? ->
            if (uri != null) {
                scope.launch {
                    val written = writeCsv(context, uri, viewModel.buildCsvExport())
                    snackbarHost.showSnackbar(if (written) exportDone else exportFailed)
                }
            }
        }

    val backupExportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(BackupExport.MIME_TYPE)) { uri: Uri? ->
            if (uri != null) {
                scope.launch {
                    val result = runCatching { backupService.export(backupPassword.toCharArray()) }
                    val written = result.getOrNull()?.let { writeBytes(context, uri, it) } == true
                    backupPassword = ""
                    backupAction = null
                    snackbarHost.showSnackbar(if (written) backupDone else backupFailed)
                }
            }
        }
    val backupImportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) {
                scope.launch {
                    val imported =
                        runCatching {
                            context.contentResolver.openInputStream(uri)?.use { input ->
                                backupService.import(input.readBytes(), backupPassword.toCharArray())
                            } ?: error("Could not read backup")
                        }.isSuccess
                    backupPassword = ""
                    backupAction = null
                    snackbarHost.showSnackbar(if (imported) backupDone else backupFailed)
                }
            }
        }

    SettingsContent(
        state = state,
        snackbarHost = snackbarHost,
        onSearchClick = onSearchClick,
        // Only Android can grant or revoke notification access; the switch opens its settings page.
        onNotificationToggled = { onOpenNotificationAccessSettings() },
        onManageApps = onOpenNotificationAccessSettings,
        onOpenUnparsedCaptures = onOpenUnparsedCaptures,
        onOpenExcludedEntries = onOpenExcludedEntries,
        onOpenBudgets = onOpenBudgets,
        onThemeSelected = viewModel::onThemeSelected,
        onHapticsToggled = viewModel::onHapticsToggled,
        onOpenSource = {
            runCatching { uriHandler.openUri(ProjectLinks.SOURCE_CODE_URL) }
                .onFailure { scope.launch { snackbarHost.showSnackbar(noBrowser) } }
        },
        onExport = { exportLauncher.launch(LedgerExport.DEFAULT_FILE_NAME) },
        onExportFull = { backupAction = BackupAction.Export },
        onImportFull = { backupAction = BackupAction.Import },
    )

    backupAction?.let { action ->
        AlertDialog(
            onDismissRequest = {
                backupPassword = ""
                backupAction = null
                showImportWarning = false
            },
            title = {
                Text(
                    stringResource(
                        if (action ==
                            BackupAction.Export
                        ) {
                            R.string.settings_backup_export_title
                        } else {
                            R.string.settings_backup_import_title
                        },
                    ),
                )
            },
            text = {
                Column {
                    Text(stringResource(R.string.settings_backup_password_body))
                    QoodyTextField(
                        value = backupPassword,
                        onValueChange = { backupPassword = it },
                        placeholder = stringResource(R.string.settings_backup_password_label),
                        visualTransformation = PasswordVisualTransformation(),
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = backupPassword.isNotBlank(),
                    onClick = {
                        if (action == BackupAction.Export) {
                            backupExportLauncher.launch(BackupExport.DEFAULT_FILE_NAME)
                        } else {
                            showImportWarning = true
                        }
                    },
                ) { Text(stringResource(R.string.settings_backup_continue)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    backupPassword = ""
                    backupAction = null
                    showImportWarning = false
                }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
    if (showImportWarning) {
        AlertDialog(
            onDismissRequest = { showImportWarning = false },
            title = { Text(stringResource(R.string.settings_backup_replace_title)) },
            text = { Text(stringResource(R.string.settings_backup_replace_body)) },
            confirmButton = {
                Button(onClick = {
                    showImportWarning = false
                    backupImportLauncher.launch(arrayOf(BackupExport.MIME_TYPE))
                }) {
                    Text(stringResource(R.string.settings_backup_replace_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showImportWarning = false },
                ) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

private enum class BackupAction { Export, Import }

/** Writes [csv] to the document the user chose. Returns whether it succeeded. */
private suspend fun writeCsv(
    context: Context,
    uri: Uri,
    csv: String,
): Boolean =
    withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openOutputStream(uri)?.use { it.write(csv.toByteArray()) } != null
        }.getOrDefault(false)
    }

private suspend fun writeBytes(
    context: Context,
    uri: Uri,
    bytes: ByteArray,
): Boolean =
    withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) } != null
        }.getOrDefault(false)
    }

@Composable
fun SettingsContent(
    state: SettingsUiState,
    snackbarHost: SnackbarHostState,
    onSearchClick: () -> Unit,
    onNotificationToggled: (Boolean) -> Unit,
    onManageApps: () -> Unit,
    onOpenUnparsedCaptures: () -> Unit,
    onOpenExcludedEntries: () -> Unit,
    onOpenBudgets: () -> Unit = {},
    onThemeSelected: (AppTheme) -> Unit,
    onHapticsToggled: (Boolean) -> Unit,
    onOpenSource: () -> Unit,
    onExport: () -> Unit,
    onExportFull: () -> Unit,
    onImportFull: () -> Unit,
) {
    ScreenContainer {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                QoodyTopBar(onSearchClick = onSearchClick)
                when (state) {
                    SettingsUiState.Loading -> {
                        LoadingIndicator()
                    }

                    is SettingsUiState.Content -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding =
                                PaddingValues(
                                    start = QoodyTheme.spacing.md,
                                    end = QoodyTheme.spacing.md,
                                    bottom = QoodyTheme.spacing.lg,
                                ),
                            verticalArrangement = Arrangement.spacedBy(QoodyTheme.spacing.lg),
                        ) {
                            item { Header() }
                            item {
                                AutomationSection(
                                    state,
                                    onNotificationToggled,
                                    onManageApps,
                                    onOpenUnparsedCaptures,
                                    onOpenExcludedEntries,
                                    onOpenBudgets,
                                )
                            }
                            item { InterfaceSection(state, onThemeSelected, onHapticsToggled) }
                            item { PrivacySection(onOpenSource, onExport, onExportFull, onImportFull) }
                            item { Footer() }
                        }
                    }
                }
            }
            SnackbarHost(hostState = snackbarHost, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun Header() {
    Column(modifier = Modifier.fillMaxWidth().padding(top = QoodyTheme.spacing.xs)) {
        SectionLabel(stringResource(R.string.settings_eyebrow))
        Text(
            text = stringResource(R.string.settings_title),
            style = QoodyTheme.typography.headlineLgMobile,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun Section(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(QoodyTheme.spacing.xs)) {
        SectionLabel(title, modifier = Modifier.padding(horizontal = QoodyTheme.spacing.xs))
        content()
    }
}

@Composable
private fun IconTile(
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.secondary,
    size: Dp = QoodyTheme.sizes.settingIconBox,
) {
    Surface(
        modifier = modifier.size(size),
        shape = QoodyTheme.shapes.field,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Box(contentAlignment = Alignment.Center) {
            QoodyIcon(id = icon, contentDescription = null, tint = tint)
        }
    }
}

@Composable
private fun AutomationSection(
    state: SettingsUiState.Content,
    onToggled: (Boolean) -> Unit,
    onManageApps: () -> Unit,
    onOpenUnparsedCaptures: () -> Unit,
    onOpenExcludedEntries: () -> Unit,
    onOpenBudgets: () -> Unit,
) {
    val enabled = state.settings.notificationListenerEnabled
    val listenerLabel = if (enabled) R.string.settings_listener_active else R.string.settings_listener_paused
    val statusDot = if (enabled) QoodyTheme.colors.positive else MaterialTheme.colorScheme.tertiaryContainer
    Section(title = stringResource(R.string.settings_section_core)) {
        QoodyCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier =
                    Modifier.fillMaxWidth().toggleable(
                        value = enabled,
                        role = Role.Switch,
                        onValueChange = onToggled,
                    ),
                horizontalArrangement = Arrangement.spacedBy(QoodyTheme.spacing.cozy),
                verticalAlignment = Alignment.Top,
            ) {
                IconTile(
                    R.drawable.ic_notifications_active,
                    tint = MaterialTheme.colorScheme.primaryContainer,
                    size = QoodyTheme.sizes.featureIconBox,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(QoodyTheme.spacing.sm),
                    ) {
                        Text(
                            text = stringResource(R.string.settings_listener_title),
                            style = QoodyTheme.typography.titleMd,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                    }
                    StatusPill(
                        label = stringResource(listenerLabel),
                        dotColor = statusDot,
                        modifier = Modifier.padding(vertical = QoodyTheme.spacing.xs),
                        textStyle = QoodyTheme.typography.bodySmMedium,
                    )
                    Text(
                        text = stringResource(R.string.settings_listener_body),
                        style = QoodyTheme.typography.bodyMd,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                QoodySwitch(checked = enabled, onCheckedChange = null)
            }
            QoodyInset(modifier = Modifier.fillMaxWidth().padding(top = QoodyTheme.spacing.md)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(QoodyTheme.spacing.sm),
                    ) {
                        QoodyIcon(
                            R.drawable.ic_verified_user,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                        )
                        Text(
                            text =
                                pluralStringResource(
                                    R.plurals.settings_listening_apps,
                                    state.settings.monitoredAppCount,
                                    state.settings.monitoredAppCount,
                                ),
                            style = QoodyTheme.typography.bodySm,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                    Box(
                        modifier =
                            Modifier
                                .heightIn(min = QoodyTheme.sizes.touchTarget)
                                .clickable(role = Role.Button, onClick = onManageApps)
                                .padding(horizontal = QoodyTheme.spacing.xs),
                        contentAlignment = Alignment.Center,
                    ) {
                        SectionLabel(
                            stringResource(R.string.settings_manage),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
        QoodyCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues()) {
            SettingRow(
                icon = R.drawable.ic_error,
                title = stringResource(R.string.settings_unparsed_title),
                subtitle =
                    if (state.unparsedCount == 0) {
                        stringResource(R.string.settings_unparsed_none)
                    } else {
                        pluralStringResource(
                            R.plurals.settings_unparsed_count,
                            state.unparsedCount,
                            state.unparsedCount,
                        )
                    },
                onClick = onOpenUnparsedCaptures,
            ) {
                QoodyIcon(
                    R.drawable.ic_arrow_forward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                )
            }
            HairlineDivider()
            SettingRow(
                icon = R.drawable.ic_visibility_off,
                title = stringResource(R.string.settings_excluded_title),
                subtitle =
                    if (state.excludedCount == 0) {
                        stringResource(R.string.settings_excluded_none)
                    } else {
                        pluralStringResource(
                            R.plurals.settings_excluded_count,
                            state.excludedCount,
                            state.excludedCount,
                        )
                    },
                onClick = onOpenExcludedEntries,
            ) {
                QoodyIcon(
                    R.drawable.ic_arrow_forward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                )
            }
            HairlineDivider()
            SettingRow(
                icon = R.drawable.ic_account_balance_wallet,
                title = stringResource(R.string.settings_budgets_title),
                subtitle = stringResource(R.string.settings_budgets_subtitle),
                onClick = onOpenBudgets,
            ) {
                QoodyIcon(
                    R.drawable.ic_arrow_forward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}

@Composable
private fun InterfaceSection(
    state: SettingsUiState.Content,
    onThemeSelected: (AppTheme) -> Unit,
    onHapticsToggled: (Boolean) -> Unit,
) {
    Section(title = stringResource(R.string.settings_section_interface)) {
        QoodyCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues()) {
            SettingRow(
                icon = R.drawable.ic_palette,
                title = stringResource(R.string.settings_theme_title),
                subtitle = stringResource(R.string.settings_theme_subtitle),
            ) {
                ThemeDropdown(selected = state.settings.theme, onSelected = onThemeSelected)
            }
            HairlineDivider()
            SettingRow(
                icon = R.drawable.ic_vibration,
                title = stringResource(R.string.settings_haptics_title),
                subtitle = stringResource(R.string.settings_haptics_subtitle),
                toggle = ToggleSpec(state.settings.hapticsEnabled, onHapticsToggled),
            ) {
                QoodySwitch(checked = state.settings.hapticsEnabled, onCheckedChange = null)
            }
        }
    }
}

private class ToggleSpec(
    val checked: Boolean,
    val onChange: (Boolean) -> Unit,
)

@Composable
private fun SettingRow(
    @DrawableRes icon: Int,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    toggle: ToggleSpec? = null,
    trailing: @Composable () -> Unit,
) {
    val rowModifier =
        when {
            toggle != null -> {
                Modifier.toggleable(value = toggle.checked, role = Role.Switch, onValueChange = toggle.onChange)
            }

            onClick != null -> {
                Modifier.clickable(role = Role.Button, onClick = onClick)
            }

            else -> {
                Modifier
            }
        }
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .then(rowModifier)
                .heightIn(min = QoodyTheme.sizes.settingRowMinHeight)
                .padding(QoodyTheme.spacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(QoodyTheme.spacing.cozy),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconTile(icon)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = QoodyTheme.typography.titleMd, color = MaterialTheme.colorScheme.onSurface)
                Text(text = subtitle, style = QoodyTheme.typography.bodySm, color = MaterialTheme.colorScheme.secondary)
            }
        }
        Box(modifier = Modifier.padding(start = QoodyTheme.spacing.sm)) { trailing() }
    }
}

@Composable
private fun ThemeDropdown(
    selected: AppTheme,
    onSelected: (AppTheme) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Surface(
            onClick = { expanded = true },
            shape = QoodyTheme.shapes.pill,
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.heightIn(min = QoodyTheme.sizes.touchTarget),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = QoodyTheme.spacing.cozy),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(QoodyTheme.spacing.xs),
            ) {
                Text(
                    text = stringResource(selected.labelRes),
                    style = QoodyTheme.typography.bodySmMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                QoodyIcon(R.drawable.ic_expand_more, contentDescription = null, size = QoodyTheme.sizes.iconSm)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            AppTheme.entries.forEach { theme ->
                DropdownMenuItem(
                    text = { Text(stringResource(theme.labelRes), style = QoodyTheme.typography.bodyMd) },
                    onClick = {
                        expanded = false
                        onSelected(theme)
                    },
                )
            }
        }
    }
}

private val AppTheme.labelRes: Int
    get() =
        when (this) {
            AppTheme.WarmPaper -> R.string.theme_warm_paper
        }

@Composable
private fun PrivacySection(
    onOpenSource: () -> Unit,
    onExport: () -> Unit,
    onExportFull: () -> Unit,
    onImportFull: () -> Unit,
) {
    Section(title = stringResource(R.string.settings_section_privacy)) {
        Column(verticalArrangement = Arrangement.spacedBy(QoodyTheme.spacing.sm)) {
            QoodyCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues()) {
                SettingRow(
                    icon = R.drawable.ic_code,
                    title = stringResource(R.string.settings_open_source_title),
                    subtitle = stringResource(R.string.settings_open_source_subtitle),
                    onClick = onOpenSource,
                ) {
                    QoodyIcon(
                        R.drawable.ic_north_east,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                }
                HairlineDivider()
                SettingRow(
                    icon = R.drawable.ic_file_download,
                    title = stringResource(R.string.settings_export_title),
                    subtitle = stringResource(R.string.settings_export_subtitle),
                    onClick = onExport,
                ) {
                    QoodyIcon(
                        R.drawable.ic_arrow_downward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                }
                HairlineDivider()
                SettingRow(
                    icon = R.drawable.ic_lock,
                    title = stringResource(R.string.settings_backup_export_title),
                    subtitle = stringResource(R.string.settings_backup_export_subtitle),
                    onClick = onExportFull,
                ) {
                    QoodyIcon(
                        R.drawable.ic_arrow_downward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                }
                HairlineDivider()
                SettingRow(
                    icon = R.drawable.ic_file_download,
                    title = stringResource(R.string.settings_backup_import_title),
                    subtitle = stringResource(R.string.settings_backup_import_subtitle),
                    onClick = onImportFull,
                ) {
                    QoodyIcon(
                        R.drawable.ic_arrow_forward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
            QoodyInset(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(QoodyTheme.spacing.md)) {
                Row(horizontalArrangement = Arrangement.spacedBy(QoodyTheme.spacing.cozy)) {
                    Surface(
                        modifier = Modifier.size(QoodyTheme.sizes.settingIconBox - QoodyTheme.spacing.xs),
                        shape = QoodyTheme.shapes.pill,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            QoodyIcon(
                                R.drawable.ic_lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                            )
                        }
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.settings_pledge_title),
                            style = QoodyTheme.typography.buttonSm,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.settings_pledge_body),
                            style = QoodyTheme.typography.bodySm,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(top = QoodyTheme.spacing.xs),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Footer() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = QoodyTheme.spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(QoodyTheme.spacing.sm),
    ) {
        SheetHandle()
        Row(
            horizontalArrangement = Arrangement.spacedBy(QoodyTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                style = QoodyTheme.typography.numericSm,
                color = MaterialTheme.colorScheme.secondary,
            )
            Text(
                text = stringResource(R.string.settings_edition),
                style = QoodyTheme.typography.bodySm,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        SectionLabel(
            text = stringResource(R.string.settings_tagline),
            color = MaterialTheme.colorScheme.secondary.copy(alpha = QoodyTheme.alphas.mutedFooter),
        )
    }
}
