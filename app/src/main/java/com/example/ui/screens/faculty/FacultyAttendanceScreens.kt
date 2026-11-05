package com.example.ui.screens.faculty

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttendanceRecordEntity
import com.example.data.model.AttendanceSessionEntity
import com.example.data.model.SubjectEntity
import com.example.data.model.UserEntity
import com.example.ui.components.QRMatrixView
import com.example.ui.theme.*
import com.example.ui.util.QRCodeGenerator
import kotlinx.coroutines.delay

// -------------------------------------------------------------
// 1. MANUAL ATTENDANCE MARKING SCREEN
// -------------------------------------------------------------
@Composable
fun FacultyMarkAttendanceScreen(
  session: AttendanceSessionEntity,
  students: List<UserEntity>,
  onBack: () -> Unit,
  onSave: (AttendanceSessionEntity, List<Pair<UserEntity, String>>) -> Unit
) {
  // Map of studentId to status ("PRESENT", "ABSENT", "LATE")
  val statusMap = remember {
    mutableStateMapOf<String, String>().apply {
      students.forEach { std ->
        put(std.id, "PRESENT") // Default to Present
      }
    }
  }

  var searchQuery by remember { mutableStateOf("") }
  var statusFilter by remember { mutableStateOf("ALL") }
  var isSavedSuccessfully by remember { mutableStateOf(false) }

  val filteredStudents = students.filter { std ->
    val matchesSearch = std.name.contains(searchQuery, ignoreCase = true) ||
      std.rollNo.contains(searchQuery, ignoreCase = true)
    val status = statusMap[std.id] ?: "PRESENT"
    val matchesStatus = when (statusFilter) {
      "PRESENT" -> status == "PRESENT"
      "ABSENT" -> status == "ABSENT"
      "LATE" -> status == "LATE"
      else -> true
    }
    matchesSearch && matchesStatus
  }

  Scaffold(
    topBar = {
      Surface(tonalElevation = 2.dp, color = MaterialTheme.colorScheme.surface) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            IconButton(onClick = onBack) {
              Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Take Attendance",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
              )
              Text(
                text = "${session.subjectName} • ${session.section}",
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
              )
            }
          }

          // Search Bar
          OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search student name or roll number...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 4.dp),
            singleLine = true
          )

          // Batch Action Buttons: Present All / Absent All
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = {
                students.forEach { statusMap[it.id] = "PRESENT" }
              },
              shape = RoundedCornerShape(8.dp),
              colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
              modifier = Modifier
                .weight(1f)
                .testTag("present_all_button")
            ) {
              Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Present All", fontSize = 12.sp)
            }

            OutlinedButton(
              onClick = {
                students.forEach { statusMap[it.id] = "ABSENT" }
              },
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier
                .weight(1f)
                .testTag("absent_all_button")
            ) {
              Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Absent All", fontSize = 12.sp)
            }
          }
        }
      }
    },
    bottomBar = {
      Surface(
        tonalElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .navigationBarsPadding()
            .padding(16.dp)
        ) {
          // Live Counts
          val presentCount = statusMap.values.count { it == "PRESENT" }
          val absentCount = statusMap.values.count { it == "ABSENT" }
          val lateCount = statusMap.values.count { it == "LATE" }

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround
          ) {
            Text("Present: $presentCount", fontWeight = FontWeight.Bold, color = Emerald600, fontSize = 13.sp)
            Text("Absent: $absentCount", fontWeight = FontWeight.Bold, color = Rose600, fontSize = 13.sp)
            Text("Late: $lateCount", fontWeight = FontWeight.Bold, color = Amber600, fontSize = 13.sp)
          }

          Button(
            onClick = {
              val list = students.map { std ->
                Pair(std, statusMap[std.id] ?: "PRESENT")
              }
              onSave(session, list)
              isSavedSuccessfully = true
            },
            colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(52.dp)
              .testTag("save_attendance_button")
          ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Save Attendance Record", fontWeight = FontWeight.Bold, fontSize = 15.sp)
          }
        }
      }
    }
  ) { padding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      if (isSavedSuccessfully) {
        item {
          Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Emerald50)
          ) {
            Row(
              modifier = Modifier.padding(14.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Emerald600)
              Text("Attendance successfully recorded ✅", color = Emerald600, fontWeight = FontWeight.Bold)
            }
          }
        }
      }

      items(filteredStudents) { student ->
        val currentStatus = statusMap[student.id] ?: "PRESENT"

        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Column {
                Text(
                  text = student.name,
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                  text = "${student.rollNo} • Section ${student.section}",
                  style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Radio Pills for Present, Absent, Late
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              StatusPillButton(
                label = "Present",
                isSelected = currentStatus == "PRESENT",
                activeColor = Emerald600,
                modifier = Modifier.weight(1f),
                onClick = { statusMap[student.id] = "PRESENT" }
              )
              StatusPillButton(
                label = "Absent",
                isSelected = currentStatus == "ABSENT",
                activeColor = Rose600,
                modifier = Modifier.weight(1f),
                onClick = { statusMap[student.id] = "ABSENT" }
              )
              StatusPillButton(
                label = "Late",
                isSelected = currentStatus == "LATE",
                activeColor = Amber600,
                modifier = Modifier.weight(1f),
                onClick = { statusMap[student.id] = "LATE" }
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun StatusPillButton(
  label: String,
  isSelected: Boolean,
  activeColor: Color,
  modifier: Modifier = Modifier,
  onClick: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = if (isSelected) activeColor else MaterialTheme.colorScheme.surfaceVariant,
    contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
    modifier = modifier
      .height(36.dp)
      .clickable { onClick() }
  ) {
    Box(contentAlignment = Alignment.Center) {
      Text(
        text = label,
        fontSize = 12.sp,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
      )
    }
  }
}

// -------------------------------------------------------------
// 2. FACULTY SMART QR CODE ATTENDANCE SESSION
// -------------------------------------------------------------
@Composable
fun FacultyQRSessionScreen(
  session: AttendanceSessionEntity,
  allActiveSessions: List<AttendanceSessionEntity> = emptyList(),
  subjects: List<SubjectEntity> = emptyList(),
  attendees: List<AttendanceRecordEntity> = emptyList(),
  onBack: () -> Unit,
  onCloseSession: () -> Unit,
  onSelectSession: (AttendanceSessionEntity) -> Unit = {},
  onStartSession: ((SubjectEntity, String) -> Unit)? = null
) {
  var currentDisplayedSession by remember(session) { mutableStateOf(session) }
  var showChangeSubjectDialog by remember { mutableStateOf(false) }

  // Session countdown timer (10 minutes)
  var secondsRemaining by remember { mutableStateOf(600) }

  LaunchedEffect(key1 = secondsRemaining) {
    if (secondsRemaining > 0) {
      delay(1000L)
      secondsRemaining -= 1
    }
  }

  val minutes = secondsRemaining / 60
  val seconds = secondsRemaining % 60
  val formattedTimer = String.format("%02d:%02d", minutes, seconds)

  // Real attendees for this active session
  val currentSessionAttendees = remember(attendees, currentDisplayedSession.id) {
    attendees.filter { it.sessionId == currentDisplayedSession.id }
  }

  if (showChangeSubjectDialog && subjects.isNotEmpty()) {
    GenerateQRSubjectSelectionDialog(
      subjects = subjects,
      initialSubject = subjects.firstOrNull { it.id == currentDisplayedSession.subjectId } ?: subjects.firstOrNull(),
      onDismiss = { showChangeSubjectDialog = false },
      onConfirm = { chosenSubject, chosenSection ->
        showChangeSubjectDialog = false
        onStartSession?.invoke(chosenSubject, chosenSection)
      }
    )
  }

  Scaffold(
    topBar = {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .statusBarsPadding()
          .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(onClick = onBack) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        Text(
          text = "Smart QR Session",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          if (subjects.isNotEmpty() && onStartSession != null) {
            FilledTonalButton(
              onClick = { showChangeSubjectDialog = true },
              shape = RoundedCornerShape(8.dp),
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
              colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = Indigo100,
                contentColor = Indigo700
              ),
              modifier = Modifier.testTag("switch_qr_subject_btn")
            ) {
              Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Switch Subject", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = Emerald50
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(6.dp)
                  .clip(CircleShape)
                  .background(Emerald500)
              )
              Text("LIVE QR", style = MaterialTheme.typography.labelSmall.copy(color = Emerald600, fontWeight = FontWeight.Bold))
            }
          }
        }
      }
    },
    bottomBar = {
      Surface(
        tonalElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
      ) {
        Box(
          modifier = Modifier
            .navigationBarsPadding()
            .padding(16.dp)
        ) {
          Button(
            onClick = onCloseSession,
            colors = ButtonDefaults.buttonColors(containerColor = Rose600),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(52.dp)
              .testTag("close_session_button")
          ) {
            Icon(Icons.Default.StopCircle, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Close Attendance Session", fontWeight = FontWeight.Bold, fontSize = 15.sp)
          }
        }
      }
    }
  ) { padding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding),
      contentPadding = PaddingValues(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      // Multi-subject selector chips for Faculty if multiple active sessions exist
      if (allActiveSessions.size > 1) {
        item {
          Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Text(
              text = "Switch Subject QR:",
              style = MaterialTheme.typography.labelSmall.copy(color = Slate600, fontWeight = FontWeight.Bold)
            )
            androidx.compose.foundation.lazy.LazyRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              items(allActiveSessions) { sess ->
                val isSelected = sess.id == currentDisplayedSession.id
                FilterChip(
                  selected = isSelected,
                  onClick = {
                    currentDisplayedSession = sess
                    onSelectSession(sess)
                  },
                  label = {
                    Text(
                      text = sess.subjectName,
                      fontSize = 11.sp,
                      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                  },
                  leadingIcon = {
                    Icon(
                      imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.QrCode,
                      contentDescription = null,
                      modifier = Modifier.size(14.dp)
                    )
                  },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Indigo600,
                    selectedLabelColor = Color.White,
                    selectedLeadingIconColor = Color.White
                  )
                )
              }
            }
          }
        }
      }

      // Session Info Header
      item {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            text = currentDisplayedSession.subjectName,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
            textAlign = TextAlign.Center
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "Teacher: ${currentDisplayedSession.facultyName} • Section: ${currentDisplayedSession.section}",
            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
          )
          Text(
            text = "Room: ${currentDisplayedSession.room} • Code: ${currentDisplayedSession.id}",
            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
          )
        }
      }

      // Dynamic QR Matrix
      item {
        val qrPayload = remember(currentDisplayedSession.id, currentDisplayedSession.qrToken) {
          QRCodeGenerator.buildPayload(
            sessionId = currentDisplayedSession.id,
            subjectId = currentDisplayedSession.subjectId,
            subjectName = currentDisplayedSession.subjectName,
            qrToken = currentDisplayedSession.qrToken,
            facultyId = currentDisplayedSession.facultyId,
            section = currentDisplayedSession.section,
            room = currentDisplayedSession.room
          )
        }
        QRMatrixView(
          token = qrPayload,
          size = 220.dp
        )
      }

      // Session Code Badge & Timer
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
          ) {
            Text(
              text = "Token: ${currentDisplayedSession.qrToken}",
              style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                fontWeight = FontWeight.Bold
              ),
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
          }

          Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (secondsRemaining < 60) Rose50 else Indigo50
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Icon(
                Icons.Default.Timer,
                contentDescription = null,
                tint = if (secondsRemaining < 60) Rose600 else Indigo600,
                modifier = Modifier.size(14.dp)
              )
              Text(
                text = "Timer: $formattedTimer",
                style = MaterialTheme.typography.labelMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = if (secondsRemaining < 60) Rose600 else Indigo600
                )
              )
            }
          }
        }
      }

      // Scanned Students Section Header
      item {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(Icons.Default.People, contentDescription = null, tint = Indigo600)
            Text(
              text = "Live Scanned Students",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
          }

          Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (currentSessionAttendees.isNotEmpty()) Emerald50 else Slate100,
            border = androidx.compose.foundation.BorderStroke(
              1.dp,
              if (currentSessionAttendees.isNotEmpty()) Emerald200 else Slate200
            )
          ) {
            Text(
              text = "${currentSessionAttendees.size} Scanned",
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = if (currentSessionAttendees.isNotEmpty()) Emerald700 else Slate600
              ),
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
          }
        }
      }

      // Empty State: Waiting for students to scan
      if (currentSessionAttendees.isEmpty()) {
        item {
          Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            modifier = Modifier.fillMaxWidth().testTag("empty_scanned_students_card")
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(44.dp)
                  .clip(CircleShape)
                  .background(Indigo100),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  Icons.Default.HourglassEmpty,
                  contentDescription = null,
                  tint = Indigo600,
                  modifier = Modifier.size(24.dp)
                )
              }
              Text(
                text = "Waiting for students to scan...",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
              )
              Text(
                text = "Students who scan the QR code above will appear here in real time with their name, roll number, and timestamp.",
                style = MaterialTheme.typography.bodySmall.copy(color = Slate600),
                textAlign = TextAlign.Center
              )
            }
          }
        }
      } else {
        // Real-time scanned students list
        items(currentSessionAttendees, key = { it.id }) { record ->
          Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("scanned_student_${record.studentRollNo}")
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
                  .size(38.dp)
                  .clip(CircleShape)
                  .background(Emerald100),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = record.studentName.take(1).uppercase(),
                  style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Emerald700
                  )
                )
              }

              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = record.studentName,
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                  text = "Roll: ${record.studentRollNo} • ${record.section}",
                  style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                Text(
                  text = "Scanned at ${record.time}",
                  style = MaterialTheme.typography.labelSmall.copy(color = Slate500, fontSize = 10.sp)
                )
              }

              Surface(
                shape = RoundedCornerShape(8.dp),
                color = Emerald50,
                border = androidx.compose.foundation.BorderStroke(1.dp, Emerald200)
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Emerald600,
                    modifier = Modifier.size(12.dp)
                  )
                  Text(
                    text = "Present ✅",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontWeight = FontWeight.Bold,
                      color = Emerald700
                    )
                  )
                }
              }
            }
          }
        }
      }

      item {
        Spacer(modifier = Modifier.height(24.dp))
      }
    }
  }
}

// -------------------------------------------------------------
// 3. FACULTY SUBJECT SELECTION MODAL FOR QR CODE GENERATION
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerateQRSubjectSelectionDialog(
  subjects: List<SubjectEntity>,
  initialSubject: SubjectEntity? = null,
  onDismiss: () -> Unit,
  onConfirm: (SubjectEntity, String) -> Unit
) {
  var selectedSubject by remember(initialSubject, subjects) {
    mutableStateOf(initialSubject ?: subjects.firstOrNull())
  }
  var selectedSection by remember { mutableStateOf("CSE-4A") }
  var validityMinutes by remember { mutableStateOf(10) }
  var isDropdownExpanded by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
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
            Icons.Default.QrCodeScanner,
            contentDescription = null,
            tint = Indigo700,
            modifier = Modifier.size(24.dp)
          )
        }
        Column {
          Text(
            text = "Generate Attendance QR",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
          )
          Text(
            text = "Choose subject to initiate live session",
            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
          )
        }
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        // Dropdown selection for subjects
        Text(
          text = "Select Teaching Subject:",
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Slate700)
        )

        ExposedDropdownMenuBox(
          expanded = isDropdownExpanded,
          onExpandedChange = { isDropdownExpanded = it },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("subject_dropdown_selector")
        ) {
          OutlinedTextField(
            value = selectedSubject?.let { "${it.code} - ${it.name}" } ?: "Choose Subject",
            onValueChange = {},
            readOnly = true,
            label = { Text("Subject *") },
            trailingIcon = {
              ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded)
            },
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = Indigo600,
              focusedLabelColor = Indigo600,
              focusedContainerColor = MaterialTheme.colorScheme.surface,
              unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .menuAnchor()
              .fillMaxWidth()
              .testTag("subject_dropdown_field")
          )

          ExposedDropdownMenu(
            expanded = isDropdownExpanded,
            onDismissRequest = { isDropdownExpanded = false },
            modifier = Modifier.testTag("subject_dropdown_menu")
          ) {
            subjects.forEach { sub ->
              val isCurrent = sub.id == selectedSubject?.id
              DropdownMenuItem(
                text = {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                      ) {
                        Surface(
                          color = if (isCurrent) Indigo600 else Indigo50,
                          shape = RoundedCornerShape(4.dp)
                        ) {
                          Text(
                            text = sub.code,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCurrent) Color.White else Indigo800,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                          )
                        }
                        Text(
                          text = sub.name,
                          style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                          )
                        )
                      }
                      Text(
                        text = "${sub.department} • Room ${sub.room}",
                        style = MaterialTheme.typography.bodySmall.copy(color = Slate500),
                        modifier = Modifier.padding(top = 2.dp)
                      )
                    }
                    if (isCurrent) {
                      Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = Indigo600,
                        modifier = Modifier.size(18.dp)
                      )
                    }
                  }
                },
                onClick = {
                  selectedSubject = sub
                  isDropdownExpanded = false
                },
                modifier = Modifier.testTag("dropdown_item_${sub.id}")
              )
            }
          }
        }

        // Details summary card for selected subject
        selectedSubject?.let { sub ->
          Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Indigo50),
            border = androidx.compose.foundation.BorderStroke(1.dp, Indigo200),
            modifier = Modifier.fillMaxWidth().testTag("selected_subject_preview_card")
          ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = sub.name,
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Indigo900)
                )
                Surface(color = Indigo200, shape = RoundedCornerShape(6.dp)) {
                  Text(
                    text = sub.code,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Indigo900,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
              }
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = "Assigned Room: ${sub.room}",
                  style = MaterialTheme.typography.bodySmall.copy(color = Indigo900)
                )
                Text(
                  text = "Min Target: ${sub.targetAttendancePercent.toInt()}%",
                  style = MaterialTheme.typography.bodySmall.copy(color = Indigo900, fontWeight = FontWeight.Bold)
                )
              }
            }
          }
        }

        // Class Section selector
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = "Class Section:",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Slate700)
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            listOf("CSE-4A", "CSE-4B", "CSE-4C").forEach { section ->
              val isSelected = selectedSection == section
              FilterChip(
                selected = isSelected,
                onClick = { selectedSection = section },
                label = { Text(section, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = Indigo600,
                  selectedLabelColor = Color.White
                ),
                modifier = Modifier.testTag("section_chip_$section")
              )
            }
          }
        }

        // Duration selector
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = "QR Session Validity:",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Slate700)
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            listOf(5, 10, 15, 30).forEach { mins ->
              val isSelected = validityMinutes == mins
              FilterChip(
                selected = isSelected,
                onClick = { validityMinutes = mins },
                label = { Text("${mins}m", fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = Indigo600,
                  selectedLabelColor = Color.White
                )
              )
            }
          }
        }

        // Guarantee / Assurance notice
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = Emerald50,
          border = androidx.compose.foundation.BorderStroke(1.dp, Emerald200),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(Icons.Default.Verified, contentDescription = null, tint = Emerald700, modifier = Modifier.size(16.dp))
            Text(
              text = "Attendance will be uniquely recorded for ${selectedSubject?.code ?: "selected subject"}. Student scans verify against this specific subject session.",
              style = MaterialTheme.typography.bodySmall.copy(color = Emerald900, fontSize = 11.sp)
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          selectedSubject?.let { onConfirm(it, selectedSection) }
        },
        enabled = selectedSubject != null,
        colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.testTag("generate_qr_confirm_btn")
      ) {
        Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Generate QR Code", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.testTag("cancel_generate_qr_btn")
      ) {
        Text("Cancel", color = Slate600)
      }
    }
  )
}

