package com.example.engine

import com.example.model.Vector3
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.max
import kotlin.math.sqrt
import kotlin.random.Random

data class CircleFitResult(
    val center: Vector3,
    val normal: Vector3,
    val radius: Float,
    val diameter: Float = radius * 2f,
    val inlierCount: Int = 0,
    val totalPoints: Int = 0,
    val confidence: Float = 1.0f
)

data class CuboidResult(
    val length: Float,
    val width: Float,
    val height: Float,
    val volume: Float,
    val surfaceArea: Float,
    val cornerVertices: List<Vector3>
)

object MeasurementEngine {

    /**
     * Euclidean Distance Formula:
     * Computes 3D distance between point A(x1, y1, z1) and B(x2, y2, z2):
     * Distance = sqrt((x2 - x1)^2 + (y2 - y1)^2 + (z2 - z1)^2)
     */
    fun calculateDistance(start: Vector3, end: Vector3): Float {
        return start.distanceTo(end)
    }

    fun calculateDistance(x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        val dz = z2 - z1
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    /**
     * Multi-point continuous tape measurement chain
     */
    fun calculateChainDistance(points: List<Vector3>): Float {
        if (points.size < 2) return 0f
        var total = 0f
        for (i in 0 until points.size - 1) {
            total += points[i].distanceTo(points[i + 1])
        }
        return total
    }

    /**
     * Angle Measurement Engine:
     * Three-Point Vertex Model: Requires placing vertex V, point A, and point B.
     * Forms vectors VA and VB.
     * θ = arccos((VA · VB) / (|VA| |VB|))
     */
    fun calculateAngle(a: Vector3, vertex: Vector3, b: Vector3): Float {
        val va = (a - vertex).normalize()
        val vb = (b - vertex).normalize()
        val cosTheta = va.dot(vb).coerceIn(-1.0f, 1.0f)
        val radians = acos(cosTheta)
        return (radians * (180.0f / Math.PI.toFloat()))
    }

    /**
     * Exact 3-Point Circle Fit on a 3D Plane:
     * Given 3 points on the circle rim, calculates circumcenter, normal, and radius in 3D.
     */
    fun fitCircleFrom3Points(p1: Vector3, p2: Vector3, p3: Vector3): CircleFitResult? {
        val v1 = p2 - p1
        val v2 = p3 - p1
        val normal = v1.cross(v2)
        val normalLen = normal.length()
        if (normalLen < 1e-6f) return null // Collinear points

        val unitNormal = normal.normalize()

        // Circumcenter formula using cross products:
        // C = P1 + ((|v1|^2 * (v2 x normal) + |v2|^2 * (normal x v1)) / (2 * |normal|^2))
        val lenV1Sq = v1.lengthSquared()
        val lenV2Sq = v2.lengthSquared()

        val term1 = v2.cross(normal) * lenV1Sq
        val term2 = normal.cross(v1) * lenV2Sq
        val offset = (term1 + term2) * (1f / (2f * normalLen * normalLen))

        val center = p1 + offset
        val radius = center.distanceTo(p1)

        return CircleFitResult(
            center = center,
            normal = unitNormal,
            radius = radius,
            inlierCount = 3,
            totalPoints = 3,
            confidence = 1.0f
        )
    }

    /**
     * Circular Object Analysis via RANSAC 3D Circle Fitting:
     * Iteratively samples 3 points from the point cloud cluster to fit candidate circles.
     * Evaluates inliers within distance threshold, finds best consensus model,
     * and isolates outliers to compute the exact center (xc, yc, zc) and Radius R (Diameter = 2R).
     */
    fun ransacFitCircle3D(
        points: List<Vector3>,
        maxIterations: Int = 120,
        distanceThreshold: Float = 0.02f // 2 cm tolerance
    ): CircleFitResult? {
        if (points.size < 3) return null
        if (points.size == 3) {
            return fitCircleFrom3Points(points[0], points[1], points[2])
        }

        var bestModel: CircleFitResult? = null
        var maxInliers = 0
        var bestInliers = listOf<Vector3>()

        val rand = Random(42)

        for (iter in 0 until maxIterations) {
            // Pick 3 random distinct points
            val idx1 = rand.nextInt(points.size)
            var idx2 = rand.nextInt(points.size)
            while (idx2 == idx1) idx2 = rand.nextInt(points.size)
            var idx3 = rand.nextInt(points.size)
            while (idx3 == idx1 || idx3 == idx2) idx3 = rand.nextInt(points.size)

            val candidate = fitCircleFrom3Points(points[idx1], points[idx2], points[idx3]) ?: continue

            // Evaluate inliers:
            // Point distance to circle = sqrt((distance to circle plane)^2 + (|distance to center in plane| - radius)^2)
            val currentInliers = mutableListOf<Vector3>()
            for (p in points) {
                val dPlane = abs((p - candidate.center).dot(candidate.normal))
                val projOnPlane = p - (candidate.normal * (p - candidate.center).dot(candidate.normal))
                val dRadius = abs(projOnPlane.distanceTo(candidate.center) - candidate.radius)
                val totalDist = sqrt(dPlane * dPlane + dRadius * dRadius)

                if (totalDist <= distanceThreshold) {
                    currentInliers.add(p)
                }
            }

            if (currentInliers.size > maxInliers) {
                maxInliers = currentInliers.size
                bestInliers = currentInliers
                bestModel = candidate.copy(
                    inlierCount = currentInliers.size,
                    totalPoints = points.size,
                    confidence = currentInliers.size.toFloat() / points.size.toFloat()
                )
            }

            // Early exit if 90% inliers achieved
            if (maxInliers >= (points.size * 0.9f).toInt()) {
                break
            }
        }

        return bestModel
    }

    /**
     * Depth/Recess Estimation:
     * Calculates the perpendicular offset distance from the rim plane to the lowest
     * internal point of the recessed object (e.g. cylinder hole, cup, well depth).
     */
    fun calculateRecessDepth(rimPlaneCenter: Vector3, rimNormal: Vector3, bottomPoint: Vector3): Float {
        val unitNormal = rimNormal.normalize()
        val vectorToBottom = bottomPoint - rimPlaneCenter
        return abs(vectorToBottom.dot(unitNormal))
    }

    /**
     * Polygon Area Measurement Engine:
     * Uses 3D Shoelace / Vector Cross-Product Triangulation:
     * For any 3D planar or near-planar polygon defined by vertices P0, P1, ..., Pn-1:
     * Area Vector = 0.5 * sum(P_i x P_{i+1})
     * Area = |Area Vector|
     */
    fun calculatePolygonArea(polygon: List<Vector3>): Float {
        if (polygon.size < 3) return 0f
        var crossSum = Vector3(0f, 0f, 0f)
        val n = polygon.size

        // Reference point to ensure numerical stability
        val ref = polygon[0]

        for (i in 0 until n) {
            val pCurrent = polygon[i] - ref
            val pNext = polygon[(i + 1) % n] - ref
            crossSum = crossSum + pCurrent.cross(pNext)
        }

        return 0.5f * crossSum.length()
    }

    /**
     * Polygon Perimeter calculation
     */
    fun calculatePolygonPerimeter(polygon: List<Vector3>): Float {
        if (polygon.size < 2) return 0f
        var perimeter = 0f
        val n = polygon.size
        for (i in 0 until n) {
            perimeter += polygon[i].distanceTo(polygon[(i + 1) % n])
        }
        return perimeter
    }

    /**
     * Volume Measurement Feature:
     * Rectangular or Cuboid Shape definition:
     * Takes 4 defined AR points or base bounds:
     * - p1 -> p2 defines Length vector
     * - p2 -> p3 defines Width vector
     * - p3 -> p4 defines Height vector (or along plane normal)
     * Calculates volume = L * W * H and all 8 3D corner vertices of the cuboid box.
     */
    fun calculateCuboidVolume(
        p1: Vector3,
        p2: Vector3,
        p3: Vector3,
        p4: Vector3
    ): CuboidResult {
        val vLength = p2 - p1
        val length = max(0.001f, vLength.length())
        val dirL = if (length > 1e-4f) vLength / length else Vector3.RIGHT

        val vWidthCandidate = p3 - p2
        // Project vWidthCandidate onto plane perpendicular to dirL
        val projOnL = dirL * vWidthCandidate.dot(dirL)
        val vWidth = vWidthCandidate - projOnL
        val width = max(0.001f, vWidth.length())
        val dirW = if (width > 1e-4f) vWidth / width else Vector3.FORWARD

        // Height is perpendicular to length and width base plane
        val dirH = dirL.cross(dirW).normalize()
        val vHeightCandidate = p4 - p3
        val height = max(0.001f, abs(vHeightCandidate.dot(dirH)))

        val volume = length * width * height
        val surfaceArea = 2f * (length * width + length * height + width * height)

        // 8 Corners of the cuboid
        val base0 = p1
        val base1 = p1 + (dirL * length)
        val base2 = p1 + (dirL * length) + (dirW * width)
        val base3 = p1 + (dirW * width)

        val top0 = base0 + (dirH * height)
        val top1 = base1 + (dirH * height)
        val top2 = base2 + (dirH * height)
        val top3 = base3 + (dirH * height)

        val corners = listOf(base0, base1, base2, base3, top0, top1, top2, top3)

        return CuboidResult(
            length = length,
            width = width,
            height = height,
            volume = volume,
            surfaceArea = surfaceArea,
            cornerVertices = corners
        )
    }

    /**
     * Convenience cuboid calculation from 3 explicit dimensions
     */
    fun calculateSimpleCuboid(length: Float, width: Float, height: Float, baseCenter: Vector3 = Vector3.ZERO): CuboidResult {
        val l = max(0.001f, length)
        val w = max(0.001f, width)
        val h = max(0.001f, height)
        val volume = l * w * h
        val surfaceArea = 2f * (l * w + l * h + w * h)

        val halfL = l / 2f
        val halfW = w / 2f
        val base0 = baseCenter + Vector3(-halfL, 0f, -halfW)
        val base1 = baseCenter + Vector3(halfL, 0f, -halfW)
        val base2 = baseCenter + Vector3(halfL, 0f, halfW)
        val base3 = baseCenter + Vector3(-halfL, 0f, halfW)
        val top0 = base0 + Vector3(0f, h, 0f)
        val top1 = base1 + Vector3(0f, h, 0f)
        val top2 = base2 + Vector3(0f, h, 0f)
        val top3 = base3 + Vector3(0f, h, 0f)

        return CuboidResult(
            length = l,
            width = w,
            height = h,
            volume = volume,
            surfaceArea = surfaceArea,
            cornerVertices = listOf(base0, base1, base2, base3, top0, top1, top2, top3)
        )
    }
}
