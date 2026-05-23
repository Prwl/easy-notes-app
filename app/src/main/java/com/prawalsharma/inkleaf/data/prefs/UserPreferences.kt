package com.prawalsharma.inkleaf.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class AppFont { DEFAULT, SERIF, MONOSPACE, CURSIVE }

@Singleton
class UserPreferences @Inject constructor(@ApplicationContext private val context: Context) {

    private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
    private val APP_FONT_KEY = stringPreferencesKey("app_font")
    private val DYNAMIC_COLOR_KEY = androidx.datastore.preferences.core.booleanPreferencesKey("dynamic_color")

    /**
     * Map of noteId → salted SHA-256 PIN hash.
     * Stored as comma-separated entries of "noteId:saltHex:hashHex".
     * Plaintext PINs are NEVER stored on disk.
     */
    private val NOTE_PINS_KEY = stringPreferencesKey("note_pins")

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        ThemeMode.valueOf(prefs[THEME_MODE_KEY] ?: ThemeMode.SYSTEM.name)
    }

    val appFont: Flow<AppFont> = context.dataStore.data.map { prefs ->
        AppFont.valueOf(prefs[APP_FONT_KEY] ?: AppFont.DEFAULT.name)
    }

    val dynamicColor: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[DYNAMIC_COLOR_KEY] ?: false
    }

    /** True if a PIN is set for the given note. */
    suspend fun hasPin(noteId: Long): Boolean = readEntry(noteId) != null

    /** Verifies [candidate] against the stored salted hash for [noteId]. */
    suspend fun verifyPin(noteId: Long, candidate: String): Boolean {
        val entry = readEntry(noteId) ?: return false
        val (saltHex, expectedHashHex) = entry
        val saltBytes = hexToBytes(saltHex)
        val candidateHash = sha256(saltBytes + candidate.toByteArray(Charsets.UTF_8))
        return constantTimeEquals(candidateHash, hexToBytes(expectedHashHex))
    }

    /** Stores a freshly-generated salt + SHA-256 hash of [pin] for [noteId]. */
    suspend fun setPin(noteId: Long, pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = sha256(salt + pin.toByteArray(Charsets.UTF_8))
        val entry = "$noteId:${bytesToHex(salt)}:${bytesToHex(hash)}"

        context.dataStore.edit { prefs ->
            val existing = prefs[NOTE_PINS_KEY] ?: ""
            val kept = existing.split(",")
                .filter { it.isNotBlank() && !it.startsWith("$noteId:") }
            prefs[NOTE_PINS_KEY] = (kept + entry).joinToString(",")
        }
    }

    suspend fun clearPin(noteId: Long) {
        context.dataStore.edit { prefs ->
            val existing = prefs[NOTE_PINS_KEY] ?: ""
            prefs[NOTE_PINS_KEY] = existing.split(",")
                .filter { it.isNotBlank() && !it.startsWith("$noteId:") }.joinToString(",")
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[THEME_MODE_KEY] = mode.name }
    }

    suspend fun setAppFont(font: AppFont) {
        context.dataStore.edit { it[APP_FONT_KEY] = font.name }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { it[DYNAMIC_COLOR_KEY] = enabled }
    }

    // ---------- Internal ----------

    /** Returns (saltHex, hashHex) or null if no entry. */
    private suspend fun readEntry(noteId: Long): Pair<String, String>? {
        val raw = context.dataStore.data.first()[NOTE_PINS_KEY] ?: return null
        val entry = raw.split(",").firstOrNull { it.startsWith("$noteId:") } ?: return null
        val parts = entry.split(":")
        if (parts.size != 3) return null
        return parts[1] to parts[2]
    }

    private fun sha256(data: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(data)

    private fun bytesToHex(bytes: ByteArray): String =
        bytes.joinToString("") { "%02x".format(it) }

    private fun hexToBytes(hex: String): ByteArray = ByteArray(hex.length / 2) { i ->
        ((Character.digit(hex[i * 2], 16) shl 4) + Character.digit(hex[i * 2 + 1], 16)).toByte()
    }

    /** Length-constant comparison to avoid timing attacks. */
    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].toInt() xor b[i].toInt())
        return diff == 0
    }
}
