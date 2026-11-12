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

### `core:bluetooth` (`CoreBluetoothTest.kt`)
- `monitorTracksEarphoneConnectionTransitions`: Verifies connection, disconnection, and device metadata propagation.
- `deviceStateProviderDetectsHeadsetPresence`: Verifies differentiation between internal speakers and wired/Bluetooth headphones.

### `core:media` (`CoreMediaTest.kt`)
- `playbackControllerRecordsActionsAndVolume`: Verifies volume ducking ratios, pause, resume, and action history.
- `audioFocusManagerRequestsAndAbandonsFocus`: Verifies audio focus request and abandon flows.

### `core:ml` (`CoreMlTest.kt`)
- `speechRecognizerReturnsTranscription`: Verifies speech transcription payload and confidence extraction.
- `speechClassifierIdentifiesDirectedSpeech`: Verifies directed vs ambient speech discrimination based on confidence thresholds.
- `confidenceEvaluatorComputesScoreAndThreshold`: Verifies multi-stage weighted confidence evaluation against user threshold.
- `trainingAndFeedbackPipelineRecordsSamples`: Verifies on-device training sample accumulation, feedback recording, and parameter offset updates.

### `core:database` (`CoreDatabaseTest.kt`)
- `listeningSessionRepositoryPerformsCrud`: Verifies session insert, query by ID, update, and clear-all operations.
- `trainingExampleRepositoryManagesExamples`: Verifies saving, retrieving, counting, and deleting training examples.
- `modelMetadataRepositoryStoresAndRetrieves`: Verifies metadata registration and retrieval.
- `userPreferencesRepositoryUpdatesAndEmits`: Verifies DataStore preference mutation and reactive flow emission.

### `feature:smartlistening` (`SmartListeningTest.kt`)
- `decisionEngineEvaluatesInterruptionAction`: Verifies heuristic decision making (ignoring ambient noise, ducking/pausing when directed speech occurs with earphones connected).
- `orchestratorTransitionsStateCorrectly`: Verifies coordinated state transitions between Audio, Bluetooth, Media, and UI states.

### `app` (`HearMeViewModelTest.kt`)
- `togglingSmartListeningUpdatesState`: Verifies ViewModel state changes when user toggles the master switch.
- `earphoneConnectionUpdatesUiState`: Verifies earphone state updates trigger auto-activation and UI reflection.
- `preferencesUpdateChangesUiState`: Verifies changes to sensitivity and music interruption mode propagate to UI state.

---

## 3. Command Line Verification

To run all unit tests:
```bash
./gradlew test
```

To assemble the debug APK:
```bash
./gradlew assembleDebug
```


---

## 3. Milestone 1.1 Architecture Audit Verification

- **Full Test Suite Execution (`.\gradlew.bat test`)**:
  - `BUILD SUCCESSFUL` across all 13 modules.
  - Zero test failures, 100% test pass rate across core contracts and feature modules.
- **Dependency Injection Verification**:
  - Successfully migrated from manual `AppContainer` to **Dagger Hilt** (`2.55`) using KSP (`2.2.10-2.0.2`).
  - Unit tests retain direct test double construction without requiring mockito or heavy android instrumentation.
- **Emulator Validation (`emulator-5554`)**:
  - APK generated via `:app:assembleDebug`.
  - Installed via `adb install -r`.
  - Launched without crash (`AndroidRuntime:E` clean).
  - Confirmed UI renders `STANDBY` (not ACTIVE) and `DISCONNECTED` (not CONNECTED), guaranteeing honest status behavior.
