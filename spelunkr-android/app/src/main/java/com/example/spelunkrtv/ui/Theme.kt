package com.example.spelunkrtv.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** The site's "8s" look: cable-guide purple and orange, yellow type, navy panels with blue rims. */
object Palette {
    val Purple = Color(0xFF1C0A57)
    val Sunset = Color(0xFFA6501A)
    val HeaderBar = Color(0xFF26094F)
    val Panel = Color(0xFF1E2A6E)
    val PanelDeep = Color(0xFF0F1A5C)
    val Rim = Color(0xFF3B6BE0)
    val Yellow = Color(0xFFFFF200)
    val Cyan = Color(0xFF7FE6E6)
    val Muted = Color(0xFFAEB8E8)
    val Live = Color(0xFFE53935)
    val Ok = Color(0xFF3DDC84)
    val Warn = Color(0xFFFFB300)
    val GoldTop = Color(0xFFD9661C)
    val GoldBottom = Color(0xFF5E2A14)
    val Selected = Brush.verticalGradient(listOf(Color(0xFFE07A1F), Color(0xFF8A3A12)))
    val Sky = Brush.verticalGradient(listOf(Purple, Color(0xFF5A2350), Sunset))
    val Gold = Brush.verticalGradient(listOf(GoldTop, GoldBottom))
}

/**
 * The site draws everything in a WeatherStar-style face. That font is not ours to ship (see
 * README), so the app uses the platform monospace, bold, with the same hard drop shadow.
 */
fun spelunkrText(size: TextUnit, color: Color = Color.White, shadow: Boolean = true) = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Bold,
    fontSize = size,
    color = color,
    shadow = if (shadow) Shadow(Color.Black, Offset(2f, 2f), 0f) else null,
)

/** True on Android TV / Google TV: D-pad first, bigger type, no touch. */
val LocalIsTv = staticCompositionLocalOf { false }

private val clockFormat = SimpleDateFormat("h:mm:ss a", Locale.US)
private val timeFormat = SimpleDateFormat("h:mm a", Locale.US)

fun clockText(ms: Long): String = clockFormat.format(Date(ms))
fun timeOfDay(ms: Long): String = timeFormat.format(Date(ms))

/** 4:38:39 or 39:55, like the site's progress readouts. */
fun runtime(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = total % 3600 / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
