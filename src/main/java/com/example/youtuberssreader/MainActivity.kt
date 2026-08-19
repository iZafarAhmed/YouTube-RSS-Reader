package com.example.youtuberssreader

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.youtuberssreader.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.URL

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var videoAdapter: VideoAdapter
    private val videoList = mutableListOf<Video>()

    // Default channels
    private val channels = mapOf(
        "NBA 2K" to "UCW39zufHfsuGgpLviKh297Q",
        "Jynxzi" to "UCjiXtODGCCulmhwypZAWSag",
        "DW Documentary" to "UCW39zufHfsuGgpLviKh297Q"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupSwipeRefresh()
        setupChannelButtons()
        
        // Load default channel
        loadChannel("UCW39zufHfsuGgpLviKh297Q")
    }

    private fun setupRecyclerView() {
        videoAdapter = VideoAdapter(videoList) { video ->
            // Open video in YouTube app
            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.youtube.com/watch?v=${video.id}"))
            startActivity(intent)
        }
        
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = videoAdapter
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            val currentChannel = channels.entries.find { it.value == binding.channelInput.text.toString() }?.value 
                ?: "UCW39zufHfsuGgpLviKh297Q"
            loadChannel(currentChannel)
        }
    }

    private fun setupChannelButtons() {
        binding.btnNba2k.setOnClickListener {
            loadChannel("UCW39zufHfsuGgpLviKh297Q")
        }
        
        binding.btnJynxzi.setOnClickListener {
            loadChannel("UCjiXtODGCCulmhwypZAWSag")
        }
        
        binding.btnLoad.setOnClickListener {
            val channelId = binding.channelInput.text.toString().trim()
            if (channelId.isNotEmpty()) {
                loadChannel(channelId)
            } else {
                Toast.makeText(this, "Please enter a channel ID", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadChannel(channelId: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.swipeRefresh.isRefreshing = true
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val videos = withContext(Dispatchers.IO) {
                    fetchYouTubeRSSFeed(channelId)
                }
                
                videoList.clear()
                videoList.addAll(videos)
                videoAdapter.notifyDataSetChanged()
                
                binding.channelTitle.text = if (videos.isNotEmpty()) videos[0].channelTitle else "No videos"
                
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun fetchYouTubeRSSFeed(channelId: String): List<Video> {
        val videos = mutableListOf<Video>()
        val url = URL("https://www.youtube.com/feeds/videos.xml?channel_id=$channelId")
        
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = false
        val parser = factory.newPullParser()
        
        parser.setInput(url.openStream(), "UTF-8")
        
        var eventType = parser.eventType
        var currentVideo = Video()
        var inEntry = false
        
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "entry" -> {
                            inEntry = true
                            currentVideo = Video()
                        }
                        "videoId" -> if (inEntry) currentVideo.id = parser.nextText()
                        "title" -> if (inEntry) currentVideo.title = parser.nextText()
                        "thumbnail" -> {
                            if (inEntry) {
                                currentVideo.thumbnail = parser.getAttributeValue(null, "url")
                            }
                        }
                        "published" -> if (inEntry) currentVideo.published = parser.nextText()
                        "statistics" -> {
                            if (inEntry) {
                                currentVideo.views = parser.getAttributeValue(null, "views") ?: "0"
                            }
                        }
                        "name" -> {
                            if (inEntry && parser.namespace == "http://www.w3.org/2005/Atom") {
                                currentVideo.channelTitle = parser.nextText()
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "entry" && inEntry) {
                        videos.add(currentVideo)
                        inEntry = false
                    }
                }
            }
            eventType = parser.next()
        }
        
        return videos
    }
}
