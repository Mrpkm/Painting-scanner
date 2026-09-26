package com.example.photomosaicscanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.photomosaicscanner.session.SessionViewModel
import com.example.photomosaicscanner.ui.camera.CameraScreen
import com.example.photomosaicscanner.ui.overview.OverviewScreen
import com.example.photomosaicscanner.ui.setup.SetupScreen
import com.example.photomosaicscanner.ui.theme.PhotoMosaicScannerTheme

/**
 * Deliberately simple, non-library navigation: three screens, driven by
 * explicit callbacks rather than a nav graph. There is no persisted "which
 * screen was open" state -- session persistence (dimensions, grid, overlap,
 * completed tiles, current tile) lives in SessionViewModel/SessionRepository
 * instead, so re-opening the app always lands on Overview for a resumable
 * session, or Setup for a fresh one.
 */
private enum class Screen { CAMERA, OVERVIEW }

class MainActivity : ComponentActivity() {

    private val viewModel: SessionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PhotoMosaicScannerTheme {
                AppRoot(viewModel)
            }
        }
    }
}

@Composable
private fun AppRoot(viewModel: SessionViewModel) {
    val session by viewModel.session.collectAsState()
    var screen by remember { mutableStateOf(Screen.OVERVIEW) }

    val currentSession = session
    if (currentSession == null) {
        SetupScreen { width, height, units, overlap, rows, columns ->
            viewModel.startNewSession(width, height, units, overlap, rows, columns)
            screen = Screen.CAMERA
        }
    } else {
        when (screen) {
            Screen.CAMERA -> CameraScreen(
                session = currentSession,
                viewModel = viewModel,
                onOpenOverview = { screen = Screen.OVERVIEW }
            )
            Screen.OVERVIEW -> OverviewScreen(
                session = currentSession,
                viewModel = viewModel,
                onContinue = { screen = Screen.CAMERA },
                onFinish = { viewModel.finishSession() },
                onStartOver = { viewModel.discardAndStartOver() }
            )
        }
    }
}
