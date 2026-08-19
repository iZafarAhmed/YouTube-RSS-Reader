package com.example.youtuberssreader

import android.content.Intent
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
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var channelTitle: TextView
    private lateinit var channelInput: EditText
    private lateinit var searchInput: EditText

    private val videoList = mutableListOf<Video>()
    private lateinit var videoAdapter: VideoAdapter
    private var currentChannelId = "UCW39zufHfsuGgpLviKh297Q"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }

        searchInput = EditText(this).apply {
            hint = "Search YouTube videos..."
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(32, 32, 32, 16) }
        }
        rootLayout.addView(searchInput)

        val btnSearch = Button(this).apply {
            text = "Search"
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(32, 0, 32, 32) }
        }
        rootLayout.addView(btnSearch)

        channelTitle = TextView(this).apply {
            text = "Or load a Channel RSS Feed:"
            textSize = 16f
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(32, 0, 32, 16) }
        }
        rootLayout.addView(channelTitle)

        channelInput = EditText(this).apply {
            hint = "Enter Channel ID"
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(32, 0, 32, 16) }
        }
        rootLayout.addView(channelInput)

        val btnLoad = Button(this).apply {
            text = "Load Channel"
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

        val statusTitle = TextView(this).apply {
            text = "Results"
            textSize = 18f
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(32, 16, 32, 16) }
        }
        rootLayout.addView(statusTitle)

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
            val intent = Intent(this, PlayerActivity::class.java)
            intent.putExtra("VIDEO_ID", video.id)
            startActivity(intent)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = videoAdapter

        btnSearch.setOnClickListener {
            val query = searchInput.text.toString().trim()
            if (query.isNotEmpty()) searchVideos(query)
            else Toast.makeText(this, "Enter search query", Toast.LENGTH_SHORT).show()
        }

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

    private fun searchVideos(query: String) {
        progressBar.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val videos = withContext(Dispatchers.IO) { fetchSearchResults(query) }
                videoList.clear()
                videoList.addAll(videos)
                videoAdapter.notifyDataSetChanged()
                channelTitle.text = "Search results for: $query"
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Search failed: " + e.message, Toast.LENGTH_LONG).show()
            } finally {
                progressBar.visibility = View.GONE
                swipeRefresh.isRefreshing = false
            }
        }
    }

    // Tries 6 different servers until one works
    private fun fetchSearchResults(query: String): List<Video> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        var lastError: Exception? = null

        for (base in PIPED_INSTANCES) {
            try {
                val videos = searchPiped("$base/search?q=$encoded&filter=videos")
                if (videos.isNotEmpty()) return videos
            } catch (e: Exception) { lastError = e }
        }

        for (base in INVIDIOUS_INSTANCES) {
            try {
                val videos = searchInvidious(base, "$base/api/v1/search?q=$encoded&type=video")
                if (videos.isNotEmpty()) return videos
            } catch (e: Exception) { lastError = e }
        }

        throw lastError ?: Exception("All search servers are offline")
    }

    private fun searchPiped(url: String): List<Video> {
        val json = JSONObject(httpGet(url))
        val items = json.getJSONArray("items")
        val videos = mutableListOf<Video>()
        for (i in 0 until items.length()) {
            val item = items.getJSONObject(i)
            val id = item.optString("url", "").substringAfter("v=", "")
            if (id.isEmpty()) continue
            videos.add(Video(
                id = id,
                title = item.optString("title", ""),
                thumbnail = item.optString("thumbnail", ""),
                published = item.optString("uploadedDate", ""),
                views = item.optLong("views", 0).toString(),
                channelTitle = item.optString("uploaderName", "")
            ))
        }
        return videos
    }

    private fun searchInvidious(base: String, url: String): List<Video> {
        val arr = JSONArray(httpGet(url))
        val videos = mutableListOf<Video>()
        for (i in 0 until arr.length()) {
            val item = arr.getJSONObject(i)
            if (item.optString("type") != "video") continue
            var thumb = ""
            val thumbs = item.optJSONArray("thumbnails")
            if (thumbs != null && thumbs.length() > 0) {
                thumb = thumbs.getJSONObject(thumbs.length() - 1).optString("url", "")
                if (thumb.startsWith("/")) thumb = base + thumb
            }
            videos.add(Video(
                id = item.optString("videoId", ""),
                title = item.optString("title", ""),
                thumbnail = thumb,
                published = item.optString("publishedText", ""),
                views = item.optLong("viewCount", 0).toString(),
                channelTitle = item.optString("author", "")
            ))
        }
        return videos
    }

    private fun httpGet(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
        try {
            return conn.inputStream.bufferedReader().readText()
        } finally {
            conn.disconnect()
        }
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

    companion object {
        private val PIPED_INSTANCES = listOf(
            "https://pipedapi.kavin.rocks",
            "https://pipedapi.adminforge.de",
            "https://pipedapi.reallyaweso.me"
        )
        private val INVIDIOUS_INSTANCES = listOf(
            "https://inv.nadeko.net",
            "https://invidious.f5.si",
            "https://iv.melmac.space"
        )
    }
}
