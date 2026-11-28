/*
 * Copyright (c) 2024–2026 Mr Reg Kimaro · VaultLedger™ (tz.mengidata.app).
 * All Rights Reserved / Hatimiliki Zote Zimehifadhiwa.
 *
 * Proprietary software. Unauthorized copying, cloning, or reverse engineering is prohibited.
 */
package tz.mengidata.app

import android.content.ComponentCallbacks2
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import tz.mengidata.app.backup.AndroidSystemBackupScheduler
import tz.mengidata.app.security.UninstallProtectionManager
import tz.mengidata.app.ui.MengiApp
import tz.mengidata.app.viewmodel.MengiViewModel

class MainActivity : FragmentActivity() {
    private val viewModel: MengiViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runCatching { enableEdgeToEdge() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.antiScreenCopyEnabled.collect { secureEnabled ->
                    runCatching {
                        if (secureEnabled) {
                            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                        } else {
                            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                        }
                    }
                }
            }
        }

        setContent {
            MengiApp(viewModel = viewModel)
        }

        // Run background system backup scheduling & device owner check off the main thread
        // so onCreate() never stalls or throws during cold launch.
        lifecycleScope.launch(Dispatchers.IO) {
            runCatching {
                AndroidSystemBackupScheduler.ensurePeriodicBackupScheduled(applicationContext)
            }
            runCatching {
                if (UninstallProtectionManager.isDeviceOwner(applicationContext)) {
                    UninstallProtectionManager.enforceDeviceOwnerUninstallBlock(
                        applicationContext,
                        blocked = true
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        runCatching { viewModel.onAppForegrounded() }
    }

    override fun onResume() {
        super.onResume()
        runCatching { viewModel.onResumeCheckInstallPermission() }
        runCatching { viewModel.refreshDevicePerformance(includeDbProbe = false) }
        runCatching { viewModel.refreshCopyrightIntegrity() }
        runCatching { viewModel.refreshAppBiometricStatus() }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        @Suppress("DEPRECATION")
        if (level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND ||
            level == ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW ||
            level == ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL
        ) {
            runCatching { viewModel.onSystemMemoryPressure(level) }
        }
    }

    override fun onStop() {
        super.onStop()
        runCatching { viewModel.onAppBackgrounded(isChangingConfigurations) }
        runCatching { viewModel.autoBackupOnBackground() }
        runCatching {
            AndroidSystemBackupScheduler.scheduleImmediateBackup(applicationContext, delayMs = 800L)
        }
    }
}
