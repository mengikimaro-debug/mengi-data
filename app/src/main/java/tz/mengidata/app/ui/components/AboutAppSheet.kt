package tz.mengidata.app.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Copyright
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tz.mengidata.app.BuildConfig
import tz.mengidata.app.R
import tz.mengidata.app.i18n.AppLanguage
import tz.mengidata.app.i18n.tr
import tz.mengidata.app.security.CopyrightIntegrityReport
import tz.mengidata.app.security.CopyrightVault
import tz.mengidata.app.ui.theme.LocalMengiColors

private data class AboutWorkflowStep(
    val stepNumber: Int,
    val icon: ImageVector,
    val titleSw: String,
    val titleEn: String,
    val bodySw: String,
    val bodyEn: String
)

private const val FOUNDER_NAME = "Mr Reg Kimaro"
private const val FOUNDER_PHONE_DISPLAY = "+255 675 160 551"
private const val FOUNDER_PHONE_E164 = "+255675160551"
private const val FOUNDER_WHATSAPP_URL = "https://wa.me/255675160551"

private val AboutWorkflowSteps = listOf(
    AboutWorkflowStep(
        stepNumber = 1,
        icon = Icons.Rounded.Home,
        titleSw = "Dashibodi & Muhtasari wa Papo Hapo",
        titleEn = "Dashboard & Instant Financial Overview",
        bodySw = "Ukifungua app unaona mara moja salio lako halisi (Zero-Loss Ledger), jumla ya mapato, matumizi, bili na akiba. Unaweza pia kutumia 'Ingiza Haraka' (Smart Auto-Entry) ambapo mfumo unatambua kiotomatiki kama muamala ni Mapato, Matumizi, Bili ya Nyumbani, Bili Binafsi, au Akiba.",
        bodyEn = "Upon opening the app, you immediately see your verified cash balance (Zero-Loss Ledger), income, expenses, bills, and savings. Smart Auto-Entry automatically classifies entries into Income, Expenses, Household Bills, Personal Bills, or Savings."
    ),
    AboutWorkflowStep(
        stepNumber = 2,
        icon = Icons.Rounded.AccountBalanceWallet,
        titleSw = "Ledger & Malipo Yanayosubiri",
        titleEn = "Cash Ledger & Pending Payments",
        bodySw = "Sehemu ya Ledger inahifadhi historia yote ya fedha iliyopangwa kwa siku, makundi na ripoti za PDF/CSV. Ndani ya bar ya Ledger pia kuna sehemu ya 'Inayosubiri' kwa ajili ya malipo yanayosubiri — rekodi jina, namba ya simu (unaweza kupiga simu moja kwa moja), eneo, maelezo ya muamala, na fuatilia kama hajalipa, amelipa nusu (mfumo ukionyesha bado kiasi gani), au kalipa kiasi chote, kisha hamishia kwenye Ledger kuu.",
        bodyEn = "The Ledger records all income and expenses grouped by day and category with PDF/CSV exports. Inside the Ledger bar, the Pending section tracks pending payments with name, tap-to-call phone number, location, full notes, and payment status (Unpaid, Partial with exact remaining balance, or Paid in Full), with one-tap posting to the main Ledger."
    ),
    AboutWorkflowStep(
        stepNumber = 3,
        icon = Icons.AutoMirrored.Rounded.ReceiptLong,
        titleSw = "Bili: Household Bill dhidi ya Personal Bill",
        titleEn = "Bills: Household vs Personal Comparison",
        bodySw = "Ndani ya bar ya Bili, mfumo unatenganisha kiotomatiki Bili za Nyumbani (Household Bill) na Bili Binafsi (Personal Bill), kila moja ikiwa na orodha yake ya matumizi kwa kila siku (per day). Unaweza kulinganisha mara moja kundi lipi liko juu, kulipa bili kwa mguso mmoja (inaingia kwenye Ledger kiotomatiki), na kupata onyo bili kubwa inapoingizwa.",
        bodyEn = "Inside the Bills bar, Household Bills and Personal Bills are separated into dedicated per-day lists so you can compare which category is higher at a glance, mark bills paid with automatic ledger sync, and receive large-bill warnings."
    ),
    AboutWorkflowStep(
        stepNumber = 4,
        icon = Icons.Rounded.Description,
        titleSw = "Ankara za Wateja (Invoices & PDF)",
        titleEn = "Customer Invoices & PDF Sharing",
        bodySw = "Tengeneza ankara za kitaalamu zenye jina la biashara yako, nembo, bidhaa, punguzo na VAT. Fuatilia wateja waliolipa nusu au waliokamilisha, pakua PDF, na tuma risiti au ankara kwa mteja moja kwa moja kupitia WhatsApp au barua pepe.",
        bodyEn = "Create professional customer invoices with your business details, line items, discounts, and VAT. Track partial or full client payments, export crisp PDFs, and share them directly."
    ),
    AboutWorkflowStep(
        stepNumber = 5,
        icon = Icons.Rounded.Savings,
        titleSw = "Akiba & Malengo (Savings Vault)",
        titleEn = "Savings Goals & Vault",
        bodySw = "Weka malengo ya akiba (kama mfuko wa dharura, kodi au upanuzi wa biashara), fuatilia asilimia ya maendeleo, na weka au toa akiba huku mfumo ukihakikisha kila shilingi inasawazishwa na kitabu cha fedha bila upotevu.",
        bodyEn = "Create savings goals (emergency fund, rent, or expansion), monitor progress percentages, and record deposits or withdrawals synchronized cleanly with your zero-loss cash ledger."
    ),
    AboutWorkflowStep(
        stepNumber = 6,
        icon = Icons.Rounded.NotificationsActive,
        titleSw = "Vikumbusho vya Mfumo wa Android (Auto)",
        titleEn = "Automatic Android System Notifications",
        bodySw = "App inaungana moja kwa moja na mfumo wa taarifa za Android wa simu yako kukukumbusha kiotomatiki kila siku jioni (Saa 1:00 Jioni / 19:00) kuhusu malipo yanayosubiri pamoja na bili ambazo bado hazijalipwa — hata app ikiwa imefungwa au simu ikizimwa na kuwashwa tena.",
        bodyEn = "Integrates directly with Android System Notifications to automatically remind you every evening at 7:00 PM about pending payments and unpaid bills—even when the app is closed or after a device reboot."
    ),
    AboutWorkflowStep(
        stepNumber = 7,
        icon = Icons.Rounded.Security,
        titleSw = "100% Offline, Biometric, Hatimiliki & Ufanisi",
        titleEn = "100% Offline, Biometrics, Copyright & Speed",
        bodySw = "Taarifa zako zote zinahifadhiwa salama kwenye simu yako bila kuhitaji intaneti, zikiwa na ulinzi wa Biometric (Alama ya Kidole / Uso), PIN Lock, Privacy Mask, Anti-Screenshot (FLAG_SECURE), Hatimiliki & Ulinzi wa Nakala, Recovery Vault, nakala ya hifadhi ya simu, na injini ya ufanisi inayorekebisha kasi kulingana na simu yoyote.",
        bodyEn = "All records stay private and 100% offline on your device with AndroidX Biometric App Lock (Fingerprint / Face), 4-Digit PIN Lock, Privacy Mask, FLAG_SECURE anti-screenshot guard, Copyright & Anti-Clone verification, Recovery Vault, phone storage backups, and a Device-Adaptive Performance Engine."
    )
)

/**
 * Matte Android 17 "About App" bottom sheet triggered from the slide drawer below Settings,
 * explaining how Mengidata works from start to finish and presenting Founder contact details
 * with direct WhatsApp and Call actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutAppSheet(
    language: AppLanguage,
    copyrightIntegrity: CopyrightIntegrityReport = CopyrightIntegrityReport(),
    onCopyCopyrightCertificate: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = LocalMengiColors.current
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = OneUiSheetShape,
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 6.dp)
                .testTag("about_app_sheet"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tr(language, "Kuhusu App", "About App"),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("about_app_close_btn")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = tr(language, "Funga", "Close"),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Matte Brand Hero Card
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = colors.heroStart,
                border = BorderStroke(1.dp, if (colors.isDark) Color(0xFF334254) else Color(0xFF3A4E66)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(colors.heroStart, colors.heroMid, colors.heroEnd)
                            )
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (colors.isDark) Color(0xFF141B24) else Color(0xFF1B2634),
                                border = BorderStroke(1.dp, if (colors.isDark) Color(0xFF324357) else Color(0xFF384D66)),
                                modifier = Modifier.size(52.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.mengidata_oneui_icon_1795525888393),
                                    contentDescription = "VaultLedger",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "VaultLedger",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (colors.isDark) Color(0xFFECEFF4) else Color(0xFFF6F5F2)
                                    )
                                    Surface(
                                        color = if (colors.isDark) Color(0xFF2B3A4C) else Color(0xFFECEAE3),
                                        shape = CircleShape
                                    ) {
                                        Text(
                                            text = "v${BuildConfig.VERSION_NAME}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (colors.isDark) Color(0xFFE6ECF2) else Color(0xFF1F2B3A),
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = tr(
                                        language,
                                        "Mfumo Kamili wa Usimamizi wa Fedha za Biashara & Binafsi",
                                        "Complete Business & Personal Finance Management Suite"
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = if (colors.isDark) Color(0xFFB5C3D2) else Color(0xFFCFD9E6)
                                )
                            }
                        }

                        Surface(
                            color = if (colors.isDark) Color(0xFF141B24) else Color(0xFF1B2634),
                            border = BorderStroke(1.dp, if (colors.isDark) Color(0xFF2B394A) else Color(0xFF354961)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.VerifiedUser,
                                    contentDescription = null,
                                    tint = if (colors.isDark) Color(0xFF8AAEE0) else Color(0xFFECEAE3),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = tr(
                                        language,
                                        "Hesabu Salama (Zero-Loss) · Bila Intaneti 100% · Kasi kwa Simu Yoyote",
                                        "Zero-Loss Accounting · 100% Offline · Adaptive Speed for Every Phone"
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (colors.isDark) Color(0xFFECEFF4) else Color(0xFFF6F5F2)
                                )
                            }
                        }
                    }
                }
            }

            // Founder & Direct Support Card (Mr Reg Kimaro + WhatsApp & Call)
            MengiCard(
                modifier = Modifier.testTag("about_founder_card"),
                contentPadding = 16.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tr(language, "FOUNDER BY", "FOUNDER BY"),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = FOUNDER_NAME,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$FOUNDER_PHONE_DISPLAY · WhatsApp / Call",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val waIntent = Intent(Intent.ACTION_VIEW, Uri.parse(FOUNDER_WHATSAPP_URL))
                            runCatching { context.startActivity(waIntent) }
                        },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.income,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("about_founder_whatsapp_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Chat,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "WhatsApp",
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    FilledTonalButton(
                        onClick = {
                            val callIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$FOUNDER_PHONE_E164"))
                            runCatching { context.startActivity(callIntent) }
                        },
                        shape = CircleShape,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("about_founder_call_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Call,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = tr(language, "Piga Simu", "Call"),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Official Copyright & Anti-Cloning Intellectual Property Card
            MengiCard(
                modifier = Modifier.testTag("about_copyright_card"),
                contentPadding = 16.dp
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
                            imageVector = Icons.Rounded.Copyright,
                            contentDescription = null,
                            tint = colors.income,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tr(language, "HATIMILIKI & HAKI MILIKI", "COPYRIGHT & INTELLECTUAL PROPERTY"),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.income
                        )
                        Text(
                            text = "© ${CopyrightVault.COPYRIGHT_YEARS} ${CopyrightVault.APP_BRAND}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${tr(language, "Mmiliki", "Owner")}: ${CopyrightVault.COPYRIGHT_HOLDER}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    StatusPill(
                        text = if (copyrightIntegrity.isAuthentic) {
                            tr(language, "Halisi", "Authentic")
                        } else {
                            tr(language, "Onyo", "Alert")
                        },
                        containerColor = if (copyrightIntegrity.isAuthentic) colors.incomeSoft else colors.expenseSoft,
                        contentColor = if (copyrightIntegrity.isAuthentic) colors.income else colors.expense
                    )
                }

                Spacer(Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.subtleSurface,
                    border = BorderStroke(1.dp, colors.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${tr(language, "Kitambulisho Halisi", "Package ID")}: ${copyrightIntegrity.installedPackageId}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (copyrightIntegrity.ownershipSeal.isNotBlank()) {
                            Text(
                                text = "${tr(language, "Muhuri wa Umiliki", "Ownership Seal")}: ${copyrightIntegrity.ownershipSeal}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "${tr(language, "Cheti cha Saini", "Cert SHA-256")}: ${copyrightIntegrity.signingCertShort}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = colors.cardBorder)
                Spacer(Modifier.height(10.dp))

                Text(
                    text = CopyrightVault.prohibitionStatement(language),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onCopyCopyrightCertificate,
                    shape = CircleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("about_copy_copyright_btn")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = tr(language, "Nakili Hati ya Hatimiliki", "Copy Copyright Certificate"),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Full End-to-End Guide Section
            Text(
                text = tr(
                    language,
                    "Jinsi App Inavyofanya Kazi (Mwanzo Mwisho)",
                    "How VaultLedger Works (Start to Finish)"
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            AboutWorkflowSteps.forEach { step ->
                MengiCard(contentPadding = 14.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = step.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                                ) {
                                    Text(
                                        text = "${step.stepNumber}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = if (language == AppLanguage.EN) step.titleEn else step.titleSw,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = if (language == AppLanguage.EN) step.bodyEn else step.bodySw,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}
