package org.yazhi.saigai.ui

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.concurrent.Executors

/**
 * What the camera panel is currently doing. Reported so the screen can print the
 * truth instead of showing a blank box when the device has no camera or the
 * provider fails to start.
 */
enum class CameraStatus {
    STARTING,
    ACTIVE,
    UNAVAILABLE,
    FAILED,
}

/**
 * Live camera preview for the Live Bridge screen (design doc B1 §4, milestone M2).
 *
 * Scope: it renders frames and, when an [analyzer] is supplied, feeds frames to it.
 * It does not record, it does not write a file, and it holds no network client — the
 * manifest declares no INTERNET permission, so a frame physically cannot leave the
 * device.
 *
 * [lensFacing] is a [CameraSelector] constant, so changing it rebinds the camera
 * (the camera-flip control from §4).
 */
@Composable
fun CameraPreview(
    lensFacing: Int,
    modifier: Modifier = Modifier,
    analyzer: ImageAnalysis.Analyzer? = null,
    onStatus: (CameraStatus) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // COMPATIBLE keeps PreviewView on a TextureView, which is what would allow a
    // Compose overlay to draw on top of the preview surface.
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FIT_CENTER
        }
    }

    DisposableEffect(lensFacing, lifecycleOwner, analyzer) {
        onStatus(CameraStatus.STARTING)
        val providerFuture = ProcessCameraProvider.getInstance(context)
        val mainExecutor = ContextCompat.getMainExecutor(context)
        val analysisExecutor = Executors.newSingleThreadExecutor()
        var provider: ProcessCameraProvider? = null

        // getInstance() completes asynchronously; cancelling the effect before it
        // finishes must not bind a camera to a dead lifecycle.
        var disposed = false

        providerFuture.addListener(
            {
                if (disposed) return@addListener
                try {
                    val cameraProvider = providerFuture.get()
                    provider = cameraProvider
                    val selector = CameraSelector.Builder()
                        .requireLensFacing(lensFacing)
                        .build()

                    if (!cameraProvider.hasCamera(selector)) {
                        onStatus(CameraStatus.UNAVAILABLE)
                        return@addListener
                    }

                    val preview = Preview.Builder()
                        .build()
                        .also { it.surfaceProvider = previewView.surfaceProvider }

                    val useCases = mutableListOf<UseCase>(preview)
                    if (analyzer != null) {
                        val analysis = ImageAnalysis.Builder()
                            // Drop stale frames instead of queuing: inference must see
                            // the current hand position, not a backlog of old ones.
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                            .build()
                        analysis.setAnalyzer(analysisExecutor, analyzer)
                        useCases += analysis
                    }

                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        selector,
                        *useCases.toTypedArray(),
                    )
                    onStatus(CameraStatus.ACTIVE)
                } catch (error: Exception) {
                    onStatus(CameraStatus.FAILED)
                }
            },
            mainExecutor,
        )

        onDispose {
            disposed = true
            provider?.unbindAll()
            analysisExecutor.shutdown()
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier,
    )
}
