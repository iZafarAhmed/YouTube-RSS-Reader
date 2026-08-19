package com.example.youtuberssreader

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.youtuberssreader.databinding.ItemVideoBinding
import java.text.SimpleDateFormat
import java.util.*

class VideoAdapter(
    private val videos: List<Video>,
    private val onItemClick: (Video) -> Unit
) : RecyclerView.Adapter<VideoAdapter.VideoViewHolder>() {

    inner class VideoViewHolder(private val binding: ItemVideoBinding) : 
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(video: Video) {
            binding.videoTitle.text = video.title
            binding.videoViews.text = formatViews(video.views)
            binding.videoDate.text = formatPublishedDate(video.published)
            
            Glide.with(binding.root.context)
                .load(video.thumbnail)
                .centerCrop()
                .into(binding.thumbnail)
            
            binding.root.setOnClickListener { onItemClick(video) }
        }
        
        private fun formatViews(views: String): String {
            val count = views.toLongOrNull() ?: 0
            return when {
                count >= 1000000 -> "${count / 1000000}M views"
                count >= 1000 -> "${count / 1000}K views"
                else -> "$count views"
            }
        }
        
        private fun formatPublishedDate(published: String): String {
            return try {
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss+00:00", Locale.US)
                val date = sdf.parse(published)
                val now = Date()
                val diff = now.time - date.time
                
                when {
                    diff < 60000 -> "Just now"
                    diff < 3600000 -> "${diff / 60000} minutes ago"
                    diff < 86400000 -> "${diff / 3600000} hours ago"
                    diff < 604800000 -> "${diff / 86400000} days ago"
                    else -> SimpleDateFormat("MMM dd, yyyy", Locale.US).format(date)
                }
            } catch (e: Exception) {
                published
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val binding = ItemVideoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VideoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        holder.bind(videos[position])
    }

    override fun getItemCount() = videos.size
}
