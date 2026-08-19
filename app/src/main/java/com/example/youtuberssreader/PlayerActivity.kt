package com.example.youtuberssreader

import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.LinearLayout
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity

class PlayerActivity : AppCompatActivity() {
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
        
        webView = WebView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false // Allows autoplay
            webChromeClient = WebChromeClient()
        }
        
        val videoId = intent.getStringExtra("VIDEO_ID") ?: ""
        // Load the embed version of the video
        val embedUrl = "https://www.youtube.com/embed/$videoId?autoplay=1&playsinline=1"
        webView.loadUrl(embedUrl)
        
        layout.addView(webView)
        setContentView(layout)
    }
    
    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
