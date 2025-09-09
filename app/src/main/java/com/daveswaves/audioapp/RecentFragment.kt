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

// This fragment displays a list of recently opened audiobooks
// using cached data stored in SharedPreferences. It also attempts
// to resolve and display cover images from the audiobook's folder.
class RecentFragment : Fragment(R.layout.fragment_recent) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- Close button handling ---
        val closeButton: Button = view.findViewById(R.id.closeButton)
        closeButton.setOnClickListener {
            // Removes this fragment from the back stack (returns to previous screen)
            parentFragmentManager.popBackStack()
        }

        // --- Load shared preferences (app-wide persistent storage) ---
        val prefs = requireContext().getSharedPreferences("audio_prefs", Context.MODE_PRIVATE)

        // --- Retrieve list of recent audiobook names (cached as a Set<String>) ---
        val recentNames = prefs.getStringSet("recent_books", emptySet()) ?: emptySet()

        // --- Load base directory URI (points to user-selected audiobook folder) ---
        val baseUriString = prefs.getString("audiobook_dir", null)
        val baseUri = baseUriString?.let { Uri.parse(it) }
        
        // --- Build a list of BookData objects (title + optional cover image) ---
        val recentBooks = recentNames.map { name ->
            BookData(
                name,
                baseUri?.let { getCoverUri(it, name) } // Try to resolve "cover.jpg"
            )
        }.toMutableList()

        // --- Setup RecyclerView for displaying recent audiobooks ---
        val recyclerView: RecyclerView = view.findViewById(R.id.recentRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // --- Attach custom adapter to handle list display + interactions ---
        recyclerView.adapter = RecentAdapter(
            recentBooks,

            // onRemove: triggered when a user removes a book from recents
            onRemove = { removedBook ->
                val updated = recentBooks.map { it.name }.toMutableSet()
                updated.remove(removedBook.name)
                // Save updated recent list back into SharedPreferences
                prefs.edit().putStringSet("recent_books", updated).apply()
            },

            // onClick: triggered when a user selects a book
            onClick = { selectedBook ->
                // Save current selection (book + cover) to SharedPreferences
                prefs.edit()
                    .putString("selected_book", selectedBook.name)
                    .putString("selected_book_cover", selectedBook.coverUri?.toString())
                    .apply()

                // Navigate to MainFragment (same behavior as BooksFragment)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, MainFragment())
                    .addToBackStack(null)
                    .commit()
            }
        )
    }

    // --- Utility: Resolve the cover image ("cover.jpg") for a given audiobook ---
    private fun getCoverUri(baseUri: Uri, bookName: String): Uri? {
        val resolver = requireContext().contentResolver
        
        // Query for all child documents (subfolders/files) of the base audiobook directory
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
                val name = cursor.getString(1) // Display name (folder name)
                
                // Check if this folder matches the book name
                if (name == bookName) {
                    // Build URI for files inside the book’s folder
                    val bookFolderUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                        baseUri, docId
                    )

                    // Search inside the book folder for "cover.jpg"
                    resolver.query(
                        bookFolderUri,
                        arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                        null, null, null
                    )?.use { filesCursor ->
                        while (filesCursor.moveToNext()) {
                            val fileDocId = filesCursor.getString(0)
                            val fileName = filesCursor.getString(1)
                            
                            // Match cover file (case-insensitive)
                            if (fileName.equals("cover.jpg", ignoreCase = true)) {
                                // Build and return URI pointing directly to cover file
                                return DocumentsContract.buildDocumentUriUsingTree(
                                    baseUri, fileDocId
                                )
                            }
                        }
                    }
                }
            }
        }
        return null // Cover not found
    }
}
