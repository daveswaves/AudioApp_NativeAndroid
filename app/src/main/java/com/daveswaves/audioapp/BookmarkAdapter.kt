// BookmarkAdapter.kt
package com.daveswaves.audioapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BookmarkAdapter(
    private val bookmarks: List<BookmarkData>,
    private val onClick: (BookmarkData) -> Unit = {}
) : RecyclerView.Adapter<BookmarkAdapter.BookmarkViewHolder>() {

    inner class BookmarkViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val bookmarkText: TextView = itemView.findViewById(R.id.bookmarkText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookmarkViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bookmark, parent, false)
        return BookmarkViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookmarkViewHolder, position: Int) {
        val bookmark = bookmarks[position]
        
        // Convert milliseconds to mm:ss format
        val totalSeconds = bookmark.position / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val timeText = String.format("%02d:%02d", minutes, seconds)
        
        // Format the display text
        val displayText = "${bookmark.book}\n${bookmark.chapter} - $timeText"
        holder.bookmarkText.text = displayText

        holder.itemView.setOnClickListener {
            onClick(bookmark)
        }
    }

    override fun getItemCount(): Int = bookmarks.size
}
