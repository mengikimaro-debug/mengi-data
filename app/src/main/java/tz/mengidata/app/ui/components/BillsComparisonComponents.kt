package tz.mengidata.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tz.mengidata.app.data.accounting.AutoBucket
import tz.mengidata.app.data.accounting.BillCategoryTab
import tz.mengidata.app.data.accounting.BillLedgerEntry
import tz.mengidata.app.data.accounting.BillsComparisonSummary
import tz.mengidata.app.data.accounting.DailyBillGroup
import tz.mengidata.app.data.accounting.HigherBillCategory
import tz.mengidata.app.i18n.AppLanguage
import tz.mengidata.app.i18n.FormatUtils
import tz.mengidata.app.i18n.tr
import tz.mengidata.app.ui.theme.LocalMengiColors
import kotlin.math.roundToInt

/**
 * Matte Android 17 comparison card inside the Bills tab showing Household Bill vs
 * Personal Bill totals, daily averages, today's breakdown, and clearly highlighting
 * which category is currently higher.
 */
@Composable
fun BillsComparisonOverviewCard(
    summary: BillsComparisonSummary,
    selectedTab: BillCategoryTab,
    currency: String,
    language: AppLanguage,
    onSelectTab: (BillCategoryTab) -> Unit,
    onPayAllDueBills: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalMengiColors.current
    val householdColors = bucketVisualColors(AutoBucket.HOUSEHOLD_BILL)
    val personalColors = bucketVisualColors(AutoBucket.BILL)

    val householdPct = (summary.householdShare * 100f).roundToInt().coerceIn(0, 100)
    val personalPct = (100 - householdPct).coerceIn(0, 100)

    val verdictBg: Color
    val verdictInk: Color
    val verdictIcon: ImageVector
    val verdictText: String

    when (summary.higherOverall) {
        HigherBillCategory.HOUSEHOLD -> {
            verdictBg = householdColors.softBg
            verdictInk = householdColors.ink
            verdictIcon = Icons.AutoMirrored.Rounded.TrendingUp
            verdictText = tr(
                language,
                "Bili ya Nyumbani iko juu kwa ${FormatUtils.formatMoney(summary.overallDifference, currency)} ($householdPct%)",
                "Household Bill is higher by ${FormatUtils.formatMoney(summary.overallDifference, currency)} ($householdPct%)"
            )
        }
        HigherBillCategory.PERSONAL -> {
            verdictBg = personalColors.softBg
            verdictInk = personalColors.ink
            verdictIcon = Icons.AutoMirrored.Rounded.TrendingUp
            verdictText = tr(
                language,
                "Bili Binafsi iko juu kwa ${FormatUtils.formatMoney(summary.overallDifference, currency)} ($personalPct%)",
                "Personal Bill is higher by ${FormatUtils.formatMoney(summary.overallDifference, currency)} ($personalPct%)"
            )
        }
        HigherBillCategory.EQUAL -> {
            verdictBg = MaterialTheme.colorScheme.surfaceContainerHigh
            verdictInk = MaterialTheme.colorScheme.onSurface
            verdictIcon = Icons.Rounded.SwapHoriz
            verdictText = tr(
                language,
                "Bili ya Nyumbani na Binafsi ziko sawa",
                "Household and Personal spending are equal"
            )
        }
        HigherBillCategory.NONE -> {
            verdictBg = MaterialTheme.colorScheme.surfaceContainerHigh
            verdictInk = MaterialTheme.colorScheme.onSurfaceVariant
            verdictIcon = Icons.Rounded.AutoAwesome
            verdictText = tr(
                language,
                "Mfumo utalinganisha kiotomatiki ukiongeza bili au matumizi",
                "System will auto-compare once bills or expenses are logged"
            )
        }
    }

    MengiCard(
        modifier = modifier.testTag("bills_comparison_card"),
        contentPadding = 16.dp
    ) {
        // Eyebrow + combined total
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = tr(language, "Ulinganisho wa Matumizi · Auto", "Spending Comparison · Auto"),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (summary.combinedTotal > 0.005) {
                Text(
                    text = FormatUtils.formatCompactMoney(summary.combinedTotal, currency),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // Verdict pill clearly answering which category is higher
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = verdictBg,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("bills_higher_verdict")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = verdictIcon,
                    contentDescription = null,
                    tint = verdictInk,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = verdictText,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = verdictInk,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // 2-segment matte proportional bar
        if (summary.combinedTotal > 0.005) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(9.dp)
                    .clip(CircleShape)
                    .background(colors.subtleSurface),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (summary.householdShare > 0.001f) {
                    Box(
                        modifier = Modifier
                            .weight(summary.householdShare.coerceAtLeast(0.02f))
                            .fillMaxHeight()
                            .background(householdColors.dot)
                    )
                }
                if (summary.personalShare > 0.001f) {
                    Box(
                        modifier = Modifier
                            .weight(summary.personalShare.coerceAtLeast(0.02f))
                            .fillMaxHeight()
                            .background(personalColors.dot)
                    )
                }
            }

            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(householdColors.dot)
                    )
                    Text(
                        text = tr(language, "Nyumbani $householdPct%", "Household $householdPct%"),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = householdColors.ink
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = tr(language, "Binafsi $personalPct%", "Personal $personalPct%"),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = personalColors.ink
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(personalColors.dot)
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Side-by-side clickable comparison tiles for Household vs Personal
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ComparisonCategoryTile(
                title = tr(language, "Bili ya Nyumbani", "Household Bill"),
                totalText = FormatUtils.formatCompactMoney(summary.householdTotal, currency),
                todayText = tr(
                    language,
                    "Leo: ${FormatUtils.formatCompactMoney(summary.householdTodayTotal, currency)}",
                    "Today: ${FormatUtils.formatCompactMoney(summary.householdTodayTotal, currency)}"
                ),
                avgText = tr(
                    language,
                    "Wastani/siku: ${FormatUtils.formatCompactMoney(summary.householdDailyAvg, currency)}",
                    "Avg/day: ${FormatUtils.formatCompactMoney(summary.householdDailyAvg, currency)}"
                ),
                metaText = tr(
                    language,
                    "${summary.householdCount} · ${summary.householdDaysCount} siku",
                    "${summary.householdCount} · ${summary.householdDaysCount}d"
                ),
                icon = Icons.Rounded.Home,
                visualColors = householdColors,
                selected = selectedTab == BillCategoryTab.HOUSEHOLD,
                isHigher = summary.higherOverall == HigherBillCategory.HOUSEHOLD,
                language = language,
                onClick = { onSelectTab(BillCategoryTab.HOUSEHOLD) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("bills_compare_tile_household")
            )

            ComparisonCategoryTile(
                title = tr(language, "Bili Binafsi", "Personal Bill"),
                totalText = FormatUtils.formatCompactMoney(summary.personalTotal, currency),
                todayText = tr(
                    language,
                    "Leo: ${FormatUtils.formatCompactMoney(summary.personalTodayTotal, currency)}",
                    "Today: ${FormatUtils.formatCompactMoney(summary.personalTodayTotal, currency)}"
                ),
                avgText = tr(
                    language,
                    "Wastani/siku: ${FormatUtils.formatCompactMoney(summary.personalDailyAvg, currency)}",
                    "Avg/day: ${FormatUtils.formatCompactMoney(summary.personalDailyAvg, currency)}"
                ),
                metaText = tr(
                    language,
                    "${summary.personalCount} · ${summary.personalDaysCount} siku",
                    "${summary.personalCount} · ${summary.personalDaysCount}d"
                ),
                icon = Icons.Rounded.Person,
                visualColors = personalColors,
                selected = selectedTab == BillCategoryTab.PERSONAL,
                isHigher = summary.higherOverall == HigherBillCategory.PERSONAL,
                language = language,
                onClick = { onSelectTab(BillCategoryTab.PERSONAL) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("bills_compare_tile_personal")
            )
        }

        // Today's quick comparison strip when there is spending today
        if (summary.higherToday != HigherBillCategory.NONE) {
            Spacer(Modifier.height(10.dp))
            val todayLine = when (summary.higherToday) {
                HigherBillCategory.HOUSEHOLD -> tr(
                    language,
                    "Leo: Nyumbani iko juu kwa ${FormatUtils.formatMoney(summary.todayDifference, currency)}",
                    "Today: Household is higher by ${FormatUtils.formatMoney(summary.todayDifference, currency)}"
                )
                HigherBillCategory.PERSONAL -> tr(
                    language,
                    "Leo: Binafsi iko juu kwa ${FormatUtils.formatMoney(summary.todayDifference, currency)}",
                    "Today: Personal is higher by ${FormatUtils.formatMoney(summary.todayDifference, currency)}"
                )
                HigherBillCategory.EQUAL -> tr(
                    language,
                    "Leo: Nyumbani na Binafsi ziko sawa",
                    "Today: Household and Personal are equal"
                )
                HigherBillCategory.NONE -> ""
            }
            if (todayLine.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = todayLine,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        if (summary.dueNowCount > 0) {
            Spacer(Modifier.height(12.dp))
            FilledTonalButton(
                onClick = onPayAllDueBills,
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("bills_pay_all_due_btn")
            ) {
                Icon(Icons.Rounded.DoneAll, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = tr(
                        language,
                        "Lipa Bili Zote Zinazodaiwa Sasa (${summary.dueNowCount})",
                        "Pay All Due Bills Now (${summary.dueNowCount})"
                    ),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ComparisonCategoryTile(
    title: String,
    totalText: String,
    todayText: String,
    avgText: String,
    metaText: String,
    icon: ImageVector,
    visualColors: BucketVisualColors,
    selected: Boolean,
    isHigher: Boolean,
    language: AppLanguage,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (selected) visualColors.softBg else MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) visualColors.dot else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(visualColors.dot),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                if (isHigher) {
                    Surface(
                        shape = CircleShape,
                        color = visualColors.dot
                    ) {
                        Text(
                            text = tr(language, "JUU", "HIGH"),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) visualColors.ink else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = totalText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (selected) visualColors.ink else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = todayText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = avgText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = metaText,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = visualColors.ink,
                maxLines = 1
            )
        }
    }
}

/**
 * Automatic per-day section header shown inside each category list (Household Bill / Personal Bill).
 * Displays friendly date, automatic daily total, item count, and comparison against the other
 * category on that same day.
 */
@Composable
fun BillsDailyGroupHeader(
    group: DailyBillGroup,
    tab: BillCategoryTab,
    currency: String,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val visualColors = bucketVisualColors(
        if (tab == BillCategoryTab.HOUSEHOLD) AutoBucket.HOUSEHOLD_BILL else AutoBucket.BILL
    )
    val otherShortLabel = if (tab == BillCategoryTab.HOUSEHOLD) {
        tr(language, "Binafsi", "Personal")
    } else {
        tr(language, "Nyumbani", "Household")
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(visualColors.dot)
                )
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = FormatUtils.friendlyDateLabel(group.dayIso, language),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Surface(
                            shape = CircleShape,
                            color = visualColors.softBg
                        ) {
                            Text(
                                text = "${group.totalCount}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = visualColors.ink,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 1.dp)
                            )
                        }
                    }
                    if (group.otherCategoryDayTotal > 0.005) {
                        val compareNote = when {
                            group.isHigherThanOtherOnDay -> tr(
                                language,
                                "Juu ya $otherShortLabel kwa +${FormatUtils.formatCompactMoney(group.dayDifference, currency)}",
                                "Higher than $otherShortLabel by +${FormatUtils.formatCompactMoney(group.dayDifference, currency)}"
                            )
                            group.isLowerThanOtherOnDay -> tr(
                                language,
                                "$otherShortLabel siku hii: ${FormatUtils.formatCompactMoney(group.otherCategoryDayTotal, currency)}",
                                "$otherShortLabel this day: ${FormatUtils.formatCompactMoney(group.otherCategoryDayTotal, currency)}"
                            )
                            else -> tr(
                                language,
                                "Sawa na $otherShortLabel siku hii",
                                "Equal to $otherShortLabel this day"
                            )
                        }
                        Text(
                            text = compareNote,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (group.isHigherThanOtherOnDay) visualColors.ink else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = tr(language, "Jumla ya siku", "Day total"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = FormatUtils.formatMoney(group.dayTotal, currency),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = visualColors.ink,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Matte card representing an expense transaction automatically synced from the ledger
 * into either the Household Bill or Personal Bill per-day list (zero duplicate counting).
 */
@Composable
fun AutoBillExpenseRowCard(
    entry: BillLedgerEntry.AutoExpense,
    currency: String,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val txn = entry.txn
    val colors = LocalMengiColors.current
    val bucket = if (entry.tab == BillCategoryTab.HOUSEHOLD) {
        AutoBucket.HOUSEHOLD_BILL
    } else {
        AutoBucket.BILL
    }
    val visualColors = bucketVisualColors(bucket)

    val subtitleParts = buildList {
        if (txn.party.isNotBlank()) add(txn.party)
        add(FormatUtils.localizedCategory(txn.category, language))
        add(FormatUtils.localizedMethod(txn.method, language))
    }

    MengiCard(
        modifier = modifier.testTag("auto_bill_txn_${txn.id}"),
        contentPadding = 14.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(visualColors.softBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryIconForName(txn.category),
                    contentDescription = FormatUtils.localizedCategory(txn.category, language),
                    tint = visualColors.dot,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitleParts.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusPill(
                        text = tr(language, "Auto · Miamala", "Auto · Ledger"),
                        containerColor = visualColors.softBg,
                        contentColor = visualColors.ink
                    )
                    StatusPill(
                        text = tr(language, "Imelipwa", "Paid"),
                        containerColor = colors.incomeSoft,
                        contentColor = colors.income
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = FormatUtils.formatMoney(txn.amount, currency),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = FormatUtils.friendlyDateLabel(entry.dayIso, language),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}
