# HearMe: Feature Catalog & Milestone Status

## 1. Feature Overview

HearMe provides an intelligent audio-awareness layer between the user and their physical environment while wearing earphones.

---

## 2. Feature Implementation Status Matrix

| Feature Domain | Feature Name | Description | Milestone 1 Status | Planned Milestone |
| :--- | :--- | :--- | :--- | :--- |
| **System Architecture** | Multi-Module Foundation | 13 independent Gradle modules with strict Clean Architecture separation | **Implemented & Tested** | M1 |
| **System Architecture** | Swappable Contracts | Interfaces + Test Doubles for Audio, BT, Media, ML, and Orchestration | **Implemented & Tested** | M1 |
| **System Architecture** | Dependency Injection | Dagger Hilt (`2.55`) dependency injection container, ViewModel injection, and production-safe hardware modules | **Implemented & Tested** | M1.1 |
| **Storage & Persistence**| Room Database | Room 2.7.2 schema for Training Examples, Listening Sessions, Model Metadata | **Implemented & Tested** | M1.1 |
| **Storage & Persistence**| DataStore Preferences | Reactive DataStore preferences for sensitivity, auto-activation, and behavior | **Implemented & Tested** | M1 |
| **User Interface** | Design System | Material 3 typography, dynamic color support, polished acoustic palette | **Implemented & Tested** | M1 |
| **User Interface** | Navigation | Jetpack Navigation Compose with start-destination switching based on onboarding | **Implemented & Tested** | M1 |
| **User Interface** | Onboarding Screen | Brand welcome, privacy guarantees, and contextual permission education | **Implemented & Tested** | M1 |
| **User Interface** | Home Dashboard | Smart Listening toggle, status indicator, earphone monitor, quick navigation | **Implemented & Tested** | M1 |
| **User Interface** | Training Placeholder | Comprehensive roadmap & visual cards explaining forthcoming model training | **Implemented & Tested** | M1 |
| **User Interface** | Settings Screen | Interactive sensitivity picker, auto-activation toggle, ducking vs pausing radio | **Implemented & Tested** | M1 |
| **User Interface** | History Screen | Chronological session list, trigger sources, outcomes, and clear-history action | **Implemented & Tested** | M1 |
| **Audio Processing** | PCM Audio Capture | Low-latency AudioRecord buffer management with background thread dispatching | **Implemented (Real AudioRecord)** | M2 |
| **Audio Processing** | Speech Activity Detection | On-device Voice Activity Detection (VAD) via RMS energy and adaptive noise tracking | **Implemented (EnergyVadDetector)** | M2 |
| **Bluetooth & Routing** | Earphone Auto-Detection | AudioDeviceCallback hardware detection (BT A2DP, BT SCO, Wired, USB) | **Implemented (RealEarphoneConnectionMonitor)** | M2 |
| **Service & Lifecycle** | Foreground Service | Foreground microphone service with persistent notification and stop action | **Implemented (SmartListeningService)** | M2 |
| **Media Control** | Media Ducking / Pausing | Android MediaSession / AudioFocus controller to pause and lower music | Contract Established (Stubs) | M3 |
| **Intelligence** | Directed Speech Filter | Differentiating acoustic direction & proximity (speaker addressing listener) | Contract Established (Fake) | M3 |
| **Intelligence** | Keyword & Name Spotting | Local Wake-word / Name recognition without cloud connection | Contract Established (Fake) | M3 |
| **Personalization** | Voice Profiling | Training user's voice model without earphones to prevent self-interruption | Architectural Placeholder | M4 |
| **Personalization** | YES / NO Feedback Loop | User feedback mechanism to fine-tune sensitivity thresholds | Architectural Placeholder | M5 |


---

## 3. Honest Status Guarantee (Milestone 1.1)

In adherence to the core architectural principles of HearMe:
- **No Mock Data in Production UI**: When launched in production mode, HearMe displays honest hardware state: `STANDBY` (or `Inactive`) when no listening service is capturing audio, and `DISCONNECTED` when no actual private earphone is confirmed attached.
- **No Simulated AI**: The app does not display simulated transcripts or fabricated training progress.
- **Isolated Fakes**: Fake test doubles (`FakeAudioCaptureManager`, `FakeEarphoneConnectionMonitor`, etc.) are isolated and restricted to unit tests, while production builds use explicit standalone fallback objects that cleanly fail or yield empty streams until milestone implementation.
