package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.data.model.toModel
import com.example.ui.components.AttendXTopBar
import com.example.ui.components.NotificationsDialog
import com.example.ui.components.ReportExportDialog
import com.example.ui.screens.admin.AdminMainScreen
import com.example.ui.screens.auth.AuthScreen
import com.example.ui.screens.faculty.FacultyMainScreen
import com.example.ui.screens.faculty.FacultyMarkAttendanceScreen
import com.example.ui.screens.faculty.FacultyQRSessionScreen
import com.example.ui.screens.landing.LandingScreen
import com.example.ui.screens.notice.NoticeBoardScreen
import com.example.ui.screens.schedule.TodaysScheduleScreen
import com.example.ui.screens.student.AttendanceScannerScreen
import com.example.ui.screens.student.LeaveRequestScreen
import com.example.ui.screens.student.StudentLeaveScreen
import com.example.ui.screens.student.StudentMainScreen
import com.example.ui.screens.student.StudentQRScannerScreen
import com.example.ui.theme.AttendXTheme
import com.example.ui.viewmodel.AttendXViewModel
import com.example.ui.viewmodel.ScreenDestination

class MainActivity : ComponentActivity() {

  private val viewModel: AttendXViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val isDarkMode by viewModel.isDarkMode.collectAsState()

      AttendXTheme(darkTheme = isDarkMode) {
        AttendXApp(viewModel = viewModel)
      }
    }
  }
}

@Composable
fun AttendXApp(viewModel: AttendXViewModel) {
  val currentUser by viewModel.currentUser.collectAsState()
  val currentScreen by viewModel.currentScreen.collectAsState()
  val isDarkMode by viewModel.isDarkMode.collectAsState()

  val allStudents by viewModel.allStudents.collectAsState()
  val allFaculty by viewModel.allFaculty.collectAsState()
  val allDepartments by viewModel.allDepartments.collectAsState()
  val allSubjects by viewModel.allSubjects.collectAsState()
  val allTimetables by viewModel.allTimetables.collectAsState()
  val activeSessions by viewModel.activeSessions.collectAsState()
  val allAuditLogs by viewModel.allAuditLogs.collectAsState()
  val studentRecords by viewModel.studentRecords.collectAsState()
  val notifications by viewModel.notifications.collectAsState()
  val studentSummary by viewModel.studentSummary.collectAsState()
  val studentSubjectStats by viewModel.studentSubjectStats.collectAsState()
  val selectedSessionForMarking by viewModel.selectedSessionForMarking.collectAsState()
  val facultyActiveQRSession by viewModel.facultyActiveQRSession.collectAsState()
  val activeSessionAttendees by viewModel.activeSessionAttendees.collectAsState()
  val targetSubjectForScanning by viewModel.targetSubjectForScanning.collectAsState()
  val studentLeaves by viewModel.studentLeaves.collectAsState()
  val facultyLeaves by viewModel.facultyLeaves.collectAsState()
  val attendancePolicy by viewModel.attendancePolicy.collectAsState()
  val allNotices by viewModel.allNotices.collectAsState()

  val snackbarHostState = remember { SnackbarHostState() }
  var showNotificationsDialog by remember { mutableStateOf(false) }
  var reportDialogData by remember { mutableStateOf<Triple<String, String, String>?>(null) }

  // Listen to snackbar messages
  LaunchedEffect(key1 = true) {
    viewModel.snackbarMessage.collect { message ->
      snackbarHostState.showSnackbar(message)
    }
  }

  // Handle hardware back button
  BackHandler(enabled = currentScreen !is ScreenDestination.Landing) {
    when (currentScreen) {
      is ScreenDestination.StudentQRScanner -> viewModel.navigateTo(ScreenDestination.StudentMain)
      is ScreenDestination.LeaveRequest -> viewModel.navigateTo(ScreenDestination.StudentMain)
      is ScreenDestination.StudentLeaves -> viewModel.navigateTo(ScreenDestination.StudentMain)
      is ScreenDestination.TodaysSchedule -> viewModel.navigateTo(ScreenDestination.StudentMain)
      is ScreenDestination.NoticeBoard -> {
        when (currentUser?.role) {
          "STUDENT" -> viewModel.navigateTo(ScreenDestination.StudentMain)
          "FACULTY" -> viewModel.navigateTo(ScreenDestination.FacultyMain)
          "ADMIN" -> viewModel.navigateTo(ScreenDestination.AdminMain)
          else -> viewModel.navigateTo(ScreenDestination.Landing)
        }
      }
      is ScreenDestination.FacultyLeaves -> viewModel.navigateTo(ScreenDestination.FacultyMain)
      is ScreenDestination.FacultyMarkAttendance -> viewModel.navigateTo(ScreenDestination.FacultyMain)
      is ScreenDestination.FacultyQRSession -> viewModel.navigateTo(ScreenDestination.FacultyMain)
      is ScreenDestination.Auth -> viewModel.navigateTo(ScreenDestination.Landing)
      else -> viewModel.logout()
    }
  }

  Scaffold(
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      if (currentScreen is ScreenDestination.StudentMain ||
        currentScreen is ScreenDestination.FacultyMain ||
        currentScreen is ScreenDestination.AdminMain
      ) {
        val unreadCount = notifications.count { !it.isRead }
        AttendXTopBar(
          user = currentUser,
          isDarkMode = isDarkMode,
          unreadNotificationCount = unreadCount,
          onToggleDarkMode = { viewModel.toggleDarkMode() },
          onOpenNotifications = { showNotificationsDialog = true },
          onOpenNoticeBoard = { viewModel.navigateTo(ScreenDestination.NoticeBoard) },
          onOpenProfile = {
            viewModel.showMessage("Logged in as ${currentUser?.name} (${currentUser?.role})")
          },
          onLogout = { viewModel.logout() }
        )
      }
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(
          if (currentScreen is ScreenDestination.StudentMain ||
            currentScreen is ScreenDestination.FacultyMain ||
            currentScreen is ScreenDestination.AdminMain
          ) innerPadding else androidx.compose.foundation.layout.PaddingValues()
        )
    ) {
      AnimatedContent(
        targetState = currentScreen,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "screen_transition"
      ) { destination ->
        when (destination) {
          is ScreenDestination.Landing -> {
            LandingScreen(
              onNavigateToLogin = { viewModel.navigateTo(ScreenDestination.Auth) },
              onQuickDemoLogin = { role -> viewModel.quickDemoLogin(role) },
              onToggleTheme = { viewModel.toggleDarkMode() },
              isDarkMode = isDarkMode
            )
          }

          is ScreenDestination.Auth -> {
            AuthScreen(
              onBackToLanding = { viewModel.navigateTo(ScreenDestination.Landing) },
              onLogin = { email, pass, cb -> viewModel.login(email, pass, cb) },
              onQuickDemoLogin = { role -> viewModel.quickDemoLogin(role) }
            )
          }

          is ScreenDestination.StudentMain -> {
            currentUser?.let { user ->
              StudentMainScreen(
                user = user,
                summary = studentSummary,
                subjectStats = studentSubjectStats,
                records = studentRecords,
                timetables = allTimetables,
                activeSessions = activeSessions,
                pendingLeavesCount = studentLeaves.count { it.status.equals("PENDING", ignoreCase = true) },
                notices = allNotices,
                onScanQR = { viewModel.openQRScannerForSubject(null) },
                onScanQRForSubject = { subjectId -> viewModel.openQRScannerForSubject(subjectId) },
                onOpenNoticeBoard = { viewModel.navigateTo(ScreenDestination.NoticeBoard) },
                onOpenLeaveSection = { viewModel.navigateTo(ScreenDestination.StudentLeaves) },
                onOpenSchedule = { viewModel.navigateTo(ScreenDestination.TodaysSchedule) },
                onOpenReport = { title, summary, table ->
                  reportDialogData = Triple(title, summary, table)
                },
                onUpdateProfile = { name, phone, section ->
                  viewModel.updateUserProfile(name, phone, section)
                }
              )
            }
          }

          is ScreenDestination.FacultyMain -> {
            currentUser?.let { faculty ->
              FacultyMainScreen(
                faculty = faculty,
                subjects = allSubjects,
                students = allStudents,
                timetables = allTimetables,
                activeSessions = activeSessions,
                leaveRequests = facultyLeaves,
                notices = allNotices,
                onOpenNoticeBoard = { viewModel.navigateTo(ScreenDestination.NoticeBoard) },
                onReviewLeave = { id, status, remarks ->
                  viewModel.reviewLeave(id, status, remarks)
                },
                onBroadcastDefaulters = { defaulters, message ->
                  viewModel.broadcastDefaulterAlert(defaulters, message)
                },
                onStartSession = { subject, section -> viewModel.startNewSession(subject, section) },
                onTakeAttendance = { session -> viewModel.setFacultyActiveQRSession(session) },
                onOpenReport = { title, summary, table ->
                  reportDialogData = Triple(title, summary, table)
                },
                onOpenActiveQRSession = {
                  viewModel.navigateTo(ScreenDestination.FacultyQRSession)
                },
                onOpenSessionQR = { session ->
                  viewModel.setFacultyActiveQRSession(session)
                }
              )
            }
          }

          is ScreenDestination.AdminMain -> {
            currentUser?.let { admin ->
              AdminMainScreen(
                admin = admin,
                students = allStudents,
                facultyList = allFaculty,
                departments = allDepartments,
                subjects = allSubjects,
                timetables = allTimetables,
                auditLogs = allAuditLogs,
                attendancePolicy = attendancePolicy,
                notices = allNotices,
                onOpenNoticeBoard = { viewModel.navigateTo(ScreenDestination.NoticeBoard) },
                onToggleUserActive = { viewModel.toggleUserActive(it) },
                onDeleteUser = { viewModel.deleteUser(it) },
                onAddOrUpdateUser = { user, isEdit -> viewModel.addOrUpdateUser(user, isEdit) },
                onAddOrUpdateSubject = { subject, isEdit -> viewModel.addOrUpdateSubject(subject, isEdit) },
                onDeleteSubject = { viewModel.deleteSubject(it) },
                onAddOrUpdateDepartment = { dept, isEdit -> viewModel.addOrUpdateDepartment(dept, isEdit) },
                onDeleteDepartment = { viewModel.deleteDepartment(it) },
                onAddOrUpdateTimetable = { tt, isEdit -> viewModel.addOrUpdateTimetable(tt, isEdit) },
                onDeleteTimetable = { viewModel.deleteTimetable(it) },
                onUpdateAttendancePolicy = { viewModel.updateAttendancePolicy(it) },
                onOpenReport = { title, summary, table ->
                  reportDialogData = Triple(title, summary, table)
                }
              )
            }
          }

          is ScreenDestination.FacultyMarkAttendance -> {
            // Enforce live QR session flow for attendance
            val session = selectedSessionForMarking ?: activeSessions.firstOrNull()
            if (session != null) {
              FacultyQRSessionScreen(
                session = session,
                allActiveSessions = activeSessions,
                subjects = allSubjects,
                attendees = activeSessionAttendees,
                onBack = { viewModel.navigateTo(ScreenDestination.FacultyMain) },
                onCloseSession = { viewModel.closeActiveSession() },
                onSelectSession = { viewModel.setFacultyActiveQRSession(it) },
                onStartSession = { subject, section -> viewModel.startNewSession(subject, section) }
              )
            } else {
              viewModel.navigateTo(ScreenDestination.FacultyMain)
            }
          }

          is ScreenDestination.FacultyQRSession -> {
            val session = facultyActiveQRSession ?: activeSessions.firstOrNull()
            if (session != null) {
              FacultyQRSessionScreen(
                session = session,
                allActiveSessions = activeSessions,
                subjects = allSubjects,
                attendees = activeSessionAttendees,
                onBack = { viewModel.navigateTo(ScreenDestination.FacultyMain) },
                onCloseSession = { viewModel.closeActiveSession() },
                onSelectSession = { viewModel.setFacultyActiveQRSession(it) },
                onStartSession = { subject, section -> viewModel.startNewSession(subject, section) }
              )
            }
          }

          is ScreenDestination.StudentQRScanner -> {
            AttendanceScannerScreen(
              activeSessions = activeSessions,
              subjects = allSubjects,
              currentUser = currentUser,
              initialTargetSubject = targetSubjectForScanning,
              onBack = { viewModel.navigateTo(ScreenDestination.StudentMain) },
              onVerifyQRToken = { token, cb ->
                viewModel.scanQRCode(token, cb)
              }
            )
          }

          is ScreenDestination.StudentLeaves -> {
            currentUser?.let { student ->
              StudentLeaveScreen(
                student = student,
                leaves = studentLeaves,
                facultyList = allFaculty,
                onApplyLeave = { facultyId, facultyName, leaveType, startDate, endDate, numberOfDays, reason, attachmentUri ->
                  viewModel.applyStudentLeave(
                    facultyId = facultyId,
                    facultyName = facultyName,
                    leaveType = leaveType,
                    startDate = startDate,
                    endDate = endDate,
                    numberOfDays = numberOfDays,
                    reason = reason,
                    attachmentUri = attachmentUri
                  )
                },
                onDeleteLeave = { leave ->
                  viewModel.deleteLeave(leave)
                },
                onBack = { viewModel.navigateTo(ScreenDestination.StudentMain) }
              )
            }
          }

          is ScreenDestination.LeaveRequest -> {
            currentUser?.let { student ->
              val pendingModels = studentLeaves
                .filter { it.status.equals("PENDING", ignoreCase = true) }
                .map { it.toModel() }
              LeaveRequestScreen(
                initialName = student.name,
                initialRollNumber = student.rollNo,
                existingPendingRequests = pendingModels,
                onSubmitRequest = { model ->
                  viewModel.submitLeaveRequest(model)
                },
                onDeleteRequest = { id ->
                  viewModel.deleteLeaveById(id)
                },
                onBack = { viewModel.navigateTo(ScreenDestination.StudentMain) }
              )
            }
          }

          is ScreenDestination.TodaysSchedule -> {
            currentUser?.let { user ->
              TodaysScheduleScreen(
                user = user,
                timetables = allTimetables,
                records = studentRecords,
                activeSessions = activeSessions,
                onScanQR = { viewModel.openQRScannerForSubject(null) },
                onScanQRForSubject = { subjectId -> viewModel.openQRScannerForSubject(subjectId) },
                onBack = { viewModel.navigateTo(ScreenDestination.StudentMain) }
              )
            }
          }

          is ScreenDestination.FacultyLeaves -> {
            currentUser?.let { faculty ->
              com.example.ui.screens.faculty.FacultyLeaveRequestsContent(
                faculty = faculty,
                leaveRequests = facultyLeaves,
                onReviewLeave = { leaveId, status, remarks ->
                  viewModel.reviewLeave(leaveId, status, remarks)
                }
              )
            }
          }

          is ScreenDestination.NoticeBoard -> {
            NoticeBoardScreen(
              user = currentUser,
              notices = allNotices,
              onBack = {
                when (currentUser?.role) {
                  "STUDENT" -> viewModel.navigateTo(ScreenDestination.StudentMain)
                  "FACULTY" -> viewModel.navigateTo(ScreenDestination.FacultyMain)
                  "ADMIN" -> viewModel.navigateTo(ScreenDestination.AdminMain)
                  else -> viewModel.navigateTo(ScreenDestination.Landing)
                }
              },
              onPostNotice = { title, content, cat, audience, priority, uri, pinned, broadcast ->
                viewModel.postNotice(title, content, cat, audience, priority, uri, pinned, broadcast)
              },
              onDeleteNotice = { notice ->
                viewModel.deleteNotice(notice)
              },
              onTogglePin = { notice ->
                viewModel.toggleNoticePin(notice)
              },
              onScanSession = {
                viewModel.openQRScannerForSubject(null)
              }
            )
          }
        }
      }
    }
  }

  // Report Export Dialog
  if (reportDialogData != null) {
    val (title, summary, table) = reportDialogData!!
    ReportExportDialog(
      reportTitle = title,
      reportSummary = summary,
      tableDataText = table,
      onDismiss = { reportDialogData = null }
    )
  }

  // Notifications Dialog
  if (showNotificationsDialog) {
    NotificationsDialog(
      notifications = notifications,
      onMarkRead = { viewModel.markNotificationRead(it) },
      onDismiss = { showNotificationsDialog = false },
      onScanSession = {
        showNotificationsDialog = false
        viewModel.openQRScannerForSubject(null)
      }
    )
  }
}
