package com.example.ui.screens.faculty

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.AttendanceSessionEntity
import com.example.data.model.LeaveRequestEntity
import com.example.data.model.NoticeEntity
import com.example.data.model.SubjectEntity
import com.example.data.model.TimetableEntity
import com.example.data.model.UserEntity
import com.example.ui.components.*
import com.example.ui.screens.student.DocumentPreviewDialog
import com.example.ui.theme.*

@Composable
fun FacultyMainScreen(
  faculty: UserEntity,
  subjects: List<SubjectEntity>,
  students: List<UserEntity>,
  timetables: List<TimetableEntity>,
  activeSessions: List<AttendanceSessionEntity>,
  leaveRequests: List<LeaveRequestEntity> = emptyList(),
  notices: List<NoticeEntity> = emptyList(),
  onOpenNoticeBoard: () -> Unit = {},
  onReviewLeave: (leaveId: String, status: String, remarks: String) -> Unit = { _, _, _ -> },
  onBroadcastDefaulters: (List<UserEntity>, String) -> Unit = { _, _ -> },
  onStartSession: (SubjectEntity, String) -> Unit = { _, _ -> },
  onTakeAttendance: (AttendanceSessionEntity) -> Unit,
  onOpenReport: (String, String, String) -> Unit,
  onOpenActiveQRSession: () -> Unit,
  onOpenSessionQR: (AttendanceSessionEntity) -> Unit = { onOpenActiveQRSession() }
) {
  var selectedTab by remember { mutableStateOf(0) }
  var showSubjectQRDialog by remember { mutableStateOf(false) }
  var subjectToPreselect by remember { mutableStateOf<SubjectEntity?>(null) }

  val pendingLeavesCount = remember(leaveRequests) {
    leaveRequests.count { it.status.equals("PENDING", ignoreCase = true) }
  }

  if (showSubjectQRDialog) {
    GenerateQRSubjectSelectionDialog(
      subjects = subjects,
      initialSubject = subjectToPreselect ?: subjects.firstOrNull(),
      onDismiss = {
        showSubjectQRDialog = false
        subjectToPreselect = null
      },
      onConfirm = { chosenSubject, chosenSection ->
        showSubjectQRDialog = false
        subjectToPreselect = null
        onStartSession(chosenSubject, chosenSection)
      }
    )
  }

  Scaffold(
    floatingActionButton = {
      ExtendedFloatingActionButton(
        onClick = {
          subjectToPreselect = null
          showSubjectQRDialog = true
        },
        icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) },
        text = { Text("Generate QR", fontWeight = FontWeight.Bold) },
        containerColor = Indigo600,
        contentColor = Color.White,
        modifier = Modifier.testTag("fab_generate_qr")
      )
    },
    bottomBar = {
      NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
      ) {
        NavigationBarItem(
          selected = selectedTab == 0,
          onClick = { selectedTab = 0 },
          icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
          label = { Text("Classes") },
          modifier = Modifier.testTag("faculty_nav_classes")
        )
        NavigationBarItem(
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          icon = { Icon(Icons.Default.MenuBook, contentDescription = "Subjects") },
          label = { Text("My Subjects") },
          modifier = Modifier.testTag("faculty_nav_subjects")
        )
        NavigationBarItem(
          selected = selectedTab == 2,
          onClick = { selectedTab = 2 },
          icon = { Icon(Icons.Default.People, contentDescription = "Students") },
          label = { Text("Students") },
          modifier = Modifier.testTag("faculty_nav_students")
        )
        NavigationBarItem(
          selected = selectedTab == 3,
          onClick = { selectedTab = 3 },
          icon = { Icon(Icons.Default.Assessment, contentDescription = "Reports") },
          label = { Text("Reports") },
          modifier = Modifier.testTag("faculty_nav_reports")
        )
        NavigationBarItem(
          selected = selectedTab == 4,
          onClick = { selectedTab = 4 },
          icon = {
            BadgedBox(
              badge = {
                if (pendingLeavesCount > 0) {
                  Badge(
                    containerColor = Amber500,
                    contentColor = Color.White
                  ) {
                    Text("$pendingLeavesCount")
                  }
                }
              }
            ) {
              Icon(Icons.Default.EventBusy, contentDescription = "Leaves")
            }
          },
          label = { Text("Leaves") },
          modifier = Modifier.testTag("faculty_nav_leaves")
        )
      }
    }
  ) { padding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
    ) {
      when (selectedTab) {
        0 -> FacultyClassesContent(
          faculty = faculty,
          subjects = subjects,
          studentsCount = students.size,
          timetables = timetables,
          activeSessions = activeSessions,
          leaveRequests = leaveRequests,
          notices = notices,
          onOpenNoticeBoard = onOpenNoticeBoard,
          onNavigateToLeaves = { selectedTab = 4 },
          onOpenQRSelection = { sub ->
            subjectToPreselect = sub
            showSubjectQRDialog = true
          },
          onTakeAttendance = onTakeAttendance,
          onOpenActiveQRSession = onOpenActiveQRSession,
          onOpenSessionQR = onOpenSessionQR
        )
        1 -> FacultySubjectsContent(
          subjects = subjects,
          onOpenQRSelection = { sub ->
            subjectToPreselect = sub
            showSubjectQRDialog = true
          }
        )
        2 -> FacultyStudentsContent(
          students = students,
          onExportList = {
            val summary = "Class Roster: CSE Section 4A (${students.size} enrolled students)"
            val table = buildString {
              append(String.format("%-12s %-22s %-12s\n", "Roll No", "Name", "Section"))
              append("----------------------------------------------------\n")
              students.forEach { s ->
                append(String.format("%-12s %-22s %-12s\n", s.rollNo, s.name.take(20), s.section))
              }
            }
            onOpenReport("Class Roster - CSE-4A", summary, table)
          }
        )
        3 -> FacultyReportsContent(
          faculty = faculty,
          subjects = subjects,
          students = students,
          onGenerateReport = onOpenReport,
          onBroadcastDefaulters = onBroadcastDefaulters
        )
        4 -> FacultyLeaveRequestsContent(
          faculty = faculty,
          leaveRequests = leaveRequests,
          onReviewLeave = onReviewLeave
        )
      }
    }
  }
}

// -------------------------------------------------------------
// 1. FACULTY CLASSES / DASHBOARD
// -------------------------------------------------------------
@Composable
private fun FacultyClassesContent(
  faculty: UserEntity,
  subjects: List<SubjectEntity>,
  studentsCount: Int,
  timetables: List<TimetableEntity>,
  activeSessions: List<AttendanceSessionEntity>,
  leaveRequests: List<LeaveRequestEntity> = emptyList(),
  notices: List<NoticeEntity> = emptyList(),
  onOpenNoticeBoard: () -> Unit = {},
  onNavigateToLeaves: () -> Unit = {},
  onOpenQRSelection: (SubjectEntity?) -> Unit,
  onTakeAttendance: (AttendanceSessionEntity) -> Unit,
  onOpenActiveQRSession: () -> Unit,
  onOpenSessionQR: (AttendanceSessionEntity) -> Unit = { onOpenActiveQRSession() }
) {
  val pendingLeaves = remember(leaveRequests) {
    leaveRequests.filter { it.status.equals("PENDING", ignoreCase = true) }
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Top Greeting
    item {
      Column {
        Text(
          text = "Welcome, ${faculty.name} 👨🏫",
          style = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.5).sp
          )
        )
        Text(
          text = "${faculty.designation} • ${faculty.department}",
          style = MaterialTheme.typography.bodySmall.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        )
      }
    }

    // Quick Action: Choose Subject and Generate QR Code
    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Indigo300),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onOpenQRSelection(null) }
          .testTag("faculty_quick_generate_qr_card")
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          Box(
            modifier = Modifier
              .size(46.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(Indigo600),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              Icons.Default.QrCodeScanner,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(24.dp)
            )
          }
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Generate QR Attendance",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
              text = "Choose subject from dropdown to launch live QR session",
              style = MaterialTheme.typography.bodySmall.copy(color = Slate600)
            )
          }
          FilledTonalButton(
            onClick = { onOpenQRSelection(null) },
            colors = ButtonDefaults.filledTonalButtonColors(containerColor = Indigo100, contentColor = Indigo800),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.testTag("quick_action_select_subject_btn")
          ) {
            Text("Select", fontWeight = FontWeight.Bold, fontSize = 12.sp)
          }
        }
      }
    }

    // Campus Notice Board & Announcement Card
    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, Indigo200),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onOpenNoticeBoard() }
          .testTag("faculty_notice_board_card")
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(40.dp)
                  .clip(RoundedCornerShape(10.dp))
                  .background(Indigo100),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  Icons.Default.Campaign,
                  contentDescription = null,
                  tint = Indigo700,
                  modifier = Modifier.size(22.dp)
                )
              }
              Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  Text(
                    text = "Notice Board & Circulars",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                  )
                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Indigo100
                  ) {
                    Text(
                      text = "${notices.size} Active",
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold,
                      color = Indigo800,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                  }
                }
                Text(
                  text = "Broadcast class updates & notify students",
                  style = MaterialTheme.typography.bodySmall.copy(color = Slate500)
                )
              }
            }

            Button(
              onClick = onOpenNoticeBoard,
              colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
              shape = RoundedCornerShape(8.dp),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Post Notice", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    // Student Leave Requests Alert Banner
    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
          containerColor = if (pendingLeaves.isNotEmpty()) Amber50 else MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
          1.dp,
          if (pendingLeaves.isNotEmpty()) Amber300 else Slate200
        ),
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onNavigateToLeaves() }
          .testTag("faculty_dashboard_leave_card")
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Box(
            modifier = Modifier
              .size(44.dp)
              .clip(CircleShape)
              .background(if (pendingLeaves.isNotEmpty()) Amber100 else Slate100),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.EventBusy,
              contentDescription = null,
              tint = if (pendingLeaves.isNotEmpty()) Amber700 else Slate600,
              modifier = Modifier.size(22.dp)
            )
          }

          Column(modifier = Modifier.weight(1f)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Text(
                text = "Student Leave Applications",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
              )
              if (pendingLeaves.isNotEmpty()) {
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = Amber500
                ) {
                  Text(
                    text = "${pendingLeaves.size} Pending",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontWeight = FontWeight.Bold,
                      color = Color.White,
                      fontSize = 10.sp
                    ),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
              }
            }
            Text(
              text = if (pendingLeaves.isNotEmpty())
                "${pendingLeaves.first().studentName} and others informed/applied for leave"
              else
                "No pending student leave requests requiring review",
              style = MaterialTheme.typography.bodySmall.copy(color = Slate600)
            )
          }

          FilledTonalButton(
            onClick = onNavigateToLeaves,
            colors = ButtonDefaults.filledTonalButtonColors(
              containerColor = if (pendingLeaves.isNotEmpty()) Amber200 else Slate100,
              contentColor = if (pendingLeaves.isNotEmpty()) Amber900 else Slate700
            ),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
          ) {
            Text("Review", fontWeight = FontWeight.Bold, fontSize = 12.sp)
          }
        }
      }
    }

    // Active Live Session Banners for All Subjects
    if (activeSessions.isNotEmpty()) {
      item {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Live QR Attendance Sessions (${activeSessions.size})",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
              text = "Tap to display QR",
              style = MaterialTheme.typography.labelSmall.copy(color = Indigo600, fontWeight = FontWeight.Bold)
            )
          }

          activeSessions.forEach { active ->
            Card(
              shape = RoundedCornerShape(14.dp),
              colors = CardDefaults.cardColors(containerColor = Indigo900),
              modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenSessionQR(active) }
                .testTag("faculty_active_session_${active.subjectId}")
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                      modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Emerald500)
                    )
                    Text(
                      text = "LIVE QR ACTIVE",
                      style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Emerald500
                      )
                    )
                  }
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(
                    text = "${active.subjectName} (${active.section})",
                    style = MaterialTheme.typography.titleSmall.copy(
                      fontWeight = FontWeight.Bold,
                      color = Color.White
                    )
                  )
                  Text(
                    text = "Room ${active.room} • Code: ${active.qrToken}",
                    style = MaterialTheme.typography.bodySmall.copy(color = Slate300)
                  )
                }

                Button(
                  onClick = { onOpenSessionQR(active) },
                  colors = ButtonDefaults.buttonColors(containerColor = Indigo500),
                  shape = RoundedCornerShape(10.dp)
                ) {
                  Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("View QR", fontSize = 12.sp)
                }
              }
            }
          }
        }
      }
    }

    // Quick Stats Cards
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        StatCard(
          title = "Assigned Subjects",
          value = "${subjects.size}",
          icon = Icons.Default.MenuBook,
          accentColor = Indigo600,
          modifier = Modifier.weight(1f)
        )
        StatCard(
          title = "Enrolled Students",
          value = "$studentsCount",
          icon = Icons.Default.People,
          accentColor = Blue600,
          modifier = Modifier.weight(1f)
        )
      }
    }

    // Today's Assigned Classes Header
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Today's Classes",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
        Text(
          text = "Monday Schedule",
          style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
      }
    }

    // List of classes for faculty
    items(subjects) { subject ->
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = subject.name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
              )
              Text(
                text = "Section: CSE-4A • ${subject.room} • 10:00 AM",
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
              )
              Text(
                text = "$studentsCount Registered Students",
                style = MaterialTheme.typography.labelSmall.copy(
                  color = Indigo600,
                  fontWeight = FontWeight.SemiBold
                )
              )
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          val activeForSubject = activeSessions.firstOrNull { it.subjectId == subject.id }
          if (activeForSubject != null) {
            Button(
              onClick = { onOpenSessionQR(activeForSubject) },
              modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .testTag("view_active_qr_session_${subject.id}"),
              colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
              shape = RoundedCornerShape(10.dp)
            ) {
              Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("View Live QR Session (Active)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
          } else {
            Button(
              onClick = { onOpenQRSelection(subject) },
              modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .testTag("start_qr_session_${subject.id}"),
              colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
              shape = RoundedCornerShape(10.dp)
            ) {
              Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Start Live QR Session", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
          }
        }
      }
    }
  }
}

// -------------------------------------------------------------
// 2. FACULTY SUBJECTS
// -------------------------------------------------------------
@Composable
private fun FacultySubjectsContent(
  subjects: List<SubjectEntity>,
  onOpenQRSelection: (SubjectEntity?) -> Unit
) {
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    item {
      Text(
        text = "Assigned Teaching Subjects",
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
      )
      Text(
        text = "Curriculum, syllabus targets, and attendance requirements.",
        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
      )
    }

    items(subjects) { sub ->
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column {
              Text(
                text = sub.name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
              )
              Text(
                text = "Subject Code: ${sub.code} • ${sub.semester}",
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
              )
            }
            Surface(
              color = Indigo50,
              shape = RoundedCornerShape(8.dp)
            ) {
              Text(
                text = sub.department,
                style = MaterialTheme.typography.labelSmall.copy(
                  color = Indigo700,
                  fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("Total Planned Classes: ${sub.totalClasses}", style = MaterialTheme.typography.bodySmall)
            Text("Min Attendance: ${sub.targetAttendancePercent.toInt()}%", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
          }

          Spacer(modifier = Modifier.height(14.dp))

          Button(
            onClick = { onOpenQRSelection(sub) },
            colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("start_session_btn_${sub.id}")
          ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Start Attendance Session")
          }
        }
      }
    }
  }
}

// -------------------------------------------------------------
// 3. FACULTY STUDENTS LIST
// -------------------------------------------------------------
@Composable
private fun FacultyStudentsContent(
  students: List<UserEntity>,
  onExportList: () -> Unit
) {
  var searchQuery by remember { mutableStateOf("") }
  val filtered = students.filter {
    it.name.contains(searchQuery, ignoreCase = true) || it.rollNo.contains(searchQuery, ignoreCase = true)
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "Class Roster",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
          )
          Text(
            text = "Section CSE-4A (${students.size} students)",
            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
          )
        }
        IconButton(onClick = onExportList) {
          Icon(Icons.Default.FileDownload, contentDescription = "Export Roster", tint = Indigo600)
        }
      }
    }

    item {
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text("Search by name or roll number...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
      )
    }

    items(filtered) { std ->
      Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Box(
            modifier = Modifier
              .size(40.dp)
              .clip(CircleShape)
              .background(Indigo100),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = std.name.take(1).uppercase(),
              fontWeight = FontWeight.Bold,
              color = Indigo700
            )
          }

          Column(modifier = Modifier.weight(1f)) {
            Text(std.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            Text(
              "${std.rollNo} • ${std.section}",
              style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
          }

          Surface(
            shape = RoundedCornerShape(6.dp),
            color = Emerald50
          ) {
            Text(
              text = "Eligible",
              style = MaterialTheme.typography.labelSmall.copy(color = Emerald600, fontWeight = FontWeight.Bold),
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }
        }
      }
    }
  }
}

// -------------------------------------------------------------
// 4. FACULTY REPORTS
// -------------------------------------------------------------
@Composable
private fun FacultyReportsContent(
  faculty: UserEntity,
  subjects: List<SubjectEntity>,
  students: List<UserEntity>,
  onGenerateReport: (String, String, String) -> Unit,
  onBroadcastDefaulters: (List<UserEntity>, String) -> Unit = { _, _ -> }
) {
  var showBroadcastDialog by remember { mutableStateOf(false) }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    item {
      Text(
        text = "Faculty Reports Hub",
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
      )
      Text(
        text = "Generate and export department-ready CSV/PDF attendance files and broadcast alerts.",
        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
      )
    }

    item {
      ReportCard(
        title = "Class Attendance Summary (CSE-4A)",
        description = "Overall attendance percentages, present/absent ratios, and low-attendance warnings for all registered students.",
        onExport = {
          val summary = "Generated by: ${faculty.name}\nDepartment: CSE\nTotal Students: ${students.size}\nAverage Class Attendance: 86.4%"
          val table = buildString {
            append(String.format("%-12s %-20s %-8s %-6s\n", "Roll No", "Student Name", "Status", "Att %"))
            append("----------------------------------------------------\n")
            students.forEachIndexed { i, s ->
              val pct = if (i % 3 == 0) 73.2f else 88.5f
              append(String.format("%-12s %-20s %-8s %.1f%%\n", s.rollNo, s.name.take(18), if (pct >= 75f) "Safe" else "Warning", pct))
            }
          }
          onGenerateReport("CSE-4A Class Attendance Summary", summary, table)
        }
      )
    }

    item {
      ReportCard(
        title = "Subject-Wise Detailed Report",
        description = "Class count breakdown for Computer Networks and assigned laboratory sessions.",
        onExport = {
          val summary = "Subject: Computer Networks (CS401)\nFaculty: ${faculty.name}\nClasses Held: 45\nEligibility Cutoff: 75%"
          val table = buildString {
            append(String.format("%-22s %-10s %-10s\n", "Metric", "Count", "Rate"))
            append("----------------------------------------------------\n")
            append(String.format("%-22s %-10d %-10s\n", "Total Classes", 45, "100%"))
            append(String.format("%-22s %-10d %-10s\n", "Present Marks", 39, "86.7%"))
            append(String.format("%-22s %-10d %-10s\n", "Absent Marks", 4, "8.9%"))
            append(String.format("%-22s %-10d %-10s\n", "Late Check-ins", 2, "4.4%"))
          }
          onGenerateReport("CS401 Subject Attendance Report", summary, table)
        }
      )
    }

    item {
      ReportCard(
        title = "Debarment & Low Attendance List",
        description = "Students falling under the 75% college mandatory threshold for academic review.",
        secondaryActionLabel = "Broadcast Alert 📢",
        onSecondaryAction = { showBroadcastDialog = true },
        onExport = {
          val summary = "Dean's Notice - Low Attendance Audit\nThreshold: < 75%\nAction: Parent notification dispatched"
          val table = buildString {
            append(String.format("%-12s %-20s %-10s\n", "Roll No", "Student", "Current %"))
            append("----------------------------------------------------\n")
            append(String.format("%-12s %-20s %-10s\n", "CSE24-004", "Ananya Sen", "71.4%"))
            append(String.format("%-12s %-20s %-10s\n", "CSE24-007", "Dev Malhotra", "68.2%"))
          }
          onGenerateReport("Debarment Warning List", summary, table)
        }
      )
    }
  }

  if (showBroadcastDialog) {
    BroadcastDefaultersDialog(
      students = students,
      onDismiss = { showBroadcastDialog = false },
      onBroadcast = { defaulters, message ->
        onBroadcastDefaulters(defaulters, message)
        showBroadcastDialog = false
      }
    )
  }
}

@Composable
private fun ReportCard(
  title: String,
  description: String,
  secondaryActionLabel: String? = null,
  onSecondaryAction: (() -> Unit)? = null,
  onExport: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        description,
        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
      )
      Spacer(modifier = Modifier.height(14.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Button(
          onClick = onExport,
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.buttonColors(containerColor = Indigo600)
        ) {
          Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("CSV / PDF", fontSize = 13.sp)
        }

        if (secondaryActionLabel != null && onSecondaryAction != null) {
          OutlinedButton(
            onClick = onSecondaryAction,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Rose700),
            border = androidx.compose.foundation.BorderStroke(1.dp, Rose300)
          ) {
            Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(16.dp), tint = Rose600)
            Spacer(modifier = Modifier.width(4.dp))
            Text(secondaryActionLabel, fontWeight = FontWeight.Bold, fontSize = 13.sp)
          }
        }
      }
    }
  }
}

@Composable
fun BroadcastDefaultersDialog(
  students: List<UserEntity>,
  onDismiss: () -> Unit,
  onBroadcast: (List<UserEntity>, String) -> Unit
) {
  // Pre-select students with simulated low attendance or first 2 students
  val initialDefaulters = remember(students) {
    if (students.size > 2) listOf(students[1], students.last()) else students
  }
  var selectedStudents by remember { mutableStateOf(initialDefaulters.toSet()) }
  var warningMessage by remember {
    mutableStateOf("URGENT: Your attendance is currently below 75%. You are at risk of being debarred from the end-term university examinations. Please meet your subject faculty immediately.")
  }

  androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 6.dp,
      modifier = Modifier
        .fillMaxWidth()
        .wrapContentHeight()
        .padding(8.dp)
    ) {
      Column(
        modifier = Modifier.padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.WarningAmber, contentDescription = null, tint = Rose600)
            Text(
              text = "Broadcast Debarment Warning",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
          }
          IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close")
          }
        }

        Text(
          text = "Select students with low attendance (< 75%) to dispatch immediate in-app warning notifications and log in Dean's audit record.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Select Students Chips
        Text(
          text = "Recipients (${selectedStudents.size} selected):",
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
        )
        androidx.compose.foundation.lazy.LazyRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          items(students.size) { index ->
            val student = students[index]
            val isSelected = selectedStudents.contains(student)
            FilterChip(
              selected = isSelected,
              onClick = {
                selectedStudents = if (isSelected) {
                  selectedStudents - student
                } else {
                  selectedStudents + student
                }
              },
              label = { Text("${student.rollNo} (${student.name.take(10)})", fontSize = 11.sp) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Rose600,
                selectedLabelColor = Color.White
              ),
              shape = RoundedCornerShape(8.dp)
            )
          }
        }

        // Message text
        OutlinedTextField(
          value = warningMessage,
          onValueChange = { warningMessage = it },
          label = { Text("Debarment Notice Text") },
          modifier = Modifier.fillMaxWidth(),
          minLines = 3,
          maxLines = 5,
          shape = RoundedCornerShape(12.dp)
        )

        // Buttons
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
            Text("Cancel")
          }
          Button(
            onClick = {
              if (selectedStudents.isNotEmpty() && warningMessage.isNotBlank()) {
                onBroadcast(selectedStudents.toList(), warningMessage.trim())
              }
            },
            enabled = selectedStudents.isNotEmpty() && warningMessage.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = Rose600),
            modifier = Modifier.weight(1.5f)
          ) {
            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Send Warning", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

// -------------------------------------------------------------
// 5. FACULTY LEAVE REQUESTS MANAGEMENT
// -------------------------------------------------------------
@Composable
fun FacultyLeaveRequestsContent(
  faculty: UserEntity,
  leaveRequests: List<LeaveRequestEntity>,
  onReviewLeave: (leaveId: String, status: String, remarks: String) -> Unit
) {
  var selectedFilter by remember { mutableStateOf("PENDING") }
  var reviewingLeave by remember { mutableStateOf<LeaveRequestEntity?>(null) }
  var reviewInitialStatus by remember { mutableStateOf("APPROVED") }
  var previewingDocumentUri by remember { mutableStateOf<String?>(null) }

  val filteredLeaves = remember(leaveRequests, selectedFilter) {
    when (selectedFilter) {
      "PENDING" -> leaveRequests.filter { it.status.equals("PENDING", ignoreCase = true) }
      "APPROVED" -> leaveRequests.filter { it.status.equals("APPROVED", ignoreCase = true) }
      "REJECTED" -> leaveRequests.filter { it.status.equals("REJECTED", ignoreCase = true) }
      else -> leaveRequests
    }
  }

  val pendingCount = leaveRequests.count { it.status.equals("PENDING", ignoreCase = true) }
  val approvedCount = leaveRequests.count { it.status.equals("APPROVED", ignoreCase = true) }
  val rejectedCount = leaveRequests.count { it.status.equals("REJECTED", ignoreCase = true) }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // Header
    item {
      Column {
        Text(
          text = "Student Leave Requests",
          style = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.5).sp
          )
        )
        Text(
          text = "Review and sanction student leave applications for classes",
          style = MaterialTheme.typography.bodySmall.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        )
      }
    }

    // Quick Stats Bar
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = Amber50,
          border = androidx.compose.foundation.BorderStroke(1.dp, Amber200),
          modifier = Modifier.weight(1f)
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text("$pendingCount", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, color = Amber700))
            Text("Pending", style = MaterialTheme.typography.labelSmall.copy(color = Amber800))
          }
        }

        Surface(
          shape = RoundedCornerShape(12.dp),
          color = Emerald50,
          border = androidx.compose.foundation.BorderStroke(1.dp, Emerald200),
          modifier = Modifier.weight(1f)
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text("$approvedCount", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, color = Emerald700))
            Text("Approved", style = MaterialTheme.typography.labelSmall.copy(color = Emerald800))
          }
        }

        Surface(
          shape = RoundedCornerShape(12.dp),
          color = Rose50,
          border = androidx.compose.foundation.BorderStroke(1.dp, Rose200),
          modifier = Modifier.weight(1f)
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text("$rejectedCount", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, color = Rose700))
            Text("Rejected", style = MaterialTheme.typography.labelSmall.copy(color = Rose800))
          }
        }
      }
    }

    // Filter Chips
    item {
      androidx.compose.foundation.lazy.LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        val filters = listOf(
          Triple("PENDING", "Pending Review", pendingCount),
          Triple("APPROVED", "Approved", approvedCount),
          Triple("REJECTED", "Rejected", rejectedCount),
          Triple("ALL", "All Applications", leaveRequests.size)
        )
        items(filters.size) { index ->
          val (key, label, count) = filters[index]
          val isSelected = selectedFilter == key
          FilterChip(
            selected = isSelected,
            onClick = { selectedFilter = key },
            label = {
              Text(
                text = "$label ($count)",
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 13.sp
              )
            },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = Indigo600,
              selectedLabelColor = Color.White
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.testTag("faculty_leave_filter_$key")
          )
        }
      }
    }

    // List of Leave Cards
    if (filteredLeaves.isEmpty()) {
      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp)
        ) {
          Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(Icons.Default.CheckCircleOutline, contentDescription = null, tint = Indigo600, modifier = Modifier.size(40.dp))
            Text(
              text = if (selectedFilter == "PENDING") "All caught up! No pending leaves." else "No leave applications found.",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Text(
              text = "Student leave applications and reason notes will appear here when submitted.",
              style = MaterialTheme.typography.bodySmall.copy(color = Slate500),
              textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
          }
        }
      }
    } else {
      items(filteredLeaves.size) { index ->
        val leave = filteredLeaves[index]
        FacultyLeaveCard(
          leave = leave,
          onQuickApprove = {
            reviewingLeave = leave
            reviewInitialStatus = "APPROVED"
          },
          onQuickReject = {
            reviewingLeave = leave
            reviewInitialStatus = "REJECTED"
          },
          onPreviewAttachment = { uri ->
            previewingDocumentUri = uri
          }
        )
      }
    }
  }

  // Review Dialog
  reviewingLeave?.let { leave ->
    FacultyReviewLeaveDialog(
      leave = leave,
      initialStatus = reviewInitialStatus,
      onDismiss = { reviewingLeave = null },
      onConfirm = { status, remarks ->
        onReviewLeave(leave.id, status, remarks)
        reviewingLeave = null
      }
    )
  }

  // Document / Medical Certificate Preview
  previewingDocumentUri?.let { uri ->
    DocumentPreviewDialog(
      attachmentUri = uri,
      onDismiss = { previewingDocumentUri = null }
    )
  }
}

@Composable
fun FacultyLeaveCard(
  leave: LeaveRequestEntity,
  onQuickApprove: () -> Unit,
  onQuickReject: () -> Unit,
  onPreviewAttachment: (String) -> Unit = {}
) {
  val (statusBg, statusBorder, statusText, statusIcon) = when (leave.status.uppercase()) {
    "APPROVED" -> QuadrupleColors(Emerald50, Emerald200, Emerald700, Icons.Default.CheckCircle)
    "REJECTED" -> QuadrupleColors(Rose50, Rose200, Rose700, Icons.Default.Cancel)
    else -> QuadrupleColors(Amber50, Amber200, Amber700, Icons.Default.HourglassEmpty)
  }

  val typeLabel = when (leave.leaveType.uppercase()) {
    "SICK_LEAVE", "SICK" -> "Sick Leave 🤒"
    "MEDICAL" -> "Medical Emergency 🏥"
    "DUTY_LEAVE", "DUTY" -> "Duty / Event 🏆"
    "CASUAL_LEAVE", "CASUAL" -> "Casual Leave 🏖️"
    else -> "Personal Leave ⚡"
  }

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("faculty_leave_card_${leave.id}")
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      // Header: Student info & status pill
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
      ) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(42.dp)
              .clip(CircleShape)
              .background(Indigo100),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = leave.studentName.take(1).uppercase(),
              fontWeight = FontWeight.Bold,
              color = Indigo700,
              fontSize = 18.sp
            )
          }

          Column {
            Text(
              text = leave.studentName,
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
              text = "${leave.studentRollNo} • ${leave.studentSection}",
              style = MaterialTheme.typography.bodySmall.copy(color = Slate500)
            )
          }
        }

        Surface(
          shape = RoundedCornerShape(20.dp),
          color = statusBg,
          border = androidx.compose.foundation.BorderStroke(1.dp, statusBorder)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Icon(statusIcon, contentDescription = null, tint = statusText, modifier = Modifier.size(13.dp))
            Text(
              text = leave.status.uppercase(),
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = statusText,
                fontSize = 11.sp
              )
            )
          }
        }
      }

      // Details Pill Row
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(10.dp))
          .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
          .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = typeLabel,
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Indigo700)
        )
        Text(
          text = "${leave.startDate} → ${leave.endDate} (${leave.numberOfDays}d)",
          style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
        )
      }

      // Student Reason Box
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(10.dp))
          .background(Color(0xFFF8FAFC))
          .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
          .padding(10.dp)
      ) {
        Text(
          text = "Student's Reason:",
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Slate500)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = leave.reason,
          style = MaterialTheme.typography.bodyMedium.copy(color = Slate800)
        )
      }

      // Medical / Duty Proof Attachment
      if (!leave.attachmentUri.isNullOrBlank()) {
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = Indigo50,
          border = androidx.compose.foundation.BorderStroke(1.dp, Indigo200),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onPreviewAttachment(leave.attachmentUri) }
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(Icons.Default.AttachFile, contentDescription = null, tint = Indigo700, modifier = Modifier.size(16.dp))
              Text(
                text = "Supporting Medical / Duty Proof",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Indigo900)
              )
            }
            Text(
              text = "Inspect 🔍",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Indigo700)
            )
          }
        }
      }

      // If already reviewed, display faculty remarks
      if (leave.facultyRemarks.isNotBlank() || leave.reviewedAt != null) {
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = if (leave.status.equals("APPROVED", ignoreCase = true)) Emerald50 else Rose50,
          border = androidx.compose.foundation.BorderStroke(1.dp, if (leave.status.equals("APPROVED", ignoreCase = true)) Emerald200 else Rose200),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Text(
              text = "Reviewer Remarks:",
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = if (leave.status.equals("APPROVED", ignoreCase = true)) Emerald800 else Rose800
              )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = leave.facultyRemarks.ifBlank { "No additional remarks" },
              style = MaterialTheme.typography.bodySmall.copy(
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                color = if (leave.status.equals("APPROVED", ignoreCase = true)) Emerald900 else Rose900
              )
            )
          }
        }
      }

      // Actions for PENDING status
      if (leave.status.equals("PENDING", ignoreCase = true)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          OutlinedButton(
            onClick = onQuickReject,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Rose600),
            border = androidx.compose.foundation.BorderStroke(1.dp, Rose300),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .weight(1f)
              .testTag("reject_leave_btn_${leave.id}")
          ) {
            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Reject", fontWeight = FontWeight.Bold)
          }

          Button(
            onClick = onQuickApprove,
            colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .weight(1f)
              .testTag("approve_leave_btn_${leave.id}")
          ) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Approve", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

@Composable
fun FacultyReviewLeaveDialog(
  leave: LeaveRequestEntity,
  initialStatus: String,
  onDismiss: () -> Unit,
  onConfirm: (status: String, remarks: String) -> Unit
) {
  var status by remember { mutableStateOf(initialStatus) }
  var remarks by remember {
    mutableStateOf(
      if (initialStatus == "APPROVED") "Approved. Please catch up with missed class materials."
      else "Cannot be approved due to attendance falling below minimum threshold."
    )
  }

  androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 6.dp,
      modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Review Leave Application",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
          )
          IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate400)
          }
        }

        // Student Info Banner
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
              text = "${leave.studentName} (${leave.studentRollNo})",
              fontWeight = FontWeight.Bold,
              style = MaterialTheme.typography.bodyMedium
            )
            Text(
              text = "Dates: ${leave.startDate} to ${leave.endDate} (${leave.numberOfDays} Days)",
              style = MaterialTheme.typography.bodySmall.copy(color = Slate600)
            )
            Text(
              text = "Reason: \"${leave.reason}\"",
              style = MaterialTheme.typography.bodySmall.copy(
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                color = Slate800
              )
            )
          }
        }

        // Supporting Document Proof (if present)
        if (!leave.attachmentUri.isNullOrBlank()) {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = Indigo50,
            border = androidx.compose.foundation.BorderStroke(1.dp, Indigo200),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.AttachFile, contentDescription = null, tint = Indigo700, modifier = Modifier.size(16.dp))
                Text(
                  text = "Attached Medical/Duty Proof:",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Indigo900)
                )
              }
              AsyncImage(
                model = leave.attachmentUri,
                contentDescription = "Medical proof",
                modifier = Modifier
                  .fillMaxWidth()
                  .height(110.dp)
                  .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
              )
            }
          }
        }

        // Decision Toggle
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          FilterChip(
            selected = status == "APPROVED",
            onClick = {
              status = "APPROVED"
              if (remarks.contains("Cannot be approved")) {
                remarks = "Approved. Please catch up with missed class materials."
              }
            },
            label = { Text("Approve ✅", fontWeight = FontWeight.Bold) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = Emerald600,
              selectedLabelColor = Color.White
            ),
            modifier = Modifier.weight(1f)
          )

          FilterChip(
            selected = status == "REJECTED",
            onClick = {
              status = "REJECTED"
              if (remarks.contains("Approved")) {
                remarks = "Cannot be approved due to attendance falling below minimum threshold."
              }
            },
            label = { Text("Reject ❌", fontWeight = FontWeight.Bold) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = Rose600,
              selectedLabelColor = Color.White
            ),
            modifier = Modifier.weight(1f)
          )
        }

        // Remarks Input
        OutlinedTextField(
          value = remarks,
          onValueChange = { remarks = it },
          label = { Text("Remarks for Student") },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("faculty_review_remarks_input"),
          minLines = 2,
          maxLines = 4,
          shape = RoundedCornerShape(12.dp)
        )

        // Actions
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          OutlinedButton(
            onClick = onDismiss,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f)
          ) {
            Text("Cancel")
          }

          Button(
            onClick = { onConfirm(status, remarks) },
            colors = ButtonDefaults.buttonColors(
              containerColor = if (status == "APPROVED") Emerald600 else Rose600
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .weight(1.3f)
              .testTag("confirm_review_btn")
          ) {
            Text(
              text = if (status == "APPROVED") "Confirm Approval" else "Confirm Rejection",
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }
  }
}

data class QuadrupleColors(
  val first: Color,
  val second: Color,
  val third: Color,
  val fourth: androidx.compose.ui.graphics.vector.ImageVector
)

