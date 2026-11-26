package org.yazhi.saigai.cam

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import kotlin.math.max

/**
 * One detected hand, in normalised image coordinates (0f..1f).
 *
 * [points] always has 21 entries in MediaPipe's fixed order (wrist, thumb chain,
 * index chain, middle chain, ring chain, pinky chain) — see [HandSkeleton] for the
 * drawing order. [score] is the hand-presence/handedness confidence, not a phrase
 * confidence.
 */
data class HandObservation(
    val points: List<PointF>,
    val handedness: String,
    val score: Float,
)

enum class TrackerStatus {
    IDLE,

    /** Model is loaded and frames are being analysed. */
    RUNNING,

    /** `assets/hand_landmarker.task` is absent — see scripts/fetch_hand_model.sh. */
    MODEL_MISSING,

    /** MediaPipe refused to start; [HandTracker.error] says why. */
    FAILED,
}

/**
 * On-device hand landmark extraction — the input stage of design doc B1 §4 (M2).
 *
 * This class does exactly what §4 specifies for the *first* stage: CameraX frame →
 * MediaPipe Hand Landmarker → 21 (x, y, z) points per hand. MediaPipe runs on the
 * CPU, from a bundled asset, with no network access (the app has no INTERNET
 * permission at all).
 *
 * What it deliberately does NOT do: map landmarks to ISL phrases. The classifier
 * that would do that (a quantised TFLite LSTM/1D-CNN) must be trained on the M0
 * dataset, which has not been collected or ISL-reviewed. Emitting invented phrase
 * labels here would be fabricated recognition, so the class stops at landmarks and
 * the UI says so.
 */
class HandTracker(private val context: Context) : ImageAnalysis.Analyzer {

    var status by mutableStateOf(TrackerStatus.IDLE)
        private set

    var hands by mutableStateOf<List<HandObservation>>(emptyList())
        private set

    var error by mutableStateOf<String?>(null)
        private set

    private var landmarker: HandLandmarker? = null
    private var submitted = false

    fun start() {
        if (landmarker != null) return
        if (!hasModel()) {
            status = TrackerStatus.MODEL_MISSING
            return
        }
        try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath(MODEL_ASSET)
                .setDelegate(Delegate.CPU)
                .build()
            val options = HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setNumHands(2)
                .setMinHandDetectionConfidence(MIN_CONFIDENCE)
                .setMinHandPresenceConfidence(MIN_CONFIDENCE)
                .setMinTrackingConfidence(MIN_CONFIDENCE)
                .setResultListener { result, _ ->
                    submitted = false
                    hands = result.landmarks().mapIndexed { index, landmarks ->
                        val category = result.handedness()
                            .getOrNull(index)
                            ?.firstOrNull()
                        HandObservation(
                            points = landmarks.map { PointF(it.x(), it.y()) },
                            handedness = category?.categoryName().orEmpty(),
                            score = category?.score() ?: 0f,
                        )
                    }
                }
                .setErrorListener { failure ->
                    submitted = false
                    error = failure.message
                    status = TrackerStatus.FAILED
                }
                .build()
            landmarker = HandLandmarker.createFromOptions(context, options)
            error = null
            status = TrackerStatus.RUNNING
        } catch (failure: Throwable) {
            // Throwable, not Exception: a missing JNI library arrives as
            // UnsatisfiedLinkError, which is an Error. Catching only Exception let
            // the app die on an ABI without a MediaPipe .so — the camera screen must
            // degrade to a status line instead of taking the process down.
            error = failure.message ?: failure::class.java.simpleName
            status = TrackerStatus.FAILED
            landmarker = null
        }
    }

    fun stop() {
        landmarker?.close()
        landmarker = null
        submitted = false
        hands = emptyList()
        status = TrackerStatus.IDLE
    }

    /**
     * Called by CameraX on a background thread. The proxy is always closed: a leaked
     * ImageProxy stalls the whole pipeline after a handful of frames.
     */
    override fun analyze(image: ImageProxy) {
        val active = landmarker
        if (active == null || status != TrackerStatus.RUNNING || submitted) {
            image.close()
            return
        }
        try {
            val bitmap = image.toRgbaBitmap()
            submitted = true
            active.detectAsync(
                BitmapImageBuilder(bitmap).build(),
                ImageProcessingOptions.builder()
                    .setRotationDegrees(image.imageInfo.rotationDegrees)
                    .build(),
                SystemTime.processingTimestamp(),
            )
        } catch (failure: Throwable) {
            // A single bad frame must not kill the tracker; the next frame retries.
            submitted = false
            error = failure.message
        } finally {
            image.close()
        }
    }

    private fun hasModel(): Boolean = try {
        context.assets.open(MODEL_ASSET).close()
        true
    } catch (missing: Exception) {
        false
    }

    /**
     * RGBA_8888 planes carry one padded row per scanline, so the raw buffer is wider
     * than [ImageProxy.getWidth]. Copy, then crop the padding away — MediaPipe needs
     * the true aspect ratio or the normalised landmarks come out stretched.
     */
    private fun ImageProxy.toRgbaBitmap(): Bitmap {
        val plane = planes[0]
        val rowPadding = plane.rowStride - plane.pixelStride * width
        val padded = Bitmap.createBitmap(
            width + rowPadding / plane.pixelStride,
            height,
            Bitmap.Config.ARGB_8888,
        )
        padded.copyPixelsFromBuffer(plane.buffer)
        if (padded.width == width) return padded
        val cropped = Bitmap.createBitmap(padded, 0, 0, width, height)
        padded.recycle()
        return cropped
    }

    private companion object {
        const val MODEL_ASSET = "hand_landmarker.task"

        /**
         * Design doc §4 uses 0.80 for the *phrase* threshold. Hand detection is a
         * different (earlier) gate, so it stays permissive at 0.5 to keep the
         * skeleton stable; the phrase threshold belongs to the missing classifier.
         */
        const val MIN_CONFIDENCE = 0.5f
    }
}

/**
 * MediaPipe requires strictly increasing timestamps in LIVE_STREAM mode and throws
 * if two frames share one. Uptime millis can collide on fast devices, so this
 * guarantees monotonicity.
 */
private object SystemTime {
    private var last = 0L

    fun processingTimestamp(): Long {
        val now = max(android.os.SystemClock.uptimeMillis(), last + 1)
        last = now
        return now
    }
}
