package com.example.youtuberssreader

import com.example.youtuberssreader.R  // <--- ADD THIS LINE

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
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
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        progressBar = findViewById(R.id.progressBar)
        channelTitle = findViewById(R.id.channelTitle)
        channelInput = findViewById(R.id.channelInput)
        val btnLoad = findViewById<Button>(R.id.btnLoad)
        val btnNba2k = findViewById<Button>(R.id.btnNba2k)
        val btnJynxzi = findViewById<Button>(R.id.btnJynxzi)

        videoAdapter = VideoAdapter(videoList) { video ->
            startActivity(Intent(Intent.ACTION_VIEW,
                Uri.parse("https://www.youtube.com/watch?v=" + video.id)))
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
                    tag.endsWith("thumbnail") && inEntry ->
                        video.thumbnail = parser.getAttributeValue(null, "url") ?: ""
                    tag == "published" && inEntry -> video.published = parser.nextText()
                    tag.endsWith("statistics") && inEntry ->
                        video.views = parser.getAttributeValue(null, "views") ?: "0"
                }
                if (inEntry) video.channelTitle = channelTitleText
            } else if (event == XmlPullParser.END_TAG) {
                if (parser.name == "entry") {
                    videos.add(video)
                    inEntry = false
                }
            }
            event = parser.next()
        }
        return videos
    }
}
