package tz.mengidata.app.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import tz.mengidata.app.data.accounting.PartySummary
import tz.mengidata.app.data.db.PendingTransactionEntity
import tz.mengidata.app.i18n.AppLanguage
import tz.mengidata.app.i18n.FormatUtils
import tz.mengidata.app.i18n.tr
import tz.mengidata.app.ui.theme.LocalMengiColors

/**
 * Matte Android 17 summary card for Pending Transactions inside the Ledger bar,
 * showing remaining balances and partial/full payment counts.
 */
@Composable
fun PendingTransactionsSummaryCard(
    pendingItems: List<PendingTransactionEntity>,
    currency: String,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val colors = LocalMengiColors.current
    val totalRemaining = remember(pendingItems) { pendingItems.sumOf { it.remainingAmount } }
    val totalAlreadyPaid = remember(pendingItems) { pendingItems.sumOf { it.effectivePaidAmount } }
    val overdueCount = remember(pendingItems) { pendingItems.count { it.isOverdue } }
    val partialCount = remember(pendingItems) { pendingItems.count { it.isPartiallyPaid } }
    val fullPaidCount = remember(pendingItems) { pendingItems.count { it.isFullyPaid } }

    MengiCard(
        modifier = modifier.testTag("pending_txns_summary_card"),
        contentPadding = 16.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Schedule,
                    contentDescription = null,
                    tint = colors.warning,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = tr(language, "Malipo Yanayosubiri", "Pending Payments"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            StatusPill(
                text = if (overdueCount > 0) {
                    tr(language, "$overdueCount zimechelewa", "$overdueCount overdue")
                } else {
                    tr(language, "${pendingItems.size} zinasubiri", "${pendingItems.size} waiting")
                },
                containerColor = if (overdueCount > 0) colors.expenseSoft else colors.warningSoft,
                contentColor = if (overdueCount > 0) colors.expense else colors.warning
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = colors.expenseSoft,
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                    Text(
                        text = tr(language, "Bado Kulipa", "Remaining"),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.expense,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = FormatUtils.formatCompactMoney(totalRemaining, currency),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.expense,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = colors.incomeSoft,
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                    Text(
                        text = tr(language, "Amelipa Tayari", "Paid So Far"),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.income,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = FormatUtils.formatCompactMoney(totalAlreadyPaid, currency),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.income,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (partialCount > 0 || fullPaidCount > 0) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (partialCount > 0) {
                    StatusPill(
                        text = tr(language, "$partialCount Amelipa Nusu", "$partialCount Partial"),
                        containerColor = colors.warningSoft,
                        contentColor = colors.warning
                    )
                }
                if (fullPaidCount > 0) {
                    StatusPill(
                        text = tr(language, "$fullPaidCount Kalipa Chote", "$fullPaidCount Paid Full"),
                        containerColor = colors.incomeSoft,
                        contentColor = colors.income
                    )
                }
            }
        }
    }
}

private fun pendingInitials(item: PendingTransactionEntity): String {
    val source = item.party.trim().ifBlank { item.description.trim() }
    if (source.isBlank()) return "ML"
    val parts = source.split(Regex("\\s+")).filter { it.isNotBlank() }
    return when {
        parts.size >= 2 -> "${parts[0].first().uppercaseChar()}${parts[1].first().uppercaseChar()}"
        parts.size == 1 && parts[0].length >= 2 -> parts[0].substring(0, 2).uppercase()
        else -> parts[0].first().uppercaseChar().toString()
    }
}

/**
 * Conversation-style inbox list for Pending Payments inside the Ledger bar.
 * Displays compact, scannable threads (initials avatar + status dot, name/title,
 * 1-line details preview, date & amount). Tapping any row opens the compact popup view.
 */
@Composable
fun PendingConversationListCard(
    items: List<PendingTransactionEntity>,
    currency: String,
    language: AppLanguage,
    onSelectItem: (PendingTransactionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalMengiColors.current
    MengiCard(
        modifier = modifier.testTag("pending_conversation_list_card"),
        contentPadding = 0.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            items.forEachIndexed { index, item ->
                PendingConversationListRow(
                    item = item,
                    currency = currency,
                    language = language,
                    onClick = { onSelectItem(item) }
                )
                if (index < items.lastIndex) {
                    HorizontalDivider(
                        color = colors.cardBorder,
                        thickness = 0.8.dp,
                        modifier = Modifier.padding(start = 74.dp, end = 14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PendingConversationListRow(
    item: PendingTransactionEntity,
    currency: String,
    language: AppLanguage,
    onClick: () -> Unit
) {
    val colors = LocalMengiColors.current
    val initials = remember(item.party, item.description) { pendingInitials(item) }

    val statusBg = when {
        item.isFullyPaid -> colors.incomeSoft
        item.isPartiallyPaid -> colors.warningSoft
        else -> colors.expenseSoft
    }
    val statusFg = when {
        item.isFullyPaid -> colors.income
        item.isPartiallyPaid -> colors.warning
        else -> colors.expense
    }
    val statusLabel = when {
        item.isFullyPaid -> tr(language, "Kalipa Chote", "Paid Full")
        item.isPartiallyPaid -> tr(language, "Amelipa Nusu", "Partial")
        else -> tr(language, "Hajalipa", "Unpaid")
    }

    val primaryTitle = item.party.trim().ifBlank { item.description.trim() }
    val previewLine = buildList {
        if (item.party.isNotBlank() && item.description.isNotBlank()) {
            add(item.description.trim())
        }
        if (item.details.isNotBlank()) {
            add(item.details.trim())
        }
        if (item.location.isNotBlank()) {
            add(item.location.trim())
        }
        if (isEmpty() && item.phoneNumber.isNotBlank()) {
            add(item.phoneNumber.trim())
        }
        if (isEmpty()) {
            add(FormatUtils.localizedCategory(item.category, language))
        }
    }.joinToString(" · ")

    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 76.dp)
            .testTag("pending_txn_card_${item.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Conversation Avatar with live payment-status dot
            Box(modifier = Modifier.size(48.dp)) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(statusBg)
                        .align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = statusFg,
                        maxLines = 1
                    )
                }
                Box(
                    modifier = Modifier
                        .size(13.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(statusFg)
                        .align(Alignment.BottomEnd)
                )
            }

            // Center: Name/Title, 1-line conversation preview, compact status line
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = primaryTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                    Text(
                        text = FormatUtils.friendlyDateLabel(item.expectedDate, language),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (item.isOverdue) FontWeight.Bold else FontWeight.Medium,
                        color = if (item.isOverdue) colors.expense else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = previewLine,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                    Text(
                        text = FormatUtils.formatCompactMoney(item.amount, currency),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = statusFg,
                        maxLines = 1
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = statusBg
                        ) {
                            Text(
                                text = statusLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = statusFg,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                maxLines = 1
                            )
                        }
                        if (item.isPartiallyPaid) {
                            Text(
                                text = "${tr(language, "Bado", "Left")} ${FormatUtils.formatCompactMoney(item.remainingAmount, currency)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.expense,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else if (item.isOverdue) {
                            Text(
                                text = tr(language, "Imechelewa", "Overdue"),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.expense,
                                maxLines = 1
                            )
                        }
                    }

                    if (item.isPartiallyPaid) {
                        Box(
                            modifier = Modifier
                                .width(42.dp)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(item.paymentProgressRatio)
                                    .fillMaxHeight()
                                    .clip(CircleShape)
                                    .background(colors.warning)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Compact small-size Popup View showing complete details and quick actions when the user
 * taps a pending payment from the conversation list.
 */
@Composable
fun PendingTransactionDetailPopup(
    item: PendingTransactionEntity,
    currency: String,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onRecordPayment: () -> Unit,
    onCompleteToLedger: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val colors = LocalMengiColors.current
    val initials = remember(item.party, item.description) { pendingInitials(item) }

    val statusBg = when {
        item.isFullyPaid -> colors.incomeSoft
        item.isPartiallyPaid -> colors.warningSoft
        else -> colors.expenseSoft
    }
    val statusFg = when {
        item.isFullyPaid -> colors.income
        item.isPartiallyPaid -> colors.warning
        else -> colors.expense
    }
    val statusLabel = when {
        item.isFullyPaid -> tr(language, "Kalipa Kiasi Chote", "Paid in Full")
        item.isPartiallyPaid -> tr(language, "Amelipa Nusu", "Paid Partial")
        else -> tr(language, "Hajalipa Bado", "Unpaid")
    }
    val hasContactInfo = item.party.isNotBlank() || item.phoneNumber.isNotBlank() || item.location.isNotBlank()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, colors.cardBorder),
            tonalElevation = 2.dp,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 348.dp)
                .testTag("pending_detail_popup_${item.id}")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header: Avatar + Name/Title + Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(statusBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = statusFg
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = item.party.ifBlank { item.description },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (item.party.isNotBlank()) {
                            Text(
                                text = item.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Surface(
                        onClick = onDismiss,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("close_pending_popup_btn")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = tr(language, "Funga", "Close"),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Compact Status & Payment Breakdown Card
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, colors.cardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pending_txn_payment_status_${item.id}")
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatusPill(
                                text = statusLabel,
                                containerColor = statusBg,
                                contentColor = statusFg
                            )
                            StatusPill(
                                text = FormatUtils.friendlyDateLabel(item.expectedDate, language),
                                containerColor = if (item.isOverdue) colors.expenseSoft else MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = if (item.isOverdue) colors.expense else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tr(language, "Jumla", "Total"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = FormatUtils.formatCompactMoney(item.amount, currency),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tr(language, "Amelipa", "Paid"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.income
                                )
                                Text(
                                    text = FormatUtils.formatCompactMoney(item.effectivePaidAmount, currency),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.income,
                                    maxLines = 1
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = tr(language, "Bado", "Left"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (item.isFullyPaid) colors.income else colors.expense
                                )
                                Text(
                                    text = if (item.isFullyPaid) {
                                        tr(language, "0 (Chote)", "0 (Full)")
                                    } else {
                                        FormatUtils.formatCompactMoney(item.remainingAmount, currency)
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (item.isFullyPaid) colors.income else colors.expense,
                                    maxLines = 1
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        ) {
                            if (item.paymentProgressRatio > 0.005f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(item.paymentProgressRatio)
                                        .fillMaxHeight()
                                        .clip(CircleShape)
                                        .background(if (item.isFullyPaid) colors.income else colors.warning)
                                )
                            }
                        }
                    }
                }

                // Contact & Details Card (Jina, Simu, Eneo, Maelezo)
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, colors.cardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pending_txn_details_${item.id}")
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (hasContactInfo) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (item.phoneNumber.isNotBlank()) {
                                    Surface(
                                        onClick = {
                                            val dial = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${item.phoneNumber.trim()}"))
                                            runCatching { context.startActivity(dial) }
                                        },
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.heightIn(min = 34.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Phone,
                                                contentDescription = tr(language, "Piga simu", "Call"),
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = item.phoneNumber,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }

                                if (item.location.isNotBlank()) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.LocationOn,
                                            contentDescription = tr(language, "Eneo", "Location"),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = item.location,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                            if (item.phoneNumber.isNotBlank() || item.location.isNotBlank()) {
                                HorizontalDivider(color = colors.cardBorder, thickness = 0.8.dp)
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Notes,
                                contentDescription = tr(language, "Maelezo", "Details"),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tr(language, "Maelezo", "Details"),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = item.details.ifBlank {
                                        tr(language, "Hakuna maelezo ya ziada.", "No extra details added.")
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (item.details.isBlank()) {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                        }

                        Text(
                            text = "${FormatUtils.localizedCategory(item.category, language)} · ${FormatUtils.localizedMethod(item.method, language)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Compact Action Row 1: Partial/Full Payment + Post to Ledger
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!item.isFullyPaid) {
                        OutlinedButton(
                            onClick = onRecordPayment,
                            shape = CircleShape,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("record_payment_pending_${item.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Payments,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = tr(language, "Lipa Nusu", "Pay"),
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Button(
                        onClick = onCompleteToLedger,
                        shape = CircleShape,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("complete_pending_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (item.isFullyPaid) {
                                tr(language, "Hamishia Ledger", "Post to Ledger")
                            } else {
                                tr(language, "→ Ledger", "→ Ledger")
                            },
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Compact Action Row 2: Edit + Delete
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = onEdit,
                        shape = CircleShape,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("edit_pending_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = tr(language, "Hariri", "Edit"),
                            maxLines = 1
                        )
                    }

                    OutlinedButton(
                        onClick = onDelete,
                        shape = CircleShape,
                        border = BorderStroke(1.dp, colors.expense.copy(alpha = 0.45f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.expense),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("delete_pending_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = tr(language, "Futa", "Delete"),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/**
 * Fast bottom sheet for recording a partial payment (Amelipa Nusu) or marking Paid in Full (Kalipa Chote)
 * directly from a pending transaction card.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingPaymentUpdateSheet(
    item: PendingTransactionEntity,
    currency: String,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onRecordPartialPayment: (Double) -> Unit,
    onMarkFullyPaid: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = LocalMengiColors.current
    var addedPaymentText by remember(item.id) { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    val addedAmount = addedPaymentText.toDoubleOrNull() ?: 0.0
    val projectedPaid = (item.effectivePaidAmount + addedAmount).coerceAtMost(item.amount)
    val projectedRemaining = (item.amount - projectedPaid).coerceAtLeast(0.0)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = OneUiSheetShape,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = tr(language, "Rekodi Malipo ya Muamala", "Record Pending Payment"),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (item.party.isNotBlank() || item.phoneNumber.isNotBlank()) {
                        Text(
                            text = listOf(item.party, item.phoneNumber, item.location)
                                .filter { it.isNotBlank() }
                                .joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = tr(language, "Jumla: ${FormatUtils.formatMoney(item.amount, currency)}", "Total: ${FormatUtils.formatMoney(item.amount, currency)}"),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = tr(language, "Bado: ${FormatUtils.formatMoney(item.remainingAmount, currency)}", "Remaining: ${FormatUtils.formatMoney(item.remainingAmount, currency)}"),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.expense
                        )
                    }
                }
            }

            OutlinedTextField(
                value = addedPaymentText,
                onValueChange = {
                    addedPaymentText = it.filter { c -> c.isDigit() || c == '.' }
                    errorText = null
                },
                label = { Text(tr(language, "Kiasi Anacholipa Sasa (Amelipa Nusu)", "Partial Payment Amount Now")) },
                placeholder = {
                    Text(
                        tr(
                            language,
                            "Mfano: ${(item.remainingAmount / 2.0).toLong()}",
                            "e.g. ${(item.remainingAmount / 2.0).toLong()}"
                        )
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = OneUiFieldShape,
                colors = oneUiTextFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pending_partial_payment_input")
            )

            if (addedAmount > 0.0) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (projectedRemaining <= 0.005) colors.incomeSoft else colors.warningSoft,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (projectedRemaining <= 0.005) {
                            tr(language, "Baada ya malipo haya: Kalipa Kiasi Chote (Bado 0)", "After this payment: Paid in Full (Remaining 0)")
                        } else {
                            tr(
                                language,
                                "Jumla atakuwa amelipa ${FormatUtils.formatMoney(projectedPaid, currency)} • Bado ${FormatUtils.formatMoney(projectedRemaining, currency)}",
                                "Total paid will be ${FormatUtils.formatMoney(projectedPaid, currency)} • Remaining ${FormatUtils.formatMoney(projectedRemaining, currency)}"
                            )
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (projectedRemaining <= 0.005) colors.income else colors.warning,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                    )
                }
            }

            if (errorText != null) {
                Text(
                    text = errorText!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = {
                    if (addedAmount <= 0.0) {
                        errorText = tr(language, "Weka kiasi kilicholipwa kwanza.", "Enter the amount paid first.")
                        return@Button
                    }
                    onRecordPartialPayment(addedAmount)
                },
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("confirm_partial_payment_btn")
            ) {
                Text(tr(language, "Hifadhi Malipo ya Nusu", "Save Partial Payment"))
            }

            FilledTonalButton(
                onClick = onMarkFullyPaid,
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("confirm_full_payment_btn")
            ) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = tr(
                        language,
                        "Kalipa Kiasi Chote (${FormatUtils.formatCompactMoney(item.amount, currency)})",
                        "Mark Paid in Full (${FormatUtils.formatCompactMoney(item.amount, currency)})"
                    ),
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

/**
 * Bottom sheet for adding or editing a Pending Transaction inside the Ledger bar,
 * including:
 * - Jina (Name), Namba ya Simu (Phone Number), Eneo (Location)
 * - Payment Status: Hajalipa / Amelipa Nusu (+ Bado kiasi gani) / Kalipa Kiasi Chote
 * - Prominent multi-line Details (Maelezo) field
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingTransactionEditorSheet(
    initial: PendingTransactionEntity?,
    knownParties: List<PartySummary>,
    currency: String,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onSave: (PendingTransactionEntity) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = LocalMengiColors.current
    var description by remember(initial) { mutableStateOf(initial?.description ?: "") }
    var details by remember(initial) { mutableStateOf(initial?.details ?: "") }
    var amountText by remember(initial, currency) {
        mutableStateOf(if (initial != null && initial.amount > 0) FormatUtils.formatEditableAmount(initial.amount, currency) else "")
    }
    var expectedDate by remember(initial) {
        mutableStateOf(initial?.expectedDate ?: FormatUtils.todayIso())
    }
    var party by remember(initial) { mutableStateOf(initial?.party ?: "") }
    var phoneNumber by remember(initial) { mutableStateOf(initial?.phoneNumber ?: "") }
    var location by remember(initial) { mutableStateOf(initial?.location ?: "") }

    // 0 = Hajalipa (Unpaid), 1 = Amelipa Nusu (Partial), 2 = Kalipa Chote (Full)
    var paymentMode by remember(initial) {
        mutableIntStateOf(
            when {
                initial == null -> 0
                initial.isFullyPaid -> 2
                initial.isPartiallyPaid -> 1
                else -> 0
            }
        )
    }
    var paidAmountText by remember(initial, currency) {
        mutableStateOf(
            if (initial != null && initial.effectivePaidAmount > 0.0) {
                FormatUtils.formatEditableAmount(initial.effectivePaidAmount, currency)
            } else {
                ""
            }
        )
    }

    var category by remember(initial) {
        mutableStateOf(initial?.category?.takeIf { it.isNotBlank() } ?: FormatUtils.expenseCategories.first())
    }
    var method by remember(initial) {
        mutableStateOf(FormatUtils.normalizePaymentMethod(initial?.method ?: "M-Pesa"))
    }
    var errorText by remember { mutableStateOf<String?>(null) }

    val availableCategories = FormatUtils.expenseCategories

    val parsedTotal = amountText.toDoubleOrNull() ?: 0.0
    val parsedPartialPaid = paidAmountText.toDoubleOrNull() ?: 0.0
    val computedPaid = when (paymentMode) {
        2 -> parsedTotal
        1 -> parsedPartialPaid.coerceIn(0.0, parsedTotal)
        else -> 0.0
    }
    val computedRemaining = (parsedTotal - computedPaid).coerceAtLeast(0.0)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = OneUiSheetShape,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (initial == null) {
                    tr(language, "Malipo Mapya Yanayosubiri", "New Pending Payment")
                } else {
                    tr(language, "Hariri Malipo Yanayosubiri", "Edit Pending Payment")
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it; errorText = null },
                label = { Text(tr(language, "Kichwa cha Malipo *", "Payment Title *")) },
                placeholder = {
                    Text(
                        tr(
                            language,
                            "Mfano: Malipo ya oda ya vifaa au fundi",
                            "e.g. Stock order payment or contractor fee"
                        )
                    )
                },
                singleLine = true,
                shape = OneUiFieldShape,
                colors = oneUiTextFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pending_title_input")
            )

            // Contact Details Section: Jina, Namba ya Simu, Eneo
            Text(
                text = tr(language, "Taarifa za Mhusika (Jina, Simu & Eneo)", "Contact Details (Name, Phone & Location)"),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = party,
                onValueChange = { party = it },
                label = { Text(tr(language, "Jina la Mhusika / Mteja", "Full Name / Client")) },
                leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                singleLine = true,
                shape = OneUiFieldShape,
                colors = oneUiTextFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pending_party_input")
            )

            RecognizedNameSuggestionsBar(
                query = party,
                knownParties = knownParties,
                language = language,
                onSelectParty = { selected -> party = selected.name }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text(tr(language, "Namba ya Simu", "Phone Number")) },
                    placeholder = { Text("07XX XXX XXX") },
                    leadingIcon = { Icon(Icons.Rounded.Phone, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    shape = OneUiFieldShape,
                    colors = oneUiTextFieldColors(),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pending_phone_input")
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text(tr(language, "Eneo / Mahali", "Location")) },
                    placeholder = { Text(tr(language, "Mfano: Kariakoo", "e.g. Kariakoo")) },
                    leadingIcon = { Icon(Icons.Rounded.LocationOn, contentDescription = null) },
                    singleLine = true,
                    shape = OneUiFieldShape,
                    colors = oneUiTextFieldColors(),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pending_location_input")
                )
            }

            // Amount & Expected Date
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' }; errorText = null },
                    label = { Text(tr(language, "Jumla ya Kiasi ($currency) *", "Total Amount ($currency) *")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = OneUiFieldShape,
                    colors = oneUiTextFieldColors(),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pending_amount_input")
                )
                OutlinedTextField(
                    value = expectedDate,
                    onValueChange = { expectedDate = it },
                    label = { Text(tr(language, "Tarehe Tarajiwa", "Expected Date")) },
                    singleLine = true,
                    shape = OneUiFieldShape,
                    colors = oneUiTextFieldColors(),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pending_date_input")
                )
            }

            // Payment Status Section: Hajalipa / Amelipa Nusu / Kalipa Chote
            Text(
                text = tr(language, "Hali ya Malipo", "Payment Status"),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            OneUiSegmentedTabs(
                options = listOf(
                    tr(language, "Hajalipa", "Unpaid"),
                    tr(language, "Amelipa Nusu", "Partial"),
                    tr(language, "Kalipa Chote", "Paid Full")
                ),
                selectedIndex = paymentMode,
                onSelect = { idx ->
                    paymentMode = idx
                    errorText = null
                    if (idx == 0) {
                        paidAmountText = ""
                    } else if (idx == 2 && parsedTotal > 0.0) {
                        paidAmountText = FormatUtils.formatEditableAmount(parsedTotal, currency)
                    }
                },
                testTags = listOf("pending_pay_unpaid", "pending_pay_partial", "pending_pay_full")
            )

            if (paymentMode == 1) {
                OutlinedTextField(
                    value = paidAmountText,
                    onValueChange = {
                        paidAmountText = it.filter { c -> c.isDigit() || c == '.' }
                        errorText = null
                    },
                    label = { Text(tr(language, "Kiasi Alicholipa ($currency) *", "Amount Paid So Far ($currency) *")) },
                    placeholder = { Text(tr(language, "Andika kiasi alicholipa nusu", "Enter partial amount paid")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = OneUiFieldShape,
                    colors = oneUiTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pending_paid_amount_input")
                )
            }

            if (parsedTotal > 0.0) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = when (paymentMode) {
                        2 -> colors.incomeSoft
                        1 -> colors.warningSoft
                        else -> MaterialTheme.colorScheme.surfaceContainerLow
                    },
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pending_balance_preview")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = tr(language, "Amelipa", "Paid"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = FormatUtils.formatMoney(computedPaid, currency),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = colors.income
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = tr(language, "Bado Kiasi Gani", "Remaining Balance"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (computedRemaining <= 0.005) {
                                    tr(language, "0 · Kalipa Chote", "0 · Paid Full")
                                } else {
                                    FormatUtils.formatMoney(computedRemaining, currency)
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (computedRemaining <= 0.005) colors.income else colors.expense
                            )
                        }
                    }
                }
            }

            // Dedicated Transaction Details / Maelezo field
            OutlinedTextField(
                value = details,
                onValueChange = { details = it },
                label = { Text(tr(language, "Maelezo ya Muamala (Kwa Nini Unasubiri)", "Transaction Details / Notes")) },
                placeholder = {
                    Text(
                        tr(
                            language,
                            "Andika maelezo kamili: makubaliano, bidhaa, risiti au ahadi ya kulipa...",
                            "Write full details: agreement, items, receipt or payment promise..."
                        )
                    )
                },
                minLines = 3,
                maxLines = 6,
                shape = OneUiFieldShape,
                colors = oneUiTextFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pending_details_input")
            )

            Text(
                text = tr(language, "Kundi", "Category"),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                availableCategories.forEach { cat ->
                    FilterChip(
                        selected = category.equals(cat, ignoreCase = true),
                        onClick = { category = cat },
                        shape = CircleShape,
                        label = { Text(FormatUtils.localizedCategory(cat, language)) }
                    )
                }
            }

            Text(
                text = tr(language, "Mfumo wa Malipo", "Payment Method"),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FormatUtils.paymentMethods.forEach { m ->
                    FilterChip(
                        selected = FormatUtils.samePaymentMethod(method, m),
                        onClick = { method = m },
                        shape = CircleShape,
                        label = { Text(FormatUtils.localizedMethod(m, language)) }
                    )
                }
            }

            if (errorText != null) {
                Text(
                    text = errorText!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (description.isBlank() || amt <= 0.0) {
                        errorText = tr(
                            language,
                            "Jaza kichwa cha muamala na jumla ya kiasi sahihi.",
                            "Enter a transaction title and a valid total amount."
                        )
                        return@Button
                    }
                    val finalPaid = when (paymentMode) {
                        2 -> amt
                        1 -> {
                            val partial = paidAmountText.toDoubleOrNull() ?: 0.0
                            if (partial <= 0.0 || partial >= amt) {
                                errorText = tr(
                                    language,
                                    "Kwa 'Amelipa Nusu', weka kiasi zaidi ya 0 na chini ya jumla (${FormatUtils.formatCompactMoney(amt, currency)}).",
                                    "For Partial payment, enter an amount greater than 0 and less than the total."
                                )
                                return@Button
                            }
                            partial
                        }
                        else -> 0.0
                    }
                    val base = initial ?: PendingTransactionEntity(
                        id = "",
                        description = description.trim()
                    )
                    onSave(
                        base.copy(
                            type = "expense",
                            description = description.trim(),
                            details = details.trim(),
                            party = party.trim(),
                            phoneNumber = phoneNumber.trim(),
                            location = location.trim(),
                            category = category,
                            amount = amt,
                            paidAmount = finalPaid,
                            method = method,
                            expectedDate = expectedDate.ifBlank { FormatUtils.todayIso() }
                        )
                    )
                },
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_pending_txn_btn")
            ) {
                Text(tr(language, "Hifadhi Malipo Yanayosubiri", "Save Pending Payment"))
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
