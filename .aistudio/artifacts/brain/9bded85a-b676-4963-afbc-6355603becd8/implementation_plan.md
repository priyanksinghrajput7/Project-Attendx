# Real-Time Multi-Device Sync & Instant Camera QR Attendance

Enable cross-device real-time synchronization between Faculty and Student phones using Firebase Firestore, replace pseudo-canvas QR drawings with genuine ISO/IEC 18004 QR matrix encoding, and empower the camera scanner to execute instant, payment-app style auto-detection and attendance confirmation.

### User Review & Critical Decisions

> [!IMPORTANT]
> The user confirmed the following architectural decisions in Phase 1:
> - **Cross-Device Sync**: Use Firebase Firestore for instant live session sync across separate devices.
> - **Scanner Behavior**: Instant auto-scan like payment apps (PhonePe / Google Pay / Paytm) with haptic vibration and audio feedback upon QR detection.
> - **QR Data Payload**: Full session details and security verification token embedded in the QR payload for instant zero-lag verification.

---

### 1. Overview & Core Concept

Currently, AttendX only persists data to local SQLite (Room) on the single device where it is running. When a faculty member starts an attendance session on Phone 1, Phone 2 (student) has no knowledge of the active session. Furthermore, the faculty screen was drawing an artistic pseudo-random Canvas grid rather than a genuine ISO/IEC standard QR code, making ML Kit camera recognition fail.

This upgrade delivers:
1. **Cloud Sync via Firebase Firestore**: Active attendance sessions and verified attendance records stream in real time between faculty and student devices.
2. **Genuine QR Code Generation**: A compliant standard QR code generator rendering crisp, high-contrast black-and-white QR modules with error correction, finder patterns, and timing patterns that ML Kit decodes in 1 video frame.
3. **Instant "Paytm-Style" Scanning**: The student scanner locks onto the QR code immediately upon entering the camera frame, vibrates/beeps, confirms the subject session, and logs attendance without requiring pre-selection or manual steps.
4. **Faculty Live Attendee Feed**: As students scan the QR code in the classroom, their names, roll numbers, and timestamps appear instantly on the faculty device screen in real time.

---

### 2. User Experience & Visual Design

- **Faculty Flow**:
  1. Faculty taps **"Start Live QR Session"** for a subject (e.g., Computer Networks, CSE-4A).
  2. The screen displays a large, sharp, high-contrast genuine QR code containing verified session metadata (`ATTENDX:SESSION_ID:SUBJECT_ID:TOKEN:TIMESTAMP`).
  3. The QR session immediately broadcasts to Firestore.
  4. As students point their cameras at the screen, the **"Verified Attendees"** counter and list on the faculty screen increment in real time without refreshing.

- **Student Flow**:
  1. Student opens the app on their phone; the home screen and notification center immediately show the **Live QR Session** alert for the active subject.
  2. Student taps **"Scan with Camera"** (or opens Scanner).
  3. Pointing the camera at the faculty screen triggers an **instant scan** (sub-500ms).
  4. A bright emerald payment-style success ripple, celebratory haptic burst, and confirmation badge appear with the subject name, faculty name, and attendance timestamp.

---

### 3. Key Product Decisions & Trade-Offs

- **Real-Time Data Persistence (Firebase Firestore + Local Cache)**:
  - *Chosen Approach*: Firebase Firestore with real-time snapshot listeners for `attendance_sessions` and `attendance_records`.
  - *Why*: Firestore provides instant multi-device synchronization, handles offline resilience automatically, and aligns with Google AI Studio integration standards.
  - *Alternative Considered*: Local Wi-Fi or Bluetooth P2P sync was considered, but it requires devices to be on the exact same subnet or Bluetooth pairing, which frequently fails in university campus networks.

- **ISO/IEC Standard QR Matrix vs Third-Party Dependency**:
  - *Chosen Approach*: Pure Kotlin ISO/IEC 18004 standard QR encoder producing standard bitmatrices directly in-project.
  - *Why*: Eliminates build-time dependency resolution risks and network timeouts in the build container while guaranteeing 100% compatibility with Google ML Kit Barcode Scanning.

- **Self-Contained Verification Payload**:
  - *Chosen Approach*: The QR code encodes both human-readable tokens and a structured payload (`ATTENDX|v1|<sessionId>|<subjectId>|<subjectName>|<token>|<timestamp>`).
  - *Why*: Allows the student camera to instantly identify which subject session is being scanned even before the Firestore snapshot finishes syncing, achieving instant payment-app speed.

---

### 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────┐
│                     FACULTY DEVICE                     │
│  - Start Session -> Generates Standard ISO QR Matrix   │
│  - Writes Session to Firestore: attendance_sessions    │
│  - Realtime Listener: attendance_records (for session) │
└──────────────────────────┬─────────────────────────────┘
                           │
                           ▼
              ┌─────────────────────────┐
              │   Firebase Firestore    │
              │  - attendance_sessions  │
              │  - attendance_records   │
              │  - campus_notices       │
              └────────────┬────────────┘
                           │
                           ▼
┌────────────────────────────────────────────────────────┐
│                     STUDENT DEVICE                     │
│  - Realtime Listener receives active session alert     │
│  - CameraX + Google ML Kit scans ISO QR in 1 frame     │
│  - Instant validation & write to attendance_records    │
│  - Haptic feedback & payment-style success animation   │
└────────────────────────────────────────────────────────┘
```

#### Firestore Collections Schema
- `attendance_sessions/{sessionId}`:
  - `id`: String
  - `subjectId`: String
  - `subjectName`: String
  - `facultyId`: String
  - `facultyName`: String
  - `section`: String
  - `room`: String
  - `qrToken`: String
  - `isActive`: Boolean
  - `date`: String (YYYY-MM-DD)
  - `startTime`: String
  - `createdAt`: Timestamp

- `attendance_records/{recordId}`:
  - `id`: String
  - `sessionId`: String
  - `studentId`: String
  - `studentName`: String
  - `studentRollNo`: String
  - `subjectId`: String
  - `subjectName`: String
  - `facultyId`: String
  - `section`: String
  - `date`: String
  - `time`: String
  - `status`: String ("PRESENT")
  - `markedVia`: String ("QR_SCAN")
  - `createdAt`: Timestamp

---

### 5. Implementation Steps (Upon Approval)

1. **Firebase Setup Handshake**:
   - Extract debug keystore SHA-1 / SHA-256 via `keytool`.
   - Initiate Firebase setup via `show_aistudio_ui` / `rpc_action` for Firestore.
   - Deploy `firestore.rules` and configure database ID.
2. **Standard QR Code Generator (`QRCodeGenerator.kt`)**:
   - Implement compliant QR matrix encoder producing clean, high-resolution bitmaps with quiet zones and standard finder patterns.
   - Replace the pseudo-canvas drawing in `QRMatrixView` with the generated standard QR bitmap.
3. **Repository & Cloud Synchronization**:
   - Integrate Firestore service into `AttendXRepository` with real-time listeners for active sessions and session attendees.
   - Update `createAttendanceSession` to write to Firestore and broadcast the live session notice.
   - Update `verifyAndMarkQRAttendance` to query Firestore and write attendance records immediately.
4. **Instant Camera Scanning Optimization**:
   - Enhance `CameraPreview` and `AttendanceScannerScreen` to detect standard QR payloads on first video frame.
   - Add instant haptic feedback, chime, and emerald payment-style celebration banner.
5. **Compilation & Verification**:
   - Execute `compile_applet` and verify complete multi-device workflow.
