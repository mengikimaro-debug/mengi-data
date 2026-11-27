package tz.mengidata.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import tz.mengidata.app.R
import tz.mengidata.app.data.db.BillEntity
import tz.mengidata.app.data.db.InvoiceWithItems
import tz.mengidata.app.data.db.SavingsGoalEntity
import tz.mengidata.app.data.db.TransactionEntity
import tz.mengidata.app.data.prefs.PinSecurityHelper
import tz.mengidata.app.i18n.AppLanguage
import tz.mengidata.app.i18n.FormatUtils
import tz.mengidata.app.i18n.tr
import tz.mengidata.app.ui.navigation.BillsRoute
import tz.mengidata.app.ui.navigation.HubRoute
import tz.mengidata.app.ui.navigation.InvoicesRoute
import tz.mengidata.app.ui.navigation.TransactionsRoute
import tz.mengidata.app.ui.theme.LocalMengiColors
import java.util.Locale

private data class SearchResultRow(
    val id: String,
    val scope: String,
    val title: String,
    val subtitle: String,
    val amountText: String,
    val amountColor: Color,
    val icon: ImageVector,
    val route: Any
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSearchSheet(
    transactions: List<TransactionEntity>,
    invoices: List<InvoiceWithItems>,
    bills: List<BillEntity>,
    savingsGoals: List<SavingsGoalEntity>,
    currency: String,
    language: AppLanguage,
    onNavigate: (Any) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalMengiColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }
    var scope by remember { mutableStateOf("all") }

    val results = remember(query, scope, transactions, invoices, bills, savingsGoals, colors) {
        val q = query.trim().lowercase(Locale.ROOT)
        val out = mutableListOf<SearchResultRow>()

        if (scope == "all" || scope == "txns") {
            transactions.filter { t ->
                val localizedCat = FormatUtils.localizedCategory(t.category, language)
                q.isEmpty() ||
                    t.description.lowercase(Locale.ROOT).contains(q) ||
                    t.party.lowercase(Locale.ROOT).contains(q) ||
                    t.category.lowercase(Locale.ROOT).contains(q) ||
                    localizedCat.lowercase(Locale.ROOT).contains(q) ||
                    t.reference.lowercase(Locale.ROOT).contains(q)
            }.take(if (q.isEmpty()) 4 else 25).forEach { t ->
                val localizedCat = FormatUtils.localizedCategory(t.category, language)
                out += SearchResultRow(
                    id = "txn_${t.id}",
                    scope = "txns",
                    title = t.description.ifBlank { localizedCat },
                    subtitle = listOf(t.party, localizedCat, t.date).filter { it.isNotBlank() }.joinToString(" · "),
                    amountText = (if (t.isIncome) "+" else "-") + FormatUtils.formatCompactMoney(t.amount, currency),
                    amountColor = if (t.isIncome) colors.income else colors.expense,
                    icon = Icons.Rounded.AccountBalanceWallet,
                    route = TransactionsRoute
                )
            }
        }

        if (scope == "all" || scope == "invoices") {
            invoices.filter { inv ->
                q.isEmpty() ||
                    inv.invoice.number.lowercase(Locale.ROOT).contains(q) ||
                    inv.invoice.customerName.lowercase(Locale.ROOT).contains(q) ||
                    inv.items.any { it.description.lowercase(Locale.ROOT).contains(q) }
            }.take(if (q.isEmpty()) 3 else 20).forEach { inv ->
                out += SearchResultRow(
                    id = "inv_${inv.invoice.id}",
                    scope = "invoices",
                    title = "${inv.invoice.number} · ${inv.invoice.customerName}",
                    subtitle = "${inv.invoice.issueDate} → ${inv.invoice.dueDate}",
                    amountText = FormatUtils.formatCompactMoney(inv.total, currency),
                    amountColor = if (inv.isPaid) colors.income else colors.warning,
                    icon = Icons.Rounded.Description,
                    route = InvoicesRoute
                )
            }
        }

        if (scope == "all" || scope == "bills") {
            bills.filter { b ->
                val localizedCat = FormatUtils.localizedCategory(b.category, language)
                q.isEmpty() ||
                    b.name.lowercase(Locale.ROOT).contains(q) ||
                    b.payee.lowercase(Locale.ROOT).contains(q) ||
                    b.category.lowercase(Locale.ROOT).contains(q) ||
                    localizedCat.lowercase(Locale.ROOT).contains(q)
            }.take(if (q.isEmpty()) 3 else 20).forEach { b ->
                out += SearchResultRow(
                    id = "bill_${b.id}",
                    scope = "bills",
                    title = b.name,
                    subtitle = listOf(b.payee, FormatUtils.localizedCategory(b.category, language), b.dueDate).filter { it.isNotBlank() }.joinToString(" · "),
                    amountText = FormatUtils.formatCompactMoney(b.amount, currency),
                    amountColor = if (b.paid) colors.income else colors.expense,
                    icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                    route = BillsRoute
                )
            }
        }

        if (scope == "all" || scope == "savings") {
            savingsGoals.filter { g ->
                q.isEmpty() ||
                    g.title.lowercase(Locale.ROOT).contains(q) ||
                    g.notes.lowercase(Locale.ROOT).contains(q)
            }.take(if (q.isEmpty()) 2 else 15).forEach { g ->
                out += SearchResultRow(
                    id = "svg_${g.id}",
                    scope = "savings",
                    title = g.title,
                    subtitle = "${g.progressPercent}% · ${FormatUtils.formatCompactMoney(g.targetAmount, currency)}",
                    amountText = FormatUtils.formatCompactMoney(g.currentAmount, currency),
                    amountColor = colors.savings,
                    icon = Icons.Rounded.Savings,
                    route = HubRoute
                )
            }
        }
        out
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
                .fillMaxHeight(0.86f)
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = tr(language, "Utafutaji wa Haraka", "Global Search"),
                style = MaterialTheme.typography.headlineMedium
            )

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(tr(language, "Tafuta miamala, ankara, bili, akiba...", "Search ledger, invoices, bills, savings...")) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = if (query.isNotEmpty()) {
                    {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Rounded.Close, contentDescription = tr(language, "Futa", "Clear"))
                        }
                    }
                } else null,
                singleLine = true,
                shape = CircleShape,
                colors = oneUiTextFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("global_search_input")
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "all" to tr(language, "Yote", "All"),
                    "txns" to tr(language, "Miamala", "Ledger"),
                    "invoices" to tr(language, "Ankara", "Invoices"),
                    "bills" to tr(language, "Bili", "Bills"),
                    "savings" to tr(language, "Akiba", "Savings")
                ).forEach { (key, label) ->
                    FilterChip(
                        selected = scope == key,
                        onClick = { scope = key },
                        shape = CircleShape,
                        label = { Text(label) }
                    )
                }
            }

            if (results.isEmpty()) {
                EmptyStateCard(
                    icon = Icons.Rounded.Search,
                    title = tr(language, "Hakuna matokeo", "No matching records"),
                    subtitle = tr(language, "Jaribu kutafuta kwa jina la mteja, kundi, au namba ya ankara.", "Try searching by customer name, category, or invoice number.")
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(results, key = { it.id }) { row ->
                        MengiCard(
                            onClick = {
                                onDismiss()
                                onNavigate(row.route)
                            },
                            contentPadding = 14.dp
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(13.dp))
                                        .background(colors.subtleSurface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(row.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = row.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = row.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = row.amountText,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = row.amountColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PinLockScreen(
    businessName: String,
    language: AppLanguage,
    onVerifyPin: (String) -> Boolean
) {
    val colors = LocalMengiColors.current
    var entered by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    fun appendDigit(d: String) {
        if (entered.length >= 4) return
        val next = entered + d
        entered = next
        isError = false
        if (next.length == 4) {
            val ok = onVerifyPin(next)
            if (!ok) {
                isError = true
                entered = ""
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(colors.heroStart, colors.heroMid, colors.heroEnd)
                )
            )
            .safeDrawingPadding()
            .padding(horizontal = 24.dp, vertical = 32.dp)
            .testTag("pin_lock_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF2B3745),
                border = BorderStroke(1.5.dp, Color(0xFF56687A)),
                shadowElevation = 4.dp,
                modifier = Modifier.size(76.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.mengidata_oneui_icon_1795525888393),
                    contentDescription = businessName.ifBlank { "VaultLedger" },
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = businessName.ifBlank { "VaultLedger" },
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color(0xFFE6EAF0)
                )
                Text(
                    text = if (isError) {
                        tr(language, "Namba ya siri si sahihi. Jaribu tena.", "Incorrect PIN. Try again.")
                    } else {
                        tr(language, "Ingiza namba ya siri ya tarakimu 4 kufungua", "Enter 4-digit PIN to unlock")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isError) Color(0xFFD6C2A5) else Color(0xFFBDC7D4),
                    textAlign = TextAlign.Center
                )
            }

            // 4 Security Code Dots (Calm Matte Solid)
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                repeat(4) { idx ->
                    val filled = idx < entered.length
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isError -> Color(0xFFC2A684)
                                    filled -> colors.lime
                                    else -> Color(0xFF394654)
                                }
                            )
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            // Calm Matte Solid Numeric Keypad
            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("", "0", "DEL")
            )
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                rows.forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(22.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        row.forEach { key ->
                            when (key) {
                                "" -> Spacer(Modifier.size(68.dp))
                                "DEL" -> {
                                    Surface(
                                        onClick = { if (entered.isNotEmpty()) entered = entered.dropLast(1) },
                                        shape = CircleShape,
                                        color = Color(0xFF232C36),
                                        border = BorderStroke(1.dp, Color(0xFF394654)),
                                        modifier = Modifier
                                            .size(68.dp)
                                            .testTag("pin_key_del")
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.AutoMirrored.Rounded.Backspace,
                                                contentDescription = tr(language, "Futa", "Delete"),
                                                tint = Color(0xFFE6EAF0),
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }
                                else -> {
                                    Surface(
                                        onClick = { appendDigit(key) },
                                        shape = CircleShape,
                                        color = Color(0xFF2B3642),
                                        border = BorderStroke(1.dp, Color(0xFF425163)),
                                        modifier = Modifier
                                            .size(68.dp)
                                            .testTag("pin_key_$key")
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = key,
                                                style = MaterialTheme.typography.headlineMedium,
                                                color = Color(0xFFE6EAF0)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PinSetupDialog(
    language: AppLanguage,
    isChangingExisting: Boolean,
    onDismiss: () -> Unit,
    onSavePin: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = OneUiDialogShape,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                if (isChangingExisting) tr(language, "Badili Namba ya Siri", "Change Security PIN")
                else tr(language, "Weka Namba ya Siri (Tarakimu 4)", "Set 4-Digit Security PIN")
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = tr(
                        language,
                        "Namba ya siri huhifadhiwa kwa usimbaji wa SHA-256 kulinda taarifa zako za biashara.",
                        "Your PIN is stored with salted SHA-256 hashing to protect your business records."
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it.filter { c -> c.isDigit() }.take(4); error = null },
                    label = { Text(tr(language, "Namba ya Siri Mpya (tarakimu 4)", "New 4-Digit PIN")) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    shape = OneUiFieldShape,
                    colors = oneUiTextFieldColors(),
                    modifier = Modifier.fillMaxWidth().testTag("pin_setup_input")
                )
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { confirmPin = it.filter { c -> c.isDigit() }.take(4); error = null },
                    label = { Text(tr(language, "Thibitisha Namba ya Siri", "Confirm PIN")) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    shape = OneUiFieldShape,
                    colors = oneUiTextFieldColors(),
                    modifier = Modifier.fillMaxWidth().testTag("pin_setup_confirm_input")
                )
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!PinSecurityHelper.isValidPin(pin)) {
                        error = tr(language, "Weka tarakimu 4 kamili.", "Enter exactly 4 digits.")
                        return@Button
                    }
                    if (pin != confirmPin) {
                        error = tr(language, "Namba za siri hazilingani.", "PINs do not match.")
                        return@Button
                    }
                    onSavePin(pin)
                },
                shape = CircleShape,
                modifier = Modifier.testTag("pin_setup_save_btn")
            ) {
                Text(tr(language, "Hifadhi Namba ya Siri", "Save PIN"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, shape = CircleShape) {
                Text(tr(language, "Ghairi", "Cancel"))
            }
        }
    )
}
