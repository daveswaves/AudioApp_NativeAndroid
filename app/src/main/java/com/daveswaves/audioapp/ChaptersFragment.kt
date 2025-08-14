// ChaptersFragment.kt
package com.daveswaves.audioapp

import android.os.Bundle
import android.view.View
import android.widget.Button
// import android.widget.ImageButton
import androidx.fragment.app.Fragment

class ChaptersFragment : Fragment(R.layout.fragment_chapters) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val closeButton: Button = view.findViewById(R.id.closeButton)
        // val closeButton: ImageButton = view.findViewById(R.id.closeButton)
        closeButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }
}
