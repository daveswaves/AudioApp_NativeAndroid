// MainFragment.kt
package com.daveswaves.audioapp

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import android.widget.ProgressBar
import kotlin.math.roundToInt

class MainFragment : Fragment(R.layout.fragment_main) {

    // Audio playback state
    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false
    private var audioFiles: List<Uri> = emptyList()
    private var currentIndex = 0
    private var currentBookName: String? = null

    // Position tracking
    private var positionUpdateRunnable: Runnable? = null
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())

    // UI components
    private lateinit var chapterTitle: TextView
    private lateinit var playButton: Button
    private lateinit var mainImage: ImageView
    private lateinit var chapterProgressBar: ProgressBar
    private lateinit var timeDisplay: TextView

    // Shared preferences helper
    private val prefs: SharedPreferences by lazy {
        requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // Activity result launcher
    private lateinit var pickFolderLauncher: ActivityResultLauncher<Uri?>

    companion object {
        private const val PREFS_NAME = "audio_prefs"
        private const val KEY_SELECTED_BOOK_COVER = "selected_book_cover"
        private const val KEY_HAS_SELECTED_FOLDER = "has_selected_folder"
        private const val KEY_AUDIOBOOK_DIR = "audiobook_dir"
        private const val KEY_SELECTED_BOOK = "selected_book"
        private const val KEY_POSITION_PREFIX = "position_"
        private const val KEY_CHAPTER_PREFIX = "chapter_"
        
        private val SUPPORTED_AUDIO_FORMATS = setOf("mp3", "m4a")
        // private const val POSITION_UPDATE_INTERVAL = 5000L // Update position every 5 seconds
        private const val POSITION_UPDATE_INTERVAL = 1000L // Update position every second
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeFolderPicker()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initializeViews(view)
        setupInitialState()
        setupButtonListeners()
        loadAudioFiles()
    }

    private fun initializeFolderPicker() {
        pickFolderLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            uri?.let { handleFolderSelection(it) }
        }
    }

    private fun initializeViews(view: View) {
        chapterTitle = view.findViewById(R.id.chapterTitle)
        playButton = view.findViewById(R.id.playButton)
        mainImage = view.findViewById(R.id.myImageView)

        // Add these new view initializations
        chapterProgressBar = view.findViewById(R.id.chapterProgressBar)
        timeDisplay = view.findViewById(R.id.timeDisplay)
    }

    private fun setupInitialState() {
        updateMainImage()
        playButton.text = getString(android.R.string.ok).takeIf { false } ?: "Play" // Fallback to "Play"
    }

    private fun updateMainImage() {
        val coverUriString = prefs.getString(KEY_SELECTED_BOOK_COVER, null)
        val hasFolder = prefs.getBoolean(KEY_HAS_SELECTED_FOLDER, false)
        
        when {
            coverUriString != null -> mainImage.setImageURI(Uri.parse(coverUriString))
            hasFolder -> mainImage.setImageResource(R.drawable.books)
            else -> mainImage.setImageResource(R.drawable.refresh)
        }
    }

    private fun setupButtonListeners() {
        chapterTitle.setOnClickListener {
            navigateToChaptersFragment()
        }
        
        playButton.setOnClickListener {
            if (isPlaying) pauseAudio() else startOrResumeAudio()
        }

        view?.findViewById<Button>(R.id.nextButton)?.setOnClickListener {
            playNextChapter()
        }

        view?.findViewById<Button>(R.id.prevButton)?.setOnClickListener {
            playPreviousChapter()
        }

        view?.findViewById<Button>(R.id.seekBackButton1min)?.setOnClickListener {
            seekBackward(60000)
        }

        view?.findViewById<Button>(R.id.seekBackButton5sec)?.setOnClickListener {
            seekBackward(10000)
        }

        view?.findViewById<Button>(R.id.seekForwardButton5sec)?.setOnClickListener {
            seekForward(10000)
        }

        view?.findViewById<Button>(R.id.seekForwardButton1min)?.setOnClickListener {
            seekForward(60000)
        }

        view?.findViewById<Button>(R.id.booksButton)?.setOnClickListener {
            navigateToBooksFragment()
        }

        view?.findViewById<Button>(R.id.refreshButton)?.setOnClickListener {
            pickFolderLauncher.launch(null)
        }

        view?.findViewById<Button>(R.id.recentButton)?.setOnClickListener {
            navigateToFragment(RecentFragment())
        }

        view?.findViewById<Button>(R.id.bookmarksButton)?.setOnClickListener {
            navigateToFragment(BookmarksFragment())
        }

        // TODO: Implement star action
        // view?.findViewById<Button>(R.id.starButton)?.setOnClickListener { }
    }

    private fun handleFolderSelection(uri: Uri) {
        // Persist permission
        requireContext().contentResolver.takePersistableUriPermission(
            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
        
        // Save to preferences
        prefs.edit()
            .putString(KEY_AUDIOBOOK_DIR, uri.toString())
            .putBoolean(KEY_HAS_SELECTED_FOLDER, true)
            .apply()
        
        // Update UI and navigate
        mainImage.setImageResource(R.drawable.books)
        val books = getAudioBooksFromFolder(uri)
        navigateToFragment(BooksFragment.newInstance(books))
    }

    private fun getAudioBooksFromFolder(folderUri: Uri): List<String> {
        val books = mutableListOf<String>()
        
        runCatching {
            requireContext().contentResolver.query(
                DocumentsContract.buildChildDocumentsUriUsingTree(
                    folderUri, DocumentsContract.getTreeDocumentId(folderUri)
                ),
                arrayOf(
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE
                ),
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
        }.onFailure { 
            // Log error or show user-friendly message
        }
        
        return books
    }

    private fun loadAudioFiles() {
        val baseUriString = prefs.getString(KEY_AUDIOBOOK_DIR, null)
        val selectedBook = prefs.getString(KEY_SELECTED_BOOK, null)
        
        if (baseUriString == null || selectedBook == null) {
            updateChapterTitle("Unknown chapter")
            resetProgressBar()
            return
        }

        // Save current position before loading new book
        saveCurrentPosition()

        runCatching {
            val files = getAudioFilesFromBook(Uri.parse(baseUriString), selectedBook)
            audioFiles = sortAudioFilesNaturally(files)
            currentBookName = selectedBook
            
            // Restore saved position and chapter for this book
            restoreSavedPosition()
            updateChapterTitle()
            resetProgressBar()
        }.onFailure {
            updateChapterTitle("Unknown chapter")
            resetProgressBar()
        }
    }

    private fun getAudioFilesFromBook(baseUri: Uri, bookName: String): List<Uri> {
        val files = mutableListOf<Uri>()
        val resolver = requireContext().contentResolver
        val booksUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            baseUri, DocumentsContract.getTreeDocumentId(baseUri)
        )

        resolver.query(
            booksUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME
            ),
            null, null, null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val docId = cursor.getString(0)
                val name = cursor.getString(1)
                if (name == bookName) {
                    files.addAll(getAudioFilesFromFolder(baseUri, docId))
                    break
                }
            }
        }

        return files
    }

    private fun getAudioFilesFromFolder(baseUri: Uri, folderId: String): List<Uri> {
        val files = mutableListOf<Uri>()
        val bookFolderUri = DocumentsContract.buildChildDocumentsUriUsingTree(baseUri, folderId)
        
        requireContext().contentResolver.query(
            bookFolderUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME
            ),
            null, null, null
        )?.use { filesCursor ->
            while (filesCursor.moveToNext()) {
                val fileDocId = filesCursor.getString(0)
                val fileName = filesCursor.getString(1)
                if (isAudioFile(fileName)) {
                    files.add(DocumentsContract.buildDocumentUriUsingTree(baseUri, fileDocId))
                }
            }
        }
        
        return files
    }

    private fun isAudioFile(fileName: String): Boolean {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return extension in SUPPORTED_AUDIO_FORMATS
    }

    private fun sortAudioFilesNaturally(files: List<Uri>): List<Uri> {
        return files.sortedWith(compareBy { uri ->
            val name = uri.lastPathSegment ?: ""
            // Extract first number in filename, fallback to Int.MAX_VALUE for natural sorting
            Regex("""\d+""").find(name)?.value?.toIntOrNull() ?: Int.MAX_VALUE
        })
    }

    private fun startOrResumeAudio() {
        if (audioFiles.isEmpty()) {
            loadAudioFiles()
            if (audioFiles.isEmpty()) {
                updateChapterTitle("No audio files found")
                return
            }
        }

        if (mediaPlayer == null) {
            playCurrentChapter()
        } else {
            mediaPlayer?.start()
            isPlaying = true
            playButton.text = "Pause"
            startPositionTracking()
        }
    }

    private fun playCurrentChapter(restorePosition: Boolean = true) {
        if (audioFiles.isEmpty()) {
            updateChapterTitle("Unknown chapter")
            return
        }

        releaseMediaPlayer()

        resetProgressBar()
        
        runCatching {
            val uri = audioFiles[currentIndex]
            mediaPlayer = MediaPlayer().apply {
                setDataSource(requireContext(), uri)
                prepare()

                // Initialize progress bar immediately after prepare
                updateProgressBar()
                
                // Only seek to saved position if restorePosition is true
                if (restorePosition) {
                    val savedPosition = getSavedPositionForCurrentChapter()
                    if (savedPosition > 0) {
                        seekTo(savedPosition)
                        updateProgressBar()
                    }
                }

                start()
                setOnCompletionListener {
                    this@MainFragment.isPlaying = false
                    playButton.text = "Play"
                    stopPositionTracking()

                    chapterProgressBar.progress = 100
                    
                    // Clear saved position for completed chapter
                    clearSavedPositionForCurrentChapter()

                    // Auto-advance to next chapter
                    if (currentIndex < audioFiles.size - 1) {
                        currentIndex = (currentIndex + 1) % audioFiles.size
                        playCurrentChapter(restorePosition = false)
                    }
                }
            }
            
            isPlaying = true
            playButton.text = "Pause"
            updateChapterTitle()
            startPositionTracking()
        }.onFailure {
            updateChapterTitle("Error playing audio")
            chapterProgressBar.progress = 0
            timeDisplay.text = "0:00 / 0:00"
        }
    }

    private fun pauseAudio() {
        mediaPlayer?.pause()
        isPlaying = false
        playButton.text = "Play"
        stopPositionTracking()
        saveCurrentPosition() // Save position when pausing
        updateProgressBar()
    }

    private fun playNextChapter() {
        if (audioFiles.isNotEmpty()) {
            saveCurrentPosition() // Save position before switching
            currentIndex = (currentIndex + 1) % audioFiles.size
            resetProgressBar()
            playCurrentChapter(restorePosition = false) // Don't restore position for new chapter
        }
    }

    private fun playPreviousChapter() {
        if (audioFiles.isNotEmpty()) {
            saveCurrentPosition() // Save position before switching
            currentIndex = if (currentIndex - 1 < 0) audioFiles.size - 1 else currentIndex - 1
            resetProgressBar()
            playCurrentChapter(restorePosition = false) // Don't restore position for new chapter
        }
    }

    private fun seekBackward(mSecs: Int) {
        val player = mediaPlayer ?: return
        
        val currentPosition = player.currentPosition
        val newPosition = (currentPosition - mSecs).coerceAtLeast(0)
        
        player.seekTo(newPosition)
        saveCurrentPosition() // Update saved position immediately
        updateProgressBar()
    }

    private fun seekForward(mSecs: Int) {
        val player = mediaPlayer ?: return
        
        val currentPosition = player.currentPosition
        val duration = player.duration
        val newPosition = (currentPosition + mSecs).coerceAtMost(duration)
        
        player.seekTo(newPosition)
        saveCurrentPosition() // Update saved position immediately
        updateProgressBar()
        
        // If we've reached the end, complete the chapter
        if (newPosition >= duration - 1000) { // 1 second buffer
            player.pause()
            isPlaying = false
            playButton.text = "Play"
            stopPositionTracking()
            clearSavedPositionForCurrentChapter()
            
            // Auto-advance to next chapter
            if (currentIndex < audioFiles.size - 1) {
                currentIndex = (currentIndex + 1) % audioFiles.size
                playCurrentChapter(restorePosition = false)
            }
        }
    }

    private fun navigateToChaptersFragment() {
        if (audioFiles.isNotEmpty()) {
            val chaptersFragment = ChaptersFragment.newInstance(
                audioFiles.map { getChapterDisplayName(it) },
                currentIndex
            )
            navigateToFragment(chaptersFragment)
        }
    }

    private fun updateChapterTitle(title: String? = null) {
        chapterTitle.text = title ?: if (audioFiles.isNotEmpty()) {
            getChapterDisplayName(audioFiles[currentIndex])
        } else {
            "Unknown chapter"
        }
    }

    private fun getChapterDisplayName(uri: Uri): String {
        return uri.lastPathSegment
            ?.substringAfterLast("/")
            ?.substringBeforeLast(".")
            ?.replace(Regex("""^\d+\._"""), { matchResult ->
                matchResult.value.removeSuffix("_")
            })
            ?: "Unknown chapter"
    }

    private fun navigateToBooksFragment() {
        val uriString = prefs.getString(KEY_AUDIOBOOK_DIR, null)
        val books = uriString?.let { getAudioBooksFromFolder(Uri.parse(it)) } ?: emptyList()
        navigateToFragment(BooksFragment.newInstance(books))
    }

    private fun navigateToFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun releaseMediaPlayer() {
        stopPositionTracking()
        saveCurrentPosition() // Save position before releasing
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseMediaPlayer()
    }

    override fun onPause() {
        super.onPause()
        saveCurrentPosition() // Save position when fragment is paused
        // Optionally pause audio when fragment is not visible
        if (isPlaying) {
            pauseAudio()
        }
    }

    // Position tracking methods
    private fun startPositionTracking() {
        stopPositionTracking() // Stop any existing tracking
        
        positionUpdateRunnable = object : Runnable {
            override fun run() {
                if (isPlaying && mediaPlayer != null) {
                    saveCurrentPosition()
                    updateProgressBar()
                    handler.postDelayed(this, POSITION_UPDATE_INTERVAL) // Use 1000L if require update every second
                }
            }
        }
        handler.postDelayed(positionUpdateRunnable!!, POSITION_UPDATE_INTERVAL) // 1000L
    }

    private fun stopPositionTracking() {
        positionUpdateRunnable?.let { runnable ->
            handler.removeCallbacks(runnable)
        }
        positionUpdateRunnable = null
    }

    private fun saveCurrentPosition() {
        val bookName = currentBookName ?: return
        val player = mediaPlayer ?: return
        
        if (audioFiles.isNotEmpty() && currentIndex < audioFiles.size) {
            val position = player.currentPosition
            val chapterName = getChapterDisplayName(audioFiles[currentIndex])
            
            if (position >= 0) {
                prefs.edit()
                    .putInt(getPositionKey(bookName, chapterName), position)
                    .putInt(getChapterIndexKey(bookName), currentIndex)
                    .apply()
            }
        }
    }

    private fun restoreSavedPosition() {
        val bookName = currentBookName ?: return
        
        if (audioFiles.isEmpty()) {
            currentIndex = 0
            return
        }
        
        // Restore chapter index
        val savedChapterIndex = prefs.getInt(getChapterIndexKey(bookName), 0)
        currentIndex = if (savedChapterIndex < audioFiles.size) savedChapterIndex else 0
    }

    private fun getSavedPositionForCurrentChapter(): Int {
        val bookName = currentBookName ?: return 0
        
        if (audioFiles.isEmpty() || currentIndex >= audioFiles.size) return 0
        
        val chapterName = getChapterDisplayName(audioFiles[currentIndex])
        return prefs.getInt(getPositionKey(bookName, chapterName), 0)
    }

    private fun clearSavedPositionForCurrentChapter() {
        val bookName = currentBookName ?: return
        
        if (audioFiles.isNotEmpty() && currentIndex < audioFiles.size) {
            val chapterName = getChapterDisplayName(audioFiles[currentIndex])
            prefs.edit()
                .remove(getPositionKey(bookName, chapterName))
                .apply()
        }
    }

    private fun getPositionKey(bookName: String, chapterName: String): String {
        return "$KEY_POSITION_PREFIX${bookName}_$chapterName"
    }

    private fun getChapterIndexKey(bookName: String): String {
        return "$KEY_CHAPTER_PREFIX$bookName"
    }

    // update the progress bar
    private fun updateProgressBar() {
        val player = mediaPlayer ?: return
        
        try {
            val currentPosition = player.currentPosition
            val duration = player.duration
            
            if (duration > 0) {
                val progress = ((currentPosition.toFloat() / duration.toFloat()) * 100).roundToInt()
                chapterProgressBar.progress = progress
                
                val currentTime = formatTime(currentPosition)
                val totalTime = formatTime(duration)
                timeDisplay.text = "$currentTime / $totalTime"
            } else {
                chapterProgressBar.progress = 0
                timeDisplay.text = "0:00 / 0:00"
            }
        } catch (e: Exception) {
            // Handle any exceptions (e.g., if MediaPlayer is not prepared)
            chapterProgressBar.progress = 0
            timeDisplay.text = "0:00 / 0:00"
        }
    }

    // format time in mm:ss format
    private fun formatTime(milliseconds: Int): String {
        val seconds = milliseconds / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%d:%02d", minutes, remainingSeconds)
    }

    // reset progress bar when no media is loaded
    private fun resetProgressBar() {
        chapterProgressBar.progress = 0
        timeDisplay.text = "0:00 / 0:00"
    }

    override fun onResume() {
        super.onResume()
        
        // Check if user selected a chapter from ChaptersFragment
        val selectedIndex = prefs.getInt("selected_chapter_index", -1)
        if (selectedIndex >= 0 && selectedIndex < audioFiles.size && selectedIndex != currentIndex) {
            saveCurrentPosition() // Save current position before switching
            currentIndex = selectedIndex
            resetProgressBar()
            playCurrentChapter(restorePosition = false) // Start from beginning of selected chapter
            
            // Clear the selection
            prefs.edit().remove("selected_chapter_index").apply()
        }
    }
}

/*
Claude AI MainFragment.kt Refactor
----------------------------------
1. Code Organization & Structure

▸ Companion object for constants instead of magic strings
▸ Lazy initialization for SharedPreferences
▸ Proper separation of initialization, setup, and business logic
▸ Clear method grouping by functionality

2. Error Handling

▸ runCatching blocks around file system operations that can fail
▸ Graceful fallbacks when audio files can't be loaded
▸ Null safety improvements throughout

3. Performance Optimizations

▸ Eliminated code duplication in audio file loading
▸ Extracted helper methods for reusable operations
▸ More efficient file filtering with a dedicated isAudioFile() method
▸ Lazy SharedPreferences initialization

4. Better Resource Management

▸ Consolidated MediaPlayer release logic
▸ Added onPause() to optionally pause audio when fragment isn't visible
▸ Proper cleanup in lifecycle methods

5. Improved Maintainability

▸ Single responsibility for each method
▸ Descriptive method names that clearly indicate their purpose
▸ Consistent naming patterns
▸ Reduced nesting levels for better readability

6. Enhanced User Experience

▸ Auto-advance to next chapter when current chapter completes
▸ Better error messages for different failure scenarios
▸ More robust file type detection

7. Code Quality Improvements

▸ Eliminated magic strings with constants
▸ Consistent code formatting
▸ Proper Kotlin idioms (e.g., ?.let, runCatching)
▸ Reduced complexity in individual methods

8. Type Safety

▸ Better null handling throughout
▸ Safe string operations with proper fallbacks
▸ Consistent Uri handling
*/