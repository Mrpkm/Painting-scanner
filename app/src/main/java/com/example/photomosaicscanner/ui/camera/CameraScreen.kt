package com.example.photomosaicscanner.ui.camera

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.photomosaicscanner.capture.PhotoCaptureController
import com.example.photomosaicscanner.model.ScanSession
import com.example.photomosaicscanner.model.TileRecord
import com.example.photomosaicscanner.session.SessionViewModel
import com.example.photomosaicscanner.ui.components.GridDiagram
import com.example.photomosaicscanner.ui.components.TileVisualState

@Composable
fun CameraScreen(
    session: ScanSession,
    viewModel: SessionViewModel,
    onOpenOverview: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    val captureController = remember { PhotoCaptureController(context) }
    var cameraReady by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).also { previewView ->
                        captureController.bind(lifecycleOwner, previewView) { success ->
                            cameraReady = success
                            if (!success) statusMessage = "Could not start the camera on this device."
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Camera permission is required to capture tiles.")
                Spacer(Modifier.height(12.dp))
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text("Grant Camera Permission")
                }
            }
        }

        // Grid overlay sized to the painting's aspect ratio, centered over the
        // preview. It is intentionally translucent so it does not hide the
        // painting underneath it.
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.9f)
                .padding(top = 88.dp, bottom = 150.dp)
        ) {
            GridDiagram(
                rows = session.rows,
                columns = session.columns,
                aspectRatio = session.paintingWidth.toFloat() / session.paintingHeight.toFloat(),
                tileState = { tile ->
                    when {
                        session.completedTiles.containsKey(tile.fileKey) -> TileVisualState.COMPLETED
                        tile == session.currentTile -> TileVisualState.CURRENT
                        else -> TileVisualState.PENDING
                    }
                },
                onTileTap = { tile -> viewModel.jumpToTile(tile) }
            )
        }

        // Top status banner: current tile + qualitative movement hint only.
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(12.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 4.dp
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                val current = session.currentTile
                Text(
                    text = if (session.isComplete) {
                        "All tiles captured"
                    } else {
                        "Current tile: ${current?.displayLabel ?: "-"} " +
                            "(${session.currentIndex + 1} of ${session.totalTiles})"
                    },
                    style = MaterialTheme.typography.titleMedium
                )
                if (!session.isComplete) {
                    Text(viewModel.movementHint().message, style = MaterialTheme.typography.bodyMedium)
                }
                statusMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }

        // Bottom controls.
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = {
                    val tile = session.currentTile ?: return@Button
                    val dir = viewModel.sessionDir() ?: return@Button
                    if (cameraReady) {
                        isCapturing = true
                        captureController.capture(tile, dir) { result ->
                            isCapturing = false
                            result.onSuccess { record -> viewModel.recordCapture(tile, record) }
                            result.onFailure { statusMessage = it.message ?: "Capture failed." }
                        }
                    } else {
                        // Camera unavailable on this device/build: still let the
                        // user track progress manually rather than block them.
                        viewModel.recordCapture(
                            tile,
                            TileRecord(
                                row = tile.row,
                                col = tile.col,
                                filename = "(not captured - marked manually)",
                                version = 1,
                                timestampMillis = System.currentTimeMillis()
                            )
                        )
                    }
                },
                enabled = !session.isComplete && !isCapturing,
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(56.dp)
            ) {
                Icon(Icons.Filled.Check, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (isCapturing) "Capturing…" else "CAPTURE / MARK COMPLETE")
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onOpenOverview) { Text("Session Overview") }
        }
    }
}
