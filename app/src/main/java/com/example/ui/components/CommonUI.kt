package com.example.ui.components

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NotificationEntity
import com.example.data.model.UserEntity
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendXTopBar(
  user: UserEntity?,
  isDarkMode: Boolean,
  unreadNotificationCount: Int = 0,
  onToggleDarkMode: () -> Unit,
  onOpenNotifications: () -> Unit,
  onOpenNoticeBoard: (() -> Unit)? = null,
  onOpenProfile: () -> Unit,
  onLogout: () -> Unit,
  modifier: Modifier = Modifier
) {
  TopAppBar(
    modifier = modifier,
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Box(
          modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
              Brush.linearGradient(
                listOf(Indigo600, Blue500)
              )
            ),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.School,
            contentDescription = "AttendX Logo",
            tint = Color.White,
            modifier = Modifier.size(20.dp)
          )
        }
        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = "Attend",
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp
              )
            )
            Text(
              text = "X",
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Black,
                color = Indigo500
              )
            )
          }
          Text(
            text = user?.role?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Smart Campus",
            style = MaterialTheme.typography.labelSmall.copy(
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          )
        }
      }
    },
    actions = {
      // Dark mode toggle
      IconButton(
        onClick = onToggleDarkMode,
        modifier = Modifier.testTag("theme_toggle_button")
      ) {
        Icon(
          imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
          contentDescription = "Toggle Theme"
        )
      }

      // Campus Notice Board button
      if (onOpenNoticeBoard != null) {
        IconButton(
          onClick = onOpenNoticeBoard,
          modifier = Modifier.testTag("topbar_notice_board_button")
        ) {
          Icon(
            imageVector = Icons.Outlined.Campaign,
            contentDescription = "Notice Board",
            tint = Indigo600
          )
        }
      }

      // Notifications button with badge
      IconButton(
        onClick = onOpenNotifications,
        modifier = Modifier.testTag("notifications_button")
      ) {
        BadgedBox(
          badge = {
            if (unreadNotificationCount > 0) {
              Badge(containerColor = Rose500) {
                Text(text = "$unreadNotificationCount")
              }
            }
          }
        ) {
          Icon(
            imageVector = Icons.Outlined.Notifications,
            contentDescription = "Notifications"
          )
        }
      }

      // User avatar / Profile
      if (user != null) {
        Box(
          modifier = Modifier
            .padding(end = 8.dp)
            .size(36.dp)
            .clip(CircleShape)
            .background(Indigo100)
            .clickable { onOpenProfile() }
            .testTag("user_avatar_button"),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = user.name.take(1).uppercase(),
            fontWeight = FontWeight.Bold,
            color = Indigo700,
            fontSize = 15.sp
          )
        }
      }
    },
    colors = TopAppBarDefaults.topAppBarColors(
      containerColor = MaterialTheme.colorScheme.surface,
      titleContentColor = MaterialTheme.colorScheme.onSurface,
      actionIconContentColor = MaterialTheme.colorScheme.onSurface
    )
  )
}

@Composable
fun StatCard(
  title: String,
  value: String,
  subtitle: String? = null,
  icon: ImageVector,
  accentColor: Color,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier,
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          style = MaterialTheme.typography.labelMedium.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = value,
          style = MaterialTheme.typography.headlineSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp
          )
        )
        if (subtitle != null) {
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall.copy(
              color = accentColor,
              fontWeight = FontWeight.SemiBold
            )
          )
        }
      }

      Box(
        modifier = Modifier
          .size(44.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(accentColor.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = title,
          tint = accentColor,
          modifier = Modifier.size(24.dp)
        )
      }
    }
  }
}

@Composable
fun CircularAttendanceProgress(
  percentage: Float,
  modifier: Modifier = Modifier,
  strokeWidth: Dp = 12.dp,
  size: Dp = 130.dp
) {
  val animatedPercent by animateFloatAsState(
    targetValue = percentage.coerceIn(0f, 100f),
    animationSpec = tween(durationMillis = 1000),
    label = "progress"
  )

  val progressColor = when {
    percentage <= 0f -> Slate400
    percentage >= 75f -> Emerald500
    percentage >= 65f -> Amber500
    else -> Rose500
  }

  val trackColor = MaterialTheme.colorScheme.surfaceVariant

  Box(
    modifier = modifier.size(size),
    contentAlignment = Alignment.Center
  ) {
    Canvas(modifier = Modifier.size(size)) {
      val strokePx = strokeWidth.toPx()
      val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
      val topLeft = Offset(strokePx / 2f, strokePx / 2f)

      // Background track
      drawArc(
        color = trackColor,
        startAngle = -90f,
        sweepAngle = 360f,
        useCenter = false,
        topLeft = topLeft,
        size = arcSize,
        style = Stroke(width = strokePx, cap = StrokeCap.Round)
      )

      // Foreground animated progress
      drawArc(
        color = progressColor,
        startAngle = -90f,
        sweepAngle = (animatedPercent / 100f) * 360f,
        useCenter = false,
        topLeft = topLeft,
        size = arcSize,
        style = Stroke(width = strokePx, cap = StrokeCap.Round)
      )
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
        text = "${String.format("%.1f", animatedPercent)}%",
        style = MaterialTheme.typography.titleLarge.copy(
          fontWeight = FontWeight.Black,
          letterSpacing = (-0.5).sp
        )
      )
      Text(
        text = if (percentage >= 75f) "Safe Status" else "Low Alert",
        style = MaterialTheme.typography.labelSmall.copy(
          color = progressColor,
          fontWeight = FontWeight.Bold
        )
      )
    }
  }
}

@Composable
fun AttendanceStatusBadge(status: String) {
  val (bgColor, textColor, icon) = when (status.uppercase()) {
    "PRESENT" -> Triple(Emerald50, Emerald600, Icons.Default.CheckCircle)
    "ABSENT" -> Triple(Rose50, Rose600, Icons.Default.Cancel)
    "LATE" -> Triple(Amber50, Amber600, Icons.Default.Schedule)
    else -> Triple(Slate100, Slate600, Icons.Default.Info)
  }

  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp),
    modifier = Modifier
      .clip(RoundedCornerShape(8.dp))
      .background(bgColor)
      .padding(horizontal = 8.dp, vertical = 4.dp)
  ) {
    Icon(
      imageVector = icon,
      contentDescription = status,
      tint = textColor,
      modifier = Modifier.size(14.dp)
    )
    Text(
      text = status.replaceFirstChar { it.uppercase() },
      style = MaterialTheme.typography.labelSmall.copy(
        color = textColor,
        fontWeight = FontWeight.Bold
      )
    )
  }
}

@Composable
fun SubjectProgressBar(
  percentage: Float,
  modifier: Modifier = Modifier
) {
  val barColor = when {
    percentage >= 75f -> Emerald500
    percentage >= 65f -> Amber500
    else -> Rose500
  }

  Column(modifier = modifier) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = if (percentage >= 75f) "Safe (≥75%)" else "Attendance Warning",
        style = MaterialTheme.typography.labelSmall.copy(
          color = barColor,
          fontWeight = FontWeight.Bold
        )
      )
      Text(
        text = "${String.format("%.1f", percentage)}%",
        style = MaterialTheme.typography.labelSmall.copy(
          fontWeight = FontWeight.Bold
        )
      )
    }
    Spacer(modifier = Modifier.height(4.dp))
    LinearProgressIndicator(
      progress = { (percentage / 100f).coerceIn(0f, 1f) },
      modifier = Modifier
        .fillMaxWidth()
        .height(8.dp)
        .clip(RoundedCornerShape(4.dp)),
      color = barColor,
      trackColor = MaterialTheme.colorScheme.surfaceVariant
    )
  }
}

@Composable
fun QRMatrixView(
  token: String,
  modifier: Modifier = Modifier,
  size: Dp = 180.dp
) {
  Card(
    modifier = modifier.size(size),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = Color.White),
    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(16.dp),
      contentAlignment = Alignment.Center
    ) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val gridSize = 19
        val cellWidth = this.size.width / gridSize
        val cellHeight = this.size.height / gridSize

        // Helper to draw QR finder corners (7x7 outer square, 3x3 inner square)
        fun drawFinderPattern(startX: Int, startY: Int) {
          // Outer border (7x7)
          drawRoundRect(
            color = Color.Black,
            topLeft = Offset(startX * cellWidth, startY * cellHeight),
            size = Size(7 * cellWidth, 7 * cellHeight),
            cornerRadius = CornerRadius(cellWidth * 0.8f)
          )
          // Inner clear (5x5)
          drawRoundRect(
            color = Color.White,
            topLeft = Offset((startX + 1) * cellWidth, (startY + 1) * cellHeight),
            size = Size(5 * cellWidth, 5 * cellHeight),
            cornerRadius = CornerRadius(cellWidth * 0.5f)
          )
          // Center solid (3x3)
          drawRoundRect(
            color = Color.Black,
            topLeft = Offset((startX + 2) * cellWidth, (startY + 2) * cellHeight),
            size = Size(3 * cellWidth, 3 * cellHeight),
            cornerRadius = CornerRadius(cellWidth * 0.4f)
          )
        }

        // 3 Corner Finder Patterns
        drawFinderPattern(0, 0)
        drawFinderPattern(gridSize - 7, 0)
        drawFinderPattern(0, gridSize - 7)

        // Seeded random pseudo-dots based on token characters
        val seed = token.hashCode()
        for (row in 0 until gridSize) {
          for (col in 0 until gridSize) {
            val inFinder1 = row < 7 && col < 7
            val inFinder2 = row < 7 && col >= gridSize - 7
            val inFinder3 = row >= gridSize - 7 && col < 7
            if (inFinder1 || inFinder2 || inFinder3) continue

            val hash = ((seed xor (row * 31 + col * 17)) + (row * col))
            if (hash % 3 == 0 || (row % 2 == 0 && col % 3 == 0) || (row == 8 && col % 2 == 0)) {
              drawRoundRect(
                color = Color.Black,
                topLeft = Offset(col * cellWidth, row * cellHeight),
                size = Size(cellWidth * 0.88f, cellHeight * 0.88f),
                cornerRadius = CornerRadius(cellWidth * 0.2f)
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun AttendanceCalculatorDialog(
  currentPresent: Int,
  currentTotal: Int,
  targetPercentage: Float = 75f,
  onDismiss: () -> Unit
) {
  var requiredTarget by remember { mutableStateOf(targetPercentage) }
  val currentPct = if (currentTotal > 0) (currentPresent.toFloat() / currentTotal.toFloat()) * 100f else 0f

  // Calculate classes required if below target
  val classesNeeded = remember(currentPresent, currentTotal, requiredTarget) {
    if (currentTotal == 0) 0
    else {
      val targetFraction = requiredTarget / 100f
      val currentF = currentPresent.toFloat() / currentTotal.toFloat()
      if (currentF >= targetFraction) 0
      else {
        val num = (targetFraction * currentTotal) - currentPresent
        val den = 1f - targetFraction
        Math.ceil((num / den).toDouble()).toInt().coerceAtLeast(0)
      }
    }
  }

  // Calculate safe bunks allowed if at or above target
  val safeBunksAllowed = remember(currentPresent, currentTotal, requiredTarget) {
    if (currentTotal == 0 || currentPct < requiredTarget) 0
    else {
      val maxBunks = Math.floor(((currentPresent * 100.0 - requiredTarget * currentTotal) / requiredTarget)).toInt()
      maxBunks.coerceAtLeast(0)
    }
  }

  // Calculate miss impact
  val ifMissedPct = if (currentTotal > 0) {
    (currentPresent.toFloat() / (currentTotal + 1).toFloat()) * 100f
  } else 0f

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Calculate, contentDescription = null, tint = Indigo600)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Smart Bunk & Attendance Planner", fontWeight = FontWeight.Bold)
      }
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Text(
          text = "Plan your academic attendance, calculate safe bunks, or find out how many classes you need to avoid debarment.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Current status card
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
          shape = RoundedCornerShape(12.dp)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column {
              Text("Current Attendance", style = MaterialTheme.typography.labelSmall)
              Text(
                text = "${String.format("%.1f", currentPct)}%",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = if (currentPct >= 75f) Emerald600 else Rose600
              )
            }
            Column(horizontalAlignment = Alignment.End) {
              Text("Attended / Total", style = MaterialTheme.typography.labelSmall)
              Text(
                text = "$currentPresent / $currentTotal",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
              )
            }
          }
        }

        // Target Preset Chips
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          listOf(75f, 80f, 85f, 90f).forEach { preset ->
            FilterChip(
              selected = requiredTarget.toInt() == preset.toInt(),
              onClick = { requiredTarget = preset },
              label = { Text("${preset.toInt()}%") }
            )
          }
        }

        // Target adjustment slider
        Text(
          text = "Selected Target: ${requiredTarget.toInt()}%",
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
        )
        Slider(
          value = requiredTarget,
          onValueChange = { requiredTarget = it },
          valueRange = 60f..95f,
          steps = 6
        )

        // Dynamic Safe Bunk / Shortage Banner
        if (currentPct >= requiredTarget) {
          Card(
            colors = CardDefaults.cardColors(containerColor = Emerald50),
            shape = RoundedCornerShape(12.dp)
          ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.BeachAccess, contentDescription = null, tint = Emerald700, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = if (safeBunksAllowed > 0)
                    "Safe Bunk Allowance: $safeBunksAllowed Class(es) 🏖️"
                  else
                    "Right on the border (${requiredTarget.toInt()}%)",
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Emerald900)
                )
              }
              Text(
                text = if (safeBunksAllowed > 0)
                  "You can safely miss up to $safeBunksAllowed class(es) without falling below your ${requiredTarget.toInt()}% target."
                else
                  "Do not miss the upcoming class to avoid falling below your ${requiredTarget.toInt()}% target.",
                style = MaterialTheme.typography.bodySmall.copy(color = Emerald800)
              )
            }
          }
        } else {
          Card(
            colors = CardDefaults.cardColors(containerColor = Rose50),
            shape = RoundedCornerShape(12.dp)
          ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.WarningAmber, contentDescription = null, tint = Rose700, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Consecutive Classes Needed: $classesNeeded ⚠️",
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Rose900)
                )
              }
              Text(
                text = "You must attend the next $classesNeeded lectures without any absence to recover to ${requiredTarget.toInt()}%.",
                style = MaterialTheme.typography.bodySmall.copy(color = Rose800)
              )
            }
          }
        }

        // Miss impact calculation
        Card(
          colors = CardDefaults.cardColors(containerColor = Indigo50),
          shape = RoundedCornerShape(8.dp)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.TrendingDown, contentDescription = null, tint = Indigo600, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "If you miss 1 next class: drops to ${String.format("%.1f", ifMissedPct)}%",
              style = MaterialTheme.typography.labelSmall.copy(color = Indigo900, fontWeight = FontWeight.SemiBold)
            )
          }
        }
      }
    },
    confirmButton = {
      Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Indigo600)) {
        Text("Done")
      }
    }
  )
}

@Composable
fun ReportExportDialog(
  reportTitle: String,
  reportSummary: String,
  tableDataText: String,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current

  // Generate standardized CSV text
  val csvContent = remember(reportTitle, tableDataText) {
    val lines = tableDataText.lines().filter { it.isNotBlank() && !it.startsWith("---") }
    lines.joinToString("\n") { line ->
      line.trim().split(Regex("\\s{2,}")).joinToString(",") { "\"$it\"" }
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.FileDownload, contentDescription = null, tint = Indigo600)
        Spacer(modifier = Modifier.width(8.dp))
        Text(reportTitle, fontWeight = FontWeight.Bold)
      }
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text(
          text = reportSummary,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(
          shape = RoundedCornerShape(8.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
          Box(modifier = Modifier.padding(12.dp)) {
            Text(
              text = tableDataText,
              style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                fontSize = 11.sp
              ),
              maxLines = 8,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        Surface(
          shape = RoundedCornerShape(6.dp),
          color = Emerald50
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Emerald600, modifier = Modifier.size(14.dp))
            Text(
              text = "Export ready: CSV Spreadsheet & Formatted PDF Text",
              style = MaterialTheme.typography.labelSmall,
              color = Emerald700,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    },
    confirmButton = {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
          onClick = {
            val sendIntent: Intent = Intent().apply {
              action = Intent.ACTION_SEND
              putExtra(Intent.EXTRA_TITLE, "$reportTitle.csv")
              putExtra(Intent.EXTRA_TEXT, "$reportTitle\n\n$csvContent")
              type = "text/csv"
            }
            context.startActivity(Intent.createChooser(sendIntent, "Export CSV Spreadsheet"))
            onDismiss()
          }
        ) {
          Icon(Icons.Default.GridOn, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("CSV Sheet", fontSize = 12.sp)
        }

        Button(
          onClick = {
            val sendIntent: Intent = Intent().apply {
              action = Intent.ACTION_SEND
              putExtra(Intent.EXTRA_TITLE, reportTitle)
              putExtra(Intent.EXTRA_TEXT, "$reportTitle\n\n$reportSummary\n\n$tableDataText")
              type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, "Export $reportTitle")
            context.startActivity(shareIntent)
            onDismiss()
          },
          colors = ButtonDefaults.buttonColors(containerColor = Indigo600)
        ) {
          Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Share", fontSize = 12.sp)
        }
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Close")
      }
    }
  )
}

@Composable
fun NotificationsDialog(
  notifications: List<NotificationEntity>,
  onMarkRead: (String) -> Unit,
  onDismiss: () -> Unit,
  onScanSession: (() -> Unit)? = null
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Indigo600)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Notifications", fontWeight = FontWeight.Bold)
        }
        Text(
          text = "${notifications.size} total",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    },
    text = {
      if (notifications.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
          contentAlignment = Alignment.Center
        ) {
          Text("No notifications right now.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      } else {
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          notifications.take(8).forEach { notif ->
            val isQRSession = notif.type == "QR_SESSION"
            Card(
              shape = RoundedCornerShape(12.dp),
              colors = CardDefaults.cardColors(
                containerColor = when {
                  isQRSession -> Emerald50
                  notif.isRead -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                  else -> Indigo50
                }
              ),
              border = if (isQRSession) androidx.compose.foundation.BorderStroke(1.dp, Emerald300) else null,
              modifier = Modifier
                .fillMaxWidth()
                .clickable { onMarkRead(notif.id) }
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (isQRSession) {
                      Box(
                        modifier = Modifier
                          .size(8.dp)
                          .clip(CircleShape)
                          .background(Emerald500)
                      )
                    }
                    Text(
                      text = notif.title,
                      style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = when {
                          isQRSession -> Emerald900
                          notif.isRead -> MaterialTheme.colorScheme.onSurface
                          else -> Indigo900
                        }
                      )
                    )
                  }
                  if (!notif.isRead && !isQRSession) {
                    Box(
                      modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Rose500)
                    )
                  }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = notif.message,
                  style = MaterialTheme.typography.bodySmall.copy(
                    color = when {
                      isQRSession -> Emerald900
                      notif.isRead -> MaterialTheme.colorScheme.onSurfaceVariant
                      else -> Slate700
                    }
                  )
                )

                if (isQRSession) {
                  Spacer(modifier = Modifier.height(8.dp))
                  Button(
                    onClick = {
                      onMarkRead(notif.id)
                      onDismiss()
                      onScanSession?.invoke()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(38.dp)
                      .testTag("scan_from_notif_${notif.id}")
                  ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Scan with Camera", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                  }
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Done")
      }
    }
  )
}
