package com.example.ui.screens.student

import androidx.compose.runtime.Composable
import com.example.data.model.AttendanceSessionEntity
import com.example.data.model.SubjectEntity
import com.example.data.model.UserEntity

/**
 * StudentQRScannerScreen delegates directly to the comprehensive AttendanceScannerScreen.
 * Supports multi-subject attendance verification with separate QR codes.
 */
@Composable
fun StudentQRScannerScreen(
  activeSessions: List<AttendanceSessionEntity>,
  subjects: List<SubjectEntity> = emptyList(),
  currentUser: UserEntity? = null,
  initialTargetSubject: SubjectEntity? = null,
  onBack: () -> Unit,
  onVerifyQRToken: (String, (Boolean, String) -> Unit) -> Unit
) {
  AttendanceScannerScreen(
    activeSessions = activeSessions,
    subjects = subjects,
    currentUser = currentUser,
    initialTargetSubject = initialTargetSubject,
    onBack = onBack,
    onVerifyQRToken = onVerifyQRToken
  )
}
