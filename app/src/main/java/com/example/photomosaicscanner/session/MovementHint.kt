package com.example.photomosaicscanner.session

/**
 * Deliberately qualitative. Stage 1 knows the grid and the capture order,
 * but nothing about the phone's actual physical position relative to the
 * painting -- so it never says "move 4.2 cm right", only relative
 * directions like "move right". Precise positioning is reserved for Stage 2
 * once visual tracking exists.
 */
enum class MoveDirection { START, RIGHT, LEFT, ROW_CHANGE, DONE }

data class MovementHint(val direction: MoveDirection, val message: String)
