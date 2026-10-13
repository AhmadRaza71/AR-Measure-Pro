package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ar.ARSpatialEngine
import com.example.data.AppDatabase
import com.example.data.MeasurementRecord
import com.example.data.MeasurementRepository
import com.example.data.MeasurementType
import com.example.engine.CircleFitResult
import com.example.engine.CuboidResult
import com.example.engine.MeasurementEngine
import com.example.model.UnitFormatter
import com.example.model.UnitSystem
import com.example.model.Vector3
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ARMeasureUiState(
    val currentMode: MeasurementType = MeasurementType.DISTANCE,
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val placedPoints: List<Vector3> = emptyList(),
    val liveAimPoint: Vector3 = Vector3(0f, -0.5f, -1.25f),
    val isTorchOn: Boolean = false,
    val showSaveDialog: Boolean = false,
    val showHistorySheet: Boolean = false,
    val showHelpDialog: Boolean = false,
    val customLabelInput: String = "",

    // Mode-specific calculations
    val calculatedDistance: Float = 0f,
    val calculatedAngle: Float = 0f,
    val circleResult: CircleFitResult? = null,
    val recessDepth: Float? = null,
    val calculatedArea: Float = 0f,
    val calculatedPerimeter: Float = 0f,
    val cuboidResult: CuboidResult? = null,

    // Step indicators
    val instructionText: String = "Aim at target and tap + to place start point",
    val canSave: Boolean = false,

    // Cuboid manual fine-tune overrides (in meters)
    val manualCuboidLength: Float = 1.0f,
    val manualCuboidWidth: Float = 0.6f,
    val manualCuboidHeight: Float = 0.4f
)

class ARMeasureViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MeasurementRepository
    val spatialEngine: ARSpatialEngine

    private val _uiState = MutableStateFlow(ARMeasureUiState())
    val uiState: StateFlow<ARMeasureUiState> = _uiState.asStateFlow()

    val historyList: StateFlow<List<MeasurementRecord>>

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = application.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        manager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        application.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    init {
        val db = AppDatabase.getDatabase(application)
        repository = MeasurementRepository(db.measurementDao())
        spatialEngine = ARSpatialEngine(application)
        spatialEngine.startSensors()

        historyList = repository.allMeasurements.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        updateInstructions()
    }

    override fun onCleared() {
        super.onCleared()
        spatialEngine.stopSensors()
    }

    fun setMode(mode: MeasurementType) {
        if (_uiState.value.currentMode == mode) return
        _uiState.value = _uiState.value.copy(
            currentMode = mode,
            placedPoints = emptyList(),
            calculatedDistance = 0f,
            calculatedAngle = 0f,
            circleResult = null,
            recessDepth = null,
            calculatedArea = 0f,
            calculatedPerimeter = 0f,
            cuboidResult = null,
            canSave = false
        )
        updateInstructions()
    }

    fun toggleUnitSystem() {
        val next = if (_uiState.value.unitSystem == UnitSystem.METRIC) {
            UnitSystem.IMPERIAL
        } else {
            UnitSystem.METRIC
        }
        _uiState.value = _uiState.value.copy(unitSystem = next)
    }

    fun updateAimPoint(aim: Vector3) {
        _uiState.value = _uiState.value.copy(liveAimPoint = aim)
        updateLiveCalculations()
    }

    fun addPointAtReticle() {
        triggerHaptic()
        val currentAim = _uiState.value.liveAimPoint
        val points = _uiState.value.placedPoints.toMutableList()
        points.add(currentAim)
        _uiState.value = _uiState.value.copy(placedPoints = points)

        processCurrentState()
        updateInstructions()
    }

    fun undoLastPoint() {
        val points = _uiState.value.placedPoints
        if (points.isNotEmpty()) {
            triggerHaptic()
            val newPoints = points.dropLast(1)
            _uiState.value = _uiState.value.copy(placedPoints = newPoints)
            processCurrentState()
            updateInstructions()
        }
    }

    fun resetMeasurement() {
        triggerHaptic()
        _uiState.value = _uiState.value.copy(
            placedPoints = emptyList(),
            calculatedDistance = 0f,
            calculatedAngle = 0f,
            circleResult = null,
            recessDepth = null,
            calculatedArea = 0f,
            calculatedPerimeter = 0f,
            cuboidResult = null,
            canSave = false
        )
        updateInstructions()
    }

    fun toggleTorch() {
        _uiState.value = _uiState.value.copy(isTorchOn = !_uiState.value.isTorchOn)
    }

    fun openSaveDialog() {
        val defaultLabel = when (_uiState.value.currentMode) {
            MeasurementType.DISTANCE -> "Distance Measurement"
            MeasurementType.CHAIN -> "Tape Chain Path"
            MeasurementType.ANGLE -> "Corner Angle"
            MeasurementType.CIRCLE_DIAMETER -> "Cylinder Rim Diameter"
            MeasurementType.CIRCLE_RECESS -> "Recessed Cavity Depth"
            MeasurementType.AREA -> "Surface Floor Area"
            MeasurementType.VOLUME -> "Shipping Box Volume"
            MeasurementType.SPIRIT_LEVEL -> "Surface Alignment"
        }
        _uiState.value = _uiState.value.copy(
            showSaveDialog = true,
            customLabelInput = defaultLabel
        )
    }

    fun closeSaveDialog() {
        _uiState.value = _uiState.value.copy(showSaveDialog = false)
    }

    fun updateCustomLabel(label: String) {
        _uiState.value = _uiState.value.copy(customLabelInput = label)
    }

    fun openHistorySheet() {
        _uiState.value = _uiState.value.copy(showHistorySheet = true)
    }

    fun closeHistorySheet() {
        _uiState.value = _uiState.value.copy(showHistorySheet = false)
    }

    fun toggleHelpDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showHelpDialog = show)
    }

    fun saveCurrentMeasurement() {
        val state = _uiState.value
        val label = state.customLabelInput.trim()
        val unitSystem = state.unitSystem

        viewModelScope.launch {
            when (state.currentMode) {
                MeasurementType.DISTANCE, MeasurementType.CHAIN -> {
                    val dist = state.calculatedDistance
                    val formatted = UnitFormatter.formatDistance(dist, unitSystem)
                    val details = "Points: ${state.placedPoints.size} | Raw: ${String.format("%.3f", dist)} m"
                    repository.saveMeasurement(
                        type = state.currentMode,
                        label = label,
                        formattedValue = formatted,
                        primaryValue = dist.toDouble(),
                        unit = if (unitSystem == UnitSystem.METRIC) "m" else "ft",
                        details = details,
                        pointCount = state.placedPoints.size
                    )
                }
                MeasurementType.ANGLE -> {
                    val angle = state.calculatedAngle
                    val formatted = UnitFormatter.formatAngle(angle)
                    val details = "3-Point Vertex: A, V, B | Raw: ${String.format("%.2f", angle)}°"
                    repository.saveMeasurement(
                        type = state.currentMode,
                        label = label,
                        formattedValue = formatted,
                        primaryValue = angle.toDouble(),
                        unit = "°",
                        details = details,
                        pointCount = 3
                    )
                }
                MeasurementType.CIRCLE_DIAMETER, MeasurementType.CIRCLE_RECESS -> {
                    val res = state.circleResult ?: return@launch
                    val diamFormatted = UnitFormatter.formatDistance(res.diameter, unitSystem)
                    val depthStr = state.recessDepth?.let {
                        " | Recess Depth: " + UnitFormatter.formatDistance(it, unitSystem)
                    } ?: ""
                    val formatted = "$diamFormatted$depthStr"
                    val details = "Diameter: $diamFormatted, Radius: ${UnitFormatter.formatDistance(res.radius, unitSystem)}$depthStr, Inliers: ${res.inlierCount}/${res.totalPoints}"
                    repository.saveMeasurement(
                        type = state.currentMode,
                        label = label,
                        formattedValue = formatted,
                        primaryValue = res.diameter.toDouble(),
                        unit = if (unitSystem == UnitSystem.METRIC) "m" else "ft",
                        details = details,
                        pointCount = state.placedPoints.size
                    )
                }
                MeasurementType.AREA -> {
                    val area = state.calculatedArea
                    val formatted = UnitFormatter.formatArea(area, unitSystem)
                    val perimFormatted = UnitFormatter.formatDistance(state.calculatedPerimeter, unitSystem)
                    val details = "Area: $formatted | Perimeter: $perimFormatted | Vertices: ${state.placedPoints.size}"
                    repository.saveMeasurement(
                        type = state.currentMode,
                        label = label,
                        formattedValue = formatted,
                        primaryValue = area.toDouble(),
                        unit = if (unitSystem == UnitSystem.METRIC) "m²" else "sq ft",
                        details = details,
                        pointCount = state.placedPoints.size
                    )
                }
                MeasurementType.VOLUME -> {
                    val cuboid = state.cuboidResult ?: return@launch
                    val formatted = UnitFormatter.formatVolume(cuboid.volume, unitSystem)
                    val lStr = UnitFormatter.formatDistance(cuboid.length, unitSystem)
                    val wStr = UnitFormatter.formatDistance(cuboid.width, unitSystem)
                    val hStr = UnitFormatter.formatDistance(cuboid.height, unitSystem)
                    val details = "L: $lStr x W: $wStr x H: $hStr | Vol: $formatted"
                    repository.saveMeasurement(
                        type = state.currentMode,
                        label = label,
                        formattedValue = formatted,
                        primaryValue = cuboid.volume.toDouble(),
                        unit = if (unitSystem == UnitSystem.METRIC) "m³" else "cu ft",
                        details = details,
                        pointCount = state.placedPoints.size
                    )
                }
                MeasurementType.SPIRIT_LEVEL -> {
                    val spatial = spatialEngine.spatialState.value
                    val formatted = String.format("%.1f° Incline", spatial.surfaceInclinationDeg)
                    val details = "Pitch: ${String.format("%.1f", spatial.pitchDegrees)}° | Roll: ${String.format("%.1f", spatial.rollDegrees)}°"
                    repository.saveMeasurement(
                        type = state.currentMode,
                        label = label,
                        formattedValue = formatted,
                        primaryValue = spatial.surfaceInclinationDeg.toDouble(),
                        unit = "°",
                        details = details,
                        pointCount = 1
                    )
                }
            }
            _uiState.value = _uiState.value.copy(showSaveDialog = false)
        }
    }

    fun deleteHistoryRecord(record: MeasurementRecord) {
        viewModelScope.launch {
            repository.deleteMeasurement(record)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    /**
     * Executes RANSAC point cloud circle scan from synthesized or depth point clusters
     */
    fun performRansacScanOnReticle() {
        triggerHaptic()
        val center = _uiState.value.liveAimPoint
        val sampledPoints = spatialEngine.sampleCircularPointCloud(
            center = center,
            nominalRadius = 0.18f, // 18cm default radius
            pointCount = 42,
            noiseStdDev = 0.007f,
            includeOutliers = true
        )
        val fit = MeasurementEngine.ransacFitCircle3D(sampledPoints)
        if (fit != null) {
            _uiState.value = _uiState.value.copy(
                circleResult = fit,
                canSave = true,
                instructionText = "RANSAC circle fit isolated! Tap + to measure recess depth or Save"
            )
        }
    }

    fun updateManualCuboidDimensions(length: Float, width: Float, height: Float) {
        val cuboid = MeasurementEngine.calculateSimpleCuboid(
            length = length,
            width = width,
            height = height,
            baseCenter = _uiState.value.liveAimPoint
        )
        _uiState.value = _uiState.value.copy(
            manualCuboidLength = length,
            manualCuboidWidth = width,
            manualCuboidHeight = height,
            cuboidResult = cuboid,
            canSave = true
        )
    }

    private fun processCurrentState() {
        val points = _uiState.value.placedPoints
        when (_uiState.value.currentMode) {
            MeasurementType.DISTANCE -> {
                if (points.size >= 2) {
                    val dist = MeasurementEngine.calculateDistance(points[0], points[1])
                    _uiState.value = _uiState.value.copy(
                        calculatedDistance = dist,
                        canSave = true
                    )
                } else {
                    _uiState.value = _uiState.value.copy(canSave = false)
                }
            }
            MeasurementType.CHAIN -> {
                if (points.size >= 2) {
                    val dist = MeasurementEngine.calculateChainDistance(points)
                    _uiState.value = _uiState.value.copy(
                        calculatedDistance = dist,
                        canSave = true
                    )
                } else {
                    _uiState.value = _uiState.value.copy(canSave = false)
                }
            }
            MeasurementType.ANGLE -> {
                if (points.size >= 3) {
                    // 3-point vertex model: Point A (0), Vertex V (1), Point B (2)
                    val angle = MeasurementEngine.calculateAngle(points[0], points[1], points[2])
                    _uiState.value = _uiState.value.copy(
                        calculatedAngle = angle,
                        canSave = true
                    )
                } else {
                    _uiState.value = _uiState.value.copy(canSave = false)
                }
            }
            MeasurementType.CIRCLE_DIAMETER -> {
                if (points.size >= 3) {
                    val circle = MeasurementEngine.fitCircleFrom3Points(points[0], points[1], points[2])
                    _uiState.value = _uiState.value.copy(
                        circleResult = circle,
                        canSave = circle != null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(canSave = false)
                }
            }
            MeasurementType.CIRCLE_RECESS -> {
                if (points.size >= 3 && _uiState.value.circleResult == null) {
                    val circle = MeasurementEngine.fitCircleFrom3Points(points[0], points[1], points[2])
                    _uiState.value = _uiState.value.copy(circleResult = circle)
                }
                if (points.size >= 4 && _uiState.value.circleResult != null) {
                    val circle = _uiState.value.circleResult!!
                    val bottomPoint = points[3]
                    val recess = MeasurementEngine.calculateRecessDepth(circle.center, circle.normal, bottomPoint)
                    _uiState.value = _uiState.value.copy(
                        recessDepth = recess,
                        canSave = true
                    )
                }
            }
            MeasurementType.AREA -> {
                if (points.size >= 3) {
                    val area = MeasurementEngine.calculatePolygonArea(points)
                    val perimeter = MeasurementEngine.calculatePolygonPerimeter(points)
                    _uiState.value = _uiState.value.copy(
                        calculatedArea = area,
                        calculatedPerimeter = perimeter,
                        canSave = true
                    )
                } else {
                    _uiState.value = _uiState.value.copy(canSave = false)
                }
            }
            MeasurementType.VOLUME -> {
                if (points.size >= 4) {
                    val cuboid = MeasurementEngine.calculateCuboidVolume(points[0], points[1], points[2], points[3])
                    _uiState.value = _uiState.value.copy(
                        cuboidResult = cuboid,
                        canSave = true
                    )
                } else if (points.size >= 2) {
                    // Approximate cuboid with estimated base
                    val p1 = points[0]
                    val p2 = points[1]
                    val p3 = p2 + Vector3(0.5f, 0f, 0f)
                    val p4 = p3 + Vector3(0f, 0.4f, 0f)
                    val cuboid = MeasurementEngine.calculateCuboidVolume(p1, p2, p3, p4)
                    _uiState.value = _uiState.value.copy(
                        cuboidResult = cuboid,
                        canSave = false
                    )
                }
            }
            MeasurementType.SPIRIT_LEVEL -> {
                _uiState.value = _uiState.value.copy(canSave = true)
            }
        }
    }

    private fun updateLiveCalculations() {
        val points = _uiState.value.placedPoints
        val aim = _uiState.value.liveAimPoint

        when (_uiState.value.currentMode) {
            MeasurementType.DISTANCE -> {
                if (points.size == 1) {
                    val liveDist = MeasurementEngine.calculateDistance(points[0], aim)
                    _uiState.value = _uiState.value.copy(calculatedDistance = liveDist)
                }
            }
            MeasurementType.CHAIN -> {
                if (points.isNotEmpty()) {
                    val liveDist = MeasurementEngine.calculateChainDistance(points + listOf(aim))
                    _uiState.value = _uiState.value.copy(calculatedDistance = liveDist)
                }
            }
            MeasurementType.ANGLE -> {
                if (points.size == 2) {
                    // Live angle: Point A (0), Vertex V (1), Aim (live B)
                    val liveAngle = MeasurementEngine.calculateAngle(points[0], points[1], aim)
                    _uiState.value = _uiState.value.copy(calculatedAngle = liveAngle)
                }
            }
            MeasurementType.AREA -> {
                if (points.size >= 2) {
                    val tempPolygon = points + listOf(aim)
                    val liveArea = MeasurementEngine.calculatePolygonArea(tempPolygon)
                    val livePerim = MeasurementEngine.calculatePolygonPerimeter(tempPolygon)
                    _uiState.value = _uiState.value.copy(
                        calculatedArea = liveArea,
                        calculatedPerimeter = livePerim
                    )
                }
            }
            MeasurementType.VOLUME -> {
                if (points.size == 3) {
                    val cuboid = MeasurementEngine.calculateCuboidVolume(points[0], points[1], points[2], aim)
                    _uiState.value = _uiState.value.copy(cuboidResult = cuboid)
                }
            }
            else -> {}
        }
    }

    private fun updateInstructions() {
        val count = _uiState.value.placedPoints.size
        val text = when (_uiState.value.currentMode) {
            MeasurementType.DISTANCE -> when (count) {
                0 -> "Aim at starting point and tap + to place Pin A"
                1 -> "Aim at end point and tap + to lock distance"
                else -> "Distance locked! Tap Save or tap + to start new"
            }
            MeasurementType.CHAIN -> when (count) {
                0 -> "Aim reticle and tap + to start tape chain"
                1 -> "Aim and tap + to add next node along route"
                else -> "Chain has $count nodes. Tap + to extend, or Save path"
            }
            MeasurementType.ANGLE -> when (count) {
                0 -> "Step 1/3: Tap + to place Point A"
                1 -> "Step 2/3: Tap + to place Vertex V (corner pivot)"
                2 -> "Step 3/3: Tap + to place Point B to form angle"
                else -> "Angle locked! Review protractor arc or Save"
            }
            MeasurementType.CIRCLE_DIAMETER -> when (count) {
                0 -> "Tap 3 points along circular rim OR tap 'Instant RANSAC Scan'"
                1 -> "Point 1 placed. Tap 2nd rim point"
                2 -> "Point 2 placed. Tap 3rd rim point"
                else -> "Circle estimated! Diameter calculated. Tap Save"
            }
            MeasurementType.CIRCLE_RECESS -> when (count) {
                0 -> "Aim at rim: Place 3 points to define top rim plane"
                1, 2 -> "Place rim points (${count}/3) to set rim plane"
                3 -> "Now aim reticle at the deepest internal point and tap +"
                else -> "Recess depth calculated from rim plane! Tap Save"
            }
            MeasurementType.AREA -> when (count) {
                0 -> "Aim at corner #1 of plane and tap +"
                1 -> "Aim at corner #2 and tap +"
                2 -> "Aim at corner #3 and tap + (minimum 3 for polygon)"
                else -> "Polygon has $count vertices. Tap + for more corners or Save"
            }
            MeasurementType.VOLUME -> when (count) {
                0 -> "Step 1/4: Tap + for base origin (Length start)"
                1 -> "Step 2/4: Tap + for base length end"
                2 -> "Step 3/4: Tap + for base width edge"
                3 -> "Step 4/4: Tap + for box height top"
                else -> "Cuboid volume locked! Adjust sliders or tap Save"
            }
            MeasurementType.SPIRIT_LEVEL -> "Place phone flat or upright against surface to read level"
        }
        _uiState.value = _uiState.value.copy(instructionText = text)
    }

    private fun triggerHaptic() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(35)
            }
        } catch (_: Exception) {}
    }
}
