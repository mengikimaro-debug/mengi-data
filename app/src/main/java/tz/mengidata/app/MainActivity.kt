package tz.mengidata.app

import android.content.ComponentCallbacks2
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import tz.mengidata.app.ui.MengiApp
import tz.mengidata.app.viewmodel.MengiViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MengiViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MengiApp(viewModel = viewModel)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResumeCheckInstallPermission()
        viewModel.refreshDevicePerformance()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        @Suppress("DEPRECATION")
        if (level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND ||
            level == ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW ||
            level == ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL
        ) {
            viewModel.onSystemMemoryPressure(level)
        }
    }

    override fun onStop() {
        super.onStop()
        viewModel.autoBackupOnBackground()
    }
}
