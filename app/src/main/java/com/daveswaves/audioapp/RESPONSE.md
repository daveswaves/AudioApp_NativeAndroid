# Recommendations: Migrating to Jetpack DataStore

To solve the issue of scattered `SharedPreferences` keys and "messy" JSON string storage, I recommend migrating to **Jetpack DataStore (Proto DataStore)**.

Proto DataStore is ideal here because you are storing typed objects (`RecentBook`) rather than simple key-value pairs. It provides type safety, atomic transactions, and allows you to store the entire state of a book in a single object, making deletion trivial.

## 1. Proposed Architecture

Instead of managing keys like `position_Jazz_Chapter1`, `chapter_Jazz`, and `recent_cover_Jazz` separately, we define a structured schema.

### Protobuf Schema (`user_prefs.proto`)

We will define a `RecentBook` message and a list of them.

```protobuf
syntax = "proto3";

option java_package = "com.daveswaves.audioapp.data";
option java_multiple_files = true;

message RecentBook {
    string id = 1;              // Book Name (e.g., "Jazz")
    int32 chapter_index = 2;    // Current chapter index (e.g., 0)
    string chapter_title = 3;   // Display name (e.g., "Keith Jarrett...")
    int64 position = 4;         // Playback position in ms
    string cover_uri = 5;       // Persisted URI permission string
    int64 timestamp = 6;        // Last played time (for sorting)
}

message UserPreferences {
    repeated RecentBook recent_books = 1;
    string selected_book_id = 2; // Currently active book
}
```

## 2. Implementation Steps

### Step A: Add Dependencies
Add the DataStore and Protobuf dependencies to your `app/build.gradle`.

```groovy
plugins {
    id "com.google.protobuf" version "0.9.4"
}

dependencies {
    implementation "androidx.datastore:datastore:1.0.0"
    implementation "com.google.protobuf:protobuf-javalite:3.21.7"
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.21.7"
    }
    generateProtoTasks {
        all().each { task ->
            task.builtins {
                java {
                    option 'lite'
                }
            }
        }
    }
}
```

### Step B: create the Serializer
You need a Serializer to tell DataStore how to read/write your proto format.

```kotlin
object UserPreferencesSerializer : Serializer<UserPreferences> {
    override val defaultValue: UserPreferences = UserPreferences.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): UserPreferences {
        try {
            return UserPreferences.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(t: UserPreferences, output: OutputStream) = t.writeTo(output)
}
```

### Step C: Repository Pattern (The "Easy Delete" Solution)
Create a `UserPreferencesRepository` to manage the logic. This is where the "easy deletion" happens.

```kotlin
class UserPreferencesRepository(private val context: Context) {
    private val Context.userPreferencesStore: DataStore<UserPreferences> by dataStore(
        fileName = "user_prefs.pb",
        serializer = UserPreferencesSerializer
    )

    // Read data as a Flow
    val recentBooksFlow: Flow<List<RecentBook>> = context.userPreferencesStore.data
        .map { it.recentBooksList }
        // Sort by timestamp descending (newest first)
        .map { list -> list.sortedByDescending { it.timestamp } }

    // SAVE or UPDATE a book
    suspend fun updateBookProgress(id: String, chapterIndex: Int, title: String, position: Long, cover: String?) {
        context.userPreferencesStore.updateData { prefs ->
            val currentList = prefs.recentBooksList.toMutableList()
            
            // Remove existing entry if present
            currentList.removeAll { it.id == id }
            
            // Add updated entry
            val builder = RecentBook.newBuilder()
                .setId(id)
                .setChapterIndex(chapterIndex)
                .setChapterTitle(title)
                .setPosition(position)
                .setTimestamp(System.currentTimeMillis())
            
            if (cover != null) builder.setCoverUri(cover)
            
            currentList.add(builder.build())
            
            prefs.toBuilder()
                .clearRecentBooks()
                .addAllRecentBooks(currentList)
                .setSelectedBookId(id)
                .build()
        }
    }

    // DELETE a book (Clean and Simple)
    suspend fun removeBook(bookId: String) {
        context.userPreferencesStore.updateData { prefs ->
            val filteredList = prefs.recentBooksList.filter { it.id != bookId }
            
            prefs.toBuilder()
                .clearRecentBooks()
                .addAllRecentBooks(filteredList)
                .build()
        }
    }
}
```

## 3. Migration Strategy
To avoid losing user data, perform a one-time migration when the app starts.

1. Check if `SharedPreferences` contains `recent_books`.
2. If yes, iterate through the keys, build `RecentBook` objects, and push them to DataStore.
3. `SharedPreferences.edit().clear().apply()`.

## Summary of Benefits
1.  **Atomic Operations**: Reading and writing are transactional. No race conditions.
2.  **Type Safety**: No parsing JSON strings manually.
3.  **Clean Deletion**: `removeBook` removes the single object containing ID, Position, Chapter, and Cover. No need to hunt down `position_Jazz`, `chapter_Jazz`, etc.
