// BookmarkData.kt
package com.daveswaves.audioapp

data class BookmarkData(
    val book: String,
    val chapter: String,
    val chapterIndex: Int,
    val position: Int, // in milliseconds
    val timestamp: Long
)