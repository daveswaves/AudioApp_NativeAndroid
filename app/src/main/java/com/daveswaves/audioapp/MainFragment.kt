// MainFragment.kt
package com.daveswaves.audioapp

import java.io.File
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
import android.widget.Toast
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
        private const val KEY_ALL_BOOKMARKS = "all_bookmarks"
        
        private val SUPPORTED_AUDIO_FORMATS = setOf("mp3", "m4a")
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

        // Handle bookmark navigation
        arguments?.let { bundle ->
            val bookmarkBook = bundle.getString("bookmark_book")
            val bookmarkChapter = bundle.getString("bookmark_chapter") 
            val bookmarkChapterIndex = bundle.getInt("bookmark_chapter_index", -1)
            val bookmarkPosition = bundle.getInt("bookmark_position", -1)
            
            if (bookmarkBook != null && bookmarkChapterIndex >= 0 && bookmarkPosition >= 0) {
                // Clear the arguments to avoid re-triggering
                arguments = null
                
                // Toast.makeText(requireContext(), "Loading bookmark: $bookmarkBook", Toast.LENGTH_SHORT).show()
                
                // Handle the bookmark seek after everything loads
                handler.postDelayed({
                    seekToBookmark(bookmarkBook, bookmarkChapter ?: "", bookmarkChapterIndex, bookmarkPosition)
                }, 1000) // Give it time to fully initialize
            }
        }

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
        chapterProgressBar = view.findViewById(R.id.chapterProgressBar)
        timeDisplay = view.findViewById(R.id.timeDisplay)

        setupProgressBarClickListener()
    }

    private fun setupInitialState() {
        updateMainImage()
        playButton.text = "Play"
        resetProgressBar()
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

        private fun setButtonClick(id: Int, action: () -> Unit) {
        view?.findViewById<Button>(id)?.setOnClickListener { action() }
    }

    private fun setupButtonListeners() {
        chapterTitle.setOnClickListener { navigateToChaptersFragment() }

        chapterTitle.setOnLongClickListener {
            mediaPlayer?.seekTo(0)
            saveCurrentPosition()
            resetProgressBar()
            true
        }

        playButton.setOnClickListener {
            if (isPlaying) pauseAudio() else startOrResumeAudio()
        }

        setButtonClick(R.id.nextButton) { playNextChapter() }
        setButtonClick(R.id.prevButton) { playPreviousChapter() }
        setButtonClick(R.id.seekBackButton1min) { seekBackward(60000) }
        setButtonClick(R.id.seekBackButton5sec) { seekBackward(5000) }
        setButtonClick(R.id.seekForwardButton5sec) { seekForward(5000) }
        setButtonClick(R.id.seekForwardButton1min) { seekForward(60000) }
        setButtonClick(R.id.booksButton) { navigateToBooksFragment() }
        setButtonClick(R.id.refreshButton) { pickFolderLauncher.launch(null) }
        setButtonClick(R.id.recentButton) { navigateToFragment(RecentFragment()) }
        setButtonClick(R.id.bookmarksButton) { navigateToFragment(BookmarksFragment()) }

        setButtonClick(R.id.starButton) {
            val currentBook = prefs.getString(KEY_SELECTED_BOOK, null)
            val currentPosition = mediaPlayer?.currentPosition ?: 0

            if (currentBook != null && audioFiles.isNotEmpty()) {
                val chapterName = getChapterDisplayName(audioFiles[currentIndex])
                val bookmarkString =
                    "$currentBook|$chapterName|$currentIndex|$currentPosition|${System.currentTimeMillis()}"

                val updated = (prefs.getStringSet(KEY_ALL_BOOKMARKS, emptySet()) ?: emptySet())
                    .toMutableSet()
                updated.add(bookmarkString)

                prefs.edit().putStringSet(KEY_ALL_BOOKMARKS, updated).apply()

                val formatted = formatTime(currentPosition)
                Toast.makeText(requireContext(), "Bookmark added: $formatted", Toast.LENGTH_SHORT).show()
            } else {
                val reason = when {
                    currentBook == null -> "No audiobook selected"
                    audioFiles.isEmpty() -> "No audio files loaded"
                    currentPosition <= 0 -> "Audio not playing or at position 0"
                    else -> "Unknown error"
                }
                Toast.makeText(requireContext(), reason, Toast.LENGTH_SHORT).show()
            }
        }
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

    private fun updateBookmarksButtonState() {
        val bookmarksButton = view?.findViewById<Button>(R.id.bookmarksButton) ?: return
        val prefs = requireContext().getSharedPreferences("audio_prefs", Context.MODE_PRIVATE)
        val allBookmarks = prefs.getStringSet(KEY_ALL_BOOKMARKS, emptySet()) ?: emptySet()
        val currentBook = prefs.getString("selected_book", null)
        val hasBookmarks = currentBook != null && allBookmarks.any { it.startsWith("$currentBook|") }

        bookmarksButton.isEnabled = hasBookmarks
        bookmarksButton.alpha = if (hasBookmarks) 1.0f else 0.5f // grayed-out effect
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

    fun seekToBookmark(bookName: String, chapterName: String, chapterIndex: Int, position: Int) {
        val prefs = requireContext().getSharedPreferences("audio_prefs", Context.MODE_PRIVATE)
        val currentBook = prefs.getString("selected_book", null)
        
        saveCurrentPosition()
        
        if (currentBook != bookName) {
            prefs.edit().putString("selected_book", bookName).apply()
            
            updateMainImage()
            loadAudioFiles()
            updateBookmarksButtonState()
            
            // Wait for files to load then seek
            handler.postDelayed({
                performSeek(chapterIndex, position)
            }, 100)
        } else {
            performSeek(chapterIndex, position)
        }
    }

    private fun performSeek(chapterIndex: Int, position: Int) {
        // Stop current playback
        releaseMediaPlayer()
        
        // Set new chapter
        currentIndex = chapterIndex
        updateChapterTitle()
        
        // Create MediaPlayer but DON'T start playing yet
        val uri = audioFiles[currentIndex]
        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(requireContext(), uri)
                
                setOnPreparedListener { player ->
                    val seekPos = position.coerceAtMost(player.duration - 1000)
                    player.seekTo(seekPos)
                    
                    initializeProgressUI(player.duration, seekPos)
                    
                    player.start()
                    this@MainFragment.isPlaying = true
                    playButton.text = "Pause"
                    startPositionTracking()
                }
                
                setOnCompletionListener {
                    handleChapterCompletion()
                }
                
                prepareAsync()
                
            } catch (e: Exception) {
                // Toast.makeText(requireContext(), "Error loading chapter", Toast.LENGTH_SHORT).show()
                updateChapterTitle("Error loading chapter")
                resetProgressBar()
            }
        }
    }

    private fun loadAudioFiles() {
        val baseUriString = prefs.getString(KEY_AUDIOBOOK_DIR, null)
        val selectedBook = prefs.getString(KEY_SELECTED_BOOK, null)
        
        if (baseUriString == null || selectedBook == null) {
            updateChapterTitle("Unknown chapter")
            resetProgressBar()
            return
        }

        saveCurrentPosition()

        runCatching {
            val files = getAudioFilesFromBook(Uri.parse(baseUriString), selectedBook)
            audioFiles = sortAudioFilesNaturally(files)
            currentBookName = selectedBook
            
            restoreSavedPosition()
            updateChapterTitle()
            
            loadCurrentChapterInfo()
            updateBookmarksButtonState()
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
        
        runCatching {
            val uri = audioFiles[currentIndex]
            mediaPlayer = MediaPlayer().apply {
                setDataSource(requireContext(), uri)
                prepare()

                val savedPosition = if (restorePosition) getSavedPositionForCurrentChapter() else 0
                
                initializeProgressUI(duration, savedPosition)
                
                if (savedPosition > 0) {
                    seekTo(savedPosition)
                }

                start()
                
                setOnCompletionListener {
                    handleChapterCompletion()
                }
            }
            
            isPlaying = true
            playButton.text = "Pause"
            updateChapterTitle()
            startPositionTracking()

            // Save to recent books
            val prefs = requireContext().getSharedPreferences("audio_prefs", Context.MODE_PRIVATE)
            val currentBook = prefs.getString("selected_book", null)
            val currentCover = prefs.getString("selected_book_cover", null)

            currentBook?.let { bookName ->
                val recentSet = prefs.getStringSet("recent_books", mutableSetOf())!!.toMutableSet()
                recentSet.add(bookName) // ensure uniqueness
                prefs.edit()
                    .putStringSet("recent_books", recentSet)
                    .apply()

                // (optional) save cover per book in its own key
                currentCover?.let { cover ->
                    prefs.edit()
                        .putString("recent_cover_$bookName", cover)
                        .apply()
                }
            }
            
        }.onFailure {
            updateChapterTitle("Error playing audio")
            resetProgressBar()
        }
    }

    private fun handleChapterCompletion() {
        isPlaying = false
        playButton.text = "Play"
        stopPositionTracking()
        chapterProgressBar.progress = 100
        clearSavedPositionForCurrentChapter()

        // Auto-advance to next chapter
        if (currentIndex < audioFiles.size - 1) {
            currentIndex = (currentIndex + 1) % audioFiles.size
            playCurrentChapter(restorePosition = false)
        }
    }

    private fun pauseAudio() {
        mediaPlayer?.pause()
        isPlaying = false
        playButton.text = "Play"
        stopPositionTracking()
        saveCurrentPosition()
    }

    private fun playNextChapter() {
        if (audioFiles.isNotEmpty()) {
            saveCurrentPosition()
            currentIndex = (currentIndex + 1) % audioFiles.size
            playCurrentChapter(restorePosition = false)
        }
    }

    private fun playPreviousChapter() {
        if (audioFiles.isNotEmpty()) {
            saveCurrentPosition()
            currentIndex = if (currentIndex - 1 < 0) audioFiles.size - 1 else currentIndex - 1
            playCurrentChapter(restorePosition = false)
        }
    }

    private fun seekBackward(mSecs: Int) {
        val player = mediaPlayer ?: return
        
        val currentPosition = player.currentPosition
        val newPosition = (currentPosition - mSecs).coerceAtLeast(0)
        
        player.seekTo(newPosition)
        saveCurrentPosition()
        updateProgressBar()
    }

    private fun seekForward(mSecs: Int) {
        val player = mediaPlayer ?: return
        
        val currentPosition = player.currentPosition
        val duration = player.duration
        val newPosition = (currentPosition + mSecs).coerceAtMost(duration)
        
        player.seekTo(newPosition)
        saveCurrentPosition()
        updateProgressBar()
        
        // If we've reached the end, complete the chapter
        if (newPosition >= duration - 1000) { // 1 second buffer
            handleChapterCompletion()
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
        saveCurrentPosition()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseMediaPlayer()
    }

    // Position tracking methods
    private fun startPositionTracking() {
        stopPositionTracking() // Stop any existing tracking
        
        var saveCounter = 0

        positionUpdateRunnable = object : Runnable {
            override fun run() {
                if (isPlaying && mediaPlayer != null) {
                    updateProgressBar()

                    // Avoid using too much battery (update position every 30 seconds)
                    saveCounter++
                    if (saveCounter >= 30) {
                        saveCurrentPosition()
                        saveCounter = 0
                    }
                    handler.postDelayed(this, POSITION_UPDATE_INTERVAL)
                }
            }
        }
        handler.postDelayed(positionUpdateRunnable!!, POSITION_UPDATE_INTERVAL)
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
                resetProgressBar()
            }
        } catch (e: Exception) {
            resetProgressBar()
        }
    }

    private fun formatTime(milliseconds: Int): String {
        val seconds = milliseconds / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%d:%02d", minutes, remainingSeconds)
    }

    private fun resetProgressBar() {
        chapterProgressBar.progress = 0
        timeDisplay.text = "0:00 / 0:00"
    }

    private fun initializeProgressUI(duration: Int, currentPosition: Int = 0) {
        val progress = if (duration > 0 && currentPosition > 0) {
            ((currentPosition.toFloat() / duration.toFloat()) * 100).roundToInt()
        } else {
            0
        }
        
        chapterProgressBar.progress = progress
        
        val currentTime = formatTime(currentPosition)
        val totalTime = formatTime(duration)
        timeDisplay.text = "$currentTime / $totalTime"
    }

    // Load current chapter info to display duration and saved position without starting playback
    private fun loadCurrentChapterInfo() {
        if (audioFiles.isEmpty()) {
            resetProgressBar()
            return
        }

        runCatching {
            val uri = audioFiles[currentIndex]
            val tempPlayer = MediaPlayer().apply {
                setDataSource(requireContext(), uri)
                prepare()
            }
            
            val duration = tempPlayer.duration
            val savedPosition = getSavedPositionForCurrentChapter()
            
            initializeProgressUI(duration, savedPosition)
            
            tempPlayer.release()
        }.onFailure {
            resetProgressBar()
        }
    }

    private fun setupProgressBarClickListener() {
        chapterProgressBar.setOnTouchListener { _, event ->
            if (event.action == android.view.MotionEvent.ACTION_DOWN) {
                val progressBarWidth = chapterProgressBar.width
                val touchX = event.x
                val percentage = (touchX / progressBarWidth).coerceIn(0f, 1f)

                // CASE 1: No MediaPlayer yet (user hasn’t started playback)
                if (mediaPlayer == null) {
                    if (audioFiles.isNotEmpty() && currentBookName != null) {
                        // Estimate duration from last loaded info
                        val tempPlayer = MediaPlayer().apply {
                            setDataSource(requireContext(), audioFiles[currentIndex])
                            prepare()
                        }
                        val duration = tempPlayer.duration
                        tempPlayer.release()

                        val newPosition = (duration * percentage).toInt()

                        val chapterName = getChapterDisplayName(audioFiles[currentIndex])
                        prefs.edit()
                            .putInt(getPositionKey(currentBookName!!, chapterName), newPosition)
                            .apply()

                        initializeProgressUI(duration, newPosition)
                    }
                    return@setOnTouchListener true
                }

                // CASE 2: MediaPlayer already exists (normal seek)
                val player = mediaPlayer ?: return@setOnTouchListener false
                val duration = player.duration
                val newPosition = (duration * percentage).toInt()

                player.seekTo(newPosition)
                saveCurrentPosition()
                updateProgressBar()

                true
            } else {
                false
            }
        }
    }

    override fun onPause() {
        super.onPause()
        stopPositionTracking()
        saveCurrentPosition()
    }

    override fun onResume() {
        super.onResume()
        
        // Check if user selected a chapter from ChaptersFragment
        val selectedIndex = prefs.getInt("selected_chapter_index", -1)
        if (selectedIndex >= 0 && selectedIndex < audioFiles.size && selectedIndex != currentIndex) {
            saveCurrentPosition()
            currentIndex = selectedIndex
            playCurrentChapter(restorePosition = false)
            
            // Clear the selection
            prefs.edit().remove("selected_chapter_index").apply()
        }

        // Resume progress tracking if playback is active
        if (isPlaying) {
            startPositionTracking()
        }
    }
}