package tz.mengidata.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeContrastTest {

    private fun assertSchemeMeetsTarget(scheme: ColorScheme, minRatio: Float, label: String) {
        val pairs = listOf(
            "onBackground/background" to (scheme.onBackground to scheme.background),
            "onSurface/surface" to (scheme.onSurface to scheme.surface),
            "onSurfaceVariant/surfaceVariant" to (scheme.onSurfaceVariant to scheme.surfaceVariant),
            "onPrimary/primary" to (scheme.onPrimary to scheme.primary),
            "onPrimaryContainer/primaryContainer" to (scheme.onPrimaryContainer to scheme.primaryContainer),
            "onSecondary/secondary" to (scheme.onSecondary to scheme.secondary),
            "onSecondaryContainer/secondaryContainer" to (scheme.onSecondaryContainer to scheme.secondaryContainer),
            "onError/error" to (scheme.onError to scheme.error),
            "onErrorContainer/errorContainer" to (scheme.onErrorContainer to scheme.errorContainer),
            "primary/surface" to (scheme.primary to scheme.surface)
        )
        pairs.forEach { (pairName, colors) ->
            val ratio = ContrastAccessibility.contrastRatio(colors.first, colors.second)
            assertTrue(
                "[$label] $pairName contrast ratio $ratio is below $minRatio",
                ratio >= minRatio
            )
        }
    }

    @Test
    fun lightScheme_meetsWcagAA_standard_and_AAA_highContrast() {
        val standard = ContrastAccessibility.enforceAccessibleColorScheme(
            scheme = MengiLightColorScheme,
            isDark = false,
            highContrast = false
        )
        assertSchemeMeetsTarget(standard, ContrastAccessibility.WCAG_AA_NORMAL, "Light Standard")

        val highContrast = ContrastAccessibility.enforceAccessibleColorScheme(
            scheme = MengiLightColorScheme,
            isDark = false,
            highContrast = true
        )
        assertSchemeMeetsTarget(highContrast, ContrastAccessibility.WCAG_AAA_NORMAL, "Light High Contrast")
    }

    @Test
    fun darkScheme_meetsWcagAA_standard_and_AAA_highContrast() {
        val standard = ContrastAccessibility.enforceAccessibleColorScheme(
            scheme = MengiDarkColorScheme,
            isDark = true,
            highContrast = false
        )
        assertSchemeMeetsTarget(standard, ContrastAccessibility.WCAG_AA_NORMAL, "Dark Standard")

        val highContrast = ContrastAccessibility.enforceAccessibleColorScheme(
            scheme = MengiDarkColorScheme,
            isDark = true,
            highContrast = true
        )
        assertSchemeMeetsTarget(highContrast, ContrastAccessibility.WCAG_AAA_NORMAL, "Dark High Contrast")
    }

    @Test
    fun lowContrastDynamicInput_isAutomaticallyCorrected() {
        val poorDynamic = MengiLightColorScheme.copy(
            onSurfaceVariant = Color(0xFFB0B8C4),
            primary = Color(0xFF93C5FD),
            onPrimary = Color(0xFFEFF6FF)
        )
        val corrected = ContrastAccessibility.enforceAccessibleColorScheme(
            scheme = poorDynamic,
            isDark = false,
            highContrast = false
        )
        assertSchemeMeetsTarget(corrected, ContrastAccessibility.WCAG_AA_NORMAL, "Corrected Dynamic Light")
    }
}
