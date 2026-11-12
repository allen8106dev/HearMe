# HearMe: Testing Strategy & Verification Report

## 1. Testing Philosophy

HearMe enforces strict testability through Clean Architecture:
1. **Isolated Business Logic:** Core components depend strictly on interfaces, enabling fast, hermetic unit tests that execute on the JVM without requiring an Android device or emulator.
2. **Coroutines & Flows:** Unit tests utilize `kotlinx.coroutines.test.runTest`, `StandardTestDispatcher`, and `UnconfinedTestDispatcher` with structured concurrency to test async streams deterministically.
3. **Repository & Storage Verification:** Repositories have standalone unit test suites validating CRUD operations, state flow emissions, and preference serialization.

---

## 2. Test Suites Executed

### `core:common` (`CoreCommonTest.kt`)
- `resultSuccessReturnsData`: Verifies `HearMeResult.Success` wraps and delivers data correctly.
- `resultErrorReturnsException`: Verifies `HearMeResult.Error` preserves exceptions and message traces.
- `timeProviderReturnsCurrentTime`: Verifies `DefaultTimeProvider` emits valid millisecond and epoch second timestamps.

### `core:audio` (`CoreAudioTest.kt`)
- `audioCaptureManagerStartStopCycle`: Verifies capture lifecycle state transitions and start/stop controls.
- `audioCaptureStreamEmitsCorrectly`: Verifies unbuffered audio chunk emission and collection over `Flow<AudioChunk>`.
- `speechActivityDetectorReportsActivity`: Verifies confidence, energy RMS metrics, and detector reset count.
- `segmentProcessorReturnsResult`: Verifies chunk aggregation and segment processing.
- `rmsCalculationOnSilenceReturnsZero`: Mathematical verification that zero-filled PCM returns 0.0f RMS.
- `rmsCalculationOnSineWaveReturnsExpectedValue`: Mathematical verification that 0.7071 amplitude sine wave yields ~0.50f RMS.
- `vadSilenceBelowThresholdDoesNotTriggerSpeech`: Verifies quiet background chunks remain `hasSpeech = false` and 0.0 confidence.
- `vadSpeechAboveThresholdTriggersSpeechAfterMinDuration`: Verifies that speech requires sustained energy exceeding `minSpeechDurationMs` (temporal hysteresis).
- `vadSilenceHangoverPreservesSpeechStateTemporarily`: Verifies hangover window maintains active speech during brief pauses between words, and clears after timeout.
- `vadResetCleansInternalState`: Verifies that `reset()` reinitializes noise floor and temporal timestamps.

### `core:bluetooth` (`CoreBluetoothTest.kt`)
- `monitorTracksEarphoneConnectionTransitions`: Verifies connection, disconnection, and device metadata propagation.
- `monitorTracksWiredAndUsbHeadsets`: Verifies 3.5 mm wired headphones and USB-C headsets are correctly identified.
- `deviceStateProviderDetectsHeadsetPresence`: Verifies differentiation between internal speakers and wired/Bluetooth headphones.

### `feature:smartlistening` (`SmartListeningTest.kt`)
- `decisionEngineEvaluatesInterruptionAction`: Verifies heuristic decision making (ignoring ambient noise, ducking/pausing when directed speech occurs with earphones connected).
- `orchestratorStartsInStandbyState`: Verifies orchestrator starts in honest `STANDBY` state.
- `orchestratorTransitionsToActiveOnStart`: Verifies transition to `ACTIVE_LISTENING` upon successful audio capture start.
- `orchestratorTransitionsToStandbyOnStop`: Verifies graceful return to `STANDBY` when stopped.
- `orchestratorTransitionsToMicUnavailableOnPermissionError`: Verifies transition to `MICROPHONE_UNAVAILABLE` on `SecurityException`.
- `fakeOrchestratorTransitionsCorrectly`: Verifies fake orchestrator lifecycle.

### `app` (`HearMeViewModelTest.kt`)
- `togglingSmartListeningUpdatesState`: Verifies ViewModel routes toggle commands to `ServiceController` without direct Android Context dependency.
- `earphoneConnectionUpdatesUiState`: Verifies earphone state updates trigger auto-activation and UI reflection.
- `preferencesUpdateChangesUiState`: Verifies changes to sensitivity and music interruption mode propagate to UI state.

---

## 3. Milestone 2 Verification Summary

- **Full Test Suite Execution (`.\gradlew.bat test`)**:
  - `BUILD SUCCESSFUL` across all 13 modules.
  - Zero test failures, 100% test pass rate across core contracts, real VAD engine, and feature modules.
- **Hardware Abstraction & DI Verification**:
  - `RealAudioCaptureManager` wired via Hilt using `VOICE_RECOGNITION` audio source and 16 kHz mono PCM 16-bit format.
  - `RealEarphoneConnectionMonitor` wired via Hilt implementing both `EarphoneConnectionMonitor` and `AudioDeviceStateProvider`.
  - `EnergyVadDetector` wired via Hilt implementing `SpeechActivityDetector`.
  - `SmartListeningService` registered as foreground service (`microphone`) with persistent notification.
  - `ServiceController` abstraction decouples ViewModels from `android.content.Context`.
- **Emulator Validation (`emulator-5554`)**:
  - Debug APK built and installed via `adb install -r`.
  - Launched without crash (`am start` confirmed active).
  - Top resumed activity: `com.allen.hearme/.MainActivity`.
  - Real-time VAD energy indicator and speech detection pill integrated cleanly into the Compose UI.
