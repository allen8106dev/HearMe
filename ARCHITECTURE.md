# HearMe: System Architecture & Design Documentation

## 1. Overview & System Purpose

**HearMe** is an intelligent personal audio-awareness assistant designed for Android. Its primary purpose is to allow users to stay immersed in their music or podcasts through earphones while remaining reliably aware of real-world communications directed at them (e.g., someone calling their name, asking a direct question, or trying to get their attention).

The architecture adheres strictly to **Clean Architecture** and **Modern Android Development (MAD)** principles, incorporating:
- Strict modular isolation with dependency inversion.
- Reactive, unidirectional data flow (UDF) powered by Kotlin `StateFlow` and `SharedFlow`.
- Testable architectural interfaces and swappable test doubles for all external dependencies (Audio, Bluetooth, Media, ML).
- Robust local storage leveraging Room for structured relational data and Jetpack DataStore for user preferences.
- 100% on-device processing to guarantee complete privacy and zero cloud dependence for audio intelligence.

---

## 2. Modular Architecture & Dependency Graph

```
                              +-----------------------+
                              |         :app          |
                              +-----------+-----------+
                                          |
                +-------------------------+-------------------------+
                |                         |                         |
                v                         v                         v
     +--------------------+     +--------------------+    +--------------------+
     |   :feature:home    |     |  :feature:training |    |  :feature:settings |
     +---------+----------+     +---------+----------+    +---------+----------+
               |                          |                         |
               |                          |                         |
               v                          v                         v
     +--------------------+     +--------------------+    +--------------------+
     |:feature:smartlistening|  | :feature:history   |    |:feature:onboarding |
     +---------+----------+     +---------+----------+    +---------+----------+
               |                          |                         |
     +---------+--------+--------+--------+--------+                |
     |                  |        |                 |                |
     v                  v        v                 v                v
+----------+      +----------+ +-----------+ +-----------+   +---------------+
|:core:audio|     |:core:bt  | |:core:media| | :core:ml  |   |:core:database |
+----+-----+      +----+-----+ +-----+-----+ +-----+-----+   +-------+-------+
     |                 |             |             |                 |
     +-----------------+-------------+-------------+-----------------+
                                     |
                                     v
                           +-------------------+
                           |   :core:common    |
                           +-------------------+
```

### Module Responsibilities:

| Module | Type | Purpose | Dependencies |
| :--- | :--- | :--- | :--- |
| `:app` | Application | Application entry point (`HearMeApplication`), Hilt DI root (`@HiltAndroidApp`), Navigation graph (`AppNavHost`), Theme | All feature and core modules |
| `:core:common` | Library | Common result types (`HearMeResult`), dispatchers (`DispatcherProvider`), time (`TimeProvider`), logging | Kotlin Coroutines, AndroidX Core |
| `:core:database` | Library | Room database, DAOs, entities, migrations, DataStore preferences repository | `:core:common`, Room, DataStore |
| `:core:audio` | Library | Contracts for PCM audio capture, speech activity detection (VAD), audio segment processing | `:core:common` |
| `:core:bluetooth` | Library | Contracts for wireless/wired headset connection monitoring and audio device state | `:core:common` |
| `:core:media` | Library | Contracts for audio ducking, pause/resume, and system audio focus management | `:core:common` |
| `:core:ml` | Library | Contracts for speech recognition, directed speech classification, intent parsing, training manager | `:core:common`, `:core:audio` |
| `:feature:smartlistening`| Library | High-level orchestration (`ListeningOrchestrator`) and decision logic (`ListeningDecisionEngine`) | `:core:common`, `:core:audio`, `:core:bluetooth`, `:core:media`, `:core:ml`, `:core:database` |
| `:feature:home` | UI (Compose) | Main dashboard: Smart Listening toggle, status cards, earphone state, navigation triggers | `:core:common`, `:core:bluetooth`, `:feature:smartlistening` |
| `:feature:training` | UI (Compose) | Training interface placeholder for personalized voice profiles and ambient calibration | `:core:common`, `:core:ml` |
| `:feature:settings` | UI (Compose) | Sensitivity adjustments, auto-activation preferences, interruption behavior, privacy status | `:core:common`, `:core:database` |
| `:feature:history` | UI (Compose) | Chronological log of listening sessions, triggers, and detection counts | `:core:common`, `:core:database` |
| `:feature:onboarding` | UI (Compose) | First-launch welcome, privacy guarantees, and contextual permission education | `:core:common`, `:core:database` |

---

## 3. Core Architectural Contracts

All critical subsystems are abstracted behind interfaces with zero concrete coupling between feature layers:

### Audio Subsystem (`:core:audio`)
- `AudioCaptureManager`: Exposes `isCapturing: StateFlow<Boolean>` and `audioStream: Flow<AudioChunk>`, with non-blocking `startCapture(config)` and `stopCapture()`.
- `SpeechActivityDetector`: Evaluates `AudioChunk` to detect voice presence with confidence and energy RMS.
- `AudioSegmentProcessor`: Slices, normalizes, and filters audio segments for downstream classification.

### Bluetooth Subsystem (`:core:bluetooth`)
- `EarphoneConnectionMonitor`: Exposes `connectionState: StateFlow<EarphoneConnectionState>` and `connectedDevice: StateFlow<AudioDeviceInfo?>`.
- `AudioDeviceStateProvider`: Queries system audio routing (Bluetooth A2DP, Headset, Wired) and verifies if private listening devices are plugged in.

### Media Subsystem (`:core:media` & `:app`)
- `MediaPlaybackController`: Controls background music via `MediaSessionManager` and `MediaController.transportControls` (`pauseMedia()`, `resumeMedia()`, `lowerVolume()`, `restoreVolume()`, `restorePlayback()`, `cancelInterruption()`).
- `RealMediaPlaybackController`: Production implementation residing in `:app` that binds to `MediaSessionManager` using `HearMeNotificationListenerService`. Gracefully falls back to `AudioFocusManager` when Notification Listener access has not been granted.
- `HearMeNotificationListenerService`: Android `NotificationListenerService` declared in `AndroidManifest.xml` with `BIND_NOTIFICATION_LISTENER_SERVICE`. Provides the component handle required by Android's `MediaSessionManager.getActiveSessions()` without reading or persisting user notification text.
- `AudioFocusManager` & `RealAudioFocusManager`: Manages audio focus requests (`AUDIOFOCUS_GAIN_TRANSIENT` and `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK`) with system `AudioManager`. Enforces safety checks to prevent ducking or pausing during active phone calls (`MODE_IN_CALL`, `MODE_IN_COMMUNICATION`, `MODE_RINGTONE`).
- `InterruptionRecord`: Safeguard data structure capturing session ID, previous playback state, position, and action taken. Ensures HearMe NEVER resumes music that the user had manually paused or changed.

### Intelligence Subsystem (`:core:ml`)
- `SpeechRecognizer`: Generates text transcripts from segmented speech.
- `SpeechClassifier`: Distinguishes between speech directed at the user vs. background noise.
- `IntentClassifier`: Parses intent from speech (e.g. calling the user's name, urgent warnings, casual bystander remarks).
- `ConfidenceEvaluator`: Computes weighted composite confidence across VAD, directional speech, and intent models.
- `TrainingManager`: Manages on-device sample capture and model parameter fine-tuning.
- `FeedbackRepository`: Persists YES/NO user feedback on interruption accuracy.
- `PersonalisationEngine`: Adapts sensitivity thresholds and personal acoustic offsets.

### Smart Listening Subsystem (`:feature:smartlistening`)
- `ListeningOrchestrator`: Coordinates the full lifecycle: monitors earphones, starts audio capture, evaluates audio, triggers music pause/ducking, and logs sessions.
- `ListeningDecisionEngine`: Pure rule/heuristic engine evaluating whether the current acoustic context justifies interrupting the user's music.

---

## 4. Database & Storage Architecture

### Room Database (`hearme_database.db`)
Configured with Room 2.7.2, KSP annotation processor (`2.2.10-2.0.2`), and pre-allocated migration hooks:
1. `TrainingExampleEntity` (`training_examples`):
   - `id: String` (Primary Key)
   - `timestamp: Long`
   - `classification: String`
   - `transcription: String?`
   - `confidence: Float`
   - `language: String`
   - `environment_metadata: String`
   - `model_version: String`
2. `ListeningSessionEntity` (`listening_sessions`):
   - `id: String` (Primary Key)
   - `start_time: Long`
   - `end_time: Long?`
   - `trigger_source: String`
   - `session_outcome: String`
   - `detection_count: Int`
3. `ModelMetadataEntity` (`model_metadata`):
   - `model_identifier: String` (Primary Key)
   - `version: String`
   - `installation_status: String`
   - `model_configuration: String`
   - `last_updated: Long`

### Jetpack DataStore (`hearme_preferences.preferences_pb`)
Stores simple application preferences to prevent SQLite overhead for key-value settings:
- `listening_enabled`: Boolean
- `automatic_activation`: Boolean
- `detection_sensitivity`: String (`LOW`, `MEDIUM`, `HIGH`)
- `preferred_behavior`: String (`PAUSE_MUSIC`, `LOWER_VOLUME`)
- `training_reminders`: Boolean
- `onboarding_completed`: Boolean


---

## 5. Dependency Injection Architecture (Hilt)

The application utilizes **Dagger Hilt** (`2.55`) for dependency injection across the system:
- **`HearMeApplication`**: Annotated with `@HiltAndroidApp` as the dependency container root.
- **`MainActivity`**: Annotated with `@AndroidEntryPoint` providing view-layer injection.
- **`HearMeViewModel`**: Annotated with `@HiltViewModel` receiving injected repositories and orchestrators via constructor injection.
- **`DatabaseModule`**: Singleton module providing Room instances (`HearMeDatabase`), DAOs, DataStore, and database repository abstractions.
- **`ProductionHardwareModule`**: Production-safe module binding standalone interface implementations that honestly report initial hardware states (e.g. `isCapturing = false`, `EarphoneConnectionState.Disconnected`, uninitialized model exceptions).
- **Test Double Isolation**: Test doubles (`FakeAudioCaptureManager`, `FakeEarphoneConnectionMonitor`, etc.) reside in test packages and test modules, completely isolated from production injection bindings.

---

## 6. Build & SDK Specifications

| Property | Value | Platform Name / Alignment |
|---|---|---|
| **`compileSdk`** | `release(37)` | Android SDK API 37 |
| **`targetSdk`** | `36` | Android 16 (API 36) |
| **`minSdk`** | `29` | Android 10 (API 29) (Required for modern audio/BT routing) |
| **Android Gradle Plugin** | `9.3.3` | AGP 9.3 with built-in Kotlin support |
| **Kotlin Version** | `2.2.10` | Built-in compiler (Language & API target 2.1 for Hilt metadata alignment) |
| **KSP Version** | `2.2.10-2.0.2` | Matching Kotlin 2.2.10 symbol processing |
| **Hilt Version** | `2.55` | Dagger Hilt with KSP and `android.newDsl=false` compatibility |
| **Compose BOM** | `2026.02.01` | Material 3 & Navigation Compose `2.8.8` |
| **Room Database** | `2.7.2` | KSP code generation |

---

## 7. Milestone 2: Hardware-Integrated Audio & Detection Engine

### Real Audio Capture (`RealAudioCaptureManager`)
- Implements `AudioCaptureManager` using Android `AudioRecord`.
- **Target audio format**: Mono, 16 kHz sample rate, 16-bit PCM (`ENCODING_PCM_16BIT`).
- **Audio source**: `MediaRecorder.AudioSource.VOICE_RECOGNITION` for hardware-tuned speech pre-processing.
- **Buffer size**: 40 ms frames (640 samples / 1280 bytes per chunk) read asynchronously on `Dispatchers.IO`.
- **Robustness**: Validates `RECORD_AUDIO` permission before capture, gracefully recovers from audio record initialization errors, and releases native `AudioRecord` resources cleanly on cancellation or service stoppage.
- **Privacy**: Raw PCM buffers remain strictly in transient heap memory during frame evaluation and are never written to persistent disk storage.

### Real Earphone Detection (`RealEarphoneConnectionMonitor`)
- Implements `EarphoneConnectionMonitor` and `AudioDeviceStateProvider` using Android `AudioManager` and `AudioDeviceCallback`.
- Monitors hardware device connections and disconnections in real time across:
  - Bluetooth A2DP audio sinks (`TYPE_BLUETOOTH_A2DP`)
  - Bluetooth SCO headset sinks (`TYPE_BLUETOOTH_SCO`)
  - 3.5 mm wired headsets (`TYPE_WIRED_HEADSET`)
  - 3.5 mm wired headphones (`TYPE_WIRED_HEADPHONES`)
  - USB audio headsets and peripherals (`TYPE_USB_HEADSET`, `TYPE_USB_DEVICE`)
- Safely accesses Bluetooth device names only when `BLUETOOTH_CONNECT` permission is granted (Android 12+), gracefully falling back to descriptive device type labels otherwise.

### Voice Activity Detection (`EnergyVadDetector`)
- Implements `SpeechActivityDetector` with zero per-frame heap allocations.
- **Energy Metric**: Root Mean Square (RMS) amplitude calculated from 16-bit little-endian PCM samples, normalized to `[0.0, 1.0]`.
- **Adaptive Noise Tracking**: Continuously adapts background ambient noise level using exponential moving average (`alpha = 0.05`), strictly during verified quiet intervals.
- **Configurable Sensitivity**: Maps user sensitivity (`0.0` to `1.0`) to a dynamic Signal-to-Noise Ratio (SNR) threshold multiplier (`1.8x` to `5.0x`).
- **Temporal Stability**:
  - Minimum speech duration requirement (`minSpeechDurationMs = 120ms` default, configurable) to ignore short non-vocal transients/clicks.
  - Silence hangover delay (`silenceTimeoutMs = 300ms` default) preventing flutter/choppiness between words during continuous speech.

### Foreground Listening Service (`SmartListeningService`)
- Hosts continuous background microphone capture under Android 14+ foreground service policies.
- Type: `android:foregroundServiceType="microphone"`.
- Features an ongoing, non-dismissible notification with a one-tap **Stop Listening** action.
- Exposes reactive, single-source-of-truth StateFlows (`liveServiceState`, `liveRmsEnergy`, `isSpeechActive`) consumed by the UI.
- Abstraction: `ServiceController` interface decouples the ViewModel from the Android `Context` framework class, enabling unit testability without mock frameworks.

