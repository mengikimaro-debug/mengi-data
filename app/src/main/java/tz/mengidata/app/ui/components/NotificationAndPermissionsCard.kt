package tz.mengidata.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FolderCopy
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import tz.mengidata.app.i18n.AppLanguage
import tz.mengidata.app.i18n.FormatUtils
import tz.mengidata.app.i18n.tr
import tz.mengidata.app.notifications.PhonePermissionsSnapshot
import tz.mengidata.app.ui.theme.LocalMengiColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NotificationAndPermissionsCard(
    language: AppLanguage,
    currency: String,
    phonePermissions: PhonePermissionsSnapshot,
    notificationsEnabled: Boolean,
    billAlertsEnabled: Boolean,
    transactionAlertsEnabled: Boolean,
    savingsAlertsEnabled: Boolean,
    systemAlertsEnabled: Boolean,
    largeBillThreshold: Double,
    onRequestPostNotificationsPermission: () -> Unit,
    onOpenInstallPermissionSettings: () -> Unit,
    onOpenAppNotificationSettings: () -> Unit,
    onOpenAppDetailsSettings: () -> Unit,
    onSetNotificationsEnabled: (Boolean) -> Unit,
    onSetBillAlertsEnabled: (Boolean) -> Unit,
    onSetTransactionAlertsEnabled: (Boolean) -> Unit,
    onSetSavingsAlertsEnabled: (Boolean) -> Unit,
    onSetSystemAlertsEnabled: (Boolean) -> Unit,
    onSaveLargeBillThreshold: (Double) -> Unit,
    onTestLargeBillWarningPopup: () -> Unit,
    onTestSystemNotification: () -> Unit,
    onSendDueRemindersNow: () -> Unit
) {
    val colors = LocalMengiColors.current
    var thresholdText by remember(largeBillThreshold) {
        mutableStateOf(largeBillThreshold.toLong().toString())
    }

    MengiCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.warningSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.NotificationsActive,
                    contentDescription = null,
                    tint = colors.warning,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tr(
                        language,
                        "Kidhibiti cha Taarifa & Ruhusa za Simu",
                        "Notification Manager & Phone Permissions"
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = tr(
                        language,
                        "Ruhusa zote za Android 13+ na onyo la pop-up la sekunde 3 kwa bili kubwa.",
                        "Android 13+ permissions & 3-second pop-up warning for large bills."
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StatusPill(
                text = if (phonePermissions.allRequiredGranted) {
                    tr(language, "Ruhusa Zote Sawa", "All Granted")
                } else {
                    tr(language, "Ruhusu Simu", "Action Needed")
                },
                containerColor = if (phonePermissions.allRequiredGranted) colors.incomeSoft else colors.warningSoft,
                contentColor = if (phonePermissions.allRequiredGranted) colors.income else colors.warning
            )
        }

        Spacer(Modifier.height(14.dp))

        // 1. Phone Permissions Status Grid (Android 13+)
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
                Text(
                    text = tr(language, "1. Hali ya Ruhusa za Simu (Android 13+)", "1. Phone Permissions Status (Android 13+)"),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )

                PermissionStatusRow(
                    icon = Icons.Rounded.NotificationsActive,
                    title = tr(language, "Ruhusa ya Taarifa (POST_NOTIFICATIONS)", "Notifications (POST_NOTIFICATIONS)"),
                    subtitle = if (phonePermissions.postNotificationsGranted && phonePermissions.systemNotificationsEnabled) {
                        tr(language, "Imewashwa — chaneli ${phonePermissions.channelsConfigured} zipo tayari", "Granted — ${phonePermissions.channelsConfigured} channels active")
                    } else {
                        tr(language, "Bonyeza kuruhusu taarifa za bili na onyo la sekunde 3", "Tap to allow bill alerts & 3-second warnings")
                    },
                    granted = phonePermissions.postNotificationsGranted && phonePermissions.systemNotificationsEnabled,
                    actionLabel = if (phonePermissions.postNotificationsGranted && phonePermissions.systemNotificationsEnabled) {
                        tr(language, "Sawa", "Active")
                    } else {
                        tr(language, "Ruhusu", "Allow")
                    },
                    onAction = {
                        if (!phonePermissions.postNotificationsGranted) {
                            onRequestPostNotificationsPermission()
                        } else {
                            onOpenAppNotificationSettings()
                        }
                    },
                    testTag = "perm_post_notifications_btn"
                )

                PermissionStatusRow(
                    icon = Icons.Rounded.SystemUpdate,
                    title = tr(language, "Kusakinisha Masasisho (Install Unknown Apps)", "Install Updates (REQUEST_INSTALL_PACKAGES)"),
                    subtitle = if (phonePermissions.installPackagesGranted) {
                        tr(language, "Inaruhusu kusakinisha Mengidata.apk kutoka GitHub", "Allowed to install Mengidata.apk from GitHub")
                    } else {
                        tr(language, "Inahitajika wakati wa kusakinisha toleo jipya la GitHub", "Required to install new GitHub APK updates")
                    },
                    granted = phonePermissions.installPackagesGranted,
                    actionLabel = if (phonePermissions.installPackagesGranted) {
                        tr(language, "Sawa", "Active")
                    } else {
                        tr(language, "Ruhusu", "Allow")
                    },
                    onAction = onOpenInstallPermissionSettings,
                    testTag = "perm_install_packages_btn"
                )

                PermissionStatusRow(
                    icon = Icons.Rounded.FolderCopy,
                    title = tr(language, "Hifadhi ya Simu & PDF (Documents/Mengidata)", "Phone Storage & PDF Vault (Scoped Storage)"),
                    subtitle = tr(
                        language,
                        "Inafanya kazi moja kwa moja kwenye Android 13+ bila ruhusa hatarishi",
                        "Zero-permission MediaStore & SAF active on Android 13+"
                    ),
                    granted = true,
                    actionLabel = tr(language, "Tayari", "Ready"),
                    onAction = onOpenAppDetailsSettings,
                    testTag = "perm_storage_vault_btn"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenAppNotificationSettings,
                        shape = CircleShape,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("open_sys_notif_settings_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(4.dp))
                        Text(tr(language, "Chaneli za Simu", "System Channels"))
                    }
                    OutlinedButton(
                        onClick = onOpenAppDetailsSettings,
                        shape = CircleShape,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("open_app_details_settings_btn")
                    ) {
                        Icon(Icons.Rounded.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(4.dp))
                        Text(tr(language, "Ruhusa za App", "App Info"))
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // 2. 3-Second Large Bill Warning Pop-Up Configuration
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = colors.expenseSoft,
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
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WarningAmber,
                            contentDescription = null,
                            tint = colors.expense,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = tr(language, "2. Onyo la Bili Kubwa (3-Sec Pop-Up)", "2. Large Bill Warning (3-Sec Pop-Up)"),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = colors.expense
                            )
                            Text(
                                text = tr(
                                    language,
                                    "Kiwango cha sasa: ${FormatUtils.formatMoney(largeBillThreshold, currency)}",
                                    "Current limit: ${FormatUtils.formatMoney(largeBillThreshold, currency)}"
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    StatusPill(
                        text = "3 SEC",
                        containerColor = colors.warningSoft,
                        contentColor = colors.warning
                    )
                }

                Text(
                    text = tr(
                        language,
                        "Kila unapohifadhi au kulipa bili/matumizi yanayofika au kuzidi kiwango hiki, dirisha la onyo (Pop-Up View) litatokea juu ya skrini kwa sekunde 3 pamoja na taarifa ya simu.",
                        "Whenever a bill or expense meets or exceeds this amount, a 3-second pop-up warning banner appears at the top of the screen alongside a high-priority notification."
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val presets = listOf(50_000.0, 100_000.0, 250_000.0, 500_000.0)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presets.forEach { preset ->
                        FilterChip(
                            selected = kotlin.math.abs(largeBillThreshold - preset) < 1.0,
                            onClick = {
                                thresholdText = preset.toLong().toString()
                                onSaveLargeBillThreshold(preset)
                            },
                            label = { Text(FormatUtils.formatMoney(preset, currency)) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = thresholdText,
                        onValueChange = { raw -> thresholdText = raw.filter { it.isDigit() } },
                        label = {
                            Text(tr(language, "Kiwango cha Bili Kubwa ($currency)", "Large Bill Limit ($currency)"))
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = OneUiFieldShape,
                        colors = oneUiTextFieldColors(),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("large_bill_threshold_input")
                    )
                    Button(
                        onClick = {
                            val parsed = thresholdText.toDoubleOrNull() ?: largeBillThreshold
                            onSaveLargeBillThreshold(parsed)
                        },
                        shape = CircleShape,
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("save_large_bill_threshold_btn")
                    ) {
                        Text(tr(language, "Hifadhi", "Set"))
                    }
                }

                Button(
                    onClick = onTestLargeBillWarningPopup,
                    shape = CircleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("test_large_bill_popup_btn")
                ) {
                    Icon(Icons.Rounded.WarningAmber, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(
                        tr(
                            language,
                            "Jaribu Onyo la Bili Kubwa (Pop-Up Sekunde 3)",
                            "Test Large Bill Warning (3-Sec Pop-Up)"
                        )
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        HorizontalDivider(color = colors.cardBorder)
        Spacer(Modifier.height(12.dp))

        // 3. Channel Toggles
        Text(
            text = tr(language, "3. Udhibiti wa Aina za Taarifa", "3. Notification Categories"),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))

        NotificationToggleRow(
            title = tr(language, "Washa Taarifa Zote za Mengidata", "Enable All Mengidata Notifications"),
            subtitle = tr(language, "Swichi kuu ya taarifa za mfumo kwenye simu", "Master toggle for phone notifications"),
            checked = notificationsEnabled,
            onCheckedChange = onSetNotificationsEnabled,
            testTag = "notif_master_switch"
        )
        NotificationToggleRow(
            title = tr(language, "Bili za Nyumbani, Ankara & Onyo la Bili Kubwa", "Household Bills, Invoices & Large Bill Warning"),
            subtitle = tr(language, "Inajumuisha pop-up ya sekunde 3 bili ikiwa kubwa", "Includes 3-second pop-up warning when a bill is large"),
            checked = billAlertsEnabled && notificationsEnabled,
            onCheckedChange = onSetBillAlertsEnabled,
            enabled = notificationsEnabled,
            testTag = "notif_bills_switch"
        )
        NotificationToggleRow(
            title = tr(language, "Miamala Mipya (Mapato & Matumizi)", "Ledger Transactions (Income & Expenses)"),
            subtitle = tr(language, "Taarifa kila muamala unapohifadhiwa kwenye leja", "Alerts when transactions are recorded"),
            checked = transactionAlertsEnabled && notificationsEnabled,
            onCheckedChange = onSetTransactionAlertsEnabled,
            enabled = notificationsEnabled,
            testTag = "notif_txns_switch"
        )
        NotificationToggleRow(
            title = tr(language, "Akiba & Malengo (Savings Vault)", "Savings Deposits & Goal Milestones"),
            subtitle = tr(language, "Taarifa za kuweka, kutoa na kufikia lengo la akiba", "Alerts for deposits, withdrawals & goal completion"),
            checked = savingsAlertsEnabled && notificationsEnabled,
            onCheckedChange = onSetSavingsAlertsEnabled,
            enabled = notificationsEnabled,
            testTag = "notif_savings_switch"
        )
        NotificationToggleRow(
            title = tr(language, "Hifadhi ya Simu & Masasisho ya GitHub", "Phone Storage Sync & GitHub Updates"),
            subtitle = tr(language, "Taarifa za kuhifadhi nakala na toleo jipya la programu", "Backup sync confirmation & new release alerts"),
            checked = systemAlertsEnabled && notificationsEnabled,
            onCheckedChange = onSetSystemAlertsEnabled,
            enabled = notificationsEnabled,
            testTag = "notif_system_switch"
        )

        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalButton(
                onClick = onTestSystemNotification,
                shape = CircleShape,
                modifier = Modifier
                    .weight(1f)
                    .testTag("send_test_notification_btn")
            ) {
                Icon(Icons.Rounded.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.size(6.dp))
                Text(tr(language, "Jaribu Taarifa", "Test Alert"))
            }
            OutlinedButton(
                onClick = onSendDueRemindersNow,
                shape = CircleShape,
                modifier = Modifier
                    .weight(1f)
                    .testTag("run_due_reminders_btn")
            ) {
                Icon(Icons.Rounded.VerifiedUser, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.size(6.dp))
                Text(tr(language, "Kagua Madeni", "Due Check"))
            }
        }
    }
}

@Composable
private fun PermissionStatusRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    granted: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
    testTag: String
) {
    val colors = LocalMengiColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = if (granted) Icons.Rounded.CheckCircle else icon,
            contentDescription = null,
            tint = if (granted) colors.income else colors.warning,
            modifier = Modifier.size(20.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (granted) {
            Surface(
                onClick = onAction,
                shape = CircleShape,
                color = colors.incomeSoft,
                modifier = Modifier.testTag(testTag)
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.income,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        } else {
            Button(
                onClick = onAction,
                shape = CircleShape,
                modifier = Modifier
                    .height(36.dp)
                    .testTag(testTag)
            ) {
                Text(actionLabel, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun NotificationToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            modifier = Modifier.testTag(testTag)
        )
    }
}
