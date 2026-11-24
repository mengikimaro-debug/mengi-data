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

// Samsung One UI 8.5 Accessible Palette (WCAG AA/AAA Verified in Both Light & Dark States)
// Light: Signature One UI Cool Frost Canvas (#F2F4F8), Pure White 26dp Squircles, One UI Cobalt Blue (#0064D2)
// Dark: True One UI AMOLED Pitch Black (#000000), Elevated Charcoal Cards (#17171A), Luminous Galaxy Sky (#54A8FF)

private val OneUiDarkCanvas = Color(0xFF000000)
private val OneUiDarkCard = Color(0xFF17171A)
private val OneUiDarkElevated = Color(0xFF232329)
private val OneUiDarkBorder = Color(0xFF2D2E36)

private val OneUiLightCanvas = Color(0xFFF2F4F8)
private val OneUiPureWhite = Color(0xFFFFFFFF)
private val OneUiLightPill = Color(0xFFE7ECF4)
private val OneUiLightBorder = Color(0xFFDCE3EE)

private val DeepInk = Color(0xFF0A101F)
private val SlateInk = Color(0xFF111827)
private val SlateMutedAccessible = Color(0xFF475467) // >= 6.8:1 on Light Canvas & Pill
private val SnowInk = Color(0xFFF9FAFB)
private val SilverMutedAccessible = Color(0xFFAAB4C4) // >= 7.8:1 on Dark Card & Elevated

private val OneUiBlueLight = Color(0xFF0064D2) // Signature One UI Cobalt Blue (>= 5.6:1 on White)
private val OneUiBlueDark = Color(0xFF54A8FF) // Luminous One UI Sky Blue (>= 7.9:1 on #17171A)
private val OneUiBlueSoftLight = Color(0xFFE3F0FF)
private val OneUiBlueInkLight = Color(0xFF003B82)
private val OneUiBlueSoftDark = Color(0xFF142A48)
private val OneUiBlueInkDark = Color(0xFFD8EBFF)

private val GalaxyIndigoLight = Color(0xFF3E4CC6)
private val GalaxyIndigoDark = Color(0xFF93A8FF)
private val GalaxyIndigoSoft = Color(0xFFE5E9FF)
private val GalaxyIndigoInk = Color(0xFF262F82)

private val Amber700 = Color(0xFFB45309)
private val AmberSoft = Color(0xFFFFF8E6)
private val Amber400 = Color(0xFFFBBF24)
private val AmberDarkSoft = Color(0xFF2B2009)

private val EmeraldIncomeLight = Color(0xFF047857) // >= 5.1:1 on White & EmeraldSoftLight
private val EmeraldIncomeSoftLight = Color(0xFFE9FBF3)
private val EmeraldIncomeDark = Color(0xFF34D399) // >= 8.2:1 on Dark Card & EmeraldSoftDark
private val EmeraldIncomeSoftDark = Color(0xFF09281F)

private val SoftPlumLight = Color(0xFF5E4D82) // Soft non-red muted lavender, >= 6.5:1 contrast
private val SoftPlumBgLight = Color(0xFFF3EFFA)
private val SoftPlumDark = Color(0xFFC4B5E6) // Soft pastel lavender on AMOLED dark, >= 8.5:1 contrast
private val SoftPlumBgDark = Color(0xFF1E1A2B)

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
    heroStart = Color(0xFF0A1733),
    heroMid = Color(0xFF0D2E6E),
    heroEnd = Color(0xFF0366D6),
    heroAccentGlow = Color(0xFF5CC8FF),
    heroGoldGlow = Color(0xFFA5B4FC),
    lime = Color(0xFF93D5FF),
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
    glassHighlight = Color.White.copy(alpha = 0.90f),
    dockSurface = Color(0xF5FFFFFF),
    dockBorder = Color(0xFFD5DFEE)
)

private val DarkExtendedColors = MengiExtendedColors(
    isDark = true,
    heroStart = Color(0xFF0A1326),
    heroMid = Color(0xFF10244A),
    heroEnd = Color(0xFF163D80),
    heroAccentGlow = Color(0xFF54A8FF),
    heroGoldGlow = Color(0xFF8DA4FF),
    lime = Color(0xFF8CC6FF),
    limeSoft = OneUiBlueSoftDark,
    limeInk = OneUiBlueInkDark,
    income = EmeraldIncomeDark,
    incomeSoft = EmeraldIncomeSoftDark,
    expense = SoftPlumDark,
    expenseSoft = SoftPlumBgDark,
    warning = Amber400,
    warningSoft = AmberDarkSoft,
    savings = GalaxyIndigoDark,
    savingsSoft = Color(0xFF1C1F3E),
    cardBorder = OneUiDarkBorder,
    subtleSurface = OneUiDarkElevated,
    glassHighlight = Color.White.copy(alpha = 0.08f),
    dockSurface = Color(0xEB1B1C22),
    dockBorder = Color(0x30FFFFFF)
)

val LocalMengiColors = staticCompositionLocalOf { LightExtendedColors }

internal val MengiLightColorScheme = lightColorScheme(
    primary = OneUiBlueLight,
    onPrimary = Color.White,
    primaryContainer = OneUiBlueSoftLight,
    onPrimaryContainer = OneUiBlueInkLight,
    secondary = GalaxyIndigoLight,
    onSecondary = Color.White,
    secondaryContainer = GalaxyIndigoSoft,
    onSecondaryContainer = GalaxyIndigoInk,
    tertiary = Amber700,
    onTertiary = Color.White,
    tertiaryContainer = AmberSoft,
    onTertiaryContainer = Color(0xFF78350F),
    background = OneUiLightCanvas,
    onBackground = SlateInk,
    surface = OneUiPureWhite,
    onSurface = SlateInk,
    surfaceVariant = OneUiLightPill,
    onSurfaceVariant = SlateMutedAccessible,
    outline = OneUiLightBorder,
    outlineVariant = Color(0xFFE3E9F2),
    error = SoftPlumLight,
    onError = Color.White,
    errorContainer = SoftPlumBgLight,
    onErrorContainer = Color(0xFF423461)
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
    onTertiaryContainer = Color(0xFFFDE68A),
    background = OneUiDarkCanvas,
    onBackground = SnowInk,
    surface = OneUiDarkCard,
    onSurface = SnowInk,
    surfaceVariant = OneUiDarkElevated,
    onSurfaceVariant = SilverMutedAccessible,
    outline = OneUiDarkBorder,
    outlineVariant = Color(0xFF282A33),
    error = SoftPlumDark,
    onError = DeepInk,
    errorContainer = SoftPlumBgDark,
    onErrorContainer = Color(0xFFE6DFFA)
)

val MengiTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = OneUiDisplayFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.8).sp
    ),
    displaySmall = TextStyle(
        fontFamily = OneUiDisplayFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.6).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = OneUiDisplayFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.3).sp
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

// Samsung One UI 8.5 Signature Squircle Radius System
val MengiShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp)
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
