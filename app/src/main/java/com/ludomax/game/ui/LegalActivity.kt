package com.ludomax.game.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.ludomax.game.R
import com.ludomax.game.audio.SoundManager

/** Shows a bundled HTML page (privacy policy, terms, how to play) completely offline. */
class LegalActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_legal)
        Ui.edgeToEdge(this, findViewById(R.id.root))

        val asset = intent.getStringExtra(EXTRA_ASSET) ?: "privacy_policy.html"
        findViewById<TextView>(R.id.title).text = intent.getStringExtra(EXTRA_TITLE) ?: ""
        findViewById<android.view.View>(R.id.btnBack).setOnClickListener {
            SoundManager.play(SoundManager.Sfx.CLICK)
            finish()
        }

        val web = findViewById<WebView>(R.id.web)
        web.setBackgroundColor(0)
        web.settings.javaScriptEnabled = false
        web.settings.allowFileAccess = false
        web.settings.allowContentAccess = false
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url
                if (url.scheme == "mailto") {
                    try {
                        startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse(url.toString())))
                    } catch (_: Exception) {
                    }
                }
                return true // never navigate away from the bundled page
            }
        }
        web.loadUrl("file:///android_asset/$asset")
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
        findViewById<WebView>(R.id.web)?.apply {
            stopLoading()
            loadUrl("about:blank")
            destroy()
        }
        super.onDestroy()
    }

    companion object {
        const val EXTRA_ASSET = "asset"
        const val EXTRA_TITLE = "title"
    }
}
