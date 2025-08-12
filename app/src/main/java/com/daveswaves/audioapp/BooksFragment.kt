// BooksFragment.kt
package com.daveswaves.audioapp

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment

class BooksFragment : Fragment(R.layout.fragment_books) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val closeButton: Button = view.findViewById(R.id.closeButton)
        closeButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }
}
