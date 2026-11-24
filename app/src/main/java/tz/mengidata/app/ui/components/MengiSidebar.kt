package tz.mengidata.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tz.mengidata.app.BuildConfig
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
    partyCount: Int = 0,
    selectedRouteMatch: (Any) -> Boolean,
    isSettingsSelected: Boolean,
    onSelectDestination: (Any) -> Unit,
    onOpenPartyDirectory: () -> Unit = {},
    onOpenSettings: () -> Unit,
    onCloseDrawer: () -> Unit
) {
    val colors = LocalMengiColors.current
    val themeState = LocalMengiThemeState.current
    val currency = settings.currency.ifBlank { "TZS" }
    val hasUpdate = updateState is UpdateUiState.Available

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
            // Samsung One UI 8.5 Galaxy Frost Header Squircle
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = colors.heroStart,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
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
                        .drawBehind {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(colors.heroAccentGlow.copy(alpha = 0.28f), Color.Transparent),
                                    center = Offset(size.width * 0.88f, size.height * 0.14f),
                                    radius = size.width * 0.65f
                                ),
                                center = Offset(size.width * 0.88f, size.height * 0.14f),
                                radius = size.width * 0.65f
                            )
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(colors.heroGoldGlow.copy(alpha = 0.16f), Color.Transparent),
                                    center = Offset(size.width * 0.12f, size.height * 0.92f),
                                    radius = size.width * 0.5f
                                ),
                                center = Offset(size.width * 0.12f, size.height * 0.92f),
                                radius = size.width * 0.5f
                            )
                        }
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = settings.businessName.ifBlank { "Mengidata" },
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = Color.White,
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
                                    color = Color.White.copy(alpha = 0.78f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
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
                                    color = Color.White.copy(alpha = 0.14f),
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
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Surface(
                                    onClick = onCloseDrawer,
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.14f),
                                    modifier = Modifier
                                        .size(40.dp)
                                        .testTag("close_sidebar_btn")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = tr(language, "Funga menyu", "Close menu"),
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Frosted Glass Balance Pill
                        Surface(
                            color = Color.White.copy(alpha = 0.13f),
                            border = BorderStroke(0.75.dp, Color.White.copy(alpha = 0.16f)),
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
                                        color = colors.lime
                                    )
                                    Text(
                                        text = if (privacyMaskEnabled) "••••••" else FormatUtils.formatMoney(zeroLossLedger.verifiedCashBalance, currency),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Surface(
                                    color = Color.White.copy(alpha = 0.18f),
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = "v${BuildConfig.VERSION_NAME}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            val itemColors = NavigationDrawerItemDefaults.colors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                unselectedContainerColor = Color.Transparent,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurface
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
                            text = tr(language, "Wateja & Wadau", "Customers & Vendors"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    selected = false,
                    onClick = onOpenPartyDirectory,
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.Groups,
                            contentDescription = tr(language, "Wateja & Wadau", "Customers & Vendors")
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
                    badge = if (hasUpdate) {
                        {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(colors.income)
                            )
                        }
                    } else null,
                    shape = CircleShape,
                    colors = itemColors,
                    modifier = Modifier
                        .padding(vertical = 2.dp)
                        .testTag("drawer_settings_item")
                )
            }

            Spacer(Modifier.weight(1f, fill = true))

            // Zero-Loss Integrity Footer Pill
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = CircleShape,
                border = BorderStroke(0.75.dp, if (colors.isDark) Color.White.copy(alpha = 0.06f) else colors.cardBorder.copy(alpha = 0.5f)),
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
                            "Hesabu salama · Offline 100%",
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
