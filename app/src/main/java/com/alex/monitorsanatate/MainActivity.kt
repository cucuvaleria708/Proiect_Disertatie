package com.alex.monitorsanatate

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import com.alex.monitorsanatate.domain.repository.ConnectionRepository
import com.alex.monitorsanatate.navigation.NavGraph
import com.alex.monitorsanatate.ui.theme.MonitorSanatateTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var connectionRepository: ConnectionRepository

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        intent?.data?.let { uri -> viewModel.processDeepLink(uri.toString()) }

        enableEdgeToEdge()
        setContent {
            MonitorSanatateTheme {
                val startDestination by viewModel.startDestination.collectAsState()
                if (startDestination != null) {
                    key(startDestination) {
                        NavGraph(
                            startDestination  = startDestination!!,
                            mainViewModel     = viewModel
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent?.data?.let { uri -> viewModel.processDeepLink(uri.toString()) }
    }

    override fun onDestroy() {
        super.onDestroy()
        connectionRepository.cleanup()
    }
}
