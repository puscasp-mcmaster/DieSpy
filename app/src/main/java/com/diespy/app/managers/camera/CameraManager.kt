package com.diespy.app.managers.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import android.view.Surface
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Manages CameraX setup, permissions, and frame capturing.
 */
class CameraManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val onFrameCaptured: (Bitmap) -> Unit // Callback for processing frames
) {
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private lateinit var cameraExecutor: ExecutorService
    private var isFrontCamera = false

    /**
     * Initializes and starts the camera.
     */
    fun startCamera(viewFinder: PreviewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraExecutor = Executors.newSingleThreadExecutor()

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases(viewFinder)
        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * Configures and binds camera preview & image analysis.
     */
    private fun bindCameraUseCases(viewFinder: PreviewView) {
        val cameraProvider = cameraProvider ?: return

        val rotation = viewFinder.display.rotation
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        val preview = Preview.Builder()
            .setTargetRotation(rotation)
            .build().apply {
                setSurfaceProvider(viewFinder.surfaceProvider)
            }

        imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetRotation(rotation)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()

        imageAnalyzer?.setAnalyzer(cameraExecutor) { imageProxy ->
            processImage(imageProxy)
        }

        cameraProvider.unbindAll()
        try {
            cameraProvider.bindToLifecycle(
                lifecycleOwner, cameraSelector, preview, imageAnalyzer
            )
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    /**
     * Processes frames and sends them to the callback.
     */
    private fun processImage(imageProxy: ImageProxy) {
        val bitmap = imageProxy.toBitmap()
        val correctedBitmap = rotateBitmap(bitmap, imageProxy.imageInfo.rotationDegrees)

        onFrameCaptured(correctedBitmap) // Sends frame to `CameraFragment`
        imageProxy.close()
    }

    /**
     * Rotates image to correct orientation.
     */
    private fun rotateBitmap(bitmap: Bitmap, rotationDegrees: Int): Bitmap {
        val matrix = Matrix().apply {
            postRotate(rotationDegrees.toFloat())
            if (isFrontCamera) postScale(-1f, 1f)
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * Cleans up resources when camera is no longer needed.
     */
    fun stopCamera() {
        cameraExecutor.shutdown()
        imageAnalyzer?.clearAnalyzer()
        cameraProvider?.unbindAll()
    }

    companion object {
        private const val TAG = "CameraManager"
    }
}
