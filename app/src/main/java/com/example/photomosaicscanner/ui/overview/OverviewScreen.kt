package com.example.photomosaicscanner.ui.overview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.photomosaicscanner.grid.GridCalculator
import com.example.photomosaicscanner.model.ScanSession
import com.example.photomosaicscanner.session.SessionViewModel

@Composable
fun OverviewScreen(
    session: ScanSession,
    viewModel: SessionViewModel,
    onContinue: () -> Unit,
    onFinish: () -> Unit,
    onStartOver: () -> Unit
) {
    val order = GridCalculator.serpentineOrder(session.rows, session.columns)
    val completed = session.completedTiles.size
    val total = session.totalTiles
    val progress = if (total > 0) completed.toFloat() / total else 0f

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text(
            "Painting: ${session.paintingWidth} × ${session.paintingHeight} ${session.units.label}",
            style = MaterialTheme.typography.titleMedium
        )
        Text("Grid: ${session.rows} × ${session.columns}  •  Overlap: ${session.overlapPercent}%")
        Spacer(Modifier.height(16.dp))

        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(4.dp))
        Text("$completed / $total photographs")
        Spacer(Modifier.height(16.dp))

        Text("Tap a tile to jump to it for capture or retake:", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(session.columns.coerceAtLeast(1)),
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(order) { tile ->
                val isDone = session.completedTiles.containsKey(tile.fileKey)
                val isCurrent = tile == session.currentTile
                OutlinedButton(onClick = {
                    viewModel.jumpToTile(tile)
                    onContinue()
                }) {
                    Text(
                        text = when {
                            isCurrent -> "${tile.displayLabel}\n→"
                            isDone -> "${tile.displayLabel}\n✓"
                            else -> tile.displayLabel
                        },
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onContinue, modifier = Modifier.weight(1f)) { Text("Continue") }
            OutlinedButton(onClick = onFinish, modifier = Modifier.weight(1f)) { Text("Finish Session") }
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onStartOver) { Text("Start a new painting scan…") }
    }
}
