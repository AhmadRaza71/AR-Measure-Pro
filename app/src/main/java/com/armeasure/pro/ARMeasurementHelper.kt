package com.armeasure.pro

import android.content.Context
import com.google.ar.core.Frame
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.TrackingState

class ARMeasurementHelper {

    // Perform screen-center ray cast to detect real-world 3D points
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

    // Compute 3D Euclidean distance in meters
    fun calculateDistance(x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        val dz = z2 - z1
        return kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
    }
}
