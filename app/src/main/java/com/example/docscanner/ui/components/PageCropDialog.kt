package com.example.docscanner.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.docscanner.data.model.Page
import com.example.docscanner.model.PointF
import com.example.docscanner.model.Quadrilateral
import java.io.File
import kotlin.math.hypot

/**
 * Interactive quadrilateral crop dialog allowing users to adjust document boundary
 * corners (TL, TR, BR, BL) on any scanned page without re-scanning.
 */
@Composable
fun PageCropDialog(
    page: Page,
    imageVersion: Long,
    isCropping: Boolean = false,
    onDismiss: () -> Unit,
    onApplyCrop: (Quadrilateral) -> Unit
) {
    val context = LocalContext.current

    // Corner positions in normalized [0..1] coordinates
    var topLeft by remember { mutableStateOf(PointF(0.05f, 0.05f)) }
    var topRight by remember { mutableStateOf(PointF(0.95f, 0.05f)) }
    var bottomRight by remember { mutableStateOf(PointF(0.95f, 0.95f)) }
    var bottomLeft by remember { mutableStateOf(PointF(0.05f, 0.95f)) }

    // Currently dragged corner: 0=TL, 1=TR, 2=BR, 3=BL, -1=none
    var draggedCorner by remember { mutableIntStateOf(-1) }

    AlertDialog(
        onDismissRequest = { if (!isCropping) onDismiss() },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("✂️", style = MaterialTheme.typography.titleMedium)
                Column {
                    Text(
                        text = "Re-crop Page ${page.pageIndex + 1}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Drag the 4 corner pins to adjust edges",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Interactive Crop Canvas area
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(3f / 4f)
                        .clip(RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val canvasWidth = constraints.maxWidth.toFloat()
                        val canvasHeight = constraints.maxHeight.toFloat()
                        val handleRadiusPx = 28f

                        // Page background image
                        val sourcePath = if (page.originalImagePath.isNotBlank() && File(page.originalImagePath).exists()) {
                            page.originalImagePath
                        } else {
                            page.imagePath
                        }

                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(File(sourcePath))
                                .memoryCacheKey("${sourcePath}_$imageVersion")
                                .diskCacheKey("${sourcePath}_$imageVersion")
                                .build(),
                            contentDescription = "Page to crop",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.FillBounds
                        )

                        // Interactive Canvas overlay with polygon lines and handles
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(canvasWidth, canvasHeight) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            val tlPx = Offset(topLeft.x * canvasWidth, topLeft.y * canvasHeight)
                                            val trPx = Offset(topRight.x * canvasWidth, topRight.y * canvasHeight)
                                            val brPx = Offset(bottomRight.x * canvasWidth, bottomRight.y * canvasHeight)
                                            val blPx = Offset(bottomLeft.x * canvasWidth, bottomLeft.y * canvasHeight)

                                            val hitDist = 70f
                                            draggedCorner = when {
                                                hypot(offset.x - tlPx.x, offset.y - tlPx.y) < hitDist -> 0
                                                hypot(offset.x - trPx.x, offset.y - trPx.y) < hitDist -> 1
                                                hypot(offset.x - brPx.x, offset.y - brPx.y) < hitDist -> 2
                                                hypot(offset.x - blPx.x, offset.y - blPx.y) < hitDist -> 3
                                                else -> -1
                                            }
                                        },
                                        onDragEnd = { draggedCorner = -1 },
                                        onDragCancel = { draggedCorner = -1 },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            if (draggedCorner != -1) {
                                                val dxNorm = dragAmount.x / canvasWidth
                                                val dyNorm = dragAmount.y / canvasHeight
                                                when (draggedCorner) {
                                                    0 -> topLeft = PointF(
                                                        (topLeft.x + dxNorm).coerceIn(0f, topRight.x - 0.05f),
                                                        (topLeft.y + dyNorm).coerceIn(0f, bottomLeft.y - 0.05f)
                                                    )
                                                    1 -> topRight = PointF(
                                                        (topRight.x + dxNorm).coerceIn(topLeft.x + 0.05f, 1f),
                                                        (topRight.y + dyNorm).coerceIn(0f, bottomRight.y - 0.05f)
                                                    )
                                                    2 -> bottomRight = PointF(
                                                        (bottomRight.x + dxNorm).coerceIn(bottomLeft.x + 0.05f, 1f),
                                                        (bottomRight.y + dyNorm).coerceIn(topRight.y + 0.05f, 1f)
                                                    )
                                                    3 -> bottomLeft = PointF(
                                                        (bottomLeft.x + dxNorm).coerceIn(0f, bottomRight.x - 0.05f),
                                                        (bottomLeft.y + dyNorm).coerceIn(topLeft.y + 0.05f, 1f)
                                                    )
                                                }
                                            }
                                        }
                                    )
                                }
                        ) {
                            val tlPx = Offset(topLeft.x * size.width, topLeft.y * size.height)
                            val trPx = Offset(topRight.x * size.width, topRight.y * size.height)
                            val brPx = Offset(bottomRight.x * size.width, bottomRight.y * size.height)
                            val blPx = Offset(bottomLeft.x * size.width, bottomLeft.y * size.height)

                            // Polygon path
                            val polyPath = Path().apply {
                                moveTo(tlPx.x, tlPx.y)
                                lineTo(trPx.x, trPx.y)
                                lineTo(brPx.x, brPx.y)
                                lineTo(blPx.x, blPx.y)
                                close()
                            }

                            // Inner translucent fill
                            drawPath(
                                path = polyPath,
                                color = Color(0x334285F4)
                            )

                            // Polygon boundary lines
                            drawPath(
                                path = polyPath,
                                color = Color(0xFF1E88E5),
                                style = Stroke(
                                    width = 4.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 8f), 0f)
                                )
                            )

                            // Corner circles
                            val corners = listOf(tlPx, trPx, brPx, blPx)
                            corners.forEachIndexed { idx, pt ->
                                val isSelected = draggedCorner == idx
                                // Outer glow/halo
                                drawCircle(
                                    color = if (isSelected) Color(0x80FFB300) else Color(0x401E88E5),
                                    radius = if (isSelected) handleRadiusPx * 1.5f else handleRadiusPx * 1.2f,
                                    center = pt
                                )
                                // Solid center handle
                                drawCircle(
                                    color = if (isSelected) Color(0xFFFFB300) else Color(0xFF1976D2),
                                    radius = handleRadiusPx,
                                    center = pt
                                )
                                drawCircle(
                                    color = Color.White,
                                    radius = handleRadiusPx * 0.45f,
                                    center = pt
                                )
                            }
                        }

                        if (isCropping) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Color.White)
                            }
                        }
                    }
                }

                // Preset corner adjustment actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        modifier = Modifier.weight(1f),
                        enabled = !isCropping,
                        onClick = {
                            topLeft = PointF(0f, 0f)
                            topRight = PointF(1f, 0f)
                            bottomRight = PointF(1f, 1f)
                            bottomLeft = PointF(0f, 1f)
                        }
                    ) {
                        Text("Full (100%)", style = MaterialTheme.typography.labelSmall)
                    }

                    FilledTonalButton(
                        modifier = Modifier.weight(1f),
                        enabled = !isCropping,
                        onClick = {
                            val q = Quadrilateral.inset(0.05f)
                            topLeft = q.topLeft
                            topRight = q.topRight
                            bottomRight = q.bottomRight
                            bottomLeft = q.bottomLeft
                        }
                    ) {
                        Text("Inset 5%", style = MaterialTheme.typography.labelSmall)
                    }

                    FilledTonalButton(
                        modifier = Modifier.weight(1f),
                        enabled = !isCropping,
                        onClick = {
                            val q = Quadrilateral.inset(0.10f)
                            topLeft = q.topLeft
                            topRight = q.topRight
                            bottomRight = q.bottomRight
                            bottomLeft = q.bottomLeft
                        }
                    ) {
                        Text("Inset 10%", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isCropping,
                onClick = {
                    val quad = Quadrilateral(
                        topLeft = topLeft,
                        topRight = topRight,
                        bottomRight = bottomRight,
                        bottomLeft = bottomLeft
                    )
                    onApplyCrop(quad)
                }
            ) {
                if (isCropping) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Cropping...")
                } else {
                    Icon(Icons.Default.Crop, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Apply Crop")
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                enabled = !isCropping,
                onClick = onDismiss
            ) {
                Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Cancel")
            }
        }
    )
}
