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
import androidx.documentfile.provider.DocumentFile
// import androidx.appcompat.app.AlertDialog //DEBUG
// import java.io.OutputStreamWriter
// import android.content.SharedPreferences

// This fragment displays a list of recently opened audiobooks
// using cached data stored in SharedPreferences. It also attempts
// to resolve and display cover images from the audiobook's folder.
class RecentFragment : Fragment(R.layout.fragment_recent) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Close button handling
        val closeButton: Button = view.findViewById(R.id.closeButton)
        closeButton.setOnClickListener {
            // Removes this fragment from the back stack (returns to previous screen)
            parentFragmentManager.popBackStack()
        }

        // Load shared preferences (app-wide persistent storage)
        val prefs = requireContext().getSharedPreferences("audio_prefs", Context.MODE_PRIVATE)
        // Retrieve list of recent audiobook names (cached as a Set<String>)
        val recentNames = prefs.getStringSet("recent_books", emptySet()) ?: emptySet()

        // val debugMessage = prefs.all.entries.joinToString("\n") { (key, value) ->
        //     "$key = $value"
        // }
        // AlertDialog.Builder(requireContext())
        //     .setTitle("Recents Data")
        //     .setMessage(debugMessage)
        //     .setPositiveButton("OK", null)
        //     .show()

        //✅ START NEW CODE BLOCK
        val baseUriString = prefs.getString(PrefsKeys.AUDIOBOOK_DIR, null)
        val recentBooks = mutableListOf<BookData>()
        val updatedRecentNames = mutableSetOf<String>()
        val editor = prefs.edit()


        // DEBUG: OUTPUT TO FILE

        // val debugMessage = prefs.all.entries.joinToString("\n") { (key, value) -> "$key = $value"}
        // val debugMessage = recentNames.joinToString("\n")
        // savePrefsDebugFile(requireContext(), prefs, Uri.parse(baseUriString))


        if (baseUriString != null) {
            val baseUri = Uri.parse(baseUriString)

            for (name in recentNames) {
                val exists = bookExists(baseUri, name)

                if (exists) {
                    val coverUri = prefs
                        .getString("recent_cover_$name", null)
                        ?.let(Uri::parse)
                    
                    recentBooks.add(BookData(name, coverUri))
                    updatedRecentNames.add(name)
                }
                // !!! NEED A FIX: not working !!!
                // Remove reference from memory if audio file does not exist
                else {
                    editor.remove("recent_cover_$name")
                    editor.remove("${name}_chapter_index")

                    prefs.all.keys
                        .filter { it.startsWith("${name}_") }
                        .forEach { editor.remove(it) }
                    
                    val allBookmarks = prefs.getStringSet(PrefsKeys.ALL_BOOKMARKS, emptySet()) ?: emptySet()
                    // val bookmarks = prefs.getStringSet(PrefsKeys.ALL_BOOKMARKS, emptySet())!!.toMutableSet()
                    val updatedBookmarks = allBookmarks.filterNot { it.startsWith("$name|") }.toSet()

                    editor.putStringSet(
                        PrefsKeys.ALL_BOOKMARKS,
                        updatedBookmarks
                        // bookmarks.filterNot { it.startsWith("$name|") }.toSet()
                    )

                    if (prefs.getString(PrefsKeys.SELECTED_BOOK, null) == name) {
                        editor.remove(PrefsKeys.SELECTED_BOOK)
                        editor.remove(PrefsKeys.SELECTED_BOOK_COVER)
                        editor.apply()
                    }
                }
            }
        }
        //✅ END NEW CODE BLOCK

        // val recentBooks = recentNames.map { name ->
        //     val coverUri = prefs
        //         .getString("recent_cover_$name", null)
        //         ?.let(Uri::parse)
            
        //     BookData(name, coverUri)
        // }.toMutableList()

        // Setup RecyclerView for displaying recent audiobooks
        val recyclerView: RecyclerView = view.findViewById(R.id.recentRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Attach custom adapter to handle list display + interactions
        recyclerView.adapter = RecentAdapter(
            recentBooks,

            // onRemove: triggered when a user removes a book from recents
            onRemove = { removedBook ->
                val editor = prefs.edit()

                // Remove from recents set
                val updated = recentBooks.map { it.name }.toMutableSet()
                updated.remove(removedBook.name)
                editor.putStringSet("recent_books", updated)

                // Remove saved cover for this book
                editor.remove("recent_cover_${removedBook.name}")

                // Remove position + chapter index keys
                val positionPrefix = "${removedBook.name}_"
                val allKeys = prefs.all.keys
                allKeys.filter { it.startsWith(positionPrefix) }.forEach { editor.remove(it) }

                // also explicit keys (if you’re using helpers like getPositionKey/getChapterIndexKey)
                editor.remove("${removedBook.name}_chapter_index")

                // Remove bookmarks belonging to this book
                val allBookmarks = prefs.getStringSet(PrefsKeys.ALL_BOOKMARKS, emptySet())!!.toMutableSet()
                val filtered = allBookmarks.filterNot { it.startsWith("${removedBook.name}|") }.toSet()
                editor.putStringSet(PrefsKeys.ALL_BOOKMARKS, filtered)

                // If the deleted book was the selected one, clear selection
                val selectedBook = prefs.getString("selected_book", null)
                if (selectedBook == removedBook.name) {
                    editor.remove("selected_book")
                    editor.remove("selected_book_cover")
                }

                editor.apply()
            },

            // onClick: Triggered when a user selects a book
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

    // private fun savePrefsDebugFile(context: Context, prefs: SharedPreferences, baseUri: Uri) {
    //     val baseDir = DocumentFile.fromTreeUri(context, baseUri) ?: return

    //     // Create or overwrite the debug file
    //     val debugFileName = "prefs_debug.txt"

    //     // Delete existing debug file if it exists
    //     baseDir.findFile(debugFileName)?.delete()

    //     val debugFile = baseDir.createFile("text/plain", debugFileName) ?: return

    //     val allPrefs = prefs.all
    //     val debugMessage = allPrefs.entries.joinToString("\n") { (key, value) ->
    //         "$key = $value"
    //     }

    //     context.contentResolver.openOutputStream(debugFile.uri)?.use { outputStream ->
    //         OutputStreamWriter(outputStream).use { writer ->
    //             writer.write(debugMessage)
    //         }
    //     }
    // }

    private fun bookExists(baseUri: Uri, bookName: String): Boolean {
        val baseDir = DocumentFile.fromTreeUri(requireContext(), baseUri) ?: return false
        val bookDir = baseDir.findFile(bookName)
        return bookDir?.isDirectory == true
    }
}
