package tz.mengidata.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tz.mengidata.app.i18n.AppLanguage
import tz.mengidata.app.i18n.tr
import tz.mengidata.app.notifications.LargeBillWarningPopup
import tz.mengidata.app.ui.theme.LocalMengiColors

/**
 * 3-Second Pop-up Warning Banner (Heads-Up Overlay) displayed whenever a bill or expense
 * meets or exceeds the user's Large Bill Warning threshold.
 * Uses calm matte slate & terracotta/amber warning tones (zero neon glare) and
 * automatically shrinks its 3-second countdown bar before disappearing.
 */
@Composable
fun LargeBillWarningPopOverlay(
    popup: LargeBillWarningPopup?,
    language: AppLanguage,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalMengiColors.current
    val progress = remember(popup?.id) { Animatable(1f) }

    LaunchedEffect(popup?.id) {
        if (popup != null) {
            progress.snapTo(1f)
            progress.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = popup.durationMillis.toInt(),
                    easing = LinearEasing
                )
            )
        }
    }

    AnimatedVisibility(
        visible = popup != null,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(tween(220)),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(tween(220)),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("large_bill_warning_popup")
    ) {
        val current = popup ?: return@AnimatedVisibility
        val cardBg = if (colors.isDark) Color(0xFF281E20) else Color(0xFFFDF3F1)
        val borderCol = if (colors.isDark) Color(0xFF8C4A46) else Color(0xFFD47A72)
        val accentCol = colors.expense
        val textCol = if (colors.isDark) Color(0xFFECEFF4) else Color(0xFF2B1615)
        val subCol = if (colors.isDark) Color(0xFFD5B8B5) else Color(0xFF6B3A36)

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = cardBg,
                border = BorderStroke(1.5.dp, borderCol),
                shadowElevation = 10.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(colors.expenseSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.WarningAmber,
                                contentDescription = tr(language, "Onyo la Bili Kubwa", "Large Bill Warning"),
                                tint = accentCol,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = tr(language, "ONYO: BILI KUBWA", "WARNING: LARGE BILL"),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = accentCol
                                )
                                StatusPill(
                                    text = "3s POP-UP",
                                    containerColor = colors.warningSoft,
                                    contentColor = colors.warning
                                )
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "${current.billName} • ${current.amountFormatted}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = textCol,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = tr(
                                    language,
                                    "${current.category} imezidi kiwango cha tahadhari (${current.thresholdFormatted})",
                                    "${current.category} exceeds warning limit (${current.thresholdFormatted})"
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = subCol,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("dismiss_large_bill_warning_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = tr(language, "Funga onyo", "Dismiss warning"),
                                tint = subCol,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { progress.value },
                        color = accentCol,
                        trackColor = colors.cardBorder,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(CircleShape)
                    )
                }
            }
        }
    }
}
