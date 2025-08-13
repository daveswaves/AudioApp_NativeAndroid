// BooksAdapter.kt
package com.daveswaves.audioapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
// import android.widget.Toast

class BooksAdapter(
    private val books: List<String>,
    private val onClick: (String) -> Unit = {}
) : RecyclerView.Adapter<BooksAdapter.BookViewHolder>() {

    inner class BookViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val bookTitle: TextView = itemView.findViewById(android.R.id.text1)

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
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return BookViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        val bookName = books[position]
        holder.bookTitle.text = bookName
        holder.itemView.setOnClickListener {
            // Toast.makeText(holder.itemView.context, "Clicked $bookName", Toast.LENGTH_SHORT).show()
            onClick(bookName)
        }
    }

    override fun getItemCount(): Int = books.size
}
