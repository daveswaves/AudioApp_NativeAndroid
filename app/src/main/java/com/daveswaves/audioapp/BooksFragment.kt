// BooksFragment.kt
package com.daveswaves.audioapp

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class BooksFragment : Fragment(R.layout.fragment_books) {
    companion object {
        private const val ARG_BOOKS = "books"
        private const val ARG_QUERY = "query"

        fun newInstance(books: List<String>, query: String? = null): BooksFragment {
            val fragment = BooksFragment()
            val args = Bundle()
            args.putStringArrayList(ARG_BOOKS, ArrayList(books))
            args.putString(ARG_QUERY, query)
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
        val query = arguments?.getString(ARG_QUERY)

        // --- Load shared preferences (app-wide persistent storage) ---
        val prefs = requireContext().getSharedPreferences("audio_prefs", Context.MODE_PRIVATE)
        // --- Load base directory URI (points to user-selected audiobook folder) ---
        val baseUriString = prefs.getString("audiobook_dir", null)
        val baseUri = baseUriString?.let { Uri.parse(it) }

        val recyclerView: RecyclerView = view.findViewById(R.id.booksRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        
        // Create book data with cover URIs
        val booksWithCovers = bookList.map { bookName ->
            val coverUri = baseUri?.let { getCoverUri(it, bookName) }
            BookData(bookName, coverUri)
        }
        
        recyclerView.adapter = BooksAdapter(booksWithCovers) { selectedBook ->
            val coverUri = getCoverUri(baseUri!!, selectedBook.name)

            prefs.edit()
                .putString("selected_book", selectedBook.name)
                .putString("selected_book_cover", coverUri?.toString())
                .apply()

            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, MainFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun getCoverUri(baseUri: Uri, bookName: String): Uri? {
        val resolver = requireContext().contentResolver
        val booksUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            baseUri, DocumentsContract.getTreeDocumentId(baseUri)
        )

        resolver.query(
            booksUri,
            arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null, null, null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val docId = cursor.getString(0)
                val name = cursor.getString(1)
                if (name == bookName) {
                    val bookFolderUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                        baseUri, docId
                    )
                    resolver.query(
                        bookFolderUri,
                        arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                        null, null, null
                    )?.use { filesCursor ->
                        while (filesCursor.moveToNext()) {
                            val fileDocId = filesCursor.getString(0)
                            val fileName = filesCursor.getString(1)
                            if (fileName.equals("cover.jpg", ignoreCase = true)) {
                                return DocumentsContract.buildDocumentUriUsingTree(
                                    baseUri, fileDocId
                                )
                            }
                        }
                    }
                }
            }
        }
        return null
    }
}

data class BookData(val name: String, val coverUri: Uri?)
