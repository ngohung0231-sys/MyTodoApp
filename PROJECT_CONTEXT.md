# Project Context: MyTodoApp

## Overview
A Task Management (To-Do) application built with Kotlin and modern Android components.

## Tech Stack
- **Language:** Kotlin
- **Database:** Room (SQLite) with Coroutines & Flow
- **Navigation:** Jetpack Navigation Component
- **UI Architecture:** Fragment-based with Repository pattern
- **UI Components:** RecyclerView (Nested), ConstraintLayout, Material Design 3

## Data Models
### 1. Folder
- `folderId`: Primary Key (Auto-generate)
- `folderName`: String
- `folderImg`: Int (Resource ID for the icon)
- `folderColor`: Int (Color value)
- `taskCount`: Int (Dynamic count of tasks)

### 2. Task
- `id`: Primary Key
- `title`, `description`: String
- `date`, `time`: Stored via TypeConverters
- `folderId`: Foreign key relation to Folder
- `isCompleted`: Boolean status

## Key Components & Logic

### 1. Database (`TodoDatabase.kt`)
- Uses `fallbackToDestructiveMigration()` (Data resets on schema change).
- **Initialization:** Inserts default folders (Others, Personal, Exercise, Travel, Study, Groceries) using `ic_folder` as the default icon.

### 2. Home Screen (`HomeFragment.kt`)
- **Calendar:** Horizontal week view.
- **Folders:** Horizontal list using `FolderAdapter`.
- **Tasks:** Grouped by folder using `FolderGroupAdapter` (contains nested `TaskAdapter`).
- **Filtering:** Today, Upcoming, and Calendar-specific date filtering.

### 3. Adapters
- `FolderAdapter`: Displays folders on the home screen. 
    - *Fix History:* Removed hardcoded logic that forced `R.drawable.icon` for `folderId == 1`. Now correctly uses `folderImg` and `folderColor`.
- `FolderGroupAdapter`: Displays a Folder header and its associated tasks.
- `TaskAdapter`: Individual task items with status toggle.

### 4. Layouts
- `fragment_calender.xml`: Uses `NestedScrollView`. 
    - *Constraint:* Must have exactly ONE direct child (wrapped everything in a vertical `LinearLayout`).
- `item_folder_home.xml`: Layout for the horizontal folder cards on Home.

## Resources & Styling

### 1. Colors (`colors.xml`)
- Main Colors: `blue` (#4997cf), `pink` (#fe6e9a), `red` (#ee4d5e), `green` (#44be65), `yellow` (#f89520), `purple` (#a792ec).
- Backgrounds: `grey` (#f3f5f9) used for cards and backgrounds.

### 2. Themes (`themes.xml`)
- `Theme.MyTodoApp`: Base Material 3 theme (NoActionBar).
- `Theme.App.Starting`: Splash Screen theme using Android 12+ SplashScreen API.
- `CustomCalendarTheme`: Specific styling for DatePicker/Calendar dialogs.
- `CircleImageStyle`: Used for rounded avatars (50% corner size).

### 3. Key Drawables
- `ic_folder`: The default vector icon for folders.
- `ripple_rounded_bg`: Standard ripple effect for clickable items.
- `bg_grey`: Common background for card items with rounded corners.
- `filter_task_bg`: Used for the selected state of Today/Upcoming filters.

## Recent Critical Fixes
- **ScrollView Error:** Fixed `IllegalStateException` in Calendar Fragment by wrapping multiple children in a single container.
- **Folder Icon Issue:** Fixed an issue where folders appeared as a red "X" (App Icon) instead of the folder icon. This was caused by hardcoded ID checks in `FolderAdapter`.

## File Structure
- `adapter/`: UI Adapters for RecyclerViews.
- `database/`: Room DB, DAO, Repository, and TypeConverters.
- `fragment/`: All UI screens (Home, Task, Calendar, AddTask, etc.).
- `model/`: Data classes (Folder, Task, CalendarDay).
- `res/layout/`: XML UI definitions.
- `res/values/`: Colors, Themes, and Strings.
