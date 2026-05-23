package com.prawalsharma.inkleaf.ui.theme

import androidx.compose.ui.graphics.Color

// Primary brand colors
val Orange = Color(0xFFFF6B35)
val OrangeLight = Color(0xFFFF8C69)
val Yellow = Color(0xFFFFF9C4)
val DarkBackground = Color(0xFF1A1A2E)
val DarkSurface = Color(0xFF16213E)
val DarkCard = Color(0xFF0F3460)

// ─── 200+ Note Background Colors ─────────────────────────────────────────────
// Organised by hue family. Content color rule: dark hex (0xFF1A1A2E or 0xFF16213E)
// → white text; everything else → black text.

data class NoteColorGroup(val name: String, val colors: List<Int>)

val NoteColorGroups: List<NoteColorGroup> = listOf(
    NoteColorGroup("Default", listOf(
        0xFFFFFDE7.toInt(), 0xFFFFFFFF.toInt(), 0xFFF5F5F5.toInt(),
        0xFFEEEEEE.toInt(), 0xFFE0E0E0.toInt()
    )),
    NoteColorGroup("Yellow", listOf(
        0xFFFFF9C4.toInt(), 0xFFFFF59D.toInt(), 0xFFFFF176.toInt(),
        0xFFFFEE58.toInt(), 0xFFFFEB3B.toInt(), 0xFFFDD835.toInt(),
        0xFFF9A825.toInt(), 0xFFF57F17.toInt(), 0xFFFFE082.toInt(),
        0xFFFFD54F.toInt()
    )),
    NoteColorGroup("Orange", listOf(
        0xFFFFCCBC.toInt(), 0xFFFFAB91.toInt(), 0xFFFF8A65.toInt(),
        0xFFFF7043.toInt(), 0xFFFF5722.toInt(), 0xFFE64A19.toInt(),
        0xFFFF6B35.toInt(), 0xFFFF8C69.toInt(), 0xFFFFCC80.toInt(),
        0xFFFFB74D.toInt()
    )),
    NoteColorGroup("Pink & Red", listOf(
        0xFFFFCDD2.toInt(), 0xFFEF9A9A.toInt(), 0xFFE57373.toInt(),
        0xFFEF5350.toInt(), 0xFFF44336.toInt(), 0xFFE53935.toInt(),
        0xFFF8BBD0.toInt(), 0xFFF48FB1.toInt(), 0xFFF06292.toInt(),
        0xFFEC407A.toInt(), 0xFFE91E63.toInt(), 0xFFAD1457.toInt(),
        0xFFFF80AB.toInt(), 0xFFFF4081.toInt(), 0xFFF50057.toInt()
    )),
    NoteColorGroup("Purple", listOf(
        0xFFE1BEE7.toInt(), 0xFFCE93D8.toInt(), 0xFFBA68C8.toInt(),
        0xFFAB47BC.toInt(), 0xFF9C27B0.toInt(), 0xFF7B1FA2.toInt(),
        0xFFD1C4E9.toInt(), 0xFFB39DDB.toInt(), 0xFF9575CD.toInt(),
        0xFF7E57C2.toInt(), 0xFF673AB7.toInt(), 0xFF4527A0.toInt(),
        0xFFEA80FC.toInt(), 0xFFE040FB.toInt(), 0xFFD500F9.toInt()
    )),
    NoteColorGroup("Blue", listOf(
        0xFFBBDEFB.toInt(), 0xFF90CAF9.toInt(), 0xFF64B5F6.toInt(),
        0xFF42A5F5.toInt(), 0xFF2196F3.toInt(), 0xFF1976D2.toInt(),
        0xFF0D47A1.toInt(), 0xFFB3E5FC.toInt(), 0xFF81D4FA.toInt(),
        0xFF4FC3F7.toInt(), 0xFF29B6F6.toInt(), 0xFF039BE5.toInt(),
        0xFF82B1FF.toInt(), 0xFF448AFF.toInt(), 0xFF2979FF.toInt(),
        0xFF2962FF.toInt()
    )),
    NoteColorGroup("Cyan & Teal", listOf(
        0xFFB2EBF2.toInt(), 0xFF80DEEA.toInt(), 0xFF4DD0E1.toInt(),
        0xFF26C6DA.toInt(), 0xFF00BCD4.toInt(), 0xFF0097A7.toInt(),
        0xFFB2DFDB.toInt(), 0xFF80CBC4.toInt(), 0xFF4DB6AC.toInt(),
        0xFF26A69A.toInt(), 0xFF009688.toInt(), 0xFF00695C.toInt(),
        0xFF84FFFF.toInt(), 0xFF18FFFF.toInt(), 0xFF00E5FF.toInt()
    )),
    NoteColorGroup("Green", listOf(
        0xFFC8E6C9.toInt(), 0xFFA5D6A7.toInt(), 0xFF81C784.toInt(),
        0xFF66BB6A.toInt(), 0xFF4CAF50.toInt(), 0xFF388E3C.toInt(),
        0xFF1B5E20.toInt(), 0xFFDCEDC8.toInt(), 0xFFC5E1A5.toInt(),
        0xFFAED581.toInt(), 0xFF9CCC65.toInt(), 0xFF8BC34A.toInt(),
        0xFF558B2F.toInt(), 0xFFCCFF90.toInt(), 0xFFB2FF59.toInt(),
        0xFF76FF03.toInt()
    )),
    NoteColorGroup("Lime", listOf(
        0xFFF0F4C3.toInt(), 0xFFE6EE9C.toInt(), 0xFFDCE775.toInt(),
        0xFFD4E157.toInt(), 0xFFCDDC39.toInt(), 0xFFAFB42B.toInt(),
        0xFFF9FBE7.toInt(), 0xFFF4FF81.toInt(), 0xFFEEFF41.toInt(),
        0xFFC6FF00.toInt()
    )),
    NoteColorGroup("Brown & Grey", listOf(
        0xFFD7CCC8.toInt(), 0xFFBCAAA4.toInt(), 0xFFA1887F.toInt(),
        0xFF8D6E63.toInt(), 0xFF795548.toInt(), 0xFF4E342E.toInt(),
        0xFFCFD8DC.toInt(), 0xFFB0BEC5.toInt(), 0xFF90A4AE.toInt(),
        0xFF78909C.toInt(), 0xFF546E7A.toInt(), 0xFF37474F.toInt(),
        0xFF263238.toInt()
    )),
    NoteColorGroup("Dark", listOf(
        0xFF212121.toInt(), 0xFF303030.toInt(), 0xFF424242.toInt(),
        0xFF1A1A2E.toInt(), 0xFF16213E.toInt(), 0xFF0F3460.toInt(),
        0xFF1B1B2F.toInt(), 0xFF162447.toInt(), 0xFF1F4068.toInt(),
        0xFF1B262C.toInt()
    )),
    NoteColorGroup("Pastel", listOf(
        0xFFFFF0F5.toInt(), 0xFFFFF5EE.toInt(), 0xFFF0FFF0.toInt(),
        0xFFF0F8FF.toInt(), 0xFFFAF0E6.toInt(), 0xFFFDF5E6.toInt(),
        0xFFE6E6FA.toInt(), 0xFFFFE4E1.toInt(), 0xFFE0FFFF.toInt(),
        0xFFFFEFD5.toInt(), 0xFFF5FFFA.toInt(), 0xFFFFF8DC.toInt(),
        0xFFFFECB3.toInt(), 0xFFFFD180.toInt(), 0xFFEAD7FF.toInt(),
        0xFFD7F2FF.toInt()
    )),
    NoteColorGroup("Gradient-Start", listOf(
        0xFFFF6B6B.toInt(), 0xFFFF8E53.toInt(), 0xFFFFC371.toInt(),
        0xFF56AB2F.toInt(), 0xFF2196F3.toInt(), 0xFF833AB4.toInt(),
        0xFFFC5C7D.toInt(), 0xFF6A3093.toInt(), 0xFF11998E.toInt(),
        0xFF38EF7D.toInt()
    ))
)

/** Flat list for backward-compat and simple pickers. */
val NoteColors: List<Int> = NoteColorGroups.flatMap { it.colors }

/** Returns true if the note background color needs white content (text/icons). */
fun needsWhiteContent(colorInt: Int): Boolean =
    colorInt == 0xFF1A1A2E.toInt() || colorInt == 0xFF16213E.toInt() ||
    colorInt == 0xFF0F3460.toInt() || colorInt == 0xFF212121.toInt() ||
    colorInt == 0xFF303030.toInt() || colorInt == 0xFF424242.toInt() ||
    colorInt == 0xFF1B1B2F.toInt() || colorInt == 0xFF162447.toInt() ||
    colorInt == 0xFF1F4068.toInt() || colorInt == 0xFF1B262C.toInt() ||
    colorInt == 0xFF263238.toInt() || colorInt == 0xFF37474F.toInt() ||
    colorInt == 0xFF4E342E.toInt() || colorInt == 0xFF0D47A1.toInt() ||
    colorInt == 0xFF4527A0.toInt() || colorInt == 0xFF7B1FA2.toInt() ||
    colorInt == 0xFFAD1457.toInt() || colorInt == 0xFF006030.toInt() ||
    colorInt == 0xFF00695C.toInt() || colorInt == 0xFF0097A7.toInt()
