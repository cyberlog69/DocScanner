package com.example.docscanner.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.example.docscanner.service.SignaturePlacement
import com.example.docscanner.service.SignatureService
import com.example.docscanner.service.TargetPageOption
import java.io.ByteArrayOutputStream

data class SignatureStroke(
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Float
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SignaturePadDialog(
    currentPageNumber: Int,
    totalPages: Int,
    onDismiss: () -> Unit,
    onStampPdf: (signatureBytes: ByteArray, targetPages: List<Int>, placement: SignaturePlacement) -> Unit,
    onSharePng: (signatureBytes: ByteArray) -> Unit
) {
    val context = LocalContext.current
    val strokes = remember { mutableStateListOf<SignatureStroke>() }
    var currentStrokePoints by remember { mutableStateOf<List<Offset>>(emptyList()) }

    val inkColors = listOf(
        Color(0xFF1D4ED8), // Signature Blue
        Color(0xFF111827), // Classic Dark Black
        Color(0xFFDC2626)  // Legal Red
    )
    var selectedColor by remember { mutableStateOf(inkColors[0]) }
    var strokeWidth by remember { mutableFloatStateOf(6f) }
    var saveForFuture by remember { mutableStateOf(true) }

    var selectedPlacement by remember { mutableStateOf(SignaturePlacement.BOTTOM_RIGHT) }
    var selectedTargetOption by remember { mutableStateOf(TargetPageOption.CURRENT_PAGE) }

    // Check if a saved signature already exists
    val hasSavedSignature = remember { SignatureService.getSavedSignatureFile(context) != null }
    var useSavedSignature by remember { mutableStateOf(false) }
    var loadedSavedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .padding(vertical = 16.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Electronic Signature 🖊️",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (hasSavedSignature) {
                    TextButton(onClick = {
                        useSavedSignature = !useSavedSignature
                        if (useSavedSignature && loadedSavedBitmap == null) {
                            loadedSavedBitmap = SignatureService.loadSavedSignatureBitmap(context)
                        }
                    }) {
                        Text(if (useSavedSignature) "Draw New" else "Use Saved")
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (useSavedSignature && loadedSavedBitmap != null) {
                    // Show preview of saved signature
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
                        color = Color(0xFFFAFAFA)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Using Saved Signature",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(8.dp)
                            )
                            // Draw loaded bitmap
                            Canvas(modifier = Modifier.size(200.dp, 120.dp)) {
                                val bmp = loadedSavedBitmap ?: return@Canvas
                                drawImage(
                                    image = bmp.asImageBitmap(),
                                    dstSize = androidx.compose.ui.unit.IntSize(size.width.toInt(), size.height.toInt())
                                )
                            }
                        }
                    }
                } else {
                    // Drawing Canvas Pad
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFCFCFC))
                            .border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    ) {
                        Canvas(
                            modifier = Modifier
                                .matchParentSize()
                                .pointerInput(selectedColor, strokeWidth) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            currentStrokePoints = listOf(offset)
                                        },
                                        onDrag = { change, _ ->
                                            currentStrokePoints = currentStrokePoints + change.position
                                        },
                                        onDragEnd = {
                                            if (currentStrokePoints.isNotEmpty()) {
                                                strokes.add(
                                                    SignatureStroke(
                                                        points = currentStrokePoints,
                                                        color = selectedColor,
                                                        strokeWidth = strokeWidth
                                                    )
                                                )
                                                currentStrokePoints = emptyList()
                                            }
                                        }
                                    )
                                }
                        ) {
                            // Baseline watermark guide line
                            drawLine(
                                color = Color.LightGray.copy(alpha = 0.6f),
                                start = Offset(20f, size.height * 0.78f),
                                end = Offset(size.width - 20f, size.height * 0.78f),
                                strokeWidth = 1.5f
                            )

                            // Render completed strokes
                            for (stroke in strokes) {
                                if (stroke.points.size < 2) continue
                                val path = createSmoothPath(stroke.points)
                                drawPath(
                                    path = path,
                                    color = stroke.color,
                                    style = Stroke(
                                        width = stroke.strokeWidth,
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                            }

                            // Render stroke currently in flight
                            if (currentStrokePoints.size >= 2) {
                                val currentPath = createSmoothPath(currentStrokePoints)
                                drawPath(
                                    path = currentPath,
                                    color = selectedColor,
                                    style = Stroke(
                                        width = strokeWidth,
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                            }
                        }

                        // Pad Action Buttons (Clear, Undo)
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    if (strokes.isNotEmpty()) strokes.removeAt(strokes.lastIndex)
                                },
                                enabled = strokes.isNotEmpty()
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Undo, "Undo")
                            }
                            IconButton(
                                onClick = {
                                    strokes.clear()
                                    currentStrokePoints = emptyList()
                                },
                                enabled = strokes.isNotEmpty()
                            ) {
                                Icon(Icons.Default.Clear, "Clear")
                            }
                        }

                        if (strokes.isEmpty() && currentStrokePoints.isEmpty()) {
                            Text(
                                text = "Sign here with finger or stylus ✍️",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .align(Alignment.Center)
                            )
                        }
                    }

                    // Ink Color & Stroke Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            inkColors.forEach { color ->
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (selectedColor == color) 2.5.dp else 0.dp,
                                            color = if (selectedColor == color) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { selectedColor = color },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selectedColor == color) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Stroke Width Selector
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(4f to "Fine", 6f to "Medium", 9f to "Bold").forEach { (width, label) ->
                                FilterChip(
                                    selected = strokeWidth == width,
                                    onClick = { strokeWidth = width },
                                    label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }

                    // Save signature for future checkbox
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = saveForFuture,
                            onCheckedChange = { saveForFuture = it }
                        )
                        Text(
                            text = "Save signature for 1-tap reuse",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                // Page Target Option
                Text(
                    text = "Target Page",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TargetPageOption.entries.forEach { option ->
                        val label = when (option) {
                            TargetPageOption.CURRENT_PAGE -> "Page ${currentPageNumber + 1}"
                            TargetPageOption.LAST_PAGE -> "Last Page ($totalPages)"
                            TargetPageOption.FIRST_PAGE -> "First Page (1)"
                            TargetPageOption.ALL_PAGES -> "All Pages"
                        }
                        FilterChip(
                            selected = selectedTargetOption == option,
                            onClick = { selectedTargetOption = option },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                // Placement Option
                Text(
                    text = "Placement",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SignaturePlacement.entries.forEach { placement ->
                        FilterChip(
                            selected = selectedPlacement == placement,
                            onClick = { selectedPlacement = placement },
                            label = { Text(placement.displayName, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            val hasContent = (useSavedSignature && loadedSavedBitmap != null) || strokes.isNotEmpty()
            Button(
                onClick = {
                    val signatureBitmap = if (useSavedSignature && loadedSavedBitmap != null) {
                        loadedSavedBitmap!!
                    } else {
                        renderStrokesToBitmap(strokes)
                    }

                    if (signatureBitmap != null) {
                        if (saveForFuture && !useSavedSignature) {
                            SignatureService.saveSignatureToDisk(context, signatureBitmap)
                        }

                        val bytes = bitmapToPngBytes(signatureBitmap)
                        val targetPages = resolveTargetPages(selectedTargetOption, currentPageNumber, totalPages)
                        onStampPdf(bytes, targetPages, selectedPlacement)
                    }
                },
                enabled = hasContent
            ) {
                Text("Sign & Apply to PDF")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val hasContent = (useSavedSignature && loadedSavedBitmap != null) || strokes.isNotEmpty()
                if (hasContent) {
                    OutlinedIconButton(
                        onClick = {
                            val bmp = if (useSavedSignature && loadedSavedBitmap != null) {
                                loadedSavedBitmap!!
                            } else {
                                renderStrokesToBitmap(strokes)
                            }
                            if (bmp != null) {
                                onSharePng(bitmapToPngBytes(bmp))
                            }
                        }
                    ) {
                        Icon(Icons.Default.Share, "Share PNG")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

private fun createSmoothPath(points: List<Offset>): Path {
    val path = Path()
    if (points.isEmpty()) return path
    path.moveTo(points[0].x, points[0].y)
    if (points.size == 1) {
        path.lineTo(points[0].x + 0.5f, points[0].y + 0.5f)
        return path
    }
    for (i in 1 until points.size) {
        val prev = points[i - 1]
        val curr = points[i]
        val midX = (prev.x + curr.x) / 2f
        val midY = (prev.y + curr.y) / 2f
        path.quadraticTo(prev.x, prev.y, midX, midY)
    }
    path.lineTo(points.last().x, points.last().y)
    return path
}

private fun renderStrokesToBitmap(strokes: List<SignatureStroke>): Bitmap? {
    if (strokes.isEmpty()) return null

    // Compute bounding box of all strokes
    var minX = Float.MAX_VALUE
    var minY = Float.MAX_VALUE
    var maxX = Float.MIN_VALUE
    var maxY = Float.MIN_VALUE

    for (stroke in strokes) {
        for (pt in stroke.points) {
            if (pt.x < minX) minX = pt.x
            if (pt.y < minY) minY = pt.y
            if (pt.x > maxX) maxX = pt.x
            if (pt.y > maxY) maxY = pt.y
        }
    }

    val padding = 20f
    minX = (minX - padding).coerceAtLeast(0f)
    minY = (minY - padding).coerceAtLeast(0f)
    maxX += padding
    maxY += padding

    val width = (maxX - minX).toInt().coerceAtLeast(100)
    val height = (maxY - minY).toInt().coerceAtLeast(50)

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)

    for (stroke in strokes) {
        if (stroke.points.size < 2) continue
        val paint = Paint().apply {
            color = stroke.color.toArgb()
            strokeWidth = stroke.strokeWidth
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        val shiftedPoints = stroke.points.map { Offset(it.x - minX, it.y - minY) }
        val androidPath = createSmoothPath(shiftedPoints).asAndroidPath()
        canvas.drawPath(androidPath, paint)
    }

    return bitmap
}

private fun bitmapToPngBytes(bitmap: Bitmap): ByteArray {
    val stream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
    return stream.toByteArray()
}

private fun resolveTargetPages(option: TargetPageOption, currentPage: Int, totalPages: Int): List<Int> {
    return when (option) {
        TargetPageOption.CURRENT_PAGE -> listOf(currentPage + 1)
        TargetPageOption.FIRST_PAGE -> listOf(1)
        TargetPageOption.LAST_PAGE -> listOf(totalPages.coerceAtLeast(1))
        TargetPageOption.ALL_PAGES -> (1..totalPages.coerceAtLeast(1)).toList()
    }
}
