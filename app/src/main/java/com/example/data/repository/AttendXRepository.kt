package com.example.data.repository

import com.example.data.dao.AttendXDao
import com.example.data.model.AttendanceRecordEntity
import com.example.data.model.AttendanceSessionEntity
import com.example.data.model.AuditLogEntity
import com.example.data.model.DepartmentEntity
import com.example.data.model.LeaveRequestEntity
import com.example.data.model.NoticeEntity
import com.example.data.model.NotificationEntity
import com.example.data.model.SubjectEntity
import com.example.data.model.TimetableEntity
import com.example.data.model.UserEntity
import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class AttendanceSummary(
  val totalClasses: Int,
  val presentCount: Int,
  val absentCount: Int,
  val lateCount: Int,
  val percentage: Float
)

data class SubjectAttendanceStat(
  val subjectId: String,
  val subjectName: String,
  val facultyName: String,
  val room: String,
  val totalClasses: Int,
  val presentCount: Int,
  val absentCount: Int,
  val lateCount: Int,
  val percentage: Float,
  val isWarning: Boolean
)

data class CollegeAnalytics(
  val totalStudents: Int,
  val totalFaculty: Int,
  val totalDepartments: Int,
  val totalSubjects: Int,
  val collegeAverageAttendance: Float,
  val presentRate: Float,
  val absentRate: Float,
  val lateRate: Float,
  val departmentStats: Map<String, Float>
)

class AttendXRepository(
  private val dao: AttendXDao,
  private val context: Context? = null
) {

  private val firestore: FirebaseFirestore? by lazy {
    try {
      context?.let { ctx ->
        val dbId = ctx.getString(com.example.R.string.firestore_database_id)
        if (dbId.isNotBlank()) {
          FirebaseFirestore.getInstance(dbId)
        } else null
      }
    } catch (e: Exception) {
      Log.w("AttendXRepo", "Firestore init: ${e.message}")
      null
    }
  }

  private var sessionSyncRegistration: ListenerRegistration? = null
  private var attendeesSyncRegistration: ListenerRegistration? = null

  /**
   * Real-time multi-device sync: Listen for active attendance sessions started by faculty
   * on other devices and reflect them immediately in student's local cache & notification center.
   */
  fun startRealtimeSessionSync(scope: CoroutineScope) {
    val db = firestore ?: return
    sessionSyncRegistration?.remove()
    sessionSyncRegistration = db.collection("attendance_sessions")
      .whereEqualTo("isActive", true)
      .addSnapshotListener { snapshots, error ->
        if (error != null || snapshots == null) return@addSnapshotListener
        scope.launch(Dispatchers.IO) {
          try {
            for (doc in snapshots.documents) {
              val id = doc.getString("id") ?: doc.id
              val subjectId = doc.getString("subjectId") ?: ""
              val subjectName = doc.getString("subjectName") ?: ""
              val facultyId = doc.getString("facultyId") ?: ""
              val facultyName = doc.getString("facultyName") ?: ""
              val section = doc.getString("section") ?: "CSE-4A"
              val room = doc.getString("room") ?: "LT-101"
              val date = doc.getString("date") ?: ""
              val startTime = doc.getString("startTime") ?: ""
              val endTime = doc.getString("endTime") ?: ""
              val qrToken = doc.getString("qrToken") ?: ""
              val isActive = doc.getBoolean("isActive") ?: true

              if (isActive && qrToken.isNotBlank()) {
                val entity = AttendanceSessionEntity(
                  id = id,
                  subjectId = subjectId,
                  subjectName = subjectName,
                  facultyId = facultyId,
                  facultyName = facultyName,
                  section = section,
                  date = date,
                  startTime = startTime,
                  endTime = endTime,
                  room = room,
                  status = "ACTIVE",
                  qrToken = qrToken
                )
                dao.insertSession(entity)

                // Ensure student has live alert in Notification Bell
                dao.insertNotification(
                  NotificationEntity(
                    id = "NOTIF-SYNC-$id",
                    userId = "ALL",
                    title = "🔔 Live QR Session: $subjectName",
                    message = "$facultyName has opened live attendance session for $subjectName ($section) in $room. Scan with camera now!",
                    type = "QR_SESSION",
                    timestamp = System.currentTimeMillis(),
                    isRead = false
                  )
                )

                // Ensure student has live alert in Notice Board
                dao.insertNotice(
                  NoticeEntity(
                    id = "NOTICE-QR-$id",
                    title = "🔔 Live QR Session: $subjectName",
                    content = "$facultyName has opened live attendance session for $subjectName ($section) in $room. Open camera to scan your QR attendance now.",
                    category = "QR_SESSION",
                    postedBy = facultyName,
                    authorId = facultyId,
                    authorRole = "FACULTY",
                    postedAt = System.currentTimeMillis(),
                    priority = "URGENT",
                    isPinned = true,
                    targetAudience = section
                  )
                )
              }
            }
          } catch (e: Exception) {
            Log.e("AttendXRepo", "Error syncing session: ${e.message}")
          }
        }
      }
  }

  /**
   * Real-time multi-device sync: Listen for student attendance records logged for a specific session
   * so faculty sees attendees appear instantly on their phone.
   */
  fun startRealtimeAttendeesSync(sessionId: String, scope: CoroutineScope) {
    val db = firestore ?: return
    attendeesSyncRegistration?.remove()
    attendeesSyncRegistration = db.collection("attendance_records")
      .whereEqualTo("sessionId", sessionId)
      .addSnapshotListener { snapshots, error ->
        if (error != null || snapshots == null) return@addSnapshotListener
        scope.launch(Dispatchers.IO) {
          try {
            for (doc in snapshots.documents) {
              val recId = doc.getString("id") ?: doc.id
              val studentId = doc.getString("studentId") ?: ""
              val studentName = doc.getString("studentName") ?: ""
              val studentRoll = doc.getString("studentRollNo") ?: ""
              val subId = doc.getString("subjectId") ?: ""
              val subName = doc.getString("subjectName") ?: ""
              val facId = doc.getString("facultyId") ?: ""
              val sec = doc.getString("section") ?: ""
              val d = doc.getString("date") ?: ""
              val t = doc.getString("time") ?: ""
              val status = doc.getString("status") ?: "PRESENT"

              if (studentId.isNotBlank()) {
                val recordEntity = AttendanceRecordEntity(
                  id = recId,
                  studentId = studentId,
                  studentName = studentName,
                  studentRollNo = studentRoll,
                  subjectId = subId,
                  subjectName = subName,
                  facultyId = facId,
                  section = sec,
                  date = d,
                  time = t,
                  status = status,
                  sessionId = sessionId,
                  markedVia = "QR_CODE"
                )
                dao.insertAttendanceRecord(recordEntity)
              }
            }
          } catch (e: Exception) {
            Log.e("AttendXRepo", "Error syncing attendee: ${e.message}")
          }
        }
      }
  }

  val allUsers: Flow<List<UserEntity>> = dao.getAllUsers()
  val allDepartments: Flow<List<DepartmentEntity>> = dao.getAllDepartments()
  val allSubjects: Flow<List<SubjectEntity>> = dao.getAllSubjects()
  val allTimetables: Flow<List<TimetableEntity>> = dao.getAllTimetables()
  val activeSessions: Flow<List<AttendanceSessionEntity>> = dao.getActiveSessions()
  val allAuditLogs: Flow<List<AuditLogEntity>> = dao.getAllAuditLogs()

  fun getUserById(id: String): Flow<UserEntity?> = dao.getUserById(id)

  suspend fun ensureSeededData() {
    withContext(Dispatchers.IO) {
      try {
        if (dao.getUserCount() == 0 || dao.findUserByEmailOrId("admin@attendx.edu") == null) {
          com.example.data.database.AttendXDatabase.populateInitialData(dao)
        }
        // Clean any legacy demo attendance records so student attendance starts strictly from zero
        dao.deleteLegacyDemoAttendanceRecords()
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }
  }

  suspend fun authenticate(emailOrRoll: String, password: String): UserEntity? {
    val cleanInput = emailOrRoll.trim()
    val cleanPass = password.trim()
    if (cleanInput.isEmpty()) return null

    // 1. Direct query (case-insensitive in DAO)
    var user = dao.findUserByEmailOrId(cleanInput)

    // 2. Friendly role shorthand handling (e.g. entering "student", "faculty", "admin")
    if (user == null) {
      user = when (cleanInput.lowercase()) {
        "student", "alex", "alex sharma" -> {
          dao.findUserByEmailOrId("student@attendx.edu")
            ?: dao.findUserByEmailOrId("alex.sharma@attendx.edu")
            ?: dao.findUserByEmailOrId("psr@gmail.com")
            ?: dao.getFirstUserByRole("STUDENT")
        }
        "faculty", "prof chen", "teacher" -> {
          dao.findUserByEmailOrId("faculty@attendx.edu")
            ?: dao.findUserByEmailOrId("robert.chen@attendx.edu")
            ?: dao.getFirstUserByRole("FACULTY")
        }
        "admin", "administrator" -> {
          dao.findUserByEmailOrId("admin@attendx.edu")
            ?: dao.getFirstUserByRole("ADMIN")
        }
        else -> null
      }
    }

    if (user != null) {
      // Allow exact password, default demo passwords, or universal demo fallback
      if (user.password.equals(cleanPass, ignoreCase = false) ||
          cleanPass == "password123" ||
          cleanPass == "123456" ||
          cleanPass.isEmpty()
      ) {
        return user
      }
    }
    return null
  }

  suspend fun getDemoUserByRole(role: String): UserEntity? {
    return withContext(Dispatchers.IO) {
      when (role.uppercase()) {
        "STUDENT" -> {
          dao.findUserByEmailOrId("student@attendx.edu")
            ?: dao.findUserByEmailOrId("alex.sharma@attendx.edu")
            ?: dao.findUserByEmailOrId("psr@gmail.com")
            ?: dao.getFirstUserByRole("STUDENT")
        }
        "FACULTY" -> {
          dao.findUserByEmailOrId("faculty@attendx.edu")
            ?: dao.findUserByEmailOrId("robert.chen@attendx.edu")
            ?: dao.getFirstUserByRole("FACULTY")
        }
        "ADMIN" -> {
          dao.findUserByEmailOrId("admin@attendx.edu")
            ?: dao.getFirstUserByRole("ADMIN")
        }
        else -> null
      }
    }
  }

  fun getStudentsBySection(section: String): Flow<List<UserEntity>> =
    dao.getStudentsBySection(section)

  fun getStudents(): Flow<List<UserEntity>> =
    dao.getUsersByRole("STUDENT")

  fun getFaculty(): Flow<List<UserEntity>> =
    dao.getUsersByRole("FACULTY")

  fun getSubjectsByFaculty(facultyId: String): Flow<List<SubjectEntity>> =
    dao.getSubjectsByFaculty(facultyId)

  fun getTimetableForDay(day: String): Flow<List<TimetableEntity>> =
    dao.getTimetableForDay(day)

  fun getAttendanceForStudent(studentId: String): Flow<List<AttendanceRecordEntity>> =
    dao.getAttendanceForStudent(studentId)

  fun getAttendanceForSession(sessionId: String): Flow<List<AttendanceRecordEntity>> =
    dao.getAttendanceForSession(sessionId)

  fun getNotifications(userId: String): Flow<List<NotificationEntity>> =
    dao.getNotificationsForUser(userId)

  suspend fun markNotificationAsRead(id: String) {
    dao.markNotificationAsRead(id)
  }

  // --- ATTENDANCE MARKING (FACULTY) ---
  suspend fun saveBatchAttendance(
    sessionId: String,
    subjectId: String,
    subjectName: String,
    facultyId: String,
    section: String,
    date: String,
    time: String,
    records: List<Pair<UserEntity, String>> // (Student, Status: PRESENT/ABSENT/LATE)
  ) {
    val entities = records.map { (student, status) ->
      AttendanceRecordEntity(
        id = "REC-${UUID.randomUUID().toString().take(8)}",
        studentId = student.id,
        studentName = student.name,
        studentRollNo = student.rollNo,
        subjectId = subjectId,
        subjectName = subjectName,
        facultyId = facultyId,
        section = section,
        date = date,
        time = time,
        status = status,
        sessionId = sessionId,
        markedVia = "MANUAL",
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
      )
    }
    dao.insertAttendanceRecords(entities)

    dao.insertAuditLog(
      AuditLogEntity(
        id = "LOG-${UUID.randomUUID().toString().take(8)}",
        actorName = "Faculty",
        actorRole = "FACULTY",
        action = "Attendance Saved",
        details = "Recorded attendance for ${records.size} students in $subjectName ($section)"
      )
    )
  }

  // --- SMART QR ATTENDANCE VERIFICATION (STUDENT) ---
  suspend fun verifyAndMarkQRAttendance(
    student: UserEntity,
    qrToken: String
  ): Result<String> {
    val cleanToken = qrToken.trim()
    if (cleanToken.isBlank()) {
      return Result.failure(Exception("QR token cannot be empty."))
    }

    // 1. Check for standard AttendX structured payload: ATTENDX|v1|sessionId|subjectId|subjectName|qrToken|facultyId|section|room|timestamp
    if (cleanToken.startsWith("ATTENDX|v1|")) {
      val parts = cleanToken.split("|")
      if (parts.size >= 7) {
        val payloadSessionId = parts[2]
        val payloadSubjectId = parts[3]
        val payloadSubjectName = parts[4]
        val payloadQrToken = parts[5]
        val payloadFacultyId = parts[6]
        val payloadSection = if (parts.size > 7) parts[7] else "CSE-4A"
        val payloadRoom = if (parts.size > 8) parts[8] else "LT-101"

        // Ensure session exists in local Room DB for instant offline-first scan verification
        val existingSession = dao.getActiveSessions().firstOrNull()?.firstOrNull { it.id == payloadSessionId }
        if (existingSession == null) {
          val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
          val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
          dao.insertSession(
            AttendanceSessionEntity(
              id = payloadSessionId,
              subjectId = payloadSubjectId,
              subjectName = payloadSubjectName,
              facultyId = payloadFacultyId,
              facultyName = "Faculty",
              section = payloadSection,
              date = todayStr,
              startTime = timeStr,
              endTime = "",
              room = payloadRoom,
              status = "ACTIVE",
              qrToken = payloadQrToken
            )
          )
        }
      }
    }

    val activeList = dao.getActiveSessions().firstOrNull() ?: emptyList()
    if (activeList.isEmpty()) {
      return Result.failure(Exception("No active attendance session is open right now. Faculty must start the QR session according to timetable before students can scan."))
    }

    // Match only among currently active sessions started by faculty
    val matchingSession = activeList.firstOrNull { session ->
      session.qrToken.trim().equals(cleanToken, ignoreCase = true) ||
      session.id.trim().equals(cleanToken, ignoreCase = true) ||
      cleanToken.contains(session.qrToken.trim(), ignoreCase = true) ||
      cleanToken.contains(session.id.trim(), ignoreCase = true) ||
      (cleanToken.contains(session.subjectName.trim(), ignoreCase = true) && cleanToken.contains("LIVE", ignoreCase = true))
    }

    if (matchingSession == null) {
      return Result.failure(Exception("QR code does not match any active session. Attendance can only be scanned when faculty opens the session for this subject."))
    }

    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    // Check duplicate in this session
    val existingInSession = dao.checkStudentInSession(student.id, matchingSession.id)
    if (existingInSession != null) {
      return Result.failure(Exception("Attendance already recorded as ${existingInSession.status} for ${matchingSession.subjectName}."))
    }

    // Also check if already marked for this subject today
    val existingToday = dao.checkStudentInSubjectOnDate(student.id, matchingSession.subjectId, todayStr)
    if (existingToday != null) {
      return Result.failure(Exception("Attendance already recorded today for ${matchingSession.subjectName} (${existingToday.status})."))
    }

    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val record = AttendanceRecordEntity(
      id = "REC-${UUID.randomUUID().toString().take(8)}",
      studentId = student.id,
      studentName = student.name,
      studentRollNo = student.rollNo,
      subjectId = matchingSession.subjectId,
      subjectName = matchingSession.subjectName,
      facultyId = matchingSession.facultyId,
      section = matchingSession.section,
      date = todayStr,
      time = timeFormat.format(Date()),
      status = "PRESENT",
      sessionId = matchingSession.id,
      markedVia = "QR_CODE"
    )
    dao.insertAttendanceRecord(record)

    // Sync attendance record to Cloud Firestore so faculty phone receives attendee immediately
    try {
      firestore?.collection("attendance_records")?.document("${matchingSession.id}_${student.id}")?.set(
        mapOf(
          "id" to record.id,
          "sessionId" to matchingSession.id,
          "studentId" to student.id,
          "studentName" to student.name,
          "studentRollNo" to student.rollNo,
          "subjectId" to matchingSession.subjectId,
          "subjectName" to matchingSession.subjectName,
          "facultyId" to matchingSession.facultyId,
          "section" to matchingSession.section,
          "date" to todayStr,
          "time" to record.time,
          "status" to "PRESENT",
          "markedVia" to "QR_SCAN"
        )
      )?.addOnFailureListener { e ->
        Log.w("AttendXRepo", "Firestore record attendance failed: ${e.message}")
      }
    } catch (e: Exception) {
      Log.w("AttendXRepo", "Firestore write error: ${e.message}")
    }

    // Send notification
    dao.insertNotification(
      NotificationEntity(
        id = "NOTIF-${UUID.randomUUID().toString().take(8)}",
        userId = student.id,
        title = "Attendance Marked ✅ - ${matchingSession.subjectName}",
        message = "Your attendance for ${matchingSession.subjectName} (${matchingSession.section}) was confirmed via QR code.",
        type = "ATTENDANCE"
      )
    )

    // Audit log
    dao.insertAuditLog(
      AuditLogEntity(
        id = "LOG-${UUID.randomUUID().toString().take(8)}",
        actorName = student.name,
        actorRole = "STUDENT",
        action = "QR Attendance Verified",
        details = "Student ${student.rollNo} scanned QR token for ${matchingSession.subjectName}"
      )
    )

    return Result.success("Attendance marked Present for ${matchingSession.subjectName} ✅")
  }

  // --- SESSION CONTROLS ---
  suspend fun createAttendanceSession(
    subject: SubjectEntity,
    faculty: UserEntity,
    section: String,
    startTime: String = "10:00 AM",
    endTime: String = "11:00 AM",
    room: String = subject.room
  ): AttendanceSessionEntity {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val today = dateFormat.format(Date())
    val sessionCode = "SES-${(1000..9999).random()}"
    val qrToken = "ATTENDX-${subject.code}-${(10000..99999).random()}"

    val session = AttendanceSessionEntity(
      id = sessionCode,
      subjectId = subject.id,
      subjectName = subject.name,
      facultyId = faculty.id,
      facultyName = faculty.name,
      section = section,
      date = today,
      startTime = startTime,
      endTime = endTime,
      room = room,
      status = "ACTIVE",
      qrToken = qrToken
    )
    dao.insertSession(session)

    // Sync session to Cloud Firestore for multi-device live broadcast
    try {
      firestore?.collection("attendance_sessions")?.document(sessionCode)?.set(
        mapOf(
          "id" to sessionCode,
          "subjectId" to subject.id,
          "subjectName" to subject.name,
          "facultyId" to faculty.id,
          "facultyName" to faculty.name,
          "section" to section,
          "date" to today,
          "startTime" to startTime,
          "endTime" to endTime,
          "room" to room,
          "status" to "ACTIVE",
          "qrToken" to qrToken,
          "isActive" to true
        )
      )?.addOnFailureListener { e ->
        Log.w("AttendXRepo", "Firestore create session failed: ${e.message}")
      }

      firestore?.collection("campus_notices")?.document("NOTICE-QR-$sessionCode")?.set(
        mapOf(
          "id" to "NOTICE-QR-$sessionCode",
          "title" to "🔔 Live QR Session: ${subject.name}",
          "content" to "${faculty.name} has opened live attendance session for ${subject.name} ($section) in $room. Open camera to scan your QR attendance now.",
          "category" to "QR_SESSION",
          "priority" to "URGENT",
          "authorName" to faculty.name,
          "date" to today,
          "time" to startTime,
          "isPinned" to true
        )
      )
    } catch (e: Exception) {
      Log.w("AttendXRepo", "Firestore broadcast error: ${e.message}")
    }

    // Alert students that a live QR session is open for their subject
    dao.insertNotification(
      NotificationEntity(
        id = "NOTIF-${UUID.randomUUID().toString().take(8)}",
        userId = "ALL",
        title = "🔔 Live QR Session: ${subject.name}",
        message = "${faculty.name} has opened the live attendance session for ${subject.name} ($section) in $room. Scan with camera now!",
        type = "QR_SESSION",
        timestamp = System.currentTimeMillis(),
        isRead = false
      )
    )

    // Publish high-priority alert to Campus Notice Center
    dao.insertNotice(
      NoticeEntity(
        id = "NOTICE-QR-${sessionCode}",
        title = "🔔 Live QR Session: ${subject.name}",
        content = "${faculty.name} has opened the live attendance session for ${subject.name} ($section) in $room. Open camera to scan your QR attendance now.",
        category = "QR_SESSION",
        postedBy = faculty.name,
        authorId = faculty.id,
        authorRole = "FACULTY",
        postedAt = System.currentTimeMillis(),
        priority = "URGENT",
        isPinned = true,
        targetAudience = section
      )
    )

    dao.insertAuditLog(
      AuditLogEntity(
        id = "LOG-${UUID.randomUUID().toString().take(8)}",
        actorName = faculty.name,
        actorRole = "FACULTY",
        action = "Attendance Session Started",
        details = "Started session $sessionCode for ${subject.name} ($section)"
      )
    )

    return session
  }

  suspend fun closeSession(session: AttendanceSessionEntity) {
    dao.updateSession(session.copy(status = "CLOSED"))

    try {
      firestore?.collection("attendance_sessions")?.document(session.id)?.update(
        mapOf(
          "isActive" to false,
          "status" to "CLOSED"
        )
      )
    } catch (e: Exception) {
      Log.w("AttendXRepo", "Firestore close session error: ${e.message}")
    }

    dao.insertAuditLog(
      AuditLogEntity(
        id = "LOG-${UUID.randomUUID().toString().take(8)}",
        actorName = session.facultyName,
        actorRole = "FACULTY",
        action = "Attendance Session Closed",
        details = "Closed session ${session.id} for ${session.subjectName}"
      )
    )
  }

  // --- CRUD (ADMIN & GENERAL) ---
  suspend fun insertAuditLog(log: AuditLogEntity) = dao.insertAuditLog(log)
  suspend fun insertUser(user: UserEntity) = dao.insertUser(user)
  suspend fun updateUser(user: UserEntity) = dao.updateUser(user)
  suspend fun deleteUser(user: UserEntity) = dao.deleteUser(user)

  suspend fun insertDepartment(dept: DepartmentEntity) = dao.insertDepartment(dept)
  suspend fun deleteDepartment(dept: DepartmentEntity) = dao.deleteDepartment(dept)

  suspend fun insertSubject(sub: SubjectEntity) = dao.insertSubject(sub)
  suspend fun deleteSubject(sub: SubjectEntity) = dao.deleteSubject(sub)

  suspend fun insertTimetable(tt: TimetableEntity) = dao.insertTimetable(tt)
  suspend fun deleteTimetable(tt: TimetableEntity) = dao.deleteTimetable(tt)

  // --- COMPUTATIONS & CALCULATOR ---
  fun computeSummary(records: List<AttendanceRecordEntity>): AttendanceSummary {
    val total = records.size
    val present = records.count { it.status.equals("PRESENT", ignoreCase = true) }
    val absent = records.count { it.status.equals("ABSENT", ignoreCase = true) }
    val late = records.count { it.status.equals("LATE", ignoreCase = true) }
    val pct = if (total > 0) ((present + (late * 0.5f)) / total) * 100f else 0f
    return AttendanceSummary(total, present, absent, late, pct)
  }

  fun computeSubjectStats(
    subjects: List<SubjectEntity>,
    records: List<AttendanceRecordEntity>
  ): List<SubjectAttendanceStat> {
    return subjects.map { subject ->
      val subRecords = records.filter { it.subjectId == subject.id || it.subjectName.equals(subject.name, ignoreCase = true) }
      val total = subRecords.size
      val present = subRecords.count { it.status.equals("PRESENT", ignoreCase = true) }
      val absent = subRecords.count { it.status.equals("ABSENT", ignoreCase = true) }
      val late = subRecords.count { it.status.equals("LATE", ignoreCase = true) }
      val pct = if (total > 0) ((present + (late * 0.5f)) / total) * 100f else 0f
      val isWarning = total > 0 && pct < subject.targetAttendancePercent

      SubjectAttendanceStat(
        subjectId = subject.id,
        subjectName = subject.name,
        facultyName = subject.facultyName,
        room = subject.room,
        totalClasses = total,
        presentCount = present,
        absentCount = absent,
        lateCount = late,
        percentage = pct,
        isWarning = isWarning
      )
    }
  }

  /**
   * Calculator:
   * Target required = 75%
   * Current present = P, total = T
   * If P/T < target:
   * (P + x) / (T + x) >= target
   * P + x >= target*T + target*x
   * x*(1 - target) >= target*T - P
   * x >= (target*T - P) / (1 - target)
   */
  fun calculateClassesRequired(
    currentPresent: Int,
    currentTotal: Int,
    targetPercent: Float = 75f
  ): Int {
    if (currentTotal == 0) return 0
    val targetFraction = targetPercent / 100f
    val currentPct = (currentPresent.toFloat() / currentTotal.toFloat()) * 100f
    if (currentPct >= targetPercent) return 0
    val numerator = (targetFraction * currentTotal) - currentPresent
    val denominator = 1f - targetFraction
    val required = Math.ceil((numerator / denominator).toDouble()).toInt()
    return if (required > 0) required else 0
  }

  fun calculateMissImpact(currentPresent: Int, currentTotal: Int): Float {
    val nextTotal = currentTotal + 1
    return (currentPresent.toFloat() / nextTotal.toFloat()) * 100f
  }

  // --- LEAVE MANAGEMENT ---
  fun getAllLeaveRequests(): Flow<List<LeaveRequestEntity>> = dao.getAllLeaveRequests()

  fun getLeaveRequestsForStudent(studentId: String, studentRollNo: String = ""): Flow<List<LeaveRequestEntity>> =
    dao.getLeaveRequestsForStudent(studentId, studentRollNo)

  fun getLeaveRequestsForFaculty(facultyId: String, facultyName: String = "", facultyRole: String = ""): Flow<List<LeaveRequestEntity>> =
    dao.getLeaveRequestsForFaculty(facultyId, facultyName, facultyRole)

  fun getPendingLeaveRequests(): Flow<List<LeaveRequestEntity>> =
    dao.getPendingLeaveRequests()

  suspend fun applyForLeave(
    student: UserEntity,
    facultyId: String,
    facultyName: String,
    leaveType: String,
    startDate: String,
    endDate: String,
    numberOfDays: Int,
    reason: String,
    attachmentUri: String? = null
  ): LeaveRequestEntity {
    val leaveId = "LV-${(1000..9999).random()}"
    val leave = LeaveRequestEntity(
      id = leaveId,
      studentId = student.id,
      studentName = student.name,
      studentRollNo = student.rollNo,
      studentRegNo = student.regNo,
      studentSection = student.section,
      facultyId = facultyId,
      facultyName = facultyName,
      leaveType = leaveType,
      startDate = startDate,
      endDate = endDate,
      numberOfDays = if (numberOfDays > 0) numberOfDays else 1,
      reason = reason.trim(),
      status = "PENDING",
      attachmentUri = attachmentUri
    )
    dao.insertLeaveRequest(leave)

    // Notify targeted faculty
    if (facultyId.isNotBlank()) {
      dao.insertNotification(
        NotificationEntity(
          id = "NOTIF-${UUID.randomUUID().toString().take(8)}",
          userId = facultyId,
          title = "New Leave Application 📋",
          message = "${student.name} (${student.rollNo}) applied for $leaveType from $startDate to $endDate: \"$reason\"${if (attachmentUri != null) " [Attached Proof 📎]" else ""}",
          type = "ANNOUNCEMENT"
        )
      )
    }

    // Also notify student of successful submission
    dao.insertNotification(
      NotificationEntity(
        id = "NOTIF-${UUID.randomUUID().toString().take(8)}",
        userId = student.id,
        title = "Leave Application Submitted ⏳",
        message = "Your $leaveType application ($startDate to $endDate) was sent to $facultyName for review.${if (attachmentUri != null) " (Document attached)" else ""}",
        type = "ANNOUNCEMENT"
      )
    )

    // Audit log
    dao.insertAuditLog(
      AuditLogEntity(
        id = "LOG-${UUID.randomUUID().toString().take(8)}",
        actorName = student.name,
        actorRole = "STUDENT",
        action = "Leave Applied",
        details = "Submitted $leaveType ($numberOfDays day/s) from $startDate to $endDate to $facultyName${if (attachmentUri != null) " with medical/duty certificate" else ""}"
      )
    )

    return leave
  }

  suspend fun broadcastDefaulterAlert(
    faculty: UserEntity,
    defaulters: List<UserEntity>,
    customMessage: String
  ) {
    val timestamp = System.currentTimeMillis()
    defaulters.forEach { student ->
      dao.insertNotification(
        NotificationEntity(
          id = "NOTIF-${UUID.randomUUID().toString().take(8)}",
          userId = student.id,
          title = "⚠️ Low Attendance Debarment Notice",
          message = customMessage.ifBlank {
            "Your attendance is below the mandatory 75% college threshold. Please meet Prof. ${faculty.name} immediately to avoid debarment from upcoming semester exams."
          },
          type = "ANNOUNCEMENT",
          timestamp = timestamp
        )
      )
    }
    dao.insertAuditLog(
      AuditLogEntity(
        id = "LOG-${UUID.randomUUID().toString().take(8)}",
        actorName = faculty.name,
        actorRole = "FACULTY",
        action = "Defaulter Broadcast Notice",
        details = "Dispatched low attendance warning notices to ${defaulters.size} students"
      )
    )
  }

  suspend fun reviewLeaveRequest(
    leaveId: String,
    status: String, // "APPROVED" or "REJECTED"
    remarks: String,
    reviewer: UserEntity
  ) {
    dao.updateLeaveStatus(
      id = leaveId,
      status = status,
      remarks = remarks.trim(),
      reviewedAt = System.currentTimeMillis()
    )

    // Retrieve leave to notify student
    val allLeaves = dao.getAllLeaveRequests().firstOrNull() ?: emptyList()
    val leave = allLeaves.firstOrNull { it.id == leaveId }
    if (leave != null) {
      val isApproved = status.equals("APPROVED", ignoreCase = true)
      val emoji = if (isApproved) "✅" else "❌"
      val statusText = if (isApproved) "Approved" else "Rejected"
      dao.insertNotification(
        NotificationEntity(
          id = "NOTIF-${UUID.randomUUID().toString().take(8)}",
          userId = leave.studentId,
          title = "Leave Request $statusText $emoji",
          message = "Your leave request for ${leave.startDate} to ${leave.endDate} was $statusText by ${reviewer.name}. Remarks: \"${remarks.ifBlank { "No remarks provided" }}\"",
          type = "ANNOUNCEMENT"
        )
      )
    }

    dao.insertAuditLog(
      AuditLogEntity(
        id = "LOG-${UUID.randomUUID().toString().take(8)}",
        actorName = reviewer.name,
        actorRole = "FACULTY",
        action = "Leave Reviewed: $status",
        details = "Faculty ${reviewer.name} marked leave $leaveId as $status with remarks: $remarks"
      )
    )
  }

  suspend fun cancelLeave(leave: LeaveRequestEntity, student: UserEntity) {
    val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date())
    dao.markLeaveAsCancelled(
      id = leave.id,
      remarks = "Cancelled and withdrawn by student ${student.name} on $dateStr",
      reviewedAt = System.currentTimeMillis()
    )

    // Remove any related pending notifications so faculty/students don't see them repeatedly
    dao.deleteNotificationsContaining(leave.id)
    dao.deleteNotificationsContaining(leave.studentRollNo)

    // Log the cancellation in audit trail
    dao.insertAuditLog(
      AuditLogEntity(
        id = "LOG-${UUID.randomUUID().toString().take(8)}",
        actorName = student.name,
        actorRole = "STUDENT",
        action = "Leave Application Cancelled",
        details = "Student ${student.name} (${student.rollNo}) cancelled & withdrew leave application ${leave.id} (${leave.startDate} to ${leave.endDate})"
      )
    )

    // Send single withdrawal notice to faculty
    if (leave.facultyId.isNotBlank()) {
      dao.insertNotification(
        NotificationEntity(
          id = "NOTIF-${UUID.randomUUID().toString().take(8)}",
          userId = leave.facultyId,
          title = "Leave Request Withdrawn 🚫",
          message = "${student.name} (${student.rollNo}) has withdrawn their leave request for ${leave.startDate} to ${leave.endDate}.",
          type = "ANNOUNCEMENT"
        )
      )
    }
  }

  suspend fun deleteLeave(leave: LeaveRequestEntity) {
    dao.deleteLeaveById(leave.id)
    dao.deleteLeaveRequest(leave)
    dao.deleteNotificationsContaining(leave.id)
  }

  suspend fun deleteLeaveById(id: String) {
    dao.deleteLeaveById(id)
    dao.deleteNotificationsContaining(id)
  }

  // --- NOTICE BOARD & CAMPUS CIRCULARS ---
  val allNotices: Flow<List<NoticeEntity>> = dao.getAllNotices()

  fun getNoticesForUser(audience: String, role: String): Flow<List<NoticeEntity>> =
    dao.getNoticesForTarget(audience, role)

  suspend fun postNotice(
    notice: NoticeEntity,
    sendNotification: Boolean = true
  ) {
    dao.insertNotice(notice)

    // Automatically broadcast notification so students receive immediate alert
    if (sendNotification) {
      val priorityEmoji = when (notice.priority.uppercase()) {
        "URGENT" -> "🚨"
        "HIGH" -> "⚠️"
        else -> "📢"
      }
      val categoryTag = if (notice.category.isNotBlank()) "[${notice.category.uppercase()}] " else ""
      val titlePrefix = "$priorityEmoji $categoryTag"

      dao.insertNotification(
        NotificationEntity(
          id = "NOTIF-${UUID.randomUUID().toString().take(8)}",
          userId = if (notice.targetAudience.equals("ALL", ignoreCase = true)) "ALL" else notice.targetAudience,
          title = "$titlePrefix${notice.title}",
          message = "${notice.postedBy} (${notice.authorRole}): ${notice.content}",
          type = "ANNOUNCEMENT",
          timestamp = notice.postedAt,
          isRead = false
        )
      )
    }

    dao.insertAuditLog(
      AuditLogEntity(
        id = "LOG-${UUID.randomUUID().toString().take(8)}",
        actorName = notice.postedBy,
        actorRole = notice.authorRole,
        action = "Notice Published",
        details = "Published notice '${notice.title}' targeted to ${notice.targetAudience} (${notice.category})"
      )
    )
  }

  suspend fun deleteNotice(notice: NoticeEntity) {
    dao.deleteNotice(notice)
    dao.deleteNoticeById(notice.id)
  }

  suspend fun toggleNoticePin(noticeId: String, currentPinned: Boolean) {
    dao.updateNoticePin(noticeId, !currentPinned)
  }
}
