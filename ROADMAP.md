# HearMe: Strategic Product & Technical Roadmap

## Milestone 1: Project Foundation & Architecture (Current)
- Complete multi-module Gradle configuration (13 logical modules).
- Clean Architecture contracts established for Audio, Bluetooth, Media, ML, and Orchestration.
- Unit-tested test doubles for all core subsystems.
- Production-ready Room entities, DAOs, and DataStore repository.
- Complete Material 3 UI with Navigation Compose (Onboarding, Home, Training, Settings, History).
- App builds and launches on Android emulator with zero compilation errors.

---

## Milestone 2: Audio Engine & System Integrations
- Concrete `AudioCaptureManager` using Android `AudioRecord` with 16kHz 16-bit PCM streaming.
- `EarphoneConnectionMonitor` listening for Bluetooth Headset (A2DP / SCO) and wired headphone broadcasts via `AudioManager.registerAudioDeviceCallback`.
- Foreground service implementation (`SmartListeningService`) with `FOREGROUND_SERVICE_MICROPHONE`.
- Audio focus and media playback controller implementing pause/resume and audio ducking.

---

## Milestone 3: Speech Activity Detection & Intent Modeling
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
