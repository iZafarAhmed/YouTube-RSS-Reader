package com.example.youtuberssreader

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.youtuberssreader.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.net.URL

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val videoList = mutableListOf<Video>()
    private lateinit var videoAdapter: VideoAdapter
    private var currentChannelId = "UCW39zufHfsuGgpLviKh297Q"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        videoAdapter = VideoAdapter(videoList) { video ->
            startActivity(Intent(Intent.ACTION_VIEW,
                Uri.parse("https://www.youtube.com/watch?v=" + video.id)))
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = videoAdapter

        binding.btnLoad.setOnClickListener {
            val id = binding.channelInput.text.toString().trim()
            if (id.isNotEmpty()) loadChannel(id)
            else Toast.makeText(this, "Enter a channel ID", Toast.LENGTH_SHORT).show()
        }
        binding.btnNba2k.setOnClickListener { loadChannel("UCW39zufHfsuGgpLviKh297Q") }
        binding.btnJynxzi.setOnClickListener { loadChannel("UCjiXtODGCCulmhwypZAWSag") }
        binding.swipeRefresh.setOnRefreshListener { loadChannel(currentChannelId) }

        loadChannel(currentChannelId)
    }

    private fun loadChannel(channelId: String) {
        currentChannelId = channelId
        binding.progressBar.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val videos = withContext(Dispatchers.IO) { fetchFeed(channelId) }
                videoList.clear()
                videoList.addAll(videos)
                videoAdapter.notifyDataSetChanged()
                binding.channelTitle.text = videos.firstOrNull()?.channelTitle ?: "No videos found"
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error: " + e.message, Toast.LENGTH_LONG).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun fetchFeed(channelId: String): List<Video> {
        val videos = mutableListOf<Video>()
        var channelTitle = ""

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
                    tag == "title" && !inEntry -> channelTitle = parser.nextText()
                    tag == "title" && inEntry -> video.title = parser.nextText()
                    tag.endsWith("videoId") && inEntry -> video.id = parser.nextText()
                    tag.endsWith("thumbnail") && inEntry ->
                        video.thumbnail = parser.getAttributeValue(null, "url") ?: ""
                    tag == "published" && inEntry -> video.published = parser.nextText()
                    tag.endsWith("statistics") && inEntry ->
                        video.views = parser.getAttributeValue(null, "views") ?: "0"
                }
                if (inEntry) video.channelTitle = channelTitle
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
