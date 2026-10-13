package com.example.data

import kotlinx.coroutines.flow.Flow

class MeasurementRepository(private val dao: MeasurementDao) {

    val allMeasurements: Flow<List<MeasurementRecord>> = dao.getAllMeasurements()

    fun getMeasurementsByType(type: MeasurementType): Flow<List<MeasurementRecord>> =
        dao.getMeasurementsByType(type.name)

    suspend fun saveMeasurement(
        type: MeasurementType,
        label: String,
        formattedValue: String,
        primaryValue: Double,
        unit: String,
        details: String,
        pointCount: Int
    ): Long {
        val finalLabel = if (label.isNotBlank()) label else "${type.displayName} Measurement"
        val record = MeasurementRecord(
            type = type.name,
            label = finalLabel,
            formattedValue = formattedValue,
            primaryValue = primaryValue,
            unit = unit,
            details = details,
            pointCount = pointCount
        )
        return dao.insert(record)
    }

    suspend fun deleteMeasurement(record: MeasurementRecord) {
        dao.delete(record)
    }

    suspend fun deleteById(id: Long) {
        dao.deleteById(id)
    }

    suspend fun clearHistory() {
        dao.clearAll()
    }
}
