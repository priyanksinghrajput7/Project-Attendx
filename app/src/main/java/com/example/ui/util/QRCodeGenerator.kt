package com.example.ui.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

object QRCodeGenerator {

  /**
   * Generates a high-contrast, genuine ISO/IEC 18004 QR code Bitmap using ZXing.
   * Configured with Error Correction Level M and proper quiet zone margins for
   * instantaneous detection by Google ML Kit Barcode Scanning.
   */
  fun generateQRCodeBitmap(
    content: String,
    width: Int = 512,
    height: Int = 512
  ): Bitmap? {
    if (content.isBlank()) return null
    return try {
      val hints = hashMapOf<EncodeHintType, Any>(
        EncodeHintType.CHARACTER_SET to "UTF-8",
        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
        EncodeHintType.MARGIN to 1 // 1-module quiet zone for compact, crisp display
      )
      val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, width, height, hints)
      val matrixWidth = bitMatrix.width
      val matrixHeight = bitMatrix.height
      val pixels = IntArray(matrixWidth * matrixHeight)

      for (y in 0 until matrixHeight) {
        val offset = y * matrixWidth
        for (x in 0 until matrixWidth) {
          pixels[offset + x] = if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE
        }
      }

      Bitmap.createBitmap(matrixWidth, matrixHeight, Bitmap.Config.ARGB_8888).apply {
        setPixels(pixels, 0, matrixWidth, 0, 0, matrixWidth, matrixHeight)
      }
    } catch (e: Exception) {
      null
    }
  }

  /**
   * Helper to format a standardized AttendX QR payload containing full session details
   * for zero-lag instant verification across devices.
   */
  fun buildPayload(
    sessionId: String,
    subjectId: String,
    subjectName: String,
    qrToken: String,
    facultyId: String,
    section: String,
    room: String
  ): String {
    return "ATTENDX|v1|$sessionId|$subjectId|$subjectName|$qrToken|$facultyId|$section|$room|${System.currentTimeMillis()}"
  }
}
