package com.example.youtuberssreader

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.net.URL

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var channelTitle: TextView
    private lateinit var channelInput: EditText

    private val videoList = mutableListOf<Video>()
    private lateinit var videoAdapter: VideoAdapter
    private var currentChannelId = "UCW39zufHfsuGgpLviKh297Q"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }

        channelInput = EditText(this).apply {
            hint = "Enter Channel ID"
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(32, 32, 32, 16) }
        }
        rootLayout.addView(channelInput)

        val btnLoad = Button(this).apply {
            text = "Load"
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(32, 0, 32, 16) }
        }
        rootLayout.addView(btnLoad)

        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(32, 0, 32, 16) }
        }
        val btnNba2k = Button(this).apply { text = "NBA 2K"; layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) }
        val btnJynxzi = Button(this).apply { text = "Jynxzi"; layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) }
        btnRow.addView(btnNba2k)
        btnRow.addView(btnJynxzi)
        rootLayout.addView(btnRow)

        channelTitle = TextView(this).apply {
            text = "Channel Title"
            textSize = 18f
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(32, 16, 32, 16) }
        }
        rootLayout.addView(channelTitle)

        val frameLayout = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }

        swipeRefresh = SwipeRefreshLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        }
        
        recyclerView = RecyclerView(this).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
        swipeRefresh.addView(recyclerView)
        frameLayout.addView(swipeRefresh)

        progressBar = ProgressBar(this).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply { gravity = Gravity.CENTER }
            visibility = View.GONE
        }
        frameLayout.addView(progressBar)

        rootLayout.addView(frameLayout)
        setContentView(rootLayout)

        videoAdapter = VideoAdapter(videoList) { video ->
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=" + video.id)))
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = videoAdapter

        btnLoad.setOnClickListener {
            val id = channelInput.text.toString().trim()
            if (id.isNotEmpty()) loadChannel(id)
            else Toast.makeText(this, "Enter a channel ID", Toast.LENGTH_SHORT).show()
        }
        btnNba2k.setOnClickListener { loadChannel("UCW39zufHfsuGgpLviKh297Q") }
        btnJynxzi.setOnClickListener { loadChannel("UCjiXtODGCCulmhwypZAWSag") }
        swipeRefresh.setOnRefreshListener { loadChannel(currentChannelId) }

        loadChannel(currentChannelId)
    }

    private fun loadChannel(channelId: String) {
        currentChannelId = channelId
        progressBar.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val videos = withContext(Dispatchers.IO) { fetchFeed(channelId) }
                videoList.clear()
                videoList.addAll(videos)
                videoAdapter.notifyDataSetChanged()
                channelTitle.text = videos.firstOrNull()?.channelTitle ?: "No videos found"
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error: " + e.message, Toast.LENGTH_LONG).show()
            } finally {
                progressBar.visibility = View.GONE
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun fetchFeed(channelId: String): List<Video> {
        val videos = mutableListOf<Video>()
        var channelTitleText = ""
        try {
            val parser = XmlPullParserFactory.newInstance().newPullParser()
            parser.setInput(URL("https://www.youtube.com/feeds/videos.xml?channel_id=$channelId").openStream(), "UTF-8")
            var inEntry = false
            var video = Video()
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG) {
                    val tag = parser.name ?: ""
                    when {
                        tag == "entry" -> { inEntry = true; video = Video() }
                        tag == "title" && !inEntry -> channelTitleText = parser.nextText()
                        tag == "title" && inEntry -> video.title = parser.nextText()
                        tag.endsWith("videoId") && inEntry -> video.id = parser.nextText()
                        tag.endsWith("thumbnail") && inEntry -> video.thumbnail = parser.getAttributeValue(null, "url") ?: ""
                        tag == "published" && inEntry -> video.published = parser.nextText()
                        tag.endsWith("statistics") && inEntry -> video.views = parser.getAttributeValue(null, "views") ?: "0"
                    }
                    if (inEntry) video.channelTitle = channelTitleText
                } else if (event == XmlPullParser.END_TAG) {
                    if (parser.name == "entry") { videos.add(video); inEntry = false }
                }
                event = parser.next()
            }
        } catch (e: Exception) { throw e }
        return videos
    }
}
