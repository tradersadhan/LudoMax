package com.ludomax.game.engine

/**
 * Geometry of the LudoMax mini board (9 x 9 cells, 2 tokens per player).
 *
 * Colours are indexed clockwise starting at the top-right corner:
 *   0 = GREEN (top-right), 1 = RED (bottom-right), 2 = BLUE (bottom-left), 3 = YELLOW (top-left)
 *
 * The outer track ("loop") has 28 cells. Index 0 is GREEN's start cell and the
 * indexes run COUNTER-clockwise on screen. Every token travels CLOCKWISE, so a
 * token's loop index goes DOWN by one for every step.
 *
 * Token position values (per token):
 *   -1      = in the yard (home base)
 *   0..26   = on the outer loop (0 = the colour's own start cell)
 *   27, 28  = inside the coloured home column
 *   29      = reached the centre (finished)
 */
object Board {
    const val SIZE = 9
    const val LOOP = 28
    const val YARD = -1
    const val LAST_LOOP_POS = 26
    const val HOME_COL_START = 27
    const val FINISH = 29
    const val TOKENS_PER_PLAYER = 2

    const val GREEN = 0
    const val RED = 1
    const val BLUE = 2
    const val YELLOW = 3

    val COLOR_NAMES = arrayOf("GREEN", "RED", "BLUE", "YELLOW")

    /** Loop cells as (row, col), counter-clockwise starting at GREEN start (1,5). */
    private val LOOP_CELLS: Array<IntArray> = arrayOf(
        intArrayOf(1, 5), intArrayOf(0, 5), intArrayOf(0, 4), intArrayOf(0, 3),
        intArrayOf(1, 3), intArrayOf(2, 3), intArrayOf(3, 2),
        intArrayOf(3, 1), intArrayOf(3, 0), intArrayOf(4, 0), intArrayOf(5, 0),
        intArrayOf(5, 1), intArrayOf(5, 2), intArrayOf(6, 3),
        intArrayOf(7, 3), intArrayOf(8, 3), intArrayOf(8, 4), intArrayOf(8, 5),
        intArrayOf(7, 5), intArrayOf(6, 5), intArrayOf(5, 6),
        intArrayOf(5, 7), intArrayOf(5, 8), intArrayOf(4, 8), intArrayOf(3, 8),
        intArrayOf(3, 7), intArrayOf(3, 6), intArrayOf(2, 5)
    )

    /** Loop index of each colour's start cell. */
    val START_INDEX = intArrayOf(0, 21, 14, 7)

    /** Home column cells (row, col) for each colour, from the entry cell towards the centre. */
    private val HOME_COLUMN: Array<Array<IntArray>> = arrayOf(
        arrayOf(intArrayOf(1, 4), intArrayOf(2, 4)),   // GREEN  (down)
        arrayOf(intArrayOf(4, 7), intArrayOf(4, 6)),   // RED    (left)
        arrayOf(intArrayOf(7, 4), intArrayOf(6, 4)),   // BLUE   (up)
        arrayOf(intArrayOf(4, 1), intArrayOf(4, 2))    // YELLOW (right)
    )

    /** Loop indexes of the four extra "star" safe cells (besides the 4 start cells). */
    private val STAR_INDEX = intArrayOf(3, 10, 17, 24)

    private val safeLoop = BooleanArray(LOOP).also { arr ->
        START_INDEX.forEach { arr[it] = true }
        STAR_INDEX.forEach { arr[it] = true }
    }

    /** Top-left corner of each colour's 3x3 yard in cell units (x = col, y = row). */
    private val YARD_ORIGIN = arrayOf(
        floatArrayOf(6f, 0f), // GREEN  top-right
        floatArrayOf(6f, 6f), // RED    bottom-right
        floatArrayOf(0f, 6f), // BLUE   bottom-left
        floatArrayOf(0f, 0f)  // YELLOW top-left
    )

    fun isLoopSafe(loopIndex: Int): Boolean = safeLoop[loopIndex]

    fun isStarIndex(loopIndex: Int): Boolean = STAR_INDEX.contains(loopIndex)

    fun isStartIndex(loopIndex: Int): Boolean = START_INDEX.contains(loopIndex)

    fun loopRow(loopIndex: Int): Int = LOOP_CELLS[loopIndex][0]
    fun loopCol(loopIndex: Int): Int = LOOP_CELLS[loopIndex][1]

    fun homeColumnRow(color: Int, i: Int): Int = HOME_COLUMN[color][i][0]
    fun homeColumnCol(color: Int, i: Int): Int = HOME_COLUMN[color][i][1]

    /** Loop index for a token of [color] that sits at loop position [pos] (0..26). */
    fun loopIndex(color: Int, pos: Int): Int {
        require(pos in 0..LAST_LOOP_POS)
        return ((START_INDEX[color] - pos) % LOOP + LOOP) % LOOP
    }

    /** Loop index of the cell that has the entry arrow for [color]. */
    fun entryLoopIndex(color: Int): Int = loopIndex(color, LAST_LOOP_POS)

    /** True if a token at [pos] is on a cell where it cannot be captured. */
    fun isSafePos(color: Int, pos: Int): Boolean = when {
        pos < 0 -> true
        pos in 0..LAST_LOOP_POS -> safeLoop[loopIndex(color, pos)]
        else -> true // home column and centre
    }

    /**
     * Board coordinates (x = column, y = row, in cell units, cell centre = +0.5)
     * of a token. [tokenId] chooses the yard slot / finish offset.
     */
    fun cellCenter(color: Int, tokenId: Int, pos: Int): FloatArray {
        return when {
            pos == YARD -> {
                val o = YARD_ORIGIN[color]
                // Two diagonal yard slots, like the reference board.
                val d = 1f + tokenId
                floatArrayOf(o[0] + d, o[1] + d)
            }
            pos <= LAST_LOOP_POS -> {
                val li = loopIndex(color, pos)
                floatArrayOf(loopCol(li) + 0.5f, loopRow(li) + 0.5f)
            }
            pos < FINISH -> {
                val i = pos - HOME_COLUMN_OFFSET
                floatArrayOf(homeColumnCol(color, i) + 0.5f, homeColumnRow(color, i) + 0.5f)
            }
            else -> {
                // Centre triangle of the colour; two tokens sit side by side.
                val side = if (tokenId == 0) -0.22f else 0.22f
                when (color) {
                    GREEN -> floatArrayOf(4.5f + side, 3.62f)
                    RED -> floatArrayOf(5.38f, 4.5f + side)
                    BLUE -> floatArrayOf(4.5f - side, 5.38f)
                    else -> floatArrayOf(3.62f, 4.5f - side)
                }
            }
        }
    }

    private const val HOME_COLUMN_OFFSET = HOME_COL_START
}
