package com.prawalsharma.inkleaf.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.text.font.FontFamily

/** CompositionLocal that carries the user-selected font family app-wide. */
val LocalAppFont = compositionLocalOf { FontFamily.Default }
