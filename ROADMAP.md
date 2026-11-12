# HearMe: Strategic Product & Technical Roadmap

## Milestone 1: Project Foundation & Architecture (Completed)
- Complete multi-module Gradle configuration (13 logical modules).
- Clean Architecture contracts established for Audio, Bluetooth, Media, ML, and Orchestration.
- Unit-tested test doubles for all core subsystems.
- Production-ready Room entities, DAOs, and DataStore repository.
- Complete Material 3 UI with Navigation Compose (Onboarding, Home, Training, Settings, History).
- App builds and launches on Android emulator with zero compilation errors.

---

## Milestone 1.1: Architecture Audit & Hilt Migration (Completed)
- Migrated from manual `AppContainer` to Dagger Hilt 2.55.
- Verified SDK compatibility: `compileSdk release(37)`, `targetSdk 36`, `minSdk 29`.
- Isolated test doubles from production DI bindings.
- Honest status guarantee: UI displays `STANDBY` and `DISCONNECTED` until real hardware confirms activation.

---

## Milestone 2: Real Audio Engine, Earphone Detection & Voice Activity Detection (Completed)
- Concrete `AudioCaptureManager` using Android `AudioRecord` with 16kHz 16-bit mono PCM streaming via `VOICE_RECOGNITION`.
- Real `EarphoneConnectionMonitor` using `AudioManager` and `AudioDeviceCallback` for Bluetooth A2DP, SCO, wired, and USB earphones.
- Real-time energy-based `SpeechActivityDetector` (`EnergyVadDetector`) with RMS calculation, adaptive noise floor tracking, and temporal hysteresis.
- `SmartListeningService` foreground microphone service with persistent notification and stop action.
- Dynamic `RECORD_AUDIO` permission workflow in UI.
- Real-time audio level progress bar and speech indicator on the Home dashboard.

---

## Milestone 3: Speech Activity Detection & Intent Modeling (Next)
- Local Voice Activity Detection (VAD) using ONNX Runtime / TensorFlow Lite (Silero VAD model).
- Directed speech classifier to differentiate ambient babble from nearby speakers directly addressing the user.
- Keyword / User-name recognition model operating completely offline.
- Decision engine tuning for low false-positive rate.

---

## Milestone 4: Personalization Engine & No-Earphone Training
- Speaker verification model to profile the user's voice and exclude user self-speech.
- Dedicated training workflow allowing users to speak calibration phrases without earphones.
- Ambient noise baseline estimation for dynamic threshold shifting.

---

## Milestone 5: Active Learning & Feedback Loop
- Interactive YES / NO notifications following media interruptions.
- On-device threshold optimization based on recorded user feedback.
- Detailed accuracy analytics in the History destination.

---

## Milestone 6: Battery Optimization & Hardware Acceleration
- Power profiling using Android Studio Energy Profiler.
- NNAPI / GPU delegation for neural inference.
- Low-power background duty-cycling when no voice energy is present.
