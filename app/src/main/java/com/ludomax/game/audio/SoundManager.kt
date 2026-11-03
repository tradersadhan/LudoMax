package com.ludomax.game.audio

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import com.ludomax.game.R

/** All game audio: looping background music + short sound effects. Fully offline (bundled files). */
object SoundManager {

    enum class Sfx(val res: Int) {
        CLICK(R.raw.sfx_click),
        DICE(R.raw.sfx_dice),
        STEP(R.raw.sfx_step),
        OUT(R.raw.sfx_out),
        CAPTURE(R.raw.sfx_capture),
        HOME(R.raw.sfx_home),
        SIX(R.raw.sfx_six),
        NO_MOVE(R.raw.sfx_nomove),
        WIN(R.raw.sfx_win)
    }

    private const val PREFS = "ludomax_prefs"
    private const val KEY_MUSIC = "music_on"
    private const val KEY_SFX = "sfx_on"

    private var prefs: SharedPreferences? = null
    private var pool: SoundPool? = null
    private val ids = HashMap<Sfx, Int>()
    private var music: MediaPlayer? = null
    private var appContext: Context? = null
    private var musicWanted = false

    var musicEnabled: Boolean = true
        private set
    var sfxEnabled: Boolean = true
        private set

    fun init(context: Context) {
        if (appContext != null) return
        val app = context.applicationContext
        appContext = app
        prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        musicEnabled = prefs?.getBoolean(KEY_MUSIC, true) ?: true
        sfxEnabled = prefs?.getBoolean(KEY_SFX, true) ?: true

        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val sp = SoundPool.Builder().setMaxStreams(6).setAudioAttributes(attrs).build()
        for (s in Sfx.values()) ids[s] = sp.load(app, s.res, 1)
        pool = sp
    }

    fun play(sfx: Sfx, rate: Float = 1f, volume: Float = 1f) {
        if (!sfxEnabled) return
        val id = ids[sfx] ?: return
        pool?.play(id, volume, volume, 1, 0, rate.coerceIn(0.5f, 2f))
    }

    fun setMusicEnabled(on: Boolean) {
        musicEnabled = on
        prefs?.edit()?.putBoolean(KEY_MUSIC, on)?.apply()
        if (on) startMusic() else pauseMusicInternal()
    }

    fun setSfxEnabled(on: Boolean) {
        sfxEnabled = on
        prefs?.edit()?.putBoolean(KEY_SFX, on)?.apply()
    }

    /** Call from onResume of every activity. */
    fun startMusic() {
        musicWanted = true
        if (!musicEnabled) return
        val ctx = appContext ?: return
        try {
            var mp = music
            if (mp == null) {
                mp = MediaPlayer.create(ctx, R.raw.music_loop)
                mp?.isLooping = true
                mp?.setVolume(0.45f, 0.45f)
                music = mp
            }
            if (mp != null && !mp.isPlaying) mp.start()
        } catch (_: Exception) {
            music = null
        }
    }

    /** Call from onPause of every activity. */
    fun pauseMusic() {
        musicWanted = false
        pauseMusicInternal()
    }

    private fun pauseMusicInternal() {
        try {
            val mp = music
            if (mp != null && mp.isPlaying) mp.pause()
        } catch (_: Exception) {
        }
    }

    /** Lower the music while a game is paused / dialogs are open. */
    fun duck(duck: Boolean) {
        val v = if (duck) 0.18f else 0.45f
        try {
            music?.setVolume(v, v)
        } catch (_: Exception) {
        }
    }
}
