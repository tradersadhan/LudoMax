package com.ludomax.game.ui

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.ludomax.game.R
import com.ludomax.game.audio.SoundManager
import com.ludomax.game.engine.Board

class GameActivity : AppCompatActivity(), GameView.Listener {

    private lateinit var gameView: GameView
    private lateinit var config: GameConfig
    private var dialogOpen = false
    private lateinit var btnMusic: ImageView
    private lateinit var btnSound: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)
        Ui.edgeToEdge(this, findViewById(R.id.root))

        gameView = findViewById(R.id.gameView)
        gameView.listener = this
        btnMusic = findViewById(R.id.btnMusic)
        btnSound = findViewById(R.id.btnSound)

        config = buildConfig()
        refreshToggles()

        findViewById<View>(R.id.btnPause).setOnClickListener {
            SoundManager.play(SoundManager.Sfx.CLICK)
            showPause()
        }
        btnMusic.setOnClickListener {
            SoundManager.setMusicEnabled(!SoundManager.musicEnabled)
            SoundManager.play(SoundManager.Sfx.CLICK)
            refreshToggles()
        }
        btnSound.setOnClickListener {
            SoundManager.setSfxEnabled(!SoundManager.sfxEnabled)
            SoundManager.play(SoundManager.Sfx.CLICK)
            refreshToggles()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!dialogOpen) showPause()
            }
        })

        // Start once the view has been measured.
        gameView.post { gameView.startGame(config) }
    }

    private fun buildConfig(): GameConfig {
        val vsCpu = intent.getBooleanExtra(EXTRA_VS_CPU, true)
        val count = intent.getIntExtra(EXTRA_PLAYERS, 2).coerceIn(2, 4)
        val human = intent.getIntExtra(EXTRA_COLOR, 2).coerceIn(0, 3)
        val hard = intent.getBooleanExtra(EXTRA_HARD, false)

        val players = ArrayList<Int>()
        if (count == 2) {
            players.add(human)
            players.add((human + 2) % 4)
        } else {
            for (i in 0 until count) players.add((human + i) % 4)
        }
        val cpu = BooleanArray(4)
        if (vsCpu) for (p in players) cpu[p] = p != human
        return GameConfig(players, cpu, hard, vsCpu)
    }

    private fun refreshToggles() {
        btnMusic.alpha = if (SoundManager.musicEnabled) 1f else 0.4f
        btnSound.setImageResource(if (SoundManager.sfxEnabled) R.drawable.ic_sound_on else R.drawable.ic_sound_off)
    }

    private fun showPause() {
        if (dialogOpen) return
        dialogOpen = true
        gameView.setPaused(true)
        SoundManager.duck(true)
        val kit = DialogKit(this)
        kit.title("Game Paused")
        kit.button("RESUME", R.drawable.bg_btn_green) { kit.dismiss() }
        kit.button("RESTART", R.drawable.bg_btn_orange) {
            kit.dismiss()
            gameView.startGame(config)
        }
        kit.button("MAIN MENU", R.drawable.bg_btn_red) {
            kit.dismiss()
            finish()
        }
        kit.show(cancelable = true) {
            dialogOpen = false
            SoundManager.duck(false)
            gameView.setPaused(false)
        }
    }

    override fun onGameOver(ranking: List<Int>) {
        if (isFinishing || dialogOpen) return
        dialogOpen = true
        val winner = ranking[0]
        val title = when {
            !config.vsComputer -> "${Board.COLOR_NAMES[winner]} WINS!"
            !config.cpu[winner] -> "YOU WIN!"
            else -> "COMPUTER WINS"
        }
        val kit = DialogKit(this)
        kit.title(title, 28f)
        for ((i, color) in ranking.withIndex()) {
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.gravity = Gravity.CENTER_VERTICAL
            row.setPadding(Ui.dpi(this, 6f), Ui.dpi(this, 6f), Ui.dpi(this, 6f), Ui.dpi(this, 6f))
            val dot = View(this)
            dot.background = Ui.circle(Draw.BOARD[color])
            row.addView(dot, LinearLayout.LayoutParams(Ui.dpi(this, 22f), Ui.dpi(this, 22f)))
            val tv = TextView(this)
            val who = if (config.vsComputer) (if (config.cpu[color]) " (CPU)" else " (You)") else ""
            tv.text = "${ordinal(i + 1)}   ${Board.COLOR_NAMES[color]}$who"
            tv.setTextColor(if (i == 0) Color.parseColor("#FFE94A") else Color.WHITE)
            tv.textSize = if (i == 0) 20f else 17f
            tv.typeface = android.graphics.Typeface.DEFAULT_BOLD
            val lp = LinearLayout.LayoutParams(0, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            lp.marginStart = Ui.dpi(this, 12f)
            row.addView(tv, lp)
            kit.add(row, 4)
        }
        kit.button("PLAY AGAIN", R.drawable.bg_btn_green) {
            kit.dismiss()
            gameView.startGame(config)
        }
        kit.button("MAIN MENU", R.drawable.bg_btn_red) {
            kit.dismiss()
            finish()
        }
        kit.show(cancelable = false) {
            dialogOpen = false
        }
    }

    private fun ordinal(n: Int) = when (n) {
        1 -> "1st"
        2 -> "2nd"
        3 -> "3rd"
        else -> "${n}th"
    }

    override fun onResume() {
        super.onResume()
        SoundManager.startMusic()
        if (!dialogOpen) gameView.setPaused(false)
    }

    override fun onPause() {
        super.onPause()
        gameView.setPaused(true)
        SoundManager.pauseMusic()
    }

    companion object {
        const val EXTRA_VS_CPU = "vs_cpu"
        const val EXTRA_PLAYERS = "players"
        const val EXTRA_COLOR = "color"
        const val EXTRA_HARD = "hard"
    }
}
