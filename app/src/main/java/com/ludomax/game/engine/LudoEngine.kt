package com.ludomax.game.engine

/** A planned (not yet applied) move of one token. */
class MovePlan(
    val color: Int,
    val tokenId: Int,
    val from: Int,
    val to: Int,
    val dice: Int,
    /** Every position the token visits, in order (excluding [from]). */
    val steps: List<Int>,
    /** Opponent tokens sent back to the yard: pairs of (color, tokenId). */
    val captured: List<IntArray>
) {
    val leavesYard: Boolean get() = from == Board.YARD
    val reachesHome: Boolean get() = to == Board.FINISH
    val captures: Boolean get() = captured.isNotEmpty()
}

/**
 * Pure game rules, free of any Android code.
 *
 * Rules implemented:
 *  - Roll a 6 to bring a token out of the yard onto your start cell.
 *  - Tokens travel clockwise, then turn into their own coloured home column.
 *  - An exact roll is needed to reach the centre.
 *  - Landing on an opponent token (on an unsafe cell) sends it back to its yard.
 *  - Start cells and star cells are safe. Two tokens of the same colour on one
 *    cell protect each other.
 *  - A 6, a capture or reaching the centre gives another turn.
 *  - Three 6s in a row cancel the turn.
 *  - Players are ranked in the order they bring both tokens home.
 */
class LudoEngine(val players: List<Int>) {

    /** pos[color][tokenId] */
    val pos: Array<IntArray> = Array(4) { IntArray(Board.TOKENS_PER_PLAYER) { Board.YARD } }

    /** Colours that finished, in order (1st place first). */
    val ranking = ArrayList<Int>()

    var currentIndex = 0
        private set

    var consecutiveSixes = 0

    val current: Int get() = players[currentIndex]

    init {
        require(players.size in 2..4) { "2 to 4 players" }
    }

    fun isFinished(color: Int): Boolean = pos[color].all { it == Board.FINISH }

    val isGameOver: Boolean get() = players.count { !isFinished(it) } <= 1

    fun legalMoves(color: Int, dice: Int): List<Int> {
        val out = ArrayList<Int>(2)
        for (id in 0 until Board.TOKENS_PER_PLAYER) {
            if (canMove(pos[color][id], dice)) out.add(id)
        }
        return out
    }

    private fun canMove(p: Int, dice: Int): Boolean = when {
        p == Board.FINISH -> false
        p == Board.YARD -> dice == 6
        else -> p + dice <= Board.FINISH
    }

    fun plan(color: Int, tokenId: Int, dice: Int): MovePlan {
        val from = pos[color][tokenId]
        val steps = ArrayList<Int>()
        val to: Int
        if (from == Board.YARD) {
            to = 0
            steps.add(0)
        } else {
            to = from + dice
            for (p in from + 1..to) steps.add(p)
        }

        val captured = ArrayList<IntArray>()
        if (to in 0..Board.LAST_LOOP_POS) {
            val idx = Board.loopIndex(color, to)
            if (!Board.isLoopSafe(idx)) {
                for (other in players) {
                    if (other == color) continue
                    val here = ArrayList<Int>(2)
                    for (id in 0 until Board.TOKENS_PER_PLAYER) {
                        val p = pos[other][id]
                        if (p in 0..Board.LAST_LOOP_POS && Board.loopIndex(other, p) == idx) here.add(id)
                    }
                    // A pair of the same colour on one cell cannot be captured.
                    if (here.size == 1) captured.add(intArrayOf(other, here[0]))
                }
            }
        }
        return MovePlan(color, tokenId, from, to, dice, steps, captured)
    }

    /** Applies the plan to the game state. */
    fun commit(plan: MovePlan) {
        pos[plan.color][plan.tokenId] = plan.to
        for (c in plan.captured) pos[c[0]][c[1]] = Board.YARD
        if (isFinished(plan.color) && !ranking.contains(plan.color)) ranking.add(plan.color)
    }

    /** Moves to the next player who has not finished yet. */
    fun advanceTurn() {
        consecutiveSixes = 0
        if (isGameOver) return
        var i = currentIndex
        do {
            i = (i + 1) % players.size
        } while (isFinished(players[i]))
        currentIndex = i
    }

    /** Colours still playing, then (if the game is over) the last one gets the final rank. */
    fun finalRanking(): List<Int> {
        val r = ArrayList(ranking)
        for (p in players) if (!r.contains(p)) r.add(p)
        return r
    }

    /** Sum of progress of a colour (used by the bot / tie breaks). */
    fun progress(color: Int): Int = pos[color].sumOf { if (it < 0) 0 else it + 1 }
}
