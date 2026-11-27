package tz.mengidata.app.ui.theme

import android.app.Activity
import android.os.Build
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

// Android 17 Material 3 Expressive Typography Pairing:
// Space Grotesk (geometric expressive display & figures) + DM Sans (crisp humanist UI body & labels)
val ExpressiveDisplayFamily = FontFamily(
    Font(R.font.space_grotesk, FontWeight.Normal),
    Font(R.font.space_grotesk, FontWeight.Medium),
    Font(R.font.space_grotesk, FontWeight.SemiBold),
    Font(R.font.space_grotesk, FontWeight.Bold)
)

val ExpressiveBodyFamily = FontFamily(
    Font(R.font.dm_sans, FontWeight.Normal),
    Font(R.font.dm_sans, FontWeight.Medium),
    Font(R.font.dm_sans, FontWeight.SemiBold),
    Font(R.font.dm_sans, FontWeight.Bold)
)

// Android 17 Matte Material 3 Expressive Tonal Palette (Zero Green / Zero Glare / Zero Glow, WCAG AA/AAA Verified)
// Light Mode Matte Palette:
// Calm Warm Stone Off-White Canvas (#F3F1EC) & Matte Alabaster Cards (#FAF8F4)
// Paired with Deep Matte Indigo-Navy (#253447) & Slate Blue (#3A5270) — zero green, zero glare
private val M3LightCanvas = Color(0xFFF3F1EC)          // Calm Matte Warm Stone Canvas (Zero Green)
private val M3LightSurface = Color(0xFFFAF8F4)         // Soft Matte Alabaster Card Surface
private val M3LightContainerLow = Color(0xFFECE9E2)    // Matte Stone Container Low
private val M3LightContainer = Color(0xFFE3DFD7)       // Matte Stone Container
private val M3LightContainerHigh = Color(0xFFDAD5CC)   // Matte Stone Container High
private val M3LightContainerHighest = Color(0xFFD0CAC0)
private val M3LightOutline = Color(0xFF778190)         // Muted Matte Slate-Blue Outline
private val M3LightOutlineVariant = Color(0xFFD2D5DC)  // Soft Matte Slate Card Border

// Dark Mode Matte Palette:
// Calm Matte Charcoal-Obsidian Canvas (#11141A) & Matte Graphite-Navy Cards (#171C24)
// Paired with Muted Matte Steel-Blue (#324A66 / #82A6CC) & Soft Matte Amber-Bronze (#C99266) — zero green, zero neon
private val M3DarkCanvas = Color(0xFF11141A)           // Calm Matte Charcoal-Midnight Background
private val M3DarkSurface = Color(0xFF171C24)          // Matte Graphite-Navy Card Surface
private val M3DarkContainerLow = Color(0xFF1C222C)     // Matte Slate-Navy Container Low
private val M3DarkContainer = Color(0xFF212834)        // Matte Slate-Navy Container
private val M3DarkContainerHigh = Color(0xFF27303E)    // Matte Slate-Navy Container High
private val M3DarkContainerHighest = Color(0xFF2F3A4A) // Highest Matte Container
private val M3DarkOutline = Color(0xFF4C596B)          // Calm Matte Steel Outline
private val M3DarkOutlineVariant = Color(0xFF252D3A)   // Subtle Matte Card Border

private val InkDark = Color(0xFF1A222E)                // Deep Matte Navy-Charcoal Ink (Zero Green)
private val InkMutedLight = Color(0xFF4A5565)          // Calm Matte Slate-Navy Secondary Text
private val InkSnow = Color(0xFFE8ECF2)                // Soft Matte Alabaster Text
private val InkMutedDark = Color(0xFFA8B3C2)           // Muted Silver-Slate Secondary Text

private val PinePrimaryLight = Color(0xFF253447)       // Calm Matte Deep Indigo-Navy (Zero Green)
private val PineContainerLight = Color(0xFFD9E1EC)     // Soft Matte Slate-Blue Pill Container
private val PineOnContainerLight = Color(0xFF182433)   // Deep Matte Navy Ink

private val PinePrimaryDark = Color(0xFF84A7CC)        // Calm Matte Steel Blue (Non-glowing, Zero Green)
private val PineOnPrimaryDark = Color(0xFF101A26)
private val PineContainerDark = Color(0xFF2B425B)      // Matte Steel-Blue Active Selection Pill
private val PineOnContainerDark = Color(0xFFEBF0F7)    // Soft Matte Alabaster on Active Pill

private val SlateSecondaryLight = Color(0xFF394D66)    // Muted Matte Fjord Blue Secondary
private val SlateContainerLight = Color(0xFFDFE5EE)
private val SlateOnContainerLight = Color(0xFF1B2838)

private val SlateSecondaryDark = Color(0xFF9AB4D0)     // Muted Matte Powder-Steel Secondary
private val SlateOnSecondaryDark = Color(0xFF13202E)
private val SlateContainerDark = Color(0xFF243548)
private val SlateOnContainerDark = Color(0xFFE5EDF7)

private val ClayTertiaryLight = Color(0xFF73533D)      // Warm Matte Bronze-Clay Accent for Light Mode
private val ClayContainerLight = Color(0xFFEBE1D8)
private val ClayOnContainerLight = Color(0xFF2E1E14)

private val ClayTertiaryDark = Color(0xFFD1966A)       // Soft Matte Warm Amber-Bronze
private val ClayOnTertiaryDark = Color(0xFF24150A)
private val ClayContainerDark = Color(0xFF3D2A1D)      // Matte Warm Bronze Badge Container
private val ClayOnContainerDark = Color(0xFFF2DFD0)

private val OchreWarningLight = Color(0xFF825626)
private val OchreWarningSoftLight = Color(0xFFEDE2D3)
private val OchreWarningDark = Color(0xFFD6A26D)       // Matte Amber-Sand Warning Font
private val OchreWarningSoftDark = Color(0xFF332619)   // Matte Warm Umber Surface

// Positive / Income tokens: Muted Matte Royal Slate-Blue (Zero Green anywhere)
private val SageIncomeLight = Color(0xFF2C4C70)        // Matte Royal Slate-Blue Income in Light Mode (No Green)
private val SageIncomeSoftLight = Color(0xFFDCE5F0)
private val SageIncomeDark = Color(0xFF82AEE0)         // Matte Sky-Steel Blue Income in Dark Mode (No Green)
private val SageIncomeSoftDark = Color(0xFF1B2B3D)

private val TerraExpenseLight = Color(0xFF8C4349)      // Muted Matte Rosewood Expense in Light Mode
private val TerraExpenseSoftLight = Color(0xFFEEDCE0)
private val TerraExpenseDark = Color(0xFFD48A88)       // Muted Matte Terracotta-Rose in Dark Mode
private val TerraExpenseSoftDark = Color(0xFF342123)

private val MatteSavingsLight = Color(0xFF4E446B)      // Muted Matte Indigo-Plum Savings in Light Mode
private val MatteSavingsSoftLight = Color(0xFFE3DFEC)
private val MatteSavingsDark = Color(0xFFA89CC8)       // Muted Matte Lavender-Slate Savings in Dark Mode
private val MatteSavingsSoftDark = Color(0xFF262235)

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
    heroStart = Color(0xFF1F2B3A),
    heroMid = Color(0xFF263547),
    heroEnd = Color(0xFF2E3F54),
    heroAccentGlow = Color(0xFF263547),
    heroGoldGlow = Color(0xFF2E3F54),
    lime = Color(0xFFD8E1EC),
    limeSoft = PineContainerLight,
    limeInk = PineOnContainerLight,
    income = SageIncomeLight,
    incomeSoft = SageIncomeSoftLight,
    expense = TerraExpenseLight,
    expenseSoft = TerraExpenseSoftLight,
    warning = OchreWarningLight,
    warningSoft = OchreWarningSoftLight,
    savings = MatteSavingsLight,
    savingsSoft = MatteSavingsSoftLight,
    cardBorder = M3LightOutlineVariant,
    subtleSurface = M3LightContainerLow,
    glassHighlight = M3LightContainerHigh,
    dockSurface = M3LightSurface,
    dockBorder = Color(0xFFC6C9D0)
)

private val DarkExtendedColors = MengiExtendedColors(
    isDark = true,
    heroStart = Color(0xFF18202B),
    heroMid = Color(0xFF1E2937),
    heroEnd = Color(0xFF253244),
    heroAccentGlow = Color(0xFF334A63),
    heroGoldGlow = Color(0xFFC89065),
    lime = Color(0xFFD19A72),
    limeSoft = PineContainerDark,
    limeInk = PineOnContainerDark,
    income = SageIncomeDark,
    incomeSoft = SageIncomeSoftDark,
    expense = TerraExpenseDark,
    expenseSoft = TerraExpenseSoftDark,
    warning = OchreWarningDark,
    warningSoft = OchreWarningSoftDark,
    savings = MatteSavingsDark,
    savingsSoft = MatteSavingsSoftDark,
    cardBorder = M3DarkOutlineVariant,
    subtleSurface = M3DarkContainerLow,
    glassHighlight = M3DarkContainerHigh,
    dockSurface = Color(0xFF161B23),
    dockBorder = Color(0xFF2B3442)
)

val LocalMengiColors = staticCompositionLocalOf { LightExtendedColors }

internal val MengiLightColorScheme = lightColorScheme(
    primary = PinePrimaryLight,
    onPrimary = Color(0xFFF6F5F2),
    primaryContainer = PineContainerLight,
    onPrimaryContainer = PineOnContainerLight,
    secondary = SlateSecondaryLight,
    onSecondary = Color(0xFFF6F5F2),
    secondaryContainer = SlateContainerLight,
    onSecondaryContainer = SlateOnContainerLight,
    tertiary = ClayTertiaryLight,
    onTertiary = Color(0xFFF6F5F2),
    tertiaryContainer = ClayContainerLight,
    onTertiaryContainer = ClayOnContainerLight,
    background = M3LightCanvas,
    onBackground = InkDark,
    surface = M3LightSurface,
    onSurface = InkDark,
    surfaceVariant = M3LightContainerHigh,
    onSurfaceVariant = InkMutedLight,
    surfaceContainerLowest = M3LightSurface,
    surfaceContainerLow = M3LightContainerLow,
    surfaceContainer = M3LightContainer,
    surfaceContainerHigh = M3LightContainerHigh,
    surfaceContainerHighest = M3LightContainerHighest,
    outline = M3LightOutline,
    outlineVariant = M3LightOutlineVariant,
    error = TerraExpenseLight,
    onError = Color(0xFFFAF8F4),
    errorContainer = TerraExpenseSoftLight,
    onErrorContainer = Color(0xFF4D1D22)
)

internal val MengiDarkColorScheme = darkColorScheme(
    primary = PinePrimaryDark,
    onPrimary = PineOnPrimaryDark,
    primaryContainer = PineContainerDark,
    onPrimaryContainer = PineOnContainerDark,
    secondary = SlateSecondaryDark,
    onSecondary = SlateOnSecondaryDark,
    secondaryContainer = SlateContainerDark,
    onSecondaryContainer = SlateOnContainerDark,
    tertiary = ClayTertiaryDark,
    onTertiary = ClayOnTertiaryDark,
    tertiaryContainer = ClayContainerDark,
    onTertiaryContainer = ClayOnContainerDark,
    background = M3DarkCanvas,
    onBackground = InkSnow,
    surface = M3DarkSurface,
    onSurface = InkSnow,
    surfaceVariant = M3DarkContainerHigh,
    onSurfaceVariant = InkMutedDark,
    surfaceContainerLowest = Color(0xFF0D1015),
    surfaceContainerLow = M3DarkContainerLow,
    surfaceContainer = M3DarkContainer,
    surfaceContainerHigh = M3DarkContainerHigh,
    surfaceContainerHighest = M3DarkContainerHighest,
    outline = M3DarkOutline,
    outlineVariant = M3DarkOutlineVariant,
    error = Color(0xFFD98A8A),
    onError = Color(0xFF2E1113),
    errorContainer = Color(0xFF3B1A1D),
    onErrorContainer = Color(0xFFF2D5D7)
)

val MengiTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = ExpressiveDisplayFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 37.sp,
        lineHeight = 43.sp,
        letterSpacing = (-1.1).sp
    ),
    displaySmall = TextStyle(
        fontFamily = ExpressiveDisplayFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.75).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = ExpressiveDisplayFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.4).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = ExpressiveDisplayFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.25).sp
    ),
    titleLarge = TextStyle(
        fontFamily = ExpressiveDisplayFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.15).sp
    ),
    titleMedium = TextStyle(
        fontFamily = ExpressiveBodyFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.5.sp,
        lineHeight = 21.sp
    ),
    titleSmall = TextStyle(
        fontFamily = ExpressiveBodyFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.5.sp,
        lineHeight = 18.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = ExpressiveBodyFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = ExpressiveBodyFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp,
        lineHeight = 19.sp
    ),
    bodySmall = TextStyle(
        fontFamily = ExpressiveBodyFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp
    ),
    labelLarge = TextStyle(
        fontFamily = ExpressiveBodyFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
        fontFamily = ExpressiveBodyFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.5.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp
    ),
    labelSmall = TextStyle(
        fontFamily = ExpressiveBodyFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 10.5.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.7.sp
    )
)

// Android 17 Material 3 Expressive Shape Tokens
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
            cardBorder = scheme.outlineVariant,
            subtleSurface = scheme.surfaceContainerLow
        )
    }

    val heroStart = if (isDark) {
        lerp(Color(0xFF141B24), scheme.primaryContainer, 0.30f)
    } else {
        lerp(Color(0xFF1F2733), scheme.primary, 0.42f)
    }
    val heroMid = if (isDark) {
        lerp(Color(0xFF19222E), scheme.primary, 0.22f)
    } else {
        lerp(Color(0xFF242E3B), scheme.primary, 0.48f)
    }
    val heroEnd = if (isDark) {
        lerp(Color(0xFF1E2938), scheme.secondary, 0.22f)
    } else {
        lerp(Color(0xFF293545), scheme.secondary, 0.40f)
    }

    return base.copy(
        heroStart = heroStart,
        heroMid = heroMid,
        heroEnd = heroEnd,
        heroAccentGlow = heroMid,
        heroGoldGlow = heroEnd,
        lime = ContrastAccessibility.ensureAccessibleForeground(scheme.primaryContainer, heroStart, 4.5f),
        limeSoft = scheme.primaryContainer,
        limeInk = scheme.onPrimaryContainer,
        income = income,
        expense = expense,
        warning = warning,
        savings = savings,
        cardBorder = scheme.outlineVariant,
        subtleSurface = scheme.surfaceContainerLow,
        glassHighlight = scheme.surfaceContainerHigh,
        dockSurface = scheme.surfaceContainer,
        dockBorder = scheme.outlineVariant
    )
}

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
    val systemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> systemDark
    }

    val context = LocalContext.current
    val dynamicSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val useDynamic = dynamicColorEnabled && dynamicSupported

    val baseScheme = remember(isDark, useDynamic) {
        when {
            useDynamic && isDark -> dynamicDarkColorScheme(context)
            useDynamic && !isDark -> dynamicLightColorScheme(context)
            isDark -> MengiDarkColorScheme
            else -> MengiLightColorScheme
        }
    }

    val accessibleScheme = remember(baseScheme, isDark, highContrastEnabled) {
        ContrastAccessibility.enforceAccessibleColorScheme(
            scheme = baseScheme,
            isDark = isDark,
            highContrast = highContrastEnabled
        )
    }

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
            colorScheme = accessibleScheme,
            typography = MengiTypography,
            shapes = MengiShapes,
            content = content
        )
    }
}
