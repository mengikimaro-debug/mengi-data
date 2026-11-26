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

// Android 17 Matte Material 3 Expressive Tonal Palette (Zero Glare / Zero Glow, WCAG AA/AAA Verified)
private val M3LightCanvas = Color(0xFFF3F2EE)          // Warm Matte Stone
private val M3LightSurface = Color(0xFFFAF9F6)         // Soft Matte Paper
private val M3LightContainerLow = Color(0xFFECEAE3)    // Matte Stone Container Low
private val M3LightContainer = Color(0xFFE4E1D9)       // Matte Container
private val M3LightContainerHigh = Color(0xFFDCD8CF)   // Matte Container High
private val M3LightContainerHighest = Color(0xFFD2CEC4)
private val M3LightOutline = Color(0xFFB7B2A8)
private val M3LightOutlineVariant = Color(0xFFD5D1C8)

private val M3DarkCanvas = Color(0xFF111413)           // Deep Matte Charcoal
private val M3DarkSurface = Color(0xFF181C1A)          // Matte Dark Slate-Pine
private val M3DarkContainerLow = Color(0xFF1D2220)
private val M3DarkContainer = Color(0xFF222825)
private val M3DarkContainerHigh = Color(0xFF2A312D)
private val M3DarkContainerHighest = Color(0xFF323B36)
private val M3DarkOutline = Color(0xFF424B46)
private val M3DarkOutlineVariant = Color(0xFF2C332F)

private val InkDark = Color(0xFF191D1B)
private val InkMutedLight = Color(0xFF4E5652)
private val InkSnow = Color(0xFFEBE9E3)
private val InkMutedDark = Color(0xFFB3BCB7)

private val PinePrimaryLight = Color(0xFF2A5248)         // Matte Deep Forest Sage
private val PineContainerLight = Color(0xFFCFE0DB)
private val PineOnContainerLight = Color(0xFF122924)

private val PinePrimaryDark = Color(0xFF9AC2B6)          // Matte Soft Sage
private val PineOnPrimaryDark = Color(0xFF0E2922)
private val PineContainerDark = Color(0xFF23473E)
private val PineOnContainerDark = Color(0xFFCFE0DB)

private val SlateSecondaryLight = Color(0xFF4B5966)      // Matte Cool Slate
private val SlateContainerLight = Color(0xFFD7DFE6)
private val SlateOnContainerLight = Color(0xFF1D2730)

private val SlateSecondaryDark = Color(0xFFB2C2CF)
private val SlateOnSecondaryDark = Color(0xFF1D2933)
private val SlateContainerDark = Color(0xFF34424E)
private val SlateOnContainerDark = Color(0xFFD7DFE6)

private val ClayTertiaryLight = Color(0xFF73513E)        // Matte Warm Terracotta Clay
private val ClayContainerLight = Color(0xFFEBDDD4)
private val ClayOnContainerLight = Color(0xFF331E14)

private val ClayTertiaryDark = Color(0xFFCFA994)
private val ClayOnTertiaryDark = Color(0xFF382115)
private val ClayContainerDark = Color(0xFF54392B)
private val ClayOnContainerDark = Color(0xFFEBDDD4)

private val OchreWarningLight = Color(0xFF8A5318)
private val OchreWarningSoftLight = Color(0xFFF3E6D6)
private val OchreWarningDark = Color(0xFFDFB17A)
private val OchreWarningSoftDark = Color(0xFF342717)

private val SageIncomeLight = Color(0xFF1F684A)
private val SageIncomeSoftLight = Color(0xFFD7EAE0)
private val SageIncomeDark = Color(0xFF7DC29E)
private val SageIncomeSoftDark = Color(0xFF183024)

private val TerraExpenseLight = Color(0xFF983942)
private val TerraExpenseSoftLight = Color(0xFFF4DFE1)
private val TerraExpenseDark = Color(0xFFDF9EA4)
private val TerraExpenseSoftDark = Color(0xFF361E21)

private val MatteSavingsLight = Color(0xFF514C78)
private val MatteSavingsSoftLight = Color(0xFFE2E0ED)
private val MatteSavingsDark = Color(0xFFB9B4D8)
private val MatteSavingsSoftDark = Color(0xFF252338)

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
    heroStart = Color(0xFF1E2B27),
    heroMid = Color(0xFF23322E),
    heroEnd = Color(0xFF273833),
    heroAccentGlow = Color(0xFF23322E),
    heroGoldGlow = Color(0xFF273833),
    lime = Color(0xFFC4DDD5),
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
    dockBorder = M3LightOutlineVariant
)

private val DarkExtendedColors = MengiExtendedColors(
    isDark = true,
    heroStart = Color(0xFF1C2623),
    heroMid = Color(0xFF202C28),
    heroEnd = Color(0xFF24322E),
    heroAccentGlow = Color(0xFF202C28),
    heroGoldGlow = Color(0xFF24322E),
    lime = Color(0xFFC4DDD5),
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
    subtleSurface = M3DarkContainerHigh,
    glassHighlight = M3DarkContainerHighest,
    dockSurface = M3DarkContainer,
    dockBorder = M3DarkOutline
)

val LocalMengiColors = staticCompositionLocalOf { LightExtendedColors }

internal val MengiLightColorScheme = lightColorScheme(
    primary = PinePrimaryLight,
    onPrimary = Color(0xFFF7F7F4),
    primaryContainer = PineContainerLight,
    onPrimaryContainer = PineOnContainerLight,
    secondary = SlateSecondaryLight,
    onSecondary = Color(0xFFF7F7F4),
    secondaryContainer = SlateContainerLight,
    onSecondaryContainer = SlateOnContainerLight,
    tertiary = ClayTertiaryLight,
    onTertiary = Color(0xFFF7F7F4),
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
    onError = Color(0xFFFDFBFA),
    errorContainer = TerraExpenseSoftLight,
    onErrorContainer = Color(0xFF531A20)
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
    surfaceContainerLowest = Color(0xFF141816),
    surfaceContainerLow = M3DarkContainerLow,
    surfaceContainer = M3DarkContainer,
    surfaceContainerHigh = M3DarkContainerHigh,
    surfaceContainerHighest = M3DarkContainerHighest,
    outline = M3DarkOutline,
    outlineVariant = M3DarkOutlineVariant,
    error = TerraExpenseDark,
    onError = Color(0xFF47161B),
    errorContainer = TerraExpenseSoftDark,
    onErrorContainer = Color(0xFFF7DCDF)
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
        lerp(Color(0xFF141816), scheme.primaryContainer, 0.30f)
    } else {
        lerp(Color(0xFF1E2623), scheme.primary, 0.42f)
    }
    val heroMid = if (isDark) {
        lerp(Color(0xFF181E1B), scheme.primary, 0.22f)
    } else {
        lerp(Color(0xFF222D29), scheme.primary, 0.48f)
    }
    val heroEnd = if (isDark) {
        lerp(Color(0xFF1A2220), scheme.secondary, 0.22f)
    } else {
        lerp(Color(0xFF25322F), scheme.secondary, 0.40f)
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
