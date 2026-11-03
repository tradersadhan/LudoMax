package com.ludomax.game.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import com.ludomax.game.R
import com.ludomax.game.audio.SoundManager

class SplashActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private var started = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        Ui.edgeToEdge(this, findViewById(R.id.root))

        val logo = findViewById<View>(R.id.splashLogo)
        val text = findViewById<View>(R.id.splashText)
        logo.scaleX = 0.5f
        logo.scaleY = 0.5f
        logo.alpha = 0f
        text.alpha = 0f
        logo.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(900)
            .setInterpolator(OvershootInterpolator(1.6f)).start()
        text.animate().alpha(1f).setStartDelay(700).setDuration(600).start()

        handler.postDelayed({ openMenu() }, 2200)
    }

    private fun openMenu() {
        if (started || isFinishing) return
        started = true
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun onResume() {
        super.onResume()
        SoundManager.startMusic()
    }

    override fun onPause() {
        super.onPause()
        SoundManager.pauseMusic()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
