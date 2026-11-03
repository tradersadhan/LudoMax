package com.ludomax.game.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.sin
import kotlin.random.Random

/** Animated menu background: deep-blue gradient with slowly floating dice, rings and dots. */
class BackdropView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private class Item(
        val x: Float, val speed: Float, val size: Float, val phase: Float,
        val type: Int, val color: Int, val spin: Float
    )

    private val items = ArrayList<Item>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private var shader: Shader? = null
    private val ticker = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 1000
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener { invalidate() }
    }

    init {
        val rnd = Random(7)
        for (i in 0 until 16) {
            items.add(
                Item(
                    rnd.nextFloat(), 0.012f + rnd.nextFloat() * 0.03f, 0.05f + rnd.nextFloat() * 0.09f,
                    rnd.nextFloat(), rnd.nextInt(3), Draw.BOARD[rnd.nextInt(4)], (rnd.nextFloat() - 0.5f) * 40f
                )
            )
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        ticker.start()
    }

    override fun onDetachedFromWindow() {
        ticker.cancel()
        super.onDetachedFromWindow()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        shader = LinearGradient(
            0f, 0f, w * 0.7f, h.toFloat(),
            intArrayOf(Color.parseColor("#031A8A"), Color.parseColor("#0A2FC2"), Color.parseColor("#020B4F")),
            floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(canvas: Canvas) {
        paint.shader = shader
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = null

        val t = SystemClock.uptimeMillis() / 1000f
        val w = width.toFloat()
        val h = height.toFloat()
        for (item in items) {
            val prog = ((item.phase + t * item.speed) % 1f)
            val cy = h * (1.1f - prog * 1.3f)
            val cx = w * item.x + sin(t * 0.6f + item.phase * 6f) * w * 0.03f
            val s = w * item.size
            val a = 70
            paint.color = Color.argb(a, Color.red(item.color), Color.green(item.color), Color.blue(item.color))
            when (item.type) {
                0 -> {
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = s * 0.14f
                    canvas.drawCircle(cx, cy, s * 0.5f, paint)
                }
                1 -> {
                    paint.style = Paint.Style.FILL
                    canvas.drawCircle(cx, cy, s * 0.35f, paint)
                }
                else -> {
                    canvas.save()
                    canvas.rotate(item.spin * t, cx, cy)
                    paint.style = Paint.Style.FILL
                    rect.set(cx - s * 0.5f, cy - s * 0.5f, cx + s * 0.5f, cy + s * 0.5f)
                    canvas.drawRoundRect(rect, s * 0.2f, s * 0.2f, paint)
                    paint.color = Color.argb(150, 255, 255, 255)
                    canvas.drawCircle(cx - s * 0.22f, cy - s * 0.22f, s * 0.07f, paint)
                    canvas.drawCircle(cx, cy, s * 0.07f, paint)
                    canvas.drawCircle(cx + s * 0.22f, cy + s * 0.22f, s * 0.07f, paint)
                    canvas.restore()
                }
            }
        }
    }
}
