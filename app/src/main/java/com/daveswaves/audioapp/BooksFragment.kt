// BooksFragment.kt
package com.daveswaves.audioapp

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class BooksFragment : Fragment(R.layout.fragment_books) {
    companion object {
        private const val ARG_BOOKS = "books"

        fun newInstance(books: List<String>): BooksFragment {
            val fragment = BooksFragment()
            val args = Bundle()
            args.putStringArrayList(ARG_BOOKS, ArrayList(books))
            fragment.arguments = args
            return fragment
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val closeButton: Button = view.findViewById(R.id.closeButton)
        closeButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val bookList = arguments?.getStringArrayList(ARG_BOOKS) ?: emptyList()

        val recyclerView: RecyclerView = view.findViewById(R.id.booksRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = BooksAdapter(bookList)
    }
}
