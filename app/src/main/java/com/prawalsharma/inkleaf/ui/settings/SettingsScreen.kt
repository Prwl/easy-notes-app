package com.prawalsharma.inkleaf.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prawalsharma.inkleaf.BuildConfig
import com.prawalsharma.inkleaf.R
import com.prawalsharma.inkleaf.data.prefs.AppFont
import com.prawalsharma.inkleaf.data.prefs.ThemeMode
import com.prawalsharma.inkleaf.ui.home.NotesEmptyIllustration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToArchive: () -> Unit,
    onNavigateToTrash: () -> Unit,
    onNavigateToHidden: () -> Unit,
    bottomPadding: PaddingValues = PaddingValues(),
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showThemeDialog by remember { mutableStateOf(false) }
    var showFontDialog by remember { mutableStateOf(false) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var showLicensesDialog by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val rateUnavailableMsg = stringResource(R.string.snackbar_rate_unavailable)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SettingsEvent.ShowMessage -> snackbarHostState.showSnackbar(event.text)
                is SettingsEvent.ShareFile -> { /* handled in ViewModel */ }
            }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingRestoreUri = uri
            showRestoreConfirmDialog = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings), fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(bottom = bottomPadding.calculateBottomPadding())
                .verticalScroll(rememberScrollState())
        ) {
            SettingsSectionHeader(stringResource(R.string.settings_section_notes))
            SettingsItem(
                icon = Icons.Default.Archive,
                title = stringResource(R.string.archive),
                subtitle = stringResource(R.string.settings_archive_subtitle),
                onClick = onNavigateToArchive
            )
            SettingsItem(
                icon = Icons.Default.Delete,
                title = stringResource(R.string.trash),
                subtitle = stringResource(R.string.settings_trash_subtitle),
                onClick = onNavigateToTrash
            )
            SettingsItem(
                icon = Icons.Default.VisibilityOff,
                title = stringResource(R.string.hidden_notes),
                subtitle = stringResource(R.string.settings_hidden_subtitle),
                onClick = onNavigateToHidden
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SettingsSectionHeader(stringResource(R.string.settings_section_appearance))
            SettingsItem(
                icon = Icons.Default.DarkMode,
                title = stringResource(R.string.settings_theme_title),
                subtitle = when (uiState.themeMode) {
                    ThemeMode.SYSTEM -> stringResource(R.string.theme_system_default)
                    ThemeMode.LIGHT -> stringResource(R.string.theme_light)
                    ThemeMode.DARK -> stringResource(R.string.theme_dark)
                },
                onClick = { showThemeDialog = true }
            )
            SettingsItem(
                icon = Icons.Default.FontDownload,
                title = stringResource(R.string.settings_font_title),
                subtitle = when (uiState.appFont) {
                    AppFont.DEFAULT -> stringResource(R.string.font_default)
                    AppFont.SERIF -> stringResource(R.string.font_serif)
                    AppFont.MONOSPACE -> stringResource(R.string.font_monospace)
                    AppFont.CURSIVE -> stringResource(R.string.font_cursive)
                },
                onClick = { showFontDialog = true }
            )
            // Material You toggle — only on Android 12+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                SwitchSettingsItem(
                    icon = Icons.Default.Palette,
                    title = stringResource(R.string.settings_dynamic_color_title),
                    subtitle = stringResource(R.string.settings_dynamic_color_subtitle),
                    checked = uiState.dynamicColor,
                    onCheckedChange = { viewModel.setDynamicColor(it) }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SettingsSectionHeader(stringResource(R.string.settings_section_backup))
            SettingsItem(
                icon = Icons.Default.Upload,
                title = stringResource(R.string.settings_export_backup_title),
                subtitle = stringResource(R.string.settings_export_backup_subtitle),
                onClick = { viewModel.exportBackup() }
            )
            SettingsItem(
                icon = Icons.Default.Download,
                title = stringResource(R.string.settings_import_backup_title),
                subtitle = stringResource(R.string.settings_import_backup_subtitle),
                onClick = { restoreLauncher.launch(arrayOf("application/json", "text/*")) }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SettingsSectionHeader(stringResource(R.string.settings_section_about))
            SettingsItem(
                icon = Icons.Default.Info,
                title = stringResource(R.string.settings_version_title),
                subtitle = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                onClick = {}
            )
            SettingsItem(
                icon = Icons.Default.Star,
                title = stringResource(R.string.settings_rate_title),
                subtitle = stringResource(R.string.settings_rate_subtitle),
                onClick = {
                    val pkg = context.packageName
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg")).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        })
                    } catch (_: Exception) {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$pkg")).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            })
                        } catch (_: Exception) {
                            // Show snackbar via coroutine
                        }
                    }
                }
            )
            SettingsItem(
                icon = Icons.Default.Policy,
                title = stringResource(R.string.settings_privacy_title),
                subtitle = stringResource(R.string.settings_privacy_subtitle),
                onClick = {
                    val url = context.getString(R.string.privacy_policy_url)
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                }
            )
            SettingsItem(
                icon = Icons.AutoMirrored.Filled.Article,
                title = stringResource(R.string.settings_licenses_title),
                subtitle = stringResource(R.string.settings_licenses_subtitle),
                onClick = { showLicensesDialog = true }
            )
            SettingsItem(
                icon = Icons.Default.Feedback,
                title = stringResource(R.string.settings_feedback_title),
                subtitle = stringResource(R.string.settings_feedback_subtitle),
                onClick = {
                    val subject = "Inkleaf feedback (v${BuildConfig.VERSION_NAME})"
                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
                        putExtra(Intent.EXTRA_SUBJECT, subject)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    try { context.startActivity(intent) } catch (_: Exception) {}
                }
            )

            // App logo + tagline footer
            Spacer(Modifier.height(24.dp))
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(modifier = Modifier.size(56.dp)) {
                    NotesEmptyIllustration()
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.settings_tagline),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }

    if (showRestoreConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirmDialog = false },
            title = { Text(stringResource(R.string.dialog_restore_backup_title)) },
            text = { Text(stringResource(R.string.dialog_restore_backup_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        pendingRestoreUri?.let { viewModel.importBackup(it) }
                        showRestoreConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.restore)) }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirmDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (showThemeDialog) {
        ThemePickerDialog(
            current = uiState.themeMode,
            onSelect = { viewModel.setThemeMode(it); showThemeDialog = false },
            onDismiss = { showThemeDialog = false }
        )
    }

    if (showFontDialog) {
        FontPickerDialog(
            current = uiState.appFont,
            onSelect = { viewModel.setAppFont(it); showFontDialog = false },
            onDismiss = { showFontDialog = false }
        )
    }

    if (showLicensesDialog) {
        LicensesDialog(onDismiss = { showLicensesDialog = false })
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        }
        Icon(
            Icons.Default.ChevronRight, null,
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
        )
    }
}

@Composable
private fun SwitchSettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ThemePickerDialog(
    current: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_theme_title)) },
        text = {
            Column {
                ThemeMode.entries.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(mode) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = mode == current, onClick = { onSelect(mode) })
                        Spacer(Modifier.width(8.dp))
                        Text(
                            when (mode) {
                                ThemeMode.SYSTEM -> stringResource(R.string.theme_system_default)
                                ThemeMode.LIGHT -> stringResource(R.string.theme_light)
                                ThemeMode.DARK -> stringResource(R.string.theme_dark)
                            }
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
private fun FontPickerDialog(
    current: AppFont,
    onSelect: (AppFont) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_font_title)) },
        text = {
            Column {
                AppFont.entries.forEach { font ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(font) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = font == current, onClick = { onSelect(font) })
                        Spacer(Modifier.width(8.dp))
                        Text(
                            when (font) {
                                AppFont.DEFAULT -> stringResource(R.string.font_default)
                                AppFont.SERIF -> stringResource(R.string.font_serif)
                                AppFont.MONOSPACE -> stringResource(R.string.font_monospace)
                                AppFont.CURSIVE -> stringResource(R.string.font_cursive)
                            },
                            fontFamily = when (font) {
                                AppFont.DEFAULT -> androidx.compose.ui.text.font.FontFamily.Default
                                AppFont.SERIF -> androidx.compose.ui.text.font.FontFamily.Serif
                                AppFont.MONOSPACE -> androidx.compose.ui.text.font.FontFamily.Monospace
                                AppFont.CURSIVE -> androidx.compose.ui.text.font.FontFamily.Cursive
                            }
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

private data class License(val name: String, val license: String)

private val openSourceLicenses = listOf(
    License("AndroidX Core / Lifecycle / Activity", "Apache License 2.0 — Copyright 2018 The Android Open Source Project"),
    License("Jetpack Compose", "Apache License 2.0 — Copyright 2020 The Android Open Source Project"),
    License("Hilt (Dagger)", "Apache License 2.0 — Copyright 2020 The Dagger Authors"),
    License("Room", "Apache License 2.0 — Copyright 2017 The Android Open Source Project"),
    License("Glance (App Widgets)", "Apache License 2.0 — Copyright 2021 The Android Open Source Project"),
    License("Coil", "Apache License 2.0 — Copyright 2021 Coil Contributors"),
    License("Kotlin Coroutines", "Apache License 2.0 — Copyright 2016 JetBrains s.r.o.")
)

private const val APACHE_SHORT = """Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied."""

@Composable
private fun LicensesDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(
                    stringResource(R.string.licenses_dialog_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(24.dp, 24.dp, 24.dp, 8.dp)
                )
                LazyColumn(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .heightIn(max = 400.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(openSourceLicenses) { lic ->
                        Column {
                            Text(lic.name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text(lic.license, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                            Spacer(Modifier.height(4.dp))
                            Text(APACHE_SHORT, style = MaterialTheme.typography.bodySmall.copy(fontSize = androidx.compose.ui.unit.TextUnit(10f, androidx.compose.ui.unit.TextUnitType.Sp)),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            HorizontalDivider(modifier = Modifier.padding(top = 12.dp))
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_done)) }
                }
            }
        }
    }
}
