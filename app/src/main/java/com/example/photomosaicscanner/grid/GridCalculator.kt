package com.example.photomosaicscanner.grid

import com.example.photomosaicscanner.model.TileCoordinate
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Pure geometry helper, no Android dependencies.
 *
 * Stage 1 has no way to measure the camera's true field of view against the
 * physical painting, so [suggestGrid] produces a rough starting point only.
 * It is labeled as an estimate everywhere it is surfaced in the UI, per the
 * project's "never give false precision" principle. Manual grid entry
 * remains the primary, trusted path; the suggestion is just a convenience.
 */
object GridCalculator {

    /** Serpentine (boustrophedon) visiting order over a rows x columns grid. */
    fun serpentineOrder(rows: Int, columns: Int): List<TileCoordinate> {
        val order = mutableListOf<TileCoordinate>()
        for (row in 0 until rows) {
            val colRange = if (row % 2 == 0) 0 until columns else (columns - 1) downTo 0
            for (col in colRange) order.add(TileCoordinate(row, col))
        }
        return order
    }

    data class GridSuggestion(val rows: Int, val columns: Int, val note: String)

    /**
     * Rough starting suggestion assuming a typical close-range handheld
     * capture footprint of roughly 35 x 25 cm per tile before overlap is
     * applied. This is a placeholder assumption, not a measurement of the
     * device's actual field of view. Precise camera-geometry-based grid
     * calculation is deferred to Stage 2, once real positioning data exists.
     */
    fun suggestGrid(paintingWidthCm: Double, paintingHeightCm: Double, overlapPercent: Int): GridSuggestion {
        val assumedTileWidthCm = 35.0
        val assumedTileHeightCm = 25.0
        val overlapFraction = (overlapPercent / 100.0).coerceIn(0.0, 0.9)
        val effectiveStepW = assumedTileWidthCm * (1 - overlapFraction)
        val effectiveStepH = assumedTileHeightCm * (1 - overlapFraction)
        val columns = max(1, (paintingWidthCm / effectiveStepW).roundToInt())
        val rows = max(1, (paintingHeightCm / effectiveStepH).roundToInt())
        return GridSuggestion(
            rows = rows,
            columns = columns,
            note = "Estimate only, based on an assumed capture footprint, not a real " +
                "measurement of your camera or distance. Take a test photo of one " +
                "corner of the painting and adjust rows/columns before relying on it."
        )
    }
}
