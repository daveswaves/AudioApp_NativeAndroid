// MainFragment.kt
package com.daveswaves.audioapp

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment

class MainFragment : Fragment(R.layout.fragment_main) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Ref buttons
        val booksButton: Button = view.findViewById(R.id.booksButton)
        val refreshButton: Button = view.findViewById(R.id.refreshButton)
        val recentButton: Button = view.findViewById(R.id.recentButton)
        val starButton: Button = view.findViewById(R.id.starButton)
        val bookmarksButton: Button = view.findViewById(R.id.bookmarksButton)
        
        recentButton.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, RecentFilesFragment())
                .addToBackStack(null)
                .commit()
        }

        booksButton.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, BooksFragment())
                .addToBackStack(null)
                .commit()
        }

        bookmarksButton.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, BookmarksFragment())
                .addToBackStack(null)
                .commit()
        }

        // TODO: Add more views etc.
    }
}


        // bookmarksButton.setOnClickListener {
        //     // Navigate to BookmarksFragment (to be created)
        // }

        // // TODO: Implement refresh logic
        // refreshButton.setOnClickListener {
        //     // Refresh main view content
        // }

        // // TODO: Implement star action
        // starButton.setOnClickListener {
        //     // Star/favorite current book
        // }

