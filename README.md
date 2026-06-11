# LuminaAI - Android AI Assistant

A completely offline, privacy-first Android AI assistant built with Jetpack Compose, Room Database, and a coroutine-based state machine architecture.

## 📋 Table of Contents

- [Architecture Overview](#architecture-overview)
- [System Design](#system-design)
- [Project Structure](#project-structure)
- [Features](#features)
- [Technology Stack](#technology-stack)
- [Implementation Details](#implementation-details)
- [Setup & Installation](#setup--installation)
- [Testing](#testing)

---

## Architecture Overview

### **Clean Architecture Pattern**

The project follows **Clean Architecture** with clear separation of concerns:

```
┌─────────────────────────────────────────┐
│         PRESENTATION LAYER              │
│  (Compose UI, ViewModels, Screens)      │
└────────────────────┬────────────────────┘
                     │
┌────────────────────▼────────────────────┐
│         DOMAIN LAYER                    │
│  (Business Logic, State Machine)        │
└────────────────────┬────────────────────┘
                     │
┌────────────────────▼────────────────────┐
│         DATA LAYER                      │
│  (Room DB, DataStore, Repositories)     │
└────────────────────┬────────────────────┘
                     │
┌────────────────────▼────────────────────┐
│    EXTERNAL SERVICES & FRAMEWORKS       │
│  (WorkManager, AudioRecord, etc)        │
└─────────────────────────────────────────┘
```

### **Key Architectural Decisions**

#### 1. **StateFlow-Based State Management**
- **Decision**: Use `StateFlow` over `LiveData` for reactive state management
- **Reasoning**: 
  - Coroutine-native, better integration with Compose
  - Cold flow support for lazy initialization
  - Built-in thread safety and replay capability
- **Implementation**: `ChatState` sealed interface with `ChatPipeline` managing transitions

#### 2. **Coroutine-Based State Machine**
- **Decision**: Implement message processing as a coroutine state machine
- **Reasoning**:
  - Handles cancellation gracefully (if user sends new message, cancel current one)
  - Built-in timeout support via `withTimeoutOrNull()`
  - No external state machine libraries needed
- **States**: Typing → Validating → Processing → Responding → Idle
- **Error Handling**: Timeout after 8 seconds moves to Error state with retry option

#### 3. **ViewModel Scope for Data Persistence**
- **Decision**: Keep UI state in ViewModel's `MutableStateFlow` (not SavedStateHandle)
- **Reasoning**:
  - SavedStateHandle can't serialize custom objects
  - ViewModel survives back navigation naturally
  - Simpler, no need for Parcelable/serialization
- **Limitation**: Data lost on process death (acceptable for non-critical UI state)
- **Solution**: Critical data (user profile) saved to DataStore/Room instead

#### 4. **Offline-First Architecture**
- **Decision**: All data stored locally with optional cloud sync
- **Reasoning**:
  - Privacy-first: no user data leaves device
  - Works completely offline
  - Better performance (no network latency)
- **Implementation**:
  - Room Database as source of truth
  - WorkManager for background sync when network available
  - Local-wins conflict resolution (local data always takes precedence)

#### 5. **Separation: DataStore vs Room**
- **DataStore**: Small, frequently-changing preferences (user profile from onboarding)
- **Room**: Large datasets, complex queries (chat messages, reminders)
- **Reasoning**: Each tool optimized for its use case

#### 6. **Canvas-Based Animations (Pure Graphics)**
- **Decision**: No Lottie, no GIF, pure Compose Canvas API
- **Reasoning**:
  - Lightweight, no external dependencies
  - Full control over animation parameters
  - Real-time reactivity to mic amplitude
- **Implementation**: AuraCircle with radial gradient, breathing pulse, amplitude-driven glow

---

## System Design

### **1. Onboarding Flow (Part 1)**

```
┌─────────────────────────────────────────────────────────┐
│                   OnboardingScreen                       │
│          (HorizontalPager, userScrollEnabled=false)      │
└────────────────┬────────────────┬────────────────────────┘
                 │                │
        ┌────────▼──────┐  ┌──────▼──────────┐
        │   Step 1      │  │   Step 2        │
        │  ValueProps   │  │  CollectInfo    │
        │  (Animated    │  │  (Name, Age,    │
        │   text        │  │   Phone, OTP)   │
        │   reveal)     │  │                 │
        └────────┬──────┘  └──────┬──────────┘
                 │                │
                 │        ┌───────▼─────────┐
                 │        │    Step 3       │
                 │        │  Personality    │
                 │        │  (3 trait grid) │
                 │        └───────┬─────────┘
                 │                │
                 └────────┬───────┘
                          │
                  ┌───────▼────────────┐
                  │ OnboardingViewModel │
                  │  (State Management) │
                  └───────┬────────────┘
                          │
                  ┌───────▼────────────┐
                  │ ProfileDataStore    │
                  │ (Persist Profile)   │
                  └────────────────────┘
```

**Data Flow:**
1. User fills 3 steps with validation at each step
2. ViewModel maintains state in `MutableStateFlow<OnboardingUiState>`
3. Back navigation preserves state because ViewModel stays alive
4. On completion, profile saved to DataStore with GSON serialization
5. Navigation to HomeScreen with back stack cleared

**Key Files:**
- `OnboardingScreen.kt`: 3 composables (ValueProps, CollectInfo, Personality)
- `OnboardingViewModel.kt`: State management + validation
- `ProfileDataStore.kt`: DataStore persistence layer

---

### **2. Home Screen (Part 2)**

```
┌──────────────────────────────────────┐
│         HomeScreen                   │
│  ┌────────────────────────────────┐  │
│  │   AuraCircle                   │  │
│  │  (Canvas-based animation)      │  │
│  │  - Idle: breathing pulse       │  │
│  │  - Listening: amplitude-driven │  │
│  └────────────────────────────────┘  │
│                                      │
│  ┌────────────────────────────────┐  │
│  │   Chat History LazyColumn      │  │
│  │  (Reversed, newest at bottom)  │  │
│  │  - Pagination: 20 items/load   │  │
│  │  - Scroll-up triggers load     │  │
│  └────────────────────────────────┘  │
│                                      │
│  ┌────────────────────────────────┐  │
│  │   Bottom Action Bar            │  │
│  │  - Mic button (toggle listen)  │  │
│  │  - Keyboard button (text input)│  │
│  └────────────────────────────────┘  │
│                                      │
│  ┌───────��────────────────────────┐  │
│  │   Animated Text Input          │  │
│  │  (Slide up from bottom)        │  │
│  └────────────────────────────────┘  │
└──────────────────────────────────────┘
        │           │          │
        ▼           ▼          ▼
   AudioRecord  HomeViewModel  ChatPipeline
   (Amplitude)  (State+Queries) (State Machine)
```

**Audio Flow:**
1. Mic button toggles `isListening` state
2. `LaunchedEffect` collects amplitude from `AudioRecorderHelper.getAmplitudeFlow()`
3. Real-time amplitude (RMS normalized) passed to AuraCircle
4. Canvas redraws with amplitude-driven glow intensity

**Pagination Flow:**
1. LazyColumn tracks `firstVisibleItemIndex`
2. When user scrolls to top (index ≤ 2), `shouldLoadMore` triggers
3. `loadMoreMessages()` increments currentLimit by 20
4. Room query with LIMIT 20 OFFSET 0 fetches more items

**Key Files:**
- `AuraCircle.kt`: Canvas-based animation (pure graphics, no Lottie)
- `HomeScreen.kt`: UI composition, audio collection, pagination
- `AudioRecorderHelper.kt`: AudioRecord wrapping, amplitude calculation
- `HomeViewModel.kt`: Chat history queries, pagination state

---

### **3. Coroutine State Machine (Part 3)**

```
                    ┌─────────┐
                    │ sendMsg │
                    └────┬────┘
                         │
                    ┌────▼──────┐
                    │  Typing    │ (300ms)
                    └────┬───────┘
                         │
                    ┌────▼─────────┐
                    │ Validating   │ (300ms)
                    └────┬────────┬┘
                         │        │
                    No   │        │ Yes
                    ┌────▼┐   ┌──▼────┐
                    │Error│   │Process│ (8s timeout)
                    └─────┘   └──┬────┘
                                 │
                         ┌───────┴──────┐
                         │              │
                      Success        Timeout
                         │              │
                    ┌────▼──────┐   ┌──▼───┐
                    │ Responding│   │Error │
                    └────┬──────┘   └──────┘
                         │
                    ┌────▼──────┐
                    │   Idle    │
                    └───────────┘
```

**State Machine Logic:**
```kotlin
sealed interface ChatState {
    object Idle : ChatState              // Initial/final state
    object Typing : ChatState            // User just sent message
    object Validating : ChatState        // Check if message is valid
    object Processing : ChatState        // AI processing (8s timeout)
    data class Responding(val message: String) : ChatState  // Sending response
    data class Error(val message: String) : ChatState       // Timeout or error
}
```

**Key Features:**
- **Cancellation**: New message cancels current job (`currentMessageJob?.cancel()`)
- **Timeout**: `withTimeoutOrNull(8000L)` returns null if exceeds 8s
- **Error Handling**: Timeout or validation error → Error state with retry
- **No External Libraries**: Pure Kotlin coroutines

**Key Files:**
- `ChatState.kt`: State definitions (sealed interface)
- `ChatPipeline.kt`: State machine logic, transitions, timeout handling
- `ChatPipelineTest.kt`: 4 unit tests (happy path, cancellation, validation, timeout)

---

### **4. Offline-First Data Layer (Part 4)**

```
┌─────────────────────────────────────────────────────────┐
│                    AppDatabase                          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │ ChatMessage  │  │ UserProfile  │  │  Reminder    │  │
│  │  Entity      │  │  Entity      │  │  Entity      │  │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  │
│         │                 │                 │          │
│  ┌──────▼───────┐  ┌──────▼───────┐  ┌──────▼───────┐  │
│  │  ChatDao     │  │UserProfileDao│  │ ReminderDao  │  │
│  │ (Flow-based) │  │ (Flow-based) │  │ (Flow-based) │  │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  │
│         │                 │                 │          │
│  ┌──────▼──────────────────▼──────────────────▼───────┐ │
│  │         TypeConverters (Converters.kt)            │ │
│  │  - MessageMeta (GSON)                             │ │
│  │  - MessageSender enum (String)                    │ │
│  │  - List<String> (GSON)                            │ │
│  └──────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
         │
    ┌────▼───────────────────────────────┐
    │         SyncManager                 │
    │  ┌──────────────────────────────┐  │
    │  │  observeSyncStatus()         │  │
    │  │  Tracks: Idle/Syncing/etc    │  │
    │  │  Exposes: StateFlow<SyncStatus> │
    │  └──────────────────────────────┘  │
    └────┬───────────────────────────────┘
         │
    ┌────▼────────────────────────────────┐
    │        SyncWorker                   │
    │  (CoroutineWorker)                  │
    │  ┌──────────────────────────────┐  │
    │  │ Runs only on CONNECTED network│  │
    │  │ Period: 15 minutes            │  │
    │  │ Syncs: changed rows (>lastSync)  │
    │  │ Conflict: local wins          │  │
    │  └──────────────────────────────┘  │
    └────────────────────────────────────┘
```

**Entities:**

```kotlin
// 1. Chat Messages (Room)
@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sender: MessageSender,           // Enum converter
    val messageText: String,
    val timestamp: Long,
    val meta: MessageMeta                // GSON converter
)

// 2. User Profile (Room)
@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val name: String,
    val age: String,
    val phone: String,
    val selectedTraits: String,          // JSON serialized list
    val lastUpdated: Long
)

// 3. Reminders (Room)
@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val message: String,
    val scheduledTime: Long,
    val isCompleted: Boolean,
    val createdAt: Long
)
```

**Query Pattern (Flow-Based):**

```kotlin
@Dao
interface ChatDao {
    // Reactive pagination with offset
    @Query("SELECT * FROM chat_messages ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    fun getChatHistory(limit: Int = 20, offset: Int = 0): Flow<List<ChatMessage>>
    
    // For SyncManager: only unsynced rows
    @Query("SELECT * FROM chat_messages WHERE timestamp > :lastSyncedAt")
    suspend fun getUnsyncedMessages(lastSyncedAt: Long): List<ChatMessage>
}
```

**Sync Strategy:**

```kotlin
// 1. Track last sync time in DataStore
val LAST_SYNCED_KEY = longPreferencesKey("last_synced_at")

// 2. On sync, fetch only rows changed since last sync
val unsyncedMessages = chatDao.getUnsyncedMessages(lastSyncedAt)

// 3. Push to remote (simulated by delay)
delay(2000)

// 4. Update last sync time
dataStore.edit { preferences ->
    preferences[LAST_SYNCED_KEY] = System.currentTimeMillis()
}

// 5. Conflict resolution: local always wins (push-based)
```

**Key Features:**
- ✅ All queries return `Flow<T>` for reactivity
- ✅ WorkManager runs only on `NetworkType.CONNECTED`
- ✅ Syncs only changed rows since `lastSyncedAt`
- ✅ Local-wins conflict resolution
- ✅ Observable sync status via StateFlow

**Key Files:**
- `Model.kt`: All entity definitions + TypeConverters
- `AppDatabase.kt`: Room database setup, version=2, migration
- `ChatDao.kt`, `UserProfileDao.kt`, `ReminderDao.kt`: Data access
- `SyncWorker.kt`: Background sync with WorkManager
- `SyncManager.kt`: Sync status observation for UI

---

## Project Structure

```
luminaai/
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt           # Room database singleton
│   │   ├── Model.kt                 # Entities + TypeConverters
│   │   ├── ChatDao.kt               # Chat queries
│   │   ├── UserProfileDao.kt        # User profile queries
│   │   ├── ReminderDao.kt           # Reminder queries
│   │   ├── ProfileDataStore.kt      # DataStore for onboarding
│   │
│   └── sync/
│       ├── SyncWorker.kt            # WorkManager background sync
│       └── SyncManager.kt           # Sync status observable
│
├── domain/
│   └── pipeline/
│       ├── ChatState.kt             # State definitions
│       └── ChatPipeline.kt          # State machine logic
│
├── presentation/
│   ├── onboarding/
│   │   ├── OnboardingScreen.kt      # 3-step composables
│   │   └── OnboardingViewModel.kt   # Onboarding state
│   │
│   └── home/
│       ├── HomeScreen.kt            # Main UI + audio/pagination
│       ├── HomeViewModel.kt         # Chat + sync state
│       ├── AuraCircle.kt            # Canvas animation
│       └── AudioRecorderHelper.kt   # Mic input + amplitude
│
├── ui/
│   └── theme/
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
│
├── MainActivity.kt                  # NavHost + SyncWorker setup
└── AndroidManifest.xml             # Permissions

tests/
└── domain/pipeline/
    └── ChatPipelineTest.kt         # 4 unit tests
```

---

## Features

### ✅ Part 1: Onboarding Flow
- [x] 3-step swipeable onboarding (ValueProps → CollectInfo → Personality)
- [x] Animated text reveal (one prop at a time)
- [x] Form validation (no empty, phone 10 digits, OTP=1234)
- [x] Personality selector (grid, exactly 3 traits)
- [x] DataStore persistence
- [x] Back navigation with data restoration

### ✅ Part 2: Home Screen
- [x] Aura Circle (pure Canvas, no Lottie)
- [x] Idle animation (breathing pulse)
- [x] Listening state (amplitude-reactive glow)
- [x] Mic button + AudioRecord integration
- [x] Custom keyboard slide-up animation
- [x] Chat history from Room (sender, message, timestamp)
- [x] Pagination (20 items/load)
- [x] Parallax fade effect on scroll

### ✅ Part 3: Coroutine State Machine
- [x] 5-state pipeline (Typing → Validating → Processing → Responding → Idle)
- [x] StateFlow + sealed interface
- [x] Cancellation mid-flow
- [x] 8-second timeout → Error state
- [x] Retry option
- [x] 4 unit tests (happy path, cancellation, validation, timeout)

### ✅ Part 4: Offline-First Data Layer
- [x] Room entities (ChatMessage, UserProfile, Reminder)
- [x] MessageMeta TypeConverter (GSON)
- [x] MessageSender enum TypeConverter
- [x] Flow-based queries (pagination support)
- [x] WorkManager sync (NetworkType.CONNECTED)
- [x] lastSyncedAt timestamp logic
- [x] Local-wins conflict resolution
- [x] Observable sync status (StateFlow)

---

## Technology Stack

| Layer | Technology | Purpose |
|-------|-----------|---------|
| **UI Framework** | Jetpack Compose | Declarative UI |
| **Navigation** | Navigation Compose | Screen transitions |
| **State Management** | StateFlow + ViewModel | Reactive state |
| **State Machine** | Coroutines | Message pipeline |
| **Local Storage** | Room Database | Message + profile persistence |
| **Preferences** | DataStore | Encrypted preferences |
| **Background Tasks** | WorkManager | Offline-first sync |
| **Serialization** | GSON | TypeConverter serialization |
| **Audio Input** | AudioRecord | Mic amplitude |
| **Graphics** | Canvas API | Aura animation |
| **Testing** | JUnit 4 + Coroutines Test | Unit tests |
| **DI** | Manual (no Hilt) | Lightweight approach |

---

## Implementation Details

### Onboarding Back Navigation

**Problem**: SavedStateHandle can't store custom objects  
**Solution**: ViewModel stays alive during back navigation, MutableStateFlow preserves state

```kotlin
// ✅ Correct: StateFlow in ViewModel
private val _uiState = MutableStateFlow(OnboardingUiState())
val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

// ❌ Wrong: SavedStateHandle with custom object
savedStateHandle["uiState"] = _uiState.value  // Crashes!
```

### Audio Amplitude Visualization

**Design**: LaunchedEffect collects amplitude flow when listening

```kotlin
LaunchedEffect(isListening) {
    if (isListening) {
        audioRecorderHelper.getAmplitudeFlow().collect { amplitude ->
            audioAmplitude = amplitude  // Real-time updates
        }
    } else {
        audioAmplitude = 0f
    }
}
```

### State Machine Timeout

**Design**: `withTimeoutOrNull()` for graceful timeout handling

```kotlin
val response = withTimeoutOrNull(8000L) {
    processMessageNetworkCall(message)
}

if (response == null) {
    // Timeout exceeded - move to Error state
    _chatState.value = ChatState.Error("Timeout exceeded 8 seconds")
    return@launch
}
```

### Room Schema Migration

**Version 1 → 2**: Added UserProfileEntity and Reminder tables

```kotlin
@Database(
    entities = [ChatMessage::class, UserProfileEntity::class, Reminder::class],
    version = 2,  // Incremented
    exportSchema = false
)
```

---

## Setup & Installation

### Prerequisites
- Android Studio Giraffe or later
- Android SDK 24+
- Kotlin 1.9+

### Steps

1. **Clone Repository**
   ```bash
   git clone <repo-url>
   cd kastack-android-assignment
   ```

2. **Build Project**
   ```bash
   ./gradlew build
   ```

3. **Run on Device/Emulator**
   ```bash
   ./gradlew installDebug
   ```

4. **Grant Permissions**
   - RECORD_AUDIO: Required for mic input (runtime permission)
   - INTERNET: Optional (for future cloud sync)

---

## Testing

### Unit Tests

```bash
# Run all tests
./gradlew test

# Run specific test
./gradlew test --tests ChatPipelineTest
```

### Test Coverage

**ChatPipelineTest.kt** (4 tests):
1. ✅ Happy path transition (Typing → Validating → Processing → Responding → Idle)
2. ✅ Cancellation mid-flow (new message cancels current job)
3. ✅ Validation error (empty message triggers Error immediately)
4. ✅ Timeout scenario (8s Processing timeout triggers Error)

---

## Design Patterns Used

| Pattern | Usage | File |
|---------|-------|------|
| **Clean Architecture** | Layer separation | Entire project |
| **MVVM** | UI state management | ViewModel + Compose |
| **State Machine** | Message processing | ChatPipeline |
| **Singleton** | Database instance | AppDatabase |
| **Repository** | Data access abstraction | DAO interfaces |
| **Sealed Interface** | Type-safe states | ChatState |
| **Flow** | Reactive streams | Room queries |
| **Builder** | Complex object creation | Room + WorkManager |

---

## Performance Considerations

1. **Room Pagination**: Limits queries to 20 items/load (10KB average)
2. **DataStore**: Async preferences (non-blocking)
3. **WorkManager**: Respects device constraints (no battery drain)
4. **Canvas Animation**: 60fps breathing pulse (efficient)
5. **Audio Buffer**: 50ms emit rate (smooth but efficient)

---

## Future Enhancements

- [ ] Cloud backend integration (optional)
- [ ] Local LLM inference (e.g., ONNX)
- [ ] Voice response generation (TTS)
- [ ] Dark mode support
- [ ] Backup/restore via cloud
- [ ] Multi-user profiles
- [ ] Custom reminder notifications

---

## License

MIT License - See LICENSE file

---

## Author

Paras Patil (@PatilParas05)

---

**Last Updated**: June 11, 2026  
**Version**: 1.0.0
