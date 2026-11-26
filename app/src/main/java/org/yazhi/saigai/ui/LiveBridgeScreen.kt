package org.yazhi.saigai.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlin.math.roundToInt
import org.yazhi.saigai.R
import org.yazhi.saigai.cam.HandTracker
import org.yazhi.saigai.cam.TrackerStatus
import org.yazhi.saigai.model.LanguageMode
import org.yazhi.saigai.model.Phrase
import org.yazhi.saigai.tts.SpeakerStatus

/**
 * Live Bridge screen (design doc §4).
 *
 * M1 scope: camera permission flow, caption surface, replay, and the mic affordance
 * as a disabled placeholder. CameraX preview and MediaPipe inference are M2, STT is M3 —
 * the placeholders name the milestone so the screen never implies capability it lacks.
 */
@Composable
fun LiveBridgeScreen(
    caption: Phrase?,
    languageMode: LanguageMode,
    speakerStatus: SpeakerStatus,
    onReplay: () -> Unit,
) {
    val context = LocalContext.current
    var cameraGranted by remember { mutableStateOf(hasCameraPermission(context)) }
    var askedOnce by rememberSaveable { mutableStateOf(false) }
    var lensFacing by rememberSaveable { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var cameraStatus by remember { mutableStateOf(CameraStatus.STARTING) }

    val handTracker = remember { HandTracker(context) }
    // Only run inference while the camera is actually granted and bound: starting
    // MediaPipe with no frame source would show "no hand in frame" for a camera
    // that is simply switched off.
    DisposableEffect(handTracker, cameraGranted) {
        if (cameraGranted) handTracker.start() else handTracker.stop()
        onDispose { handTracker.stop() }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        cameraGranted = granted
        askedOnce = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CameraPanel(
            granted = cameraGranted,
            askedOnce = askedOnce,
            lensFacing = lensFacing,
            cameraStatus = cameraStatus,
            analyzer = handTracker,
            onFlip = {
                lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                    CameraSelector.LENS_FACING_FRONT
                } else {
                    CameraSelector.LENS_FACING_BACK
                }
            },
            onStatus = { cameraStatus = it },
            onRequestPermission = {
                if (hasCameraPermission(context)) {
                    cameraGranted = true
                } else {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }
            },
            onOpenSettings = { context.openAppSettings() },
        )

        HandTrackingPanel(tracker = handTracker, cameraGranted = cameraGranted)

        SpeakerStatusLine(speakerStatus)

        ConfidenceRow(confidence = handTracker.hands.maxOfOrNull { it.score })

        CaptionSurface(
            caption = caption,
            languageMode = languageMode,
        )

        OutputActions(
            canReplay = caption != null,
            onReplay = onReplay,
        )
    }
}

@Composable
private fun CameraPanel(
    granted: Boolean,
    askedOnce: Boolean,
    lensFacing: Int,
    cameraStatus: CameraStatus,
    analyzer: ImageAnalysis.Analyzer?,
    onFlip: () -> Unit,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onStatus: (CameraStatus) -> Unit,
) {
    if (granted) {
        LivePreviewPanel(
            lensFacing = lensFacing,
            cameraStatus = cameraStatus,
            analyzer = analyzer,
            onFlip = onFlip,
            onStatus = onStatus,
        )
        return
    }

    val shape = RoundedCornerShape(16.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(shape)
            .border(2.dp, MaterialTheme.colorScheme.outline, shape),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.camera_title),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = stringResource(R.string.camera_rationale),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
            if (askedOnce) {
                Text(
                    text = stringResource(R.string.camera_denied),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            Button(
                onClick = onRequestPermission,
                modifier = Modifier.padding(top = 12.dp),
            ) {
                Text(stringResource(R.string.camera_allow))
            }
            if (askedOnce) {
                TextButton(onClick = onOpenSettings) {
                    Text(stringResource(R.string.camera_open_settings))
                }
            }
        }
    }
}

/**
 * Granted state: the real CameraX preview plus the camera-flip control from design
 * doc §4. The line under the frame reports STARTING / ACTIVE / UNAVAILABLE / FAILED,
 * so a device with no usable camera says so instead of showing a blank rectangle.
 */
@Composable
private fun LivePreviewPanel(
    lensFacing: Int,
    cameraStatus: CameraStatus,
    analyzer: ImageAnalysis.Analyzer?,
    onFlip: () -> Unit,
    onStatus: (CameraStatus) -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(shape)
                .border(2.dp, MaterialTheme.colorScheme.outline, shape),
        ) {
            CameraPreview(
                lensFacing = lensFacing,
                modifier = Modifier.fillMaxSize(),
                analyzer = analyzer,
                onStatus = onStatus,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(
                    when (cameraStatus) {
                        CameraStatus.STARTING -> R.string.camera_starting
                        CameraStatus.ACTIVE -> R.string.camera_active
                        CameraStatus.UNAVAILABLE -> R.string.camera_unavailable
                        CameraStatus.FAILED -> R.string.camera_failed
                    },
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = if (cameraStatus == CameraStatus.ACTIVE) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.error
                },
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onFlip) {
                Text(stringResource(R.string.camera_flip))
            }
        }
    }
}

/**
 * Design doc §10 open question: budget Android devices often ship without a Tamil
 * offline voice. Say so on screen instead of failing silently into English-only audio.
 */
@Composable
private fun SpeakerStatusLine(status: SpeakerStatus) {
    val (messageRes, isProblem) = when (status) {
        SpeakerStatus.READY -> R.string.tts_ready to false
        SpeakerStatus.INITIALISING -> R.string.tts_initialising to false
        SpeakerStatus.TAMIL_VOICE_MISSING -> R.string.tts_tamil_missing to true
        SpeakerStatus.UNAVAILABLE -> R.string.tts_unavailable to true
    }
    Text(
        text = stringResource(messageRes),
        style = MaterialTheme.typography.bodyMedium,
        color = if (isProblem) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * Hand-landmark stage of design doc §4 (M2). It reports exactly what the model
 * produced — how many hands, where the 21 landmarks are, and the presence score —
 * and then states plainly that phrase classification is not trained yet.
 */
@Composable
private fun HandTrackingPanel(
    tracker: HandTracker,
    cameraGranted: Boolean,
) {
    val shape = RoundedCornerShape(16.dp)
    val count = tracker.hands.size
    val isProblem = tracker.status == TrackerStatus.MODEL_MISSING ||
        tracker.status == TrackerStatus.FAILED
    val statusText = when {
        !cameraGranted -> stringResource(R.string.hand_idle)
        tracker.status == TrackerStatus.MODEL_MISSING -> stringResource(R.string.hand_model_missing)
        tracker.status == TrackerStatus.FAILED -> stringResource(R.string.hand_failed)
        tracker.status == TrackerStatus.IDLE -> stringResource(R.string.hand_idle)
        count == 0 -> stringResource(R.string.hand_none)
        else -> stringResource(R.string.hand_detected, count)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(2.dp, MaterialTheme.colorScheme.outline, shape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.hand_tracking_title),
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = statusText,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isProblem) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        // Surface the real reason a failed init failed. Without this the screen only
        // says "unavailable" and the cause has to be dug out of logcat.
        val failureDetail = tracker.error
        if (isProblem && failureDetail != null) {
            Text(
                text = failureDetail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (count > 0) {
            HandSkeleton(
                hands = tracker.hands,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(0.6f),
            )
        }
        Text(
            text = stringResource(R.string.hand_classifier_pending),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Design doc §4's confidence meter, now fed by the real hand-presence score instead
 * of a hardcoded 0f. A null score means nothing is being tracked, which is shown as
 * "no hand" rather than a misleading 0%.
 */
@Composable
private fun ConfidenceRow(confidence: Float?) {
    val label = stringResource(R.string.confidence_label)
    val valueText = if (confidence == null) {
        stringResource(R.string.confidence_no_hand)
    } else {
        // Built in code, not in strings.xml: a "%1$d%%" resource carries no Tamil
        // and would be a meaningless entry in values-ta.
        "${(confidence * 100).roundToInt()}%"
    }
    val description = "$label: $valueText"
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(text = valueText, style = MaterialTheme.typography.bodyMedium)
        }
        LinearProgressIndicator(
            progress = { confidence ?: 0f },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .semantics { contentDescription = description },
        )
    }
}

@Composable
private fun CaptionSurface(
    caption: Phrase?,
    languageMode: LanguageMode,
) {
    val shape = RoundedCornerShape(16.dp)
    val emptyText = stringResource(R.string.captions_placeholder)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(2.dp, MaterialTheme.colorScheme.outline, shape),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (caption == null) {
                Text(
                    text = emptyText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                if (languageMode.showsTamil()) {
                    Text(
                        text = caption.ta,
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                if (languageMode.showsEnglish()) {
                    Text(
                        text = caption.en,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        }
    }
}

@Composable
private fun OutputActions(
    canReplay: Boolean,
    onReplay: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedButton(
            onClick = onReplay,
            enabled = canReplay,
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = stringResource(R.string.replay),
                style = MaterialTheme.typography.labelLarge,
            )
        }
        Button(
            onClick = {},
            enabled = false,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        ) {
            Text(
                text = stringResource(R.string.hold_to_speak),
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
            )
        }
    }
    Text(
        text = stringResource(R.string.mic_disabled_note),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 4.dp),
        textAlign = TextAlign.Center,
    )
}

private fun hasCameraPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED

private fun Context.openAppSettings() {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(intent)
}
