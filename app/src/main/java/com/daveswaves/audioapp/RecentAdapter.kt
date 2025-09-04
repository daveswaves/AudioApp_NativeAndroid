// RecentAdapter.kt
package com.daveswaves.audioapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RecentAdapter(
    private val recentBooks: MutableList<BookData>,
    private val onRemove: (BookData) -> Unit,
    private val onClick: (BookData) -> Unit = {}
) : RecyclerView.Adapter<RecentAdapter.RecentViewHolder>() {

    inner class RecentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val bookTitle: TextView = itemView.findViewById(R.id.bookTitle)
        val bookCover: ImageView = itemView.findViewById(R.id.bookCover)
        val removeButton: Button = itemView.findViewById(R.id.removeRecentsBtn)

        init {
            // Handle item tap
            itemView.setOnClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onClick(recentBooks[pos])
                }
            }

            // Handle ❌ tap
            removeButton.setOnClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    val book = recentBooks[pos]
                    onRemove(book)
                    recentBooks.removeAt(pos)
                    notifyItemRemoved(pos)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_recents, parent, false)
        return RecentViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecentViewHolder, position: Int) {
        val bookData = recentBooks[position]
        holder.bookTitle.text = bookData.name

        // Load cover or default
        if (bookData.coverUri != null) {
            holder.bookCover.setImageURI(bookData.coverUri)
        } else {
            holder.bookCover.setImageResource(R.drawable.books)
        }
    }

    override fun getItemCount(): Int = recentBooks.size
}
