package com.example.ui.screens.schedule

import androidx.compose.animation.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttendanceRecordEntity
import com.example.data.model.AttendanceSessionEntity
import com.example.data.model.TimetableEntity
import com.example.data.model.UserEntity
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Screen displaying the user's classes for the day with time slots,
 * course names, room locations, and faculty details.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodaysScheduleScreen(
  user: UserEntity,
  timetables: List<TimetableEntity>,
  records: List<AttendanceRecordEntity> = emptyList(),
  activeSessions: List<AttendanceSessionEntity> = emptyList(),
  onScanQR: () -> Unit = {},
  onScanQRForSubject: (String) -> Unit = { onScanQR() },
  onBack: () -> Unit = {}
) {
  val weekdays = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")

  // Determine current day of week
  val calendar = remember { Calendar.getInstance() }
  val currentDayName = remember {
    when (calendar.get(Calendar.DAY_OF_WEEK)) {
      Calendar.MONDAY -> "Monday"
      Calendar.TUESDAY -> "Tuesday"
      Calendar.WEDNESDAY -> "Wednesday"
      Calendar.THURSDAY -> "Thursday"
      Calendar.FRIDAY -> "Friday"
      Calendar.SATURDAY -> "Saturday"
      else -> "Sunday"
    }
  }

  val isWeekend = currentDayName == "Saturday" || currentDayName == "Sunday"
  // Default to today if weekday, else Monday
  var selectedDay by remember {
    mutableStateOf(if (isWeekend) "Monday" else currentDayName)
  }

  val todayDateFormatted = remember {
    val sdf = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())
    sdf.format(calendar.time)
  }

  var selectedClassForDirections by remember { mutableStateOf<TimetableEntity?>(null) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "Today's Schedule",
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
              text = "$todayDateFormatted • ${user.section}",
              style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
          }
        },
        navigationIcon = {
          IconButton(
            onClick = onBack,
            modifier = Modifier.testTag("schedule_back_button")
          ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          if (user.role.equals("STUDENT", ignoreCase = true)) {
            FilledTonalButton(
              onClick = onScanQR,
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
              modifier = Modifier
                .padding(end = 8.dp)
                .testTag("schedule_header_scan_qr_btn")
            ) {
              Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Scan QR", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
      )
    }
  ) { padding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Weekend / Day Notice banner
      if (isWeekend && selectedDay == "Monday") {
        item {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = Indigo50,
            border = androidx.compose.foundation.BorderStroke(1.dp, Indigo100),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Icon(Icons.Default.Info, contentDescription = null, tint = Indigo600)
              Text(
                text = "It's the weekend! Previewing upcoming Monday classes for Section ${user.section}.",
                style = MaterialTheme.typography.bodySmall.copy(color = Indigo900, fontWeight = FontWeight.Medium)
              )
            }
          }
        }
      }

      // Day of Week Selector Chips
      item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "Select Day",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Slate600)
          )
          LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(weekdays) { day ->
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
                    Text(
                      text = day.take(3),
                      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
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
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("schedule_day_chip_$day")
              )
            }
          }
        }
      }

      // Filter classes for selected day
      val dayClasses = timetables
        .filter { it.dayOfWeek.equals(selectedDay, ignoreCase = true) }
        .sortedBy { it.startTime }

      // Schedule Overview Metrics Card
      item {
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(
                  text = if (selectedDay.equals(currentDayName, ignoreCase = true)) "Today's Agenda" else "$selectedDay's Agenda",
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                  text = "${dayClasses.size} scheduled classes • Section ${user.section}",
                  style = MaterialTheme.typography.bodySmall.copy(color = Slate500)
                )
              }

              Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (dayClasses.isNotEmpty()) Emerald50 else Slate100,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (dayClasses.isNotEmpty()) Emerald200 else Slate200)
              ) {
                Text(
                  text = if (dayClasses.isNotEmpty()) "${dayClasses.size} Classes" else "Free Day",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (dayClasses.isNotEmpty()) Emerald700 else Slate600
                  ),
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
              }
            }

            if (dayClasses.isNotEmpty()) {
              HorizontalDivider(color = Slate100)
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Column {
                  Text("Starts At", style = MaterialTheme.typography.labelSmall.copy(color = Slate500))
                  Text(dayClasses.first().startTime, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                }
                Column {
                  Text("Ends At", style = MaterialTheme.typography.labelSmall.copy(color = Slate500))
                  Text(dayClasses.last().endTime, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                }
                Column {
                  Text("Location", style = MaterialTheme.typography.labelSmall.copy(color = Slate500))
                  Text("Main Campus", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                }
              }
            }
          }
        }
      }

      // Schedule Section Title
      item {
        Text(
          text = "Classes & Time Slots",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
      }

      if (dayClasses.isEmpty()) {
        item {
          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(
                Icons.Default.Weekend,
                contentDescription = null,
                tint = Indigo400,
                modifier = Modifier.size(42.dp)
              )
              Text(
                text = "No Classes Scheduled",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
              )
              Text(
                text = "You have no lectures or practical sessions on $selectedDay.",
                style = MaterialTheme.typography.bodySmall.copy(color = Slate500)
              )
            }
          }
        }
      } else {
        items(dayClasses, key = { it.id }) { item ->
          // Check attendance status for this subject
          val attendanceRecord = records.firstOrNull { rec ->
            rec.subjectName.equals(item.subjectName, ignoreCase = true) ||
              rec.subjectId.equals(item.subjectId, ignoreCase = true)
          }

          val hasActiveSession = activeSessions.any { it.subjectId == item.subjectId }

          ClassScheduleCard(
            item = item,
            attendanceStatus = attendanceRecord?.status,
            isToday = selectedDay.equals(currentDayName, ignoreCase = true),
            hasActiveSession = hasActiveSession,
            onScanQR = {
              onScanQRForSubject(item.subjectId)
              onScanQR()
            },
            onOpenDirections = { selectedClassForDirections = item }
          )
        }
      }
    }
  }

  // Room Location & Directions Dialog
  selectedClassForDirections?.let { cls ->
    RoomLocationDirectionsDialog(
      timetable = cls,
      onDismiss = { selectedClassForDirections = null }
    )
  }
}

/**
 * Individual Class Card with Time Slot, Course Name, Room Location & Faculty
 */
@Composable
fun ClassScheduleCard(
  item: TimetableEntity,
  attendanceStatus: String? = null,
  isToday: Boolean = false,
  hasActiveSession: Boolean = false,
  onScanQR: () -> Unit = {},
  onOpenDirections: () -> Unit = {}
) {
  Surface(
    shape = RoundedCornerShape(18.dp),
    color = MaterialTheme.colorScheme.surface,
    border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
    shadowElevation = 1.dp,
    modifier = Modifier
      .fillMaxWidth()
      .testTag("class_schedule_card_${item.id}")
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      // 1. Time Slot Bar
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = Indigo50,
          border = androidx.compose.foundation.BorderStroke(1.dp, Indigo100)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(Icons.Default.Schedule, contentDescription = null, tint = Indigo700, modifier = Modifier.size(14.dp))
            Text(
              text = "${item.startTime} – ${item.endTime}",
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = Indigo900
              )
            )
          }
        }

        // Attendance / Schedule status badge
        when (attendanceStatus?.uppercase()) {
          "PRESENT" -> {
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
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Emerald600, modifier = Modifier.size(12.dp))
                Text(
                  text = "ATTENDED",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Emerald700)
                )
              }
            }
          }
          "ABSENT" -> {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = Rose50,
              border = androidx.compose.foundation.BorderStroke(1.dp, Rose200)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Icon(Icons.Default.Cancel, contentDescription = null, tint = Rose600, modifier = Modifier.size(12.dp))
                Text(
                  text = "MISSED",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Rose700)
                )
              }
            }
          }
          else -> {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = if (hasActiveSession) Emerald50 else Amber50,
              border = androidx.compose.foundation.BorderStroke(1.dp, if (hasActiveSession) Emerald200 else Amber200)
            ) {
              Text(
                text = if (hasActiveSession) "LIVE QR ACTIVE" else if (isToday) "SCHEDULED TODAY" else "SCHEDULED",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  color = if (hasActiveSession) Emerald700 else Amber800
                ),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }
        }
      }

      // 2. Course Name & Code
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
      ) {
        Box(
          modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Indigo100),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Default.MenuBook, contentDescription = null, tint = Indigo700, modifier = Modifier.size(24.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = item.subjectName,
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              fontSize = 17.sp
            )
          )
          Spacer(modifier = Modifier.height(2.dp))
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = Slate100
            ) {
              Text(
                text = item.subjectId.ifEmpty { "COURSE" },
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  color = Slate700
                ),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
            Text(
              text = "• Section ${item.section}",
              style = MaterialTheme.typography.bodySmall.copy(color = Slate500)
            )
          }
        }
      }

      HorizontalDivider(color = Slate100)

      // 3. Room Location & Faculty Instructor
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Room Location with click to view building directions
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = Color(0xFFF1F5F9),
          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
          modifier = Modifier.clickable { onOpenDirections() }
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Rose600, modifier = Modifier.size(16.dp))
            Text(
              text = item.room,
              style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Bold,
                color = Slate800
              )
            )
            Icon(Icons.Default.HelpOutline, contentDescription = "Directions", tint = Slate400, modifier = Modifier.size(12.dp))
          }
        }

        // Faculty Name
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Icon(Icons.Default.Person, contentDescription = null, tint = Slate500, modifier = Modifier.size(16.dp))
          Text(
            text = item.facultyName,
            style = MaterialTheme.typography.bodySmall.copy(
              fontWeight = FontWeight.Medium,
              color = Slate700
            )
          )
        }
      }

      // 4. Quick Action Button for Today's Classes
      if (isToday) {
        if (hasActiveSession) {
          Button(
            onClick = onScanQR,
            colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(40.dp)
              .testTag("scan_for_class_${item.id}")
          ) {
            Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Scan Class Attendance (Live QR Active)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
          }
        } else {
          OutlinedButton(
            onClick = onScanQR,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(40.dp)
              .testTag("scan_for_class_${item.id}")
          ) {
            Icon(Icons.Default.HourglassEmpty, contentDescription = null, tint = Slate500, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("QR Not Started (Waiting for Faculty)", fontWeight = FontWeight.Medium, fontSize = 12.sp, color = Slate600)
          }
        }
      }
    }
  }
}

/**
 * Dialog displaying building & room location directions
 */
@Composable
fun RoomLocationDirectionsDialog(
  timetable: TimetableEntity,
  onDismiss: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Box(
        modifier = Modifier
          .size(48.dp)
          .clip(CircleShape)
          .background(Rose50),
        contentAlignment = Alignment.Center
      ) {
        Icon(Icons.Default.Room, contentDescription = null, tint = Rose600, modifier = Modifier.size(28.dp))
      }
    },
    title = {
      Text(
        text = "Room Location: ${timetable.room}",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "${timetable.subjectName} (${timetable.startTime} - ${timetable.endTime})",
          style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = Indigo700)
        )

        Surface(
          shape = RoundedCornerShape(8.dp),
          color = Slate50,
          border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
              text = "📍 Building: Academic Block A",
              style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
            )
            val floor = when {
              timetable.room.contains("10", ignoreCase = true) -> "Ground / 1st Floor"
              timetable.room.contains("20", ignoreCase = true) -> "2nd Floor (Take West Elevator)"
              timetable.room.contains("30", ignoreCase = true) -> "3rd Floor (Near Central Staircase)"
              timetable.room.contains("Hall", ignoreCase = true) -> "Main Auditorium Complex (Wing B)"
              else -> "2nd Floor, Engineering Wing"
            }
            Text(
              text = "🏢 Floor: $floor",
              style = MaterialTheme.typography.bodySmall
            )
            Text(
              text = "👨‍🏫 Faculty: ${timetable.facultyName}",
              style = MaterialTheme.typography.bodySmall
            )
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Close")
      }
    }
  )
}

/**
 * Compact Today's Schedule Card section for embedding directly on the Dashboard
 */
@Composable
fun TodaysScheduleSection(
  timetables: List<TimetableEntity>,
  records: List<AttendanceRecordEntity> = emptyList(),
  activeSessions: List<AttendanceSessionEntity> = emptyList(),
  onScanQR: () -> Unit = {},
  onScanQRForSubject: (String) -> Unit = { onScanQR() },
  onSeeAll: () -> Unit = {}
) {
  val calendar = remember { Calendar.getInstance() }
  val currentDayName = remember {
    when (calendar.get(Calendar.DAY_OF_WEEK)) {
      Calendar.MONDAY -> "Monday"
      Calendar.TUESDAY -> "Tuesday"
      Calendar.WEDNESDAY -> "Wednesday"
      Calendar.THURSDAY -> "Thursday"
      Calendar.FRIDAY -> "Friday"
      Calendar.SATURDAY -> "Saturday"
      else -> "Sunday"
    }
  }

  val isWeekend = currentDayName == "Saturday" || currentDayName == "Sunday"
  val targetDay = if (isWeekend) "Monday" else currentDayName

  val todayClasses = remember(timetables, targetDay) {
    timetables.filter { it.dayOfWeek.equals(targetDay, ignoreCase = true) }.sortedBy { it.startTime }
  }

  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Icon(Icons.Default.Today, contentDescription = null, tint = Indigo600, modifier = Modifier.size(20.dp))
        Text(
          text = "Today's Schedule",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = Indigo50
        ) {
          Text(
            text = "$targetDay (${todayClasses.size})",
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Bold,
              color = Indigo700
            ),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
          )
        }
      }

      TextButton(
        onClick = onSeeAll,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        modifier = Modifier.testTag("view_full_schedule_button")
      ) {
        Text("View Full", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Indigo600)
        Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp))
      }
    }

    if (todayClasses.isEmpty()) {
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Box(modifier = Modifier.padding(16.dp)) {
          Text("No classes scheduled for today.", style = MaterialTheme.typography.bodySmall.copy(color = Slate600))
        }
      }
    } else {
      todayClasses.take(3).forEach { item ->
        val record = records.firstOrNull {
          it.subjectName.equals(item.subjectName, ignoreCase = true) ||
            it.subjectId.equals(item.subjectId, ignoreCase = true)
        }
        val hasActive = activeSessions.any { it.subjectId.equals(item.subjectId, ignoreCase = true) }
        ClassScheduleCard(
          item = item,
          attendanceStatus = record?.status,
          isToday = !isWeekend,
          hasActiveSession = hasActive,
          onScanQR = {
            onScanQRForSubject(item.subjectId)
            onScanQR()
          }
        )
      }
    }
  }
}
