package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AttendXDatabase
import com.example.data.model.AttendanceRecordEntity
import com.example.data.model.AttendanceSessionEntity
import com.example.data.model.AuditLogEntity
import com.example.data.model.DepartmentEntity
import com.example.data.model.LeaveRequestEntity
import com.example.data.model.LeaveRequestModel
import com.example.data.model.NoticeEntity
import com.example.data.model.NotificationEntity
import com.example.data.model.SubjectEntity
import com.example.data.model.TimetableEntity
import com.example.data.model.UserEntity
import com.example.data.model.toModel
import com.example.data.repository.AttendXRepository
import com.example.data.repository.AttendanceSummary
import com.example.data.repository.SubjectAttendanceStat
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AttendanceRequirementPolicy(
  val minAttendancePercent: Float = 75.0f,
  val warningThresholdPercent: Float = 70.0f,
  val condonationLimitPercent: Float = 65.0f,
  val lateGracePeriodMinutes: Int = 10,
  val maxCasualLeaves: Int = 6,
  val academicTerm: String = "Academic Year 2026–2027 (Odd Semester)",
  val allowMedicalDutyCondonation: Boolean = true
)

sealed class ScreenDestination {
  object Landing : ScreenDestination()
  object Auth : ScreenDestination()
  object StudentMain : ScreenDestination()
  object FacultyMain : ScreenDestination()
  object AdminMain : ScreenDestination()
  object FacultyMarkAttendance : ScreenDestination()
  object FacultyQRSession : ScreenDestination()
  object StudentQRScanner : ScreenDestination()
  object StudentLeaves : ScreenDestination()
  object FacultyLeaves : ScreenDestination()
  object LeaveRequest : ScreenDestination()
  object TodaysSchedule : ScreenDestination()
  object NoticeBoard : ScreenDestination()
}

class AttendXViewModel(application: Application) : AndroidViewModel(application) {

  private val database = AttendXDatabase.getDatabase(application, viewModelScope)
  val repository = AttendXRepository(database.attendXDao())

  // --- CURRENT USER & AUTH ---
  private val _currentUser = MutableStateFlow<UserEntity?>(null)
  val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

  private val _currentScreen = MutableStateFlow<ScreenDestination>(ScreenDestination.Landing)
  val currentScreen: StateFlow<ScreenDestination> = _currentScreen.asStateFlow()

  private val _isDarkMode = MutableStateFlow(false)
  val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

  // --- SNACKBAR / MESSAGES ---
  private val _snackbarMessage = MutableSharedFlow<String>()
  val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

  // --- ATTENDANCE REQUIREMENTS & POLICIES (CONFIGURABLE BY ADMIN) ---
  private val prefs = application.getSharedPreferences("attendx_policy_prefs", Context.MODE_PRIVATE)

  private val _attendancePolicy = MutableStateFlow(loadPolicyFromPrefs())
  val attendancePolicy: StateFlow<AttendanceRequirementPolicy> = _attendancePolicy.asStateFlow()

  private fun loadPolicyFromPrefs(): AttendanceRequirementPolicy {
    return AttendanceRequirementPolicy(
      minAttendancePercent = prefs.getFloat("min_attendance", 75.0f),
      warningThresholdPercent = prefs.getFloat("warning_threshold", 70.0f),
      condonationLimitPercent = prefs.getFloat("condonation_limit", 65.0f),
      lateGracePeriodMinutes = prefs.getInt("late_grace_minutes", 10),
      maxCasualLeaves = prefs.getInt("max_casual_leaves", 6),
      academicTerm = prefs.getString("academic_term", "Academic Year 2026–2027 (Odd Semester)") ?: "Academic Year 2026–2027 (Odd Semester)",
      allowMedicalDutyCondonation = prefs.getBoolean("allow_med_condonation", true)
    )
  }

  fun updateAttendancePolicy(policy: AttendanceRequirementPolicy) {
    prefs.edit()
      .putFloat("min_attendance", policy.minAttendancePercent)
      .putFloat("warning_threshold", policy.warningThresholdPercent)
      .putFloat("condonation_limit", policy.condonationLimitPercent)
      .putInt("late_grace_minutes", policy.lateGracePeriodMinutes)
      .putInt("max_casual_leaves", policy.maxCasualLeaves)
      .putString("academic_term", policy.academicTerm)
      .putBoolean("allow_med_condonation", policy.allowMedicalDutyCondonation)
      .apply()
    _attendancePolicy.value = policy
    viewModelScope.launch {
      repository.insertAuditLog(
        AuditLogEntity(
          id = "LOG-${UUID.randomUUID().toString().take(8)}",
          actorName = _currentUser.value?.name ?: "Admin",
          actorRole = "ADMIN",
          action = "Requirements & Policy Updated",
          details = "Updated minimum attendance requirement to ${policy.minAttendancePercent}%, warning threshold to ${policy.warningThresholdPercent}%"
        )
      )
    }
    showMessage("College Attendance Requirements & Policy updated successfully! 📋")
  }

  // --- DATA FLOWS ---
  val allStudents: StateFlow<List<UserEntity>> = repository.getStudents()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val allFaculty: StateFlow<List<UserEntity>> = repository.getFaculty()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val allDepartments: StateFlow<List<DepartmentEntity>> = repository.allDepartments
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val allSubjects: StateFlow<List<SubjectEntity>> = repository.allSubjects
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val allTimetables: StateFlow<List<TimetableEntity>> = repository.allTimetables
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val activeSessions: StateFlow<List<AttendanceSessionEntity>> = repository.activeSessions
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val allAuditLogs: StateFlow<List<AuditLogEntity>> = repository.allAuditLogs
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // Campus Notices & Circulars Flow
  val allNotices: StateFlow<List<NoticeEntity>> = repository.allNotices
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // Student specific records flow
  private val _studentRecords = MutableStateFlow<List<AttendanceRecordEntity>>(emptyList())
  val studentRecords: StateFlow<List<AttendanceRecordEntity>> = _studentRecords.asStateFlow()

  // Student notifications flow
  private val _notifications = MutableStateFlow<List<NotificationEntity>>(emptyList())
  val notifications: StateFlow<List<NotificationEntity>> = _notifications.asStateFlow()

  // Leave Requests Flows
  val allLeaves: StateFlow<List<LeaveRequestEntity>> = repository.getAllLeaveRequests()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  private val _studentLeaves = MutableStateFlow<List<LeaveRequestEntity>>(emptyList())
  val studentLeaves: StateFlow<List<LeaveRequestEntity>> = _studentLeaves.asStateFlow()

  private val _facultyLeaves = MutableStateFlow<List<LeaveRequestEntity>>(emptyList())
  val facultyLeaves: StateFlow<List<LeaveRequestEntity>> = _facultyLeaves.asStateFlow()

  // Faculty active marking session
  private val _selectedSessionForMarking = MutableStateFlow<AttendanceSessionEntity?>(null)
  val selectedSessionForMarking: StateFlow<AttendanceSessionEntity?> = _selectedSessionForMarking.asStateFlow()

  private val _facultyActiveQRSession = MutableStateFlow<AttendanceSessionEntity?>(null)
  val facultyActiveQRSession: StateFlow<AttendanceSessionEntity?> = _facultyActiveQRSession.asStateFlow()

  // Real-time list of students who have scanned the QR code for the active session
  private val _activeSessionAttendees = MutableStateFlow<List<AttendanceRecordEntity>>(emptyList())
  val activeSessionAttendees: StateFlow<List<AttendanceRecordEntity>> = _activeSessionAttendees.asStateFlow()

  private var sessionAttendeesJob: kotlinx.coroutines.Job? = null

  fun observeSessionAttendees(sessionId: String?) {
    sessionAttendeesJob?.cancel()
    if (sessionId.isNullOrBlank()) {
      _activeSessionAttendees.value = emptyList()
      return
    }
    sessionAttendeesJob = viewModelScope.launch {
      repository.getAttendanceForSession(sessionId).collect { list ->
        _activeSessionAttendees.value = list
      }
    }
  }

  private val _targetSubjectForScanning = MutableStateFlow<SubjectEntity?>(null)
  val targetSubjectForScanning: StateFlow<SubjectEntity?> = _targetSubjectForScanning.asStateFlow()

  // Student Attendance Summary
  val studentSummary: StateFlow<AttendanceSummary> = _studentRecords
    .combine(_currentUser) { records, _ ->
      repository.computeSummary(records)
    }.stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(5000),
      AttendanceSummary(0, 0, 0, 0, 0f)
    )

  // Student Subject Attendance Stats
  val studentSubjectStats: StateFlow<List<SubjectAttendanceStat>> = combine(
    allSubjects,
    _studentRecords
  ) { subjects, records ->
    repository.computeSubjectStats(subjects, records)
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  private val userObserverJobs = mutableListOf<kotlinx.coroutines.Job>()

  init {
    viewModelScope.launch(Dispatchers.IO) {
      repository.ensureSeededData()
    }

    // Collect active user's records & notifications whenever user changes
    viewModelScope.launch {
      _currentUser.collect { user ->
        userObserverJobs.forEach { it.cancel() }
        userObserverJobs.clear()

        if (user != null) {
          userObserverJobs.add(launch {
            repository.getAttendanceForStudent(user.id).collect { records ->
              _studentRecords.value = records
            }
          })
          userObserverJobs.add(launch {
            repository.getNotifications(user.id).collect { notifs ->
              _notifications.value = notifs
            }
          })
          userObserverJobs.add(launch {
            repository.getLeaveRequestsForStudent(user.id, user.rollNo).collect { leaves ->
              _studentLeaves.value = leaves
            }
          })
          userObserverJobs.add(launch {
            if (user.role.equals("ADMIN", ignoreCase = true)) {
              repository.getAllLeaveRequests().collect { leaves ->
                _facultyLeaves.value = leaves
              }
            } else {
              repository.getLeaveRequestsForFaculty(user.id, user.name, user.role).collect { leaves ->
                _facultyLeaves.value = leaves
              }
            }
          })
        } else {
          _studentRecords.value = emptyList()
          _notifications.value = emptyList()
          _studentLeaves.value = emptyList()
          _facultyLeaves.value = emptyList()
        }
      }
    }
  }

  fun toggleDarkMode() {
    _isDarkMode.value = !_isDarkMode.value
  }

  fun navigateTo(destination: ScreenDestination) {
    _currentScreen.value = destination
  }

  fun showMessage(msg: String) {
    viewModelScope.launch {
      _snackbarMessage.emit(msg)
    }
  }

  // --- AUTHENTICATION ---
  fun login(emailOrRoll: String, pass: String, onResult: (Boolean, String) -> Unit) {
    viewModelScope.launch {
      repository.ensureSeededData()
      val user = repository.authenticate(emailOrRoll, pass)
      if (user != null) {
        if (!user.isActive) {
          onResult(false, "Account is currently deactivated. Please contact college admin.")
          return@launch
        }
        _currentUser.value = user
        when (user.role.uppercase()) {
          "STUDENT" -> _currentScreen.value = ScreenDestination.StudentMain
          "FACULTY" -> _currentScreen.value = ScreenDestination.FacultyMain
          "ADMIN" -> _currentScreen.value = ScreenDestination.AdminMain
          else -> _currentScreen.value = ScreenDestination.StudentMain
        }
        onResult(true, "Welcome back, ${user.name}!")
      } else {
        onResult(false, "Invalid credentials. Please check email/roll number and password.")
      }
    }
  }

  fun quickDemoLogin(role: String) {
    viewModelScope.launch {
      repository.ensureSeededData()
      val user = repository.getDemoUserByRole(role)
        ?: repository.authenticate(role, "password123")
      if (user != null) {
        if (!user.isActive) {
          showMessage("Account is currently deactivated.")
          return@launch
        }
        _currentUser.value = user
        when (user.role.uppercase()) {
          "STUDENT" -> _currentScreen.value = ScreenDestination.StudentMain
          "FACULTY" -> _currentScreen.value = ScreenDestination.FacultyMain
          "ADMIN" -> _currentScreen.value = ScreenDestination.AdminMain
          else -> _currentScreen.value = ScreenDestination.StudentMain
        }
        showMessage("Logged in as ${user.name} (${user.role})")
      } else {
        showMessage("Demo account for $role is being prepared. Please try again in a moment.")
      }
    }
  }

  fun logout() {
    _currentUser.value = null
    _currentScreen.value = ScreenDestination.Landing
  }

  // --- QR CODE SCANNING (STUDENT) ---
  fun scanQRCode(scannedToken: String, onComplete: (Boolean, String) -> Unit) {
    val student = _currentUser.value ?: run {
      onComplete(false, "User not authenticated.")
      return
    }
    viewModelScope.launch {
      val result = repository.verifyAndMarkQRAttendance(student, scannedToken)
      result.onSuccess { msg ->
        showMessage(msg)
        onComplete(true, msg)
      }.onFailure { err ->
        onComplete(false, err.message ?: "Verification failed.")
      }
    }
  }

  fun openQRScannerForSubject(subjectId: String? = null) {
    if (subjectId != null) {
      _targetSubjectForScanning.value = allSubjects.value.firstOrNull {
        it.id == subjectId || it.code.equals(subjectId, ignoreCase = true)
      }
    } else {
      _targetSubjectForScanning.value = null
    }
    _currentScreen.value = ScreenDestination.StudentQRScanner
  }

  fun setFacultyActiveQRSession(session: AttendanceSessionEntity) {
    _facultyActiveQRSession.value = session
    observeSessionAttendees(session.id)
    _currentScreen.value = ScreenDestination.FacultyQRSession
  }

  // --- FACULTY SESSIONS & ATTENDANCE ---
  fun startNewSession(subject: SubjectEntity, section: String = "CSE-4A") {
    val faculty = _currentUser.value ?: return
    viewModelScope.launch {
      val session = repository.createAttendanceSession(subject, faculty, section)
      _facultyActiveQRSession.value = session
      observeSessionAttendees(session.id)
      _currentScreen.value = ScreenDestination.FacultyQRSession
      showMessage("Attendance session started! Code: ${session.id}")
    }
  }

  fun closeActiveSession() {
    val session = _facultyActiveQRSession.value ?: return
    viewModelScope.launch {
      repository.closeSession(session)
      _facultyActiveQRSession.value = null
      observeSessionAttendees(null)
      _currentScreen.value = ScreenDestination.FacultyMain
      showMessage("Attendance session ${session.id} closed successfully ✅")
    }
  }

  fun openManualMarking(session: AttendanceSessionEntity) {
    _selectedSessionForMarking.value = session
    _currentScreen.value = ScreenDestination.FacultyMarkAttendance
  }

  fun saveManualAttendance(
    session: AttendanceSessionEntity,
    markingList: List<Pair<UserEntity, String>>
  ) {
    viewModelScope.launch {
      repository.saveBatchAttendance(
        sessionId = session.id,
        subjectId = session.subjectId,
        subjectName = session.subjectName,
        facultyId = session.facultyId,
        section = session.section,
        date = session.date,
        time = session.startTime,
        records = markingList
      )
      showMessage("Attendance successfully recorded for ${markingList.size} students ✅")
      _currentScreen.value = ScreenDestination.FacultyMain
    }
  }

  // --- ADMIN ACTIONS ---
  fun toggleUserActive(user: UserEntity) {
    viewModelScope.launch {
      val updated = user.copy(isActive = !user.isActive)
      repository.updateUser(updated)
      repository.insertAuditLog(
        AuditLogEntity(
          id = "LOG-${UUID.randomUUID().toString().take(8)}",
          actorName = _currentUser.value?.name ?: "Admin",
          actorRole = "ADMIN",
          action = if (updated.isActive) "User Activated" else "User Deactivated",
          details = "Changed active status of ${user.name} (${user.role})"
        )
      )
      showMessage("User ${user.name} is now ${if (updated.isActive) "Active" else "Deactivated"}")
    }
  }

  fun deleteUser(user: UserEntity) {
    viewModelScope.launch {
      repository.deleteUser(user)
      repository.insertAuditLog(
        AuditLogEntity(
          id = "LOG-${UUID.randomUUID().toString().take(8)}",
          actorName = _currentUser.value?.name ?: "Admin",
          actorRole = "ADMIN",
          action = "User Deleted",
          details = "Deleted ${user.role.lowercase()} account for ${user.name}"
        )
      )
      showMessage("User ${user.name} deleted.")
    }
  }

  fun addOrUpdateUser(user: UserEntity, isEditing: Boolean = false) {
    viewModelScope.launch {
      repository.insertUser(user)
      repository.insertAuditLog(
        AuditLogEntity(
          id = "LOG-${UUID.randomUUID().toString().take(8)}",
          actorName = _currentUser.value?.name ?: "Admin",
          actorRole = "ADMIN",
          action = if (isEditing) "User Profile Edited" else "New User Created",
          details = "${if (isEditing) "Modified" else "Created"} ${user.role.lowercase()} details for ${user.name}"
        )
      )
      showMessage("${user.role.lowercase().replaceFirstChar { it.uppercase() }} ${user.name} ${if (isEditing) "updated" else "added"} successfully ✅")
    }
  }

  fun deleteSubject(subject: SubjectEntity) {
    viewModelScope.launch {
      repository.deleteSubject(subject)
      repository.insertAuditLog(
        AuditLogEntity(
          id = "LOG-${UUID.randomUUID().toString().take(8)}",
          actorName = _currentUser.value?.name ?: "Admin",
          actorRole = "ADMIN",
          action = "Subject Deleted",
          details = "Deleted subject ${subject.name} (${subject.code})"
        )
      )
      showMessage("Subject ${subject.name} removed.")
    }
  }

  fun addOrUpdateSubject(subject: SubjectEntity, isEditing: Boolean = false) {
    viewModelScope.launch {
      repository.insertSubject(subject)
      repository.insertAuditLog(
        AuditLogEntity(
          id = "LOG-${UUID.randomUUID().toString().take(8)}",
          actorName = _currentUser.value?.name ?: "Admin",
          actorRole = "ADMIN",
          action = if (isEditing) "Subject Requirement Edited" else "New Subject Created",
          details = "${if (isEditing) "Updated" else "Created"} subject ${subject.name} (${subject.code}), min attendance: ${subject.targetAttendancePercent}%"
        )
      )
      showMessage("Subject ${subject.name} ${if (isEditing) "updated" else "saved"} successfully ✅")
    }
  }

  fun deleteDepartment(dept: DepartmentEntity) {
    viewModelScope.launch {
      repository.deleteDepartment(dept)
      repository.insertAuditLog(
        AuditLogEntity(
          id = "LOG-${UUID.randomUUID().toString().take(8)}",
          actorName = _currentUser.value?.name ?: "Admin",
          actorRole = "ADMIN",
          action = "Department Deleted",
          details = "Deleted department ${dept.name} (${dept.code})"
        )
      )
      showMessage("Department ${dept.name} removed.")
    }
  }

  fun addOrUpdateDepartment(dept: DepartmentEntity, isEditing: Boolean = false) {
    viewModelScope.launch {
      repository.insertDepartment(dept)
      repository.insertAuditLog(
        AuditLogEntity(
          id = "LOG-${UUID.randomUUID().toString().take(8)}",
          actorName = _currentUser.value?.name ?: "Admin",
          actorRole = "ADMIN",
          action = if (isEditing) "Department Edited" else "Department Created",
          details = "${if (isEditing) "Updated" else "Created"} department ${dept.name} (${dept.code})"
        )
      )
      showMessage("Department ${dept.name} ${if (isEditing) "updated" else "added"} successfully ✅")
    }
  }

  fun addDepartment(dept: DepartmentEntity) {
    addOrUpdateDepartment(dept, isEditing = false)
  }

  fun addOrUpdateTimetable(tt: TimetableEntity, isEditing: Boolean = false) {
    viewModelScope.launch {
      repository.insertTimetable(tt)
      repository.insertAuditLog(
        AuditLogEntity(
          id = "LOG-${UUID.randomUUID().toString().take(8)}",
          actorName = _currentUser.value?.name ?: "Admin",
          actorRole = "ADMIN",
          action = if (isEditing) "Class Schedule Edited" else "Class Schedule Added",
          details = "${if (isEditing) "Updated" else "Added"} schedule for ${tt.subjectName} on ${tt.dayOfWeek} (${tt.startTime} - ${tt.endTime}) in ${tt.room}"
        )
      )
      showMessage("Class schedule for ${tt.subjectName} ${if (isEditing) "updated" else "added"} successfully ✅")
    }
  }

  fun addTimetable(tt: TimetableEntity) {
    addOrUpdateTimetable(tt, isEditing = false)
  }

  fun deleteTimetable(tt: TimetableEntity) {
    viewModelScope.launch {
      repository.deleteTimetable(tt)
      repository.insertAuditLog(
        AuditLogEntity(
          id = "LOG-${UUID.randomUUID().toString().take(8)}",
          actorName = _currentUser.value?.name ?: "Admin",
          actorRole = "ADMIN",
          action = "Class Schedule Deleted",
          details = "Deleted schedule for ${tt.subjectName} on ${tt.dayOfWeek}"
        )
      )
      showMessage("Class schedule removed.")
    }
  }

  fun markNotificationRead(id: String) {
    viewModelScope.launch {
      repository.markNotificationAsRead(id)
    }
  }

  fun updateUserProfile(name: String, phone: String, section: String) {
    val current = _currentUser.value ?: return
    viewModelScope.launch {
      val updated = current.copy(name = name, phone = phone, section = section)
      repository.updateUser(updated)
      _currentUser.value = updated
      showMessage("Profile updated successfully ✅")
    }
  }

  // --- LEAVE ACTIONS ---
  fun applyStudentLeave(
    facultyId: String,
    facultyName: String,
    leaveType: String,
    startDate: String,
    endDate: String,
    numberOfDays: Int,
    reason: String,
    attachmentUri: String? = null,
    onSuccess: () -> Unit = {}
  ) {
    val student = _currentUser.value ?: return
    if (reason.isBlank()) {
      showMessage("Please provide a reason for the leave application.")
      return
    }
    viewModelScope.launch {
      try {
        val resolvedFaculty = allFaculty.value.firstOrNull { it.id == facultyId || it.name.equals(facultyName, ignoreCase = true) }
          ?: allFaculty.value.firstOrNull()
        val targetFacultyId = if (facultyId.isNotBlank()) facultyId else (resolvedFaculty?.id ?: "U-FAC-001")
        val targetFacultyName = if (facultyName.isNotBlank()) facultyName else (resolvedFaculty?.name ?: "Prof. Robert Chen")

        repository.applyForLeave(
          student = student,
          facultyId = targetFacultyId,
          facultyName = targetFacultyName,
          leaveType = leaveType,
          startDate = startDate,
          endDate = endDate,
          numberOfDays = numberOfDays,
          reason = reason,
          attachmentUri = attachmentUri
        )
        showMessage("Leave application submitted to $targetFacultyName! 📋")
        onSuccess()
      } catch (e: Exception) {
        showMessage("Failed to submit leave: ${e.localizedMessage}")
      }
    }
  }

  fun broadcastDefaulterAlert(
    defaulters: List<UserEntity>,
    message: String,
    onSuccess: () -> Unit = {}
  ) {
    val faculty = _currentUser.value ?: return
    if (defaulters.isEmpty()) {
      showMessage("No defaulter students found to alert.")
      return
    }
    viewModelScope.launch {
      try {
        repository.broadcastDefaulterAlert(faculty, defaulters, message)
        showMessage("Debarment warning dispatched to ${defaulters.size} students! 📢")
        onSuccess()
      } catch (e: Exception) {
        showMessage("Failed to dispatch notices: ${e.localizedMessage}")
      }
    }
  }

  fun reviewLeave(
    leaveId: String,
    status: String,
    remarks: String,
    onSuccess: () -> Unit = {}
  ) {
    val reviewer = _currentUser.value ?: return
    viewModelScope.launch {
      try {
        repository.reviewLeaveRequest(leaveId, status, remarks, reviewer)
        val text = if (status.equals("APPROVED", ignoreCase = true)) "Approved" else "Rejected"
        showMessage("Leave request marked as $text ✅")
        onSuccess()
      } catch (e: Exception) {
        showMessage("Failed to review leave: ${e.localizedMessage}")
      }
    }
  }

  fun cancelLeave(leave: LeaveRequestEntity) {
    val student = _currentUser.value ?: return
    viewModelScope.launch {
      try {
        repository.cancelLeave(leave, student)
        showMessage("Leave request for ${leave.startDate} cancelled & withdrawn ✅")
      } catch (e: Exception) {
        showMessage("Failed to cancel leave: ${e.localizedMessage}")
      }
    }
  }

  fun deleteLeave(leave: LeaveRequestEntity) {
    viewModelScope.launch {
      try {
        repository.deleteLeave(leave)
        showMessage("Leave request removed.")
      } catch (e: Exception) {
        showMessage("Failed to cancel leave: ${e.localizedMessage}")
      }
    }
  }

  fun submitLeaveRequest(model: LeaveRequestModel) {
    viewModelScope.launch {
      try {
        val existingStudent = allStudents.value.firstOrNull { it.rollNo.equals(model.rollNumber, ignoreCase = true) }
        val user = _currentUser.value ?: existingStudent ?: UserEntity(
          id = "U-STD-${model.rollNumber}",
          email = "${model.rollNumber.lowercase()}@attendx.edu",
          password = "password123",
          name = model.studentName,
          role = "STUDENT",
          rollNo = model.rollNumber,
          regNo = "REG-${model.rollNumber}",
          section = "CSE-4A",
          department = "Computer Science & Engineering"
        )
        val faculty = allFaculty.value.firstOrNull()
        val targetFacultyId = faculty?.id ?: "U-FAC-001"
        val targetFacultyName = faculty?.name ?: "Prof. Robert Chen"
        repository.applyForLeave(
          student = user,
          facultyId = targetFacultyId,
          facultyName = targetFacultyName,
          leaveType = model.leaveType,
          startDate = model.startDate,
          endDate = model.endDate,
          numberOfDays = model.numberOfDays,
          reason = model.reason
        )
        showMessage("Leave request submitted to $targetFacultyName for ${model.studentName}!")
      } catch (e: Exception) {
        showMessage("Failed to submit request: ${e.localizedMessage}")
      }
    }
  }

  fun deleteLeaveById(id: String) {
    viewModelScope.launch {
      try {
        repository.deleteLeaveById(id)
        showMessage("Leave request cancelled & withdrawn.")
      } catch (e: Exception) {
        showMessage("Failed to cancel request: ${e.localizedMessage}")
      }
    }
  }

  // --- NOTICE BOARD OPERATIONS (ADMIN & FACULTY BROADCAST) ---
  fun postNotice(
    title: String,
    content: String,
    category: String,
    targetAudience: String,
    priority: String,
    attachmentUri: String? = null,
    isPinned: Boolean = false,
    sendNotification: Boolean = true,
    onSuccess: () -> Unit = {}
  ) {
    val user = _currentUser.value ?: return
    viewModelScope.launch {
      try {
        val notice = NoticeEntity(
          id = "NTC-${UUID.randomUUID().toString().take(8)}",
          title = title.trim(),
          content = content.trim(),
          postedBy = user.name,
          authorRole = user.role,
          authorId = user.id,
          targetAudience = targetAudience,
          category = category,
          priority = priority,
          isPinned = isPinned,
          attachmentUri = attachmentUri,
          postedAt = System.currentTimeMillis()
        )
        repository.postNotice(notice, sendNotification)
        showMessage("Notice published & broadcasted successfully! 📢")
        onSuccess()
      } catch (e: Exception) {
        showMessage("Failed to post notice: ${e.localizedMessage}")
      }
    }
  }

  fun deleteNotice(notice: NoticeEntity) {
    viewModelScope.launch {
      try {
        repository.deleteNotice(notice)
        showMessage("Notice removed from notice board.")
      } catch (e: Exception) {
        showMessage("Failed to remove notice: ${e.localizedMessage}")
      }
    }
  }

  fun toggleNoticePin(notice: NoticeEntity) {
    viewModelScope.launch {
      try {
        repository.toggleNoticePin(notice.id, notice.isPinned)
        showMessage(if (notice.isPinned) "Notice unpinned." else "Notice pinned to top! 📌")
      } catch (e: Exception) {
        showMessage("Failed to update notice: ${e.localizedMessage}")
      }
    }
  }
}
