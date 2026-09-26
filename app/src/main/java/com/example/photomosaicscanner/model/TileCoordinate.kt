package com.example.photomosaicscanner.model

/**
 * A stable physical grid location on the painting, independent of capture
 * order. Row/column are zero-indexed internally; filenames and on-screen
 * labels use 1-indexed values, matching the "r01_c01" naming convention
 * required for Hugin-friendly output.
 */
data class TileCoordinate(val row: Int, val col: Int) {
    val fileKey: String get() = "r%02d_c%02d".format(row + 1, col + 1)
    val displayLabel: String get() = "R${row + 1}C${col + 1}"
}
