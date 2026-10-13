package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.example.ar.ARSpatialEngine
import com.example.ar.SpatialState
import com.example.data.MeasurementType
import com.example.engine.CircleFitResult
import com.example.engine.CuboidResult
import com.example.model.UnitFormatter
import com.example.model.UnitSystem
import com.example.model.Vector3
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

val CyanReticle = Color(0xFF00F0FF)
val NeonYellow = Color(0xFFFFB703)
val LaserGreen = Color(0xFF00FF9D)
val AccentRed = Color(0xFFFF4D4D)
val GlowBlue = Color(0x6600F0FF)

@Composable
fun AROverlayCanvas(
    spatialEngine: ARSpatialEngine,
    spatialState: SpatialState,
    currentMode: MeasurementType,
    unitSystem: UnitSystem,
    placedPoints: List<Vector3>,
    liveAimPoint: Vector3,
    circleResult: CircleFitResult?,
    recessDepth: Float?,
    cuboidResult: CuboidResult?,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val center = Offset(width / 2f, height / 2f)

        // 1. Draw central HUD Reticle with dynamic targeting brackets
        drawTargetReticle(center, spatialState.isLevel)

        // 2. Project placed points to 2D screen positions
        val projectedPoints = placedPoints.mapNotNull { worldPos ->
            spatialEngine.projectWorldToScreen(worldPos, width, height)?.let { screenPos ->
                worldPos to screenPos
            }
        }

        // Project live reticle aiming point
        val liveAimScreen = spatialEngine.projectWorldToScreen(liveAimPoint, width, height) ?: center

        // 3. Render specific geometry based on active mode
        when (currentMode) {
            MeasurementType.DISTANCE, MeasurementType.CHAIN -> {
                drawDistanceAndChain(
                    projectedPoints = projectedPoints,
                    liveAimScreen = liveAimScreen,
                    liveAimPoint = liveAimPoint,
                    currentMode = currentMode,
                    unitSystem = unitSystem,
                    textMeasurer = textMeasurer
                )
            }
            MeasurementType.ANGLE -> {
                drawAngleGeometry(
                    projectedPoints = projectedPoints,
                    liveAimScreen = liveAimScreen,
                    liveAimPoint = liveAimPoint,
                    textMeasurer = textMeasurer
                )
            }
            MeasurementType.CIRCLE_DIAMETER, MeasurementType.CIRCLE_RECESS -> {
                drawCircleGeometry(
                    projectedPoints = projectedPoints,
                    circleResult = circleResult,
                    recessDepth = recessDepth,
                    spatialEngine = spatialEngine,
                    width = width,
                    height = height,
                    unitSystem = unitSystem,
                    textMeasurer = textMeasurer
                )
            }
            MeasurementType.AREA -> {
                drawPolygonGeometry(
                    projectedPoints = projectedPoints,
                    liveAimScreen = liveAimScreen,
                    liveAimPoint = liveAimPoint,
                    unitSystem = unitSystem,
                    textMeasurer = textMeasurer
                )
            }
            MeasurementType.VOLUME -> {
                drawCuboidGeometry(
                    projectedPoints = projectedPoints,
                    cuboidResult = cuboidResult,
                    spatialEngine = spatialEngine,
                    width = width,
                    height = height,
                    unitSystem = unitSystem,
                    textMeasurer = textMeasurer
                )
            }
            MeasurementType.SPIRIT_LEVEL -> {
                drawSpiritLevel(center, spatialState, textMeasurer)
            }
        }

        // 4. Draw placed point pins with badges
        projectedPoints.forEachIndexed { index, (worldPos, screenPos) ->
            drawPin(
                pos = screenPos,
                label = when (currentMode) {
                    MeasurementType.ANGLE -> when (index) {
                        0 -> "A"
                        1 -> "V (Vertex)"
                        2 -> "B"
                        else -> "#${index + 1}"
                    }
                    MeasurementType.VOLUME -> "P${index + 1}"
                    else -> "${index + 1}"
                },
                color = if (index == 1 && currentMode == MeasurementType.ANGLE) NeonYellow else LaserGreen,
                textMeasurer = textMeasurer
            )
        }
    }
}

private fun DrawScope.drawTargetReticle(center: Offset, isLevel: Boolean) {
    val reticleColor = if (isLevel) LaserGreen else CyanReticle
    val radius = 28f
    val bracketLen = 14f

    // Center dot
    drawCircle(
        color = reticleColor,
        radius = 4f,
        center = center
    )

    // Outer thin circle
    drawCircle(
        color = reticleColor.copy(alpha = 0.4f),
        radius = radius,
        center = center,
        style = Stroke(width = 2f)
    )

    // Corner brackets
    val bOffset = radius + 6f
    // Top-Left
    drawLine(reticleColor, Offset(center.x - bOffset, center.y - bOffset), Offset(center.x - bOffset + bracketLen, center.y - bOffset), strokeWidth = 2.5f)
    drawLine(reticleColor, Offset(center.x - bOffset, center.y - bOffset), Offset(center.x - bOffset, center.y - bOffset + bracketLen), strokeWidth = 2.5f)
    // Top-Right
    drawLine(reticleColor, Offset(center.x + bOffset, center.y - bOffset), Offset(center.x + bOffset - bracketLen, center.y - bOffset), strokeWidth = 2.5f)
    drawLine(reticleColor, Offset(center.x + bOffset, center.y - bOffset), Offset(center.x + bOffset, center.y - bOffset + bracketLen), strokeWidth = 2.5f)
    // Bottom-Left
    drawLine(reticleColor, Offset(center.x - bOffset, center.y + bOffset), Offset(center.x - bOffset + bracketLen, center.y + bOffset), strokeWidth = 2.5f)
    drawLine(reticleColor, Offset(center.x - bOffset, center.y + bOffset), Offset(center.x - bOffset, center.y + bOffset - bracketLen), strokeWidth = 2.5f)
    // Bottom-Right
    drawLine(reticleColor, Offset(center.x + bOffset, center.y + bOffset), Offset(center.x + bOffset - bracketLen, center.y + bOffset), strokeWidth = 2.5f)
    drawLine(reticleColor, Offset(center.x + bOffset, center.y + bOffset), Offset(center.x + bOffset, center.y + bOffset - bracketLen), strokeWidth = 2.5f)
}

private fun DrawScope.drawDistanceAndChain(
    projectedPoints: List<Pair<Vector3, Offset>>,
    liveAimScreen: Offset,
    liveAimPoint: Vector3,
    currentMode: MeasurementType,
    unitSystem: UnitSystem,
    textMeasurer: TextMeasurer
) {
    if (projectedPoints.isEmpty()) return

    // Draw lines between existing points
    for (i in 0 until projectedPoints.size - 1) {
        val (p1World, p1Screen) = projectedPoints[i]
        val (p2World, p2Screen) = projectedPoints[i + 1]

        drawLine(
            color = LaserGreen,
            start = p1Screen,
            end = p2Screen,
            strokeWidth = 4f
        )

        val midScreen = Offset((p1Screen.x + p2Screen.x) / 2f, (p1Screen.y + p2Screen.y) / 2f)
        val dist = p1World.distanceTo(p2World)
        drawMetricBadge(
            center = midScreen,
            text = UnitFormatter.formatDistance(dist, unitSystem),
            color = LaserGreen,
            textMeasurer = textMeasurer
        )
    }

    // Draw dotted line to live aiming reticle
    if (currentMode == MeasurementType.DISTANCE && projectedPoints.size == 1) {
        val (p1World, p1Screen) = projectedPoints[0]
        drawLine(
            color = CyanReticle,
            start = p1Screen,
            end = liveAimScreen,
            strokeWidth = 3f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
        )
        val midScreen = Offset((p1Screen.x + liveAimScreen.x) / 2f, (p1Screen.y + liveAimScreen.y) / 2f)
        val dist = p1World.distanceTo(liveAimPoint)
        drawMetricBadge(
            center = midScreen,
            text = UnitFormatter.formatDistance(dist, unitSystem),
            color = CyanReticle,
            textMeasurer = textMeasurer
        )
    } else if (currentMode == MeasurementType.CHAIN && projectedPoints.isNotEmpty()) {
        val (lastWorld, lastScreen) = projectedPoints.last()
        drawLine(
            color = CyanReticle,
            start = lastScreen,
            end = liveAimScreen,
            strokeWidth = 3f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
        )
    }
}

private fun DrawScope.drawAngleGeometry(
    projectedPoints: List<Pair<Vector3, Offset>>,
    liveAimScreen: Offset,
    liveAimPoint: Vector3,
    textMeasurer: TextMeasurer
) {
    when (projectedPoints.size) {
        1 -> {
            // Point A placed, aiming at Vertex V
            val pA = projectedPoints[0].second
            drawLine(CyanReticle, pA, liveAimScreen, strokeWidth = 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f))
        }
        2 -> {
            // Point A (0) and Vertex V (1) placed, aiming at Point B
            val pA = projectedPoints[0].second
            val pV = projectedPoints[1].second
            drawLine(LaserGreen, pV, pA, strokeWidth = 4f)
            drawLine(NeonYellow, pV, liveAimScreen, strokeWidth = 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f))

            val angle = Vector3.angleDegrees(projectedPoints[0].first, projectedPoints[1].first, liveAimPoint)
            drawAngleArc(pV, pA, liveAimScreen, angle, textMeasurer)
        }
        3 -> {
            // Full A, V, B placed
            val pA = projectedPoints[0].second
            val pV = projectedPoints[1].second
            val pB = projectedPoints[2].second
            drawLine(LaserGreen, pV, pA, strokeWidth = 4f)
            drawLine(LaserGreen, pV, pB, strokeWidth = 4f)

            val angle = Vector3.angleDegrees(projectedPoints[0].first, projectedPoints[1].first, projectedPoints[2].first)
            drawAngleArc(pV, pA, pB, angle, textMeasurer)
        }
    }
}

private fun DrawScope.drawAngleArc(
    vertex: Offset,
    armA: Offset,
    armB: Offset,
    angleDeg: Float,
    textMeasurer: TextMeasurer
) {
    val dirA = (armA - vertex)
    val lenA = kotlin.math.sqrt(dirA.x * dirA.x + dirA.y * dirA.y)
    val dirB = (armB - vertex)
    val lenB = kotlin.math.sqrt(dirB.x * dirB.x + dirB.y * dirB.y)

    if (lenA > 10f && lenB > 10f) {
        val arcRadius = 45f
        val normA = Offset(dirA.x / lenA, dirA.y / lenA)
        val normB = Offset(dirB.x / lenB, dirB.y / lenB)

        val bisector = Offset((normA.x + normB.x) / 2f, (normA.y + normB.y) / 2f)
        val bisLen = kotlin.math.sqrt(bisector.x * bisector.x + bisector.y * bisector.y)

        val arcCenter = if (bisLen > 0.01f) {
            Offset(vertex.x + (bisector.x / bisLen) * (arcRadius + 20f), vertex.y + (bisector.y / bisLen) * (arcRadius + 20f))
        } else {
            Offset(vertex.x + 35f, vertex.y - 35f)
        }

        drawCircle(
            color = NeonYellow.copy(alpha = 0.25f),
            radius = arcRadius,
            center = vertex,
            style = Stroke(width = 3f)
        )

        drawMetricBadge(
            center = arcCenter,
            text = UnitFormatter.formatAngle(angleDeg),
            color = NeonYellow,
            textMeasurer = textMeasurer
        )
    }
}

private fun DrawScope.drawCircleGeometry(
    projectedPoints: List<Pair<Vector3, Offset>>,
    circleResult: CircleFitResult?,
    recessDepth: Float?,
    spatialEngine: ARSpatialEngine,
    width: Float,
    height: Float,
    unitSystem: UnitSystem,
    textMeasurer: TextMeasurer
) {
    if (circleResult != null) {
        val centerScreen = spatialEngine.projectWorldToScreen(circleResult.center, width, height) ?: return

        // Compute perimeter points in 3D and project to 2D
        val numSegments = 32
        val circlePath = Path()
        var firstPoint: Offset? = null

        val u = circleResult.normal.cross(Vector3.UP).normalize().let {
            if (it.length() < 0.1f) circleResult.normal.cross(Vector3.RIGHT).normalize() else it
        }
        val v = circleResult.normal.cross(u).normalize()

        for (i in 0 until numSegments) {
            val theta = (2.0 * PI * i / numSegments).toFloat()
            val rimPoint3D = circleResult.center + (u * (circleResult.radius * cos(theta))) + (v * (circleResult.radius * sin(theta)))
            val rimScreen = spatialEngine.projectWorldToScreen(rimPoint3D, width, height) ?: continue

            if (firstPoint == null) {
                firstPoint = rimScreen
                circlePath.moveTo(rimScreen.x, rimScreen.y)
            } else {
                circlePath.lineTo(rimScreen.x, rimScreen.y)
            }
        }

        firstPoint?.let { circlePath.close() }

        // Draw translucent circle disk & boundary line
        drawPath(circlePath, color = CyanReticle.copy(alpha = 0.15f), style = Fill)
        drawPath(circlePath, color = CyanReticle, style = Stroke(width = 3.5f))

        // Center marker
        drawCircle(color = NeonYellow, radius = 6f, center = centerScreen)

        // Draw Diameter badge
        val diamText = "Ø " + UnitFormatter.formatDistance(circleResult.diameter, unitSystem)
        drawMetricBadge(
            center = Offset(centerScreen.x, centerScreen.y - 30f),
            text = diamText,
            color = CyanReticle,
            textMeasurer = textMeasurer
        )

        // If recess depth was measured
        if (recessDepth != null) {
            val depthText = "Depth: " + UnitFormatter.formatDistance(recessDepth, unitSystem)
            drawMetricBadge(
                center = Offset(centerScreen.x, centerScreen.y + 30f),
                text = depthText,
                color = LaserGreen,
                textMeasurer = textMeasurer
            )
        }
    }
}

private fun DrawScope.drawPolygonGeometry(
    projectedPoints: List<Pair<Vector3, Offset>>,
    liveAimScreen: Offset,
    liveAimPoint: Vector3,
    unitSystem: UnitSystem,
    textMeasurer: TextMeasurer
) {
    if (projectedPoints.isEmpty()) return

    val path = Path()
    projectedPoints.forEachIndexed { i, (_, screenPos) ->
        if (i == 0) path.moveTo(screenPos.x, screenPos.y) else path.lineTo(screenPos.x, screenPos.y)
    }

    if (projectedPoints.size >= 3) {
        path.close()
        drawPath(path, color = LaserGreen.copy(alpha = 0.2f), style = Fill)
        drawPath(path, color = LaserGreen, style = Stroke(width = 3.5f))

        // Find polygon centroid
        var avgX = 0f
        var avgY = 0f
        projectedPoints.forEach { (_, screenPos) ->
            avgX += screenPos.x
            avgY += screenPos.y
        }
        val centroid = Offset(avgX / projectedPoints.size, avgY / projectedPoints.size)

        val area = com.example.engine.MeasurementEngine.calculatePolygonArea(projectedPoints.map { it.first })
        drawMetricBadge(
            center = centroid,
            text = UnitFormatter.formatArea(area, unitSystem),
            color = LaserGreen,
            textMeasurer = textMeasurer
        )
    } else {
        // Draw open path with dotted connection to live aim
        drawPath(path, color = CyanReticle, style = Stroke(width = 3f))
        val last = projectedPoints.last().second
        drawLine(
            color = CyanReticle,
            start = last,
            end = liveAimScreen,
            strokeWidth = 2.5f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
        )
    }
}

private fun DrawScope.drawCuboidGeometry(
    projectedPoints: List<Pair<Vector3, Offset>>,
    cuboidResult: CuboidResult?,
    spatialEngine: ARSpatialEngine,
    width: Float,
    height: Float,
    unitSystem: UnitSystem,
    textMeasurer: TextMeasurer
) {
    if (cuboidResult != null && cuboidResult.cornerVertices.size == 8) {
        val screenCorners = cuboidResult.cornerVertices.map { spatialEngine.projectWorldToScreen(it, width, height) }
        val validIndices = screenCorners.indices.filter { screenCorners[it] != null }

        // Edges of cuboid:
        // Base: 0-1, 1-2, 2-3, 3-0
        // Top: 4-5, 5-6, 6-7, 7-4
        // Pillars: 0-4, 1-5, 2-6, 3-7
        val edges = listOf(
            0 to 1, 1 to 2, 2 to 3, 3 to 0,
            4 to 5, 5 to 6, 6 to 7, 7 to 4,
            0 to 4, 1 to 5, 2 to 6, 3 to 7
        )

        for ((i1, i2) in edges) {
            val p1 = screenCorners[i1]
            val p2 = screenCorners[i2]
            if (p1 != null && p2 != null) {
                drawLine(
                    color = NeonYellow,
                    start = p1,
                    end = p2,
                    strokeWidth = 3.5f
                )
            }
        }

        // Draw volume badge at centroid of top face
        val p4 = screenCorners[4]
        val p6 = screenCorners[6]
        if (p4 != null && p6 != null) {
            val topCenter = Offset((p4.x + p6.x) / 2f, (p4.y + p6.y) / 2f)
            val volText = "Vol: " + UnitFormatter.formatVolume(cuboidResult.volume, unitSystem)
            drawMetricBadge(
                center = topCenter,
                text = volText,
                color = NeonYellow,
                textMeasurer = textMeasurer
            )
        }
    }
}

private fun DrawScope.drawSpiritLevel(
    center: Offset,
    spatialState: SpatialState,
    textMeasurer: TextMeasurer
) {
    val outerRadius = 110f
    val bubbleRadius = 24f

    // Outer level ring
    drawCircle(
        color = Color(0xFF1C2541),
        radius = outerRadius,
        center = center,
        style = Fill
    )
    drawCircle(
        color = if (spatialState.isLevel) LaserGreen else CyanReticle,
        radius = outerRadius,
        center = center,
        style = Stroke(width = 3f)
    )

    // Inner target zone (1 degree target)
    drawCircle(
        color = (if (spatialState.isLevel) LaserGreen else CyanReticle).copy(alpha = 0.25f),
        radius = 28f,
        center = center,
        style = Fill
    )
    drawCircle(
        color = if (spatialState.isLevel) LaserGreen else CyanReticle,
        radius = 28f,
        center = center,
        style = Stroke(width = 1.5f)
    )

    // Crosshairs
    drawLine(Color.White.copy(alpha = 0.4f), Offset(center.x - outerRadius, center.y), Offset(center.x + outerRadius, center.y), 1.5f)
    drawLine(Color.White.copy(alpha = 0.4f), Offset(center.x, center.y - outerRadius), Offset(center.x, center.y + outerRadius), 1.5f)

    // Bubble position derived from roll and pitch
    // clamp to outer ring
    val sensitivity = 5f
    val bubbleOffsetX = (spatialState.rollDegrees * sensitivity).coerceIn(-outerRadius + bubbleRadius, outerRadius - bubbleRadius)
    val bubbleOffsetY = (-spatialState.pitchDegrees * sensitivity).coerceIn(-outerRadius + bubbleRadius, outerRadius - bubbleRadius)
    val bubbleCenter = Offset(center.x + bubbleOffsetX, center.y + bubbleOffsetY)

    drawCircle(
        color = if (spatialState.isLevel) LaserGreen else NeonYellow,
        radius = bubbleRadius,
        center = bubbleCenter,
        style = Fill
    )
    drawCircle(
        color = Color.White,
        radius = bubbleRadius,
        center = bubbleCenter,
        style = Stroke(width = 2f)
    )

    // Degree readout below ring
    val degText = String.format("%.1f° Incline", spatialState.surfaceInclinationDeg)
    drawMetricBadge(
        center = Offset(center.x, center.y + outerRadius + 30f),
        text = degText,
        color = if (spatialState.isLevel) LaserGreen else NeonYellow,
        textMeasurer = textMeasurer
    )
}

private fun DrawScope.drawPin(
    pos: Offset,
    label: String,
    color: Color,
    textMeasurer: TextMeasurer
) {
    // Glow effect
    drawCircle(color.copy(alpha = 0.3f), radius = 18f, center = pos)
    drawCircle(color, radius = 9f, center = pos)
    drawCircle(Color.White, radius = 4f, center = pos)

    // Small label tag
    val layout = textMeasurer.measure(
        text = label,
        style = TextStyle(color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    )
    val badgeTop = pos.y - 28f
    val badgeLeft = pos.x - (layout.size.width / 2f)

    drawRoundRect(
        color = Color(0xCC0B132B),
        topLeft = Offset(badgeLeft - 6f, badgeTop - 4f),
        size = Size(layout.size.width + 12f, layout.size.height + 8f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
    )

    drawText(
        textMeasurer = textMeasurer,
        text = label,
        topLeft = Offset(badgeLeft, badgeTop),
        style = TextStyle(color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    )
}

private fun DrawScope.drawMetricBadge(
    center: Offset,
    text: String,
    color: Color,
    textMeasurer: TextMeasurer
) {
    val layout = textMeasurer.measure(
        text = text,
        style = TextStyle(color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
    )
    val width = layout.size.width + 20f
    val height = layout.size.height + 12f
    val topLeft = Offset(center.x - width / 2f, center.y - height / 2f)

    // Background pill with colored border
    drawRoundRect(
        color = Color(0xE60B132B),
        topLeft = topLeft,
        size = Size(width, height),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
    )
    drawRoundRect(
        color = color,
        topLeft = topLeft,
        size = Size(width, height),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f),
        style = Stroke(width = 2f)
    )

    drawText(
        textMeasurer = textMeasurer,
        text = text,
        topLeft = Offset(topLeft.x + 10f, topLeft.y + 6f),
        style = TextStyle(color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
    )
}
