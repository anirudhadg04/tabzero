package com.example.detoxproject.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.media.ExifInterface
import android.net.Uri
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.detoxproject.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * High-performance, memory-safe bitmap loader with EXIF orientation correction.
 */
suspend fun loadAndOrientBitmap(context: Context, uri: Uri, maxDimension: Int = 1600): Bitmap? {
    return withContext(Dispatchers.IO) {
        try {
            // 1. Measure dimensions
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            }

            val origW = boundsOptions.outWidth
            val origH = boundsOptions.outHeight
            if (origW <= 0 || origH <= 0) return@withContext null

            // 2. Compute sample size
            var sampleSize = 1
            while ((origW / sampleSize) > maxDimension || (origH / sampleSize) > maxDimension) {
                sampleSize *= 2
            }

            // 3. Decode scaled bitmap
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val sampledBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return@withContext null

            // 4. Handle EXIF orientation
            val exifOrientation = try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val exif = ExifInterface(stream)
                    exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                } ?: ExifInterface.ORIENTATION_NORMAL
            } catch (_: Exception) {
                ExifInterface.ORIENTATION_NORMAL
            }

            val matrix = Matrix()
            when (exifOrientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                else -> return@withContext sampledBitmap
            }

            val orientedBitmap = Bitmap.createBitmap(
                sampledBitmap, 0, 0,
                sampledBitmap.width, sampledBitmap.height,
                matrix, true
            )
            if (orientedBitmap != sampledBitmap) {
                sampledBitmap.recycle()
            }
            orientedBitmap
        } catch (_: Exception) {
            null
        }
    }
}

/**
 * Interactive Profile Picture Crop Dialog.
 * Allows panning, zooming, rotating, and circular cropping of gallery images.
 */
@Composable
fun ImageCropDialog(
    imageUri: Uri,
    onDismiss: () -> Unit,
    onCropConfirmed: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    var sourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(imageUri) {
        isLoading = true
        sourceBitmap = loadAndOrientBitmap(context, imageUri)
        isLoading = false
    }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var rotationDegrees by remember { mutableFloatStateOf(0f) }

    val cropBoxSizeDp = 270.dp
    val density = LocalDensity.current
    val cropBoxSizePx = with(density) { cropBoxSizeDp.toPx() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = OfflineSurface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Crop Profile Picture",
                            style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = OfflineTextPrimary
                        )
                        Text(
                            text = "Drag and zoom to position inside the circle",
                            style = Typography.labelSmall,
                            color = OfflineTextSecondary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                            tint = OfflineTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Viewport Box
                Box(
                    modifier = Modifier
                        .size(cropBoxSizeDp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.Black)
                        .clipToBounds()
                        .pointerInput(sourceBitmap) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(0.6f, 6.0f)
                                offset = Offset(offset.x + pan.x, offset.y + pan.y)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = OfflineAccentPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    } else if (sourceBitmap != null) {
                        val bmp = sourceBitmap!!
                        val fitScale = minOf(cropBoxSizePx / bmp.width, cropBoxSizePx / bmp.height)

                        androidx.compose.foundation.Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Crop Preview",
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = fitScale * scale,
                                    scaleY = fitScale * scale,
                                    translationX = offset.x,
                                    translationY = offset.y,
                                    rotationZ = rotationDegrees
                                )
                        )

                        // Circular viewfinder overlay (mask)
                        ComposeCanvas(modifier = Modifier.fillMaxSize()) {
                            val radius = size.minDimension / 2f
                            val centerOffset = Offset(size.width / 2f, size.height / 2f)

                            // Subtle guideline circle
                            drawCircle(
                                color = Color.White.copy(alpha = 0.85f),
                                radius = radius,
                                center = centerOffset,
                                style = Stroke(width = 2.5f.dp.toPx())
                            )
                        }
                    } else {
                        Text(
                            text = "Unable to load image",
                            style = Typography.bodySmall,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Interactive Zoom & Rotate controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Zoom out
                    IconButton(
                        onClick = { scale = (scale / 1.25f).coerceAtLeast(0.6f) },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(OfflineSurfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Zoom out",
                            tint = OfflineTextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Slider for fine precision
                    Slider(
                        value = scale,
                        onValueChange = { scale = it },
                        valueRange = 0.6f..5.0f,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = OfflineAccentPrimary,
                            activeTrackColor = OfflineAccentPrimary,
                            inactiveTrackColor = OfflineBorder
                        )
                    )

                    // Zoom in
                    IconButton(
                        onClick = { scale = (scale * 1.25f).coerceAtMost(6.0f) },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(OfflineSurfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Zoom in",
                            tint = OfflineTextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Rotate 90 degrees
                    IconButton(
                        onClick = { rotationDegrees = (rotationDegrees + 90f) % 360f },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(OfflineSurfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RotateRight,
                            contentDescription = "Rotate 90 degrees",
                            tint = OfflineTextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Reset
                    IconButton(
                        onClick = {
                            scale = 1f
                            offset = Offset.Zero
                            rotationDegrees = 0f
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(OfflineSurfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset",
                            tint = OfflineTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Cancel", color = OfflineTextPrimary)
                    }

                    Button(
                        onClick = {
                            val bmp = sourceBitmap ?: return@Button
                            val fitScale = minOf(cropBoxSizePx / bmp.width, cropBoxSizePx / bmp.height)
                            val targetDim = 512

                            val matrix = Matrix().apply {
                                postTranslate(-bmp.width / 2f, -bmp.height / 2f)
                                postRotate(rotationDegrees)
                                postScale(fitScale * scale, fitScale * scale)
                                postTranslate(cropBoxSizePx / 2f + offset.x, cropBoxSizePx / 2f + offset.y)
                                val outputScale = targetDim.toFloat() / cropBoxSizePx
                                postScale(outputScale, outputScale)
                            }

                            val cropped = Bitmap.createBitmap(targetDim, targetDim, Bitmap.Config.ARGB_8888)
                            val canvas = Canvas(cropped)
                            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                            canvas.drawBitmap(bmp, matrix, paint)

                            onCropConfirmed(cropped)
                        },
                        modifier = Modifier.weight(1.3f),
                        colors = ButtonDefaults.buttonColors(containerColor = OfflineDarkButton),
                        shape = RoundedCornerShape(14.dp),
                        enabled = sourceBitmap != null && !isLoading
                    ) {
                        Text("Crop & Save", color = OfflineOnDarkButton, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
