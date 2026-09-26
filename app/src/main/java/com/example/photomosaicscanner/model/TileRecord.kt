package com.example.photomosaicscanner.model

/**
 * Metadata about the most recent capture of a tile. Older versions of a
 * retaken tile are never deleted from disk (see CaptureFileNaming) -- this
 * record only points at the current/latest one.
 */
data class TileRecord(
    val row: Int,
    val col: Int,
    val filename: String,
    val version: Int,
    val timestampMillis: Long
)
