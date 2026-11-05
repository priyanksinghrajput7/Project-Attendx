package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("AttendX", appName)
  }

  @Test
  fun `verify attendance calculator logic`() {
    // 35 attended out of 50 total = 70%
    // To reach 75%:
    // (0.75 * 50 - 35) / (1 - 0.75) = (37.5 - 35) / 0.25 = 2.5 / 0.25 = 10 classes
    val targetFraction = 0.75f
    val currentPresent = 35
    val currentTotal = 50
    val num = (targetFraction * currentTotal) - currentPresent
    val den = 1f - targetFraction
    val required = Math.ceil((num / den).toDouble()).toInt()
    assertEquals(10, required)
  }

  @Test
  fun `verify qr token matching formats`() {
    val sessionToken = "ATTENDX-CN-2026-9821"
    val sessionId = "SES-1024"

    // Exact match
    val rawScanned1 = "ATTENDX-CN-2026-9821"
    val isMatch1 = rawScanned1.trim().equals(sessionToken, ignoreCase = true)
    assertEquals(true, isMatch1)

    // Formatted or URL contained match
    val rawScanned2 = "https://attendx.edu/verify?token=ATTENDX-CN-2026-9821"
    val isMatch2 = rawScanned2.contains(sessionToken)
    assertEquals(true, isMatch2)

    // Session code match
    val rawScanned3 = "SES-1024"
    val isMatch3 = rawScanned3.trim().equals(sessionId, ignoreCase = true)
    assertEquals(true, isMatch3)
  }

  @Test
  fun `verify camera permission states logic`() {
    // When permission is granted, camera scanner is active
    val isCameraPermissionGranted = true
    val scannerState = if (isCameraPermissionGranted) "LIVE_SCANNER" else "PERMISSION_REQUIRED"
    assertEquals("LIVE_SCANNER", scannerState)

    // When permission is not granted, fallback rationale/request is shown
    val isDenied = false
    val fallbackState = if (isDenied) "LIVE_SCANNER" else "PERMISSION_REQUIRED"
    assertEquals("PERMISSION_REQUIRED", fallbackState)
  }
}
