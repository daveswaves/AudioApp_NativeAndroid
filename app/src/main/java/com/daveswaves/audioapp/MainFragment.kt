// MainFragment.kt
package com.daveswaves.audioapp

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.View
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment

class MainFragment : Fragment(R.layout.fragment_main) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val booksButton: Button = view.findViewById(R.id.booksButton)
        booksButton.setOnClickListener {
            val prefs = requireContext().getSharedPreferences("audio_prefs", Context.MODE_PRIVATE)
            val uriString = prefs.getString("audiobook_dir", null)
            val books = if (uriString != null) {
                getAudioBooksFromFolder(Uri.parse(uriString))
            } else {
                emptyList()
            }
            
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, BooksFragment.newInstance(books))
                .addToBackStack(null)
                .commit()
        }

        val refreshButton: Button = view.findViewById(R.id.refreshButton)
        refreshButton.setOnClickListener {
            pickFolderLauncher.launch(null)
        }
        
        val recentButton: Button = view.findViewById(R.id.recentButton)
        recentButton.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, RecentFilesFragment())
                .addToBackStack(null)
                .commit()
        }

        // TODO: Implement star action
        val starButton: Button = view.findViewById(R.id.starButton)
        // starButton.setOnClickListener {}

        val bookmarksButton: Button = view.findViewById(R.id.bookmarksButton)
        bookmarksButton.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, BookmarksFragment())
                .addToBackStack(null)
                .commit()
        }

        // TODO: Add more button clicks.
    }

    // Select audio folder method
    private val pickFolderLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            // Persist permission so it's accessible next time
            requireContext().contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            
            // Save to SharedPreferences
            val prefs = requireContext().getSharedPreferences("audio_prefs", Context.MODE_PRIVATE)
            prefs.edit().putString("audiobook_dir", uri.toString()).apply()

            // Get list of books immediately
            val books = getAudioBooksFromFolder(uri)

            // Navigate to BooksFragment and pass the list
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, BooksFragment.newInstance(books))
                .addToBackStack(null)
                .commit()
        }
    }

    private fun getAudioBooksFromFolder(folderUri: Uri): List<String> {
        val books = mutableListOf<String>()
        requireContext().contentResolver.query(
            DocumentsContract.buildChildDocumentsUriUsingTree(
                folderUri, DocumentsContract.getTreeDocumentId(folderUri)
            ),
            arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE),
            null, null, null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val name = cursor.getString(0)
                val mime = cursor.getString(1)
                if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                    books.add(name)
                }
            }
        }
        return books
    }
}
