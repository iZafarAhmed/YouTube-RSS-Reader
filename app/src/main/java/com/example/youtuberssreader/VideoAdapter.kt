package com.example.youtuberssreader

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
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

    class VideoViewHolder(
        val thumbnail: ImageView,
        val videoTitle: TextView,
        val videoDate: TextView,
        val videoViews: TextView,
        view: View
    ) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val context = parent.context
        
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setPadding(16, 16, 16, 16)
        }

        val thumb = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 500)
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        root.addView(thumb)

        val title = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = 16 }
            textSize = 16f
            maxLines = 2
        }
        root.addView(title)

        val metaRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = 8 }
        }
        
        val date = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            textSize = 12f
        }
        metaRow.addView(date)

        val views = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            textSize = 12f
        }
        metaRow.addView(views)
        
        root.addView(metaRow)
        return VideoViewHolder(thumb, title, date, views, root)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val video = videos[position]
        holder.videoTitle.text = video.title
        holder.videoViews.text = formatViews(video.views)
        holder.videoDate.text = formatDate(video.published)

        Glide.with(holder.itemView).load(video.thumbnail).centerCrop().into(holder.thumbnail)
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
        } catch (e: Exception) { published }
    }
}
