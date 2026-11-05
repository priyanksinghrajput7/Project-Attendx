package com.example.ui.screens.notice

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.NoticeEntity
import com.example.data.model.UserEntity
import com.example.ui.screens.student.DocumentPreviewDialog
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoticeBoardScreen(
  user: UserEntity?,
  notices: List<NoticeEntity>,
  onBack: () -> Unit,
  onPostNotice: (
    title: String,
    content: String,
    category: String,
    targetAudience: String,
    priority: String,
    attachmentUri: String?,
    isPinned: Boolean,
    sendNotification: Boolean
  ) -> Unit = { _, _, _, _, _, _, _, _ -> },
  onDeleteNotice: (NoticeEntity) -> Unit = {},
  onTogglePin: (NoticeEntity) -> Unit = {},
  onScanSession: (() -> Unit)? = null
) {
  val canPostNotice = user?.role in listOf("ADMIN", "FACULTY")
  val isAdmin = user?.role == "ADMIN"
  var searchQuery by remember { mutableStateOf("") }
  var selectedCategory by remember { mutableStateOf("ALL") }
  var showCreateDialog by remember { mutableStateOf(false) }
  var previewingAttachmentUri by remember { mutableStateOf<String?>(null) }
  var viewingNoticeDetail by remember { mutableStateOf<NoticeEntity?>(null) }

  val categories = listOf("ALL", "PINNED", "EXAM", "ACADEMIC", "URGENT", "HOLIDAY", "EVENT")

  val filteredNotices = remember(notices, searchQuery, selectedCategory) {
    notices.filter { notice ->
      val matchesSearch = searchQuery.isBlank() ||
        notice.title.contains(searchQuery, ignoreCase = true) ||
        notice.content.contains(searchQuery, ignoreCase = true) ||
        notice.postedBy.contains(searchQuery, ignoreCase = true) ||
        notice.category.contains(searchQuery, ignoreCase = true)

      val matchesCategory = when (selectedCategory) {
        "ALL" -> true
        "PINNED" -> notice.isPinned
        else -> notice.category.equals(selectedCategory, ignoreCase = true)
      }
      matchesSearch && matchesCategory
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = "Campus Notice Board",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("📢", fontSize = 16.sp)
            }
            Text(
              text = if (user?.role == "STUDENT") "Official circulars & department updates"
                     else "Broadcast circulars & notify students",
              style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
          }
        },
        navigationIcon = {
          IconButton(onClick = onBack, modifier = Modifier.testTag("notice_back_button")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          if (canPostNotice) {
            FilledTonalButton(
              onClick = { showCreateDialog = true },
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = Indigo100,
                contentColor = Indigo700
              ),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
              modifier = Modifier
                .padding(end = 8.dp)
                .testTag("post_notice_top_button")
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Post Notice", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      )
    },
    floatingActionButton = {
      if (canPostNotice) {
        ExtendedFloatingActionButton(
          onClick = { showCreateDialog = true },
          containerColor = Indigo600,
          contentColor = Color.White,
          icon = { Icon(Icons.Default.Campaign, contentDescription = null) },
          text = { Text("Publish Notice 📢", fontWeight = FontWeight.Bold) },
          modifier = Modifier.testTag("post_notice_fab")
        )
      }
    }
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
    ) {
      // Search Bar
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text("Search notices by title, exam, faculty...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Indigo600) },
        trailingIcon = {
          if (searchQuery.isNotEmpty()) {
            IconButton(onClick = { searchQuery = "" }) {
              Icon(Icons.Default.Clear, contentDescription = "Clear")
            }
          }
        },
        shape = RoundedCornerShape(14.dp),
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp)
          .testTag("notice_search_field")
      )

      // Category Chips
      LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(categories) { cat ->
          val isSelected = selectedCategory == cat
          val label = when (cat) {
            "ALL" -> "All (${notices.size})"
            "PINNED" -> "📌 Pinned (${notices.count { it.isPinned }})"
            "EXAM" -> "Exams"
            "ACADEMIC" -> "Academic"
            "URGENT" -> "🚨 Urgent"
            "HOLIDAY" -> "Holidays"
            "EVENT" -> "Events"
            else -> cat
          }
          FilterChip(
            selected = isSelected,
            onClick = { selectedCategory = cat },
            label = { Text(label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = Indigo600,
              selectedLabelColor = Color.White
            ),
            shape = RoundedCornerShape(20.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

      // Notices List
      if (filteredNotices.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
          contentAlignment = Alignment.Center
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Box(
              modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Indigo50),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                Icons.Outlined.Campaign,
                contentDescription = null,
                tint = Indigo600,
                modifier = Modifier.size(36.dp)
              )
            }
            Text(
              text = "No Notices Found",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
              text = if (searchQuery.isNotEmpty()) "Try adjusting your search terms or filters"
                     else "No official circulars have been published in this category yet.",
              style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
              textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            if (canPostNotice) {
              Button(
                onClick = { showCreateDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                shape = RoundedCornerShape(10.dp)
              ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Publish First Notice")
              }
            }
          }
        }
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          items(filteredNotices, key = { it.id }) { notice ->
            NoticeItemCard(
              notice = notice,
              currentUser = user,
              isAdmin = isAdmin,
              onInspectAttachment = { uri -> previewingAttachmentUri = uri },
              onClickNotice = { viewingNoticeDetail = notice },
              onTogglePin = { onTogglePin(notice) },
              onDelete = { onDeleteNotice(notice) },
              onScanSession = onScanSession
            )
          }
        }
      }
    }
  }

  // Create Notice Dialog
  if (showCreateDialog) {
    CreateNoticeDialog(
      currentUser = user,
      onDismiss = { showCreateDialog = false },
      onPublish = { title, content, category, targetAudience, priority, attachmentUri, isPinned, sendNotification ->
        onPostNotice(title, content, category, targetAudience, priority, attachmentUri, isPinned, sendNotification)
        showCreateDialog = false
      }
    )
  }

  // Notice Detail Dialog
  viewingNoticeDetail?.let { notice ->
    NoticeDetailDialog(
      notice = notice,
      onDismiss = { viewingNoticeDetail = null },
      onInspectAttachment = { uri -> previewingAttachmentUri = uri }
    )
  }

  // Document Zoom / Full Preview Dialog
  previewingAttachmentUri?.let { uri ->
    DocumentPreviewDialog(
      attachmentUri = uri,
      onDismiss = { previewingAttachmentUri = null }
    )
  }
}

@Composable
fun NoticeItemCard(
  notice: NoticeEntity,
  currentUser: UserEntity?,
  isAdmin: Boolean,
  onInspectAttachment: (String) -> Unit,
  onClickNotice: () -> Unit,
  onTogglePin: () -> Unit,
  onDelete: () -> Unit,
  onScanSession: (() -> Unit)? = null
) {
  val context = LocalContext.current
  val canDelete = isAdmin || (currentUser?.name == notice.postedBy) || (currentUser?.id == notice.authorId)
  val formattedDate = remember(notice.postedAt) {
    SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(notice.postedAt))
  }

  val (catBg, catText) = when (notice.category.uppercase()) {
    "EXAM" -> Pair(Color(0xFFEDE9FE), Color(0xFF6D28D9)) // Purple
    "URGENT" -> Pair(Rose50, Rose700)
    "ACADEMIC" -> Pair(Indigo50, Indigo700)
    "HOLIDAY" -> Pair(Emerald50, Emerald700)
    "EVENT" -> Pair(Amber50, Amber700)
    else -> Pair(Slate100, Slate700)
  }

  val isUrgent = notice.priority.equals("URGENT", ignoreCase = true)

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClickNotice() }
      .testTag("notice_card_${notice.id}"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (notice.isPinned) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                       else MaterialTheme.colorScheme.surface
    ),
    border = BorderStroke(
      width = if (notice.isPinned) 1.5.dp else if (isUrgent) 1.dp else 0.5.dp,
      color = if (notice.isPinned) Amber400 else if (isUrgent) Rose300 else Slate200
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = if (notice.isPinned) 3.dp else 1.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      // Header: Pinned status, Category tag, Priority badge, and Menu actions
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          if (notice.isPinned) {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = Amber100,
              contentColor = Amber800
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
              ) {
                Icon(Icons.Default.PushPin, contentDescription = null, modifier = Modifier.size(12.dp))
                Text("PINNED", fontSize = 10.sp, fontWeight = FontWeight.Black)
              }
            }
          }

          Surface(
            shape = RoundedCornerShape(6.dp),
            color = catBg,
            contentColor = catText
          ) {
            Text(
              text = notice.category.uppercase(),
              modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold
            )
          }

          if (isUrgent) {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = Rose100,
              contentColor = Rose700
            ) {
              Text(
                text = "URGENT ⚠️",
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold
              )
            }
          }

          // Target audience
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = Slate100,
            contentColor = Slate700
          ) {
            Text(
              text = if (notice.targetAudience == "ALL") "All Campus" else notice.targetAudience,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
              fontSize = 10.sp,
              fontWeight = FontWeight.Medium
            )
          }
        }

        // Action icons for Admin / Author
        Row(verticalAlignment = Alignment.CenterVertically) {
          if (isAdmin) {
            IconButton(
              onClick = onTogglePin,
              modifier = Modifier.size(28.dp)
            ) {
              Icon(
                if (notice.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                contentDescription = "Pin/Unpin",
                tint = if (notice.isPinned) Amber600 else Slate400,
                modifier = Modifier.size(16.dp)
              )
            }
          }

          if (canDelete) {
            IconButton(
              onClick = onDelete,
              modifier = Modifier.size(28.dp)
            ) {
              Icon(
                Icons.Outlined.Delete,
                contentDescription = "Delete Notice",
                tint = Rose500,
                modifier = Modifier.size(16.dp)
              )
            }
          }
        }
      }

      // Title
      Text(
        text = notice.title,
        style = MaterialTheme.typography.titleMedium.copy(
          fontWeight = FontWeight.Bold,
          lineHeight = 22.sp
        ),
        color = MaterialTheme.colorScheme.onSurface
      )

      // Content preview
      Text(
        text = notice.content,
        style = MaterialTheme.typography.bodyMedium.copy(
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          lineHeight = 20.sp
        ),
        maxLines = 3,
        overflow = TextOverflow.Ellipsis
      )

      // Attachment preview if present
      if (!notice.attachmentUri.isNullOrBlank()) {
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = Indigo50,
          border = BorderStroke(1.dp, Indigo200),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onInspectAttachment(notice.attachmentUri) }
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
                text = "Attached Official Circular / Document",
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

      // Quick action for Live QR Session notices
      if (notice.title.contains("Live QR Session", ignoreCase = true) || notice.category.equals("QR_SESSION", ignoreCase = true)) {
        Button(
          onClick = { onScanSession?.invoke() },
          colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .testTag("scan_from_notice_${notice.id}")
        ) {
          Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Scan with Camera", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
      }

      HorizontalDivider(color = Slate200.copy(alpha = 0.5f), thickness = 0.5.dp)

      // Footer: Author & Timestamp & Share button
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Box(
            modifier = Modifier
              .size(24.dp)
              .clip(CircleShape)
              .background(if (notice.authorRole == "ADMIN") Indigo600 else Emerald600),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = notice.postedBy.take(1).uppercase(),
              color = Color.White,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            )
          }
          Column {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(
                text = notice.postedBy,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
              )
              Surface(
                shape = RoundedCornerShape(4.dp),
                color = if (notice.authorRole == "ADMIN") Indigo100 else Emerald100
              ) {
                Text(
                  text = notice.authorRole,
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (notice.authorRole == "ADMIN") Indigo700 else Emerald700,
                  modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
              }
            }
            Text(
              text = formattedDate,
              style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
            )
          }
        }

        // Share button
        IconButton(
          onClick = {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
              type = "text/plain"
              putExtra(Intent.EXTRA_SUBJECT, notice.title)
              putExtra(
                Intent.EXTRA_TEXT,
                "📢 *CAMPUS NOTICE: ${notice.title}*\n\n${notice.content}\n\nPosted by: ${notice.postedBy} (${notice.authorRole})\nDate: $formattedDate"
              )
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Campus Notice"))
          },
          modifier = Modifier.size(28.dp)
        ) {
          Icon(
            Icons.Default.Share,
            contentDescription = "Share Notice",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
          )
        }
      }
    }
  }
}

@Composable
fun CreateNoticeDialog(
  currentUser: UserEntity?,
  onDismiss: () -> Unit,
  onPublish: (
    title: String,
    content: String,
    category: String,
    targetAudience: String,
    priority: String,
    attachmentUri: String?,
    isPinned: Boolean,
    sendNotification: Boolean
  ) -> Unit
) {
  var title by remember { mutableStateOf("") }
  var content by remember { mutableStateOf("") }
  var category by remember { mutableStateOf("ACADEMIC") }
  var targetAudience by remember { mutableStateOf("ALL") }
  var priority by remember { mutableStateOf("NORMAL") }
  var isPinned by remember { mutableStateOf(false) }
  var sendNotification by remember { mutableStateOf(true) }
  var selectedAttachmentUri by remember { mutableStateOf<String?>(null) }

  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri: Uri? ->
    if (uri != null) {
      selectedAttachmentUri = uri.toString()
    }
  }

  val categories = listOf("ACADEMIC", "EXAM", "HOLIDAY", "EVENT", "ATTENDANCE", "URGENT")
  val audiences = listOf("ALL", "CSE-4A", "CSE-4B", "STUDENTS", "FACULTY")
  val priorities = listOf("NORMAL", "HIGH", "URGENT")

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 6.dp,
      modifier = Modifier
        .fillMaxWidth()
        .wrapContentHeight()
        .padding(6.dp)
    ) {
      Column(
        modifier = Modifier
          .padding(20.dp)
          .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Header
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
              Icon(Icons.Default.Campaign, contentDescription = null, tint = Indigo600, modifier = Modifier.size(20.dp))
            }
            Column {
              Text(
                text = "Publish Notice 📢",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
              )
              Text(
                text = "Posted as: ${currentUser?.name ?: "Faculty"}",
                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
              )
            }
          }
          IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close")
          }
        }

        // Notice Title
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Notice Title *") },
          placeholder = { Text("e.g., Mid-Term Exam Seating Plan & Timetable") },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("notice_input_title"),
          singleLine = true,
          shape = RoundedCornerShape(12.dp)
        )

        // Notice Content
        OutlinedTextField(
          value = content,
          onValueChange = { content = it },
          label = { Text("Notice Announcement Details *") },
          placeholder = { Text("Type complete notice details, guidelines, or instructions...") },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("notice_input_content"),
          minLines = 3,
          maxLines = 5,
          shape = RoundedCornerShape(12.dp)
        )

        // Category Selection
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(text = "Category:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
          LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(categories) { cat ->
              FilterChip(
                selected = category == cat,
                onClick = { category = cat },
                label = { Text(cat, fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = Indigo600,
                  selectedLabelColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp)
              )
            }
          }
        }

        // Target Audience & Priority
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // Audience
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = "Target Audience:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              items(audiences) { aud ->
                FilterChip(
                  selected = targetAudience == aud,
                  onClick = { targetAudience = aud },
                  label = { Text(aud, fontSize = 10.sp) },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Emerald600,
                    selectedLabelColor = Color.White
                  ),
                  shape = RoundedCornerShape(8.dp)
                )
              }
            }
          }
        }

        // Priority & Pinned
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Text("Priority:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
            priorities.forEach { p ->
              FilterChip(
                selected = priority == p,
                onClick = { priority = p },
                label = { Text(p, fontSize = 10.sp) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = if (p == "URGENT") Rose600 else Indigo600,
                  selectedLabelColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp)
              )
            }
          }
        }

        // Optional Attachment / Circular slip
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = Slate50,
          border = BorderStroke(1.dp, Slate200),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.AttachFile, contentDescription = null, tint = Indigo600, modifier = Modifier.size(16.dp))
                Text(
                  text = if (selectedAttachmentUri == null) "Attach Circular Slip / PDF" else "Attachment Added ✅",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                )
              }
              OutlinedButton(
                onClick = {
                  photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                  )
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                shape = RoundedCornerShape(8.dp)
              ) {
                Text(if (selectedAttachmentUri == null) "Choose File" else "Change", fontSize = 11.sp)
              }
            }

            if (selectedAttachmentUri != null) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
              ) {
                AsyncImage(
                  model = selectedAttachmentUri,
                  contentDescription = "Attachment preview",
                  modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(6.dp)),
                  contentScale = ContentScale.Crop
                )
                TextButton(onClick = { selectedAttachmentUri = null }) {
                  Text("Remove", color = Rose600, fontSize = 11.sp)
                }
              }
            }
          }
        }

        // Options: Broadcast Notification & Pin to top
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Checkbox(
            checked = sendNotification,
            onCheckedChange = { sendNotification = it },
            colors = CheckboxDefaults.colors(checkedColor = Indigo600)
          )
          Text(
            text = "Broadcast alert notification to students",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
          )
        }

        // Pin notice checkbox
        if (currentUser?.role in listOf("ADMIN", "FACULTY")) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Checkbox(
              checked = isPinned,
              onCheckedChange = { isPinned = it },
              colors = CheckboxDefaults.colors(checkedColor = Amber500)
            )
            Text(
              text = "Pin this notice to top of notice board 📌",
              style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
            )
          }
        }

        // Buttons
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(10.dp)
          ) {
            Text("Cancel")
          }
          Button(
            onClick = {
              if (title.isNotBlank() && content.isNotBlank()) {
                onPublish(
                  title.trim(),
                  content.trim(),
                  category,
                  targetAudience,
                  priority,
                  selectedAttachmentUri,
                  isPinned,
                  sendNotification
                )
              }
            },
            enabled = title.isNotBlank() && content.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
            modifier = Modifier
              .weight(1.6f)
              .testTag("publish_notice_submit_button"),
            shape = RoundedCornerShape(10.dp)
          ) {
            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Publish Notice", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

@Composable
fun NoticeDetailDialog(
  notice: NoticeEntity,
  onDismiss: () -> Unit,
  onInspectAttachment: (String) -> Unit
) {
  val context = LocalContext.current
  val formattedDate = remember(notice.postedAt) {
    SimpleDateFormat("EEEE, dd MMMM yyyy 'at' hh:mm a", Locale.getDefault()).format(Date(notice.postedAt))
  }

  Dialog(onDismissRequest = onDismiss) {
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
        modifier = Modifier
          .padding(20.dp)
          .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = Indigo100,
              contentColor = Indigo700
            ) {
              Text(
                text = notice.category.uppercase(),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              )
            }
            if (notice.isPinned) {
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = Amber100,
                contentColor = Amber800
              ) {
                Text(
                  text = "📌 PINNED",
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Black
                )
              }
            }
          }
          IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close")
          }
        }

        // Title
        Text(
          text = notice.title,
          style = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.Bold,
            lineHeight = 26.sp
          )
        )

        // Author & Date Info
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Box(
            modifier = Modifier
              .size(32.dp)
              .clip(CircleShape)
              .background(if (notice.authorRole == "ADMIN") Indigo600 else Emerald600),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = notice.postedBy.take(1).uppercase(),
              color = Color.White,
              fontWeight = FontWeight.Bold
            )
          }
          Column {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(text = notice.postedBy, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
              Text("(${notice.authorRole})", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
            }
            Text(text = formattedDate, style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
          }
        }

        HorizontalDivider(color = Slate200, thickness = 0.8.dp)

        // Notice Content
        Text(
          text = notice.content,
          style = MaterialTheme.typography.bodyMedium.copy(
            lineHeight = 22.sp,
            color = MaterialTheme.colorScheme.onSurface
          )
        )

        // Attachment Preview
        if (!notice.attachmentUri.isNullOrBlank()) {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = Slate50,
            border = BorderStroke(1.dp, Indigo200),
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onInspectAttachment(notice.attachmentUri) }
          ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  Icon(Icons.Default.AttachFile, contentDescription = null, tint = Indigo600, modifier = Modifier.size(16.dp))
                  Text("Official Circular Document", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                }
                Text("Tap to zoom 🔍", fontSize = 11.sp, color = Indigo600, fontWeight = FontWeight.Bold)
              }
              AsyncImage(
                model = notice.attachmentUri,
                contentDescription = "Attachment preview",
                modifier = Modifier
                  .fillMaxWidth()
                  .height(140.dp)
                  .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
              )
            }
          }
        }

        // Bottom Actions: Share & Dismiss
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedButton(
            onClick = {
              val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, notice.title)
                putExtra(
                  Intent.EXTRA_TEXT,
                  "📢 *CAMPUS NOTICE: ${notice.title}*\n\n${notice.content}\n\nPosted by: ${notice.postedBy} (${notice.authorRole})\n$formattedDate"
                )
              }
              context.startActivity(Intent.createChooser(shareIntent, "Share Notice"))
            },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(10.dp)
          ) {
            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Share")
          }

          Button(
            onClick = onDismiss,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
            shape = RoundedCornerShape(10.dp)
          ) {
            Text("Close")
          }
        }
      }
    }
  }
}
