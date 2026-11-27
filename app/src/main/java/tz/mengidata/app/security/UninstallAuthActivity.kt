package tz.mengidata.app.security

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tz.mengidata.app.data.prefs.AppPreferences
import tz.mengidata.app.i18n.AppLanguage
import tz.mengidata.app.i18n.tr
import tz.mengidata.app.ui.theme.LocalMengiColors
import tz.mengidata.app.ui.theme.MengidataTheme

/**
 * Full-screen Android System Fingerprint gate launched immediately by
 * VaultUninstallAccessibilityService whenever anyone attempts to uninstall
 * or disable protection for VaultLedger.
 *
 * - Fingerprint verified -> opens a 45s owner grace window, releases Device Admin/Owner lock
 *   if triggered by uninstall, and lets Android proceed with the authorized action.
 * - Fingerprint cancelled/failed -> sends the user back to the phone Home screen.
 */
class UninstallAuthActivity : ComponentActivity() {

    private var statusMessage by mutableStateOf<String?>(null)
    private var promptLaunched = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        UninstallGuardSession.setAuthActivityVisible(true)

        val prefs = AppPreferences(this)
        val language = prefs.language.value
        val themeMode = prefs.themeMode.value
        val dynamicColor = prefs.dynamicColorEnabled.value
        val highContrast = prefs.highContrastEnabled.value
        val triggerMode = intent?.getStringExtra(EXTRA_TRIGGER_MODE) ?: TRIGGER_UNINSTALL

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                abortToHomeScreen()
            }
        })

        setContent {
            MengidataTheme(
                themeMode = themeMode,
                dynamicColorEnabled = dynamicColor,
                highContrastEnabled = highContrast
            ) {
                val colors = LocalMengiColors.current

                LaunchedEffect(Unit) {
                    if (!promptLaunched) {
                        promptLaunched = true
                        delay(160L)
                        triggerAndroidSystemFingerprint(language, triggerMode)
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(28.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, colors.cardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 360.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(68.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.Fingerprint,
                                            contentDescription = tr(language, "Alama ya kidole", "Fingerprint"),
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                }

                                Surface(
                                    shape = CircleShape,
                                    color = colors.incomeSoft
                                ) {
                                    Box(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = tr(language, "ULINZI WA ANDROID SYSTEM", "ANDROID SYSTEM GUARD"),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.income
                                        )
                                    }
                                }

                                Text(
                                    text = if (triggerMode == TRIGGER_UNINSTALL) {
                                        tr(language, "Thibitisha Alama ya Kidole Ku-Uninstall", "Verify Fingerprint to Uninstall")
                                    } else {
                                        tr(language, "Ulinzi wa VaultLedger Umelindwa", "VaultLedger Protection Locked")
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )

                                Text(
                                    text = statusMessage ?: tr(
                                        language,
                                        "Weka alama yako ya kidole (Fingerprint) ili kuruhusu mfumo wa Android kuendelea.",
                                        "Scan your Fingerprint so Android System can allow this protected action."
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(Modifier.height(4.dp))

                                Button(
                                    onClick = { triggerAndroidSystemFingerprint(language, triggerMode) },
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("uninstall_auth_scan_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Fingerprint,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.size(8.dp))
                                    Text(tr(language, "Weka Kidole Sasa", "Scan Fingerprint"))
                                }

                                OutlinedButton(
                                    onClick = { abortToHomeScreen() },
                                    shape = CircleShape,
                                    border = BorderStroke(1.dp, colors.cardBorder),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("uninstall_auth_cancel_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Shield,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.size(6.dp))
                                    Text(tr(language, "Ghairi & Linda App", "Cancel & Keep Protected"))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun triggerAndroidSystemFingerprint(language: AppLanguage, triggerMode: String) {
        UninstallProtectionManager.authenticateFingerprint(
            context = this,
            title = tr(language, "Ulinzi wa VaultLedger", "VaultLedger Uninstall Guard"),
            subtitle = tr(language, "Thibitisha Alama ya Kidole (Android System)", "Verify Fingerprint (Android System)"),
            description = if (triggerMode == TRIGGER_UNINSTALL) {
                tr(
                    language,
                    "Weka kidole chako ili kuruhusu kuondoa (uninstall) VaultLedger.",
                    "Scan your Fingerprint to authorize uninstalling VaultLedger."
                )
            } else {
                tr(
                    language,
                    "Weka kidole chako ili kufungua mipangilio ya ulinzi wa VaultLedger.",
                    "Scan your Fingerprint to modify VaultLedger protection settings."
                )
            },
            cancelLabel = tr(language, "Ghairi", "Cancel"),
            allowCredentialFallbackIfNoBiometric = true,
            onSuccess = {
                // Grant a 45-second verified owner window so Android System proceeds without re-blocking
                UninstallGuardSession.grantTemporaryUnlock(45_000L)
                if (triggerMode == TRIGGER_UNINSTALL) {
                    lifecycleScope.launch {
                        UninstallProtectionManager.releaseProtectionAfterFingerprint(
                            context = this@UninstallAuthActivity,
                            clearDeviceOwnerForUninstall = true
                        )
                        repeat(10) {
                            if (!UninstallProtectionManager.isDeviceAdminActive(this@UninstallAuthActivity) &&
                                !UninstallProtectionManager.isDeviceOwnerUninstallBlocked(this@UninstallAuthActivity)
                            ) {
                                return@repeat
                            }
                            delay(120L)
                        }
                        runCatching {
                            startActivity(UninstallProtectionManager.buildUninstallSelfIntent(this@UninstallAuthActivity))
                        }
                        finish()
                    }
                } else {
                    finish()
                }
            },
            onFailure = { reason ->
                if (reason == "CANCELLED") {
                    abortToHomeScreen()
                } else {
                    statusMessage = tr(
                        language,
                        "Alama ya kidole haikuthibitishwa ($reason). Bonyeza 'Weka Kidole Sasa' kujaribu tena.",
                        "Fingerprint not verified ($reason). Tap 'Scan Fingerprint' to retry."
                    )
                }
            }
        )
    }

    private fun abortToHomeScreen() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        runCatching { startActivity(homeIntent) }
        finish()
    }

    override fun onDestroy() {
        UninstallGuardSession.setAuthActivityVisible(false)
        super.onDestroy()
    }

    companion object {
        const val EXTRA_TRIGGER_MODE = "extra_trigger_mode"
        const val TRIGGER_UNINSTALL = "uninstall"
        const val TRIGGER_SETTINGS_GUARD = "settings_guard"
    }
}
