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

### `core:media` (`CoreMediaTest.kt`)
- `playbackControllerRecordsActionsAndVolume`: Verifies pause, resume, ducking, and volume restore commands.
- `audioFocusManagerRequestsAndAbandonsFocus`: Verifies audio focus request and abandon transitions.
- `tracksMediaMetadataAndNotificationAccess`: Verifies reactive emissions of `MediaTrackInfo` and Notification Listener access state.
- `restorePlaybackResumesOnlyIfPreviouslyPlaying`: Verifies safeguard that HearMe NEVER resumes music if it was already paused before the interruption occurred.
- `cancelInterruptionClearsActiveRecord`: Verifies that cancelling an interruption safely clears the active `InterruptionRecord`.

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

### `core:database` (`CoreDatabaseTest.kt`)
- `listeningSessionRepositoryPerformsCrud`: Verifies session creation, updates, and clearing.
- `trainingExampleRepositoryManagesExamples`: Verifies training example storage and retrieval.
- `modelMetadataRepositoryStoresAndRetrieves`: Verifies metadata lifecycle tracking.
- `userPreferencesRepositoryUpdatesAndEmits`: Verifies serialization and flow emissions for sensitivity, auto-activation, `DO_NOT_CONTROL`, `autoRestorePlayback`, and `restorationDelayMs`.

### `app` (`HearMeViewModelTest.kt`)
- `togglingSmartListeningUpdatesState`: Verifies ViewModel routes toggle commands to `ServiceController` without direct Android Context dependency.
- `earphoneConnectionUpdatesUiState`: Verifies earphone state updates trigger auto-activation and UI reflection.
- `preferencesUpdateChangesUiState`: Verifies changes to sensitivity and music interruption mode propagate to UI state.
- `mediaStateObservationUpdatesUiState`: Verifies live updates of track title, artist, and Notification Listener access.
- `simulateMediaInterruptionPausesThenRestores`: Verifies the developer interruption simulation action triggers pause and restoration.

---

## 3. Milestone 3 Verification Summary

- **Full Test Suite Execution (`.\gradlew.bat test`)**:
  - `BUILD SUCCESSFUL` across all 13 modules (259 actionable tasks, 100% test pass rate).
  - Hermetic JVM execution with zero emulator dependency for unit test runs.
- **Hardware Abstraction & DI Verification**:
  - `RealMediaPlaybackController` wired via Hilt using `MediaSessionManager` and `HearMeNotificationListenerService`.
  - `RealAudioFocusManager` wired via Hilt managing transient ducking and call detection.
  - Full restoration safeguards verified in automated unit test assertions.
- **Emulator Verification**:
  - Debug APK assembled and installed on `emulator-5554`.
  - App launched cleanly with zero runtime crashes.
  - UI dynamically renders Notification Access explanation and Now Playing status card.
  - `ServiceController` abstraction decouples ViewModels from `android.content.Context`.
- **Emulator Validation (`emulator-5554`)**:
  - Debug APK built and installed via `adb install -r`.
  - Launched without crash (`am start` confirmed active).
  - Top resumed activity: `com.allen.hearme/.MainActivity`.
  - Real-time VAD energy indicator and speech detection pill integrated cleanly into the Compose UI.
