// ChaptersFragment.kt
package com.daveswaves.audioapp

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

// For ChaptersAdapter class - bottom of page.
import android.view.LayoutInflater
import android.view.ViewGroup

class ChaptersFragment : Fragment(R.layout.fragment_chapters) {
    
    private var chapterNames: List<String> = emptyList()
    private var currentChapterIndex: Int = 0
    
    companion object {
        private const val ARG_CHAPTER_NAMES = "chapter_names"
        private const val ARG_CURRENT_INDEX = "current_index"
        
        fun newInstance(chapterNames: List<String>, currentIndex: Int): ChaptersFragment {
            return ChaptersFragment().apply {
                arguments = Bundle().apply {
                    putStringArrayList(ARG_CHAPTER_NAMES, ArrayList(chapterNames))
                    putInt(ARG_CURRENT_INDEX, currentIndex)
                }
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            chapterNames = it.getStringArrayList(ARG_CHAPTER_NAMES) ?: emptyList()
            currentChapterIndex = it.getInt(ARG_CURRENT_INDEX, 0)
        }
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val closeButton: Button = view.findViewById(R.id.closeButton)
        closeButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        
        // Set up RecyclerView
        val recyclerView: RecyclerView = view.findViewById(R.id.chaptersRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = ChaptersAdapter(chapterNames, currentChapterIndex) { selectedIndex ->
            // Handle chapter selection
            onChapterSelected(selectedIndex)
        }
    }
    
    private fun onChapterSelected(selectedIndex: Int) {
        // Save selected chapter index to preferences so MainFragment can access it
        val prefs = requireContext().getSharedPreferences("audio_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("selected_chapter_index", selectedIndex).apply()
        
        // Go back to MainFragment
        parentFragmentManager.popBackStack()
    }
}


class ChaptersAdapter(
    private val chapters: List<String>,
    private val currentIndex: Int,
    private val onChapterClick: (Int) -> Unit
) : RecyclerView.Adapter<ChaptersAdapter.ChapterViewHolder>() {
    
    class ChapterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val chapterText: TextView = itemView.findViewById(android.R.id.text1)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChapterViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return ChapterViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ChapterViewHolder, position: Int) {
        holder.chapterText.text = chapters[position]
        
        // Highlight current chapter
        if (position == currentIndex) {
            holder.chapterText.setTextColor(holder.itemView.context.getColor(android.R.color.holo_blue_dark))
            holder.chapterText.textSize = 18f
        } else {
            holder.chapterText.setTextColor(holder.itemView.context.getColor(android.R.color.white))
            holder.chapterText.textSize = 16f
        }
        
        holder.itemView.setOnClickListener {
            onChapterClick(position)
        }
    }
    
    override fun getItemCount(): Int = chapters.size
}