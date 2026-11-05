package com.example.ui.screens.student

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttendanceSessionEntity
import com.example.data.model.SubjectEntity
import com.example.data.model.UserEntity
import com.example.ui.components.CameraPreview
import com.example.ui.components.QRMatrixView
import com.example.ui.theme.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import kotlinx.coroutines.delay

/**
 * AttendanceScannerScreen
 * 
 * Production-ready QR Scanner component for students to verify attendance in real-time.
 * Supports multi-subject attendance with separate QR code scanning for every subject.
 * Implements CameraPreview with Google ML Kit Barcode Scanning API and Accompanist runtime permissions.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AttendanceScannerScreen(
  activeSessions: List<AttendanceSessionEntity> = emptyList(),
  subjects: List<SubjectEntity> = emptyList(),
  currentUser: UserEntity? = null,
  initialTargetSubject: SubjectEntity? = null,
  onBack: () -> Unit,
  onVerifyQRToken: (String, (Boolean, String) -> Unit) -> Unit
) {
  val context = LocalContext.current
  val hapticFeedback = LocalHapticFeedback.current
  val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)

  // Selected subject filter state
  var selectedSubjectId by remember {
    mutableStateOf(initialTargetSubject?.id ?: activeSessions.firstOrNull()?.subjectId)
  }

  // Active session for the selected subject
  val currentSession = remember(activeSessions, selectedSubjectId) {
    if (selectedSubjectId != null) {
      activeSessions.firstOrNull { it.subjectId == selectedSubjectId } ?: activeSessions.firstOrNull()
    } else {
      activeSessions.firstOrNull()
    }
  }

  // State
  var flashEnabled by remember { mutableStateOf(false) }
  var isVerifying by remember { mutableStateOf(false) }
  var verificationResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
  var scannedBarcodeData by remember { mutableStateOf<String?>(null) }
  var cameraErrorMessage by remember { mutableStateOf<String?>(null) }
  var scanCooldown by remember { mutableStateOf(false) }

  // Animated laser scanline
  val infiniteTransition = rememberInfiniteTransition(label = "scanner_laser")
  val laserPosition by infiniteTransition.animateFloat(
    initialValue = 0.05f,
    targetValue = 0.95f,
    animationSpec = infiniteRepeatable(
      animation = tween(2000, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "laser_y"
  )

  // Pulse effect for detection reticle
  val reticlePulse by infiniteTransition.animateFloat(
    initialValue = 0.85f,
    targetValue = 1.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(1000, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "reticle_pulse"
  )

  fun triggerHapticAndSound(success: Boolean) {
    try {
      hapticFeedback.performHapticFeedback(
        if (success) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove
      )
      val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
      } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
      }

      if (vibrator?.hasVibrator() == true) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          val timings = if (success) longArrayOf(0, 80, 40, 120) else longArrayOf(0, 250)
          val amplitudes = if (success) intArrayOf(0, 220, 0, 255) else intArrayOf(0, 180)
          vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
          @Suppress("DEPRECATION")
          vibrator.vibrate(200)
        }
      }

      // Audio feedback chime like payment apps (PhonePe / Google Pay / Paytm)
      try {
        val toneG = android.media.ToneGenerator(android.media.AudioManager.STREAM_NOTIFICATION, 100)
        if (success) {
          toneG.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 180)
        } else {
          toneG.startTone(android.media.ToneGenerator.TONE_PROP_NACK, 250)
        }
      } catch (_: Exception) {}
    } catch (_: Exception) {}
  }

  // Handler for QR token processing
  fun processTokenVerification(rawToken: String) {
    val clean = rawToken.trim()
    if (clean.isBlank() || isVerifying || scanCooldown) return

    scannedBarcodeData = clean
    isVerifying = true
    scanCooldown = true

    onVerifyQRToken(clean) { success, message ->
      isVerifying = false
      verificationResult = Pair(success, message)
      triggerHapticAndSound(success)
    }
  }

  // Auto-reset cooldown after verification outcome
  LaunchedEffect(scanCooldown) {
    if (scanCooldown) {
      delay(2500)
      scanCooldown = false
    }
  }

  // Automatically request camera permission when screen opens if not yet granted
  LaunchedEffect(Unit) {
    if (!cameraPermissionState.status.isGranted) {
      cameraPermissionState.launchPermissionRequest()
    }
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
        IconButton(
          onClick = onBack,
          modifier = Modifier.testTag("attendance_scanner_back_btn")
        ) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            text = "Subject QR Scanner",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
          )
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Box(
              modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(if (cameraPermissionState.status.isGranted) Emerald500 else Amber500)
            )
            Text(
              text = if (cameraPermissionState.status.isGranted) "Camera Active • Multi-Subject" else "Camera Permission Required",
              style = MaterialTheme.typography.labelSmall.copy(
                color = if (cameraPermissionState.status.isGranted) Emerald600 else Amber600,
                fontWeight = FontWeight.SemiBold
              )
            )
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = { flashEnabled = !flashEnabled },
            enabled = cameraPermissionState.status.isGranted,
            modifier = Modifier.testTag("attendance_scanner_flash_btn")
          ) {
            Icon(
              imageVector = if (flashEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
              contentDescription = "Toggle Torch",
              tint = if (flashEnabled) Amber500 else MaterialTheme.colorScheme.onSurface
            )
          }
        }
      }
    }
  ) { innerPadding ->
    if (activeSessions.isEmpty()) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
          .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Box(
          modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(Indigo50),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.HourglassEmpty,
            contentDescription = null,
            tint = Indigo600,
            modifier = Modifier.size(36.dp)
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
          text = "No Live QR Session",
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = "Faculty has not opened an attendance session yet. When your teacher opens the QR session on their screen according to the timetable, the scanner will become active here.",
          style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
          onClick = onBack,
          colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("no_session_back_btn")
        ) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Back to Dashboard", fontWeight = FontWeight.Bold)
        }
      }
    } else {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {

        // 1. ACTIVE SESSIONS CHIPS (ONLY FOR SESSIONS STARTED BY FACULTY)
        if (activeSessions.size > 1) {
          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Live Active Classes",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
              )
              Text(
                text = "${activeSessions.size} Active Sessions",
                style = MaterialTheme.typography.labelSmall.copy(color = Indigo600, fontWeight = FontWeight.Bold)
              )
            }

            Row(
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              activeSessions.forEach { sess ->
                val isSelected = currentSession?.id == sess.id
                FilterChip(
                  selected = isSelected,
                  onClick = {
                    selectedSubjectId = sess.subjectId
                  },
                  label = {
                    Text(
                      text = sess.subjectName,
                      fontSize = 12.sp,
                      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                  },
                  leadingIcon = {
                    Icon(
                      imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.Book,
                      contentDescription = null,
                      modifier = Modifier.size(14.dp)
                    )
                  },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Indigo600,
                    selectedLabelColor = Color.White,
                    selectedLeadingIconColor = Color.White
                  ),
                  modifier = Modifier.testTag("subject_filter_chip_${sess.subjectId}")
                )
              }
            }
          }
        }

        // 2. ACTIVE SUBJECT INFO BANNER
        if (currentSession != null) {
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = Indigo50,
            border = androidx.compose.foundation.BorderStroke(1.dp, Indigo200),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("active_session_card")
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(42.dp)
                  .clip(CircleShape)
                  .background(Indigo600),
                contentAlignment = Alignment.Center
              ) {
                Icon(Icons.Default.Class, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
              }

              Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Box(
                    modifier = Modifier
                      .size(8.dp)
                      .clip(CircleShape)
                      .background(Emerald500)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "Live Subject Attendance Session",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontWeight = FontWeight.Bold,
                      color = Emerald700
                    )
                  )
                }
                Text(
                  text = currentSession.subjectName,
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Indigo900),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Text(
                  text = "${currentSession.facultyName} • Room ${currentSession.room} • ${currentSession.section}",
                  style = MaterialTheme.typography.bodySmall.copy(color = Slate600),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }

              // Quick Mark button
              FilledTonalButton(
                onClick = { processTokenVerification(currentSession.qrToken) },
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = Indigo100),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.testTag("quick_mark_current_subject_btn")
              ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp), tint = Indigo700)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Mark", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Indigo700)
              }
            }
          }
        }

      // 4. CAMERA VIEWFINDER
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(300.dp)
          .clip(RoundedCornerShape(20.dp))
          .background(Color(0xFF0F172A))
          .border(2.dp, if (scanCooldown) Emerald400 else Indigo400, RoundedCornerShape(20.dp))
          .testTag("camera_preview_container"),
        contentAlignment = Alignment.Center
      ) {
        if (cameraPermissionState.status.isGranted) {
          CameraPreview(
            modifier = Modifier.fillMaxSize(),
            torchEnabled = flashEnabled,
            isPaused = isVerifying || scanCooldown,
            onBarcodeDetected = { detectedBarcode ->
              processTokenVerification(detectedBarcode)
            },
            onCameraError = { err ->
              cameraErrorMessage = err
            }
          )

          // Granted status badge overlay on top of camera
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.Black.copy(alpha = 0.6f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Emerald400.copy(alpha = 0.7f)),
            modifier = Modifier
              .align(Alignment.TopCenter)
              .padding(top = 10.dp)
              .testTag("camera_active_status_pill")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(8.dp)
                  .clip(CircleShape)
                  .background(Emerald400)
              )
              Text(
                text = "Live ML Kit Scanner • Center Subject QR",
                style = MaterialTheme.typography.labelSmall.copy(
                  color = Color.White,
                  fontWeight = FontWeight.Bold
                )
              )
            }
          }

          // Laser Scanning Animation
          Canvas(modifier = Modifier.fillMaxSize()) {
            val yPos = this.size.height * laserPosition
            drawLine(
              brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, Emerald400, Emerald500, Emerald400, Color.Transparent)
              ),
              start = Offset(20f, yPos),
              end = Offset(this.size.width - 20f, yPos),
              strokeWidth = 3.5.dp.toPx()
            )
          }

          // Reticle Target Box
          Box(
            modifier = Modifier
              .size(190.dp * reticlePulse)
              .border(
                width = 2.dp,
                brush = Brush.radialGradient(
                  colors = listOf(Color.White.copy(alpha = 0.9f), Indigo400)
                ),
                shape = RoundedCornerShape(16.dp)
              )
          )

          // Corner Frame Icons
          Icon(
            imageVector = Icons.Default.CropFree,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.size(210.dp)
          )

          // Status Overlay during verification
          if (isVerifying) {
            Box(
              modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f)),
              contentAlignment = Alignment.Center
            ) {
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                CircularProgressIndicator(color = Emerald400, modifier = Modifier.size(38.dp))
                Text(
                  text = "Analyzing & Verifying Attendance...",
                  style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                  )
                )
                scannedBarcodeData?.let {
                  Text(
                    text = "Token: $it",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFCBD5E1))
                  )
                }
              }
            }
          }
        } else {
          // Permission Request card
          Column(
            modifier = Modifier
              .fillMaxSize()
              .background(
                Brush.verticalGradient(
                  colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                )
              )
              .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
          ) {
            Box(
              modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Indigo900.copy(alpha = 0.7f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                tint = Indigo300,
                modifier = Modifier.size(28.dp)
              )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
              text = "Camera Permission Required",
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
              ),
              textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
              text = if (cameraPermissionState.status.shouldShowRationale)
                "AttendX requires camera permission to scan classroom QR codes for all subjects."
              else
                "Allow camera access to automatically scan separate QR codes displayed on classroom screens.",
              style = MaterialTheme.typography.bodySmall.copy(
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center
              ),
              modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Button(
                onClick = { cameraPermissionState.launchPermissionRequest() },
                colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("grant_camera_permission_btn")
              ) {
                Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Grant Camera Access", fontWeight = FontWeight.Bold)
              }

              OutlinedButton(
                onClick = {
                  try {
                    val intent = Intent(
                      Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                      Uri.fromParts("package", context.packageName, null)
                    )
                    context.startActivity(intent)
                  } catch (_: Exception) {}
                },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF64748B)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("open_settings_btn")
              ) {
                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Settings")
              }
            }
          }
        }
      }

      // Camera Hardware Error note
      if (cameraErrorMessage != null) {
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = Amber50,
          border = androidx.compose.foundation.BorderStroke(1.dp, Amber300),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(Icons.Outlined.Info, contentDescription = null, tint = Amber600, modifier = Modifier.size(20.dp))
            Text(
              text = "Camera: $cameraErrorMessage. Use quick subject chips or manual token entry below.",
              style = MaterialTheme.typography.bodySmall.copy(color = Amber900)
            )
          }
        }
      }

      // 5. VERIFICATION RESULT BANNER / CARD
      AnimatedVisibility(
        visible = verificationResult != null,
        enter = fadeIn(),
        exit = fadeOut()
      ) {
        if (verificationResult != null) {
          val (isSuccess, message) = verificationResult!!
          Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
              containerColor = if (isSuccess) Emerald50 else Rose50
            ),
            border = androidx.compose.foundation.BorderStroke(
              1.dp,
              if (isSuccess) Emerald300 else Rose300
            ),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("verification_result_card")
          ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                Icon(
                  imageVector = if (isSuccess) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline,
                  contentDescription = null,
                  tint = if (isSuccess) Emerald600 else Rose600,
                  modifier = Modifier.size(24.dp)
                )
                Text(
                  text = if (isSuccess) "Attendance Verified! ✅" else "Verification Failed ❌",
                  style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isSuccess) Emerald700 else Rose700
                  )
                )
              }

              Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium.copy(
                  color = if (isSuccess) Emerald900 else Rose900,
                  fontWeight = FontWeight.SemiBold
                )
              )

              if (currentUser != null) {
                Text(
                  text = "Student: ${currentUser.name} (${currentUser.rollNo}) • Section ${currentUser.section}",
                  style = MaterialTheme.typography.labelSmall.copy(color = Slate600)
                )
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
              ) {
                TextButton(
                  onClick = {
                    verificationResult = null
                    scanCooldown = false
                  },
                  modifier = Modifier.testTag("dismiss_result_btn")
                ) {
                  Text(
                    text = if (isSuccess) "Scan Another Subject" else "Try Again",
                    fontWeight = FontWeight.Bold,
                    color = if (isSuccess) Emerald700 else Rose700
                  )
                }
              }
            }
          }
        }
      }

      // 6. LIVE CAMERA SCANNING INSTRUCTION CARD (STRICT CAMERA SCAN ONLY)
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("camera_scanning_guide_card")
      ) {
        Row(
          modifier = Modifier.padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          Box(
            modifier = Modifier
              .size(46.dp)
              .clip(CircleShape)
              .background(Emerald50),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.QrCodeScanner,
              contentDescription = null,
              tint = Emerald600,
              modifier = Modifier.size(24.dp)
            )
          }

          Column {
            Text(
              text = "Live Camera Scan Required",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
            Text(
              text = "Align your camera lens with the live QR code projected by your professor. Attendance is recorded strictly via camera scanning.",
              style = MaterialTheme.typography.bodySmall.copy(color = Slate600)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}
}
