package com.example.ui.screens.student

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.LeaveRequestEntity
import com.example.data.model.UserEntity
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentLeaveScreen(
  student: UserEntity,
  leaves: List<LeaveRequestEntity>,
  facultyList: List<UserEntity>,
  onApplyLeave: (facultyId: String, facultyName: String, leaveType: String, startDate: String, endDate: String, numberOfDays: Int, reason: String, attachmentUri: String?) -> Unit,
  onCancelLeave: (LeaveRequestEntity) -> Unit = {},
  onDeleteLeave: (LeaveRequestEntity) -> Unit = {},
  onBack: () -> Unit
) {
  var selectedTabFilter by remember { mutableStateOf("ALL") }
  var showApplyDialog by remember { mutableStateOf(false) }
  var previewingDocumentUri by remember { mutableStateOf<String?>(null) }

  val pendingCount = leaves.count { it.status.equals("PENDING", ignoreCase = true) }
  val approvedCount = leaves.count { it.status.equals("APPROVED", ignoreCase = true) }
  val rejectedCount = leaves.count { it.status.equals("REJECTED", ignoreCase = true) }
  val cancelledCount = leaves.count { it.status.equals("CANCELLED", ignoreCase = true) }

  val filteredLeaves = remember(leaves, selectedTabFilter) {
    when (selectedTabFilter) {
      "PENDING" -> leaves.filter { it.status.equals("PENDING", ignoreCase = true) }
      "APPROVED" -> leaves.filter { it.status.equals("APPROVED", ignoreCase = true) }
      "REJECTED" -> leaves.filter { it.status.equals("REJECTED", ignoreCase = true) }
      "CANCELLED" -> leaves.filter { it.status.equals("CANCELLED", ignoreCase = true) }
      else -> leaves
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "Leave Applications",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
              text = "Formal academic leave requests & status",
              style = MaterialTheme.typography.bodySmall.copy(color = Slate500)
            )
          }
        },
        navigationIcon = {
          IconButton(
            onClick = onBack,
            modifier = Modifier.testTag("leave_screen_back_btn")
          ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          FilledTonalButton(
            onClick = { showApplyDialog = true },
            colors = ButtonDefaults.filledTonalButtonColors(
              containerColor = Indigo100,
              contentColor = Indigo700
            ),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            modifier = Modifier
              .padding(end = 8.dp)
              .testTag("apply_leave_top_btn")
          ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Apply Leave", fontWeight = FontWeight.Bold, fontSize = 13.sp)
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        )
      )
    },
    floatingActionButton = {
      ExtendedFloatingActionButton(
        onClick = { showApplyDialog = true },
        icon = { Icon(Icons.Default.Add, contentDescription = null) },
        text = { Text("New Leave Request", fontWeight = FontWeight.Bold) },
        containerColor = Indigo600,
        contentColor = Color.White,
        modifier = Modifier.testTag("apply_leave_fab")
      )
    }
  ) { padding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
      contentPadding = PaddingValues(top = 12.dp, bottom = 88.dp)
    ) {

      // Summary Stats Cards Row
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          LeaveStatCard(
            modifier = Modifier.weight(1f),
            title = "Total",
            count = leaves.size,
            containerColor = Slate100,
            textColor = Slate800,
            icon = Icons.Default.EventNote
          )
          LeaveStatCard(
            modifier = Modifier.weight(1f),
            title = "Pending",
            count = pendingCount,
            containerColor = Amber50,
            textColor = Amber700,
            icon = Icons.Default.HourglassEmpty
          )
          LeaveStatCard(
            modifier = Modifier.weight(1f),
            title = "Approved",
            count = approvedCount,
            containerColor = Emerald50,
            textColor = Emerald700,
            icon = Icons.Default.CheckCircle
          )
          LeaveStatCard(
            modifier = Modifier.weight(1f),
            title = if (cancelledCount > 0) "Cancelled" else "Rejected",
            count = if (cancelledCount > 0) cancelledCount else rejectedCount,
            containerColor = if (cancelledCount > 0) Slate100 else Rose50,
            textColor = if (cancelledCount > 0) Slate700 else Rose700,
            icon = if (cancelledCount > 0) Icons.Default.Block else Icons.Default.Cancel
          )
        }
      }

      // Filter Tabs
      item {
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          val filterTabs = buildList {
            add(Triple("ALL", "All Requests", leaves.size))
            add(Triple("PENDING", "Pending", pendingCount))
            add(Triple("APPROVED", "Approved", approvedCount))
            if (rejectedCount > 0) add(Triple("REJECTED", "Rejected", rejectedCount))
            if (cancelledCount > 0) add(Triple("CANCELLED", "Cancelled", cancelledCount))
          }

          items(filterTabs) { (key, label, count) ->
            val isSelected = selectedTabFilter == key
            FilterChip(
              selected = isSelected,
              onClick = { selectedTabFilter = key },
              label = {
                Text(
                  text = "$label ($count)",
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                  fontSize = 13.sp
                )
              },
              leadingIcon = if (isSelected) {
                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
              } else null,
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Indigo600,
                selectedLabelColor = Color.White,
                selectedLeadingIconColor = Color.White
              ),
              border = FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = isSelected,
                borderColor = if (isSelected) Indigo600 else Slate200
              ),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.testTag("leave_filter_$key")
            )
          }
        }
      }

      // Leave items or empty state
      if (filteredLeaves.isEmpty()) {
        item {
          Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            border = BorderStroke(1.dp, Slate200),
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 24.dp)
          ) {
            Column(
              modifier = Modifier.padding(32.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(60.dp)
                  .clip(CircleShape)
                  .background(Indigo50),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.EventAvailable,
                  contentDescription = null,
                  tint = Indigo600,
                  modifier = Modifier.size(32.dp)
                )
              }
              Text(
                text = if (selectedTabFilter == "ALL") "No Leave Requests Yet" else "No $selectedTabFilter Requests",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center
              )
              Text(
                text = "If you need time off for health, medical, family, or duty reasons, submit a leave application to your faculty.",
                style = MaterialTheme.typography.bodyMedium.copy(color = Slate500),
                textAlign = TextAlign.Center
              )
              Button(
                onClick = { showApplyDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(top = 8.dp)
              ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Apply For Leave Now")
              }
            }
          }
        }
      } else {
        items(filteredLeaves, key = { it.id }) { leave ->
          StudentLeaveCard(
            leave = leave,
            onPreviewAttachment = { uri -> previewingDocumentUri = uri },
            onCancel = { onCancelLeave(leave) },
            onDelete = { onDeleteLeave(leave) }
          )
        }
      }
    }
  }

  // Apply Leave Modal Dialog
  if (showApplyDialog) {
    ApplyLeaveDialog(
      student = student,
      facultyList = facultyList,
      onDismiss = { showApplyDialog = false },
      onSubmit = { facultyId, facultyName, leaveType, startDate, endDate, numberOfDays, reason, attachmentUri ->
        onApplyLeave(facultyId, facultyName, leaveType, startDate, endDate, numberOfDays, reason, attachmentUri)
        showApplyDialog = false
      }
    )
  }

  // Document Preview Dialog
  previewingDocumentUri?.let { uri ->
    DocumentPreviewDialog(
      attachmentUri = uri,
      onDismiss = { previewingDocumentUri = null }
    )
  }
}

@Composable
fun LeaveStatCard(
  modifier: Modifier = Modifier,
  title: String,
  count: Int,
  containerColor: Color,
  textColor: Color,
  icon: androidx.compose.ui.graphics.vector.ImageVector
) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = containerColor,
    modifier = modifier
  ) {
    Column(
      modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(18.dp))
      Text(
        text = count.toString(),
        style = MaterialTheme.typography.titleMedium.copy(
          fontWeight = FontWeight.ExtraBold,
          color = textColor
        )
      )
      Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(
          fontWeight = FontWeight.Medium,
          color = textColor.copy(alpha = 0.85f),
          fontSize = 11.sp
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
fun StudentLeaveCard(
  leave: LeaveRequestEntity,
  onPreviewAttachment: (String) -> Unit = {},
  onCancel: () -> Unit = {},
  onDelete: () -> Unit = {}
) {
  var showCancelConfirmDialog by remember { mutableStateOf(false) }

  val isCancelled = leave.status.equals("CANCELLED", ignoreCase = true)
  val isPending = leave.status.equals("PENDING", ignoreCase = true)
  val isApproved = leave.status.equals("APPROVED", ignoreCase = true)
  val isRejected = leave.status.equals("REJECTED", ignoreCase = true)

  val statusStyle = when {
    isApproved -> LeaveStatusStyle(Emerald50, Emerald200, Emerald700, Icons.Default.CheckCircle)
    isRejected -> LeaveStatusStyle(Rose50, Rose200, Rose700, Icons.Default.Cancel)
    isCancelled -> LeaveStatusStyle(Slate100, Slate300, Slate600, Icons.Default.Block)
    else -> LeaveStatusStyle(Amber50, Amber200, Amber700, Icons.Default.HourglassEmpty)
  }

  val typeInfo = when (leave.leaveType.uppercase()) {
    "SICK_LEAVE", "SICK" -> Pair("Medical / Sick Leave 🩺", Indigo600)
    "MEDICAL" -> Pair("Hospital / Emergency 🏥", Rose600)
    "DUTY_LEAVE", "DUTY" -> Pair("Academic / Duty Leave 🎓", Emerald600)
    "CASUAL_LEAVE", "CASUAL" -> Pair("Casual / Personal Leave 🏡", Amber600)
    else -> Pair("Personal / Emergency ⚡", Slate700)
  }

  if (showCancelConfirmDialog) {
    AlertDialog(
      onDismissRequest = { showCancelConfirmDialog = false },
      icon = {
        Icon(Icons.Default.Cancel, contentDescription = null, tint = Rose600, modifier = Modifier.size(32.dp))
      },
      title = {
        Text("Cancel Leave Application?", fontWeight = FontWeight.Bold)
      },
      text = {
        Text("Are you sure you want to cancel this leave application for ${leave.startDate} to ${leave.endDate}? The request will be withdrawn immediately and removed from faculty review.")
      },
      confirmButton = {
        Button(
          onClick = {
            showCancelConfirmDialog = false
            onCancel()
          },
          colors = ButtonDefaults.buttonColors(containerColor = Rose600),
          modifier = Modifier.testTag("confirm_cancel_leave_btn")
        ) {
          Text("Yes, Cancel Leave", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showCancelConfirmDialog = false }) {
          Text("Keep Application", color = Slate600)
        }
      }
    )
  }

  Surface(
    shape = RoundedCornerShape(18.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, if (isCancelled) Slate300 else Slate200),
    shadowElevation = if (isCancelled) 0.dp else 1.dp,
    modifier = Modifier
      .fillMaxWidth()
      .testTag("leave_card_${leave.id}")
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      // Header: Leave Type + Status Pill
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
            text = typeInfo.first,
            style = MaterialTheme.typography.titleSmall.copy(
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
          )
        }

        Surface(
          shape = RoundedCornerShape(20.dp),
          color = statusStyle.bg,
          border = BorderStroke(1.dp, statusStyle.border)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Icon(statusStyle.icon, contentDescription = null, tint = statusStyle.text, modifier = Modifier.size(13.dp))
            Text(
              text = if (isCancelled) "CANCELLED" else leave.status.uppercase(),
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = statusStyle.text,
                fontSize = 11.sp
              )
            )
          }
        }
      }

      // Dates & Duration
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(10.dp))
          .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
          .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Icon(Icons.Default.DateRange, contentDescription = null, tint = Indigo600, modifier = Modifier.size(16.dp))
          Text(
            text = if (leave.startDate == leave.endDate) leave.startDate else "${leave.startDate} → ${leave.endDate}",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
          )
        }

        Surface(
          shape = RoundedCornerShape(8.dp),
          color = Indigo50
        ) {
          Text(
            text = "${leave.numberOfDays} ${if (leave.numberOfDays > 1) "Days" else "Day"}",
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Bold,
              color = Indigo700
            ),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
          )
        }
      }

      // Faculty requested
      if (leave.facultyName.isNotBlank()) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Icon(Icons.Default.Person, contentDescription = null, tint = Slate500, modifier = Modifier.size(15.dp))
          Text(
            text = "Faculty In-Charge: ",
            style = MaterialTheme.typography.bodySmall.copy(color = Slate500)
          )
          Text(
            text = leave.facultyName,
            style = MaterialTheme.typography.bodySmall.copy(
              fontWeight = FontWeight.SemiBold,
              color = Indigo700
            )
          )
        }
      }

      // Student Reason
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(10.dp))
          .background(Color(0xFFF8FAFC))
          .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
          .padding(10.dp)
      ) {
        Text(
          text = "Reason for Leave:",
          style = MaterialTheme.typography.labelSmall.copy(color = Slate500, fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = leave.reason,
          style = MaterialTheme.typography.bodySmall.copy(color = Slate800)
        )
      }

      // Attached Document Proof (if present)
      if (!leave.attachmentUri.isNullOrBlank()) {
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = Indigo50,
          border = BorderStroke(1.dp, Indigo200),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onPreviewAttachment(leave.attachmentUri) }
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(Icons.Default.AttachFile, contentDescription = null, tint = Indigo700, modifier = Modifier.size(16.dp))
              Text(
                text = "Attached Medical/Duty Proof 📎",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Indigo900)
              )
            }
            Text(
              text = "Tap to View 🔍",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Indigo700)
            )
          }
        }
      }

      // Faculty Remarks (if reviewed)
      if (leave.facultyRemarks.isNotBlank() || leave.reviewedAt != null) {
        val isApp = leave.status.equals("APPROVED", ignoreCase = true)
        val isRej = leave.status.equals("REJECTED", ignoreCase = true)
        val bannerBg = when {
          isApp -> Emerald50
          isRej -> Rose50
          else -> Slate100
        }
        val bannerBorder = when {
          isApp -> Emerald200
          isRej -> Rose200
          else -> Slate300
        }
        val bannerText = when {
          isApp -> Emerald900
          isRej -> Rose900
          else -> Slate800
        }

        Surface(
          shape = RoundedCornerShape(10.dp),
          color = bannerBg,
          border = BorderStroke(1.dp, bannerBorder),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Icon(
                imageVector = when {
                  isApp -> Icons.Default.CheckCircle
                  isRej -> Icons.Default.Info
                  else -> Icons.Default.Block
                },
                contentDescription = null,
                tint = when {
                  isApp -> Emerald700
                  isRej -> Rose700
                  else -> Slate600
                },
                modifier = Modifier.size(14.dp)
              )
              Text(
                text = if (isCancelled) "Cancellation Details" else "Faculty Review Remarks",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  color = bannerText
                )
              )
            }
            if (leave.facultyRemarks.isNotBlank()) {
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "\"${leave.facultyRemarks}\"",
                style = MaterialTheme.typography.bodySmall.copy(
                  fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                  color = bannerText
                )
              )
            }
          }
        }
      }

      // Footer: Date applied & cancel/delete actions
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
        val appliedDateStr = remember(leave.appliedAt) { dateFormat.format(Date(leave.appliedAt)) }

        Text(
          text = "Applied: $appliedDateStr",
          style = MaterialTheme.typography.labelSmall.copy(color = Slate400, fontSize = 11.sp)
        )

        if (isPending) {
          FilledTonalButton(
            onClick = { showCancelConfirmDialog = true },
            colors = ButtonDefaults.filledTonalButtonColors(
              containerColor = Rose50,
              contentColor = Rose700
            ),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            modifier = Modifier.testTag("cancel_leave_btn_${leave.id}")
          ) {
            Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Cancel Leave", fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
        } else if (isCancelled) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = Slate100
            ) {
              Text(
                text = "Withdrawn by you",
                style = MaterialTheme.typography.labelSmall.copy(color = Slate600, fontSize = 10.sp),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
            IconButton(
              onClick = onDelete,
              modifier = Modifier.size(28.dp).testTag("delete_leave_btn_${leave.id}")
            ) {
              Icon(Icons.Default.DeleteOutline, contentDescription = "Delete record", tint = Slate400, modifier = Modifier.size(16.dp))
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplyLeaveDialog(
  student: UserEntity,
  facultyList: List<UserEntity>,
  onDismiss: () -> Unit,
  onSubmit: (facultyId: String, facultyName: String, leaveType: String, startDate: String, endDate: String, numberOfDays: Int, reason: String, attachmentUri: String?) -> Unit
) {
  val todayFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
  val todayStr = remember { todayFormat.format(Date()) }

  var selectedFaculty by remember(facultyList) { mutableStateOf(facultyList.firstOrNull()) }
  var expandedFacultyDropdown by remember { mutableStateOf(false) }

  var selectedLeaveType by remember { mutableStateOf("SICK_LEAVE") }
  var startDate by remember { mutableStateOf(todayStr) }
  var endDate by remember { mutableStateOf(todayStr) }
  var numberOfDays by remember { mutableStateOf(1) }
  var reasonText by remember { mutableStateOf("") }
  var selectedAttachmentUri by remember { mutableStateOf<String?>(null) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri ->
    if (uri != null) {
      selectedAttachmentUri = uri.toString()
    }
  }

  val leaveTypes = listOf(
    Pair("SICK_LEAVE", "Sick Leave 🤒"),
    Pair("MEDICAL", "Medical Emergency 🏥"),
    Pair("CASUAL_LEAVE", "Casual / Personal 🏖️"),
    Pair("DUTY_LEAVE", "Duty / Event 🏆"),
    Pair("EMERGENCY", "Emergency ⚡")
  )

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 6.dp,
      modifier = Modifier
        .fillMaxWidth()
        .wrapContentHeight()
        .padding(8.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        // Dialog Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Indigo100),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.EditCalendar, contentDescription = null, tint = Indigo700, modifier = Modifier.size(20.dp))
            }
            Column {
              Text(
                text = "Apply for Leave",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
              )
              Text(
                text = "Student: ${student.name} (${student.rollNo})",
                style = MaterialTheme.typography.bodySmall.copy(color = Slate500)
              )
            }
          }

          IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(32.dp)
          ) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate400)
          }
        }

        HorizontalDivider(color = Slate200)

        // Select Faculty / Teacher
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
            text = "Select Faculty / In-Charge *",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Slate700)
          )
          ExposedDropdownMenuBox(
            expanded = expandedFacultyDropdown,
            onExpandedChange = { expandedFacultyDropdown = !expandedFacultyDropdown }
          ) {
            OutlinedTextField(
              value = selectedFaculty?.let { "${it.name} (${it.department.take(20)})" } ?: "Select Faculty",
              onValueChange = {},
              readOnly = true,
              trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedFacultyDropdown) },
              modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
              shape = RoundedCornerShape(12.dp)
            )
            ExposedDropdownMenu(
              expanded = expandedFacultyDropdown,
              onDismissRequest = { expandedFacultyDropdown = false }
            ) {
              facultyList.forEach { faculty ->
                DropdownMenuItem(
                  text = {
                    Column {
                      Text(faculty.name, fontWeight = FontWeight.SemiBold)
                      Text(
                        "${faculty.designation} • ${faculty.department}",
                        style = MaterialTheme.typography.labelSmall.copy(color = Slate500)
                      )
                    }
                  },
                  onClick = {
                    selectedFaculty = faculty
                    expandedFacultyDropdown = false
                  }
                )
              }
            }
          }
        }

        // Leave Type Choice Chips
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = "Leave Type *",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Slate700)
          )
          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            items(leaveTypes) { (typeKey, label) ->
              val isSelected = selectedLeaveType == typeKey
              FilterChip(
                selected = isSelected,
                onClick = { selectedLeaveType = typeKey },
                label = { Text(label, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = Indigo600,
                  selectedLabelColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp)
              )
            }
          }
        }

        // Dates & Duration
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          OutlinedTextField(
            value = startDate,
            onValueChange = { startDate = it },
            label = { Text("Start Date", fontSize = 12.sp) },
            placeholder = { Text("YYYY-MM-DD") },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
          )

          OutlinedTextField(
            value = endDate,
            onValueChange = { endDate = it },
            label = { Text("End Date", fontSize = 12.sp) },
            placeholder = { Text("YYYY-MM-DD") },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
          )
        }

        // Duration in Days Stepper
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Total Duration (Days):",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
          )
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            FilledTonalIconButton(
              onClick = { if (numberOfDays > 1) numberOfDays-- },
              modifier = Modifier.size(34.dp),
              enabled = numberOfDays > 1
            ) {
              Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
            }
            Text(
              text = "$numberOfDays",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            FilledTonalIconButton(
              onClick = { if (numberOfDays < 30) numberOfDays++ },
              modifier = Modifier.size(34.dp)
            ) {
              Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(16.dp))
            }
          }
        }

        // Reason Input
        OutlinedTextField(
          value = reasonText,
          onValueChange = {
            reasonText = it
            errorMessage = null
          },
          label = { Text("Reason for Leave *") },
          placeholder = { Text("Explain clearly why you need leave (e.g., suffering from high fever, family emergency)...") },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("leave_reason_input"),
          minLines = 2,
          maxLines = 4,
          shape = RoundedCornerShape(12.dp)
        )

        // Medical Certificate / Supporting Proof Attachment
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = "Medical Certificate / Supporting Proof (Optional)",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Slate700)
          )

          if (selectedAttachmentUri != null) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = Emerald50,
              border = BorderStroke(1.dp, Emerald200),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(10.dp),
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
                      .size(44.dp)
                      .clip(RoundedCornerShape(8.dp))
                      .background(Emerald100),
                    contentAlignment = Alignment.Center
                  ) {
                    AsyncImage(
                      model = selectedAttachmentUri,
                      contentDescription = "Certificate Thumbnail",
                      modifier = Modifier.fillMaxSize(),
                      contentScale = ContentScale.Crop
                    )
                  }
                  Column {
                    Text(
                      text = "Document Attached ✅",
                      style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Emerald900)
                    )
                    Text(
                      text = "Proof ready for faculty verification",
                      style = MaterialTheme.typography.labelSmall.copy(color = Emerald700)
                    )
                  }
                }
                IconButton(onClick = { selectedAttachmentUri = null }) {
                  Icon(Icons.Default.Close, contentDescription = "Remove", tint = Rose600, modifier = Modifier.size(18.dp))
                }
              }
            }
          } else {
            OutlinedButton(
              onClick = {
                photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
              },
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(12.dp),
              border = BorderStroke(1.dp, Indigo200)
            ) {
              Icon(Icons.Default.AttachFile, contentDescription = null, tint = Indigo600, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Attach Doctor Certificate / Event Slip", color = Indigo700, fontSize = 13.sp)
            }
          }
        }

        errorMessage?.let { err ->
          Text(
            text = err,
            style = MaterialTheme.typography.labelSmall.copy(color = Rose600)
          )
        }

        // Action Buttons
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          OutlinedButton(
            onClick = onDismiss,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f)
          ) {
            Text("Cancel")
          }

          Button(
            onClick = {
              if (selectedFaculty == null) {
                errorMessage = "Please select a faculty member."
                return@Button
              }
              if (reasonText.trim().length < 5) {
                errorMessage = "Please provide a valid reason (min 5 characters)."
                return@Button
              }
              onSubmit(
                selectedFaculty!!.id,
                selectedFaculty!!.name,
                selectedLeaveType,
                startDate.trim(),
                endDate.trim(),
                numberOfDays,
                reasonText.trim(),
                selectedAttachmentUri
              )
            },
            colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .weight(1.5f)
              .testTag("submit_leave_btn")
          ) {
            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Submit Request", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

@Composable
fun DocumentPreviewDialog(
  attachmentUri: String,
  onDismiss: () -> Unit
) {
  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 8.dp,
      modifier = Modifier
        .fillMaxWidth()
        .wrapContentHeight()
        .padding(8.dp)
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
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.AttachFile, contentDescription = null, tint = Indigo600)
            Text(
              text = "Attached Certificate / Document",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
          }
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close")
          }
        }

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF1F5F9))
            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(12.dp)),
          contentAlignment = Alignment.Center
        ) {
          AsyncImage(
            model = attachmentUri,
            contentDescription = "Full Certificate Preview",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
          )
        }

        Text(
          text = "Official proof submitted for leave sanction.",
          style = MaterialTheme.typography.bodySmall.copy(color = Slate500),
          textAlign = TextAlign.Center,
          modifier = Modifier.fillMaxWidth()
        )

        Button(
          onClick = onDismiss,
          modifier = Modifier.fillMaxWidth(),
          colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
          shape = RoundedCornerShape(10.dp)
        ) {
          Text("Close Preview")
        }
      }
    }
  }
}

data class LeaveStatusStyle(
  val bg: Color,
  val border: Color,
  val text: Color,
  val icon: androidx.compose.ui.graphics.vector.ImageVector
)
