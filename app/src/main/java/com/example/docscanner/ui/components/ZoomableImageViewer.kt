package com.example.docscanner.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import java.io.File

/**
 * Interactive fullscreen pinch-to-zoom and pan lightbox for UHD-scanned document pages.
 * Supports:
 * - Pinch-to-zoom (1.0x to 5.0x magnification)
 * - Double-tap to quickly toggle between 1.0x and 2.5x
 * - Smooth drag / pan gesture navigation
 * - Page subtitle and dismiss controls
 */
@Composable
fun ZoomableImageDialog(
    imagePath: String,
    title: String = "Page Preview",
    pageIndex: Int = 0,
    pageCount: Int = 1,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            var scale by remember { mutableFloatStateOf(1f) }
            var offset by remember { mutableStateOf(Offset.Zero) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (scale > 1.2f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                } else {
                                    scale = 2.5f
                                }
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            if (scale > 1f) {
                                val maxOffsetX = (size.width * (scale - 1)) / 2
                                val maxOffsetY = (size.height * (scale - 1)) / 2
                                offset = Offset(
                                    x = (offset.x + pan.x).coerceIn(-maxOffsetX, maxOffsetX),
                                    y = (offset.y + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                                )
                            } else {
                                offset = Offset.Zero
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = File(imagePath),
                    contentDescription = "Zoomable Page Image",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        )
                )

                // Top bar overlay with close button and page indicator
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.TopStart
                ) {
                    FilledIconButton(
                        onClick = onDismiss,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color.Black.copy(alpha = 0.65f),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close zoom viewer")
                    }

                    Surface(
                        modifier = Modifier.align(Alignment.TopCenter),
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.65f)
                    ) {
                        Text(
                            text = if (pageCount > 1) "Page ${pageIndex + 1} of $pageCount" else title,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }

                    // Zoom level indicator
                    if (scale > 1.05f) {
                        Surface(
                            modifier = Modifier.align(Alignment.BottomCenter),
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.65f)
                        ) {
                            Text(
                                text = "%.1fx".format(scale),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
