// BookmarkAdapter.kt
package com.daveswaves.audioapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BookmarkAdapter(
    private val bookmarks: MutableList<BookmarkData>,
    private val onRemove: (BookmarkData) -> Unit,
    private val onClick: (BookmarkData) -> Unit = {}
) : RecyclerView.Adapter<BookmarkAdapter.BookmarkViewHolder>() {

    inner class BookmarkViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val bookmarkText: TextView = itemView.findViewById(R.id.bookmarkText)
        val removeButton: Button = itemView.findViewById(R.id.removeBookmarksBtn)

        init {
            // Handle item tap
            itemView.setOnClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onClick(bookmarks[pos])
                }
            }

            // Handle ❌ tap
            removeButton.setOnClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    val bookmark = bookmarks[pos]
                    onRemove(bookmark)
                    bookmarks.removeAt(pos)
                    notifyItemRemoved(pos)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookmarkViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_bookmarks, parent, false)
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
    }

    override fun getItemCount(): Int = bookmarks.size
}
