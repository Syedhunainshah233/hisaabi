package com.syedhunain.hisaabi.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Hisaabi brand colours: Emerald & Gold. */
object Brand {
    val Emerald = Color(0xFF0E5E4E)
    val EmeraldDeep = Color(0xFF0B3F35)
    val Gold = Color(0xFFC9A24A)
    val GoldLight = Color(0xFFE8C977)
    val Ivory = Color(0xFFF6F1E3)
    val Mint = Color(0xFFCFE3DC)
}

/** Extra colours the Material scheme has no slot for. */
data class ExtColors(
    val green: Color,       // "Liya" / you will get text
    val red: Color,         // "Diya" / you will give text
    val greenButton: Color,
    val redButton: Color,
    val greenSoft: Color,
    val redSoft: Color,
    val goldSoft: Color,
    val onGoldSoft: Color,
    val divider: Color,
    val muted: Color,
    val avatarBg: Color,
    val avatarFg: Color,
    val header: Color,
    val isDark: Boolean,
)

private val LightExt = ExtColors(
    green = Color(0xFF117A4F), red = Color(0xFFC62F35),
    greenButton = Color(0xFF117A4F), redButton = Color(0xFFC62F35),
    greenSoft = Color(0xFFE7F4EE), redSoft = Color(0xFFFBECEC),
    goldSoft = Color(0xFFF3EBD6), onGoldSoft = Color(0xFF5C4A1E),
    divider = Color(0xFFEEF0ED), muted = Color(0xFF5F6670),
    avatarBg = Color(0xFFE6F0EC), avatarFg = Brand.Emerald,
    header = Brand.Emerald, isDark = false,
)

private val DarkExt = ExtColors(
    green = Color(0xFF4FD39A), red = Color(0xFFFF7A7E),
    greenButton = Color(0xFF138A5A), redButton = Color(0xFFC93A3F),
    greenSoft = Color(0xFF15302A), redSoft = Color(0xFF3A1F21),
    goldSoft = Color(0xFF2E2816), onGoldSoft = Color(0xFFE8D7A6),
    divider = Color(0xFF22302C), muted = Color(0xFFA3B0AB),
    avatarBg = Color(0xFF1E3530), avatarFg = Brand.GoldLight,
    header = Color(0xFF0F3B32), isDark = true,
)

val LocalExt = staticCompositionLocalOf { LightExt }

private val LightScheme = lightColorScheme(
    primary = Brand.Emerald,
    onPrimary = Brand.Ivory,
    primaryContainer = Color(0xFFE6F0EC),
    onPrimaryContainer = Brand.EmeraldDeep,
    secondary = Brand.Gold,
    onSecondary = Color(0xFF1A1D1F),
    background = Color(0xFFF6F7F5),
    onBackground = Color(0xFF1A1D1F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1D1F),
    surfaceVariant = Color(0xFFEEF0ED),
    onSurfaceVariant = Color(0xFF5F6670),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFFFFFFF),
    outline = Color(0xFFD9DDD8),
    outlineVariant = Color(0xFFE5E8E4),
    error = Color(0xFFC62F35),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF2E9C83),
    onPrimary = Color(0xFF04201A),
    primaryContainer = Color(0xFF1E3530),
    onPrimaryContainer = Brand.GoldLight,
    secondary = Brand.GoldLight,
    onSecondary = Color(0xFF1A1D1F),
    background = Color(0xFF0D1412),
    onBackground = Color(0xFFE9EEEC),
    surface = Color(0xFF16201D),
    onSurface = Color(0xFFE9EEEC),
    surfaceVariant = Color(0xFF1E2A26),
    onSurfaceVariant = Color(0xFFA3B0AB),
    surfaceContainer = Color(0xFF16201D),
    surfaceContainerHigh = Color(0xFF1B2723),
    outline = Color(0xFF34423D),
    outlineVariant = Color(0xFF26332F),
    error = Color(0xFFFF7A7E),
)

private val AppTypography = Typography().let { t ->
    t.copy(
        headlineSmall = t.headlineSmall.copy(fontWeight = FontWeight.Bold, fontSize = 22.sp),
        titleLarge = t.titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = t.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = t.labelLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
    )
}

val MoneyBig = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold)

@Composable
fun HisaabiTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) DarkScheme else LightScheme,
        typography = AppTypography,
        shapes = Shapes(
            small = RoundedCornerShape(10.dp),
            medium = RoundedCornerShape(14.dp),
            large = RoundedCornerShape(16.dp),
        ),
    ) {
        CompositionLocalProvider(LocalExt provides if (dark) DarkExt else LightExt) {
            content()
        }
    }
}
