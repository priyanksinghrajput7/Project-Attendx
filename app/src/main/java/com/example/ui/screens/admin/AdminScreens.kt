package com.example.ui.screens.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AuditLogEntity
import com.example.data.model.DepartmentEntity
import com.example.data.model.NoticeEntity
import com.example.data.model.SubjectEntity
import com.example.data.model.TimetableEntity
import com.example.data.model.UserEntity
import com.example.ui.components.StatCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.AttendanceRequirementPolicy
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@Composable
fun AdminMainScreen(
  admin: UserEntity,
  students: List<UserEntity>,
  facultyList: List<UserEntity>,
  departments: List<DepartmentEntity>,
  subjects: List<SubjectEntity>,
  timetables: List<TimetableEntity>,
  auditLogs: List<AuditLogEntity>,
  attendancePolicy: AttendanceRequirementPolicy = AttendanceRequirementPolicy(),
  notices: List<NoticeEntity> = emptyList(),
  onOpenNoticeBoard: () -> Unit = {},
  onToggleUserActive: (UserEntity) -> Unit,
  onDeleteUser: (UserEntity) -> Unit,
  onAddOrUpdateUser: (UserEntity, Boolean) -> Unit,
  onAddOrUpdateSubject: (SubjectEntity, Boolean) -> Unit,
  onDeleteSubject: (SubjectEntity) -> Unit,
  onAddOrUpdateDepartment: (DepartmentEntity, Boolean) -> Unit,
  onDeleteDepartment: (DepartmentEntity) -> Unit,
  onAddOrUpdateTimetable: (TimetableEntity, Boolean) -> Unit,
  onDeleteTimetable: (TimetableEntity) -> Unit,
  onUpdateAttendancePolicy: (AttendanceRequirementPolicy) -> Unit = {},
  onOpenReport: (String, String, String) -> Unit
) {
  var selectedTab by remember { mutableStateOf(0) }

  Scaffold(
    bottomBar = {
      NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
      ) {
        NavigationBarItem(
          selected = selectedTab == 0,
          onClick = { selectedTab = 0 },
          icon = { Icon(Icons.Default.Analytics, contentDescription = "Analytics") },
          label = { Text("Analytics") },
          modifier = Modifier.testTag("admin_nav_analytics")
        )
        NavigationBarItem(
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          icon = { Icon(Icons.Default.School, contentDescription = "Students") },
          label = { Text("Students") },
          modifier = Modifier.testTag("admin_nav_students")
        )
        NavigationBarItem(
          selected = selectedTab == 2,
          onClick = { selectedTab = 2 },
          icon = { Icon(Icons.Default.CastForEducation, contentDescription = "Faculty") },
          label = { Text("Faculty") },
          modifier = Modifier.testTag("admin_nav_faculty")
        )
        NavigationBarItem(
          selected = selectedTab == 3,
          onClick = { selectedTab = 3 },
          icon = { Icon(Icons.Default.Domain, contentDescription = "Academics") },
          label = { Text("Academics") },
          modifier = Modifier.testTag("admin_nav_academics")
        )
        NavigationBarItem(
          selected = selectedTab == 4,
          onClick = { selectedTab = 4 },
          icon = { Icon(Icons.Default.Tune, contentDescription = "Requirements & Policy") },
          label = { Text("Policy & Logs") },
          modifier = Modifier.testTag("admin_nav_policy_logs")
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
        0 -> AdminAnalyticsContent(
          students = students,
          facultyList = facultyList,
          departments = departments,
          subjects = subjects,
          policy = attendancePolicy,
          notices = notices,
          onOpenNoticeBoard = onOpenNoticeBoard,
          onGenerateReport = onOpenReport
        )
        1 -> AdminStudentsContent(
          students = students,
          departments = departments,
          onToggleActive = onToggleUserActive,
          onDelete = onDeleteUser,
          onAddOrUpdate = onAddOrUpdateUser
        )
        2 -> AdminFacultyContent(
          facultyList = facultyList,
          subjects = subjects,
          departments = departments,
          onToggleActive = onToggleUserActive,
          onDelete = onDeleteUser,
          onAddOrUpdate = onAddOrUpdateUser
        )
        3 -> AdminAcademicsContent(
          departments = departments,
          subjects = subjects,
          timetables = timetables,
          facultyList = facultyList,
          onAddOrUpdateDepartment = onAddOrUpdateDepartment,
          onDeleteDepartment = onDeleteDepartment,
          onAddOrUpdateSubject = onAddOrUpdateSubject,
          onDeleteSubject = onDeleteSubject,
          onAddOrUpdateTimetable = onAddOrUpdateTimetable,
          onDeleteTimetable = onDeleteTimetable
        )
        4 -> AdminRequirementsAndAuditContent(
          auditLogs = auditLogs,
          policy = attendancePolicy,
          onUpdatePolicy = onUpdateAttendancePolicy,
          onExportLogs = {
            val summary = "College Security & Attendance Audit Log Trail\nTotal Recorded Events: ${auditLogs.size}"
            val table = buildString {
              append(String.format("%-18s %-12s %-24s %s\n", "Actor", "Role", "Action", "Details"))
              append("------------------------------------------------------------------------------------\n")
              auditLogs.forEach { l ->
                append(String.format("%-18s %-12s %-24s %s\n", l.actorName.take(16), l.actorRole, l.action.take(22), l.details))
              }
            }
            onOpenReport("Audit Trail Log Report", summary, table)
          }
        )
      }
    }
  }
}

// -------------------------------------------------------------
// 1. ADMIN ANALYTICS CONTENT
// -------------------------------------------------------------
@Composable
private fun AdminAnalyticsContent(
  students: List<UserEntity>,
  facultyList: List<UserEntity>,
  departments: List<DepartmentEntity>,
  subjects: List<SubjectEntity>,
  policy: AttendanceRequirementPolicy,
  notices: List<NoticeEntity> = emptyList(),
  onOpenNoticeBoard: () -> Unit = {},
  onGenerateReport: (String, String, String) -> Unit
) {
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "Campus Analytics 📊",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
          )
          Text(
            text = "Apex Institute of Technology & Science",
            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
          )
        }

        Button(
          onClick = {
            val summary = "College-Wide Attendance & Academic Status\nAcademic Term: ${policy.academicTerm}\nMandatory Minimum Requirement: ${policy.minAttendancePercent}%\nOverall Campus Attendance: 87.2%"
            val table = buildString {
              append(String.format("%-22s %-12s %-10s\n", "Department", "Students", "Attendance %"))
              append("----------------------------------------------------\n")
              departments.forEach { d ->
                append(String.format("%-22s %-12d %-10s\n", d.name.take(20), d.studentCount, "87.4%"))
              }
            }
            onGenerateReport("College Accreditation Attendance Report", summary, table)
          },
          shape = RoundedCornerShape(8.dp),
          colors = ButtonDefaults.buttonColors(containerColor = Indigo600)
        ) {
          Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Export", fontSize = 12.sp)
        }
      }
    }

    // Campus Notice Board Broadcast & Management Card
    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, Indigo200),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onOpenNoticeBoard() }
          .testTag("admin_notice_board_card")
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
                  .size(42.dp)
                  .clip(RoundedCornerShape(10.dp))
                  .background(Indigo100),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  Icons.Default.Campaign,
                  contentDescription = null,
                  tint = Indigo700,
                  modifier = Modifier.size(24.dp)
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
                    color = Emerald100
                  ) {
                    Text(
                      text = "${notices.size} Live Circulars",
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold,
                      color = Emerald800,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                  }
                }
                Text(
                  text = "Broadcast circulars, exams & alerts to students/faculty",
                  style = MaterialTheme.typography.bodySmall.copy(color = Slate500)
                )
              }
            }

            Button(
              onClick = onOpenNoticeBoard,
              colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
              shape = RoundedCornerShape(8.dp),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Post Notice 📢", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    // Top Metric Cards Grid
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        StatCard(
          title = "Total Students",
          value = "${students.size}",
          subtitle = "Active Enrollment",
          icon = Icons.Default.People,
          accentColor = Indigo600,
          modifier = Modifier.weight(1f)
        )
        StatCard(
          title = "Faculty",
          value = "${facultyList.size}",
          subtitle = "Full-Time Teaching",
          icon = Icons.Default.CastForEducation,
          accentColor = Blue600,
          modifier = Modifier.weight(1f)
        )
      }
    }

    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        StatCard(
          title = "Avg Attendance",
          value = "87.2%",
          subtitle = "Above ${policy.minAttendancePercent}% Target",
          icon = Icons.Default.CheckCircle,
          accentColor = Emerald600,
          modifier = Modifier.weight(1f)
        )
        StatCard(
          title = "Departments",
          value = "${departments.size}",
          subtitle = "Academic Programs",
          icon = Icons.Default.Domain,
          accentColor = Indigo500,
          modifier = Modifier.weight(1f)
        )
      }
    }

    // College Requirements Summary Banner
    item {
      Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Indigo50)
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Icon(Icons.Default.Rule, contentDescription = null, tint = Indigo600, modifier = Modifier.size(18.dp))
              Text("Active College Attendance Criteria", fontWeight = FontWeight.Bold, color = Indigo700)
            }
            Surface(shape = RoundedCornerShape(6.dp), color = Indigo600) {
              Text(
                text = "${policy.minAttendancePercent.toInt()}% Min Target",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }
          Text(
            text = "Term: ${policy.academicTerm} • Warning Alert at ${policy.warningThresholdPercent.toInt()}% • Condonation: ${policy.condonationLimitPercent.toInt()}%",
            style = MaterialTheme.typography.bodySmall.copy(color = Indigo900)
          )
        }
      }
    }

    // Department Breakdown
    item {
      Text(
        text = "Department Breakdown",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
      )
    }

    items(departments) { dept ->
      Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(dept.name, fontWeight = FontWeight.Bold)
              Text("HOD: ${dept.headOfDepartment} • Code: ${dept.code}", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
            }
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = Emerald50
            ) {
              Text(
                text = "${dept.studentCount} Students",
                style = MaterialTheme.typography.labelSmall.copy(color = Emerald600, fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))
          LinearProgressIndicator(
            progress = { 0.88f },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = Emerald600,
            trackColor = Slate100,
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text("88% Average Class Turnout", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
        }
      }
    }

    // Defaulters List (< 75%)
    item {
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Rose50)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Icon(Icons.Default.Warning, contentDescription = null, tint = Rose600)
              Text(
                text = "Low Attendance Defaulters (< ${policy.minAttendancePercent.toInt()}%)",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Rose600)
              )
            }
            Text("2 Students", style = MaterialTheme.typography.labelSmall.copy(color = Rose600, fontWeight = FontWeight.Bold))
          }

          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "Ananya Sen (CSE24-004) - 71.4% • Dev Malhotra (CSE24-007) - 68.2%",
            style = MaterialTheme.typography.bodySmall.copy(color = Slate800)
          )
        }
      }
    }
  }
}

// -------------------------------------------------------------
// 2. ADMIN STUDENTS MANAGEMENT (ADD & EDIT)
// -------------------------------------------------------------
@Composable
private fun AdminStudentsContent(
  students: List<UserEntity>,
  departments: List<DepartmentEntity>,
  onToggleActive: (UserEntity) -> Unit,
  onDelete: (UserEntity) -> Unit,
  onAddOrUpdate: (UserEntity, Boolean) -> Unit
) {
  var showAddDialog by remember { mutableStateOf(false) }
  var editingStudent by remember { mutableStateOf<UserEntity?>(null) }
  var studentToDelete by remember { mutableStateOf<UserEntity?>(null) }
  var searchQuery by remember { mutableStateOf("") }

  val filtered = students.filter {
    it.name.contains(searchQuery, ignoreCase = true) ||
      it.rollNo.contains(searchQuery, ignoreCase = true) ||
      it.email.contains(searchQuery, ignoreCase = true) ||
      it.section.contains(searchQuery, ignoreCase = true)
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "Manage Students",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
          )
          Text(
            text = "${students.size} registered student profiles • Add & Edit anytime",
            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
          )
        }

        Button(
          onClick = { showAddDialog = true },
          colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.testTag("admin_add_student_button")
        ) {
          Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Add Student", fontSize = 12.sp)
        }
      }
    }

    item {
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text("Search by name, roll no, or section...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
      )
    }

    items(filtered, key = { it.id }) { std ->
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
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              modifier = Modifier.weight(1f)
            ) {
              Box(
                modifier = Modifier
                  .size(40.dp)
                  .clip(CircleShape)
                  .background(if (std.isActive) Indigo100 else Slate200),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = std.name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString(""),
                  fontWeight = FontWeight.Bold,
                  color = if (std.isActive) Indigo700 else Slate600
                )
              }

              Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  Text(std.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (std.isActive) Emerald50 else Rose50
                  ) {
                    Text(
                      text = if (std.isActive) "Active" else "Deactivated",
                      style = MaterialTheme.typography.labelSmall.copy(
                        color = if (std.isActive) Emerald600 else Rose600,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                      ),
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                  }
                }
                Text(
                  text = "Roll: ${std.rollNo} • Reg: ${std.regNo}",
                  style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                Text(
                  text = "${std.course} • ${std.section} • ${std.semester}",
                  style = MaterialTheme.typography.labelSmall.copy(color = Indigo600)
                )
                Text(
                  text = "${std.email} • ${std.phone}",
                  style = MaterialTheme.typography.labelSmall.copy(color = Slate500, fontSize = 11.sp)
                )
              }
            }

            // Action Buttons: Edit, Toggle Active, Delete
            Row(verticalAlignment = Alignment.CenterVertically) {
              IconButton(
                onClick = { editingStudent = std },
                modifier = Modifier.size(36.dp)
              ) {
                Icon(
                  Icons.Default.Edit,
                  contentDescription = "Edit Student",
                  tint = Indigo600,
                  modifier = Modifier.size(18.dp)
                )
              }
              IconButton(
                onClick = { onToggleActive(std) },
                modifier = Modifier.size(36.dp)
              ) {
                Icon(
                  imageVector = if (std.isActive) Icons.Default.Block else Icons.Default.CheckCircle,
                  contentDescription = "Toggle Active",
                  tint = if (std.isActive) Amber600 else Emerald600,
                  modifier = Modifier.size(18.dp)
                )
              }
              IconButton(
                onClick = { studentToDelete = std },
                modifier = Modifier.size(36.dp)
              ) {
                Icon(
                  Icons.Default.Delete,
                  contentDescription = "Delete",
                  tint = Rose600,
                  modifier = Modifier.size(18.dp)
                )
              }
            }
          }
        }
      }
    }
  }

  // Add Dialog
  if (showAddDialog) {
    AddOrEditStudentDialog(
      student = null,
      departments = departments,
      onDismiss = { showAddDialog = false },
      onSave = { newStd ->
        onAddOrUpdate(newStd, false)
        showAddDialog = false
      }
    )
  }

  // Edit Dialog
  editingStudent?.let { std ->
    AddOrEditStudentDialog(
      student = std,
      departments = departments,
      onDismiss = { editingStudent = null },
      onSave = { updatedStd ->
        onAddOrUpdate(updatedStd, true)
        editingStudent = null
      }
    )
  }

  // Delete Confirmation
  studentToDelete?.let { std ->
    AlertDialog(
      onDismissRequest = { studentToDelete = null },
      title = { Text("Delete Student Profile") },
      text = { Text("Are you sure you want to delete ${std.name} (${std.rollNo})? This will permanently remove their records.") },
      confirmButton = {
        Button(
          onClick = {
            onDelete(std)
            studentToDelete = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = Rose600)
        ) {
          Text("Delete")
        }
      },
      dismissButton = {
        TextButton(onClick = { studentToDelete = null }) { Text("Cancel") }
      }
    )
  }
}

// -------------------------------------------------------------
// 3. ADMIN FACULTY MANAGEMENT (ADD & EDIT)
// -------------------------------------------------------------
@Composable
private fun AdminFacultyContent(
  facultyList: List<UserEntity>,
  subjects: List<SubjectEntity>,
  departments: List<DepartmentEntity>,
  onToggleActive: (UserEntity) -> Unit,
  onDelete: (UserEntity) -> Unit,
  onAddOrUpdate: (UserEntity, Boolean) -> Unit
) {
  var showAddDialog by remember { mutableStateOf(false) }
  var editingFaculty by remember { mutableStateOf<UserEntity?>(null) }
  var facultyToDelete by remember { mutableStateOf<UserEntity?>(null) }
  var searchQuery by remember { mutableStateOf("") }

  val filtered = facultyList.filter {
    it.name.contains(searchQuery, ignoreCase = true) ||
      it.employeeId.contains(searchQuery, ignoreCase = true) ||
      it.department.contains(searchQuery, ignoreCase = true)
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "Faculty Directory",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
          )
          Text(
            text = "${facultyList.size} professors and teaching staff • Full editing enabled",
            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
          )
        }

        Button(
          onClick = { showAddDialog = true },
          colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
          shape = RoundedCornerShape(8.dp)
        ) {
          Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Add Faculty", fontSize = 12.sp)
        }
      }
    }

    item {
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text("Search by professor name, employee ID, or department...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
      )
    }

    items(filtered, key = { it.id }) { prof ->
      val assigned = subjects.filter { it.facultyId == prof.id || it.facultyName.contains(prof.name, ignoreCase = true) }
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
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              modifier = Modifier.weight(1f)
            ) {
              Box(
                modifier = Modifier
                  .size(40.dp)
                  .clip(CircleShape)
                  .background(if (prof.isActive) Indigo100 else Slate200),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = prof.name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString(""),
                  fontWeight = FontWeight.Bold,
                  color = if (prof.isActive) Indigo700 else Slate600
                )
              }

              Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  Text(prof.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (prof.isActive) Emerald50 else Rose50
                  ) {
                    Text(
                      text = if (prof.isActive) "Active" else "Deactivated",
                      style = MaterialTheme.typography.labelSmall.copy(
                        color = if (prof.isActive) Emerald600 else Rose600,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                      ),
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                  }
                }
                Text(
                  "${prof.designation} • ${prof.department}",
                  style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                Text(
                  "ID: ${prof.employeeId} • ${prof.email} • ${prof.phone}",
                  style = MaterialTheme.typography.labelSmall.copy(color = Indigo600)
                )
              }
            }

            // Actions: Edit, Toggle Active, Delete
            Row(verticalAlignment = Alignment.CenterVertically) {
              IconButton(
                onClick = { editingFaculty = prof },
                modifier = Modifier.size(36.dp)
              ) {
                Icon(
                  Icons.Default.Edit,
                  contentDescription = "Edit Faculty",
                  tint = Indigo600,
                  modifier = Modifier.size(18.dp)
                )
              }
              IconButton(
                onClick = { onToggleActive(prof) },
                modifier = Modifier.size(36.dp)
              ) {
                Icon(
                  imageVector = if (prof.isActive) Icons.Default.Block else Icons.Default.CheckCircle,
                  contentDescription = "Toggle Active",
                  tint = if (prof.isActive) Amber600 else Emerald600,
                  modifier = Modifier.size(18.dp)
                )
              }
              IconButton(
                onClick = { facultyToDelete = prof },
                modifier = Modifier.size(36.dp)
              ) {
                Icon(
                  Icons.Default.Delete,
                  contentDescription = "Delete",
                  tint = Rose600,
                  modifier = Modifier.size(18.dp)
                )
              }
            }
          }

          if (assigned.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = Emerald50
            ) {
              Text(
                text = "Courses Taught: " + assigned.joinToString { "${it.name} (${it.code})" },
                style = MaterialTheme.typography.labelSmall.copy(color = Emerald700, fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }
        }
      }
    }
  }

  // Add Faculty Dialog
  if (showAddDialog) {
    AddOrEditFacultyDialog(
      faculty = null,
      departments = departments,
      onDismiss = { showAddDialog = false },
      onSave = { newProf ->
        onAddOrUpdate(newProf, false)
        showAddDialog = false
      }
    )
  }

  // Edit Faculty Dialog
  editingFaculty?.let { prof ->
    AddOrEditFacultyDialog(
      faculty = prof,
      departments = departments,
      onDismiss = { editingFaculty = null },
      onSave = { updatedProf ->
        onAddOrUpdate(updatedProf, true)
        editingFaculty = null
      }
    )
  }

  // Delete Confirmation
  facultyToDelete?.let { prof ->
    AlertDialog(
      onDismissRequest = { facultyToDelete = null },
      title = { Text("Delete Faculty Profile") },
      text = { Text("Are you sure you want to remove ${prof.name} (${prof.employeeId}) from faculty records?") },
      confirmButton = {
        Button(
          onClick = {
            onDelete(prof)
            facultyToDelete = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = Rose600)
        ) {
          Text("Delete")
        }
      },
      dismissButton = {
        TextButton(onClick = { facultyToDelete = null }) { Text("Cancel") }
      }
    )
  }
}

// -------------------------------------------------------------
// 4. ADMIN ACADEMICS (DEPARTMENTS, SUBJECTS, TIMETABLES - ADD & EDIT)
// -------------------------------------------------------------
@Composable
private fun AdminAcademicsContent(
  departments: List<DepartmentEntity>,
  subjects: List<SubjectEntity>,
  timetables: List<TimetableEntity>,
  facultyList: List<UserEntity>,
  onAddOrUpdateDepartment: (DepartmentEntity, Boolean) -> Unit,
  onDeleteDepartment: (DepartmentEntity) -> Unit,
  onAddOrUpdateSubject: (SubjectEntity, Boolean) -> Unit,
  onDeleteSubject: (SubjectEntity) -> Unit,
  onAddOrUpdateTimetable: (TimetableEntity, Boolean) -> Unit,
  onDeleteTimetable: (TimetableEntity) -> Unit
) {
  var selectedSubTab by remember { mutableStateOf(0) }
  var showAddDeptDialog by remember { mutableStateOf(false) }
  var editingDepartment by remember { mutableStateOf<DepartmentEntity?>(null) }
  var deptToDelete by remember { mutableStateOf<DepartmentEntity?>(null) }

  var showAddSubDialog by remember { mutableStateOf(false) }
  var editingSubject by remember { mutableStateOf<SubjectEntity?>(null) }
  var subToDelete by remember { mutableStateOf<SubjectEntity?>(null) }

  var showAddTimetableDialog by remember { mutableStateOf(false) }
  var editingTimetable by remember { mutableStateOf<TimetableEntity?>(null) }
  var timetableToDelete by remember { mutableStateOf<TimetableEntity?>(null) }
  var selectedDayFilter by remember { mutableStateOf("All") }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    item {
      Text(
        text = "Academic Structure & Schedules",
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
      )
    }

    item {
      TabRow(selectedTabIndex = selectedSubTab) {
        Tab(selected = selectedSubTab == 0, onClick = { selectedSubTab = 0 }, text = { Text("Departments (${departments.size})") })
        Tab(selected = selectedSubTab == 1, onClick = { selectedSubTab = 1 }, text = { Text("Subjects (${subjects.size})") })
        Tab(selected = selectedSubTab == 2, onClick = { selectedSubTab = 2 }, text = { Text("Timetables (${timetables.size})") })
      }
    }

    when (selectedSubTab) {
      0 -> {
        item {
          Button(
            onClick = { showAddDeptDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Add Department")
          }
        }
        items(departments, key = { it.id }) { dept ->
          Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  Text(dept.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                  Surface(shape = RoundedCornerShape(4.dp), color = Indigo50) {
                    Text(
                      text = dept.code,
                      color = Indigo700,
                      fontWeight = FontWeight.Bold,
                      fontSize = 11.sp,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                  }
                }
                Text("Head of Dept: ${dept.headOfDepartment}", style = MaterialTheme.typography.bodySmall)
                Text("${dept.studentCount} Students • ${dept.facultyCount} Faculty Members", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
              }

              Row {
                IconButton(onClick = { editingDepartment = dept }) {
                  Icon(Icons.Default.Edit, contentDescription = "Edit Department", tint = Indigo600)
                }
                IconButton(onClick = { deptToDelete = dept }) {
                  Icon(Icons.Default.Delete, contentDescription = "Delete Department", tint = Rose600)
                }
              }
            }
          }
        }
      }

      1 -> {
        item {
          Button(
            onClick = { showAddSubDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Add Subject / Course")
          }
        }
        items(subjects, key = { it.id }) { sub ->
          Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  Text(sub.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                  Surface(shape = RoundedCornerShape(4.dp), color = Indigo50) {
                    Text(
                      text = sub.code,
                      color = Indigo700,
                      fontWeight = FontWeight.Bold,
                      fontSize = 11.sp,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                  }
                }
                Text("Faculty: ${sub.facultyName} • Location: ${sub.room}", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                  Surface(shape = RoundedCornerShape(4.dp), color = Emerald50) {
                    Text(
                      text = "Min Attendance: ${sub.targetAttendancePercent.toInt()}%",
                      color = Emerald700,
                      fontWeight = FontWeight.Bold,
                      fontSize = 11.sp,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                  }
                  Text("${sub.totalClasses} total classes planned", style = MaterialTheme.typography.labelSmall.copy(color = Slate600))
                }
              }

              Row {
                IconButton(onClick = { editingSubject = sub }) {
                  Icon(Icons.Default.Edit, contentDescription = "Edit Subject", tint = Indigo600)
                }
                IconButton(onClick = { subToDelete = sub }) {
                  Icon(Icons.Default.Delete, contentDescription = "Delete Subject", tint = Rose600)
                }
              }
            }
          }
        }
      }

      2 -> {
        // Timetable Section with Add button and Day filters
        item {
          Button(
            onClick = { showAddTimetableDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Add Class Schedule / Slot")
          }
        }

        item {
          val days = listOf("All", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday")
          LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(days) { day ->
              FilterChip(
                selected = selectedDayFilter == day,
                onClick = { selectedDayFilter = day },
                label = { Text(day) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = Indigo600,
                  selectedLabelColor = Color.White
                )
              )
            }
          }
        }

        val filteredTimetables = if (selectedDayFilter == "All") {
          timetables.sortedWith(compareBy({ it.dayOfWeek }, { it.startTime }))
        } else {
          timetables.filter { it.dayOfWeek.equals(selectedDayFilter, ignoreCase = true) }.sortedBy { it.startTime }
        }

        if (filteredTimetables.isEmpty()) {
          item {
            Card(
              shape = RoundedCornerShape(10.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
              Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                Text("No classes scheduled for $selectedDayFilter. Tap 'Add Class Schedule' above to add one.")
              }
            }
          }
        } else {
          items(filteredTimetables, key = { it.id }) { tt ->
            Card(
              shape = RoundedCornerShape(10.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(shape = RoundedCornerShape(4.dp), color = Indigo50) {
                      Text(
                        text = tt.dayOfWeek,
                        color = Indigo700,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                      )
                    }
                    Text(tt.subjectName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                  }
                  Text(
                    text = "${tt.startTime} – ${tt.endTime} • ${tt.room} • Sec ${tt.section}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium, color = Slate700)
                  )
                  Text("Faculty: ${tt.facultyName}", style = MaterialTheme.typography.labelSmall.copy(color = Indigo600))
                }

                Row {
                  IconButton(onClick = { editingTimetable = tt }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Schedule", tint = Indigo600)
                  }
                  IconButton(onClick = { timetableToDelete = tt }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Schedule", tint = Rose600)
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  // --- Department Dialogs ---
  if (showAddDeptDialog) {
    AddOrEditDepartmentDialog(
      department = null,
      onDismiss = { showAddDeptDialog = false },
      onSave = { newDept ->
        onAddOrUpdateDepartment(newDept, false)
        showAddDeptDialog = false
      }
    )
  }

  editingDepartment?.let { dept ->
    AddOrEditDepartmentDialog(
      department = dept,
      onDismiss = { editingDepartment = null },
      onSave = { updatedDept ->
        onAddOrUpdateDepartment(updatedDept, true)
        editingDepartment = null
      }
    )
  }

  deptToDelete?.let { dept ->
    AlertDialog(
      onDismissRequest = { deptToDelete = null },
      title = { Text("Delete Department") },
      text = { Text("Are you sure you want to remove department ${dept.name} (${dept.code})?") },
      confirmButton = {
        Button(
          onClick = {
            onDeleteDepartment(dept)
            deptToDelete = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = Rose600)
        ) {
          Text("Delete")
        }
      },
      dismissButton = {
        TextButton(onClick = { deptToDelete = null }) { Text("Cancel") }
      }
    )
  }

  // --- Subject Dialogs ---
  if (showAddSubDialog) {
    AddOrEditSubjectDialog(
      subject = null,
      facultyList = facultyList,
      departments = departments,
      onDismiss = { showAddSubDialog = false },
      onSave = { newSub ->
        onAddOrUpdateSubject(newSub, false)
        showAddSubDialog = false
      }
    )
  }

  editingSubject?.let { sub ->
    AddOrEditSubjectDialog(
      subject = sub,
      facultyList = facultyList,
      departments = departments,
      onDismiss = { editingSubject = null },
      onSave = { updatedSub ->
        onAddOrUpdateSubject(updatedSub, true)
        editingSubject = null
      }
    )
  }

  subToDelete?.let { sub ->
    AlertDialog(
      onDismissRequest = { subToDelete = null },
      title = { Text("Delete Subject") },
      text = { Text("Are you sure you want to remove subject ${sub.name} (${sub.code})?") },
      confirmButton = {
        Button(
          onClick = {
            onDeleteSubject(sub)
            subToDelete = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = Rose600)
        ) {
          Text("Delete")
        }
      },
      dismissButton = {
        TextButton(onClick = { subToDelete = null }) { Text("Cancel") }
      }
    )
  }

  // --- Timetable Dialogs ---
  if (showAddTimetableDialog) {
    AddOrEditTimetableDialog(
      timetable = null,
      subjects = subjects,
      facultyList = facultyList,
      onDismiss = { showAddTimetableDialog = false },
      onSave = { newTt ->
        onAddOrUpdateTimetable(newTt, false)
        showAddTimetableDialog = false
      }
    )
  }

  editingTimetable?.let { tt ->
    AddOrEditTimetableDialog(
      timetable = tt,
      subjects = subjects,
      facultyList = facultyList,
      onDismiss = { editingTimetable = null },
      onSave = { updatedTt ->
        onAddOrUpdateTimetable(updatedTt, true)
        editingTimetable = null
      }
    )
  }

  timetableToDelete?.let { tt ->
    AlertDialog(
      onDismissRequest = { timetableToDelete = null },
      title = { Text("Delete Class Schedule") },
      text = { Text("Are you sure you want to remove the ${tt.dayOfWeek} schedule for ${tt.subjectName}?") },
      confirmButton = {
        Button(
          onClick = {
            onDeleteTimetable(tt)
            timetableToDelete = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = Rose600)
        ) {
          Text("Delete")
        }
      },
      dismissButton = {
        TextButton(onClick = { timetableToDelete = null }) { Text("Cancel") }
      }
    )
  }
}

// -------------------------------------------------------------
// 5. ADMIN REQUIREMENTS & AUDIT LOGS (EDIT POLICIES & REQUIREMENTS)
// -------------------------------------------------------------
@Composable
private fun AdminRequirementsAndAuditContent(
  auditLogs: List<AuditLogEntity>,
  policy: AttendanceRequirementPolicy,
  onUpdatePolicy: (AttendanceRequirementPolicy) -> Unit,
  onExportLogs: () -> Unit
) {
  var showEditPolicyDialog by remember { mutableStateOf(false) }
  var logSearchQuery by remember { mutableStateOf("") }

  val filteredLogs = auditLogs.filter {
    it.actorName.contains(logSearchQuery, ignoreCase = true) ||
      it.action.contains(logSearchQuery, ignoreCase = true) ||
      it.details.contains(logSearchQuery, ignoreCase = true)
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
            text = "College Requirements & Audit",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
          )
          Text(
            text = "Configure college rules, attendance criteria & track all system updates",
            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
          )
        }

        IconButton(onClick = onExportLogs) {
          Icon(Icons.Default.Download, contentDescription = "Export Logs", tint = Indigo600)
        }
      }
    }

    // Editable Requirements & Policy Card
    item {
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Box(
                modifier = Modifier
                  .size(36.dp)
                  .clip(CircleShape)
                  .background(Indigo100),
                contentAlignment = Alignment.Center
              ) {
                Icon(Icons.Default.Policy, contentDescription = null, tint = Indigo600, modifier = Modifier.size(20.dp))
              }
              Column {
                Text("College Attendance Requirements", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text("Campus-wide regulations & threshold rules", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
              }
            }

            Button(
              onClick = { showEditPolicyDialog = true },
              colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
              shape = RoundedCornerShape(8.dp)
            ) {
              Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Edit Rules", fontSize = 12.sp)
            }
          }

          Divider()

          // Policy items grid
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PolicyStatChip(
              title = "Min Requirement",
              value = "${policy.minAttendancePercent.toInt()}%",
              subtitle = "Mandatory for exams",
              color = Emerald600,
              modifier = Modifier.weight(1f)
            )
            PolicyStatChip(
              title = "Warning Threshold",
              value = "${policy.warningThresholdPercent.toInt()}%",
              subtitle = "Triggers alert SMS",
              color = Amber600,
              modifier = Modifier.weight(1f)
            )
            PolicyStatChip(
              title = "Condonation Limit",
              value = "${policy.condonationLimitPercent.toInt()}%",
              subtitle = "Medical / HOD quota",
              color = Indigo600,
              modifier = Modifier.weight(1f)
            )
          }

          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("• Academic Term: ${policy.academicTerm}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium))
            Text("• QR Session Timeout: ${policy.lateGracePeriodMinutes} minutes", style = MaterialTheme.typography.bodySmall)
            Text("• Max Allowed Casual Leaves: ${policy.maxCasualLeaves} days per semester", style = MaterialTheme.typography.bodySmall)
            Text(
              "• Duty / Event Leave Condonation: ${if (policy.allowMedicalDutyCondonation) "Enabled (Counts as attendance)" else "Disabled"}",
              style = MaterialTheme.typography.bodySmall
            )
          }
        }
      }
    }

    // Audit Logs Section Header & Search
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("System Event Logs (${auditLogs.size})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
      }
    }

    item {
      OutlinedTextField(
        value = logSearchQuery,
        onValueChange = { logSearchQuery = it },
        placeholder = { Text("Filter audit trail by actor, action or detail...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
      )
    }

    items(filteredLogs, key = { it.id }) { log ->
      val dateFormatted = remember(log.timestamp) {
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        sdf.format(Date(log.timestamp))
      }
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
              .size(36.dp)
              .clip(CircleShape)
              .background(Indigo50),
            contentAlignment = Alignment.Center
          ) {
            Icon(Icons.Default.Security, contentDescription = null, tint = Indigo600, modifier = Modifier.size(18.dp))
          }

          Column(modifier = Modifier.weight(1f)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(log.action, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
              Surface(
                shape = RoundedCornerShape(4.dp),
                color = Slate100
              ) {
                Text(
                  text = log.actorRole,
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                  modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
              }
            }
            Text(
              log.details,
              style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                "By ${log.actorName}",
                style = MaterialTheme.typography.labelSmall.copy(color = Indigo600, fontSize = 10.sp)
              )
              Text(
                dateFormatted,
                style = MaterialTheme.typography.labelSmall.copy(color = Slate500, fontSize = 10.sp)
              )
            }
          }
        }
      }
    }
  }

  // Edit Policy Dialog
  if (showEditPolicyDialog) {
    EditCollegePolicyDialog(
      currentPolicy = policy,
      onDismiss = { showEditPolicyDialog = false },
      onSave = { updated ->
        onUpdatePolicy(updated)
        showEditPolicyDialog = false
      }
    )
  }
}

@Composable
private fun PolicyStatChip(
  title: String,
  value: String,
  subtitle: String,
  color: Color,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = color.copy(alpha = 0.08f),
    modifier = modifier
  ) {
    Column(modifier = Modifier.padding(10.dp)) {
      Text(title, style = MaterialTheme.typography.labelSmall.copy(color = Slate600, fontSize = 10.sp))
      Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = color))
      Text(subtitle, style = MaterialTheme.typography.labelSmall.copy(color = Slate500, fontSize = 9.sp))
    }
  }
}

// -------------------------------------------------------------
// DIALOGS: ADD / EDIT STUDENTS, FACULTY, DEPTS, SUBJECTS, TIMETABLES & POLICY
// -------------------------------------------------------------

@Composable
private fun AddOrEditStudentDialog(
  student: UserEntity?,
  departments: List<DepartmentEntity>,
  onDismiss: () -> Unit,
  onSave: (UserEntity) -> Unit
) {
  val isEdit = student != null
  var name by remember { mutableStateOf(student?.name ?: "") }
  var rollNo by remember { mutableStateOf(student?.rollNo ?: "") }
  var regNo by remember { mutableStateOf(student?.regNo ?: "") }
  var email by remember { mutableStateOf(student?.email ?: "") }
  var phone by remember { mutableStateOf(student?.phone ?: "+1 (555) 789-0123") }
  var department by remember { mutableStateOf(student?.department ?: (departments.firstOrNull()?.name ?: "Computer Science & Engineering")) }
  var course by remember { mutableStateOf(student?.course ?: "Diploma Computer Science") }
  var semester by remember { mutableStateOf(student?.semester ?: "5th Semester") }
  var section by remember { mutableStateOf(student?.section ?: "CSE-4A") }
  var isActive by remember { mutableStateOf(student?.isActive ?: true) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (isEdit) "Edit Student Details" else "Add New Student") },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = rollNo, onValueChange = { rollNo = it }, label = { Text("Roll Number (e.g. CSE24-001) *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = regNo, onValueChange = { regNo = it }, label = { Text("Registration Number (e.g. REG-2024-2027)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email Address *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone Number") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = course, onValueChange = { course = it }, label = { Text("Degree / Course") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(value = semester, onValueChange = { semester = it }, label = { Text("Semester") }, singleLine = true, modifier = Modifier.weight(1f))
          OutlinedTextField(value = section, onValueChange = { section = it }, label = { Text("Section") }, singleLine = true, modifier = Modifier.weight(1f))
        }
        OutlinedTextField(value = department, onValueChange = { department = it }, label = { Text("Department") }, singleLine = true, modifier = Modifier.fillMaxWidth())

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text("Account Active", style = MaterialTheme.typography.bodyMedium)
          Switch(checked = isActive, onCheckedChange = { isActive = it })
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (name.isNotBlank() && email.isNotBlank()) {
            val user = student?.copy(
              name = name.trim(),
              rollNo = rollNo.trim().ifBlank { student.rollNo },
              regNo = regNo.trim().ifBlank { student.regNo },
              email = email.trim(),
              phone = phone.trim(),
              department = department.trim(),
              course = course.trim(),
              semester = semester.trim(),
              section = section.trim(),
              isActive = isActive
            ) ?: UserEntity(
              id = "U-STD-${UUID.randomUUID().toString().take(6).uppercase()}",
              name = name.trim(),
              email = email.trim(),
              password = "password123",
              role = "STUDENT",
              rollNo = rollNo.trim().ifBlank { "CSE24-${(100..999).random()}" },
              regNo = regNo.trim().ifBlank { "REG-2024-${(1000..9999).random()}" },
              department = department.trim(),
              course = course.trim(),
              semester = semester.trim(),
              section = section.trim(),
              phone = phone.trim(),
              isActive = isActive
            )
            onSave(user)
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = Indigo600)
      ) {
        Text(if (isEdit) "Save Changes" else "Add Student")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    }
  )
}

@Composable
private fun AddOrEditFacultyDialog(
  faculty: UserEntity?,
  departments: List<DepartmentEntity>,
  onDismiss: () -> Unit,
  onSave: (UserEntity) -> Unit
) {
  val isEdit = faculty != null
  var name by remember { mutableStateOf(faculty?.name ?: "") }
  var employeeId by remember { mutableStateOf(faculty?.employeeId ?: "") }
  var designation by remember { mutableStateOf(faculty?.designation ?: "Associate Professor") }
  var department by remember { mutableStateOf(faculty?.department ?: (departments.firstOrNull()?.name ?: "Computer Science & Engineering")) }
  var email by remember { mutableStateOf(faculty?.email ?: "") }
  var phone by remember { mutableStateOf(faculty?.phone ?: "+1 (555) 432-8765") }
  var isActive by remember { mutableStateOf(faculty?.isActive ?: true) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (isEdit) "Edit Faculty Profile" else "Add New Faculty") },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name (e.g. Dr. Robert Chen) *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = employeeId, onValueChange = { employeeId = it }, label = { Text("Employee ID (e.g. EMP-CSE-104) *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = designation, onValueChange = { designation = it }, label = { Text("Designation") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = department, onValueChange = { department = it }, label = { Text("Department") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email Address *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone Number") }, singleLine = true, modifier = Modifier.fillMaxWidth())

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text("Faculty Active", style = MaterialTheme.typography.bodyMedium)
          Switch(checked = isActive, onCheckedChange = { isActive = it })
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (name.isNotBlank() && email.isNotBlank()) {
            val user = faculty?.copy(
              name = name.trim(),
              employeeId = employeeId.trim().ifBlank { faculty.employeeId },
              designation = designation.trim(),
              department = department.trim(),
              email = email.trim(),
              phone = phone.trim(),
              isActive = isActive
            ) ?: UserEntity(
              id = "U-FAC-${UUID.randomUUID().toString().take(6).uppercase()}",
              name = name.trim(),
              email = email.trim(),
              password = "password123",
              role = "FACULTY",
              employeeId = employeeId.trim().ifBlank { "EMP-CSE-${(100..999).random()}" },
              designation = designation.trim(),
              department = department.trim(),
              phone = phone.trim(),
              isActive = isActive
            )
            onSave(user)
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = Indigo600)
      ) {
        Text(if (isEdit) "Save Changes" else "Add Faculty")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    }
  )
}

@Composable
private fun AddOrEditDepartmentDialog(
  department: DepartmentEntity?,
  onDismiss: () -> Unit,
  onSave: (DepartmentEntity) -> Unit
) {
  val isEdit = department != null
  var deptName by remember { mutableStateOf(department?.name ?: "") }
  var deptCode by remember { mutableStateOf(department?.code ?: "") }
  var hod by remember { mutableStateOf(department?.headOfDepartment ?: "") }
  var studentCount by remember { mutableStateOf(department?.studentCount?.toString() ?: "60") }
  var facultyCount by remember { mutableStateOf(department?.facultyCount?.toString() ?: "12") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (isEdit) "Edit Department" else "Add Department") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = deptName, onValueChange = { deptName = it }, label = { Text("Department Name *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = deptCode, onValueChange = { deptCode = it }, label = { Text("Code (e.g. CSE, ECE) *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = hod, onValueChange = { hod = it }, label = { Text("Head of Department (HOD)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
          OutlinedTextField(value = studentCount, onValueChange = { studentCount = it }, label = { Text("Enrolled Students") }, singleLine = true, modifier = Modifier.weight(1f))
          OutlinedTextField(value = facultyCount, onValueChange = { facultyCount = it }, label = { Text("Faculty Count") }, singleLine = true, modifier = Modifier.weight(1f))
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (deptName.isNotBlank() && deptCode.isNotBlank()) {
            val entity = department?.copy(
              name = deptName.trim(),
              code = deptCode.trim().uppercase(),
              headOfDepartment = hod.trim(),
              studentCount = studentCount.toIntOrNull() ?: 60,
              facultyCount = facultyCount.toIntOrNull() ?: 12
            ) ?: DepartmentEntity(
              id = "DEPT-${deptCode.trim().uppercase()}",
              name = deptName.trim(),
              code = deptCode.trim().uppercase(),
              headOfDepartment = hod.trim(),
              studentCount = studentCount.toIntOrNull() ?: 60,
              facultyCount = facultyCount.toIntOrNull() ?: 12
            )
            onSave(entity)
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = Indigo600)
      ) {
        Text(if (isEdit) "Save Changes" else "Add Department")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    }
  )
}

@Composable
private fun AddOrEditSubjectDialog(
  subject: SubjectEntity?,
  facultyList: List<UserEntity>,
  departments: List<DepartmentEntity>,
  onDismiss: () -> Unit,
  onSave: (SubjectEntity) -> Unit
) {
  val isEdit = subject != null
  var subName by remember { mutableStateOf(subject?.name ?: "") }
  var subCode by remember { mutableStateOf(subject?.code ?: "") }
  var room by remember { mutableStateOf(subject?.room ?: "Lab 204") }
  var department by remember { mutableStateOf(subject?.department ?: (departments.firstOrNull()?.name ?: "Computer Science")) }
  var semester by remember { mutableStateOf(subject?.semester ?: "Semester 4") }
  var facultyName by remember { mutableStateOf(subject?.facultyName ?: (facultyList.firstOrNull()?.name ?: "Prof. Robert Chen")) }
  var targetAttendance by remember { mutableStateOf(subject?.targetAttendancePercent?.toString() ?: "75.0") }
  var totalClasses by remember { mutableStateOf(subject?.totalClasses?.toString() ?: "45") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (isEdit) "Edit Subject & Requirements" else "Add New Subject") },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedTextField(value = subName, onValueChange = { subName = it }, label = { Text("Subject Name *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = subCode, onValueChange = { subCode = it }, label = { Text("Subject Code (e.g. CS401) *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = room, onValueChange = { room = it }, label = { Text("Classroom / Room (e.g. Lab 204)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = facultyName, onValueChange = { facultyName = it }, label = { Text("Assigned Faculty Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
          value = targetAttendance,
          onValueChange = { targetAttendance = it },
          label = { Text("Min Attendance Requirement % (e.g. 75.0) *") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(value = totalClasses, onValueChange = { totalClasses = it }, label = { Text("Total Classes") }, singleLine = true, modifier = Modifier.weight(1f))
          OutlinedTextField(value = semester, onValueChange = { semester = it }, label = { Text("Semester") }, singleLine = true, modifier = Modifier.weight(1f))
        }
        OutlinedTextField(value = department, onValueChange = { department = it }, label = { Text("Department") }, singleLine = true, modifier = Modifier.fillMaxWidth())
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (subName.isNotBlank() && subCode.isNotBlank()) {
            val assignedProf = facultyList.firstOrNull { it.name.equals(facultyName.trim(), ignoreCase = true) }
            val entity = subject?.copy(
              name = subName.trim(),
              code = subCode.trim().uppercase(),
              department = department.trim(),
              semester = semester.trim(),
              facultyId = assignedProf?.id ?: subject.facultyId,
              facultyName = facultyName.trim(),
              room = room.trim(),
              targetAttendancePercent = targetAttendance.toFloatOrNull() ?: 75.0f,
              totalClasses = totalClasses.toIntOrNull() ?: 45
            ) ?: SubjectEntity(
              id = "SUB-${subCode.trim().uppercase()}",
              name = subName.trim(),
              code = subCode.trim().uppercase(),
              department = department.trim(),
              semester = semester.trim(),
              facultyId = assignedProf?.id ?: "U-FAC-001",
              facultyName = facultyName.trim(),
              room = room.trim(),
              targetAttendancePercent = targetAttendance.toFloatOrNull() ?: 75.0f,
              totalClasses = totalClasses.toIntOrNull() ?: 45
            )
            onSave(entity)
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = Indigo600)
      ) {
        Text(if (isEdit) "Save Changes" else "Add Subject")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    }
  )
}

@Composable
private fun AddOrEditTimetableDialog(
  timetable: TimetableEntity?,
  subjects: List<SubjectEntity>,
  facultyList: List<UserEntity>,
  onDismiss: () -> Unit,
  onSave: (TimetableEntity) -> Unit
) {
  val isEdit = timetable != null
  val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")
  var dayOfWeek by remember { mutableStateOf(timetable?.dayOfWeek ?: "Monday") }
  var subjectName by remember { mutableStateOf(timetable?.subjectName ?: (subjects.firstOrNull()?.name ?: "Computer Networks")) }
  var section by remember { mutableStateOf(timetable?.section ?: "CSE-4A") }
  var room by remember { mutableStateOf(timetable?.room ?: "Room 305") }
  var startTime by remember { mutableStateOf(timetable?.startTime ?: "09:00 AM") }
  var endTime by remember { mutableStateOf(timetable?.endTime ?: "10:00 AM") }
  var facultyName by remember { mutableStateOf(timetable?.facultyName ?: (facultyList.firstOrNull()?.name ?: "Prof. Robert Chen")) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (isEdit) "Edit Class Schedule" else "Add Class Schedule") },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Text("Day of the Week", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(days) { day ->
            FilterChip(
              selected = dayOfWeek == day,
              onClick = { dayOfWeek = day },
              label = { Text(day, fontSize = 12.sp) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Indigo600,
                selectedLabelColor = Color.White
              )
            )
          }
        }

        OutlinedTextField(value = subjectName, onValueChange = { subjectName = it }, label = { Text("Subject Name *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(value = startTime, onValueChange = { startTime = it }, label = { Text("Start (e.g. 09:00 AM)") }, singleLine = true, modifier = Modifier.weight(1f))
          OutlinedTextField(value = endTime, onValueChange = { endTime = it }, label = { Text("End (e.g. 10:00 AM)") }, singleLine = true, modifier = Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(value = room, onValueChange = { room = it }, label = { Text("Room / Location") }, singleLine = true, modifier = Modifier.weight(1f))
          OutlinedTextField(value = section, onValueChange = { section = it }, label = { Text("Section (e.g. CSE-4A)") }, singleLine = true, modifier = Modifier.weight(1f))
        }
        OutlinedTextField(value = facultyName, onValueChange = { facultyName = it }, label = { Text("Faculty In-Charge") }, singleLine = true, modifier = Modifier.fillMaxWidth())
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (subjectName.isNotBlank()) {
            val matchingSub = subjects.firstOrNull { it.name.equals(subjectName.trim(), ignoreCase = true) }
            val matchingFaculty = facultyList.firstOrNull { it.name.equals(facultyName.trim(), ignoreCase = true) }
            val entity = timetable?.copy(
              dayOfWeek = dayOfWeek,
              subjectId = matchingSub?.id ?: timetable.subjectId,
              subjectName = subjectName.trim(),
              section = section.trim(),
              room = room.trim(),
              startTime = startTime.trim(),
              endTime = endTime.trim(),
              facultyName = facultyName.trim()
            ) ?: TimetableEntity(
              id = "TT-${UUID.randomUUID().toString().take(6).uppercase()}",
              dayOfWeek = dayOfWeek,
              subjectId = matchingSub?.id ?: "SUB-01",
              subjectName = subjectName.trim(),
              section = section.trim(),
              room = room.trim(),
              startTime = startTime.trim(),
              endTime = endTime.trim(),
              facultyName = facultyName.trim()
            )
            onSave(entity)
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = Indigo600)
      ) {
        Text(if (isEdit) "Save Changes" else "Add Schedule")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    }
  )
}

@Composable
private fun EditCollegePolicyDialog(
  currentPolicy: AttendanceRequirementPolicy,
  onDismiss: () -> Unit,
  onSave: (AttendanceRequirementPolicy) -> Unit
) {
  var minAttendance by remember { mutableStateOf(currentPolicy.minAttendancePercent.toString()) }
  var warningThreshold by remember { mutableStateOf(currentPolicy.warningThresholdPercent.toString()) }
  var condonationLimit by remember { mutableStateOf(currentPolicy.condonationLimitPercent.toString()) }
  var lateGrace by remember { mutableStateOf(currentPolicy.lateGracePeriodMinutes.toString()) }
  var maxLeaves by remember { mutableStateOf(currentPolicy.maxCasualLeaves.toString()) }
  var academicTerm by remember { mutableStateOf(currentPolicy.academicTerm) }
  var allowDutyLeaveCredit by remember { mutableStateOf(currentPolicy.allowMedicalDutyCondonation) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Edit College Attendance Policy & Rules") },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text("Modify campus requirements and thresholds below.", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))

        OutlinedTextField(
          value = minAttendance,
          onValueChange = { minAttendance = it },
          label = { Text("Mandatory Minimum Attendance % *") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = warningThreshold,
          onValueChange = { warningThreshold = it },
          label = { Text("Attendance Warning Threshold % *") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = condonationLimit,
          onValueChange = { condonationLimit = it },
          label = { Text("Medical / Exemption Condonation Floor %") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = lateGrace,
            onValueChange = { lateGrace = it },
            label = { Text("QR Grace Period (mins)") },
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
          OutlinedTextField(
            value = maxLeaves,
            onValueChange = { maxLeaves = it },
            label = { Text("Max Casual Leaves") },
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
        }
        OutlinedTextField(
          value = academicTerm,
          onValueChange = { academicTerm = it },
          label = { Text("Current Academic Term / Semester Title") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("Duty & Medical Leave Credit", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
            Text("Automatically grant attendance credit on approved duty leaves", style = MaterialTheme.typography.labelSmall.copy(color = Slate600))
          }
          Switch(checked = allowDutyLeaveCredit, onCheckedChange = { allowDutyLeaveCredit = it })
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val updated = currentPolicy.copy(
            minAttendancePercent = minAttendance.toFloatOrNull() ?: 75.0f,
            warningThresholdPercent = warningThreshold.toFloatOrNull() ?: 70.0f,
            condonationLimitPercent = condonationLimit.toFloatOrNull() ?: 65.0f,
            lateGracePeriodMinutes = lateGrace.toIntOrNull() ?: 10,
            maxCasualLeaves = maxLeaves.toIntOrNull() ?: 6,
            academicTerm = academicTerm.trim().ifBlank { currentPolicy.academicTerm },
            allowMedicalDutyCondonation = allowDutyLeaveCredit
          )
          onSave(updated)
        },
        colors = ButtonDefaults.buttonColors(containerColor = Indigo600)
      ) {
        Text("Save Policy")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    }
  )
}
