package tz.mengidata.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.HomeWork
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WorkOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tz.mengidata.app.data.accounting.CategoryAnalyticsEngine
import tz.mengidata.app.data.accounting.CategoryAnalyticsReport
import tz.mengidata.app.data.accounting.CategoryBreakdownItem
import tz.mengidata.app.data.db.TransactionCategoryEntity
import tz.mengidata.app.i18n.AppLanguage
import tz.mengidata.app.i18n.FormatUtils
import tz.mengidata.app.i18n.tr
import tz.mengidata.app.ui.theme.ContrastAccessibility
import tz.mengidata.app.ui.theme.LocalMengiColors
import java.util.Locale

fun categoryIconForKey(key: String): ImageVector = when (key.lowercase()) {
    "store" -> Icons.Rounded.Storefront
    "work" -> Icons.Rounded.WorkOutline
    "receipt" -> Icons.AutoMirrored.Rounded.ReceiptLong
    "inventory" -> Icons.Rounded.Inventory2
    "home" -> Icons.Rounded.HomeWork
    "people" -> Icons.Rounded.Groups
    "bolt" -> Icons.Rounded.Bolt
    "wifi" -> Icons.Rounded.Wifi
    "truck" -> Icons.Rounded.LocalShipping
    "food" -> Icons.Rounded.Restaurant
    "build" -> Icons.Rounded.Build
    "campaign" -> Icons.Rounded.Campaign
    "shield" -> Icons.Rounded.Security
    "savings" -> Icons.Rounded.Savings
    else -> Icons.AutoMirrored.Rounded.Label
}

fun parseHexColorSafe(hex: String, fallback: Color = Color(0xFF334B63)): Color {
    return runCatching {
        val clean = hex.trim().removePrefix("#")
        val parsed = clean.toLong(16)
        when (clean.length) {
            6 -> Color((0xFF000000L or parsed).toInt())
            8 -> Color(parsed.toInt())
            else -> fallback
        }
    }.getOrDefault(fallback)
}

@Composable
fun accessibleCategoryAccent(hex: String): Color {
    val raw = parseHexColorSafe(hex, MaterialTheme.colorScheme.primary)
    return ContrastAccessibility.ensureAccessibleForeground(
        preferred = raw,
        background = MaterialTheme.colorScheme.surface,
        minRatio = ContrastAccessibility.WCAG_AA_LARGE
    )
}

@Composable
fun CategoryAnalyticsCard(
    report: CategoryAnalyticsReport,
    currency: String,
    language: AppLanguage,
    maxVisibleItems: Int = 6,
    onCategoryClick: ((categoryName: String, type: String) -> Unit)? = null,
    onManageCategories: (() -> Unit)? = null,
    onExportCategoryCsv: (() -> Unit)? = null
) {
    val colors = LocalMengiColors.current
    var showExpense by remember { mutableStateOf(true) }

    val items = if (showExpense) report.expenseBreakdown else report.incomeBreakdown
    val activeTotal = if (showExpense) report.totalExpense else report.totalIncome
    val visibleItems = remember(items, maxVisibleItems) { items.take(maxVisibleItems) }

    MengiCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tr(language, "Uchambuzi wa Makundi", "Category Breakdown"),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = tr(
                        language,
                        "Mapato na matumizi kwa makundi ya biashara",
                        "Grouped business income & expenses"
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (onExportCategoryCsv != null) {
                    Surface(
                        onClick = onExportCategoryCsv,
                        shape = CircleShape,
                        color = colors.subtleSurface,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("export_category_analytics_csv_btn")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.TableChart,
                                contentDescription = tr(language, "Hamisha CSV ya makundi", "Export category CSV"),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
                if (onManageCategories != null) {
                    Surface(
                        onClick = onManageCategories,
                        shape = CircleShape,
                        color = colors.subtleSurface,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("manage_categories_icon_btn")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Category,
                                contentDescription = tr(language, "Simamia makundi", "Manage categories"),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        OneUiSegmentedTabs(
            options = listOf(
                tr(language, "Matumizi (${report.expenseBreakdown.size})", "Expenses (${report.expenseBreakdown.size})"),
                tr(language, "Mapato (${report.incomeBreakdown.size})", "Income (${report.incomeBreakdown.size})")
            ),
            selectedIndex = if (showExpense) 0 else 1,
            onSelect = { showExpense = it == 0 },
            testTags = listOf("analytics_tab_expense", "analytics_tab_income")
        )

        Spacer(Modifier.height(14.dp))

        if (visibleItems.isEmpty()) {
            Surface(
                color = colors.subtleSurface,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = tr(
                        language,
                        "Hakuna miamala katika kipindi hiki bado.",
                        "No categorized transactions in this period yet."
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (showExpense) {
                        tr(language, "Jumla ya Matumizi", "Total Expenses")
                    } else {
                        tr(language, "Jumla ya Mapato", "Total Income")
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = FormatUtils.formatMoney(activeTotal, currency),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (showExpense) colors.expense else colors.income
                )
            }

            Spacer(Modifier.height(10.dp))

            // Multi-Segment One UI Pill Proportion Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape)
                    .background(colors.subtleSurface),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                visibleItems.forEach { item ->
                    val segColor = accessibleCategoryAccent(item.colorHex)
                    val weight = (item.sharePercent / 100f).coerceAtLeast(0.03f)
                    Box(
                        modifier = Modifier
                            .weight(weight)
                            .fillMaxHeight()
                            .background(segColor)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                visibleItems.forEach { item ->
                    CategoryBreakdownRow(
                        item = item,
                        currency = currency,
                        language = language,
                        onClick = onCategoryClick?.let { cb -> { cb(item.categoryName, item.type) } }
                    )
                }
            }

            if (items.size > visibleItems.size) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = tr(
                        language,
                        "+ makundi mengine ${items.size - visibleItems.size}",
                        "+ ${items.size - visibleItems.size} more categories"
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CategoryBreakdownRow(
    item: CategoryBreakdownItem,
    currency: String,
    language: AppLanguage,
    onClick: (() -> Unit)?
) {
    val colors = LocalMengiColors.current
    val accent = accessibleCategoryAccent(item.colorHex)
    val animatedShare by animateFloatAsState(
        targetValue = (item.sharePercent / 100f).coerceIn(0.02f, 1f),
        animationSpec = tween(420, easing = FastOutSlowInEasing),
        label = "cat_share_bar"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            )
            .padding(vertical = 3.dp, horizontal = 2.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val solidIconTileBg = lerp(MaterialTheme.colorScheme.surface, accent, if (colors.isDark) 0.24f else 0.14f)
            val displayCategory = FormatUtils.localizedCategory(item.categoryName, language)
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(solidIconTileBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryIconForKey(item.iconKey),
                    contentDescription = displayCategory,
                    tint = accent,
                    modifier = Modifier.size(19.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayCategory,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val countLabel = tr(
                    language,
                    "${item.transactionCount} miamala",
                    "${item.transactionCount} ${if (item.transactionCount == 1) "entry" else "entries"}"
                )
                val budgetNote = if (item.monthlyBudget > 0.0) {
                    val pct = (item.budgetRatio * 100f).toInt()
                    " · " + tr(language, "Bajeti $pct%", "Budget $pct%")
                } else ""
                Text(
                    text = countLabel + budgetNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (item.isOverBudget) colors.expense else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = FormatUtils.formatCompactMoney(item.totalAmount, currency),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = String.format(Locale.US, "%.1f%%", item.sharePercent),
                    style = MaterialTheme.typography.labelSmall,
                    color = accent
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(colors.subtleSurface)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedShare)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(accent)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagerSheet(
    categories: List<TransactionCategoryEntity>,
    initialType: String = "expense",
    language: AppLanguage,
    onDismiss: () -> Unit,
    onSaveCategory: (TransactionCategoryEntity, previousName: String?) -> Unit,
    onDeleteCategory: (categoryId: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = LocalMengiColors.current

    var activeType by remember(initialType) { mutableStateOf(initialType) }
    var editing by remember { mutableStateOf<TransactionCategoryEntity?>(null) }
    var nameInput by remember { mutableStateOf("") }
    var iconKey by remember { mutableStateOf("tag") }
    var colorHex by remember { mutableStateOf(CategoryAnalyticsEngine.paletteHexOptions.first()) }
    var budgetText by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    val visibleCats = remember(categories, activeType) {
        categories.filter { it.type == activeType }
    }

    fun resetForm() {
        editing = null
        nameInput = ""
        iconKey = "tag"
        colorHex = if (activeType == "income") "#047857" else "#0064D2"
        budgetText = ""
        errorText = null
    }

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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = tr(language, "Simamia Makundi ya Biashara", "Manage Business Categories"),
                style = MaterialTheme.typography.headlineMedium
            )

            OneUiSegmentedTabs(
                options = listOf(
                    tr(language, "Matumizi", "Expenses"),
                    tr(language, "Mapato", "Income")
                ),
                selectedIndex = if (activeType == "expense") 0 else 1,
                onSelect = { idx ->
                    activeType = if (idx == 0) "expense" else "income"
                    resetForm()
                },
                testTags = listOf("cat_mgr_tab_expense", "cat_mgr_tab_income")
            )

            // Create / Edit Form Card
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = colors.subtleSurface,
                border = BorderStroke(1.dp, colors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (editing == null) {
                            tr(language, "Ongeza Kundi Jipya", "Create New Category")
                        } else {
                            val editedDisplay = FormatUtils.localizedCategory(editing!!.name, language)
                            tr(language, "Hariri Kundi: $editedDisplay", "Edit Category: $editedDisplay")
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it; errorText = null },
                        label = { Text(tr(language, "Jina la kundi *", "Category name *")) },
                        placeholder = { Text(tr(language, "mf. Mafuta na Usafirishaji", "e.g. Fuel & Delivery")) },
                        singleLine = true,
                        shape = OneUiFieldShape,
                        colors = oneUiTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("category_name_input")
                    )

                    OutlinedTextField(
                        value = budgetText,
                        onValueChange = { budgetText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text(tr(language, "Bajeti / Lengo la Mwezi (Hiari)", "Monthly Budget / Target (Optional)")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = OneUiFieldShape,
                        colors = oneUiTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("category_budget_input")
                    )

                    Text(
                        text = tr(language, "Chagua Aikoni", "Pick Icon"),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CategoryAnalyticsEngine.iconKeyOptions.forEach { key ->
                            val selected = iconKey == key
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surface
                                    )
                                    .border(
                                        width = if (selected) 1.5.dp else 0.75.dp,
                                        color = if (selected) MaterialTheme.colorScheme.primary else colors.cardBorder,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable { iconKey = key },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = categoryIconForKey(key),
                                    contentDescription = key,
                                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = tr(language, "Chagua Rangi", "Pick Color"),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CategoryAnalyticsEngine.paletteHexOptions.forEach { hex ->
                            val swatch = parseHexColorSafe(hex)
                            val selected = colorHex.equals(hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(swatch)
                                    .border(
                                        width = if (selected) 2.5.dp else 1.dp,
                                        color = if (selected) MaterialTheme.colorScheme.onSurface else colors.cardBorder,
                                        shape = CircleShape
                                    )
                                    .clickable { colorHex = hex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (selected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (errorText != null) {
                        Text(
                            text = errorText!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (editing != null) {
                            OutlinedButton(
                                onClick = { resetForm() },
                                shape = CircleShape,
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Text(tr(language, "Ghairi", "Cancel"))
                            }
                        }
                        Button(
                            onClick = {
                                val clean = nameInput.trim()
                                if (clean.length < 2) {
                                    errorText = tr(language, "Weka jina la kundi.", "Enter a valid category name.")
                                    return@Button
                                }
                                val budgetVal = budgetText.toDoubleOrNull() ?: 0.0
                                val current = editing
                                val entity = (current ?: TransactionCategoryEntity(
                                    id = "",
                                    name = clean,
                                    type = activeType
                                )).copy(
                                    name = clean,
                                    type = activeType,
                                    iconKey = iconKey,
                                    colorHex = colorHex,
                                    monthlyBudget = budgetVal
                                )
                                onSaveCategory(entity, current?.name)
                                resetForm()
                            },
                            shape = CircleShape,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("save_category_btn")
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (editing == null) tr(language, "Ongeza", "Add Category")
                                else tr(language, "Sasisha", "Update")
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = colors.cardBorder)

            Text(
                text = tr(
                    language,
                    "Makundi yaliyopo (${visibleCats.size})",
                    "Existing categories (${visibleCats.size})"
                ),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = colors.subtleSurface,
                border = BorderStroke(1.dp, colors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    visibleCats.forEachIndexed { idx, cat ->
                        val accent = accessibleCategoryAccent(cat.colorHex)
                        val solidTileBg = lerp(MaterialTheme.colorScheme.surface, accent, if (colors.isDark) 0.24f else 0.14f)
                        val displayName = FormatUtils.localizedCategory(cat.name, language)
                        val isFallback = cat.name == "Mapato mengine" || cat.name == "Matumizi mengine"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(solidTileBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = categoryIconForKey(cat.iconKey),
                                    contentDescription = displayName,
                                    tint = accent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = displayName,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (cat.monthlyBudget > 0.0) {
                                    Text(
                                        text = tr(language, "Bajeti", "Budget") + ": ${FormatUtils.formatCompactMoney(cat.monthlyBudget)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    editing = cat
                                    nameInput = cat.name
                                    iconKey = cat.iconKey
                                    colorHex = cat.colorHex
                                    budgetText = if (cat.monthlyBudget > 0.0) cat.monthlyBudget.toLong().toString() else ""
                                    errorText = null
                                },
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = tr(language, "Hariri", "Edit"),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            if (!isFallback) {
                                IconButton(
                                    onClick = { onDeleteCategory(cat.id) },
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Delete,
                                        contentDescription = tr(language, "Futa", "Delete"),
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                        if (idx < visibleCats.lastIndex) {
                            HorizontalDivider(
                                color = colors.cardBorder,
                                modifier = Modifier.padding(start = 62.dp, end = 14.dp)
                            )
                        }
                    }
                }
            }

            TextButton(
                onClick = onDismiss,
                shape = CircleShape,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(tr(language, "Funga", "Close"))
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
