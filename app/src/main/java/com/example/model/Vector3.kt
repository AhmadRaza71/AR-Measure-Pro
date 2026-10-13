package com.example.model

import kotlin.math.acos
import kotlin.math.sqrt

/**
 * High-performance 3D Vector for AR and spatial geometric calculations.
 */
data class Vector3(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f
) {
    operator fun plus(other: Vector3): Vector3 =
        Vector3(x + other.x, y + other.y, z + other.z)

    operator fun minus(other: Vector3): Vector3 =
        Vector3(x - other.x, y - other.y, z - other.z)

    operator fun times(scalar: Float): Vector3 =
        Vector3(x * scalar, y * scalar, z * scalar)

    operator fun div(scalar: Float): Vector3 {
        require(scalar != 0f) { "Division by zero" }
        return Vector3(x / scalar, y / scalar, z / scalar)
    }

    operator fun unaryMinus(): Vector3 =
        Vector3(-x, -y, -z)

    fun dot(other: Vector3): Float =
        x * other.x + y * other.y + z * other.z

    fun cross(other: Vector3): Vector3 =
        Vector3(
            x = y * other.z - z * other.y,
            y = z * other.x - x * other.z,
            z = x * other.y - y * other.x
        )

    fun lengthSquared(): Float =
        x * x + y * y + z * z

    fun length(): Float =
        sqrt(lengthSquared())

    fun normalize(): Vector3 {
        val len = length()
        return if (len > 1e-6f) this * (1f / len) else Vector3(0f, 0f, 0f)
    }

    fun distanceTo(other: Vector3): Float =
        (this - other).length()

    companion object {
        val ZERO = Vector3(0f, 0f, 0f)
        val UP = Vector3(0f, 1f, 0f)
        val FORWARD = Vector3(0f, 0f, -1f)
        val RIGHT = Vector3(1f, 0f, 0f)

        fun angleDegrees(a: Vector3, vertex: Vector3, b: Vector3): Float {
            val va = (a - vertex).normalize()
            val vb = (b - vertex).normalize()
            val cosTheta = va.dot(vb).coerceIn(-1.0f, 1.0f)
            val radians = acos(cosTheta)
            return (radians * (180.0f / Math.PI.toFloat()))
        }
    }
}
