package com.example

import com.example.engine.MeasurementEngine
import com.example.model.Vector3
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class ExampleUnitTest {

    @Test
    fun testEuclideanDistance3D() {
        // Point A (1, 2, 3) to Point B (4, 6, 3): dx=3, dy=4, dz=0 -> dist=5
        val p1 = Vector3(1f, 2f, 3f)
        val p2 = Vector3(4f, 6f, 3f)
        val dist = MeasurementEngine.calculateDistance(p1, p2)
        assertEquals(5.0f, dist, 0.001f)

        // Raw scalar helper
        val distScalar = MeasurementEngine.calculateDistance(1f, 2f, 3f, 4f, 6f, 3f)
        assertEquals(5.0f, distScalar, 0.001f)
    }

    @Test
    fun testChainDistance() {
        val points = listOf(
            Vector3(0f, 0f, 0f),
            Vector3(3f, 0f, 0f), // segment 1 = 3
            Vector3(3f, 4f, 0f), // segment 2 = 4
            Vector3(3f, 4f, 12f) // segment 3 = 12
        )
        val totalDist = MeasurementEngine.calculateChainDistance(points)
        assertEquals(19.0f, totalDist, 0.001f)
    }

    @Test
    fun testAngleThreePointVertex() {
        // Vertex V at origin (0,0,0)
        // Point A along X axis (1, 0, 0)
        // Point B along Y axis (0, 1, 0)
        // Angle should be exactly 90 degrees
        val a = Vector3(1f, 0f, 0f)
        val vertex = Vector3(0f, 0f, 0f)
        val b = Vector3(0f, 1f, 0f)

        val angle = MeasurementEngine.calculateAngle(a, vertex, b)
        assertEquals(90.0f, angle, 0.01f)

        // Opposite vectors -> 180 degrees
        val bOpposite = Vector3(-1f, 0f, 0f)
        val straightAngle = MeasurementEngine.calculateAngle(a, vertex, bOpposite)
        assertEquals(180.0f, straightAngle, 0.01f)
    }

    @Test
    fun testCircleFitAndRansac() {
        // Circle centered at (0, 0, -2) on X-Y plane with radius 0.5 (diameter 1.0)
        val p1 = Vector3(0.5f, 0f, -2f)
        val p2 = Vector3(0f, 0.5f, -2f)
        val p3 = Vector3(-0.5f, 0f, -2f)

        val fit = MeasurementEngine.fitCircleFrom3Points(p1, p2, p3)
        assertNotNull(fit)
        assertEquals(0.5f, fit!!.radius, 0.001f)
        assertEquals(1.0f, fit.diameter, 0.001f)
        assertEquals(0f, fit.center.x, 0.001f)
        assertEquals(0f, fit.center.y, 0.001f)
        assertEquals(-2f, fit.center.z, 0.001f)

        // Test with point cloud containing inliers and outliers
        val cloud = mutableListOf(
            Vector3(0.5f, 0f, -2f),
            Vector3(0f, 0.5f, -2f),
            Vector3(-0.5f, 0f, -2f),
            Vector3(0f, -0.5f, -2f),
            Vector3(0.353f, 0.353f, -2f),
            Vector3(-0.353f, 0.353f, -2f),
            // Outliers
            Vector3(2.5f, 1.2f, -1.0f),
            Vector3(-1.8f, -2.1f, -3.0f)
        )

        val ransac = MeasurementEngine.ransacFitCircle3D(cloud)
        assertNotNull(ransac)
        assertTrue(abs(ransac!!.diameter - 1.0f) < 0.05f)
        assertTrue(ransac.inlierCount >= 5)
    }

    @Test
    fun testRecessDepthEstimation() {
        // Rim plane horizontal at Y = 0 with normal pointing UP (0, 1, 0)
        val rimCenter = Vector3(0f, 0f, -1f)
        val rimNormal = Vector3(0f, 1f, 0f)
        // Bottom of hole/cylinder is 0.25 meters deeper along Y
        val bottomPoint = Vector3(0.05f, -0.25f, -1.02f)

        val depth = MeasurementEngine.calculateRecessDepth(rimCenter, rimNormal, bottomPoint)
        assertEquals(0.25f, depth, 0.001f)
    }

    @Test
    fun testPolygonAreaShoelace() {
        // 2m x 3m rectangle on X-Z plane
        val rect = listOf(
            Vector3(0f, 0f, 0f),
            Vector3(2f, 0f, 0f),
            Vector3(2f, 0f, 3f),
            Vector3(0f, 0f, 3f)
        )
        val area = MeasurementEngine.calculatePolygonArea(rect)
        val perimeter = MeasurementEngine.calculatePolygonPerimeter(rect)

        assertEquals(6.0f, area, 0.001f)
        assertEquals(10.0f, perimeter, 0.001f)
    }

    @Test
    fun testCuboidVolume() {
        // Box: Length = 2, Width = 3, Height = 1.5
        val p1 = Vector3(0f, 0f, 0f)
        val p2 = Vector3(2f, 0f, 0f)
        val p3 = Vector3(2f, 0f, 3f)
        val p4 = Vector3(2f, 1.5f, 3f)

        val cuboid = MeasurementEngine.calculateCuboidVolume(p1, p2, p3, p4)
        assertEquals(2.0f, cuboid.length, 0.01f)
        assertEquals(3.0f, cuboid.width, 0.01f)
        assertEquals(1.5f, cuboid.height, 0.01f)
        assertEquals(9.0f, cuboid.volume, 0.02f)
        assertEquals(8, cuboid.cornerVertices.size)
    }
}
