// BooksAdapter.kt
package com.daveswaves.audioapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.widget.ImageView
// import android.widget.Toast

class BooksAdapter(
    private val books: List<BookData>,
    private val onClick: (BookData) -> Unit = {}
) : RecyclerView.Adapter<BooksAdapter.BookViewHolder>() {

    inner class BookViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val bookTitle: TextView = itemView.findViewById(R.id.bookTitle)
        val bookCover: ImageView = itemView.findViewById(R.id.bookCover)

        init {
            itemView.setOnClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    val book = books[pos]
                    onClick(book)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_books, parent, false)
        return BookViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        val bookData = books[position]
        holder.bookTitle.text = bookData.name
        
        // Load cover image or use default
        if (bookData.coverUri != null) {
            holder.bookCover.setImageURI(bookData.coverUri)
        } else {
            holder.bookCover.setImageResource(R.drawable.books)
        }
        
        holder.itemView.setOnClickListener {
            // Toast.makeText(holder.itemView.context, "Clicked $bookName", Toast.LENGTH_SHORT).show()
            onClick(bookData)
        }
    }

    override fun getItemCount(): Int = books.size
}
