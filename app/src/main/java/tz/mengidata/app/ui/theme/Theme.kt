package tz.mengidata.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import tz.mengidata.app.R
import tz.mengidata.app.i18n.ThemeMode

// Samsung One UI 8.5 Geometric Sans-Serif Typography Pairing
val OneUiDisplayFamily = FontFamily(
    Font(R.font.outfit, FontWeight.Normal),
    Font(R.font.outfit, FontWeight.Medium),
    Font(R.font.outfit, FontWeight.SemiBold),
    Font(R.font.outfit, FontWeight.Bold)
)

val PlusJakartaSansFamily = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold)
)

// Calm Matte Solid Palette (Zero Glare, Zero Neon, Zero Transparency — WCAG AA/AAA Verified)
// Light: Warm Matte Stone Canvas (#EDEFF2), Soft Matte Paper Cards (#F8F9FA), Muted Slate Navy (#334B63)
// Dark: Calm Matte Deep Slate Canvas (#0E1216), Sculpted Matte Graphite Cards (#181E25), Elevated Slate (#232B35), Crisp Matte Divider (#384452)

private val OneUiDarkCanvas = Color(0xFF0E1216)
private val OneUiDarkCard = Color(0xFF181E25)
private val OneUiDarkElevated = Color(0xFF232B35)
private val OneUiDarkBorder = Color(0xFF384452)

private val OneUiLightCanvas = Color(0xFFEDEFF2)
private val OneUiPureWhite = Color(0xFFF8F9FA)
private val OneUiLightPill = Color(0xFFE2E6EC)
private val OneUiLightBorder = Color(0xFFD1D7E0)

private val DeepInk = Color(0xFF131920)
private val SlateInk = Color(0xFF1C232C)
private val SlateMutedAccessible = Color(0xFF4F5A68) // >= 6.4:1 on Light Canvas & Pill
private val SnowInk = Color(0xFFE5EAF0) // Soft Matte Alabaster (zero glare on dark)
private val SilverMutedAccessible = Color(0xFFAAB4C2) // >= 7.8:1 on Dark Card & Elevated

private val OneUiBlueLight = Color(0xFF334B63) // Calm Matte Slate Navy (>= 8.2:1 on Soft Paper)
private val OneUiBlueDark = Color(0xFF7D95AB) // Calm Matte Steel Blue (zero glare)
private val OneUiBlueSoftLight = Color(0xFFDEE5ED)
private val OneUiBlueInkLight = Color(0xFF24384B)
private val OneUiBlueSoftDark = Color(0xFF24313E)
private val OneUiBlueInkDark = Color(0xFFD5E0EB)

private val GalaxyIndigoLight = Color(0xFF4E5975) // Calm Matte Slate Indigo
private val GalaxyIndigoDark = Color(0xFF8C97B0)
private val GalaxyIndigoSoft = Color(0xFFE4E7F0)
private val GalaxyIndigoInk = Color(0xFF343D54)

private val Amber700 = Color(0xFF7D5D3B) // Calm Matte Warm Ochre
private val AmberSoft = Color(0xFFF0EAE1)
private val Amber400 = Color(0xFFB59A7B) // Calm Matte Sand Ochre on Dark
private val AmberDarkSoft = Color(0xFF2C251D)

private val EmeraldIncomeLight = Color(0xFF346352) // Calm Matte Forest Sage
private val EmeraldIncomeSoftLight = Color(0xFFE2ECE8)
private val EmeraldIncomeDark = Color(0xFF759F8E) // Calm Matte Sage on Dark (zero glare)
private val EmeraldIncomeSoftDark = Color(0xFF1D2C26)

private val SoftPlumLight = Color(0xFF5D526E) // Calm Matte Dusty Mauve
private val SoftPlumBgLight = Color(0xFFEBE8F0)
private val SoftPlumDark = Color(0xFF9E92B0) // Calm Matte Lavender-Slate on Dark
private val SoftPlumBgDark = Color(0xFF282331)

@Stable
data class MengiThemeState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isDark: Boolean = false,
    val dynamicColorEnabled: Boolean = false,
    val dynamicColorSupported: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
    val highContrastEnabled: Boolean = false,
    val onSetThemeMode: (ThemeMode) -> Unit = {},
    val onSetDynamicColor: (Boolean) -> Unit = {},
    val onSetHighContrast: (Boolean) -> Unit = {}
)

val LocalMengiThemeState = staticCompositionLocalOf { MengiThemeState() }

@Immutable
data class MengiExtendedColors(
    val isDark: Boolean,
    val heroStart: Color,
    val heroMid: Color,
    val heroEnd: Color,
    val heroAccentGlow: Color,
    val heroGoldGlow: Color,
    val lime: Color,
    val limeSoft: Color,
    val limeInk: Color,
    val income: Color,
    val incomeSoft: Color,
    val expense: Color,
    val expenseSoft: Color,
    val warning: Color,
    val warningSoft: Color,
    val savings: Color,
    val savingsSoft: Color,
    val cardBorder: Color,
    val subtleSurface: Color,
    val glassHighlight: Color,
    val dockSurface: Color,
    val dockBorder: Color
)

private val LightExtendedColors = MengiExtendedColors(
    isDark = false,
    heroStart = Color(0xFF1D242C),
    heroMid = Color(0xFF252F3A),
    heroEnd = Color(0xFF2F3C4B),
    heroAccentGlow = Color(0xFF8BA2B8),
    heroGoldGlow = Color(0xFFA6AFC2),
    lime = Color(0xFFB8C7D6),
    limeSoft = OneUiBlueSoftLight,
    limeInk = OneUiBlueInkLight,
    income = EmeraldIncomeLight,
    incomeSoft = EmeraldIncomeSoftLight,
    expense = SoftPlumLight,
    expenseSoft = SoftPlumBgLight,
    warning = Amber700,
    warningSoft = AmberSoft,
    savings = GalaxyIndigoLight,
    savingsSoft = GalaxyIndigoSoft,
    cardBorder = OneUiLightBorder,
    subtleSurface = OneUiLightPill,
    glassHighlight = Color(0xFFE6EAEF),
    dockSurface = Color(0xFFF5F6F8),
    dockBorder = Color(0xFFCFD6DF)
)

private val DarkExtendedColors = MengiExtendedColors(
    isDark = true,
    heroStart = Color(0xFF1D252E),
    heroMid = Color(0xFF232C37),
    heroEnd = Color(0xFF2A3542),
    heroAccentGlow = Color(0xFF7D95AB),
    heroGoldGlow = Color(0xFF8C97B0),
    lime = Color(0xFFB2C1D0),
    limeSoft = OneUiBlueSoftDark,
    limeInk = OneUiBlueInkDark,
    income = EmeraldIncomeDark,
    incomeSoft = EmeraldIncomeSoftDark,
    expense = SoftPlumDark,
    expenseSoft = SoftPlumBgDark,
    warning = Amber400,
    warningSoft = AmberDarkSoft,
    savings = GalaxyIndigoDark,
    savingsSoft = Color(0xFF232938),
    cardBorder = OneUiDarkBorder,
    subtleSurface = OneUiDarkElevated,
    glassHighlight = Color(0xFF28313B),
    dockSurface = Color(0xFF1D242D),
    dockBorder = Color(0xFF3E4B5B)
)

val LocalMengiColors = staticCompositionLocalOf { LightExtendedColors }

internal val MengiLightColorScheme = lightColorScheme(
    primary = OneUiBlueLight,
    onPrimary = Color(0xFFF5F7FA),
    primaryContainer = OneUiBlueSoftLight,
    onPrimaryContainer = OneUiBlueInkLight,
    secondary = GalaxyIndigoLight,
    onSecondary = Color(0xFFF5F7FA),
    secondaryContainer = GalaxyIndigoSoft,
    onSecondaryContainer = GalaxyIndigoInk,
    tertiary = Amber700,
    onTertiary = Color(0xFFF5F7FA),
    tertiaryContainer = AmberSoft,
    onTertiaryContainer = Color(0xFF5A4229),
    background = OneUiLightCanvas,
    onBackground = SlateInk,
    surface = OneUiPureWhite,
    onSurface = SlateInk,
    surfaceVariant = OneUiLightPill,
    onSurfaceVariant = SlateMutedAccessible,
    outline = OneUiLightBorder,
    outlineVariant = Color(0xFFDFE3EA),
    error = SoftPlumLight,
    onError = Color(0xFFF5F7FA),
    errorContainer = SoftPlumBgLight,
    onErrorContainer = Color(0xFF423952)
)

internal val MengiDarkColorScheme = darkColorScheme(
    primary = OneUiBlueDark,
    onPrimary = DeepInk,
    primaryContainer = OneUiBlueSoftDark,
    onPrimaryContainer = OneUiBlueInkDark,
    secondary = GalaxyIndigoDark,
    onSecondary = DeepInk,
    secondaryContainer = OneUiDarkElevated,
    onSecondaryContainer = SnowInk,
    tertiary = Amber400,
    onTertiary = DeepInk,
    tertiaryContainer = AmberDarkSoft,
    onTertiaryContainer = Color(0xFFDFCEB8),
    background = OneUiDarkCanvas,
    onBackground = SnowInk,
    surface = OneUiDarkCard,
    onSurface = SnowInk,
    surfaceVariant = OneUiDarkElevated,
    onSurfaceVariant = SilverMutedAccessible,
    outline = OneUiDarkBorder,
    outlineVariant = Color(0xFF2C3642),
    error = SoftPlumDark,
    onError = DeepInk,
    errorContainer = SoftPlumBgDark,
    onErrorContainer = Color(0xFFDBD3E6)
)

val MengiTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = OneUiDisplayFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 38.sp,
        lineHeight = 44.sp,
        letterSpacing = (-1.1).sp
    ),
    displaySmall = TextStyle(
        fontFamily = OneUiDisplayFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 31.sp,
        lineHeight = 37.sp,
        letterSpacing = (-0.75).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = OneUiDisplayFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.4).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = OneUiDisplayFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.2).sp
    ),
    titleLarge = TextStyle(
        fontFamily = OneUiDisplayFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.15).sp
    ),
    titleMedium = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.5.sp,
        lineHeight = 21.sp
    ),
    titleSmall = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.5.sp,
        lineHeight = 18.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp,
        lineHeight = 19.sp
    ),
    bodySmall = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp
    ),
    labelLarge = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.5.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.25.sp
    ),
    labelSmall = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 10.5.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.8.sp
    )
)

// 2026 Current-Generation Expressive Squircle Radius System
val MengiShapes = Shapes(
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(34.dp)
)

private fun deriveExtendedColors(
    scheme: ColorScheme,
    isDark: Boolean,
    dynamicColorActive: Boolean,
    highContrast: Boolean
): MengiExtendedColors {
    val base = if (isDark) DarkExtendedColors else LightExtendedColors
    val minRatio = if (highContrast) ContrastAccessibility.WCAG_AAA_NORMAL else ContrastAccessibility.WCAG_AA_NORMAL

    val income = ContrastAccessibility.ensureAccessibleForeground(base.income, base.incomeSoft, minRatio)
    val expense = ContrastAccessibility.ensureAccessibleForeground(base.expense, base.expenseSoft, minRatio)
    val warning = ContrastAccessibility.ensureAccessibleForeground(base.warning, base.warningSoft, minRatio)
    val savings = ContrastAccessibility.ensureAccessibleForeground(base.savings, base.savingsSoft, minRatio)

    if (!dynamicColorActive) {
        return base.copy(
            income = income,
            expense = expense,
            warning = warning,
            savings = savings,
            cardBorder = scheme.outline,
            subtleSurface = scheme.surfaceVariant
        )
    }

    val heroStart = lerp(Color(0xFF0A0F1D), scheme.primary, 0.22f)
    val heroMid = lerp(Color(0xFF10192D), scheme.primary, 0.42f)
    val heroEnd = lerp(Color(0xFF162440), scheme.primary, 0.68f)

    return base.copy(
        heroStart = heroStart,
        heroMid = heroMid,
        heroEnd = heroEnd,
        heroAccentGlow = scheme.primaryContainer,
        heroGoldGlow = scheme.secondaryContainer,
        lime = ContrastAccessibility.ensureAccessibleForeground(scheme.primaryContainer, heroStart, 4.5f),
        limeSoft = scheme.primaryContainer,
        limeInk = scheme.onPrimaryContainer,
        income = income,
        expense = expense,
        warning = warning,
        savings = savings,
        cardBorder = scheme.outline,
        subtleSurface = scheme.surfaceVariant
    )
}

@Composable
private fun animateColorScheme(target: ColorScheme): ColorScheme {
    val spec = tween<Color>(durationMillis = 240)
    val primary by animateColorAsState(target.primary, spec, label = "theme_primary")
    val onPrimary by animateColorAsState(target.onPrimary, spec, label = "theme_onPrimary")
    val primaryContainer by animateColorAsState(target.primaryContainer, spec, label = "theme_primaryContainer")
    val onPrimaryContainer by animateColorAsState(target.onPrimaryContainer, spec, label = "theme_onPrimaryContainer")
    val background by animateColorAsState(target.background, spec, label = "theme_background")
    val onBackground by animateColorAsState(target.onBackground, spec, label = "theme_onBackground")
    val surface by animateColorAsState(target.surface, spec, label = "theme_surface")
    val onSurface by animateColorAsState(target.onSurface, spec, label = "theme_onSurface")
    val surfaceVariant by animateColorAsState(target.surfaceVariant, spec, label = "theme_surfaceVariant")
    val onSurfaceVariant by animateColorAsState(target.onSurfaceVariant, spec, label = "theme_onSurfaceVariant")
    val outline by animateColorAsState(target.outline, spec, label = "theme_outline")

    return target.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        outline = outline
    )
}

/**
 * Dynamic One UI 8.5 Theme Provider supporting System, Light, and Dark modes,
 * optional Android 12+ Material You dynamic color, and WCAG AA/AAA contrast enforcement.
 */
@Composable
fun MengidataTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColorEnabled: Boolean = false,
    highContrastEnabled: Boolean = false,
    onSetThemeMode: (ThemeMode) -> Unit = {},
    onSetDynamicColor: (Boolean) -> Unit = {},
    onSetHighContrast: (Boolean) -> Unit = {},
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val context = LocalContext.current
    val dynamicSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val useDynamic = dynamicColorEnabled && dynamicSupported

    val baseScheme = remember(isDark, useDynamic, context) {
        if (useDynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            if (isDark) MengiDarkColorScheme else MengiLightColorScheme
        }
    }

    val accessibleScheme = remember(baseScheme, isDark, highContrastEnabled) {
        ContrastAccessibility.enforceAccessibleColorScheme(
            scheme = baseScheme,
            isDark = isDark,
            highContrast = highContrastEnabled
        )
    }

    val animatedScheme = animateColorScheme(accessibleScheme)

    val extended = remember(accessibleScheme, isDark, useDynamic, highContrastEnabled) {
        deriveExtendedColors(
            scheme = accessibleScheme,
            isDark = isDark,
            dynamicColorActive = useDynamic,
            highContrast = highContrastEnabled
        )
    }

    val themeState = remember(
        themeMode,
        isDark,
        dynamicColorEnabled,
        dynamicSupported,
        highContrastEnabled,
        onSetThemeMode,
        onSetDynamicColor,
        onSetHighContrast
    ) {
        MengiThemeState(
            themeMode = themeMode,
            isDark = isDark,
            dynamicColorEnabled = dynamicColorEnabled,
            dynamicColorSupported = dynamicSupported,
            highContrastEnabled = highContrastEnabled,
            onSetThemeMode = onSetThemeMode,
            onSetDynamicColor = onSetDynamicColor,
            onSetHighContrast = onSetHighContrast
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !isDark
            controller.isAppearanceLightNavigationBars = !isDark
        }
    }

    CompositionLocalProvider(
        LocalMengiColors provides extended,
        LocalMengiThemeState provides themeState
    ) {
        MaterialTheme(
            colorScheme = animatedScheme,
            typography = MengiTypography,
            shapes = MengiShapes,
            content = content
        )
    }
}
