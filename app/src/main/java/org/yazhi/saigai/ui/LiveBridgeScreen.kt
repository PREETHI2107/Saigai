package org.yazhi.saigai.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import org.yazhi.saigai.R
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
            onRequestPermission = {
                if (hasCameraPermission(context)) {
                    cameraGranted = true
                } else {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }
            },
            onOpenSettings = { context.openAppSettings() },
        )

        SpeakerStatusLine(speakerStatus)

        ConfidenceRow()

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
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
) {
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
                text = stringResource(
                    if (granted) R.string.camera_placeholder_m1 else R.string.camera_rationale,
                ),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
            if (!granted) {
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

@Composable
private fun ConfidenceRow() {
    val label = stringResource(R.string.confidence_label)
    val placeholder = stringResource(R.string.confidence_placeholder)
    val description = "$label: $placeholder"
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(text = placeholder, style = MaterialTheme.typography.bodyMedium)
        }
        LinearProgressIndicator(
            progress = { 0f },
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
