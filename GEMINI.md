# GEMINI.md

## Project Overview

This project is an Android audiobook player application. It allows users to browse and play audiobooks stored on their device. The application is written in Kotlin and uses the Android SDK. It follows a single-activity architecture, with fragments for different screens.

The core features of the application include:

*   **Audiobook Playback:** The application can play various audio formats. It provides standard playback controls like play, pause, seek, and chapter navigation.
*   **Library Management:** Users can select a directory on their device that contains their audiobooks. The application will then scan this directory and display a list of available books.
*   **Recent Books:** The application keeps track of recently played books for quick access.
*   **Bookmarks:** Users can create bookmarks to save their position in an audiobook.
*   **Chapter Navigation:** The application can parse chapter information from the audiobook files and allow users to navigate between chapters.

## Building and Running

This is a standard Android project. You can build and run it using Android Studio or the Gradle command line tool.

To build the project from the command line, run the following command in the root directory of the project:

```bash
./gradlew build
```

To install and run the application on a connected device or emulator, run the following command:

```bash
./gradlew installDebug
./gradlew runDebug
```

## Development Conventions

*   **Language:** The project is written entirely in Kotlin.
*   **Architecture:** The application follows a single-activity architecture, with a `MainActivity` that hosts various fragments. The navigation between fragments is handled by the `FragmentManager`.
*   **Data Persistence:** The application uses `SharedPreferences` to persist data, such as the current book, chapter, position, and bookmarks.
*   **UI:** The user interface is built using Android's standard UI components, including `RecyclerView` for displaying lists and `CardView` for creating a modern-looking layout.
*   **Dependencies:** The project uses standard AndroidX libraries, such as `appcompat`, `fragment-ktx`, and `material`.
