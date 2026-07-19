# RouteGuard - Community Verification Architecture

**Version:** 1.0.1  
**Last Updated:** 2026-07-19

## 1. Architecture
The Community Verification feature strictly adheres to the Model-View-ViewModel (MVVM) design pattern. It maintains rigid layer separation:
*   **Data Layer (Repositories)**: Handles all Firebase Firestore reads, writes, and real-time listeners.
*   **Domain Layer (Managers)**: Handles validation, data aggregation, cooldown enforcement, and trust algorithms.
*   **Presentation Layer (ViewModels)**: Acts as the state coordinator, encapsulating business data into `LiveData` streams.
*   **UI Layer (Bottom Sheets / Fragments)**: Observes `LiveData` passively and delegates user interactions back to the ViewModel.

## 2. Read Flow
1. UI opens and calls `HomeViewModel.fetchConfirmationCount()` and `fetchUserConfirmation()` to request immediate one-time data, followed instantly by `startConfirmationListeners()`.
2. `HomeViewModel` delegates these requests to `ConfirmationManager`.
3. `ConfirmationManager` queries `ConfirmationRepository`.
4. The Repository executes Firestore queries/snapshot listeners, deserializes results into `ReportConfirmation` objects, and returns them via callbacks.
5. The Manager receives raw lists, calculates the `ConfirmationCount`, and passes it up.
6. The ViewModel posts the data to `confirmationCountLiveData` and `userConfirmationLiveData`.
7. The UI observes the LiveData and updates dynamically.

## 3. Write Flow
1. User clicks "Still Active" or "Not There" on the UI.
2. The UI directly invokes `ConfirmationManager.confirmReport()`, passing parameters. (Note: Bypasses ViewModel to optimize immediate validation feedback).
3. The Manager validates cooldowns and checks for duplicate states.
4. If valid, the Manager delegates a serialized `ReportConfirmation` object to `ConfirmationRepository.saveConfirmation()`.
5. The Repository performs a `set(..., SetOptions.merge())` on Firestore.
6. Upon success or failure, the callback travels back up to the UI to reset loading states and trigger toasts.

## 4. Repository Responsibilities
*   **ConfirmationRepository**: The single source of truth for Firestore. It only supports `get()`, `set()`, and `addSnapshotListener()`. It performs zero business logic. It serializes and deserializes the `ReportConfirmation` POJOs.

## 5. Manager Responsibilities
*   **ConfirmationManager**: Owns all business logic. Enforces spam rules, evaluates status matches, formats composite IDs, and aggregates lists of raw `ReportConfirmation` objects into a single `ConfirmationCount` model.

## 6. HomeViewModel Responsibilities
*   **HomeViewModel**: Orchestrates background threads via LiveData. It prevents the UI from communicating directly with the Repository for reads. Holds the state for `confirmationCountLiveData` and `userConfirmationLiveData`.

## 7. UI Responsibilities
*   **ReportDetailsBottomSheet**: Renders buttons and text. Handles click listeners. Manages "optimistic UI" by disabling buttons while network requests process. It strictly observes LiveData for dynamic updates.

## 8. Firestore Collections Used
*   `report_confirmations`: Stores confirmation records. Documents are uniquely identified by a composite string: `{reportId}_{userId}`.

## 9. Business Rules
*   Users must be authenticated to confirm a report.
*   A user can only submit `ACTIVE` or `GONE`.
*   A user cannot submit a confirmation that exactly matches their current confirmation status.
*   A user is bound by a strict time cooldown between submissions.

## 10. Cooldown Rule
*   A user must wait exactly **5 minutes** (300,000 milliseconds) between updating their confirmation status on a specific report.

## 11. Duplicate Prevention
*   **Locally**: The Manager compares the newly requested status against the `previousConfirmation.getStatus()` Enum.
*   **Database**: The Repository hardcodes the Firestore Document ID as `reportId_userId`. This physically prevents a user from occupying more than one document per report.

## 12. One-time Fetch Flow
*   Allows the UI to instantly pull data without negotiating a permanent websocket.
*   Implemented via `getConfirmations()` and `getCurrentUserConfirmation()` in the Repository.
*   Called immediately upon UI setup to quickly hydrate the screen.

## 13. Realtime Listener Flow
*   Implemented via `startConfirmationsListener()` and `startCurrentUserConfirmationListener()`.
*   Keeps the UI synchronized indefinitely as long as the Bottom Sheet remains open.
*   Idempotent architecture prevents duplicate socket connections.

## 14. Lifecycle Handling
*   Listeners are explicitly detached (`.remove()`) when the Bottom Sheet's `onDestroyView()` is triggered, preventing memory leaks and runaway bandwidth.
*   Async callbacks verify `getActivity() != null` to prevent crashes on configuration changes/rotations.

## 15. Error Handling
*   The system fails fast. If a validation rule is violated in the Manager, it immediately returns `false` via the callback with a human-readable string.
*   Firestore connection timeouts gracefully re-enable UI buttons and show a Toast, reverting optimistic UI locks.

## 16. Testing Summary
*   **Overall Stability**: 10/10 (Production Ready)
*   **Key Passed Tests**: First-time confirmations, Active to Gone swaps, Cooldown validation, Offline mode caching, Rotation survival, Multiple simultaneous user aggregations.

## 17. Known Technical Debt
*   **Write Flow Layer Bypass**: The `ReportDetailsBottomSheet` currently invokes `ConfirmationManager.confirmReport()` directly, bypassing the `HomeViewModel`. While functional, this slightly violates strict MVVM communication chains.
*   **Redundant Vote Wrapper**: The `VoteRepository` is a redundant wrapper around `VoteFirestoreRepository`.

## 18. Future Improvements
*   Migrate the Confirmation write flow into `HomeViewModel` to match the newly implemented Comments architecture.
*   Consolidate `VotingManager` and `ConfirmationManager` under a unified `VerificationManager`.
*   Implement exponential backoff for cooldowns (e.g., 5 min -> 15 min -> 1 hour for habitual spammers).

## 19. Changelog
*   **v1.0.1 (Current)**: Integrated Phase 1-5. Added synchronous one-time reads (`getConfirmations`, `getCurrentUserConfirmation`). Updated `HomeViewModel` to expose fetch methods. Connected one-time fetches to `setupConfirmations()` in `ReportDetailsBottomSheet` preceding realtime listeners.
*   **v1.0.0**: Initial implementation featuring Votes and realtime Confirmations.

## 20. Version
*   1.0.1
