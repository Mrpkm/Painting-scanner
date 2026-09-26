package com.example.photomosaicscanner.model

enum class PaintingUnits(val label: String, val toCmFactor: Double) {
    CM("cm", 1.0),
    MM("mm", 0.1),
    INCH("in", 2.54);

    companion object {
        fun fromLabel(label: String): PaintingUnits =
            values().firstOrNull { it.label == label } ?: CM
    }
}
