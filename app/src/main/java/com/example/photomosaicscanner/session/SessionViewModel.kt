package com.example.photomosaicscanner.session

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.photomosaicscanner.model.PaintingUnits
import com.example.photomosaicscanner.model.ScanSession
import com.example.photomosaicscanner.model.TileCoordinate
import com.example.photomosaicscanner.model.TileRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class SessionViewModel(application: Application) : AndroidViewModel(application) {

    private val _session = MutableStateFlow<ScanSession?>(null)
    val session: StateFlow<ScanSession?> = _session.asStateFlow()

    init {
        _session.value = SessionRepository.loadActiveSession(getApplication())
    }

    fun sessionDir(): File? = _session.value?.let { File(it.sessionFolderPath) }

    fun startNewSession(
        paintingWidth: Double,
        paintingHeight: Double,
        units: PaintingUnits,
        overlapPercent: Int,
        rows: Int,
        columns: Int
    ) {
        val context = getApplication<Application>()
        val folder = SessionRepository.createSessionFolder(context)
        val newSession = ScanSession(
            sessionId = SessionRepository.newSessionId(),
            sessionFolderPath = folder.absolutePath,
            createdAtMillis = System.currentTimeMillis(),
            paintingWidth = paintingWidth,
            paintingHeight = paintingHeight,
            units = units,
            overlapPercent = overlapPercent,
            rows = rows,
            columns = columns,
            deviceModel = SessionRepository.currentDeviceModel()
        )
        _session.value = newSession
        persist(newSession)
    }

    /** Records (or overwrites) a capture for [tile] and advances the cursor. */
    fun recordCapture(tile: TileCoordinate, record: TileRecord) {
        val current = _session.value ?: return
        val updatedTiles = current.completedTiles.toMutableMap()
        updatedTiles[tile.fileKey] = record

        val order = current.captureOrder
        val tileIndex = order.indexOf(tile)
        val nextIndex = if (tileIndex == current.currentIndex) {
            nextIncompleteIndex(order, updatedTiles, current.currentIndex)
        } else {
            current.currentIndex
        }

        val updated = current.copy(completedTiles = updatedTiles, currentIndex = nextIndex)
        _session.value = updated
        persist(updated)
    }

    /** Moves the cursor to an arbitrary tile, e.g. to retake it. */
    fun jumpToTile(tile: TileCoordinate) {
        val current = _session.value ?: return
        val idx = current.captureOrder.indexOf(tile)
        if (idx < 0) return
        val updated = current.copy(currentIndex = idx)
        _session.value = updated
        persist(updated)
    }

    fun movementHint(): MovementHint {
        val current = _session.value
            ?: return MovementHint(MoveDirection.START, "No active session.")
        val order = current.captureOrder
        if (current.isComplete) {
            return MovementHint(MoveDirection.DONE, "All tiles captured.")
        }
        val previous = order.getOrNull(current.currentIndex - 1)
            ?: return MovementHint(MoveDirection.START, "Position over tile 1 and capture.")
        val target = current.currentTile
            ?: return MovementHint(MoveDirection.DONE, "All tiles captured.")

        return when {
            target.row != previous.row -> MovementHint(
                MoveDirection.ROW_CHANGE,
                "Move to the next row, toward the " +
                    (if (target.col > previous.col) "left" else "right") + " edge."
            )
            target.col > previous.col -> MovementHint(MoveDirection.RIGHT, "Move right to the next tile.")
            target.col < previous.col -> MovementHint(MoveDirection.LEFT, "Move left to the next tile.")
            else -> MovementHint(MoveDirection.START, "Stay in position and capture.")
        }
    }

    /** First tile after [fromIndex]'s row-major serpentine spot with no capture yet. */
    private fun nextIncompleteIndex(
        order: List<TileCoordinate>,
        completed: Map<String, TileRecord>,
        fromIndex: Int
    ): Int {
        for (i in (fromIndex + 1) until order.size) {
            if (!completed.containsKey(order[i].fileKey)) return i
        }
        // Wrap to fill any earlier gap left by a retake-in-progress.
        for (i in order.indices) {
            if (!completed.containsKey(order[i].fileKey)) return i
        }
        return order.size // past the end: session is complete
    }

    fun finishSession() {
        SessionRepository.clearActiveSession(getApplication())
        _session.value = null
    }

    fun discardAndStartOver() {
        SessionRepository.clearActiveSession(getApplication())
        _session.value = null
    }

    private fun persist(session: ScanSession) {
        viewModelScope.launch(Dispatchers.IO) {
            SessionRepository.save(getApplication(), session)
        }
    }
}
