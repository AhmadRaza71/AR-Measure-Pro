package com.example.ar

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.ui.geometry.Offset
import com.example.model.Vector3
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class SpatialState(
    val pitchDegrees: Float = 0f,
    val rollDegrees: Float = 0f,
    val yawDegrees: Float = 0f,
    val surfaceInclinationDeg: Float = 0f,
    val isLevel: Boolean = false,
    val cameraDistanceMeters: Float = 1.25f,
    val trackingQuality: String = "High",
    val featurePointCount: Int = 48
)

class ARSpatialEngine(private val context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val _spatialState = MutableStateFlow(SpatialState())
    val spatialState: StateFlow<SpatialState> = _spatialState.asStateFlow()

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    // Virtual plane height relative to camera
    var planeGroundY: Float = -0.5f // 0.5 meters below camera eye

    fun startSensors() {
        rotationSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stopSensors() {
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            SensorManager.getOrientation(rotationMatrix, orientationAngles)

            // azimuth, pitch, roll in radians
            val pitchRad = orientationAngles[1]
            val rollRad = orientationAngles[2]
            val yawRad = orientationAngles[0]

            val pitchDeg = (pitchRad * (180.0 / PI)).toFloat()
            val rollDeg = (rollRad * (180.0 / PI)).toFloat()
            val yawDeg = (yawRad * (180.0 / PI)).toFloat()

            // Surface inclination from pure vertical or horizontal
            val totalTilt = sqrt(pitchDeg * pitchDeg + rollDeg * rollDeg)
            val isLevel = abs(pitchDeg) < 1.0f && abs(rollDeg) < 1.0f

            _spatialState.value = _spatialState.value.copy(
                pitchDegrees = pitchDeg,
                rollDegrees = rollDeg,
                yawDegrees = yawDeg,
                surfaceInclinationDeg = totalTilt,
                isLevel = isLevel
            )
        } else if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val ax = event.values[0]
            val ay = event.values[1]
            val az = event.values[2]

            val pitch = (atan2(ay.toDouble(), sqrt((ax * ax + az * az).toDouble())) * (180.0 / PI)).toFloat()
            val roll = (atan2(-ax.toDouble(), az.toDouble()) * (180.0 / PI)).toFloat()
            val totalTilt = sqrt(pitch * pitch + roll * roll)

            _spatialState.value = _spatialState.value.copy(
                pitchDegrees = pitch,
                rollDegrees = roll,
                surfaceInclinationDeg = totalTilt,
                isLevel = abs(pitch) < 1.0f && abs(roll) < 1.0f
            )
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    /**
     * Ray-cast from 2D screen coordinate (x, y) into 3D world space (Vector3).
     * Uses camera viewport dimensions, assumed FOV, and intersects with the detected spatial plane.
     */
    fun raycastScreenToWorld(
        screenX: Float,
        screenY: Float,
        viewWidth: Float,
        viewHeight: Float,
        currentDepth: Float = 1.25f
    ): Vector3 {
        if (viewWidth <= 0f || viewHeight <= 0f) return Vector3(0f, 0f, -currentDepth)

        // Normalized Device Coordinates (-1 to 1)
        val ndcX = (screenX - viewWidth / 2f) / (viewWidth / 2f)
        val ndcY = -(screenY - viewHeight / 2f) / (viewHeight / 2f)

        // Camera perspective frustum (approx 60 deg vertical FOV)
        val aspect = viewWidth / viewHeight
        val fovRad = (60.0 * PI / 180.0).toFloat()
        val tanHalfFov = kotlin.math.tan(fovRad / 2f)

        val rayX = ndcX * tanHalfFov * aspect
        val rayY = ndcY * tanHalfFov
        val rayZ = -1.0f

        val rayDir = Vector3(rayX, rayY, rayZ).normalize()

        // Calculate intersection with ground plane y = planeGroundY
        val t = if (abs(rayDir.y) > 0.05f) {
            val dist = (planeGroundY) / rayDir.y
            if (dist > 0.2f && dist < 10f) dist else currentDepth
        } else {
            currentDepth
        }

        return rayDir * t
    }

    /**
     * Projects 3D world coordinate (X, Y, Z) back to 2D screen pixel coordinates (Offset).
     * Used for rendering AR overlay annotations, lines, and 3D bounding boxes.
     */
    fun projectWorldToScreen(
        worldPos: Vector3,
        viewWidth: Float,
        viewHeight: Float
    ): Offset? {
        if (viewWidth <= 0f || viewHeight <= 0f) return null
        if (worldPos.z >= -0.05f) return null // Behind camera

        val aspect = viewWidth / viewHeight
        val fovRad = (60.0 * PI / 180.0).toFloat()
        val tanHalfFov = kotlin.math.tan(fovRad / 2f)

        val depth = -worldPos.z
        val ndcX = (worldPos.x / depth) / (tanHalfFov * aspect)
        val ndcY = (worldPos.y / depth) / tanHalfFov

        val screenX = (ndcX * (viewWidth / 2f)) + (viewWidth / 2f)
        val screenY = (-ndcY * (viewHeight / 2f)) + (viewHeight / 2f)

        return Offset(screenX, screenY)
    }

    /**
     * Synthesizes or detects a 3D point cloud cluster around a target center point
     * (e.g. for circular cylinder rim, hole, or circular pipe detection).
     * In physical AR, this is fed from LiDAR / depth / feature points.
     */
    fun sampleCircularPointCloud(
        center: Vector3,
        nominalRadius: Float,
        pointCount: Int = 36,
        noiseStdDev: Float = 0.008f,
        includeOutliers: Boolean = true
    ): List<Vector3> {
        val points = mutableListOf<Vector3>()
        val rand = java.util.Random(1337)

        for (i in 0 until pointCount) {
            val angle = (2.0 * PI * i / pointCount).toFloat()
            val noiseR = (rand.nextGaussian() * noiseStdDev).toFloat()
            val r = nominalRadius + noiseR
            val px = center.x + r * cos(angle)
            val pz = center.z + r * sin(angle)
            val py = center.y + (rand.nextGaussian() * (noiseStdDev * 0.5f)).toFloat()
            points.add(Vector3(px, py, pz))
        }

        if (includeOutliers) {
            // Add 10% realistic outliers from nearby floor/rim debris
            val outlierCount = (pointCount * 0.15f).toInt()
            for (i in 0 until outlierCount) {
                val ox = center.x + (rand.nextFloat() - 0.5f) * nominalRadius * 2.5f
                val oz = center.z + (rand.nextFloat() - 0.5f) * nominalRadius * 2.5f
                val oy = center.y + (rand.nextFloat() - 0.5f) * 0.1f
                points.add(Vector3(ox, oy, oz))
            }
        }

        return points
    }
}
