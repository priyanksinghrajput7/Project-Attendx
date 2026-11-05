package com.example.ui.screens.student

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import com.example.data.model.LeaveRequestModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * LeaveRequestScreen allows students to input their name, roll number,
 * reason for leave, and date range. It maintains a state list of pending
 * requests and connects to local storage.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaveRequestScreen(
  initialName: String = "",
  initialRollNumber: String = "",
  existingPendingRequests: List<LeaveRequestModel> = emptyList(),
  onSubmitRequest: (LeaveRequestModel) -> Unit = {},
  onDeleteRequest: (String) -> Unit = {},
  onBack: () -> Unit = {}
) {
  val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
  val todayCal = remember { Calendar.getInstance() }
  val todayStr = remember { dateFormat.format(todayCal.time) }
  val tomorrowCal = remember {
    Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
  }
  val tomorrowStr = remember { dateFormat.format(tomorrowCal.time) }

  // Form State
  var studentName by remember { mutableStateOf(initialName) }
  var rollNumber by remember { mutableStateOf(initialRollNumber) }
  var startDate by remember { mutableStateOf(todayStr) }
  var endDate by remember { mutableStateOf(tomorrowStr) }
  var reason by remember { mutableStateOf("") }
  var leaveType by remember { mutableStateOf("SICK_LEAVE") }

  // Validation & Feedback state
  var formError by remember { mutableStateOf<String?>(null) }
  var successMessage by remember { mutableStateOf<String?>(null) }

  // Local state list to keep track of pending requests
  val pendingRequests = remember {
    mutableStateListOf<LeaveRequestModel>().apply {
      addAll(existingPendingRequests.filter { it.status.equals("PENDING", ignoreCase = true) })
    }
  }

  // Synchronize when existing requests change
  LaunchedEffect(existingPendingRequests) {
    val pendingOnly = existingPendingRequests.filter { it.status.equals("PENDING", ignoreCase = true) }
    if (pendingOnly.isNotEmpty() && pendingRequests.isEmpty()) {
      pendingRequests.clear()
      pendingRequests.addAll(pendingOnly)
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "Leave Request",
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
              text = "Notify faculty & track leave approvals",
              style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
          }
        },
        navigationIcon = {
          IconButton(
            onClick = onBack,
            modifier = Modifier.testTag("leave_request_back_button")
          ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        )
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
      // Success Feedback Banner
      item {
        AnimatedVisibility(
          visible = successMessage != null,
          enter = fadeIn(),
          exit = fadeOut()
        ) {
          successMessage?.let { msg ->
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = Emerald50,
              border = androidx.compose.foundation.BorderStroke(1.dp, Emerald200),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Emerald600)
                Text(
                  text = msg,
                  style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Emerald900
                  ),
                  modifier = Modifier.weight(1f)
                )
                IconButton(
                  onClick = { successMessage = null },
                  modifier = Modifier.size(24.dp)
                ) {
                  Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Emerald700)
                }
              }
            }
          }
        }
      }

      // Input Form Card
      item {
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(40.dp)
                  .clip(CircleShape)
                  .background(Indigo100),
                contentAlignment = Alignment.Center
              ) {
                Icon(Icons.Default.EditCalendar, contentDescription = null, tint = Indigo600)
              }
              Column {
                Text(
                  text = "New Leave Application",
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                  text = "Fill in your details and leave period",
                  style = MaterialTheme.typography.bodySmall.copy(color = Slate500)
                )
              }
            }

            HorizontalDivider(color = Slate200)

            // 1. Student Name
            OutlinedTextField(
              value = studentName,
              onValueChange = {
                studentName = it
                formError = null
              },
              label = { Text("Student Name") },
              placeholder = { Text("e.g. Priyank Singh Rajput") },
              leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Indigo500) },
              singleLine = true,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("leave_name_input")
            )

            // 2. Roll Number
            OutlinedTextField(
              value = rollNumber,
              onValueChange = {
                rollNumber = it
                formError = null
              },
              label = { Text("Roll Number") },
              placeholder = { Text("e.g. CSE24-001") },
              leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = Indigo500) },
              singleLine = true,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("leave_roll_input")
            )

            // 3. Leave Type Chips
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text(
                text = "Category of Leave",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Slate700)
              )
              LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val types = listOf(
                  Triple("SICK_LEAVE", "Sick 🤒", Indigo600),
                  Triple("CASUAL_LEAVE", "Casual 🏖️", Amber600),
                  Triple("DUTY_LEAVE", "Duty / Event 🏆", Emerald600),
                  Triple("MEDICAL", "Medical 🏥", Rose600)
                )
                items(types) { (typeKey, label, color) ->
                  val isSelected = leaveType == typeKey
                  FilterChip(
                    selected = isSelected,
                    onClick = { leaveType = typeKey },
                    label = { Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                      selectedContainerColor = color,
                      selectedLabelColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                  )
                }
              }
            }

            // 4. Date Range: Start Date & End Date
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text(
                text = "Date Range (YYYY-MM-DD)",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Slate700)
              )

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                OutlinedTextField(
                  value = startDate,
                  onValueChange = {
                    startDate = it
                    formError = null
                  },
                  label = { Text("Start Date") },
                  placeholder = { Text("YYYY-MM-DD") },
                  leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Indigo500, modifier = Modifier.size(18.dp)) },
                  singleLine = true,
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier
                    .weight(1f)
                    .testTag("leave_start_date_input")
                )

                OutlinedTextField(
                  value = endDate,
                  onValueChange = {
                    endDate = it
                    formError = null
                  },
                  label = { Text("End Date") },
                  placeholder = { Text("YYYY-MM-DD") },
                  leadingIcon = { Icon(Icons.Default.Event, contentDescription = null, tint = Indigo500, modifier = Modifier.size(18.dp)) },
                  singleLine = true,
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier
                    .weight(1f)
                    .testTag("leave_end_date_input")
                )
              }

              // Quick Date Range Presets
              LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 4.dp)
              ) {
                val presets = listOf(
                  "Today Only" to Pair(todayStr, todayStr),
                  "Tomorrow" to Pair(tomorrowStr, tomorrowStr),
                  "2 Days" to Pair(todayStr, tomorrowStr),
                  "3 Days" to Pair(
                    todayStr,
                    dateFormat.format(Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 2) }.time)
                  ),
                  "1 Week" to Pair(
                    todayStr,
                    dateFormat.format(Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 6) }.time)
                  )
                )

                items(presets) { (label, range) ->
                  SuggestionChip(
                    onClick = {
                      startDate = range.first
                      endDate = range.second
                    },
                    label = { Text(label, fontSize = 11.sp) },
                    shape = RoundedCornerShape(8.dp)
                  )
                }
              }
            }

            // 5. Reason for Leave
            OutlinedTextField(
              value = reason,
              onValueChange = {
                reason = it
                formError = null
              },
              label = { Text("Reason for Leave") },
              placeholder = { Text("Describe the reason to inform faculty clearly...") },
              minLines = 3,
              maxLines = 5,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("leave_reason_input")
            )

            // Reason Suggestions
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              val commonReasons = listOf(
                "Viral fever & bed rest",
                "Hospital / Doctor checkup",
                "Attending family wedding",
                "College Hackathon delegation",
                "Severe migraine / headache"
              )
              items(commonReasons) { r ->
                AssistChip(
                  onClick = { reason = r },
                  label = { Text(r, fontSize = 11.sp) },
                  shape = RoundedCornerShape(8.dp)
                )
              }
            }

            // Validation Error Message
            formError?.let { err ->
              Text(
                text = "⚠ $err",
                style = MaterialTheme.typography.bodySmall.copy(
                  color = Rose600,
                  fontWeight = FontWeight.Bold
                )
              )
            }

            // Submit Button
            Button(
              onClick = {
                val trimmedName = studentName.trim()
                val trimmedRoll = rollNumber.trim()
                val trimmedReason = reason.trim()
                val trimmedStart = startDate.trim()
                val trimmedEnd = endDate.trim()

                when {
                  trimmedName.isEmpty() -> formError = "Please enter student name."
                  trimmedRoll.isEmpty() -> formError = "Please enter roll number."
                  trimmedStart.isEmpty() || trimmedEnd.isEmpty() -> formError = "Please provide both start and end dates."
                  trimmedReason.length < 5 -> formError = "Please enter a detailed reason (at least 5 characters)."
                  else -> {
                    val days = calculateDaysDifference(trimmedStart, trimmedEnd)
                    val newRequest = LeaveRequestModel(
                      id = UUID.randomUUID().toString(),
                      studentName = trimmedName,
                      rollNumber = trimmedRoll,
                      reason = trimmedReason,
                      startDate = trimmedStart,
                      endDate = trimmedEnd,
                      numberOfDays = days,
                      status = "PENDING",
                      leaveType = leaveType,
                      appliedAt = System.currentTimeMillis()
                    )

                    // Add to local state list
                    pendingRequests.add(0, newRequest)

                    // Trigger storage callback
                    onSubmitRequest(newRequest)

                    // Reset form and show success
                    reason = ""
                    formError = null
                    successMessage = "Leave request successfully submitted for $trimmedName ($trimmedRoll)!"
                  }
                }
              },
              colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("submit_leave_request_btn")
            ) {
              Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Submit Leave Request", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
          }
        }
      }

      // Section: Pending Leave Requests List
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
            Text(
              text = "Pending Leave Requests",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = Amber100
            ) {
              Text(
                text = "${pendingRequests.size} Pending",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  color = Amber800
                ),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
              )
            }
          }
        }
      }

      if (pendingRequests.isEmpty()) {
        item {
          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("empty_pending_leaves_card")
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(
                imageVector = Icons.Default.EventAvailable,
                contentDescription = null,
                tint = Indigo500,
                modifier = Modifier.size(36.dp)
              )
              Text(
                text = "No pending leave requests",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
              )
              Text(
                text = "Fill the form above to inform faculty about your absence.",
                style = MaterialTheme.typography.bodySmall.copy(color = Slate500)
              )
            }
          }
        }
      } else {
        items(pendingRequests, key = { it.id }) { req ->
          PendingLeaveItemCard(
            request = req,
            onCancel = {
              pendingRequests.remove(req)
              onDeleteRequest(req.id)
              successMessage = "Leave request for ${req.startDate} has been cancelled."
            }
          )
        }
      }
    }
  }
}

@Composable
private fun PendingLeaveItemCard(
  request: LeaveRequestModel,
  onCancel: () -> Unit
) {
  val typeLabel = when (request.leaveType.uppercase()) {
    "SICK_LEAVE", "SICK" -> "Sick Leave 🤒"
    "MEDICAL" -> "Medical 🏥"
    "DUTY_LEAVE", "DUTY" -> "Duty / Event 🏆"
    "CASUAL_LEAVE", "CASUAL" -> "Casual 🏖️"
    else -> "Personal ⚡"
  }

  Surface(
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surface,
    border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
    shadowElevation = 1.dp,
    modifier = Modifier
      .fillMaxWidth()
      .testTag("pending_leave_card_${request.id}")
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
      ) {
        Column {
          Text(
            text = request.studentName,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
          )
          Text(
            text = "Roll: ${request.rollNumber} • $typeLabel",
            style = MaterialTheme.typography.bodySmall.copy(color = Slate600)
          )
        }

        Surface(
          shape = RoundedCornerShape(16.dp),
          color = Amber50,
          border = androidx.compose.foundation.BorderStroke(1.dp, Amber200)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Icon(Icons.Default.HourglassEmpty, contentDescription = null, tint = Amber700, modifier = Modifier.size(12.dp))
            Text(
              text = "PENDING",
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = Amber800,
                fontSize = 11.sp
              )
            )
          }
        }
      }

      // Date Range Badge
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(8.dp))
          .background(Indigo50)
          .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Icon(Icons.Default.DateRange, contentDescription = null, tint = Indigo700, modifier = Modifier.size(16.dp))
          Text(
            text = "${request.startDate}  →  ${request.endDate}",
            style = MaterialTheme.typography.bodySmall.copy(
              fontWeight = FontWeight.Bold,
              color = Indigo900
            )
          )
        }
        Text(
          text = "${request.numberOfDays} Day${if (request.numberOfDays > 1) "s" else ""}",
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            color = Indigo700
          )
        )
      }

      // Reason quote box
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(8.dp))
          .background(Color(0xFFF8FAFC))
          .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
          .padding(10.dp)
      ) {
        Text(
          text = "Reason for Leave:",
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Slate500)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = request.reason,
          style = MaterialTheme.typography.bodySmall.copy(color = Slate800)
        )
      }

      // Cancel button
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
      ) {
        TextButton(
          onClick = onCancel,
          colors = ButtonDefaults.textButtonColors(contentColor = Rose600),
          modifier = Modifier.testTag("cancel_leave_btn_${request.id}")
        ) {
          Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Cancel Request", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
      }
    }
  }
}

private fun calculateDaysDifference(start: String, end: String): Int {
  return try {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val d1 = sdf.parse(start)
    val d2 = sdf.parse(end)
    if (d1 != null && d2 != null) {
      val diff = d2.time - d1.time
      val days = (diff / (1000 * 60 * 60 * 24)).toInt() + 1
      maxOf(1, days)
    } else 1
  } catch (e: Exception) {
    1
  }
}
