# GEMINI.md

Use @RULES.md for Coding Preferences

## Project Overview

This project is an Android audiobook player application written in Kotlin. It allows users to browse local directories, manage a library of audiobooks, and play various audio formats (mp3, m4a, m4b, opus).

## Architecture

The application follows a **Single-Activity Architecture**:
-   **MainActivity**: The host activity.
-   **Fragments**: All screens are implemented as Fragments (`MainFragment`, `RecentFragment`, `ChaptersFragment`, `BooksFragment`).
-   **Navigation**: Handled via `FragmentManager` transactions.

### Key Components

#### 1. MainFragment (Player Core)
-   **Responsibilities**:
    -   Manages the `MediaPlayer` lifecycle (load, play, pause, seek).
    -   Updates the UI: Progress bar, time display, cover image.
    -   Handles playback state persistence (saving position per chapter).
    -   Acts as the central navigation hub.
-   **State Handling**:
    -   Persists playback position using `position_{Book}_{Chapter}` keys.
    -   Persists current chapter index using `chapter_{Book}` keys.
    -   On `onResume`, checks for `selected_chapter_index` (signal from `ChaptersFragment`) to change chapters.

#### 2. RecentFragment (History)
-   **Responsibilities**:
    -   Displays a list of recently played audiobooks.
    -   Validates if the audiobook files still exist on the device.
    -   Cleans up "dead" links from SharedPreferences.
-   **Data Source**: `recent_books` (Set<String>) in SharedPreferences.

#### 3. ChaptersFragment (Navigation)
-   **Responsibilities**:
    -   Lists all chapters/files for the current book.
-   **Interaction**:
    -   When a user selects a chapter, it writes the `selected_chapter_index` to SharedPreferences and pops the back stack. `MainFragment` picks up this change.

## Data Persistence (SharedPreferences)

The app uses `SharedPreferences` ("audio_prefs") extensively for state management:

| Key | Type | Description |
| :--- | :--- | :--- |
| `audiobook_dir` | String | URI string of the selected root folder. |
| `selected_book` | String | Name of the currently active book. |
| `recent_books` | Set<String> | List of book names in history. |
| `position_{Book}_{Chapter}` | Int | Playback position (ms) for a specific chapter. |
| `chapter_{Book}` | Int | Index of the current chapter in the book. |
| `selected_chapter_index` | Int | *Transient*. signal from ChaptersFragment to MainFragment. |
| `all_bookmarks` | Set<String> | Serialized bookmark data (`Book|Chapter|Index|Pos|Time`). |

## Architecture Notes

### Key Simplification
The keys for playback position and chapter index have been simplified to literal string templates:
- Position: `"position_${bookName}_$chapterName"`
- Chapter Index: `"chapter_$bookName"`

### RecentFragment Cleanup Logic
**Status:** **FIXED**
**Location:** `RecentFragment.kt` -> `onViewCreated`

Missing books are now correctly removed from the `recent_books` set and all associated data (position, bookmarks) is purged when the fragment is created or when a user manually removes a book.

## Build Instructions
