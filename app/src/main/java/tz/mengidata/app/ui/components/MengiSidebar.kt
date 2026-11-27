package tz.mengidata.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CurrencyExchange
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tz.mengidata.app.BuildConfig
import tz.mengidata.app.R
import tz.mengidata.app.data.accounting.ZeroLossLedgerSummary
import tz.mengidata.app.data.db.BusinessSettingsEntity
import tz.mengidata.app.i18n.AppLanguage
import tz.mengidata.app.i18n.FormatUtils
import tz.mengidata.app.i18n.ThemeMode
import tz.mengidata.app.i18n.tr
import tz.mengidata.app.ui.navigation.BillsRoute
import tz.mengidata.app.ui.navigation.DashboardRoute
import tz.mengidata.app.ui.navigation.HubRoute
import tz.mengidata.app.ui.navigation.InvoicesRoute
import tz.mengidata.app.ui.navigation.TransactionsRoute
import tz.mengidata.app.ui.theme.LocalMengiColors
import tz.mengidata.app.ui.theme.LocalMengiThemeState
import tz.mengidata.app.updater.UpdateInfo
import tz.mengidata.app.updater.UpdateUiState

private data class SidebarNavItem(
    val route: Any,
    val labelSw: String,
    val labelEn: String,
    val icon: ImageVector,
    val tag: String
)

private val SidebarPrimaryItems = listOf(
    SidebarNavItem(DashboardRoute, "Dashibodi", "Home", Icons.Rounded.Home, "drawer_dashboard"),
    SidebarNavItem(TransactionsRoute, "Miamala", "Ledger", Icons.Rounded.AccountBalanceWallet, "drawer_transactions"),
    SidebarNavItem(InvoicesRoute, "Ankara", "Invoices", Icons.Rounded.Description, "drawer_invoices"),
    SidebarNavItem(BillsRoute, "Bili", "Bills", Icons.AutoMirrored.Rounded.ReceiptLong, "drawer_bills"),
    SidebarNavItem(HubRoute, "Akiba", "Savings", Icons.Rounded.Savings, "drawer_hub")
)

@Composable
fun MengiSidebarSheet(
    settings: BusinessSettingsEntity,
    zeroLossLedger: ZeroLossLedgerSummary,
    language: AppLanguage,
    updateState: UpdateUiState,
    privacyMaskEnabled: Boolean = false,
    isConvertingCurrency: Boolean = false,
    partyCount: Int = 0,
    selectedRouteMatch: (Any) -> Boolean,
    isUpdateSelected: Boolean = false,
    isSettingsSelected: Boolean,
    onSelectDestination: (Any) -> Unit,
    onSelectCurrency: (String) -> Unit = {},
    onOpenAutoAccounting: () -> Unit = {},
    onOpenPartyDirectory: () -> Unit = {},
    onOpenBusinessProfile: () -> Unit = {},
    onOpenUpdateScreen: () -> Unit = {},
    onOpenSettings: () -> Unit,
    onOpenAboutApp: () -> Unit = {},
    onCloseDrawer: () -> Unit
) {
    val colors = LocalMengiColors.current
    val themeState = LocalMengiThemeState.current
    val currency = FormatUtils.normalizeCurrencyCode(settings.currency)
    val activeCurrencyOption = FormatUtils.findCurrency(currency)
    var currencyListExpanded by remember { mutableStateOf(false) }

    ModalDrawerSheet(
        modifier = Modifier
            .widthIn(max = 316.dp)
            .fillMaxHeight(),
        drawerContainerColor = MaterialTheme.colorScheme.background,
        drawerContentColor = MaterialTheme.colorScheme.onBackground,
        drawerShape = RoundedCornerShape(topEnd = 32.dp, bottomEnd = 32.dp),
        windowInsets = WindowInsets.safeDrawing
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Matte Android 17 Header Card (Zero Green / Zero Glow / Zero Glare)
            Surface(
                shape = RoundedCornerShape(30.dp),
                color = colors.heroStart,
                border = BorderStroke(1.dp, if (colors.isDark) Color(0xFF334254) else Color(0xFF3A4E66)),
                shadowElevation = if (colors.isDark) 0.dp else 2.dp,
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
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (colors.isDark) Color(0xFF141B24) else Color(0xFF1B2634),
                                    border = BorderStroke(1.dp, if (colors.isDark) Color(0xFF324357) else Color(0xFF384D66)),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.mengidata_oneui_icon_1795525888393),
                                        contentDescription = "VaultLedger",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = settings.businessName.ifBlank { "VaultLedger" },
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (colors.isDark) Color(0xFFECEFF4) else Color(0xFFF6F5F2),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val subtitle = settings.tagline.ifBlank {
                                        settings.ownerName.ifBlank {
                                            tr(language, "Mfumo wa Fedha za Biashara", "Business Finance Suite")
                                        }
                                    }
                                    Text(
                                        text = subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = if (colors.isDark) Color(0xFFB5C3D2) else Color(0xFFCFD9E6),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    onClick = {
                                        val next = if (themeState.isDark) ThemeMode.LIGHT else ThemeMode.DARK
                                        themeState.onSetThemeMode(next)
                                    },
                                    shape = CircleShape,
                                    color = if (colors.isDark) Color(0xFF222F3E) else Color(0xFF1B2634),
                                    border = BorderStroke(0.8.dp, if (colors.isDark) Color(0xFF384B61) else Color(0xFF384D66)),
                                    modifier = Modifier
                                        .size(40.dp)
                                        .testTag("sidebar_theme_toggle_btn")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (themeState.isDark) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                                            contentDescription = tr(
                                                language,
                                                if (themeState.isDark) "Badili kwenda Mwanga" else "Badili kwenda Giza",
                                                if (themeState.isDark) "Switch to Light Mode" else "Switch to Dark Mode"
                                            ),
                                            tint = if (colors.isDark) Color(0xFFD4AC82) else Color(0xFFECEAE3),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Surface(
                                    onClick = onCloseDrawer,
                                    shape = CircleShape,
                                    color = if (colors.isDark) Color(0xFF222F3E) else Color(0xFF1B2634),
                                    border = BorderStroke(0.8.dp, if (colors.isDark) Color(0xFF384B61) else Color(0xFF384D66)),
                                    modifier = Modifier
                                        .size(40.dp)
                                        .testTag("close_sidebar_btn")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = tr(language, "Funga menyu", "Close menu"),
                                            tint = if (colors.isDark) Color(0xFFE6ECF2) else Color(0xFFECEAE3),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Matte Balance Pill
                        Surface(
                            color = if (colors.isDark) Color(0xFF141B24) else Color(0xFF1B2634),
                            border = BorderStroke(1.dp, if (colors.isDark) Color(0xFF2B394A) else Color(0xFF354961)),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = tr(language, "SALIO HALISI", "NET BALANCE"),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (colors.isDark) Color(0xFFBCCAD8) else Color(0xFFD2DCE8)
                                    )
                                    Text(
                                        text = if (privacyMaskEnabled) "••••••" else FormatUtils.formatMoney(zeroLossLedger.verifiedCashBalance, currency),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (colors.isDark) Color(0xFFECEFF4) else Color(0xFFF6F5F2),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Surface(
                                    color = if (colors.isDark) Color(0xFF2B3A4C) else Color(0xFFECEAE3),
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = "v${BuildConfig.VERSION_NAME}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (colors.isDark) Color(0xFFE6ECF2) else Color(0xFF1F2B3A),
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            val itemColors = NavigationDrawerItemDefaults.colors(
                selectedContainerColor = if (colors.isDark) Color(0xFF2B425B) else Color(0xFF253447),
                selectedIconColor = if (colors.isDark) Color(0xFFEBF0F7) else Color(0xFFF6F5F2),
                selectedTextColor = if (colors.isDark) Color(0xFFEBF0F7) else Color(0xFFF6F5F2),
                unselectedContainerColor = MaterialTheme.colorScheme.surface,
                unselectedIconColor = if (colors.isDark) Color(0xFFB0BDCB) else Color(0xFF2E3F54),
                unselectedTextColor = if (colors.isDark) Color(0xFFE8ECF2) else Color(0xFF1A222E)
            )

            // One UI 8.5 Grouped Navigation Squircle Card
            MengiCard(contentPadding = 10.dp) {
                Text(
                    text = tr(language, "URAMBAZAJI", "NAVIGATION"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )

                SidebarPrimaryItems.forEach { item ->
                    val selected = selectedRouteMatch(item.route)
                    val showHighBillBadge = item.route == BillsRoute && zeroLossLedger.isTodayBillHigh
                    NavigationDrawerItem(
                        label = {
                            Text(
                                text = if (language == AppLanguage.EN) item.labelEn else item.labelSw,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        selected = selected,
                        onClick = { onSelectDestination(item.route) },
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = if (language == AppLanguage.EN) item.labelEn else item.labelSw
                            )
                        },
                        badge = if (showHighBillBadge) {
                            {
                                Surface(
                                    color = colors.expense,
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = tr(language, "Juu", "High"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        } else null,
                        shape = CircleShape,
                        colors = itemColors,
                        modifier = Modifier
                            .padding(vertical = 2.dp)
                            .testTag(item.tag)
                    )
                }
            }

            // One UI 8.5 Grouped System / Settings Squircle Card
            MengiCard(contentPadding = 10.dp) {
                Text(
                    text = tr(language, "MFUMO", "SYSTEM"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )

                NavigationDrawerItem(
                    label = {
                        Text(
                            text = tr(language, "Sarafu", "Currency"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (currencyListExpanded) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    selected = currencyListExpanded,
                    onClick = { currencyListExpanded = !currencyListExpanded },
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.CurrencyExchange,
                            contentDescription = tr(language, "Sarafu", "Currency")
                        )
                    },
                    badge = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape
                            ) {
                                if (isConvertingCurrency) {
                                    Box(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                } else {
                                    Text(
                                        text = activeCurrencyOption.symbol,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Icon(
                                imageVector = if (currencyListExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    shape = CircleShape,
                    colors = itemColors,
                    modifier = Modifier
                        .padding(vertical = 2.dp)
                        .testTag("drawer_currency_item")
                )

                AnimatedVisibility(
                    visible = currencyListExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(1.dp, colors.cardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("drawer_currency_list")
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            FormatUtils.supportedCurrencies.forEachIndexed { index, option ->
                                val isSelected = option.code.equals(currency, ignoreCase = true)
                                Surface(
                                    onClick = {
                                        if (!isConvertingCurrency) {
                                            onSelectCurrency(option.code)
                                            currencyListExpanded = false
                                        }
                                    },
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        Color.Transparent
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .semantics {
                                            contentDescription = option.symbol
                                        }
                                        .testTag("drawer_currency_${option.code.lowercase()}")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = option.symbol,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                            color = if (isSelected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            },
                                            maxLines = 1
                                        )
                                        if (isSelected) {
                                            Surface(
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Check,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onPrimary,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                }
                                            }
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                            )
                                        }
                                    }
                                }
                                if (index < FormatUtils.supportedCurrencies.lastIndex) {
                                    HorizontalDivider(
                                        color = colors.cardBorder,
                                        thickness = 0.8.dp
                                    )
                                }
                            }
                        }
                    }
                }

                NavigationDrawerItem(
                    label = {
                        Text(
                            text = tr(language, "Uchambuzi wa Kiotomatiki", "Auto-Accounting"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    selected = false,
                    onClick = onOpenAutoAccounting,
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = tr(language, "Uchambuzi wa Kiotomatiki", "Auto-Accounting")
                        )
                    },
                    badge = {
                        Surface(
                            color = if (zeroLossLedger.integrityVerified) colors.incomeSoft else colors.warningSoft,
                            shape = CircleShape
                        ) {
                            Text(
                                text = if (zeroLossLedger.integrityVerified) {
                                    "0%"
                                } else {
                                    tr(language, "Kagua", "Sync")
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (zeroLossLedger.integrityVerified) colors.income else colors.warning,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    },
                    shape = CircleShape,
                    colors = itemColors,
                    modifier = Modifier
                        .padding(vertical = 2.dp)
                        .testTag("drawer_auto_accounting_item")
                )

                NavigationDrawerItem(
                    label = {
                        Text(
                            text = tr(language, "Daftari la Majina", "Names & Contacts"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    selected = false,
                    onClick = onOpenPartyDirectory,
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.Groups,
                            contentDescription = tr(language, "Daftari la Majina", "Names & Contacts")
                        )
                    },
                    badge = if (partyCount > 0) {
                        {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape
                            ) {
                                Text(
                                    text = "$partyCount",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    } else null,
                    shape = CircleShape,
                    colors = itemColors,
                    modifier = Modifier
                        .padding(vertical = 2.dp)
                        .testTag("drawer_crm_item")
                )

                NavigationDrawerItem(
                    label = {
                        Text(
                            text = tr(language, "Taarifa za Biashara & Ankara", "Business & Invoices"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    selected = false,
                    onClick = onOpenBusinessProfile,
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.Business,
                            contentDescription = tr(language, "Taarifa za Biashara & Ankara", "Business & Invoices")
                        )
                    },
                    shape = CircleShape,
                    colors = itemColors,
                    modifier = Modifier
                        .padding(vertical = 2.dp)
                        .testTag("drawer_business_profile_item")
                )

                // Native One UI 8.5 Sidebar Update Item placed directly above Settings -> opens Full-Screen UpdateScreen
                SidebarUpdateNavItem(
                    language = language,
                    updateState = updateState,
                    selected = isUpdateSelected,
                    itemColors = itemColors,
                    onClick = onOpenUpdateScreen
                )

                NavigationDrawerItem(
                    label = {
                        Text(
                            text = tr(language, "Mipangilio", "Settings"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (isSettingsSelected) FontWeight.Bold else FontWeight.SemiBold
                        )
                    },
                    selected = isSettingsSelected,
                    onClick = onOpenSettings,
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = tr(language, "Mipangilio", "Settings")
                        )
                    },
                    shape = CircleShape,
                    colors = itemColors,
                    modifier = Modifier
                        .padding(vertical = 2.dp)
                        .testTag("drawer_settings_item")
                )

                NavigationDrawerItem(
                    label = {
                        Text(
                            text = tr(language, "Kuhusu App", "About App"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    selected = false,
                    onClick = onOpenAboutApp,
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = tr(language, "Kuhusu App", "About App")
                        )
                    },
                    shape = CircleShape,
                    colors = itemColors,
                    modifier = Modifier
                        .padding(vertical = 2.dp)
                        .testTag("drawer_about_app_item")
                )
            }

            Spacer(Modifier.weight(1f, fill = true))

            // Zero-Loss Integrity Footer Pill
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = CircleShape,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.VerifiedUser,
                        contentDescription = null,
                        tint = colors.income,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = tr(
                            language,
                            "Hesabu salama · Bila Intaneti 100%",
                            "Zero-Loss Ledger · 100% Offline"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
