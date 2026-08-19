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
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            
            // FIX 1: Spoof a Desktop User-Agent to bypass Error 153 restrictions
            settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
            
            webChromeClient = WebChromeClient()
        }
        
        val videoId = intent.getStringExtra("VIDEO_ID") ?: ""
        
        // FIX 2: Use youtube-nocookie.com to bypass embed blocking
        val embedUrl = "https://www.youtube-nocookie.com/embed/$videoId?autoplay=1&playsinline=1&rel=0"
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
