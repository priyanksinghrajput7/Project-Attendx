package com.example.ui.screens.student

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttendanceRecordEntity
import com.example.data.model.AttendanceSessionEntity
import com.example.data.model.NoticeEntity
import com.example.data.model.TimetableEntity
import com.example.data.model.UserEntity
import com.example.data.repository.AttendanceSummary
import com.example.data.repository.SubjectAttendanceStat
import com.example.ui.components.*
import com.example.ui.screens.schedule.ClassScheduleCard
import com.example.ui.screens.schedule.RoomLocationDirectionsDialog
import com.example.ui.screens.schedule.TodaysScheduleSection
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun StudentMainScreen(
  user: UserEntity,
  summary: AttendanceSummary,
  subjectStats: List<SubjectAttendanceStat>,
  records: List<AttendanceRecordEntity>,
  timetables: List<TimetableEntity>,
  activeSessions: List<AttendanceSessionEntity> = emptyList(),
  pendingLeavesCount: Int = 0,
  notices: List<NoticeEntity> = emptyList(),
  onScanQR: () -> Unit,
  onScanQRForSubject: (String) -> Unit = { onScanQR() },
  onOpenNoticeBoard: () -> Unit = {},
  onOpenLeaveSection: () -> Unit = {},
  onOpenSchedule: () -> Unit = {},
  onOpenReport: (String, String, String) -> Unit,
  onUpdateProfile: (String, String, String) -> Unit
) {
  var selectedTab by remember { mutableStateOf(0) }
  var showCalculatorDialog by remember { mutableStateOf(false) }

  Scaffold(
    bottomBar = {
      NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
      ) {
        NavigationBarItem(
          selected = selectedTab == 0,
          onClick = { selectedTab = 0 },
          icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
          label = { Text("Dashboard") },
          modifier = Modifier.testTag("student_nav_dashboard")
        )
        NavigationBarItem(
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Calendar") },
          label = { Text("Calendar") },
          modifier = Modifier.testTag("student_nav_calendar")
        )
        NavigationBarItem(
          selected = selectedTab == 2,
          onClick = { selectedTab = 2 },
          icon = { Icon(Icons.Default.MenuBook, contentDescription = "Subjects") },
          label = { Text("Subjects") },
          modifier = Modifier.testTag("student_nav_subjects")
        )
        NavigationBarItem(
          selected = selectedTab == 3,
          onClick = { selectedTab = 3 },
          icon = { Icon(Icons.Default.Schedule, contentDescription = "Timetable") },
          label = { Text("Timetable") },
          modifier = Modifier.testTag("student_nav_timetable")
        )
        NavigationBarItem(
          selected = selectedTab == 4,
          onClick = { selectedTab = 4 },
          icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
          label = { Text("Profile") },
          modifier = Modifier.testTag("student_nav_profile")
        )
      }
    },
    floatingActionButton = {
      if (selectedTab == 0 || selectedTab == 1) {
        ExtendedFloatingActionButton(
          onClick = onScanQR,
          containerColor = Indigo600,
          contentColor = Color.White,
          icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) },
          text = { Text("Scan QR Attendance") },
          modifier = Modifier.testTag("student_scan_qr_fab")
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
        0 -> StudentDashboardContent(
          user = user,
          summary = summary,
          subjectStats = subjectStats,
          timetables = timetables,
          activeSessions = activeSessions,
          records = records,
          pendingLeavesCount = pendingLeavesCount,
          notices = notices,
          onOpenNoticeBoard = onOpenNoticeBoard,
          onOpenLeaveSection = onOpenLeaveSection,
          onOpenSchedule = onOpenSchedule,
          onScanQR = onScanQR,
          onOpenCalculator = { showCalculatorDialog = true },
          onExportReport = {
            val reportSummary = "Overall Attendance: ${String.format("%.1f", summary.percentage)}%\nTotal Classes: ${summary.totalClasses} (Present: ${summary.presentCount}, Absent: ${summary.absentCount}, Late: ${summary.lateCount})"
            val tableText = buildString {
              append(String.format("%-22s %-8s %-6s %-6s %-6s\n", "Subject", "Total", "Pres", "Abs", "%"))
              append("----------------------------------------------------\n")
              subjectStats.forEach { stat ->
                append(String.format("%-22s %-8d %-6d %-6d %.1f%%\n", stat.subjectName.take(20), stat.totalClasses, stat.presentCount, stat.absentCount, stat.percentage))
              }
            }
            onOpenReport("Student Attendance Report - ${user.name}", reportSummary, tableText)
          }
        )
        1 -> StudentCalendarContent(records = records, timetables = timetables)
        2 -> StudentSubjectsContent(
          subjectStats = subjectStats,
          onOpenCalculator = { showCalculatorDialog = true }
        )
        3 -> StudentTimetableContent(
          timetables = timetables,
          records = records,
          onScanQR = onScanQR
        )
        4 -> StudentProfileContent(
          user = user,
          onOpenLeaveSection = onOpenLeaveSection,
          onUpdate = onUpdateProfile
        )
      }
    }
  }

  if (showCalculatorDialog) {
    AttendanceCalculatorDialog(
      currentPresent = summary.presentCount,
      currentTotal = summary.totalClasses,
      targetPercentage = 75f,
      onDismiss = { showCalculatorDialog = false }
    )
  }
}

// -------------------------------------------------------------
// 1. STUDENT DASHBOARD
// -------------------------------------------------------------
@Composable
private fun StudentDashboardContent(
  user: UserEntity,
  summary: AttendanceSummary,
  subjectStats: List<SubjectAttendanceStat>,
  timetables: List<TimetableEntity>,
  activeSessions: List<AttendanceSessionEntity> = emptyList(),
  records: List<AttendanceRecordEntity>,
  pendingLeavesCount: Int,
  notices: List<NoticeEntity> = emptyList(),
  onOpenNoticeBoard: () -> Unit = {},
  onOpenLeaveSection: () -> Unit,
  onOpenSchedule: () -> Unit = {},
  onScanQR: () -> Unit = {},
  onScanQRForSubject: (String) -> Unit = { onScanQR() },
  onOpenCalculator: () -> Unit,
  onExportReport: () -> Unit
) {
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Top Greeting Section
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "Good Morning, ${user.name.split(" ").firstOrNull() ?: user.name} 👋",
            style = MaterialTheme.typography.titleLarge.copy(
              fontWeight = FontWeight.Black,
              letterSpacing = (-0.5).sp
            )
          )
          Text(
            text = "${user.rollNo} • ${user.section} • ${user.semester}",
            style = MaterialTheme.typography.bodySmall.copy(
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          )
        }

        IconButton(
          onClick = onExportReport,
          modifier = Modifier.testTag("download_report_button")
        ) {
          Icon(Icons.Default.FileDownload, contentDescription = "Download Report", tint = Indigo600)
        }
      }
    }

    // Student Leave Section Action Card
    item {
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, Indigo100),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onOpenLeaveSection() }
          .testTag("student_leave_action_card")
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
              .clip(CircleShape)
              .background(Indigo50),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.EventBusy,
              contentDescription = null,
              tint = Indigo600,
              modifier = Modifier.size(24.dp)
            )
          }

          Column(modifier = Modifier.weight(1f)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Text(
                text = "Student Leave Section",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
              )
              if (pendingLeavesCount > 0) {
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = Amber50,
                  border = androidx.compose.foundation.BorderStroke(1.dp, Amber200)
                ) {
                  Text(
                    text = "$pendingLeavesCount Pending",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontWeight = FontWeight.Bold,
                      color = Amber700,
                      fontSize = 10.sp
                    ),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
              }
            }
            Text(
              text = "Apply for leave or notify faculty (Sick, Casual, Duty)",
              style = MaterialTheme.typography.bodySmall.copy(color = Slate500)
            )
          }

          FilledTonalButton(
            onClick = onOpenLeaveSection,
            colors = ButtonDefaults.filledTonalButtonColors(
              containerColor = Indigo100,
              contentColor = Indigo700
            ),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
          ) {
            Text("Apply", fontWeight = FontWeight.Bold, fontSize = 12.sp)
          }
        }
      }
    }

    // Campus Notice Board Section / Quick Banner
    item {
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, Amber300),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onOpenNoticeBoard() }
          .testTag("student_notice_board_card")
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
                  .clip(RoundedCornerShape(12.dp))
                  .background(Amber100),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  Icons.Default.Campaign,
                  contentDescription = "Notice Board",
                  tint = Amber700,
                  modifier = Modifier.size(24.dp)
                )
              }
              Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  Text(
                    text = "Campus Notice Board",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                  )
                  if (notices.isNotEmpty()) {
                    Surface(
                      shape = RoundedCornerShape(6.dp),
                      color = Rose100
                    ) {
                      Text(
                        text = "${notices.size} Live 📢",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Rose700,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                      )
                    }
                  }
                }
                Text(
                  text = "Official exams, holidays & faculty notices",
                  style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
              }
            }

            TextButton(
              onClick = onOpenNoticeBoard,
              colors = ButtonDefaults.textButtonColors(contentColor = Amber700)
            ) {
              Text("View All →", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
          }

          // Top / Most recent notice preview
          val topNotice = notices.firstOrNull { it.isPinned } ?: notices.firstOrNull()
          if (topNotice != null) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = Amber50.copy(alpha = 0.7f),
              border = androidx.compose.foundation.BorderStroke(0.8.dp, Amber200),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  if (topNotice.isPinned) {
                    Text("📌", fontSize = 11.sp)
                  }
                  Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Amber200
                  ) {
                    Text(
                      text = topNotice.category.uppercase(),
                      fontSize = 9.sp,
                      fontWeight = FontWeight.Black,
                      color = Amber900,
                      modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                  }
                  Text(
                    text = "By ${topNotice.postedBy}",
                    style = MaterialTheme.typography.labelSmall.copy(
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      fontWeight = FontWeight.Medium
                    )
                  )
                }

                Text(
                  text = topNotice.title,
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                  maxLines = 1,
                  overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )

                Text(
                  text = topNotice.content,
                  style = MaterialTheme.typography.bodySmall.copy(color = Slate600),
                  maxLines = 2,
                  overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
              }
            }
          }
        }
      }
    }

    // Hero Attendance Overview Card with Circular Progress
    item {
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Overall Attendance",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
              )
              Text(
                text = if (summary.totalClasses == 0) {
                  "No attendance recorded yet. Scan live class QR to start!"
                } else if (summary.percentage >= 75f) {
                  "You are meeting the minimum criteria."
                } else {
                  "Below 75% required attendance."
                },
                style = MaterialTheme.typography.bodySmall.copy(
                  color = if (summary.totalClasses == 0) Slate600 else if (summary.percentage >= 75f) Emerald600 else Rose600,
                  fontWeight = FontWeight.SemiBold
                )
              )

              Spacer(modifier = Modifier.height(12.dp))

              OutlinedButton(
                onClick = onOpenCalculator,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
              ) {
                Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Target Calculator", fontSize = 12.sp)
              }
            }

            CircularAttendanceProgress(
              percentage = summary.percentage,
              size = 110.dp,
              strokeWidth = 10.dp
            )
          }

          Spacer(modifier = Modifier.height(16.dp))
          HorizontalDivider()
          Spacer(modifier = Modifier.height(12.dp))

          // 4 Metric Mini-Cards
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            MetricPill("Total", "${summary.totalClasses}", Slate700)
            MetricPill("Present", "${summary.presentCount}", Emerald600)
            MetricPill("Absent", "${summary.absentCount}", Rose600)
            MetricPill("Late", "${summary.lateCount}", Amber600)
          }
        }
      }
    }

    // Low Attendance Warning Banner (if any subject < 75%)
    val lowSubjects = subjectStats.filter { it.percentage < 75f }
    if (lowSubjects.isNotEmpty()) {
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = Amber50),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Icon(Icons.Default.WarningAmber, contentDescription = null, tint = Amber600)
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "⚠ Attendance Warning",
                style = MaterialTheme.typography.titleSmall.copy(
                  fontWeight = FontWeight.Bold,
                  color = Amber600
                )
              )
              Text(
                text = "Your attendance in ${lowSubjects.first().subjectName} is ${String.format("%.1f", lowSubjects.first().percentage)}% (below 75%). Tap calculator to plan recovery.",
                style = MaterialTheme.typography.bodySmall.copy(color = Slate800)
              )
            }
          }
        }
      }
    }

    // Today's Schedule Section
    item {
      TodaysScheduleSection(
        timetables = timetables,
        records = records,
        activeSessions = activeSessions,
        onScanQR = onScanQR,
        onScanQRForSubject = onScanQRForSubject,
        onSeeAll = onOpenSchedule
      )
    }

    // Subject Attendance Progress Section
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Subject Attendance",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
        Text(
          text = "Min: 75%",
          style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
      }
    }

    items(subjectStats) { stat ->
      val hasActiveSession = activeSessions.any { it.subjectId == stat.subjectId }

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
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Text(
                text = stat.subjectName,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
              )
              if (hasActiveSession) {
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = Emerald50,
                  border = androidx.compose.foundation.BorderStroke(1.dp, Emerald300)
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                  ) {
                    Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(Emerald500))
                    Text(
                      text = "LIVE QR",
                      fontSize = 9.sp,
                      fontWeight = FontWeight.Black,
                      color = Emerald700
                    )
                  }
                }
              }
            }

            Text(
              text = "${stat.presentCount}/${stat.totalClasses} classes",
              style = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            )
          }
          Spacer(modifier = Modifier.height(8.dp))
          SubjectProgressBar(percentage = stat.percentage)
          Spacer(modifier = Modifier.height(10.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "${String.format("%.1f", stat.percentage)}%",
              style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = if (stat.percentage >= 75f) Emerald600 else Rose600
              )
            )
            if (hasActiveSession) {
              FilledTonalButton(
                onClick = { onScanQRForSubject(stat.subjectId) },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                  containerColor = Emerald100,
                  contentColor = Emerald800
                ),
                modifier = Modifier.height(32.dp).testTag("scan_subject_${stat.subjectId}")
              ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Scan Live QR", fontSize = 11.sp, fontWeight = FontWeight.Bold)
              }
            } else {
              OutlinedButton(
                onClick = { onScanQRForSubject(stat.subjectId) },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(30.dp).testTag("scan_subject_${stat.subjectId}")
              ) {
                Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(13.dp), tint = Slate500)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Waiting for QR", fontSize = 11.sp, fontWeight = FontWeight.Normal, color = Slate600)
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun MetricPill(label: String, value: String, color: Color) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(
      text = value,
      style = MaterialTheme.typography.titleMedium.copy(
        fontWeight = FontWeight.Bold,
        color = color
      )
    )
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall.copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    )
  }
}

// -------------------------------------------------------------
// 2. STUDENT CALENDAR
// -------------------------------------------------------------
@Composable
private fun StudentCalendarContent(
  records: List<AttendanceRecordEntity>,
  timetables: List<TimetableEntity> = emptyList()
) {
  val todayCal = remember { Calendar.getInstance() }
  val todayYear = todayCal.get(Calendar.YEAR)
  val todayMonth = todayCal.get(Calendar.MONTH)
  val todayDay = todayCal.get(Calendar.DAY_OF_MONTH)

  var monthOffset by remember { mutableIntStateOf(0) }

  val displayCal = remember(monthOffset) {
    (todayCal.clone() as Calendar).apply {
      set(Calendar.DAY_OF_MONTH, 1)
      add(Calendar.MONTH, monthOffset)
    }
  }

  val displayYear = displayCal.get(Calendar.YEAR)
  val displayMonth = displayCal.get(Calendar.MONTH)
  val displayMonthName = remember(displayCal) {
    SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(displayCal.time)
  }

  val maxDaysInMonth = remember(displayCal) {
    displayCal.getActualMaximum(Calendar.DAY_OF_MONTH)
  }

  // Sunday to Saturday offset (Sunday = Calendar.SUNDAY = 1 -> offset 0)
  val startOffset = remember(displayCal) {
    val dow = displayCal.get(Calendar.DAY_OF_WEEK)
    (dow - Calendar.SUNDAY + 7) % 7
  }

  val totalGridCells = remember(startOffset, maxDaysInMonth) {
    val cells = startOffset + maxDaysInMonth
    val rows = kotlin.math.ceil(cells / 7.0).toInt()
    rows * 7
  }

  var selectedDay by remember(monthOffset) {
    mutableIntStateOf(if (monthOffset == 0) todayDay.coerceIn(1, maxDaysInMonth) else 1)
  }

  val selectedDateCal = remember(displayCal, selectedDay) {
    (displayCal.clone() as Calendar).apply {
      set(Calendar.DAY_OF_MONTH, selectedDay.coerceIn(1, maxDaysInMonth))
    }
  }

  val selectedDateString = remember(selectedDateCal) {
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(selectedDateCal.time)
  }

  val selectedFullFormattedDate = remember(selectedDateCal) {
    SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(selectedDateCal.time)
  }

  val selectedWeekdayName = remember(selectedDateCal) {
    SimpleDateFormat("EEEE", Locale.US).format(selectedDateCal.time)
  }

  val isSelectedDateToday = (monthOffset == 0 && selectedDay == todayDay)

  // Classes for the selected day of the week from timetable
  val scheduledClasses = remember(timetables, selectedWeekdayName) {
    timetables.filter { it.dayOfWeek.equals(selectedWeekdayName, ignoreCase = true) }
      .sortedBy { it.startTime }
  }

  // Attendance records for this exact date
  val selectedDayRecords = remember(records, selectedDateString) {
    records.filter { it.date == selectedDateString }
  }

  fun getDayDateString(day: Int): String {
    return String.format(Locale.US, "%04d-%02d-%02d", displayYear, displayMonth + 1, day)
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    item {
      Text(
        text = "Attendance Calendar",
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
      )
      Text(
        text = "Dynamic monthly attendance and timetable tracking with live date synchronization.",
        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
      )
    }

    if (records.isEmpty()) {
      item {
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = Indigo50),
          border = androidx.compose.foundation.BorderStroke(1.dp, Indigo100)
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = Indigo600, modifier = Modifier.size(24.dp))
            Column {
              Text(
                text = "No Attendance Records Yet",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Indigo900)
              )
              Text(
                text = "Attendance is recorded strictly when you scan your instructor's live QR code in class.",
                style = MaterialTheme.typography.bodySmall.copy(color = Indigo700)
              )
            }
          }
        }
      }
    }

    // Month Controller & Calendar Grid
    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          // Month navigation header
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            IconButton(
              onClick = { monthOffset-- },
              modifier = Modifier.testTag("prev_month_btn")
            ) {
              Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
            }

            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Text(
                text = displayMonthName,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
              )
              if (monthOffset != 0) {
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = Indigo50,
                  border = androidx.compose.foundation.BorderStroke(1.dp, Indigo200),
                  modifier = Modifier.clickable {
                    monthOffset = 0
                    selectedDay = todayDay.coerceIn(1, todayCal.getActualMaximum(Calendar.DAY_OF_MONTH))
                  }
                ) {
                  Text(
                    text = "Today",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontWeight = FontWeight.Bold,
                      color = Indigo700
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                  )
                }
              }
            }

            IconButton(
              onClick = { monthOffset++ },
              modifier = Modifier.testTag("next_month_btn")
            ) {
              Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Sunday to Saturday day-of-week headers
          val weekHeaders = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
          ) {
            weekHeaders.forEachIndexed { idx, d ->
              val isWeekendHeader = (idx == 0 || idx == 6)
              Text(
                text = d,
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  color = if (isWeekendHeader) Rose600 else MaterialTheme.colorScheme.onSurfaceVariant
                )
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Dynamic Calendar Grid
          val rowCount = totalGridCells / 7
          for (r in 0 until rowCount) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
              horizontalArrangement = Arrangement.SpaceAround
            ) {
              for (c in 0 until 7) {
                val cellIndex = r * 7 + c
                if (cellIndex in startOffset until (startOffset + maxDaysInMonth)) {
                  val dayNumber = cellIndex - startOffset + 1
                  val isToday = (monthOffset == 0 && dayNumber == todayDay)
                  val isSelected = (dayNumber == selectedDay)
                  val dayDateStr = getDayDateString(dayNumber)
                  val dayRecs = records.filter { it.date == dayDateStr }
                  val isWeekend = (c == 0 || c == 6)

                  val hasPresent = dayRecs.any { it.status.equals("PRESENT", ignoreCase = true) }
                  val hasAbsent = dayRecs.any { it.status.equals("ABSENT", ignoreCase = true) }
                  val hasLate = dayRecs.any { it.status.equals("LATE", ignoreCase = true) }

                  val dotColor = when {
                    hasPresent -> Emerald500
                    hasAbsent -> Rose500
                    hasLate -> Amber500
                    else -> null
                  }

                  Box(
                    modifier = Modifier
                      .size(38.dp)
                      .clip(CircleShape)
                      .background(
                        when {
                          isSelected -> Indigo600
                          isToday -> Indigo50
                          else -> Color.Transparent
                        }
                      )
                      .then(
                        if (isToday && !isSelected) {
                          Modifier.border(1.5.dp, Indigo500, CircleShape)
                        } else Modifier
                      )
                      .clickable { selectedDay = dayNumber }
                      .testTag("calendar_day_$dayNumber"),
                    contentAlignment = Alignment.Center
                  ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                      Text(
                        text = "$dayNumber",
                        fontSize = 12.sp,
                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                        color = when {
                          isSelected -> Color.White
                          isToday -> Indigo700
                          isWeekend -> Rose600.copy(alpha = 0.85f)
                          else -> MaterialTheme.colorScheme.onSurface
                        }
                      )
                      if (dotColor != null) {
                        Box(
                          modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) Color.White else dotColor)
                        )
                      } else if (isToday) {
                        Box(
                          modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) Color.White else Indigo500)
                        )
                      }
                    }
                  }
                } else {
                  Spacer(modifier = Modifier.size(38.dp))
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          // Attendance status legend
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
          ) {
            LegendItem("Present", Emerald500)
            LegendItem("Absent", Rose500)
            LegendItem("Late", Amber500)
            LegendItem("Today", Indigo600)
          }
        }
      }
    }

    // Selected Day Header & Schedule Breakdown
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = selectedFullFormattedDate,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
          )
          Text(
            text = if (isSelectedDateToday) "Today • ${scheduledClasses.size} Classes Scheduled" else "${scheduledClasses.size} Classes Scheduled",
            style = MaterialTheme.typography.bodySmall.copy(
              color = if (isSelectedDateToday) Indigo600 else Slate500,
              fontWeight = FontWeight.Medium
            )
          )
        }

        if (selectedDayRecords.isNotEmpty()) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = Emerald50,
            border = androidx.compose.foundation.BorderStroke(1.dp, Emerald200)
          ) {
            Text(
              text = "${selectedDayRecords.count { it.status.equals("PRESENT", ignoreCase = true) }} Attended",
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = Emerald700
              ),
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }
        }
      }
    }

    if (scheduledClasses.isEmpty()) {
      item {
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Icon(Icons.Default.Weekend, contentDescription = null, tint = Slate500, modifier = Modifier.size(24.dp))
            Column {
              Text(
                text = "No Classes Scheduled",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
              )
              Text(
                text = "No scheduled timetable sessions for $selectedWeekdayName.",
                style = MaterialTheme.typography.bodySmall.copy(color = Slate600)
              )
            }
          }
        }
      }
    } else {
      items(scheduledClasses) { cls ->
        val record = selectedDayRecords.firstOrNull {
          it.subjectId.equals(cls.subjectId, ignoreCase = true) ||
            it.subjectName.equals(cls.subjectName, ignoreCase = true)
        }
        val isAttended = record?.status.equals("PRESENT", ignoreCase = true)

        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isAttended) Emerald200 else Slate200
          ),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              modifier = Modifier.weight(1f),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(42.dp)
                  .clip(RoundedCornerShape(10.dp))
                  .background(if (isAttended) Emerald50 else Indigo50),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = if (isAttended) Icons.Default.CheckCircle else Icons.Default.MenuBook,
                  contentDescription = null,
                  tint = if (isAttended) Emerald600 else Indigo600,
                  modifier = Modifier.size(22.dp)
                )
              }

              Column {
                Text(
                  text = cls.subjectName,
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                  text = "${cls.startTime} - ${cls.endTime} • ${cls.room}",
                  style = MaterialTheme.typography.bodySmall.copy(color = Slate600)
                )
                Text(
                  text = "Faculty: ${cls.facultyName} • Section ${cls.section}",
                  style = MaterialTheme.typography.labelSmall.copy(color = Slate500)
                )
              }
            }

            // Attendance Status Badge
            if (isAttended) {
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = Emerald50,
                border = androidx.compose.foundation.BorderStroke(1.dp, Emerald200)
              ) {
                Column(
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                  horizontalAlignment = Alignment.CenterHorizontally
                ) {
                  Text(
                    text = "ATTENDED ✅",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontWeight = FontWeight.Bold,
                      color = Emerald700
                    )
                  )
                  if (!record?.time.isNullOrBlank()) {
                    Text(
                      text = record!!.time,
                      style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        color = Emerald600
                      )
                    )
                  }
                }
              }
            } else if (record != null) {
              AttendanceStatusBadge(record.status)
            } else {
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isSelectedDateToday) Indigo50 else Slate100,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelectedDateToday) Indigo200 else Slate200)
              ) {
                Text(
                  text = if (isSelectedDateToday) "TODAY" else "SCHEDULED",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isSelectedDateToday) Indigo700 else Slate600
                  ),
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun LegendItem(label: String, color: Color) {
  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
    Box(
      modifier = Modifier
        .size(8.dp)
        .clip(CircleShape)
        .background(color)
    )
    Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}

// -------------------------------------------------------------
// 3. SUBJECT-WISE ATTENDANCE
// -------------------------------------------------------------
@Composable
private fun StudentSubjectsContent(
  subjectStats: List<SubjectAttendanceStat>,
  onOpenCalculator: () -> Unit,
  onScanQRForSubject: (String) -> Unit = {}
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
            text = "Subject Attendance",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
          )
          Text(
            text = "Detailed syllabus breakdown and class ratios.",
            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
          )
        }

        Button(
          onClick = onOpenCalculator,
          colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
          shape = RoundedCornerShape(8.dp)
        ) {
          Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Calculator", fontSize = 12.sp)
        }
      }
    }

    items(subjectStats) { stat ->
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = stat.subjectName,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
              )
              Text(
                text = "Faculty: ${stat.facultyName} • ${stat.room}",
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
              )
            }
            Text(
              text = "${String.format("%.1f", stat.percentage)}%",
              style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Black,
                color = if (stat.percentage >= 75f) Emerald600 else Rose600
              )
            )
          }

          Spacer(modifier = Modifier.height(12.dp))
          SubjectProgressBar(percentage = stat.percentage)
          Spacer(modifier = Modifier.height(14.dp))

          // 3-Column Stats Breakdown
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column {
              Text("Total Classes", style = MaterialTheme.typography.labelSmall)
              Text("${stat.totalClasses}", fontWeight = FontWeight.Bold)
            }
            Column {
              Text("Attended", style = MaterialTheme.typography.labelSmall)
              Text("${stat.presentCount}", fontWeight = FontWeight.Bold, color = Emerald600)
            }
            Column {
              Text("Missed", style = MaterialTheme.typography.labelSmall)
              Text("${stat.absentCount}", fontWeight = FontWeight.Bold, color = Rose600)
            }
            Column {
              Text("Late", style = MaterialTheme.typography.labelSmall)
              Text("${stat.lateCount}", fontWeight = FontWeight.Bold, color = Amber600)
            }
          }

          if (stat.isWarning) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
              color = Rose50,
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Rose600, modifier = Modifier.size(16.dp))
                Text(
                  text = "Attendance is below mandatory 75%. You risk debarment from exams.",
                  style = MaterialTheme.typography.labelSmall.copy(color = Rose600)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
          ) {
            Button(
              onClick = { onScanQRForSubject(stat.subjectId) },
              colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
              shape = RoundedCornerShape(8.dp),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
              modifier = Modifier.testTag("scan_subject_qr_btn_${stat.subjectId}")
            ) {
              Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(15.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Scan QR Attendance", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}

// -------------------------------------------------------------
// 4. TIMETABLE
// -------------------------------------------------------------
@Composable
private fun StudentTimetableContent(
  timetables: List<TimetableEntity>,
  records: List<AttendanceRecordEntity> = emptyList(),
  onScanQR: () -> Unit = {},
  onScanQRForSubject: (String) -> Unit = { onScanQR() }
) {
  val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")
  val calendar = remember { java.util.Calendar.getInstance() }
  val currentDayName = remember {
    when (calendar.get(java.util.Calendar.DAY_OF_WEEK)) {
      java.util.Calendar.MONDAY -> "Monday"
      java.util.Calendar.TUESDAY -> "Tuesday"
      java.util.Calendar.WEDNESDAY -> "Wednesday"
      java.util.Calendar.THURSDAY -> "Thursday"
      java.util.Calendar.FRIDAY -> "Friday"
      else -> "Monday"
    }
  }
  var selectedDay by remember { mutableStateOf(currentDayName) }
  var selectedClassForDirections by remember { mutableStateOf<TimetableEntity?>(null) }

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
            text = "Class Schedule & Timetable",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
          )
          Text(
            text = "Time slots, course names & room locations for Section 4A.",
            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
          )
        }
      }
    }

    // Day Pills Row
    item {
      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        items(days) { day ->
          val isSelected = selectedDay == day
          val isToday = day.equals(currentDayName, ignoreCase = true)
          FilterChip(
            selected = isSelected,
            onClick = { selectedDay = day },
            label = {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Text(day)
                if (isToday) {
                  Box(
                    modifier = Modifier
                      .size(6.dp)
                      .clip(CircleShape)
                      .background(if (isSelected) Color.White else Emerald500)
                  )
                }
              }
            },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = Indigo600,
              selectedLabelColor = Color.White
            )
          )
        }
      }
    }

    val dayClasses = timetables.filter { it.dayOfWeek.equals(selectedDay, ignoreCase = true) }.sortedBy { it.startTime }
    if (dayClasses.isEmpty()) {
      item {
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
          Box(modifier = Modifier.padding(16.dp)) {
            Text("No classes scheduled for $selectedDay.")
          }
        }
      }
    } else {
      items(dayClasses, key = { it.id }) { tt ->
        val record = records.firstOrNull {
          it.subjectName.equals(tt.subjectName, ignoreCase = true) ||
            it.subjectId.equals(tt.subjectId, ignoreCase = true)
        }
        ClassScheduleCard(
          item = tt,
          attendanceStatus = record?.status,
          isToday = selectedDay.equals(currentDayName, ignoreCase = true),
          onScanQR = {
            onScanQRForSubject(tt.subjectId)
            onScanQR()
          },
          onOpenDirections = { selectedClassForDirections = tt }
        )
      }
    }
  }

  selectedClassForDirections?.let { tt ->
    RoomLocationDirectionsDialog(
      timetable = tt,
      onDismiss = { selectedClassForDirections = null }
    )
  }
}

// -------------------------------------------------------------
// 5. PROFILE
// -------------------------------------------------------------
@Composable
private fun StudentProfileContent(
  user: UserEntity,
  onOpenLeaveSection: () -> Unit = {},
  onUpdate: (String, String, String) -> Unit
) {
  var name by remember { mutableStateOf(user.name) }
  var phone by remember { mutableStateOf(user.phone) }
  var section by remember { mutableStateOf(user.section) }
  var isEditing by remember { mutableStateOf(false) }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    item {
      Text(
        text = "Student Profile",
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
      )
    }

    item {
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = Indigo50,
        border = androidx.compose.foundation.BorderStroke(1.dp, Indigo200),
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onOpenLeaveSection() }
          .testTag("student_profile_leaves_tile")
      ) {
        Row(
          modifier = Modifier.padding(14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Icon(Icons.Default.EventBusy, contentDescription = null, tint = Indigo700)
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Leave Applications / छुट्टी आवेदन",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Indigo900)
            )
            Text(
              text = "Submit new request or track approval status",
              style = MaterialTheme.typography.bodySmall.copy(color = Slate600)
            )
          }
          Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Indigo700, modifier = Modifier.size(18.dp))
        }
      }
    }

    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Box(
            modifier = Modifier
              .size(80.dp)
              .clip(CircleShape)
              .background(Indigo100),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = user.name.take(1).uppercase(),
              fontSize = 32.sp,
              fontWeight = FontWeight.Bold,
              color = Indigo700
            )
          }

          Spacer(modifier = Modifier.height(12.dp))

          Text(
            text = user.name,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
          )
          Text(
            text = "${user.rollNo} • ${user.course}",
            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
          )

          Spacer(modifier = Modifier.height(16.dp))
          HorizontalDivider()
          Spacer(modifier = Modifier.height(16.dp))

          ProfileInfoRow("Registration No", user.regNo.ifEmpty { "REG-2024-8891" })
          ProfileInfoRow("Department", user.department)
          ProfileInfoRow("Semester", user.semester)
          ProfileInfoRow("Section", user.section)
          ProfileInfoRow("Email", user.email)
          ProfileInfoRow("Phone", user.phone)

          Spacer(modifier = Modifier.height(16.dp))

          if (!isEditing) {
            OutlinedButton(
              onClick = { isEditing = true },
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Edit Profile")
            }
          } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name") },
                modifier = Modifier.fillMaxWidth()
              )
              OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone Number") },
                modifier = Modifier.fillMaxWidth()
              )
              OutlinedTextField(
                value = section,
                onValueChange = { section = it },
                label = { Text("Section") },
                modifier = Modifier.fillMaxWidth()
              )
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Button(
                  onClick = {
                    onUpdate(name, phone, section)
                    isEditing = false
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                  modifier = Modifier.weight(1f)
                ) {
                  Text("Save Changes")
                }
                OutlinedButton(
                  onClick = { isEditing = false },
                  modifier = Modifier.weight(1f)
                ) {
                  Text("Cancel")
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ProfileInfoRow(label: String, value: String) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 6.dp),
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(label, style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
    Text(value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
  }
}
