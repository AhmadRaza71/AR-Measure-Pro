package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ar.SpatialState
import com.example.data.MeasurementType
import com.example.engine.CircleFitResult
import com.example.engine.CuboidResult
import com.example.model.UnitFormatter
import com.example.model.UnitSystem

@Composable
fun MeasurementHUDCard(
    currentMode: MeasurementType,
    unitSystem: UnitSystem,
    instructionText: String,
    spatialState: SpatialState,
    calculatedDistance: Float,
    calculatedAngle: Float,
    circleResult: CircleFitResult?,
    recessDepth: Float?,
    calculatedArea: Float,
    calculatedPerimeter: Float,
    cuboidResult: CuboidResult?,
    onToggleUnit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xD90B132B))
            .border(1.5.dp, Color(0x9900F0FF), RoundedCornerShape(16.dp))
            .padding(14.dp)
            .testTag("measurement_hud_card")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row: Mode Name & Unit System Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF00FF9D))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = currentMode.displayName.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF00F0FF),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                // Unit System Toggle Chip
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1C2541))
                        .clickable { onToggleUnit() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("unit_toggle_button"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.SyncAlt,
                        contentDescription = "Toggle Unit",
                        tint = Color(0xFFFFB703),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (unitSystem == UnitSystem.METRIC) "METRIC" else "IMPERIAL",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Primary Readout Display
            val primaryText = when (currentMode) {
                MeasurementType.DISTANCE, MeasurementType.CHAIN ->
                    UnitFormatter.formatDistance(calculatedDistance, unitSystem)
                MeasurementType.ANGLE ->
                    UnitFormatter.formatAngle(calculatedAngle)
                MeasurementType.CIRCLE_DIAMETER ->
                    circleResult?.let { "Ø " + UnitFormatter.formatDistance(it.diameter, unitSystem) } ?: "0.00 m"
                MeasurementType.CIRCLE_RECESS ->
                    circleResult?.let {
                        val diam = "Ø " + UnitFormatter.formatDistance(it.diameter, unitSystem)
                        val depth = recessDepth?.let { d -> " | ↓ " + UnitFormatter.formatDistance(d, unitSystem) } ?: ""
                        "$diam$depth"
                    } ?: "0.00 m"
                MeasurementType.AREA ->
                    UnitFormatter.formatArea(calculatedArea, unitSystem)
                MeasurementType.VOLUME ->
                    cuboidResult?.let { UnitFormatter.formatVolume(it.volume, unitSystem) } ?: "0.000 m³"
                MeasurementType.SPIRIT_LEVEL ->
                    String.format("%.1f° Incline", spatialState.surfaceInclinationDeg)
            }

            Text(
                text = primaryText,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 28.sp
                ),
                color = Color.White
            )

            // Secondary Details Badge
            val secondaryText = when (currentMode) {
                MeasurementType.DISTANCE -> {
                    val map = UnitFormatter.formatDetailedDistance(calculatedDistance)
                    "${map["cm"]} • ${map["in"]} • ${map["ft"]}"
                }
                MeasurementType.CHAIN -> "Total Chain Length across placed segments"
                MeasurementType.ANGLE -> "Vertex V pivot | Dot product vector math"
                MeasurementType.CIRCLE_DIAMETER, MeasurementType.CIRCLE_RECESS -> {
                    circleResult?.let {
                        "Radius: ${UnitFormatter.formatDistance(it.radius, unitSystem)} | Inliers: ${it.inlierCount}/${it.totalPoints} (RANSAC)"
                    } ?: "Aim at circular rim to calculate diameter"
                }
                MeasurementType.AREA -> {
                    "Perimeter: " + UnitFormatter.formatDistance(calculatedPerimeter, unitSystem) + " (Shoelace 3D)"
                }
                MeasurementType.VOLUME -> {
                    cuboidResult?.let {
                        val l = UnitFormatter.formatDistance(it.length, unitSystem)
                        val w = UnitFormatter.formatDistance(it.width, unitSystem)
                        val h = UnitFormatter.formatDistance(it.height, unitSystem)
                        "L: $l  ×  W: $w  ×  H: $h"
                    } ?: "4-Point cuboid or dimension sliders"
                }
                MeasurementType.SPIRIT_LEVEL -> {
                    val status = if (spatialState.isLevel) "PERFECTLY LEVEL" else "TILTED"
                    "Pitch: ${String.format("%.1f", spatialState.pitchDegrees)}° | Roll: ${String.format("%.1f", spatialState.rollDegrees)}° | $status"
                }
            }

            AnimatedVisibility(visible = secondaryText.isNotBlank()) {
                Text(
                    text = secondaryText,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF00FF9D),
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Guidance banner
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF16213E))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Instructions",
                    tint = Color(0xFF00F0FF),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = instructionText,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 11.sp
                )
            }
        }
    }
}
