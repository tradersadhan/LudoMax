package com.ludomax.game

import android.app.Application
import com.ludomax.game.audio.SoundManager

class LudoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SoundManager.init(this)
    }
}
