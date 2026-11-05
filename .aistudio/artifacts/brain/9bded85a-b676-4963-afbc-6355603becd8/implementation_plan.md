# Timetable-Based QR Session Notification & Strict Camera-Only Attendance Scanning

Introduce automatic in-app notifications dispatched to the student Notice Center whenever a faculty member opens a live QR session for a timetable subject, and completely eliminate manual token text inputs and quick-fill buttons so attendance is strictly recorded only via real-time camera scanning.

### User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural decisions were confirmed via interactive clarification:

- **Confirmed Decision 1: Notice Center In-App Notifications**: When a faculty member initiates a live session, a high-priority notification is dispatched to the student Notice Center (`userId = "ALL"`, `type = "QR_SESSION"`), detailing the subject, section, faculty instructor, and live status.
- **Confirmed Decision 2: Direct Action from Notification**: Each live session notification in the Notice Center features a direct `"Scan with Camera"` action button that routes the student directly to `ScreenDestination.StudentQRScanner` for that specific subject.
- **Confirmed Decision 3: Strict Camera-Only Scanning**: The manual token entry card, text input field, and quick-fill pills in `AttendanceScannerScreen.kt` will be completely removed. Attendance can only be marked when the physical camera frame captures and decodes the QR code via ML Kit Barcode Scanning.

---

### 1. Overview & Core Concept

- **What It Does**:
  1. **Timetable Session Alert**: When a professor opens a live attendance session, all enrolled students receive an instant notification in their Notice Center informing them that the attendance QR is live for their class.
  2. **Camera-Only Enforcement**: Removes all simulated/manual input fields from the scanner screen. Students must physically open their device camera and align the lens with the faculty's displayed QR code.
- **Target Audience**: Students (notified when their scheduled class session begins and scanning via camera) and Faculty (ensuring students are physically present and scanning the projection).
- **Key Value**: Guarantees physical presence by preventing students from bypassing the camera via token copying or quick-tap buttons, while ensuring no student misses the scanning window.

---

### 2. User Experience & Visual Design

#### A. Notice Center Live Session Card (`NoticeBoardScreen.kt`)
- **Card Design**:
  - Border: 1.5.dp Emerald accent border with a subtle gradient background (`Emerald50` / `Indigo50`).
  - Badge: Pulsing Emerald dot with `"LIVE QR OPEN"`.
  - Content: Course name, section, faculty instructor, and time:
    - *"Prof. Robert Chen has opened the live attendance session for Computer Networks (CSE-4A). Scan your attendance now."*
  - Action Button: Prominent Emerald button `"Scan with Camera"` (`Icons.Default.QrCodeScanner`) with 48.dp touch target.
  - Tapping this button directly launches the camera scanner pre-focused on that subject.

#### B. Camera-Only Scanner Experience (`AttendanceScannerScreen.kt`)
- **Simplified & Secure UI**:
  - Full-screen camera viewfinder with real-time ML Kit analysis.
  - Centered scanning reticle with animated laser scanline.
  - Live Subject Selector chips (viewing live session details).
  - **Removed**: Manual text field (`scanner_manual_input`), token clear buttons, and `"Verify Attendance for Subject"` manual button.
- **Scan Flow**:
  1. Student points camera at faculty QR code.
  2. ML Kit camera analyzer captures raw barcode data.
  3. Barcode string is cryptographically checked against active sessions.
  4. Haptic vibration confirms scan, attendance is marked `PRESENT`, and faculty live roll updates instantly.

---

### 3. Key Product Decisions & Trade-Offs

| Decision | Chosen Approach | Rationale | Alternatives Considered |
| :--- | :--- | :--- | :--- |
| **Alert Placement** | Dedicated notice center alert card with `"Scan with Camera"` CTA button. | Keeps dashboard uncluttered while ensuring notifications are stored and actionable. | Intrusive system modal popup (distracting). |
| **Manual Input Policy** | Complete removal of text entry and quick-fill pills. | Fulfills user requirement: *"jbb qr camera meh scan ho tbhi attendance bane naki bina qr camera meh scan huea"*. | Retaining manual input with password (too easy to share/bypass). |
| **Session Notification Trigger** | Triggered immediately in `startNewSession()` inside `AttendXViewModel` / `AttendXRepository`. | Guarantees notification is created atomically when the QR code is generated. | Polling cron job (delayed). |

---

### 4. Technical Architecture & Data Strategy

#### End-to-End Notification & Scan Flow

```
┌────────────────────────────────────────────────────────┐
│                   FACULTY ACTION                       │
│                                                        │
│  Faculty initiates live session for Timetable Subject   │
│                          │                             │
│                          ▼                             │
│  AttendXRepository.startNewSession()                   │
│    ├── 1. Inserts AttendanceSessionEntity (ACTIVE)     │
│    └── 2. Inserts NotificationEntity:                  │
│           • title: "Live QR Session: Computer Networks" │
│           • message: "Prof. Chen opened attendance..."  │
│           • type: "QR_SESSION"                         │
└──────────────────────────┬─────────────────────────────┘
                           │
                           ▼
┌────────────────────────────────────────────────────────┐
│              STUDENT NOTICE CENTER                     │
│                                                        │
│  Displays Live Alert Card with Emerald Beacon          │
│  Button: [ Scan with Camera ]                          │
│                          │                             │
│                          ▼                             │
│       Routes to AttendanceScannerScreen                │
└──────────────────────────┬─────────────────────────────┘
                           │
                           ▼
┌────────────────────────────────────────────────────────┐
│             CAMERA-ONLY SCANNER SCREEN                 │
│                                                        │
│  • CameraPreview (CameraX + ML Kit Barcode Analyzer)   │
│  • ZERO manual text boxes or quick-fill buttons        │
│  • onQrCodeScanned(token) -> verifyAndMarkQRAttendance │
│                          │                             │
│                          ▼ Valid                       │
│  Room DB: AttendanceRecordEntity (markedVia = "CAMERA")│
│  Haptic Feedback + Success Dialog                      │
└────────────────────────────────────────────────────────┘
```

#### Key Implementation Steps:
1. **`AttendXRepository.kt` & `AttendXViewModel.kt`**:
   - In `startNewSession(subject, section)`: Create and insert `NotificationEntity` with `type = "QR_SESSION"`, `subjectId`, and details.
2. **`NoticeBoardScreen.kt`**:
   - Add dedicated rendering for `type == "QR_SESSION"` notifications featuring `"Scan with Camera"` CTA button that invokes `onScanSession(subjectId)`.
   - Wire `onScanSession` to navigate to `ScreenDestination.StudentQRScanner`.
3. **`AttendanceScannerScreen.kt`**:
   - Strip out manual input text box, clear button, and `"Verify Attendance for Subject"` button.
   - Clean up UI to focus 100% on the live camera viewfinder and active session guidance.
4. **Verification**:
   - Run `compile_applet` to confirm clean compilation and test end-to-end functionality.
