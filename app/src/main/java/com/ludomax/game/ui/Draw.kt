package com.ludomax.game.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.cos
import kotlin.math.sin

/** Canvas drawing helpers: board, pawns, dice, avatars. Only used on the UI thread. */
object Draw {

    // Index order: 0 GREEN, 1 RED, 2 BLUE, 3 YELLOW
    val BOARD = intArrayOf(
        Color.parseColor("#22B14C"),
        Color.parseColor("#ED1C24"),
        Color.parseColor("#00A2E8"),
        Color.parseColor("#FFC90E")
    )
    val PAWN = intArrayOf(
        Color.parseColor("#1FA648"),
        Color.parseColor("#E2262D"),
        Color.parseColor("#1A62C4"),
        Color.parseColor("#F2B705")
    )
    val PAWN_DARK = intArrayOf(
        Color.parseColor("#0B5E27"),
        Color.parseColor("#8E0F14"),
        Color.parseColor("#0A2F6B"),
        Color.parseColor("#9A6B00")
    )

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val path = Path()
    private val rect = RectF()

    // ------------------------------------------------------------------ board

    fun drawBoard(c: Canvas, size: Float) {
        val cell = size / 9f
        val gridColor = Color.parseColor("#3A3A3A")
        val lineW = maxOf(1f, size / 650f)

        fill.style = Paint.Style.FILL
        fill.shader = null
        fill.color = Color.WHITE
        c.drawRect(0f, 0f, size, size, fill)

        // Cross cells
        stroke.strokeWidth = lineW
        stroke.color = gridColor
        for (r in 0 until 9) {
            for (col in 0 until 9) {
                val inCorner = (r < 3 || r > 5) && (col < 3 || col > 5)
                val inCenter = r in 3..5 && col in 3..5
                if (inCorner || inCenter) continue
                val colorIdx = cellColor(r, col)
                fill.color = if (colorIdx >= 0) BOARD[colorIdx] else Color.WHITE
                c.drawRect(col * cell, r * cell, (col + 1) * cell, (r + 1) * cell, fill)
                c.drawRect(col * cell, r * cell, (col + 1) * cell, (r + 1) * cell, stroke)
            }
        }

        // Yards
        val yardOrigins = arrayOf(
            floatArrayOf(6f, 0f), floatArrayOf(6f, 6f), floatArrayOf(0f, 6f), floatArrayOf(0f, 0f)
        )
        for (i in 0 until 4) {
            val x = yardOrigins[i][0] * cell
            val y = yardOrigins[i][1] * cell
            fill.color = BOARD[i]
            c.drawRect(x, y, x + 3 * cell, y + 3 * cell, fill)
            stroke.color = Color.parseColor("#222222")
            c.drawRect(x, y, x + 3 * cell, y + 3 * cell, stroke)
            val inset = cell * 0.3f
            fill.color = Color.WHITE
            c.drawRect(x + inset, y + inset, x + 3 * cell - inset, y + 3 * cell - inset, fill)
            c.drawRect(x + inset, y + inset, x + 3 * cell - inset, y + 3 * cell - inset, stroke)
            // Two token slots
            for (t in 0..1) {
                val cx = x + (1f + t) * cell
                val cy = y + (1f + t) * cell
                fill.color = BOARD[i]
                c.drawCircle(cx, cy, cell * 0.34f, fill)
                stroke.color = Color.parseColor("#222222")
                c.drawCircle(cx, cy, cell * 0.34f, stroke)
            }
        }

        // Centre triangles
        val a = 3f * cell
        val b = 6f * cell
        val m = 4.5f * cell
        centreTriangle(c, BOARD[0], a, a, b, a, m, m) // green top
        centreTriangle(c, BOARD[1], b, a, b, b, m, m) // red right
        centreTriangle(c, BOARD[2], b, b, a, b, m, m) // blue bottom
        centreTriangle(c, BOARD[3], a, b, a, a, m, m) // yellow left

        // Arrows on entry cells and stars on safe cells
        arrow(c, 0, cell, 0f)     // green: down
        arrow(c, 1, cell, 90f)    // red: left
        arrow(c, 2, cell, 180f)   // blue: up
        arrow(c, 3, cell, 270f)   // yellow: right

        // Stars: start cells (white star on colour) and extra safe cells (grey star)
        val startCells = arrayOf(intArrayOf(1, 5), intArrayOf(5, 7), intArrayOf(7, 3), intArrayOf(3, 1))
        for (s in startCells) star(c, (s[1] + 0.5f) * cell, (s[0] + 0.5f) * cell, cell * 0.28f, Color.WHITE, 0x99000000.toInt())
        val greyStars = arrayOf(intArrayOf(0, 3), intArrayOf(5, 0), intArrayOf(8, 5), intArrayOf(3, 8))
        for (s in greyStars) star(c, (s[1] + 0.5f) * cell, (s[0] + 0.5f) * cell, cell * 0.28f, Color.parseColor("#9AA0A6"), 0x55000000)
    }

    private fun cellColor(r: Int, c: Int): Int {
        // Start cells and home columns
        if ((r == 1 && c == 5) || (r == 1 && c == 4) || (r == 2 && c == 4)) return 0
        if ((r == 5 && c == 7) || (r == 4 && c == 7) || (r == 4 && c == 6)) return 1
        if ((r == 7 && c == 3) || (r == 7 && c == 4) || (r == 6 && c == 4)) return 2
        if ((r == 3 && c == 1) || (r == 4 && c == 1) || (r == 4 && c == 2)) return 3
        return -1
    }

    private fun centreTriangle(
        c: Canvas, color: Int, x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float
    ) {
        path.reset()
        path.moveTo(x1, y1)
        path.lineTo(x2, y2)
        path.lineTo(x3, y3)
        path.close()
        fill.color = color
        c.drawPath(path, fill)
        stroke.color = 0x88FFFFFF.toInt()
        stroke.strokeWidth = 1.5f
        c.drawPath(path, stroke)
    }

    /** Direction arrow on the colour's entry cell. */
    private fun arrow(c: Canvas, color: Int, cell: Float, rotation: Float) {
        // Entry cells: green (0,4), red (4,8), blue (8,4), yellow (4,0)
        val row: Int
        val col: Int
        when (color) {
            0 -> { row = 0; col = 4 }
            1 -> { row = 4; col = 8 }
            2 -> { row = 8; col = 4 }
            else -> { row = 4; col = 0 }
        }
        val cx = (col + 0.5f) * cell
        val cy = (row + 0.5f) * cell
        c.save()
        c.translate(cx, cy)
        c.rotate(rotation)
        val u = cell
        path.reset()
        path.moveTo(-0.16f * u, -0.40f * u)
        path.lineTo(0.16f * u, -0.40f * u)
        path.lineTo(0.16f * u, 0.0f)
        path.lineTo(0.34f * u, 0.0f)
        path.lineTo(0f, 0.42f * u)
        path.lineTo(-0.34f * u, 0.0f)
        path.lineTo(-0.16f * u, 0.0f)
        path.close()
        fill.color = BOARD[color]
        c.drawPath(path, fill)
        stroke.color = Color.parseColor("#1E1E1E")
        stroke.strokeWidth = maxOf(1f, cell / 40f)
        c.drawPath(path, stroke)
        c.restore()
    }

    fun star(c: Canvas, cx: Float, cy: Float, r: Float, color: Int, outline: Int) {
        path.reset()
        for (i in 0 until 10) {
            val ang = Math.PI / 2 + i * Math.PI / 5
            val rad = if (i % 2 == 0) r else r * 0.45f
            val x = cx + (rad * cos(ang)).toFloat()
            val y = cy - (rad * sin(ang)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        fill.style = Paint.Style.FILL
        fill.color = color
        c.drawPath(path, fill)
        stroke.color = outline
        stroke.strokeWidth = maxOf(1f, r / 10f)
        c.drawPath(path, stroke)
    }

    // ------------------------------------------------------------------ pawn

    /**
     * Draws a pawn standing with its base centred at ([cx], [baseY]).
     * [h] = pawn height in px, [glow] 0..1 adds a bright outline.
     */
    fun pawn(c: Canvas, cx: Float, baseY: Float, h: Float, color: Int, glow: Float = 0f) {
        val s = h
        // ground shadow
        fill.style = Paint.Style.FILL
        fill.shader = null
        fill.color = 0x44000000
        rect.set(cx - 0.5f * s * 0.62f, baseY - 0.03f * s, cx + 0.5f * s * 0.62f, baseY + 0.09f * s)
        c.drawOval(rect, fill)

        val main = PAWN[color]
        val dark = PAWN_DARK[color]

        // base
        fill.color = dark
        rect.set(cx - 0.31f * s, baseY - 0.14f * s, cx + 0.31f * s, baseY + 0.02f * s)
        c.drawOval(rect, fill)

        // body
        path.reset()
        path.moveTo(cx - 0.27f * s, baseY - 0.07f * s)
        path.cubicTo(
            cx - 0.20f * s, baseY - 0.28f * s,
            cx - 0.10f * s, baseY - 0.40f * s,
            cx - 0.085f * s, baseY - 0.62f * s
        )
        path.lineTo(cx + 0.085f * s, baseY - 0.62f * s)
        path.cubicTo(
            cx + 0.10f * s, baseY - 0.40f * s,
            cx + 0.20f * s, baseY - 0.28f * s,
            cx + 0.27f * s, baseY - 0.07f * s
        )
        path.close()
        fill.shader = LinearGradient(
            cx - 0.27f * s, 0f, cx + 0.27f * s, 0f,
            intArrayOf(lighten(main, 0.35f), main, dark),
            floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP
        )
        c.drawPath(path, fill)
        fill.shader = null

        // collar
        fill.color = main
        rect.set(cx - 0.15f * s, baseY - 0.67f * s, cx + 0.15f * s, baseY - 0.57f * s)
        c.drawOval(rect, fill)

        // head
        val hx = cx
        val hy = baseY - 0.78f * s
        val hr = 0.145f * s
        fill.shader = LinearGradient(
            hx - hr, hy - hr, hx + hr, hy + hr,
            lighten(main, 0.55f), dark, Shader.TileMode.CLAMP
        )
        c.drawCircle(hx, hy, hr, fill)
        fill.shader = null
        fill.color = 0x66FFFFFF
        c.drawCircle(hx - hr * 0.35f, hy - hr * 0.35f, hr * 0.28f, fill)

        // outline
        stroke.color = dark
        stroke.strokeWidth = maxOf(1.2f, s / 28f)
        c.drawPath(path, stroke)
        c.drawCircle(hx, hy, hr, stroke)

        if (glow > 0f) {
            stroke.color = Color.argb((200 * glow).toInt().coerceIn(0, 255), 255, 255, 255)
            stroke.strokeWidth = maxOf(2f, s / 16f)
            c.drawCircle(hx, hy, hr + stroke.strokeWidth * 0.2f, stroke)
        }
    }

    fun lighten(color: Int, amt: Float): Int {
        val r = Color.red(color) + ((255 - Color.red(color)) * amt).toInt()
        val g = Color.green(color) + ((255 - Color.green(color)) * amt).toInt()
        val b = Color.blue(color) + ((255 - Color.blue(color)) * amt).toInt()
        return Color.rgb(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
    }

    // ------------------------------------------------------------------ dice

    private val PIPS = arrayOf(
        intArrayOf(),
        intArrayOf(4),
        intArrayOf(0, 8),
        intArrayOf(0, 4, 8),
        intArrayOf(0, 2, 6, 8),
        intArrayOf(0, 2, 4, 6, 8),
        intArrayOf(0, 2, 3, 5, 6, 8)
    )

    /** Draws a die centred at ([cx],[cy]) with side [size]. */
    fun dice(c: Canvas, cx: Float, cy: Float, size: Float, value: Int, rotation: Float, border: Int, glow: Float) {
        c.save()
        c.translate(cx, cy)
        c.rotate(rotation)
        val h = size / 2f
        val rad = size * 0.2f

        // shadow
        fill.style = Paint.Style.FILL
        fill.shader = null
        fill.color = 0x55000000
        rect.set(-h + size * 0.04f, -h + size * 0.08f, h + size * 0.04f, h + size * 0.10f)
        c.drawRoundRect(rect, rad, rad, fill)

        if (glow > 0f) {
            fill.color = Color.argb((120 * glow).toInt().coerceIn(0, 255), Color.red(border), Color.green(border), Color.blue(border))
            rect.set(-h - size * 0.12f, -h - size * 0.12f, h + size * 0.12f, h + size * 0.12f)
            c.drawRoundRect(rect, rad * 1.4f, rad * 1.4f, fill)
        }

        rect.set(-h, -h, h, h)
        fill.shader = LinearGradient(-h, -h, h, h, Color.WHITE, Color.parseColor("#D9DEE8"), Shader.TileMode.CLAMP)
        c.drawRoundRect(rect, rad, rad, fill)
        fill.shader = null

        stroke.color = border
        stroke.strokeWidth = maxOf(2f, size / 12f)
        rect.set(-h + stroke.strokeWidth / 2, -h + stroke.strokeWidth / 2, h - stroke.strokeWidth / 2, h - stroke.strokeWidth / 2)
        c.drawRoundRect(rect, rad, rad, stroke)

        val v = value.coerceIn(0, 6)
        val step = size * 0.25f
        val pr = size * 0.075f
        fill.color = Color.parseColor("#14213D")
        for (p in PIPS[v]) {
            val gx = (p % 3 - 1) * step
            val gy = (p / 3 - 1) * step
            c.drawCircle(gx, gy, pr, fill)
        }
        c.restore()
    }

    // ------------------------------------------------------------------ avatar

    /** Round player avatar. Human = person, CPU = robot. */
    fun avatar(c: Canvas, cx: Float, cy: Float, r: Float, color: Int, cpu: Boolean) {
        // ring
        fill.style = Paint.Style.FILL
        fill.shader = null
        fill.color = Color.WHITE
        c.drawCircle(cx, cy, r + r * 0.09f, fill)

        path.reset()
        path.addCircle(cx, cy, r, Path.Direction.CW)
        c.save()
        c.clipPath(path)

        fill.color = lighten(BOARD[color], 0.25f)
        c.drawCircle(cx, cy, r, fill)

        if (!cpu) {
            // shoulders
            fill.color = Color.parseColor("#2B3A67")
            rect.set(cx - r * 0.85f, cy + r * 0.45f, cx + r * 0.85f, cy + r * 1.6f)
            c.drawOval(rect, fill)
            // neck + head
            fill.color = Color.parseColor("#F2BE98")
            rect.set(cx - r * 0.16f, cy + r * 0.2f, cx + r * 0.16f, cy + r * 0.62f)
            c.drawRect(rect, fill)
            rect.set(cx - r * 0.36f, cy - r * 0.55f, cx + r * 0.36f, cy + r * 0.38f)
            c.drawOval(rect, fill)
            // hair
            fill.color = if (color % 2 == 0) Color.parseColor("#4A2C17") else Color.parseColor("#1C1C1C")
            rect.set(cx - r * 0.42f, cy - r * 0.72f, cx + r * 0.42f, cy + r * 0.02f)
            c.drawArc(rect, 180f, 180f, true, fill)
            // eyes
            fill.color = Color.parseColor("#1C1C1C")
            c.drawCircle(cx - r * 0.14f, cy - r * 0.1f, r * 0.045f, fill)
            c.drawCircle(cx + r * 0.14f, cy - r * 0.1f, r * 0.045f, fill)
        } else {
            // robot head
            fill.color = Color.parseColor("#C9D3E0")
            rect.set(cx - r * 0.55f, cy - r * 0.45f, cx + r * 0.55f, cy + r * 0.55f)
            c.drawRoundRect(rect, r * 0.2f, r * 0.2f, fill)
            fill.color = Color.parseColor("#7C8AA5")
            c.drawRect(cx - r * 0.03f, cy - r * 0.75f, cx + r * 0.03f, cy - r * 0.45f, fill)
            c.drawCircle(cx, cy - r * 0.78f, r * 0.08f, fill)
            fill.color = Color.parseColor("#0B1F4B")
            rect.set(cx - r * 0.42f, cy - r * 0.3f, cx + r * 0.42f, cy + r * 0.15f)
            c.drawRoundRect(rect, r * 0.12f, r * 0.12f, fill)
            fill.color = PAWN[color]
            c.drawCircle(cx - r * 0.18f, cy - r * 0.08f, r * 0.09f, fill)
            c.drawCircle(cx + r * 0.18f, cy - r * 0.08f, r * 0.09f, fill)
            fill.color = Color.parseColor("#7C8AA5")
            rect.set(cx - r * 0.25f, cy + r * 0.28f, cx + r * 0.25f, cy + r * 0.38f)
            c.drawRoundRect(rect, r * 0.05f, r * 0.05f, fill)
        }
        c.restore()
    }
}
