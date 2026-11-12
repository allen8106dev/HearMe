# HearMe: Changelog

All notable changes to the HearMe Android application are documented in this file.

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
