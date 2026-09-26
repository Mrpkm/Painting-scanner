package com.example.photomosaicscanner.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.example.photomosaicscanner.grid.GridCalculator
import com.example.photomosaicscanner.model.TileCoordinate
import com.example.photomosaicscanner.ui.theme.GridCompleted
import com.example.photomosaicscanner.ui.theme.GridCurrent
import com.example.photomosaicscanner.ui.theme.GridPending

enum class TileVisualState { PENDING, CURRENT, COMPLETED }

/**
 * Renders the capture grid: cell outlines, a fill color per tile state, and
 * the tile's serpentine sequence number. Used, unmodified, for the setup
 * preview (all pending), the live camera overlay (with real state), and the
 * overview review grid (with tap-to-jump).
 */
@Composable
fun GridDiagram(
    rows: Int,
    columns: Int,
    aspectRatio: Float,
    tileState: (TileCoordinate) -> TileVisualState,
    modifier: Modifier = Modifier,
    onTileTap: ((TileCoordinate) -> Unit)? = null
) {
    val order = remember(rows, columns) { GridCalculator.serpentineOrder(rows, columns) }
    val sequenceNumber = remember(order) {
        order.withIndex().associate { (i, tile) -> tile to (i + 1) }
    }
    val textMeasurer = rememberTextMeasurer()
    val safeRows = rows.coerceAtLeast(1)
    val safeCols = columns.coerceAtLeast(1)
    val safeRatio = if (aspectRatio.isFinite() && aspectRatio > 0f) aspectRatio else 1f

    val baseModifier = modifier
        .fillMaxWidth()
        .aspectRatio(safeRatio)

    val tapModifier = if (onTileTap != null) {
        Modifier.pointerInput(safeRows, safeCols) {
            detectTapGestures { offset ->
                val cellW = size.width / safeCols
                val cellH = size.height / safeRows
                val col = (offset.x / cellW).toInt().coerceIn(0, safeCols - 1)
                val row = (offset.y / cellH).toInt().coerceIn(0, safeRows - 1)
                onTileTap(TileCoordinate(row, col))
            }
        }
    } else {
        Modifier
    }

    Canvas(modifier = baseModifier.then(tapModifier)) {
        val cellW = size.width / safeCols
        val cellH = size.height / safeRows
        for (row in 0 until safeRows) {
            for (col in 0 until safeCols) {
                val tile = TileCoordinate(row, col)
                val color = when (tileState(tile)) {
                    TileVisualState.PENDING -> GridPending
                    TileVisualState.CURRENT -> GridCurrent
                    TileVisualState.COMPLETED -> GridCompleted
                }
                val topLeft = Offset(col * cellW, row * cellH)
                val cellSize = Size(cellW, cellH)

                drawRect(color = color.copy(alpha = 0.30f), topLeft = topLeft, size = cellSize)
                drawRect(color = color, topLeft = topLeft, size = cellSize, style = Stroke(width = 3f))

                val number = (sequenceNumber[tile] ?: 0).toString()
                val layout = textMeasurer.measure(number, style = TextStyle(fontSize = 14.sp))
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset(
                        x = topLeft.x + cellW / 2 - layout.size.width / 2,
                        y = topLeft.y + cellH / 2 - layout.size.height / 2
                    )
                )
            }
        }
    }
}
