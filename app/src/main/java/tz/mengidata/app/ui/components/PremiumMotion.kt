package tz.mengidata.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import tz.mengidata.app.i18n.FormatUtils

/**
 * Staggered spring entrance animation for premium cards and sections (< 300ms settle).
 */
@Composable
fun PremiumReveal(
    delayMillis: Int = 0,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(delayMillis == 0) }
    LaunchedEffect(delayMillis) {
        if (delayMillis > 0) {
            delay(delayMillis.toLong())
        }
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(
            animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
        ) + slideInVertically(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            initialOffsetY = { it / 6 }
        ) + scaleIn(
            initialScale = 0.97f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
    ) {
        content()
    }
}

/**
 * Smoothly animates monetary amounts with spring roll-up when period filters or records update.
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
    val animatedValue by animateFloatAsState(
        targetValue = amount.toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "money_roll"
    )
    val display = if (compact) {
        FormatUtils.formatCompactMoney(animatedValue.toDouble(), currency)
    } else {
        FormatUtils.formatMoney(animatedValue.toDouble(), currency)
    }

    Text(
        text = prefix + display,
        style = style,
        color = color,
        fontWeight = fontWeight ?: style.fontWeight,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

/**
 * Breathing luminous status dot for live indicators (Zero-Loss verified, Red High-Bill alert).
 */
@Composable
fun PulsingStatusDot(
    color: Color,
    size: Dp = 9.dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_dot")
    val haloScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "halo_scale"
    )
    val haloBlend by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_blend"
    )
    val haloColor = lerp(MaterialTheme.colorScheme.surface, color, haloBlend)

    Box(
        modifier = modifier.size(size * 2.1f),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .scale(haloScale)
                .clip(CircleShape)
                .background(haloColor)
        )
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(color)
        )
    }
}
