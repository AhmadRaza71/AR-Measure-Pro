package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AROverlayCanvas
import com.example.ui.components.ActionControlsRow
import com.example.ui.components.CameraView
import com.example.ui.components.HelpManualDialog
import com.example.ui.components.HistoryLogSheet
import com.example.ui.components.MeasurementHUDCard
import com.example.ui.components.ModeSelectorBar
import com.example.ui.components.SaveMeasurementDialog
import com.example.viewmodel.ARMeasureViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ARMeasureMainScreen(
    viewModel: ARMeasureViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val spatialState by viewModel.spatialEngine.spatialState.collectAsState()
    val historyList by viewModel.historyList.collectAsState()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFF070B19)
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val viewWidth = constraints.maxWidth.toFloat()
            val viewHeight = constraints.maxHeight.toFloat()

            // 1. Camera & Raycast Viewport layer with touch gesture detection
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { offset ->
                                // Raycast screen tap into 3D world coordinate
                                val worldPoint = viewModel.spatialEngine.raycastScreenToWorld(
                                    screenX = offset.x,
                                    screenY = offset.y,
                                    viewWidth = viewWidth,
                                    viewHeight = viewHeight
                                )
                                viewModel.updateAimPoint(worldPoint)
                                viewModel.addPointAtReticle()
                            }
                        )
                    }
            ) {
                // Live camera feed
                CameraView(
                    isTorchOn = uiState.isTorchOn,
                    modifier = Modifier.fillMaxSize()
                )

                // High-precision AR Geometric Canvas Overlay
                AROverlayCanvas(
                    spatialEngine = viewModel.spatialEngine,
                    spatialState = spatialState,
                    currentMode = uiState.currentMode,
                    unitSystem = uiState.unitSystem,
                    placedPoints = uiState.placedPoints,
                    liveAimPoint = uiState.liveAimPoint,
                    circleResult = uiState.circleResult,
                    recessDepth = uiState.recessDepth,
                    cuboidResult = uiState.cuboidResult,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // 2. Aesthetic Dark HUD Overlay Controls
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Navigation Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xD90B132B))
                        .border(1.dp, Color(0x6600F0FF), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // App Branding with glowing indicator
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00FF9D))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ARMEASURE",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = " PRO",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF00F0FF),
                            fontWeight = FontWeight.Black
                        )
                    }

                    // Surface Tracking Status Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF16213E))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (spatialState.isLevel) "PLANE LOCKED" else "TRACKING",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (spatialState.isLevel) Color(0xFF00FF9D) else Color(0xFFFFB703),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Top Action Icons: Torch, Help, History Log
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Flashlight / Torch
                        IconButton(
                            onClick = { viewModel.toggleTorch() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (uiState.isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashlightOff,
                                contentDescription = "Toggle Torch",
                                tint = if (uiState.isTorchOn) Color(0xFFFFB703) else Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Help & Math formulas
                        IconButton(
                            onClick = { viewModel.toggleHelpDialog(true) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.HelpOutline,
                                contentDescription = "Math Blueprint",
                                tint = Color(0xFF00F0FF),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Measurement History review button
                        IconButton(
                            onClick = { viewModel.openHistorySheet() },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("main_menu_history_button")
                        ) {
                            BadgedBox(
                                badge = {
                                    if (historyList.isNotEmpty()) {
                                        Badge(
                                            containerColor = Color(0xFF00FF9D),
                                            contentColor = Color(0xFF0B132B)
                                        ) {
                                            Text("${historyList.size}", fontSize = 10.sp)
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = "History Log",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // Measurement HUD Card (Real-time value, secondary specs, guidance)
                MeasurementHUDCard(
                    currentMode = uiState.currentMode,
                    unitSystem = uiState.unitSystem,
                    instructionText = uiState.instructionText,
                    spatialState = spatialState,
                    calculatedDistance = uiState.calculatedDistance,
                    calculatedAngle = uiState.calculatedAngle,
                    circleResult = uiState.circleResult,
                    recessDepth = uiState.recessDepth,
                    calculatedArea = uiState.calculatedArea,
                    calculatedPerimeter = uiState.calculatedPerimeter,
                    cuboidResult = uiState.cuboidResult,
                    onToggleUnit = { viewModel.toggleUnitSystem() }
                )

                Spacer(modifier = Modifier.weight(1f))

                // Bottom Dock: Mode Selector & Tactical Action Controls
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xE60B132B),
                                    Color(0xF5070B19)
                                )
                            )
                        )
                        .border(
                            1.dp,
                            Color(0x4400F0FF),
                            RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                        )
                        .padding(top = 8.dp)
                        .navigationBarsPadding()
                ) {
                    // Mode Selector Bar (Distance, Angle, Circle, Area, Volume, Spirit Level)
                    ModeSelectorBar(
                        currentMode = uiState.currentMode,
                        onSelectMode = { mode -> viewModel.setMode(mode) }
                    )

                    // Action controls row (Reset, Undo, + Add Pin, Volume sliders, Save)
                    ActionControlsRow(
                        currentMode = uiState.currentMode,
                        canSave = uiState.canSave,
                        placedPointsCount = uiState.placedPoints.size,
                        manualLength = uiState.manualCuboidLength,
                        manualWidth = uiState.manualCuboidWidth,
                        manualHeight = uiState.manualCuboidHeight,
                        onAddPoint = { viewModel.addPointAtReticle() },
                        onUndoPoint = { viewModel.undoLastPoint() },
                        onReset = { viewModel.resetMeasurement() },
                        onSave = { viewModel.openSaveDialog() },
                        onRansacScan = { viewModel.performRansacScanOnReticle() },
                        onUpdateCuboidDimensions = { l, w, h ->
                            viewModel.updateManualCuboidDimensions(l, w, h)
                        }
                    )
                }
            }
        }
    }

    // Modal Save Dialog
    if (uiState.showSaveDialog) {
        SaveMeasurementDialog(
            currentMode = uiState.currentMode,
            customLabel = uiState.customLabelInput,
            onLabelChange = { viewModel.updateCustomLabel(it) },
            onConfirmSave = { viewModel.saveCurrentMeasurement() },
            onDismiss = { viewModel.closeSaveDialog() }
        )
    }

    // Modal History Log Sheet
    if (uiState.showHistorySheet) {
        HistoryLogSheet(
            sheetState = sheetState,
            historyList = historyList,
            onDeleteRecord = { record -> viewModel.deleteHistoryRecord(record) },
            onClearAll = { viewModel.clearAllHistory() },
            onDismiss = { viewModel.closeHistorySheet() }
        )
    }

    // Help & Math Formulas Blueprint Dialog
    if (uiState.showHelpDialog) {
        HelpManualDialog(
            onDismiss = { viewModel.toggleHelpDialog(false) }
        )
    }
}
