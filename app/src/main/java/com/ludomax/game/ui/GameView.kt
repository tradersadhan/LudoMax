package com.ludomax.game.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.SystemClock
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import com.ludomax.game.audio.SoundManager
import com.ludomax.game.engine.Board
import com.ludomax.game.engine.Bot
import com.ludomax.game.engine.LudoEngine
import com.ludomax.game.engine.MovePlan
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/** Settings of one match. [cpu] has one flag per colour index (0 GREEN, 1 RED, 2 BLUE, 3 YELLOW). */
class GameConfig(
    val players: List<Int>,
    val cpu: BooleanArray,
    val hardBot: Boolean,
    val vsComputer: Boolean
)

/**
 * The whole game screen: board, pawns, dice, player cards, particles and the turn state machine.
 * Everything is drawn on one canvas so animations stay smooth and the game is fully offline.
 */
class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    interface Listener {
        fun onGameOver(ranking: List<Int>)
    }

    private enum class State { IDLE, WAIT_ROLL, ROLLING, CHOOSE, MOVING, OVER }

    var listener: Listener? = null

    private var cfg: GameConfig? = null
    private var engine: LudoEngine? = null
    private var state = State.IDLE
    private var paused = false
    private var gen = 0
    private val rnd = Random(System.nanoTime())

    // ---- dice / turn data
    private val diceValue = IntArray(4) { 0 }
    private var rollStart = 0L
    private var rollFace = 1
    private var lastFaceChange = 0L
    private var settleStart = -10000L
    private var legalIds: List<Int> = emptyList()
    private var currentDice = 0

    // ---- animation overrides (per token key = color * 2 + id); NaN = not animating
    private val ovX = FloatArray(8) { Float.NaN }
    private val ovY = FloatArray(8) { Float.NaN }
    private val ovLift = FloatArray(8)
    private val ovScale = FloatArray(8) { 1f }
    private val activeAnims = ArrayList<Animator>()
    private val pendingRunnables = ArrayList<Runnable>()

    // ---- message banner
    private var message = ""
    private var messageColor = Color.WHITE
    private var messageTime = 0L

    // ---- screen shake
    private var shakeUntil = 0L

    // ---- particles
    private class Particle(
        var x: Float, var y: Float, var vx: Float, var vy: Float,
        var life: Float, val maxLife: Float, val color: Int, val size: Float, val kind: Int
    )

    private val particles = ArrayList<Particle>()
    private var lastFrame = 0L

    // ---- layout
    private var bs = 0f
    private var bx = 0f
    private var by = 0f
    private var cell = 0f
    private var cardH = 0f
    private var msgY = 0f
    private val avatarCx = FloatArray(4)
    private val avatarCy = FloatArray(4)
    private var avatarR = 0f
    private val diceCx = FloatArray(4)
    private val diceCy = FloatArray(4)
    private var diceSize = 0f
    private var boardBitmap: Bitmap? = null
    private var bgShader: Shader? = null
    private var glowShader: Shader? = null

    // ---- paints
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    private val rectF = RectF()

    private val ticker = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 1000
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener { invalidate() }
    }

    // ===================================================================== public API

    fun startGame(config: GameConfig) {
        cancelEverything()
        cfg = config
        engine = LudoEngine(config.players)
        for (i in 0 until 4) diceValue[i] = 0
        for (i in 0 until 8) {
            ovX[i] = Float.NaN
            ovY[i] = Float.NaN
            ovLift[i] = 0f
            ovScale[i] = 1f
        }
        particles.clear()
        legalIds = emptyList()
        state = State.IDLE
        showMessage("Let's play!", Color.WHITE)
        SoundManager.play(SoundManager.Sfx.CLICK)
        val g = gen
        later(700) { if (g == gen) beginTurn() }
        invalidate()
    }

    fun setPaused(p: Boolean) {
        if (paused == p) return
        paused = p
        if (p) {
            try { ticker.pause() } catch (_: Exception) {}
            for (a in ArrayList(activeAnims)) try { a.pause() } catch (_: Exception) {}
        } else {
            try { if (ticker.isPaused) ticker.resume() else if (!ticker.isRunning) ticker.start() } catch (_: Exception) {}
            for (a in ArrayList(activeAnims)) try { a.resume() } catch (_: Exception) {}
            lastFrame = SystemClock.uptimeMillis()
        }
        invalidate()
    }

    // ===================================================================== lifecycle

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!ticker.isStarted) ticker.start()
        lastFrame = SystemClock.uptimeMillis()
    }

    override fun onDetachedFromWindow() {
        cancelEverything()
        ticker.cancel()
        super.onDetachedFromWindow()
    }

    private fun cancelEverything() {
        gen++
        for (r in pendingRunnables) removeCallbacks(r)
        pendingRunnables.clear()
        for (a in ArrayList(activeAnims)) a.cancel()
        activeAnims.clear()
    }

    private fun later(ms: Long, block: () -> Unit) {
        val r = object : Runnable {
            override fun run() {
                pendingRunnables.remove(this)
                if (paused) {
                    pendingRunnables.add(this)
                    postDelayed(this, 150)
                    return
                }
                block()
            }
        }
        pendingRunnables.add(r)
        postDelayed(r, ms)
    }

    // ===================================================================== layout

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w == 0 || h == 0) return
        val topReserve = Ui.dp(context, 56f)
        val msgH = Ui.dp(context, 54f)
        val gap = Ui.dp(context, 8f)
        cardH = w * 0.2f
        bs = min(w.toFloat(), h - topReserve - 2 * cardH - msgH - gap)
        bs = max(bs, w * 0.5f)
        cell = bs / 9f
        bx = (w - bs) / 2f
        val content = cardH + bs + cardH + msgH
        val y0 = topReserve + max(0f, (h - topReserve - content) / 2f)
        by = y0 + cardH
        msgY = by + bs + cardH + msgH * 0.55f

        avatarR = cardH * 0.36f
        diceSize = cardH * 0.62f
        val topRowY = y0 + cardH * 0.5f
        val botRowY = by + bs + cardH * 0.5f
        val pad = Ui.dp(context, 8f)
        // GREEN top-right, RED bottom-right, BLUE bottom-left, YELLOW top-left
        place(Board.GREEN, false, topRowY, w, pad)
        place(Board.RED, false, botRowY, w, pad)
        place(Board.BLUE, true, botRowY, w, pad)
        place(Board.YELLOW, true, topRowY, w, pad)

        val bmpSize = bs.toInt().coerceAtLeast(10)
        val bmp = Bitmap.createBitmap(bmpSize, bmpSize, Bitmap.Config.ARGB_8888)
        Draw.drawBoard(Canvas(bmp), bmpSize.toFloat())
        boardBitmap = bmp

        bgShader = LinearGradient(
            0f, 0f, w * 0.6f, h.toFloat(),
            intArrayOf(Color.parseColor("#031A8A"), Color.parseColor("#0A2FC2"), Color.parseColor("#020B4F")),
            floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP
        )
        glowShader = RadialGradient(
            w * 0.2f, h * 0.12f, w * 0.9f,
            0x3340A0FF, 0x00000000, Shader.TileMode.CLAMP
        )
    }

    private fun place(color: Int, left: Boolean, rowY: Float, w: Int, pad: Float) {
        avatarCy[color] = rowY
        diceCy[color] = rowY
        if (left) {
            avatarCx[color] = bx + avatarR + pad
            diceCx[color] = avatarCx[color] + avatarR + diceSize * 0.5f + pad * 1.6f
        } else {
            avatarCx[color] = w - bx - avatarR - pad
            diceCx[color] = avatarCx[color] - avatarR - diceSize * 0.5f - pad * 1.6f
        }
    }

    // ===================================================================== turn flow

    private fun isCpu(color: Int): Boolean = cfg?.cpu?.get(color) == true

    private fun beginTurn() {
        val e = engine ?: return
        if (state == State.OVER) return
        val color = e.current
        state = State.WAIT_ROLL
        legalIds = emptyList()
        if (isCpu(color)) {
            showMessage("${Board.COLOR_NAMES[color]} (CPU) is playing...", Draw.BOARD[color])
            val g = gen
            later(850) { if (g == gen) rollDice() }
        } else {
            val who = if (cfg?.vsComputer == true) "YOUR TURN - tap the dice" else "${Board.COLOR_NAMES[color]}: tap the dice"
            showMessage(who, Draw.BOARD[color])
        }
    }

    private fun rollDice() {
        if (state != State.WAIT_ROLL) return
        state = State.ROLLING
        rollStart = SystemClock.uptimeMillis()
        lastFaceChange = 0L
        SoundManager.play(SoundManager.Sfx.DICE)
        val g = gen
        later(850) { if (g == gen) finishRoll() }
        invalidate()
    }

    private fun finishRoll() {
        val e = engine ?: return
        val color = e.current
        state = State.MOVING // busy until the player (or CPU) picks a pawn
        val v = 1 + rnd.nextInt(6)
        diceValue[color] = v
        currentDice = v
        settleStart = SystemClock.uptimeMillis()

        if (v == 6) e.consecutiveSixes++ else e.consecutiveSixes = 0
        if (v == 6) SoundManager.play(SoundManager.Sfx.SIX)

        val g = gen
        if (e.consecutiveSixes >= 3) {
            showMessage("Three 6s in a row! Turn lost", Color.parseColor("#FF8A80"))
            SoundManager.play(SoundManager.Sfx.NO_MOVE)
            later(1300) {
                if (g == gen) {
                    e.advanceTurn()
                    beginTurn()
                }
            }
            return
        }

        val moves = e.legalMoves(color, v)
        legalIds = moves
        if (moves.isEmpty()) {
            showMessage("No move possible", Color.parseColor("#FFD180"))
            SoundManager.play(SoundManager.Sfx.NO_MOVE)
            later(1000) {
                if (g == gen) {
                    e.advanceTurn()
                    beginTurn()
                }
            }
            return
        }

        if (moves.size == 1) {
            later(if (isCpu(color)) 650 else 450) { if (g == gen) doMove(moves[0]) }
        } else if (isCpu(color)) {
            val choice = Bot.choose(e, color, v, moves, cfg?.hardBot == true, rnd)
            later(800) { if (g == gen) doMove(choice) }
        } else {
            state = State.CHOOSE
            showMessage("Choose a pawn to move", Draw.BOARD[color])
        }
    }

    private fun doMove(tokenId: Int) {
        val e = engine ?: return
        val plan = e.plan(e.current, tokenId, currentDice)
        state = State.MOVING
        legalIds = emptyList()
        animateMove(plan) { afterMove(plan) }
    }

    private fun afterMove(plan: MovePlan) {
        val e = engine ?: return
        val g = gen
        // Result of this move
        if (plan.captures) {
            showMessage("Captured! Roll again", Color.parseColor("#FF8A80"))
        }
        if (plan.reachesHome) {
            SoundManager.play(SoundManager.Sfx.HOME)
            burst(centerX(plan.color), centerY(plan.color), 26, true)
            if (!plan.captures) showMessage("Home! Roll again", Color.parseColor("#B9F6CA"))
        }

        if (e.isGameOver) {
            state = State.OVER
            val ranking = e.finalRanking()
            showMessage("${Board.COLOR_NAMES[ranking[0]]} WINS!", Draw.BOARD[ranking[0]])
            SoundManager.play(SoundManager.Sfx.WIN)
            confetti()
            later(2200) { if (g == gen) listener?.onGameOver(ranking) }
            return
        }

        if (e.isFinished(plan.color)) {
            val place = e.ranking.indexOf(plan.color) + 1
            showMessage("${Board.COLOR_NAMES[plan.color]} finished #$place", Draw.BOARD[plan.color])
            confetti()
        }

        val extra = (plan.dice == 6 || plan.captures || plan.reachesHome) && !e.isFinished(plan.color)
        later(if (extra) 450 else 350) {
            if (g != gen) return@later
            if (extra) {
                if (!plan.captures && !plan.reachesHome) showMessage("Rolled a 6! Roll again", Color.parseColor("#FFE082"))
            } else {
                e.advanceTurn()
            }
            beginTurn()
        }
    }

    // ===================================================================== animation

    private fun key(color: Int, id: Int) = color * 2 + id

    private fun register(a: Animator) {
        activeAnims.add(a)
        if (paused) try { a.pause() } catch (_: Exception) {}
    }

    private fun animateMove(plan: MovePlan, onDone: () -> Unit) {
        val e = engine ?: return
        val k = key(plan.color, plan.tokenId)
        val pts = ArrayList<FloatArray>()
        for (p in plan.steps) pts.add(Board.cellCenter(plan.color, plan.tokenId, p))
        val start = Board.cellCenter(plan.color, plan.tokenId, plan.from)
        if (plan.leavesYard) SoundManager.play(SoundManager.Sfx.OUT)
        ovScale[k] = 1f

        fun finishAll() {
            // Captures first (fly back to yard), then commit
            val g = gen
            val done = {
                if (g == gen) {
                    e.commit(plan)
                    ovX[k] = Float.NaN
                    ovY[k] = Float.NaN
                    ovLift[k] = 0f
                    for (c in plan.captured) {
                        val ck = key(c[0], c[1])
                        ovX[ck] = Float.NaN
                        ovY[ck] = Float.NaN
                        ovLift[ck] = 0f
                    }
                    onDone()
                }
            }
            if (plan.captured.isEmpty()) {
                done()
            } else {
                SoundManager.play(SoundManager.Sfx.CAPTURE)
                shakeUntil = SystemClock.uptimeMillis() + 350
                val here = pts.last()
                burst(here[0], here[1], 22, false)
                val starts = ArrayList<FloatArray>()
                val ends = ArrayList<FloatArray>()
                for (c in plan.captured) {
                    starts.add(Board.cellCenter(c[0], c[1], e.pos[c[0]][c[1]]))
                    ends.add(Board.cellCenter(c[0], c[1], Board.YARD))
                }
                val anim = ValueAnimator.ofFloat(0f, 1f)
                anim.duration = 600
                anim.interpolator = AccelerateDecelerateInterpolator()
                anim.addUpdateListener { va ->
                    val t = va.animatedFraction
                    for (i in plan.captured.indices) {
                        val ck = key(plan.captured[i][0], plan.captured[i][1])
                        ovX[ck] = starts[i][0] + (ends[i][0] - starts[i][0]) * t
                        ovY[ck] = starts[i][1] + (ends[i][1] - starts[i][1]) * t
                        ovLift[ck] = (sin(PI * t) * 1.6).toFloat()
                    }
                    invalidate()
                }
                anim.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        activeAnims.remove(animation)
                        done()
                    }
                })
                register(anim)
                anim.start()
            }
        }

        fun hop(from: FloatArray, i: Int) {
            if (i >= pts.size) {
                finishAll()
                return
            }
            val to = pts[i]
            val g = gen
            val a = ValueAnimator.ofFloat(0f, 1f)
            a.duration = if (plan.leavesYard) 420L else if (pts.size > 4) 120L else 160L
            a.interpolator = if (plan.leavesYard) AccelerateDecelerateInterpolator() else LinearInterpolator()
            val lift = if (plan.leavesYard) 1.1f else 0.38f
            a.addUpdateListener { va ->
                val t = va.animatedFraction
                ovX[k] = from[0] + (to[0] - from[0]) * t
                ovY[k] = from[1] + (to[1] - from[1]) * t
                ovLift[k] = (sin(PI * t) * lift).toFloat()
                invalidate()
            }
            if (!plan.leavesYard) {
                SoundManager.play(SoundManager.Sfx.STEP, 0.9f + 0.05f * i)
            }
            a.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    activeAnims.remove(animation)
                    if (g != gen) return
                    hop(to, i + 1)
                }
            })
            register(a)
            a.start()
        }

        ovX[k] = start[0]
        ovY[k] = start[1]
        hop(start, 0)
    }

    private fun centerX(color: Int) = when (color) {
        Board.GREEN -> 4.5f
        Board.RED -> 5.4f
        Board.BLUE -> 4.5f
        else -> 3.6f
    }

    private fun centerY(color: Int) = when (color) {
        Board.GREEN -> 3.6f
        Board.RED -> 4.5f
        Board.BLUE -> 5.4f
        else -> 4.5f
    }

    private fun showMessage(text: String, color: Int) {
        message = text
        messageColor = color
        messageTime = SystemClock.uptimeMillis()
        invalidate()
    }

    // ===================================================================== particles

    private fun burst(cellX: Float, cellY: Float, count: Int, golden: Boolean) {
        val px = bx + cellX * cell
        val py = by + cellY * cell
        val colors = if (golden)
            intArrayOf(Color.parseColor("#FFD54F"), Color.WHITE, Color.parseColor("#FFB300"))
        else
            intArrayOf(Color.parseColor("#FF5252"), Color.parseColor("#FFAB40"), Color.WHITE)
        for (i in 0 until count) {
            val ang = rnd.nextFloat() * (2 * PI).toFloat()
            val sp = cell * (2.2f + rnd.nextFloat() * 4.5f)
            particles.add(
                Particle(
                    px, py, cos(ang) * sp, sin(ang) * sp,
                    0.7f + rnd.nextFloat() * 0.5f, 1.1f, colors[rnd.nextInt(colors.size)],
                    cell * (0.07f + rnd.nextFloat() * 0.09f), 0
                )
            )
        }
    }

    private fun confetti() {
        val cols = intArrayOf(
            Color.parseColor("#FF5252"), Color.parseColor("#FFD740"), Color.parseColor("#69F0AE"),
            Color.parseColor("#40C4FF"), Color.parseColor("#E040FB"), Color.WHITE
        )
        val w = width.toFloat()
        for (i in 0 until 90) {
            particles.add(
                Particle(
                    rnd.nextFloat() * w, -rnd.nextFloat() * height * 0.25f,
                    (rnd.nextFloat() - 0.5f) * cell * 3f, cell * (3f + rnd.nextFloat() * 5f),
                    2.2f + rnd.nextFloat() * 1.5f, 3.7f, cols[rnd.nextInt(cols.size)],
                    cell * (0.09f + rnd.nextFloat() * 0.1f), 1
                )
            )
        }
    }

    private fun updateParticles(dt: Float) {
        val iter = particles.iterator()
        while (iter.hasNext()) {
            val p = iter.next()
            p.life -= dt
            if (p.life <= 0f) {
                iter.remove()
                continue
            }
            p.x += p.vx * dt
            p.y += p.vy * dt
            if (p.kind == 0) {
                p.vy += cell * 6f * dt
                p.vx *= 0.985f
            } else {
                p.vx += sin(p.y * 0.02f) * cell * 0.6f * dt
            }
        }
    }

    // ===================================================================== drawing

    override fun onDraw(canvas: Canvas) {
        val now = SystemClock.uptimeMillis()
        val dt = ((now - lastFrame).coerceIn(0, 50)) / 1000f
        lastFrame = now
        if (!paused) updateParticles(dt)

        // background
        paint.shader = bgShader
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = glowShader
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = null

        val e = engine
        val c = cfg
        if (e == null || c == null || boardBitmap == null) return

        canvas.save()
        if (now < shakeUntil) {
            val amp = Ui.dp(context, 4f) * ((shakeUntil - now) / 350f)
            canvas.translate((rnd.nextFloat() - 0.5f) * 2 * amp, (rnd.nextFloat() - 0.5f) * 2 * amp)
        }

        drawCards(canvas, e, c, now)

        // board with soft shadow
        paint.color = 0x55000000
        canvas.drawRect(bx + 4f, by + 6f, bx + bs + 4f, by + bs + 6f, paint)
        canvas.drawBitmap(boardBitmap!!, bx, by, null)

        drawTargets(canvas, e, now)
        drawTokens(canvas, e, now)
        drawParticles(canvas)
        canvas.restore()

        drawMessage(canvas, now)
    }

    private fun drawCards(canvas: Canvas, e: LudoEngine, c: GameConfig, now: Long) {
        for (color in c.players) {
            val active = state != State.OVER && e.current == color && !e.isFinished(color)
            val cx = avatarCx[color]
            val cy = avatarCy[color]
            val pulse = (0.5f + 0.5f * sin(now / 220.0).toFloat())

            if (active) {
                paint.style = Paint.Style.FILL
                paint.color = Color.argb((60 + 70 * pulse).toInt(), Color.red(Draw.BOARD[color]), Color.green(Draw.BOARD[color]), Color.blue(Draw.BOARD[color]))
                canvas.drawCircle(cx, cy, avatarR * (1.28f + 0.1f * pulse), paint)
            }
            Draw.avatar(canvas, cx, cy, avatarR, color, c.cpu[color])

            // name chip
            val chipW = avatarR * 1.9f
            val chipH = avatarR * 0.5f
            rectF.set(cx - chipW / 2, cy + avatarR * 0.62f, cx + chipW / 2, cy + avatarR * 0.62f + chipH)
            paint.style = Paint.Style.FILL
            paint.color = 0xCC0B1230.toInt()
            canvas.drawRoundRect(rectF, chipH / 2, chipH / 2, paint)
            textPaint.color = Color.parseColor("#FFE94A")
            textPaint.textSize = chipH * 0.62f
            canvas.drawText(Board.COLOR_NAMES[color], cx, rectF.centerY() + chipH * 0.22f, textPaint)

            // finished badge
            val place = e.ranking.indexOf(color)
            if (place >= 0) {
                paint.color = Color.parseColor("#FFC107")
                canvas.drawCircle(cx + avatarR * 0.8f, cy - avatarR * 0.8f, avatarR * 0.34f, paint)
                textPaint.color = Color.parseColor("#3E2723")
                textPaint.textSize = avatarR * 0.4f
                canvas.drawText("#${place + 1}", cx + avatarR * 0.8f, cy - avatarR * 0.8f + avatarR * 0.14f, textPaint)
            }

            // dice
            var value = diceValue[color]
            var rot = 0f
            var scale = 1f
            var dy = 0f
            var glow = 0f
            if (active && state == State.ROLLING) {
                if (now - lastFaceChange > 70) {
                    rollFace = 1 + rnd.nextInt(6)
                    lastFaceChange = now
                }
                value = rollFace
                val t = (now - rollStart) / 1000f
                rot = (sin(t * 28.0) * 24.0).toFloat()
                dy = -abs(sin(t * 14.0).toFloat()) * diceSize * 0.35f
                scale = 1.12f
            } else if (active && now - settleStart < 350 && state != State.WAIT_ROLL) {
                val t = (now - settleStart) / 350f
                scale = 1f + 0.3f * (1f - t) * cos(t * 9.0).toFloat().coerceAtLeast(0f)
            } else if (active && state == State.WAIT_ROLL && !isCpu(color)) {
                scale = 1f + 0.08f * pulse
                glow = 0.6f + 0.4f * pulse
            }
            if (value == 0) value = 1
            Draw.dice(canvas, diceCx[color], diceCy[color] + dy, diceSize * scale, value, rot, Draw.BOARD[color], glow)
        }
    }

    private fun tokenBase(color: Int, id: Int, e: LudoEngine): FloatArray {
        val k = key(color, id)
        if (!ovX[k].isNaN()) return floatArrayOf(ovX[k], ovY[k])
        return Board.cellCenter(color, id, e.pos[color][id])
    }

    private fun drawTargets(canvas: Canvas, e: LudoEngine, now: Long) {
        if (state != State.CHOOSE) return
        val color = e.current
        val pulse = (0.5f + 0.5f * sin(now / 180.0).toFloat())
        for (id in legalIds) {
            val plan = e.plan(color, id, currentDice)
            val p = Board.cellCenter(color, id, plan.to)
            val px = bx + p[0] * cell
            val py = by + p[1] * cell
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = cell * 0.08f
            paint.color = Color.argb((150 + 90 * pulse).toInt(), 255, 255, 255)
            canvas.drawCircle(px, py, cell * (0.30f + 0.08f * pulse), paint)
            paint.style = Paint.Style.FILL
            paint.color = Color.argb(90, 255, 255, 255)
            canvas.drawCircle(px, py, cell * 0.18f, paint)
        }
    }

    private class TokenDraw(val color: Int, val id: Int, var x: Float, var y: Float, var scale: Float, val lift: Float, val moving: Boolean)

    private fun drawTokens(canvas: Canvas, e: LudoEngine, now: Long) {
        val list = ArrayList<TokenDraw>(8)
        for (color in e.players) {
            for (id in 0 until Board.TOKENS_PER_PLAYER) {
                val k = key(color, id)
                val b = tokenBase(color, id, e)
                list.add(TokenDraw(color, id, b[0], b[1], ovScale[k], ovLift[k], !ovX[k].isNaN()))
            }
        }
        // Share a cell: shrink and spread
        for (i in list.indices) {
            val a = list[i]
            if (a.moving) continue
            var sameBefore = 0
            var sameTotal = 0
            for (j in list.indices) {
                val b = list[j]
                if (b.moving) continue
                if (abs(a.x - b.x) < 0.05f && abs(a.y - b.y) < 0.05f) {
                    sameTotal++
                    if (j < i) sameBefore++
                }
            }
            if (sameTotal > 1) {
                a.scale *= if (sameTotal > 2) 0.62f else 0.78f
                val spread = if (sameTotal > 2) 0.26f else 0.2f
                a.x += (sameBefore - (sameTotal - 1) / 2f) * spread * 2f
            }
        }
        list.sortWith(compareBy<TokenDraw> { it.moving }.thenBy { it.y })

        val pulse = (0.5f + 0.5f * sin(now / 170.0).toFloat())
        val movableColor = e.current
        for (t in list) {
            val canPick = state == State.CHOOSE && t.color == movableColor && legalIds.contains(t.id)
            var h = cell * 0.98f * t.scale
            val px = bx + t.x * cell
            var baseY = by + (t.y + 0.34f) * cell
            baseY -= t.lift * cell
            if (canPick) {
                baseY -= abs(sin(now / 160.0).toFloat()) * cell * 0.16f
                h *= 1f + 0.05f * pulse
            }
            if (t.lift > 0.02f) {
                // shadow stays on the ground
                paint.style = Paint.Style.FILL
                paint.color = 0x33000000
                val gy = by + (t.y + 0.34f) * cell
                rectF.set(px - cell * 0.22f, gy - cell * 0.03f, px + cell * 0.22f, gy + cell * 0.06f)
                canvas.drawOval(rectF, paint)
            }
            Draw.pawn(canvas, px, baseY, h, t.color, if (canPick) 0.6f + 0.4f * pulse else 0f)
        }
    }

    private fun drawParticles(canvas: Canvas) {
        paint.style = Paint.Style.FILL
        for (p in particles) {
            val a = (p.life / p.maxLife).coerceIn(0f, 1f)
            paint.color = Color.argb((255 * min(1f, a * 1.5f)).toInt(), Color.red(p.color), Color.green(p.color), Color.blue(p.color))
            if (p.kind == 0) {
                canvas.drawCircle(p.x, p.y, p.size * (0.4f + a), paint)
            } else {
                rectF.set(p.x - p.size, p.y - p.size * 0.5f, p.x + p.size, p.y + p.size * 0.5f)
                canvas.save()
                canvas.rotate(p.y * 0.8f, p.x, p.y)
                canvas.drawRect(rectF, paint)
                canvas.restore()
            }
        }
    }

    private fun drawMessage(canvas: Canvas, now: Long) {
        if (message.isEmpty()) return
        val age = (now - messageTime) / 1000f
        val pop = if (age < 0.25f) 0.8f + 0.2f * (age / 0.25f) else 1f
        val sz = Ui.dp(context, 17f)
        textPaint.textSize = sz
        val tw = textPaint.measureText(message)
        val pw = tw + sz * 2
        val ph = sz * 2.3f
        canvas.save()
        canvas.scale(pop, pop, width / 2f, msgY)
        rectF.set(width / 2f - pw / 2, msgY - ph / 2, width / 2f + pw / 2, msgY + ph / 2)
        paint.style = Paint.Style.FILL
        paint.color = 0xAA050A26.toInt()
        canvas.drawRoundRect(rectF, ph / 2, ph / 2, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = Ui.dp(context, 2f)
        paint.color = messageColor
        canvas.drawRoundRect(rectF, ph / 2, ph / 2, paint)
        paint.style = Paint.Style.FILL
        textPaint.color = Color.WHITE
        canvas.drawText(message, width / 2f, msgY + sz * 0.35f, textPaint)
        canvas.restore()
    }

    // ===================================================================== touch

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) return true
        if (event.actionMasked != MotionEvent.ACTION_UP) return true
        if (paused) return true
        val e = engine ?: return true
        val x = event.x
        val y = event.y
        val color = e.current

        when (state) {
            State.WAIT_ROLL -> {
                if (!isCpu(color)) {
                    val near = hypot(x - diceCx[color], y - diceCy[color]) < diceSize * 1.1f ||
                        hypot(x - avatarCx[color], y - avatarCy[color]) < avatarR * 1.3f
                    if (near) {
                        SoundManager.play(SoundManager.Sfx.CLICK)
                        rollDice()
                    }
                }
            }
            State.CHOOSE -> {
                var best = -1
                var bestD = cell * 0.85f
                for (id in legalIds) {
                    val b = tokenBase(color, id, e)
                    val px = bx + b[0] * cell
                    val py = by + b[1] * cell
                    val d = hypot(x - px, y - py)
                    if (d < bestD) {
                        bestD = d
                        best = id
                    }
                }
                if (best >= 0) {
                    performClick()
                    doMove(best)
                }
            }
            else -> {}
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
