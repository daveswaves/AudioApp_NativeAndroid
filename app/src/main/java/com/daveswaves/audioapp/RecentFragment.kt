// RecentFragment.kt
package com.daveswaves.audioapp

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class RecentFragment : Fragment(R.layout.fragment_recent) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val closeButton: Button = view.findViewById(R.id.closeButton)
        closeButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val prefs = requireContext().getSharedPreferences("audio_prefs", Context.MODE_PRIVATE)

        // Load recents (stored as Set<String>)
        val recentNames = prefs.getStringSet("recent_books", emptySet()) ?: emptySet()

        // If you want cover images too, resolve them here:
        val baseUriString = prefs.getString("audiobook_dir", null)
        val baseUri = baseUriString?.let { Uri.parse(it) }
        val recentBooks = recentNames.map { name ->
            BookData(name, baseUri?.let { getCoverUri(it, name) })
        }.toMutableList()

        val recyclerView: RecyclerView = view.findViewById(R.id.recentRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        recyclerView.adapter = RecentAdapter(
            recentBooks,
            onRemove = { removedBook ->
                val updated = recentBooks.map { it.name }.toMutableSet()
                updated.remove(removedBook.name)
                prefs.edit().putStringSet("recent_books", updated).apply()
            },
            onClick = { selectedBook ->
                // Same behavior as BooksFragment
                prefs.edit()
                    .putString("selected_book", selectedBook.name)
                    .putString("selected_book_cover", selectedBook.coverUri?.toString())
                    .apply()

                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, MainFragment())
                    .addToBackStack(null)
                    .commit()
            }
        )
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
