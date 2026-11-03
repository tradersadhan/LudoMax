package com.ludomax.game.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import android.widget.TextView
import com.ludomax.game.BuildConfig
import com.ludomax.game.R
import com.ludomax.game.audio.SoundManager

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Ui.edgeToEdge(this, findViewById(R.id.root))

        findViewById<TextView>(R.id.version).text = "LudoMax v${BuildConfig.VERSION_NAME}  •  100% offline"

        click(R.id.btnCpu) { showSetup(vsComputer = true) }
        click(R.id.btnFriends) { showSetup(vsComputer = false) }
        click(R.id.btnHow) { openPage("how_to_play.html", "How to Play") }
        click(R.id.btnSettings) { showSettings() }
        click(R.id.btnShare) { share() }
        click(R.id.btnPrivacy) { openPage("privacy_policy.html", "Privacy Policy") }
        click(R.id.btnTerms) { openPage("terms_and_conditions.html", "Terms & Conditions") }

        // Entrance animation
        val ids = intArrayOf(R.id.logo, R.id.tagline, R.id.btnCpu, R.id.btnFriends, R.id.btnHow, R.id.btnSettings, R.id.btnShare)
        for ((i, id) in ids.withIndex()) {
            val v = findViewById<View>(id)
            v.alpha = 0f
            v.translationY = 60f
            v.animate().alpha(1f).translationY(0f).setStartDelay(80L * i).setDuration(450).start()
        }
    }

    private fun click(id: Int, action: () -> Unit) {
        findViewById<View>(id).setOnClickListener {
            SoundManager.play(SoundManager.Sfx.CLICK)
            action()
        }
    }

    private fun openPage(asset: String, title: String) {
        val i = Intent(this, LegalActivity::class.java)
        i.putExtra(LegalActivity.EXTRA_ASSET, asset)
        i.putExtra(LegalActivity.EXTRA_TITLE, title)
        startActivity(i)
    }

    private fun share() {
        val send = Intent(Intent.ACTION_SEND)
        send.type = "text/plain"
        send.putExtra(
            Intent.EXTRA_TEXT,
            "Play LudoMax - a fun offline Ludo game! https://play.google.com/store/apps/details?id=" +
                BuildConfig.APPLICATION_ID.removeSuffix(".debug")
        )
        startActivity(Intent.createChooser(send, "Share LudoMax"))
    }

    private fun showSetup(vsComputer: Boolean) {
        var playerCount = 2
        var humanColor = Board_BLUE
        var hard = false

        val kit = DialogKit(this)
        kit.title(if (vsComputer) "Play vs Computer" else "Pass & Play")
        kit.label("Players")
        kit.add(kit.chips(listOf("2 Players", "3 Players", "4 Players"), 0) { playerCount = it + 2 })
        if (vsComputer) {
            kit.label("Your colour")
            kit.add(kit.swatches(humanColor) { humanColor = it })
            kit.label("Computer level")
            kit.add(kit.chips(listOf("Easy", "Hard"), 0) { hard = it == 1 })
        } else {
            kit.text("Friends take turns on the same phone.", Color.parseColor("#CFE3FF"), 13f)
        }
        kit.button("START GAME", R.drawable.bg_btn_green) {
            kit.dismiss()
            val i = Intent(this, GameActivity::class.java)
            i.putExtra(GameActivity.EXTRA_VS_CPU, vsComputer)
            i.putExtra(GameActivity.EXTRA_PLAYERS, playerCount)
            i.putExtra(GameActivity.EXTRA_COLOR, if (vsComputer) humanColor else 0)
            i.putExtra(GameActivity.EXTRA_HARD, hard)
            startActivity(i)
        }
        kit.button("CANCEL", R.drawable.bg_btn_red) { kit.dismiss() }
        kit.show()
    }

    private fun showSettings() {
        val kit = DialogKit(this)
        kit.title("Settings")

        fun row(label: String, checked: Boolean, onChange: (Boolean) -> Unit): View {
            val r = android.widget.LinearLayout(this)
            r.orientation = android.widget.LinearLayout.HORIZONTAL
            r.gravity = android.view.Gravity.CENTER_VERTICAL
            val t = TextView(this)
            t.text = label
            t.setTextColor(Color.WHITE)
            t.textSize = 18f
            r.addView(t, android.widget.LinearLayout.LayoutParams(0, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            val sw = SwitchCompat(this)
            sw.isChecked = checked
            sw.setOnCheckedChangeListener { _, on ->
                onChange(on)
            }
            r.addView(sw)
            r.setPadding(0, Ui.dpi(this, 6f), 0, Ui.dpi(this, 6f))
            return r
        }

        kit.add(row("Music", SoundManager.musicEnabled) { SoundManager.setMusicEnabled(it) }, 10)
        kit.add(row("Sound effects", SoundManager.sfxEnabled) {
            SoundManager.setSfxEnabled(it)
            if (it) SoundManager.play(SoundManager.Sfx.CLICK)
        }, 4)
        kit.button("DONE", R.drawable.bg_btn_green) { kit.dismiss() }
        kit.show()
    }

    override fun onResume() {
        super.onResume()
        SoundManager.startMusic()
    }

    override fun onPause() {
        super.onPause()
        SoundManager.pauseMusic()
    }

    private companion object {
        const val Board_BLUE = 2
    }
}
