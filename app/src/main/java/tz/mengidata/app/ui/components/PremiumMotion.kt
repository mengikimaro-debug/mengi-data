package tz.mengidata.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import tz.mengidata.app.i18n.FormatUtils

/**
 * Zero-jank instant reveal container for LazyColumn items and dashboard cards.
 * Eliminates scroll-time LaunchedEffect delays and repeated spring relayouts.
 */
@Composable
fun PremiumReveal(
    delayMillis: Int = 0,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        content()
    }
}

/**
 * Allocation-light monetary text display without per-frame animation GC churn or Float precision loss.
 */
@Composable
fun AnimatedMoneyText(
    amount: Double,
    currency: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    prefix: String = "",
    fontWeight: FontWeight? = null
) {
    val display = remember(amount, currency, compact, prefix) {
        val formatted = if (compact) {
            FormatUtils.formatCompactMoney(amount, currency)
        } else {
            FormatUtils.formatMoney(amount, currency)
        }
        prefix + formatted
    }

    Text(
        text = display,
        style = style,
        color = color,
        fontWeight = fontWeight ?: style.fontWeight,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

/**
 * Crisp static halo status dot with zero continuous Choreographer/GPU wakeups.
 */
@Composable
fun PulsingStatusDot(
    color: Color,
    size: Dp = 9.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size * 2.1f)) {
        val coreRadius = size.toPx() / 2f
        drawCircle(
            color = color.copy(alpha = 0.24f),
            radius = coreRadius * 1.75f
        )
        drawCircle(
            color = color,
            radius = coreRadius
        )
    }
}
