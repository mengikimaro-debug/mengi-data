package tz.mengidata.app

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
    }
}
