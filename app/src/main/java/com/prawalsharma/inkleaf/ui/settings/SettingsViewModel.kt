package com.prawalsharma.inkleaf.ui.settings

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prawalsharma.inkleaf.R
import com.prawalsharma.inkleaf.data.prefs.AppFont
import com.prawalsharma.inkleaf.data.prefs.ThemeMode
import com.prawalsharma.inkleaf.data.prefs.UserPreferences
import com.prawalsharma.inkleaf.utils.BackupManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.net.Uri

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val appFont: AppFont = AppFont.DEFAULT,
    val dynamicColor: Boolean = false,
    val isBackupInProgress: Boolean = false
)

/** One-shot events to the UI (snackbar messages, share intents). */
sealed class SettingsEvent {
    data class ShowMessage(val text: String) : SettingsEvent()
    data class ShareFile(val uri: Uri) : SettingsEvent()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: UserPreferences,
    private val backupManager: BackupManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        prefs.themeMode, prefs.appFont, prefs.dynamicColor
    ) { theme, font, dynamic -> SettingsUiState(theme, font, dynamic) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    private val _events = Channel<SettingsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { prefs.setThemeMode(mode) }
    fun setAppFont(font: AppFont) = viewModelScope.launch { prefs.setAppFont(font) }
    fun setDynamicColor(enabled: Boolean) = viewModelScope.launch { prefs.setDynamicColor(enabled) }

    fun exportBackup() = viewModelScope.launch {
        try {
            val uri = backupManager.exportBackup()
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Inkleaf Backup")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_backup_chooser)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            _events.send(SettingsEvent.ShowMessage(context.getString(R.string.msg_backup_exported)))
        } catch (e: Exception) {
            _events.send(SettingsEvent.ShowMessage(context.getString(R.string.msg_export_failed, e.message)))
        }
    }

    fun importBackup(uri: Uri) = viewModelScope.launch {
        try {
            val count = backupManager.importBackup(uri)
            _events.send(SettingsEvent.ShowMessage(context.getString(R.string.msg_backup_restored, count)))
        } catch (e: Exception) {
            _events.send(SettingsEvent.ShowMessage(context.getString(R.string.msg_restore_failed, e.message)))
        }
    }
}
