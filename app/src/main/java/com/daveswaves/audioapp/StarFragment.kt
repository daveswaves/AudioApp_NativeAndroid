// StarFragment.kt
package com.daveswaves.audioapp

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
// import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class StarFragment : Fragment(R.layout.fragment_bookmarks) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val closeButton: Button = view.findViewById(R.id.closeButton)
        closeButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val prefs = requireContext().getSharedPreferences("audio_prefs", Context.MODE_PRIVATE)
        val bookmarkSet = prefs.getStringSet("all_bookmarks", emptySet()) ?: emptySet()

        // Parse bookmarks and sort by timestamp (newest first)
        val bookmarks = bookmarkSet.mapNotNull { bookmarkString ->
            val parts = bookmarkString.split("|")
            if (parts.size == 5) {
                try {
                    BookmarkData(
                        book = parts[0],
                        chapter = parts[1],
                        chapterIndex = parts[2].toInt(),
                        position = parts[3].toInt(),
                        timestamp = parts[4].toLong()
                    )
                } catch (e: NumberFormatException) {
                    null
                }
            } else {
                null
            }
        }.sortedByDescending { it.timestamp }

        val recyclerView: RecyclerView = view.findViewById(R.id.bookmarksRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        recyclerView.adapter = BookmarkAdapter(bookmarks) { bookmark ->
            // Create new MainFragment with bookmark data
            val bundle = Bundle().apply {
                putString("bookmark_book", bookmark.book)
                putString("bookmark_chapter", bookmark.chapter)
                putInt("bookmark_chapter_index", bookmark.chapterIndex)
                putInt("bookmark_position", bookmark.position)
            }
            
            val newMainFragment = MainFragment().apply {
                arguments = bundle
            }
            
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, newMainFragment)
                .commit()
        }
    }
}
