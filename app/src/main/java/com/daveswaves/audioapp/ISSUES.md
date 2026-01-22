# ISSUES.md

## Known Issues & Fixes

### RecentFragment Cleanup Logic
**Status:** **Needs Fix**
**Location:** `RecentFragment.kt` -> `onViewCreated`

**Problem:**
The fragment iterates through `recentNames` to verify file existence. If a book is missing, it correctly removes specific keys (cover, bookmarks, position) but **fails to update the main `recent_books` list** in SharedPreferences. This causes "dead" books to reappear or persist effectively.

**Fix Required:**
After the validation loop, if `updatedRecentNames` differs from the original list, it must be saved back to preferences.

```kotlin
// ... inside verification loop ...
if (exists) {
    updatedRecentNames.add(name)
} else {
    // ... cleanup specific keys ...
}

// FIX: Save the updated list
if (updatedRecentNames.size != recentNames.size) {
    editor.putStringSet("recent_books", updatedRecentNames)
}
editor.apply()
```
