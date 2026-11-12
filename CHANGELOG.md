# HearMe: Changelog

All notable changes to the HearMe Android application are documented in this file.

## [0.2.0] - Milestone 2: Real Audio Engine, Earphone Detection & Voice Activity Detection

### Added
- **Real Microphone Audio Capture (`RealAudioCaptureManager`)**:
  - Implemented `AudioCaptureManager` using Android `AudioRecord`.
  - Captures mono, 16 kHz, 16-bit PCM using `VOICE_RECOGNITION` audio source.
  - Dedicated coroutine dispatcher (`Dispatchers.IO`) prevents main-thread blocking.
  - Robust permission check, initialization recovery, and explicit resource disposal.
  - Ephemeral memory handling: raw audio is never written to disk storage.
- **Real Earphone Connection Monitor (`RealEarphoneConnectionMonitor`)**:
  - Implemented `EarphoneConnectionMonitor` and `AudioDeviceStateProvider` using `AudioDeviceCallback`.
  - Real-time reactive detection of Bluetooth A2DP, Bluetooth SCO, 3.5 mm wired headsets, wired headphones, and USB audio peripherals.
  - Safe extraction of device names respecting `BLUETOOTH_CONNECT` permission on Android 12+.
- **Real-Time Voice Activity Detection (`EnergyVadDetector`)**:
  - Implemented lightweight, zero-allocation energy-based VAD using Root Mean Square (RMS) calculation.
  - Exponentially smoothed adaptive ambient noise floor (`alpha = 0.05`).
  - Minimum speech duration requirement (`120ms`) prevents clicks and spikes from triggering false alarms.
  - Silence hangover delay (`300ms`) ensures smooth continuity during natural speech pauses.
  - Continuous speech activity likelihood calculation.
- **Foreground Listening Service (`SmartListeningService`)**:
  - Foreground service with `FOREGROUND_SERVICE_MICROPHONE` type and persistent notification.
  - One-tap "Stop Listening" notification action for immediate user control.
  - Live reactive StateFlow singletons (`liveServiceState`, `liveRmsEnergy`, `isSpeechActive`).
  - Decoupled `ServiceController` abstraction enabling unit-testable service triggering.
- **Home Dashboard Updates**:
  - Real-time audio level progress bar and "SPEECH" detection indicator badge.
  - Expanded `ListeningState` domain covering `STANDBY`, `STARTING`, `ACTIVE_LISTENING`, `SPEECH_DETECTED`, `MICROPHONE_UNAVAILABLE`, `INTERRUPTED`, `STOPPED`, and `ERROR`.
  - Dynamic `RECORD_AUDIO` permission request launcher wired to toggle switch.
- **Unit Tests**:
  - RMS mathematical verification on silence and pure sine waves.
  - VAD thresholding, min speech duration, silence hangover, and reset lifecycle tests.
  - Expanded earphone tests covering wired headphones and USB headsets.

---

## [0.1.1] - Milestone 1.1: Architecture Audit & Hilt Migration

### Added
- **Dagger Hilt Dependency Injection**:
  - Replaced manual `AppContainer` with Dagger Hilt `2.55`.
  - Configured `@HiltAndroidApp`, `@AndroidEntryPoint`, and `@HiltViewModel`.
  - Isolated test doubles from production DI modules.
  - Honest hardware state reporting in initial production modules.

---

## [0.1.0] - Milestone 1: Project Foundation & Architecture

### Added
- **Multi-Module Gradle Architecture**:
  - Configured 13 modules: `:app`, `:core:common`, `:core:database`, `:core:audio`, `:core:bluetooth`, `:core:media`, `:core:ml`, `:feature:smartlistening`, `:feature:onboarding`, `:feature:home`, `:feature:training`, `:feature:settings`, `:feature:history`.
  - Configured Gradle Kotlin DSL with Version Catalog (`gradle/libs.versions.toml`).
  - Target Android SDK 37 (compileSdk = release(37)), sensible minSdk = 29.
- **Architectural Contracts & Test Doubles**:
  - Audio: `AudioCaptureManager`, `SpeechActivityDetector`, `AudioSegmentProcessor`.
  - Bluetooth: `EarphoneConnectionMonitor`, `AudioDeviceStateProvider`.
  - Media: `MediaPlaybackController`, `AudioFocusManager`.
  - Machine Learning & Intelligence: `SpeechRecognizer`, `SpeechClassifier`, `IntentClassifier`, `ConfidenceEvaluator`, `TrainingManager`, `FeedbackRepository`, `PersonalisationEngine`.
  - Smart Listening: `ListeningOrchestrator`, `ListeningDecisionEngine`.
  - Comprehensive fake implementations for every contract to facilitate hermetic testing and UI development.
- **Database & Preferences**:
  - Room 2.6 database setup with `TrainingExampleEntity`, `ListeningSessionEntity`, and `ModelMetadataEntity`.
  - Corresponding DAOs and repository abstractions (`TrainingExampleDao`, `ListeningSessionDao`, `ModelMetadataDao`).
  - Jetpack DataStore preferences repository (`DataStoreUserPreferencesRepository`) for lightweight settings.
- **Material 3 User Interface**:
  - Onboarding destination explaining permissions (Microphone, Bluetooth, Foreground Service, On-Device processing) without immediate permission prompting.
  - Home destination with HearMe branding, Smart Listening toggle, status pills, earphone connection card, prominent "Start Training" action, and quick navigation.
  - Training destination placeholder clearly distinguished from active features with planned roadmap cards.
  - Settings destination with detection sensitivity picker, auto-activation switch, music interruption behavior selector (Pause vs Duck), and privacy indicator.
  - History destination showing chronological session logs and clear-history action.
  - Full Jetpack Navigation Compose integration (`AppNavHost`).
  - Custom acoustic brand design system and theme (`HearMeTheme`).
- **Dependency Injection & App Container**:
  - `AppContainer` and `DefaultAppContainer` providing scoped singletons and custom ViewModel factory.
  - `HearMeApplication` initializing the application container on launch.
- **Testing**:
  - Comprehensive unit test suites across all core and feature modules.
- **Documentation**:
  - `ARCHITECTURE.md`, `FEATURES.md`, `ROADMAP.md`, `TESTING.md`, `CHANGELOG.md`.
