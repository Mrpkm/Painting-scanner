package com.example.photomosaicscanner.capture

import com.example.photomosaicscanner.model.TileCoordinate
import java.io.File

object CaptureFileNaming {
    /**
     * Returns the next free filename for a tile and never overwrites an
     * existing capture. First capture: r01_c01.jpg. A retake produces
     * r01_c01_v2.jpg, then _v3, etc., so a previous photograph is never
     * silently lost.
     */
    fun nextAvailableFile(sessionDir: File, tile: TileCoordinate): Pair<File, Int> {
        val base = tile.fileKey
        var version = 1
        var candidate = File(sessionDir, "$base.jpg")
        while (candidate.exists()) {
            version += 1
            candidate = File(sessionDir, "${base}_v$version.jpg")
        }
        return candidate to version
    }
}
