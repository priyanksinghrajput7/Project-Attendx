package com.example.ui.components

import android.util.Log
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "CameraPreview"

/**
 * High-performance CameraPreview composable utilizing CameraX and Google ML Kit Barcode Scanning API
 * to detect and stream attendance QR tokens and session codes.
 */
@Composable
fun CameraPreview(
  modifier: Modifier = Modifier,
  torchEnabled: Boolean = false,
  isPaused: Boolean = false,
  onBarcodeDetected: (String) -> Unit,
  onCameraError: ((String) -> Unit)? = null
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current

  var camera by remember { mutableStateOf<Camera?>(null) }
  val isProcessing = remember { AtomicBoolean(false) }

  // ML Kit Barcode Scanner configured for QR Codes and 2D matrix codes
  val barcodeScanner = remember {
    val options = BarcodeScannerOptions.Builder()
      .setBarcodeFormats(
        Barcode.FORMAT_QR_CODE,
        Barcode.FORMAT_DATA_MATRIX,
        Barcode.FORMAT_AZTEC
      )
      .build()
    BarcodeScanning.getClient(options)
  }

  val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

  // Sync torch mode
  LaunchedEffect(torchEnabled, camera) {
    try {
      if (camera?.cameraInfo?.hasFlashUnit() == true) {
        camera?.cameraControl?.enableTorch(torchEnabled)
      }
    } catch (e: Exception) {
      Log.w(TAG, "Torch toggle error: ${e.message}")
    }
  }

  DisposableEffect(Unit) {
    onDispose {
      analysisExecutor.shutdown()
      try {
        barcodeScanner.close()
      } catch (e: Exception) {
        Log.w(TAG, "Error closing barcode scanner: ${e.message}")
      }
    }
  }

  Box(modifier = modifier) {
    AndroidView(
      modifier = Modifier.fillMaxSize(),
      factory = { ctx ->
        val previewView = PreviewView(ctx).apply {
          layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
          )
          scaleType = PreviewView.ScaleType.FILL_CENTER
          implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
        val mainExecutor = ContextCompat.getMainExecutor(ctx)

        cameraProviderFuture.addListener({
          try {
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
              .build()
              .also {
                it.setSurfaceProvider(previewView.surfaceProvider)
              }

            val imageAnalysis = ImageAnalysis.Builder()
              .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
              .build()

            imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy: ImageProxy ->
              if (isPaused) {
                imageProxy.close()
                return@setAnalyzer
              }

              processImageProxy(
                imageProxy = imageProxy,
                barcodeScanner = barcodeScanner,
                isProcessing = isProcessing,
                onBarcodeDetected = onBarcodeDetected
              )
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            cameraProvider.unbindAll()
            camera = cameraProvider.bindToLifecycle(
              lifecycleOwner,
              cameraSelector,
              preview,
              imageAnalysis
            )

            if (torchEnabled && camera?.cameraInfo?.hasFlashUnit() == true) {
              camera?.cameraControl?.enableTorch(true)
            }
          } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
            onCameraError?.invoke(exc.localizedMessage ?: "Failed to initialize camera.")
          }
        }, mainExecutor)

        previewView
      }
    )
  }
}

@OptIn(ExperimentalGetImage::class)
private fun processImageProxy(
  imageProxy: ImageProxy,
  barcodeScanner: com.google.mlkit.vision.barcode.BarcodeScanner,
  isProcessing: AtomicBoolean,
  onBarcodeDetected: (String) -> Unit
) {
  val mediaImage = imageProxy.image
  if (mediaImage != null && !isProcessing.get()) {
    val rotationDegrees = imageProxy.imageInfo.rotationDegrees
    val image = InputImage.fromMediaImage(mediaImage, rotationDegrees)

    isProcessing.set(true)

    barcodeScanner.process(image)
      .addOnSuccessListener { barcodes ->
        for (barcode in barcodes) {
          val rawValue = barcode.rawValue ?: barcode.displayValue
          if (!rawValue.isNullOrBlank()) {
            onBarcodeDetected(rawValue)
            break
          }
        }
      }
      .addOnFailureListener { exc ->
        Log.d(TAG, "Barcode scan frame error: ${exc.message}")
      }
      .addOnCompleteListener {
        isProcessing.set(false)
        imageProxy.close()
      }
  } else {
    imageProxy.close()
  }
}
