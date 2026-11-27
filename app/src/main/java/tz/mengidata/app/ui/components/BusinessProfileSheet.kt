package tz.mengidata.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import tz.mengidata.app.data.db.BusinessSettingsEntity
import tz.mengidata.app.i18n.AppLanguage
import tz.mengidata.app.i18n.tr
import tz.mengidata.app.ui.theme.LocalMengiColors

/**
 * Compact, scrollable Material 3 ModalBottomSheet for editing Business Profile & Invoice Defaults,
 * opened directly from the Sidebar so Settings stays lightweight and focused.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessProfileSheet(
    settings: BusinessSettingsEntity,
    language: AppLanguage,
    onSaveSettings: (BusinessSettingsEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalMengiColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var businessName by remember(settings) { mutableStateOf(settings.businessName) }
    var ownerName by remember(settings) { mutableStateOf(settings.ownerName) }
    var tagline by remember(settings) { mutableStateOf(settings.tagline) }
    var address by remember(settings) { mutableStateOf(settings.address) }
    var phone by remember(settings) { mutableStateOf(settings.phone) }
    var email by remember(settings) { mutableStateOf(settings.email) }
    var tin by remember(settings) { mutableStateOf(settings.tin) }
    var invoicePrefix by remember(settings) { mutableStateOf(settings.invoicePrefix) }
    var vatRateText by remember(settings) { mutableStateOf(settings.vatRate.toString()) }
    var paymentDetails by remember(settings) { mutableStateOf(settings.paymentDetails) }
    var invoiceNote by remember(settings) { mutableStateOf(settings.invoiceNote) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .testTag("business_profile_sheet"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                        .background(colors.incomeSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Business,
                        contentDescription = null,
                        tint = colors.income,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tr(language, "Taarifa za Biashara & Ankara", "Business Profile & Invoice Defaults"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = tr(
                            language,
                            "Zinaonekana kwenye ankara za PDF na risiti.",
                            "Printed on your PDF invoices and business headers."
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            OutlinedTextField(
                value = businessName,
                onValueChange = { businessName = it },
                label = { Text(tr(language, "Jina la Biashara *", "Business Name *")) },
                singleLine = true,
                shape = OneUiFieldShape,
                colors = oneUiTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = ownerName,
                    onValueChange = { ownerName = it },
                    label = { Text(tr(language, "Mmiliki", "Owner")) },
                    singleLine = true,
                    shape = OneUiFieldShape,
                    colors = oneUiTextFieldColors(),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = tin,
                    onValueChange = { tin = it },
                    label = { Text("TIN") },
                    singleLine = true,
                    shape = OneUiFieldShape,
                    colors = oneUiTextFieldColors(),
                    modifier = Modifier.weight(1f)
                )
            }
            OutlinedTextField(
                value = tagline,
                onValueChange = { tagline = it },
                label = { Text(tr(language, "Maelezo Mafupi ya Biashara", "Tagline")) },
                singleLine = true,
                shape = OneUiFieldShape,
                colors = oneUiTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(tr(language, "Simu", "Phone")) },
                    singleLine = true,
                    shape = OneUiFieldShape,
                    colors = oneUiTextFieldColors(),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(tr(language, "Barua pepe", "Email")) },
                    singleLine = true,
                    shape = OneUiFieldShape,
                    colors = oneUiTextFieldColors(),
                    modifier = Modifier.weight(1f)
                )
            }
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text(tr(language, "Anwani ya Ofisi", "Office Address")) },
                singleLine = true,
                shape = OneUiFieldShape,
                colors = oneUiTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = invoicePrefix,
                    onValueChange = { invoicePrefix = it.take(6).uppercase() },
                    label = { Text(tr(language, "Kiambishi cha Ankara", "Invoice Prefix")) },
                    singleLine = true,
                    shape = OneUiFieldShape,
                    colors = oneUiTextFieldColors(),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = vatRateText,
                    onValueChange = { vatRateText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("VAT (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = OneUiFieldShape,
                    colors = oneUiTextFieldColors(),
                    modifier = Modifier.weight(1f)
                )
            }
            OutlinedTextField(
                value = paymentDetails,
                onValueChange = { paymentDetails = it },
                label = { Text(tr(language, "Maelezo ya Malipo (M-Pesa / Benki)", "Payment Details")) },
                minLines = 2,
                shape = OneUiFieldShape,
                colors = oneUiTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = invoiceNote,
                onValueChange = { invoiceNote = it },
                label = { Text(tr(language, "Maelezo ya chini ya ankara", "Invoice Footer Note")) },
                minLines = 2,
                shape = OneUiFieldShape,
                colors = oneUiTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = {
                    onSaveSettings(
                        settings.copy(
                            businessName = businessName.trim().ifBlank { "VaultLedger" },
                            ownerName = ownerName.trim(),
                            tagline = tagline.trim(),
                            address = address.trim(),
                            phone = phone.trim(),
                            email = email.trim(),
                            tin = tin.trim(),
                            invoicePrefix = invoicePrefix.trim().ifBlank { "MD" },
                            vatRate = (vatRateText.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 100.0),
                            paymentDetails = paymentDetails.trim(),
                            invoiceNote = invoiceNote.trim()
                        )
                    )
                    onDismiss()
                },
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_settings_btn")
            ) {
                Text(
                    text = tr(language, "Hifadhi Taarifa za Biashara", "Save Business Settings"),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
