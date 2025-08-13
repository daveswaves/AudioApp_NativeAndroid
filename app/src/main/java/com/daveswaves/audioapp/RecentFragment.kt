// RecentFragment.kt
package com.daveswaves.audioapp

import android.os.Bundle
import android.view.View
import android.widget.Button
// import android.widget.ImageButton
import androidx.fragment.app.Fragment

class RecentFragment : Fragment(R.layout.fragment_recent) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val closeButton: Button = view.findViewById(R.id.closeButton)
        // val closeButton: ImageButton = view.findViewById(R.id.closeButton)
        closeButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }
}
