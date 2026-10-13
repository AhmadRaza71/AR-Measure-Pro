package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MeasurementType

@Composable
fun ActionControlsRow(
    currentMode: MeasurementType,
    canSave: Boolean,
    placedPointsCount: Int,
    manualLength: Float,
    manualWidth: Float,
    manualHeight: Float,
    onAddPoint: () -> Unit,
    onUndoPoint: () -> Unit,
    onReset: () -> Unit,
    onSave: () -> Unit,
    onRansacScan: () -> Unit,
    onUpdateCuboidDimensions: (Float, Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var showVolumeAdjuster by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Expandable Volume dimension sliders for quick rectangular box fine-tuning
        AnimatedVisibility(visible = currentMode == MeasurementType.VOLUME && showVolumeAdjuster) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xE60B132B))
                    .border(1.dp, Color(0xFF00F0FF), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text("Box Dimensions Fine-Tune", style = MaterialTheme.typography.labelMedium, color = Color(0xFF00F0FF))
                Spacer(modifier = Modifier.height(4.dp))

                // Length slider
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("L: ${String.format("%.2f", manualLength)}m", color = Color.White, fontSize = 12.sp, modifier = Modifier.width(68.dp))
                    Slider(
                        value = manualLength,
                        onValueChange = { onUpdateCuboidDimensions(it, manualWidth, manualHeight) },
                        valueRange = 0.1f..4.0f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF00F0FF), activeTrackColor = Color(0xFF00F0FF)),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Width slider
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("W: ${String.format("%.2f", manualWidth)}m", color = Color.White, fontSize = 12.sp, modifier = Modifier.width(68.dp))
                    Slider(
                        value = manualWidth,
                        onValueChange = { onUpdateCuboidDimensions(manualLength, it, manualHeight) },
                        valueRange = 0.1f..4.0f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFFFFB703), activeTrackColor = Color(0xFFFFB703)),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Height slider
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("H: ${String.format("%.2f", manualHeight)}m", color = Color.White, fontSize = 12.sp, modifier = Modifier.width(68.dp))
                    Slider(
                        value = manualHeight,
                        onValueChange = { onUpdateCuboidDimensions(manualLength, manualWidth, it) },
                        valueRange = 0.1f..3.0f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF00FF9D), activeTrackColor = Color(0xFF00FF9D)),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Circular object: Quick RANSAC scan button
        if (currentMode == MeasurementType.CIRCLE_DIAMETER || currentMode == MeasurementType.CIRCLE_RECESS) {
            Button(
                onClick = onRansacScan,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xCC1C2541),
                    contentColor = Color(0xFF00F0FF)
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .border(1.dp, Color(0xFF00F0FF), RoundedCornerShape(20.dp))
                    .testTag("ransac_circle_scan_button")
            ) {
                Icon(Icons.Default.Radar, contentDescription = "RANSAC Scan", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Instant RANSAC Rim Scan", fontSize = 12.sp)
            }
        }

        // Primary Control Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Reset / Clear button
            IconButton(
                onClick = onReset,
                enabled = placedPointsCount > 0,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color(0xCC1C2541),
                    contentColor = Color(0xFFFF4D4D),
                    disabledContainerColor = Color(0x661C2541),
                    disabledContentColor = Color.Gray
                ),
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .testTag("reset_measurement_button")
            ) {
                Icon(Icons.Default.DeleteSweep, contentDescription = "Reset Points")
            }

            // Undo last point
            IconButton(
                onClick = onUndoPoint,
                enabled = placedPointsCount > 0,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color(0xCC1C2541),
                    contentColor = Color.White,
                    disabledContainerColor = Color(0x661C2541),
                    disabledContentColor = Color.Gray
                ),
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .testTag("undo_point_button")
            ) {
                Icon(Icons.Default.Undo, contentDescription = "Undo Point")
            }

            // Big Center Action Button: "+ Add Pin"
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFF00F0FF), Color(0xFF0090FF))
                        )
                    )
                    .border(2.5.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                FilledIconButton(
                    onClick = onAddPoint,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color(0xFF0B132B)
                    ),
                    modifier = Modifier
                        .size(68.dp)
                        .testTag("add_pin_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Point",
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            // Volume mode: Dimension adjuster toggle
            if (currentMode == MeasurementType.VOLUME) {
                IconButton(
                    onClick = { showVolumeAdjuster = !showVolumeAdjuster },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (showVolumeAdjuster) Color(0xFF00F0FF) else Color(0xCC1C2541),
                        contentColor = if (showVolumeAdjuster) Color(0xFF0B132B) else Color.White
                    ),
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .testTag("toggle_volume_adjuster_button")
                ) {
                    Icon(Icons.Default.Tune, contentDescription = "Adjust Dimensions")
                }
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }

            // Save Measurement to History
            IconButton(
                onClick = onSave,
                enabled = canSave,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (canSave) Color(0xFF00FF9D) else Color(0x661C2541),
                    contentColor = Color(0xFF0B132B),
                    disabledContainerColor = Color(0x661C2541),
                    disabledContentColor = Color.Gray
                ),
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .testTag("save_measurement_button")
            ) {
                Icon(Icons.Default.BookmarkAdd, contentDescription = "Save Measurement")
            }
        }
    }
}
