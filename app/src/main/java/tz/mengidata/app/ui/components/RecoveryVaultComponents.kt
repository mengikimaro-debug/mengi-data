package tz.mengidata.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.RestoreFromTrash
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tz.mengidata.app.data.db.DeletedRecordEntity
import tz.mengidata.app.i18n.AppLanguage
import tz.mengidata.app.i18n.FormatUtils
import tz.mengidata.app.i18n.tr
import tz.mengidata.app.ui.theme.LocalMengiColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecoveryVaultSheet(
    deletedRecords: List<DeletedRecordEntity>,
    currency: String,
    language: AppLanguage,
    onRestore: (String) -> Unit,
    onDeletePermanently: (String) -> Unit,
    onEmptyVault: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = LocalMengiColors.current
    var showEmptyConfirm by remember { mutableStateOf(false) }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.incomeSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.RestoreFromTrash,
                            contentDescription = null,
                            tint = colors.income,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                    Column {
                        Text(
                            text = tr(language, "Kikapu cha Urejeshaji", "Recovery Vault"),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = tr(
                                language,
                                "Data zilizofutwa hulindwa hapa — rejesha kwa mguso mmoja.",
                                "Deleted records are protected here — restore in one tap."
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (deletedRecords.isNotEmpty()) {
                    TextButton(
                        onClick = { showEmptyConfirm = true },
                        shape = CircleShape,
                        modifier = Modifier.testTag("empty_recovery_vault_btn")
                    ) {
                        Text(
                            text = tr(language, "Safisha", "Empty"),
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            HorizontalDivider(color = colors.cardBorder)

            if (deletedRecords.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = colors.subtleSurface,
                    border = BorderStroke(1.dp, colors.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shield,
                            contentDescription = null,
                            tint = colors.income,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = tr(language, "Hakuna rekodi iliyofutwa", "No deleted records"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = tr(
                                language,
                                "Ukifuta muamala, bili, ankara au akiba, itahifadhiwa hapa kwanza badala ya kupotea.",
                                "Whenever you delete a transaction, bill, invoice, or savings goal, it is safely archived here first."
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(deletedRecords, key = { it.id }) { record ->
                        DeletedRecordRow(
                            record = record,
                            currency = currency,
                            language = language,
                            onRestore = { onRestore(record.id) },
                            onDeletePermanently = { onDeletePermanently(record.id) }
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showEmptyConfirm) {
        TypedConfirmDangerDialog(
            title = tr(language, "Safisha Kikapu cha Urejeshaji?", "Empty Recovery Vault?"),
            message = tr(
                language,
                "Rekodi zote ${deletedRecords.size} kwenye kikapu zitaondolewa kabisa na hazitaweza kurejeshwa.",
                "All ${deletedRecords.size} archived records in the Recovery Vault will be permanently removed."
            ),
            requiredKeyword = tr(language, "FUTA", "DELETE"),
            confirmLabel = tr(language, "Ondoa Kabisa", "Delete Forever"),
            cancelLabel = tr(language, "Ghairi", "Cancel"),
            language = language,
            onConfirm = {
                onEmptyVault()
                showEmptyConfirm = false
            },
            onDismiss = { showEmptyConfirm = false }
        )
    }
}

@Composable
private fun DeletedRecordRow(
    record: DeletedRecordEntity,
    currency: String,
    language: AppLanguage,
    onRestore: () -> Unit,
    onDeletePermanently: () -> Unit
) {
    val colors = LocalMengiColors.current
    val (typeLabel, icon) = when (record.entityType) {
        "bill" -> tr(language, "Bili", "Bill") to Icons.AutoMirrored.Rounded.ReceiptLong
        "invoice" -> tr(language, "Ankara", "Invoice") to Icons.Rounded.Description
        "savings_goal" -> tr(language, "Akiba", "Savings") to Icons.Rounded.Savings
        else -> tr(language, "Muamala", "Transaction") to Icons.Rounded.AccountBalanceWallet
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = colors.subtleSurface,
        border = BorderStroke(1.dp, colors.cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = typeLabel,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = record.title.ifBlank { typeLabel },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = listOf(typeLabel, record.subtitle, "${tr(language, "Ilifutwa", "Deleted")}: ${record.deletedAt}")
                            .filter { it.isNotBlank() }
                            .joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (record.amount > 0.0) {
                    Text(
                        text = FormatUtils.formatCompactMoney(record.amount, currency),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = onRestore,
                    shape = CircleShape,
                    modifier = Modifier.testTag("restore_trash_${record.id}")
                ) {
                    Icon(Icons.Rounded.RestoreFromTrash, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(tr(language, "Rejesha Sasa", "Restore Now"))
                }
                IconButton(
                    onClick = onDeletePermanently,
                    modifier = Modifier.testTag("purge_trash_${record.id}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteForever,
                        contentDescription = tr(language, "Ondoa kabisa", "Delete permanently"),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

/**
 * High-safety confirmation dialog that prevents accidental data loss by requiring
 * the user to explicitly type a confirmation keyword (e.g. "FUTA" or "DELETE").
 */
@Composable
fun TypedConfirmDangerDialog(
    title: String,
    message: String,
    requiredKeyword: String,
    confirmLabel: String,
    cancelLabel: String,
    language: AppLanguage,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var typedText by remember { mutableStateOf("") }
    val isUnlocked = typedText.trim().equals(requiredKeyword, ignoreCase = true)

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = OneUiDialogShape,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = tr(
                        language,
                        "Ili kulinda data zisifutike kwa bahati mbaya, andika \"$requiredKeyword\" hapa chini:",
                        "To prevent accidental deletion, type \"$requiredKeyword\" below:"
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error
                )
                OutlinedTextField(
                    value = typedText,
                    onValueChange = { typedText = it },
                    placeholder = { Text(requiredKeyword) },
                    singleLine = true,
                    shape = OneUiFieldShape,
                    colors = oneUiTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("danger_confirm_keyword_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = isUnlocked,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                modifier = Modifier.testTag("danger_confirm_submit_btn")
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, shape = CircleShape) {
                Text(cancelLabel)
            }
        }
    )
}
