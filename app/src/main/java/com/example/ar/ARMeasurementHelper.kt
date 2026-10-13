package com.example.ar

import android.content.Context
import com.example.engine.MeasurementEngine
import com.example.model.Vector3
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Frame
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import kotlin.math.sqrt

class ARMeasurementHelper(private val context: Context) {

    /**
     * Check if ARCore is installed and supported on the current device
     */
    fun isARCoreSupported(): Boolean {
        return try {
            val availability = ArCoreApk.getInstance().checkAvailability(context)
            availability.isSupported
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Perform screen-center ray cast to detect real-world 3D points
     */
    fun performHitTest(frame: Frame, x: Float, y: Float): HitResult? {
        val hitResults = frame.hitTest(x, y)
        for (hit in hitResults) {
            val trackable = hit.trackable
            if (trackable is Plane && trackable.isPoseInPolygon(hit.hitPose)) {
                return hit
            }
        }
        return hitResults.firstOrNull()
    }

    /**
     * Compute 3D Euclidean distance in meters
     */
    fun calculateDistance(x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        val dz = z2 - z1
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    /**
     * Extracts hit result pose into Vector3
     */
    fun extractHitVector(hitResult: HitResult): Vector3 {
        val pose = hitResult.hitPose
        return Vector3(pose.tx(), pose.ty(), pose.tz())
    }
}
