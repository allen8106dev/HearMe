package com.allen.hearme

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.allen.hearme.navigation.AppNavHost
import com.allen.hearme.ui.HearMeViewModel
import com.allen.hearme.ui.theme.HearMeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: HearMeViewModel by viewModels()

    /** Launcher: requests RECORD_AUDIO; on grant, starts the service. */
    private val requestMicPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.toggleSmartListening(true)
        }
        // If denied, the ViewModel/Service will surface MICROPHONE_UNAVAILABLE — no crash.
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HearMeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavHost(
                        viewModel = viewModel,
                        onRequestMicPermission = {
                            requestMicPermission.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    )
                }
            }
        }
    }
}
