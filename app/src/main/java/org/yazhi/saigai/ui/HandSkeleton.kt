package org.yazhi.saigai.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.yazhi.saigai.cam.HandObservation

/**
 * MediaPipe hand topology: which of the 21 landmarks connect to which.
 *
 * Index order is MediaPipe's HandLandmark enum — 0 wrist, then four joints per
 * finger starting at the thumb (1-4), index (5-8), middle (9-12), ring (13-16),
 * pinky (17-20). Every chain is rooted at the wrist.
 */
private val BONES: List<Pair<Int, Int>> = listOf(
    0 to 1, 1 to 2, 2 to 3, 3 to 4, // thumb
    0 to 5, 5 to 6, 6 to 7, 7 to 8, // index
    0 to 9, 9 to 10, 10 to 11, 11 to 12, // middle
    0 to 13, 13 to 14, 14 to 15, 15 to 16, // ring
    0 to 17, 17 to 18, 18 to 19, 19 to 20, // pinky
)

/**
 * Draws tracked hand skeletons from normalised MediaPipe landmarks.
 *
 * Why a separate square canvas instead of an overlay on the camera frame: mapping
 * image-space landmarks onto a `PreviewView` requires reproducing CameraX's exact
 * crop/rotation/mirroring transform, which cannot be iterated on without a physical
 * device before the demo. A square canvas maps normalised (0..1) coordinates
 * directly, so it cannot drift out of alignment.
 */
@Composable
fun HandSkeleton(
    hands: List<HandObservation>,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier.aspectRatio(1f),
    ) {
        val unit = size.minDimension
        val dx = (size.width - unit) / 2f
        val dy = (size.height - unit) / 2f

        hands.forEach { hand ->
            val joints = hand.points.map { point ->
                Offset(dx + point.x * unit, dy + point.y * unit)
            }
            BONES.forEach { (from, to) ->
                val a = joints.getOrNull(from)
                val b = joints.getOrNull(to)
                if (a != null && b != null) {
                    drawLine(color = color, start = a, end = b, strokeWidth = 3.dp.toPx())
                }
            }
            joints.forEach { joint ->
                drawCircle(color = color, radius = 4.dp.toPx(), center = joint)
            }
        }
    }
}
