package com.prawalsharma.inkleaf.ui.drawing

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prawalsharma.inkleaf.R
import java.io.File
import java.io.FileOutputStream

data class DrawPath(
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Float,
    val isEraser: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawingScreen(
    existingPath: String = "",     // file path to existing PNG or ""
    onSave: (String) -> Unit,      // returns file path
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val paths = remember { mutableStateListOf<DrawPath>() }
    val undoneStack = remember { mutableStateListOf<DrawPath>() }

    // System back closes the drawing overlay instead of killing the app
    androidx.activity.compose.BackHandler { onBack() }

    var currentColor by remember { mutableStateOf(Color.Black) }
    var brushSize by remember { mutableFloatStateOf(8f) }
    var isEraser by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }

    val brushColors = listOf(
        Color.Black, Color.White, Color(0xFFE53935), Color(0xFF1E88E5),
        Color(0xFF43A047), Color(0xFFFDD835), Color(0xFFFF6B35), Color(0xFF8E24AA)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.drawing_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    IconButton(
                        onClick = { if (paths.isNotEmpty()) { undoneStack.add(paths.removeLast()) } },
                        enabled = paths.isNotEmpty()
                    ) { Icon(Icons.AutoMirrored.Filled.Undo, stringResource(R.string.cd_undo)) }
                    IconButton(onClick = {
                        // Save canvas to PNG
                        val bmp = android.graphics.Bitmap.createBitmap(1080, 1080,
                            android.graphics.Bitmap.Config.ARGB_8888)
                        try {
                            val androidCanvas = android.graphics.Canvas(bmp)
                            androidCanvas.drawColor(android.graphics.Color.WHITE)
                            paths.forEach { dp ->
                                val paint = android.graphics.Paint().apply {
                                    color = if (dp.isEraser) android.graphics.Color.WHITE
                                    else dp.color.toArgb()
                                    strokeWidth = dp.strokeWidth * 3
                                    style = android.graphics.Paint.Style.STROKE
                                    strokeCap = android.graphics.Paint.Cap.ROUND
                                    strokeJoin = android.graphics.Paint.Join.ROUND
                                    isAntiAlias = true
                                }
                                if (dp.points.size >= 2) {
                                    val path = android.graphics.Path()
                                    path.moveTo(dp.points[0].x * 3, dp.points[0].y * 3)
                                    dp.points.drop(1).forEach { pt -> path.lineTo(pt.x * 3, pt.y * 3) }
                                    androidCanvas.drawPath(path, paint)
                                }
                            }
                            val dir = File(context.filesDir, "drawings")
                            dir.mkdirs()
                            val file = File(dir, "drawing_${System.currentTimeMillis()}.png")
                            FileOutputStream(file).use { bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
                            onSave(file.absolutePath)
                        } finally {
                            bmp.recycle()
                        }
                    }) { Icon(Icons.Default.Save, stringResource(R.string.cd_save)) }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Toolbar
            Surface(tonalElevation = 2.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Color swatches
                    brushColors.forEach { c ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(c)
                                .then(
                                    if (c == currentColor && !isEraser)
                                        Modifier.background(Color.Transparent)
                                            .clip(CircleShape)
                                    else Modifier
                                )
                                .then(
                                    Modifier.pointerInput(Unit) {
                                        detectDragGestures(
                                            onDragStart = { isEraser = false; currentColor = c },
                                            onDrag = { _, _ -> }
                                        )
                                    }
                                )
                        ) {
                            if (c == currentColor && !isEraser) {
                                Icon(Icons.Default.Check, null,
                                    tint = if (c == Color.White) Color.Black else Color.White,
                                    modifier = Modifier.align(Alignment.Center).size(16.dp))
                            }
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    // Eraser toggle
                    IconButton(onClick = { isEraser = !isEraser }) {
                        Icon(
                            if (isEraser) Icons.Default.Edit else Icons.Default.Rectangle,
                            if (isEraser) stringResource(R.string.cd_draw) else stringResource(R.string.cd_eraser),
                            tint = if (isEraser) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Brush size slider
            Slider(
                value = brushSize,
                onValueChange = { brushSize = it },
                valueRange = 2f..40f,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )

            // Drawing canvas
            var currentPoints = remember { mutableStateListOf<Offset>() }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                currentPoints.clear()
                                currentPoints.add(offset)
                            },
                            onDrag = { change, _ ->
                                currentPoints.add(change.position)
                            },
                            onDragEnd = {
                                if (currentPoints.isNotEmpty()) {
                                    paths.add(
                                        DrawPath(
                                            points = currentPoints.toList(),
                                            color = currentColor,
                                            strokeWidth = brushSize,
                                            isEraser = isEraser
                                        )
                                    )
                                    undoneStack.clear()
                                    currentPoints.clear()
                                }
                            }
                        )
                    }
            ) {
                paths.forEach { dp -> drawPath(dp) }
                // Draw live stroke
                if (currentPoints.size >= 2) {
                    DrawPath(
                        points = currentPoints.toList(),
                        color = if (isEraser) Color.White else currentColor,
                        strokeWidth = brushSize
                    ).let { drawPath(it) }
                }
            }
        }
    }
}

private fun DrawScope.drawPath(dp: DrawPath) {
    if (dp.points.size < 2) return
    val path = Path()
    path.moveTo(dp.points[0].x, dp.points[0].y)
    dp.points.drop(1).forEach { path.lineTo(it.x, it.y) }
    drawPath(
        path = path,
        color = if (dp.isEraser) Color.White else dp.color,
        style = Stroke(
            width = dp.strokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}
