package com.example.youtuberssreader

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VideoAdapter(
    private val videos: List<Video>,
    private val onItemClick: (Video) -> Unit
) : RecyclerView.Adapter<VideoAdapter.VideoViewHolder>() {

    class VideoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val thumbnail: ImageView = view.findViewById(R.id.thumbnail)
        val videoTitle: TextView = view.findViewById(R.id.videoTitle)
        val videoDate: TextView = view.findViewById(R.id.videoDate)
        val videoViews: TextView = view.findViewById(R.id.videoViews)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_video, parent, false)
        return VideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val video = videos[position]
        holder.videoTitle.text = video.title
        holder.videoViews.text = formatViews(video.views)
        holder.videoDate.text = formatDate(video.published)

        Glide.with(holder.itemView)
            .load(video.thumbnail)
            .centerCrop()
            .into(holder.thumbnail)

        holder.itemView.setOnClickListener { onItemClick(video) }
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
