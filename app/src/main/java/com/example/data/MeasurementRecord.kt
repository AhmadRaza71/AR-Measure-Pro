package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(tableName = "measurement_history")
data class MeasurementRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val type: String, // String representation of MeasurementType
    val label: String, // Custom label (e.g., "Dining Table", "Pipe Bore")
    val formattedValue: String, // e.g. "2.45 m", "45.2°", "1.28 m²", "0.450 m³"
    val primaryValue: Double, // Raw value in base units (m, deg, m^2, m^3)
    val unit: String,
    val details: String, // e.g., "L: 1.2m, W: 0.8m, H: 0.5m", "Radius: 12.5 cm, Rim Offset: 4.2 cm"
    val pointCount: Int = 0
) {
    fun getFormattedDate(): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
