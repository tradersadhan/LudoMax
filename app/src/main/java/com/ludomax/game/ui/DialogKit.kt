package com.ludomax.game.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.ludomax.game.R
import com.ludomax.game.audio.SoundManager

/** Small helper to build the game's styled dialogs from code. */
class DialogKit(private val ctx: Context) {

    val root = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        background = androidx.core.content.ContextCompat.getDrawable(ctx, R.drawable.bg_dialog)
        val p = Ui.dpi(ctx, 20f)
        setPadding(p, p, p, p)
    }
    private var dialog: AlertDialog? = null

    fun title(text: String, size: Float = 22f): DialogKit {
        root.addView(TextView(ctx).apply {
            this.text = text
            setTextColor(Color.parseColor("#FFE94A"))
            textSize = size
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, Ui.dpi(ctx, 8f))
        }, lp(top = 0))
        return this
    }

    fun label(text: String): DialogKit {
        root.addView(TextView(ctx).apply {
            this.text = text
            setTextColor(Color.parseColor("#CFE3FF"))
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
        }, lp(top = 12))
        return this
    }

    fun text(text: String, color: Int = Color.WHITE, size: Float = 16f, gravity: Int = Gravity.CENTER): DialogKit {
        root.addView(TextView(ctx).apply {
            this.text = text
            setTextColor(color)
            textSize = size
            this.gravity = gravity
        }, lp(top = 6))
        return this
    }

    fun add(view: View, topDp: Int = 8): DialogKit {
        root.addView(view, lp(top = topDp))
        return this
    }

    fun button(text: String, bg: Int, onClick: () -> Unit): DialogKit {
        root.addView(makeButton(text, bg, onClick), lp(top = 12, height = 52))
        return this
    }

    fun makeButton(text: String, bg: Int, onClick: () -> Unit): TextView = TextView(ctx).apply {
        this.text = text
        setTextColor(Color.WHITE)
        textSize = 16f
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER
        background = androidx.core.content.ContextCompat.getDrawable(ctx, bg)
        setOnClickListener {
            SoundManager.play(SoundManager.Sfx.CLICK)
            onClick()
        }
    }

    fun lp(top: Int = 0, height: Int = ViewGroup.LayoutParams.WRAP_CONTENT): LinearLayout.LayoutParams {
        val h = if (height > 0 && height != ViewGroup.LayoutParams.WRAP_CONTENT) Ui.dpi(ctx, height.toFloat()) else height
        return LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, h).apply {
            topMargin = Ui.dpi(ctx, top.toFloat())
        }
    }

    /** A row of text chips; returns a view and reports the selected index. */
    fun chips(options: List<String>, selected: Int, onSelect: (Int) -> Unit): LinearLayout {
        val row = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        val views = ArrayList<TextView>()
        fun refresh(sel: Int) {
            for ((i, v) in views.withIndex()) {
                if (i == sel) {
                    v.background = Ui.rounded(Color.parseColor("#FFE94A"), 14f, ctx)
                    v.setTextColor(Color.parseColor("#1A237E"))
                } else {
                    v.background = Ui.rounded(0x33FFFFFF, 14f, ctx, 0x66FFFFFF, 1.5f)
                    v.setTextColor(Color.WHITE)
                }
            }
        }
        for ((i, o) in options.withIndex()) {
            val tv = TextView(ctx).apply {
                text = o
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setOnClickListener {
                    SoundManager.play(SoundManager.Sfx.CLICK)
                    refresh(i)
                    onSelect(i)
                }
            }
            views.add(tv)
            row.addView(tv, LinearLayout.LayoutParams(0, Ui.dpi(ctx, 44f), 1f).apply {
                if (i > 0) marginStart = Ui.dpi(ctx, 8f)
            })
        }
        refresh(selected)
        return row
    }

    /** Four colour swatches (GREEN, RED, BLUE, YELLOW). */
    fun swatches(selected: Int, onSelect: (Int) -> Unit): LinearLayout {
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        val views = ArrayList<View>()
        fun refresh(sel: Int) {
            for ((i, v) in views.withIndex()) {
                val d = Ui.circle(Draw.BOARD[i])
                if (i == sel) d.setStroke(Ui.dpi(ctx, 4f), Color.WHITE) else d.setStroke(Ui.dpi(ctx, 1.5f), 0x66FFFFFF)
                v.background = d
                v.scaleX = if (i == sel) 1.12f else 0.92f
                v.scaleY = if (i == sel) 1.12f else 0.92f
            }
        }
        for (i in 0 until 4) {
            val v = View(ctx).apply {
                setOnClickListener {
                    SoundManager.play(SoundManager.Sfx.CLICK)
                    refresh(i)
                    onSelect(i)
                }
            }
            views.add(v)
            row.addView(v, LinearLayout.LayoutParams(Ui.dpi(ctx, 46f), Ui.dpi(ctx, 46f)).apply {
                marginStart = Ui.dpi(ctx, 8f)
                marginEnd = Ui.dpi(ctx, 8f)
            })
        }
        refresh(selected)
        return row
    }

    fun show(cancelable: Boolean = true, onDismiss: (() -> Unit)? = null): AlertDialog {
        val d = AlertDialog.Builder(ctx, R.style.LudoDialog)
            .setView(root)
            .setCancelable(cancelable)
            .create()
        d.setOnDismissListener { onDismiss?.invoke() }
        d.show()
        val w = (ctx.resources.displayMetrics.widthPixels * 0.9f).toInt()
        d.window?.setLayout(w, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog = d
        return d
    }

    fun dismiss() {
        dialog?.dismiss()
    }
}
