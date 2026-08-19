package com.example.youtuberssreader

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.youtuberssreader.databinding.ItemVideoBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VideoAdapter(
    private val videos: List<Video>,
    private val onItemClick: (Video) -> Unit
) : RecyclerView.Adapter<VideoAdapter.VideoViewHolder>() {

    inner class VideoViewHolder(private val binding: ItemVideoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(video: Video) {
            binding.videoTitle.text = video.title
            binding.videoViews.text = formatViews(video.views)
            binding.videoDate.text = formatDate(video.published)

            Glide.with(binding.root)
                .load(video.thumbnail)
                .centerCrop()
                .into(binding.thumbnail)

            binding.root.setOnClickListener { onItemClick(video) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val binding = ItemVideoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VideoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        holder.bind(videos[position])
    }

    override fun getItemCount(): Int = videos.size

    private fun formatViews(views: String): String {
        val count = views.toLongOrNull() ?: 0
        return when {
            count >= 1000000 -> String.format(Locale.US, "%.1fM views", count / 1000000.0)
            count >= 1000 -> String.format(Locale.US, "%.1fK views", count / 1000.0)
            else -> "$count views"
        }
    }

    private fun formatDate(published: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            val date = sdf.parse(published) ?: return published
            val diff = Date().time - date.time
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
