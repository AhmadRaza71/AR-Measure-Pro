package com.example.model

import java.util.Locale

enum class UnitSystem(val displayName: String) {
    METRIC("Metric (m/cm)"),
    IMPERIAL("Imperial (ft/in)")
}

object UnitFormatter {

    fun formatDistance(meters: Float, unitSystem: UnitSystem): String {
        return when (unitSystem) {
            UnitSystem.METRIC -> {
                when {
                    meters < 0.05f -> String.format(Locale.US, "%.1f mm", meters * 1000f)
                    meters < 1.0f -> String.format(Locale.US, "%.1f cm", meters * 100f)
                    else -> String.format(Locale.US, "%.2f m", meters)
                }
            }
            UnitSystem.IMPERIAL -> {
                val totalInches = meters * 39.3701f
                if (totalInches < 12f) {
                    String.format(Locale.US, "%.1f in", totalInches)
                } else {
                    val feet = (totalInches / 12f).toInt()
                    val remainingInches = totalInches % 12f
                    String.format(Locale.US, "%d' %.1f\"", feet, remainingInches)
                }
            }
        }
    }

    fun formatDetailedDistance(meters: Float): Map<String, String> {
        return mapOf(
            "m" to String.format(Locale.US, "%.3f m", meters),
            "cm" to String.format(Locale.US, "%.1f cm", meters * 100f),
            "mm" to String.format(Locale.US, "%.0f mm", meters * 1000f),
            "in" to String.format(Locale.US, "%.2f in", meters * 39.3701f),
            "ft" to String.format(Locale.US, "%.2f ft", meters * 3.28084f)
        )
    }

    fun formatAngle(degrees: Float): String {
        return String.format(Locale.US, "%.1f°", degrees)
    }

    fun formatArea(sqMeters: Float, unitSystem: UnitSystem): String {
        return when (unitSystem) {
            UnitSystem.METRIC -> {
                if (sqMeters < 0.01f) {
                    String.format(Locale.US, "%.1f cm²", sqMeters * 10000f)
                } else {
                    String.format(Locale.US, "%.3f m²", sqMeters)
                }
            }
            UnitSystem.IMPERIAL -> {
                val sqFeet = sqMeters * 10.7639f
                if (sqFeet < 1f) {
                    String.format(Locale.US, "%.1f sq in", sqMeters * 1550f)
                } else {
                    String.format(Locale.US, "%.2f sq ft", sqFeet)
                }
            }
        }
    }

    fun formatVolume(cubicMeters: Float, unitSystem: UnitSystem): String {
        return when (unitSystem) {
            UnitSystem.METRIC -> {
                val liters = cubicMeters * 1000f
                if (cubicMeters < 0.01f) {
                    String.format(Locale.US, "%.2f L (%.0f cm³)", liters, cubicMeters * 1_000_000f)
                } else {
                    String.format(Locale.US, "%.3f m³ (%.1f L)", cubicMeters, liters)
                }
            }
            UnitSystem.IMPERIAL -> {
                val cuFeet = cubicMeters * 35.3147f
                if (cuFeet < 1f) {
                    String.format(Locale.US, "%.1f cu in", cubicMeters * 61023.7f)
                } else {
                    String.format(Locale.US, "%.2f cu ft", cuFeet)
                }
            }
        }
    }
}
