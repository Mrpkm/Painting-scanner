package com.example.photomosaicscanner.capture

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.photomosaicscanner.model.TileCoordinate
import com.example.photomosaicscanner.model.TileRecord
import java.io.File

/**
 * Thin wrapper around CameraX. Stage 1's job is limited to showing a live
 * preview and saving one full-resolution JPEG per tile with a deterministic
 * name -- no frame analysis, no feature matching, no auto-capture. Stage 2
 * can add an ImageAnalysis use case alongside [ImageCapture] here without
 * touching this capture path.
 *
 * Note on "108 MP mode": this requests the highest resolution CameraX
 * itself can expose via ResolutionSelector. A vendor-specific ultra-high-
 * resolution sensor mode is not guaranteed to be reachable through CameraX's
 * standard API on every device, and this app does not assume it is.
 *
 * Note on RAW/DNG: not implemented. Stable CameraX does not expose RAW
 * capture through the standard ImageCapture API; supporting it would
 * require Camera2Interop and device-by-device verification, which is out
 * of scope for Stage 1 per the brief ("do not assume RAW support exists").
 */
class PhotoCaptureController(private val context: Context) {

    private var imageCapture: ImageCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null

    fun bind(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        onBound: (Boolean) -> Unit
    ) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            try {
                val provider = providerFuture.get()
                cameraProvider = provider

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val resolutionSelector = ResolutionSelector.Builder()
                    .setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
                    .build()

                val capture = ImageCapture.Builder()
                    .setResolutionSelector(resolutionSelector)
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .build()

                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    capture
                )
                imageCapture = capture
                onBound(true)
            } catch (e: Exception) {
                Log.e(TAG, "Camera bind failed", e)
                onBound(false)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    /** Captures the given tile at full resolution into [sessionDir]. */
    fun capture(
        tile: TileCoordinate,
        sessionDir: File,
        onResult: (Result<TileRecord>) -> Unit
    ) {
        val capture = imageCapture
        if (capture == null) {
            onResult(Result.failure(IllegalStateException("Camera not ready")))
            return
        }
        if (!sessionDir.exists()) sessionDir.mkdirs()
        val (file, version) = CaptureFileNaming.nextAvailableFile(sessionDir, tile)
        val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()

        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    onResult(
                        Result.success(
                            TileRecord(
                                row = tile.row,
                                col = tile.col,
                                filename = file.name,
                                version = version,
                                timestampMillis = System.currentTimeMillis()
                            )
                        )
                    )
                }

                override fun onError(exception: ImageCaptureException) {
                    onResult(Result.failure(exception))
                }
            }
        )
    }

    fun unbind() {
        cameraProvider?.unbindAll()
    }

    companion object {
        private const val TAG = "PhotoCaptureController"
    }
}
