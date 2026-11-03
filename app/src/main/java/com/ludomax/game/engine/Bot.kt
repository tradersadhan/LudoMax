package com.ludomax.game.engine

import kotlin.random.Random

/** Computer opponent. EASY plays mostly random, HARD scores every move. */
object Bot {

    fun choose(
        engine: LudoEngine,
        color: Int,
        dice: Int,
        moves: List<Int>,
        hard: Boolean,
        rnd: Random
    ): Int {
        if (moves.size == 1) return moves[0]
        if (!hard) {
            // Easy: take an obvious capture sometimes, otherwise random.
            if (rnd.nextInt(100) < 35) {
                for (id in moves) {
                    val plan = engine.plan(color, id, dice)
                    if (plan.captures || plan.reachesHome) return id
                }
            }
            return moves[rnd.nextInt(moves.size)]
        }

        var best = moves[0]
        var bestScore = Double.NEGATIVE_INFINITY
        for (id in moves) {
            val s = score(engine, color, id, dice) + rnd.nextDouble() * 3.0
            if (s > bestScore) {
                bestScore = s
                best = id
            }
        }
        return best
    }

    private fun score(engine: LudoEngine, color: Int, id: Int, dice: Int): Double {
        val plan = engine.plan(color, id, dice)
        var s = 0.0

        if (plan.reachesHome) s += 100.0
        for (c in plan.captured) s += 70.0 + engine.pos[c[0]][c[1]] * 2.0
        if (plan.leavesYard) s += 45.0
        if (plan.to >= Board.HOME_COL_START && plan.to < Board.FINISH) s += 30.0
        if (plan.to in 0..Board.LAST_LOOP_POS && Board.isSafePos(color, plan.to)) s += 25.0

        // Make a safe pair with my own other token.
        val other = engine.pos[color][1 - id]
        if (plan.to in 0..Board.LAST_LOOP_POS && other == plan.to) s += 20.0

        val dangerAfter = threatened(engine, color, plan.to, id)
        if (dangerAfter) s -= 40.0 + plan.to

        // Running away from a current threat is valuable.
        if (plan.from in 0..Board.LAST_LOOP_POS && !Board.isSafePos(color, plan.from) &&
            threatened(engine, color, plan.from, id) && !dangerAfter
        ) {
            s += 28.0
        }

        s += dice + plan.to * 0.5
        return s
    }

    /** Can any opponent reach this cell with one roll (1..6)? */
    private fun threatened(
        engine: LudoEngine,
        color: Int,
        pos: Int,
        movingId: Int
    ): Boolean {
        if (pos !in 0..Board.LAST_LOOP_POS) return false
        if (Board.isSafePos(color, pos)) return false
        // Protected by own pair?
        val other = engine.pos[color][1 - movingId]
        if (other == pos) return false

        val idx = Board.loopIndex(color, pos)
        for (opp in engine.players) {
            if (opp == color) continue
            for (oid in 0 until Board.TOKENS_PER_PLAYER) {
                val op = engine.pos[opp][oid]
                if (op !in 0..Board.LAST_LOOP_POS) continue
                for (d in 1..6) {
                    val np = op + d
                    if (np > Board.LAST_LOOP_POS) break
                    if (Board.loopIndex(opp, np) == idx) return true
                }
            }
        }
        return false
    }
}
