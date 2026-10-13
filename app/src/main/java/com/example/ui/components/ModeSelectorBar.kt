package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SquareFoot
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.MeasurementType

@Composable
fun ModeSelectorBar(
    currentMode: MeasurementType,
    onSelectMode: (MeasurementType) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MeasurementType.values().forEach { mode ->
            val isSelected = currentMode == mode
            val icon = getModeIcon(mode)

            FilterChip(
                selected = isSelected,
                onClick = { onSelectMode(mode) },
                label = { Text(mode.displayName, style = MaterialTheme.typography.labelMedium) },
                leadingIcon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = mode.displayName,
                        modifier = Modifier.size(16.dp),
                        tint = if (isSelected) Color(0xFF0B132B) else Color(0xFF00F0FF)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color(0xCC1C2541),
                    labelColor = Color.White,
                    selectedContainerColor = Color(0xFF00F0FF),
                    selectedLabelColor = Color(0xFF0B132B)
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = if (isSelected) Color(0xFF00F0FF) else Color(0x6600F0FF),
                    borderWidth = 1.dp
                ),
                modifier = Modifier.testTag("mode_chip_${mode.name.lowercase()}")
            )
        }
    }
}

private fun getModeIcon(type: MeasurementType): ImageVector {
    return when (type) {
        MeasurementType.DISTANCE -> Icons.Default.Straighten
        MeasurementType.CHAIN -> Icons.AutoMirrored.Filled.TrendingFlat
        MeasurementType.ANGLE -> Icons.Default.SquareFoot
        MeasurementType.CIRCLE_DIAMETER -> Icons.Default.FiberManualRecord
        MeasurementType.CIRCLE_RECESS -> Icons.Default.VerticalAlignBottom
        MeasurementType.AREA -> Icons.Default.CropFree
        MeasurementType.VOLUME -> Icons.Default.ViewInAr
        MeasurementType.SPIRIT_LEVEL -> Icons.Default.ScreenRotation
    }
}
