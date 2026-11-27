package tz.mengidata.app

import android.content.ComponentCallbacks2
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import tz.mengidata.app.backup.AndroidSystemBackupScheduler
import tz.mengidata.app.security.UninstallProtectionManager
import tz.mengidata.app.ui.MengiApp
import tz.mengidata.app.viewmodel.MengiViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MengiViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runCatching { enableEdgeToEdge() }

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

    override fun onResume() {
        super.onResume()
        runCatching { viewModel.onResumeCheckInstallPermission() }
        runCatching { viewModel.refreshDevicePerformance(includeDbProbe = false) }
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
        runCatching { viewModel.autoBackupOnBackground() }
        runCatching {
            AndroidSystemBackupScheduler.scheduleImmediateBackup(applicationContext, delayMs = 800L)
        }
    }
}
