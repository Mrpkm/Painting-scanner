package com.example.photomosaicscanner.model

import com.example.photomosaicscanner.grid.GridCalculator
import org.json.JSONArray
import org.json.JSONObject

/**
 * Full state of one painting scan. Immutable: every mutation (a capture, a
 * retake, moving the cursor) produces a copy, which SessionViewModel then
 * persists via SessionRepository. Uses org.json (built into Android) for
 * serialization rather than adding a JSON library dependency.
 */
data class ScanSession(
    val sessionId: String,
    val sessionFolderPath: String,
    val createdAtMillis: Long,
    val paintingWidth: Double,
    val paintingHeight: Double,
    val units: PaintingUnits,
    val overlapPercent: Int,
    val rows: Int,
    val columns: Int,
    val deviceModel: String,
    val currentIndex: Int = 0,
    val completedTiles: Map<String, TileRecord> = emptyMap()
) {
    val captureOrder: List<TileCoordinate> get() = GridCalculator.serpentineOrder(rows, columns)
    val totalTiles: Int get() = rows * columns
    val currentTile: TileCoordinate? get() = captureOrder.getOrNull(currentIndex)
    val isComplete: Boolean get() = completedTiles.size >= totalTiles

    fun toJson(): JSONObject = JSONObject().apply {
        put("sessionId", sessionId)
        put("sessionFolderPath", sessionFolderPath)
        put("createdAtMillis", createdAtMillis)
        put("paintingWidth", paintingWidth)
        put("paintingHeight", paintingHeight)
        put("units", units.label)
        put("overlap", overlapPercent / 100.0)
        put("rows", rows)
        put("columns", columns)
        put("device", deviceModel)
        put("currentIndex", currentIndex)
        put("captureOrder", "serpentine")
        val tilesJson = JSONArray()
        completedTiles.values
            .sortedWith(compareBy({ it.row }, { it.col }))
            .forEach { record ->
                tilesJson.put(
                    JSONObject().apply {
                        put("row", record.row + 1)
                        put("col", record.col + 1)
                        put("filename", record.filename)
                        put("version", record.version)
                        put("timestamp", record.timestampMillis)
                    }
                )
            }
        put("tiles", tilesJson)
    }

    companion object {
        fun fromJson(json: JSONObject): ScanSession {
            val units = PaintingUnits.fromLabel(json.getString("units"))
            val tiles = mutableMapOf<String, TileRecord>()
            val tilesJson = json.optJSONArray("tiles")
            if (tilesJson != null) {
                for (i in 0 until tilesJson.length()) {
                    val t = tilesJson.getJSONObject(i)
                    val row = t.getInt("row") - 1
                    val col = t.getInt("col") - 1
                    val record = TileRecord(
                        row = row,
                        col = col,
                        filename = t.getString("filename"),
                        version = t.optInt("version", 1),
                        timestampMillis = t.optLong("timestamp", 0L)
                    )
                    tiles[TileCoordinate(row, col).fileKey] = record
                }
            }
            return ScanSession(
                sessionId = json.getString("sessionId"),
                sessionFolderPath = json.getString("sessionFolderPath"),
                createdAtMillis = json.getLong("createdAtMillis"),
                paintingWidth = json.getDouble("paintingWidth"),
                paintingHeight = json.getDouble("paintingHeight"),
                units = units,
                overlapPercent = (json.getDouble("overlap") * 100).toInt(),
                rows = json.getInt("rows"),
                columns = json.getInt("columns"),
                deviceModel = json.optString("device", "unknown"),
                currentIndex = json.optInt("currentIndex", 0),
                completedTiles = tiles
            )
        }
    }
}
