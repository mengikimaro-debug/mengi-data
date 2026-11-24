package tz.mengidata.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min

/**
 * WCAG 2.1 color contrast utilities and automatic ColorScheme accessibility guardrail.
 * Ensures foreground/background pairs meet AA (>= 4.5:1) or High Contrast AAA (>= 7.0:1)
 * across both Light and Dark modes, including Android 12+ Material You dynamic palettes.
 */
object ContrastAccessibility {

    const val WCAG_AA_NORMAL = 4.5f
    const val WCAG_AA_LARGE = 3.0f
    const val WCAG_AAA_NORMAL = 7.0f

    private val PureWhite = Color(0xFFFFFFFF)
    private val DeepInk = Color(0xFF090D16)

    /**
     * Computes exact WCAG 2.1 contrast ratio between two opaque colors (range 1.0..21.0).
     */
    fun contrastRatio(foreground: Color, background: Color): Float {
        val fgLum = foreground.luminance()
        val bgLum = background.luminance()
        val lighter = max(fgLum, bgLum)
        val darker = min(fgLum, bgLum)
        return (lighter + 0.05f) / (darker + 0.05f)
    }

    fun meetsWcagAA(foreground: Color, background: Color): Boolean =
        contrastRatio(foreground, background) >= WCAG_AA_NORMAL

    fun meetsWcagAALarge(foreground: Color, background: Color): Boolean =
        contrastRatio(foreground, background) >= WCAG_AA_LARGE

    fun meetsWcagAAA(foreground: Color, background: Color): Boolean =
        contrastRatio(foreground, background) >= WCAG_AAA_NORMAL

    /**
     * Returns [preferred] if it already achieves [minRatio] against [background].
     * Otherwise shifts [preferred] smoothly toward the higher-contrast extreme (white or deep ink)
     * until [minRatio] is satisfied, preserving hue character as much as possible.
     */
    fun ensureAccessibleForeground(
        preferred: Color,
        background: Color,
        minRatio: Float = WCAG_AA_NORMAL
    ): Color {
        if (contrastRatio(preferred, background) >= minRatio) return preferred

        val whiteRatio = contrastRatio(PureWhite, background)
        val inkRatio = contrastRatio(DeepInk, background)
        val targetExtreme = if (whiteRatio >= inkRatio) PureWhite else DeepInk

        var low = 0f
        var high = 1f
        var best = targetExtreme
        repeat(10) {
            val mid = (low + high) / 2f
            val candidate = lerp(preferred, targetExtreme, mid)
            if (contrastRatio(candidate, background) >= minRatio) {
                best = candidate
                high = mid
            } else {
                low = mid
            }
        }
        return best
    }

    /**
     * Audits and adjusts a Material 3 [ColorScheme] so all primary text, container text,
     * secondary/muted labels, and outlines remain accessible in both light and dark states.
     */
    fun enforceAccessibleColorScheme(
        scheme: ColorScheme,
        isDark: Boolean,
        highContrast: Boolean
    ): ColorScheme {
        val textTarget = if (highContrast) WCAG_AAA_NORMAL else WCAG_AA_NORMAL
        val borderTarget = if (highContrast) 4.5f else WCAG_AA_LARGE

        val adjustedBg = if (highContrast) {
            if (isDark) Color(0xFF000000) else Color(0xFFFFFFFF)
        } else {
            scheme.background
        }

        val adjustedSurface = if (highContrast) {
            if (isDark) Color(0xFF0D111A) else Color(0xFFFFFFFF)
        } else {
            scheme.surface
        }

        val adjustedSurfaceVariant = if (highContrast) {
            if (isDark) Color(0xFF182032) else Color(0xFFE8EEF7)
        } else {
            scheme.surfaceVariant
        }

        val adjustedPrimary = ensureAccessibleForeground(
            preferred = scheme.primary,
            background = adjustedSurface,
            minRatio = if (highContrast) WCAG_AAA_NORMAL else WCAG_AA_NORMAL
        )

        val adjustedOnPrimary = ensureAccessibleForeground(
            preferred = scheme.onPrimary,
            background = adjustedPrimary,
            minRatio = textTarget
        )

        val adjustedSecondary = ensureAccessibleForeground(
            preferred = scheme.secondary,
            background = adjustedSurface,
            minRatio = if (highContrast) WCAG_AAA_NORMAL else WCAG_AA_NORMAL
        )

        val adjustedOnSecondary = ensureAccessibleForeground(
            preferred = scheme.onSecondary,
            background = adjustedSecondary,
            minRatio = textTarget
        )

        val adjustedError = ensureAccessibleForeground(
            preferred = scheme.error,
            background = adjustedSurface,
            minRatio = if (highContrast) WCAG_AAA_NORMAL else WCAG_AA_NORMAL
        )

        val adjustedOnError = ensureAccessibleForeground(
            preferred = scheme.onError,
            background = adjustedError,
            minRatio = textTarget
        )

        return scheme.copy(
            primary = adjustedPrimary,
            onPrimary = adjustedOnPrimary,
            onPrimaryContainer = ensureAccessibleForeground(
                preferred = scheme.onPrimaryContainer,
                background = scheme.primaryContainer,
                minRatio = textTarget
            ),
            secondary = adjustedSecondary,
            onSecondary = adjustedOnSecondary,
            onSecondaryContainer = ensureAccessibleForeground(
                preferred = scheme.onSecondaryContainer,
                background = scheme.secondaryContainer,
                minRatio = textTarget
            ),
            onTertiary = ensureAccessibleForeground(
                preferred = scheme.onTertiary,
                background = scheme.tertiary,
                minRatio = textTarget
            ),
            onTertiaryContainer = ensureAccessibleForeground(
                preferred = scheme.onTertiaryContainer,
                background = scheme.tertiaryContainer,
                minRatio = textTarget
            ),
            background = adjustedBg,
            onBackground = ensureAccessibleForeground(
                preferred = scheme.onBackground,
                background = adjustedBg,
                minRatio = textTarget
            ),
            surface = adjustedSurface,
            onSurface = ensureAccessibleForeground(
                preferred = scheme.onSurface,
                background = adjustedSurface,
                minRatio = textTarget
            ),
            surfaceVariant = adjustedSurfaceVariant,
            onSurfaceVariant = ensureAccessibleForeground(
                preferred = scheme.onSurfaceVariant,
                background = adjustedSurfaceVariant,
                minRatio = textTarget
            ),
            outline = if (highContrast) {
                ensureAccessibleForeground(
                    preferred = scheme.outline,
                    background = adjustedSurface,
                    minRatio = borderTarget
                )
            } else {
                scheme.outline
            },
            error = adjustedError,
            onError = adjustedOnError,
            onErrorContainer = ensureAccessibleForeground(
                preferred = scheme.onErrorContainer,
                background = scheme.errorContainer,
                minRatio = textTarget
            )
        )
    }
}
