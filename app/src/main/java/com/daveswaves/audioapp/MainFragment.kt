// MainFragment.kt
package com.daveswaves.audioapp

import android.media.MediaPlayer
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.View
import android.widget.Button
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment

class MainFragment : Fragment(R.layout.fragment_main) {

    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false  // track play/pause state

    private var audioFiles: List<Uri> = emptyList()
    private var currentIndex = 0  // current chapter index

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize main image based on saved state (cover > book > refresh)
        val prefs = requireContext().getSharedPreferences("audio_prefs", Context.MODE_PRIVATE)
        val coverUriString = prefs.getString("selected_book_cover", null)
        val hasFolder = prefs.getBoolean("has_selected_folder", false)
        val mainImage: ImageView = view.findViewById(R.id.myImageView)
        
        when {
            coverUriString != null -> mainImage.setImageURI(Uri.parse(coverUriString))
            hasFolder -> mainImage.setImageResource(R.drawable.books)
            else -> mainImage.setImageResource(R.drawable.refresh)
        }

        // Audio transport buttons
        val playButton: Button = view.findViewById(R.id.playButton)
        playButton.text = "Play" // initial state
        loadAudioFiles(prefs) // load all chapters
        playButton.setOnClickListener {
            // playFirstAudioFile(prefs)
            if (isPlaying) {pauseAudio(playButton)}
            else {startOrResumeAudio(prefs, playButton)}
        }

        val nextButton: Button = view.findViewById(R.id.nextButton)
        nextButton.setOnClickListener {
            if (audioFiles.isNotEmpty()) {
                currentIndex = (currentIndex + 1) % audioFiles.size
                playCurrentChapter(playButton)
            }
        }

        val prevButton: Button = view.findViewById(R.id.prevButton)
        prevButton.setOnClickListener {
            if (audioFiles.isNotEmpty()) {
                currentIndex = if (currentIndex - 1 < 0) audioFiles.size - 1 else currentIndex - 1
                playCurrentChapter(playButton)
            }
        }
        // END - Audio transport buttons

        val booksButton: Button = view.findViewById(R.id.booksButton)
        booksButton.setOnClickListener {
            // val prefs = requireContext().getSharedPreferences("audio_prefs", Context.MODE_PRIVATE)
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
                .replace(R.id.fragmentContainer, RecentFragment())
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
    private val pickFolderLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            // Persist permission so it's accessible next time
            requireContext().contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            
            // Save to SharedPreferences
            val prefs = requireContext().getSharedPreferences("audio_prefs", Context.MODE_PRIVATE)
            // prefs.edit().putString("audiobook_dir", uri.toString()).apply()
            prefs.edit()
                .putString("audiobook_dir", uri.toString())
                .putBoolean("has_selected_folder", true)
                .apply()
            
            // Update main image immediately to generic book (cover will override after selection)
            view?.findViewById<ImageView>(R.id.myImageView)?.setImageResource(R.drawable.books)

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

    private fun loadAudioFiles(prefs: android.content.SharedPreferences) {
        val baseUriString = prefs.getString("audiobook_dir", null)
        val selectedBook = prefs.getString("selected_book", null)
        if (baseUriString == null || selectedBook == null) return

        val resolver = requireContext().contentResolver
        val baseUri = Uri.parse(baseUriString)
        val booksUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            baseUri, DocumentsContract.getTreeDocumentId(baseUri)
        )

        val files = mutableListOf<Uri>()

        resolver.query(
            booksUri,
            arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null, null, null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val docId = cursor.getString(0)
                val name = cursor.getString(1)
                if (name == selectedBook) {
                    val bookFolderUri = DocumentsContract.buildChildDocumentsUriUsingTree(baseUri, docId)
                    resolver.query(
                        bookFolderUri,
                        arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                        null, null, null
                    )?.use { filesCursor ->
                        while (filesCursor.moveToNext()) {
                            val fileDocId = filesCursor.getString(0)
                            val fileName = filesCursor.getString(1)
                            if (fileName.endsWith(".mp3", true) || fileName.endsWith(".m4a", true)) {
                                files.add(DocumentsContract.buildDocumentUriUsingTree(baseUri, fileDocId))
                            }
                        }
                    }
                }
            }
        }

        audioFiles = files.sortedBy { it.lastPathSegment } // optional: sort alphabetically
        currentIndex = 0
    }

    private fun startOrResumeAudio(prefs: android.content.SharedPreferences, button: Button) {
        if (mediaPlayer == null) {
            // First-time playback: get first audio file
            val baseUriString = prefs.getString("audiobook_dir", null)
            val selectedBook = prefs.getString("selected_book", null)
            if (baseUriString == null || selectedBook == null) return

            val resolver = requireContext().contentResolver
            val baseUri = Uri.parse(baseUriString)

            val booksUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                baseUri, DocumentsContract.getTreeDocumentId(baseUri)
            )

            var firstAudioUri: Uri? = null

            resolver.query(
                booksUri,
                arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                null, null, null
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val docId = cursor.getString(0)
                    val name = cursor.getString(1)
                    if (name == selectedBook) {
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
                                if (fileName.endsWith(".mp3", true) || fileName.endsWith(".m4a", true)) {
                                    firstAudioUri = DocumentsContract.buildDocumentUriUsingTree(
                                        baseUri, fileDocId
                                    )
                                    break
                                }
                            }
                        }
                    }
                }
            }

            firstAudioUri?.let { uri ->
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(requireContext(), uri)
                    prepare()
                    start()
                }
            }
        } else {
            // Resume if paused
            mediaPlayer?.start()
        }

        isPlaying = true
        button.text = "Pause"
    }

    private fun playCurrentChapter(playButton: Button) {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(requireContext(), audioFiles[currentIndex])
            prepare()
            start()
            setOnCompletionListener {
                this@MainFragment.isPlaying = false
                playButton.text = "Play"
            }
        }
        isPlaying = true
        playButton.text = "Pause"
    }

    private fun pauseAudio(button: Button) {
        mediaPlayer?.pause()
        isPlaying = false
        button.text = "Play"
    }

    /*
    private fun playFirstAudioFile(prefs: android.content.SharedPreferences) {
        val baseUriString = prefs.getString("audiobook_dir", null)
        val selectedBook = prefs.getString("selected_book", null)
        if (baseUriString == null || selectedBook == null) return

        val resolver = requireContext().contentResolver
        val baseUri = Uri.parse(baseUriString)

        val booksUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            baseUri, DocumentsContract.getTreeDocumentId(baseUri)
        )

        var firstAudioUri: Uri? = null

        resolver.query(
            booksUri,
            arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null, null, null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val docId = cursor.getString(0)
                val name = cursor.getString(1)
                if (name == selectedBook) {
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
                            if (fileName.endsWith(".mp3", true) || fileName.endsWith(".m4a", true)) {
                                firstAudioUri = DocumentsContract.buildDocumentUriUsingTree(
                                    baseUri, fileDocId
                                )
                                break
                            }
                        }
                    }
                }
            }
        }

        firstAudioUri?.let { uri ->
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(requireContext(), uri)
                prepare()
                start()
            }
        }
    }
    */

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
