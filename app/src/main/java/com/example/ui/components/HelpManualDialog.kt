package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HelpManualDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F1A30),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Functions,
                    contentDescription = null,
                    tint = Color(0xFF00F0FF)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AR Mathematical Blueprint",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                MathSection(
                    title = "1. Point-to-Point Distance",
                    formula = "d = √[(x₂ - x₁)² + (y₂ - y₁)² + (z₂ - z₁)²]",
                    description = "Screen touches cast rays into 3D world space. Euclidean metric computes precise straight-line or chain-tape distance in meters."
                )

                MathSection(
                    title = "2. Three-Point Vertex Angle",
                    formula = "θ = arccos[ (VA · VB) / (|VA| |VB|) ]",
                    description = "Requires placing Point A, Vertex V, and Point B. Computes normalized dot product between vectors VA and VB to derive exact degrees."
                )

                MathSection(
                    title = "3. RANSAC Circle & Recess Depth",
                    formula = "Ø = 2R  |  Depth = |(P_bottom - P_rim) · n̂|",
                    description = "RANSAC isolates point cloud outliers to calculate center (xc, yc, zc) and diameter. Depth calculates the perpendicular offset from the fitted rim plane."
                )

                MathSection(
                    title = "4. Polygon Area (3D Shoelace)",
                    formula = "Area = 0.5 × |∑ (P_i × P_{i+1})|",
                    description = "Multi-point polygon vertices on a surface plane are evaluated with cross-product summation to yield enclosed area and perimeter."
                )

                MathSection(
                    title = "5. 3D Cuboid Volume",
                    formula = "V = L × W × H",
                    description = "Detects or defines rectangular bounds with AR points or sliders. Computes volume in cubic meters/feet and renders complete 3D wireframe box."
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00F0FF),
                    contentColor = Color(0xFF0B132B)
                )
            ) {
                Text("Understood")
            }
        }
    )
}

@Composable
private fun MathSection(title: String, formula: String, description: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF16213E))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFF00F0FF),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formula,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                ),
                color = Color(0xFFFFB703),
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 11.sp
            )
        }
    }
}
