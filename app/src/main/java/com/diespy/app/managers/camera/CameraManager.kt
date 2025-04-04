package com.diespy.app.managers.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.diespy.app.ui.dice_detection.DiceDetectionFragment
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val onFrameCaptured: (Bitmap) -> Unit // Callback for processing frames
) {
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private lateinit var cameraExecutor: ExecutorService
    private var isFrontCamera = false

    //Camera X setup
    fun startCamera(viewFinder: PreviewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraExecutor = Executors.newSingleThreadExecutor()

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases(viewFinder)
        }, ContextCompat.getMainExecutor(context))
    }

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

    //Frame processing
    private fun processImage(imageProxy: ImageProxy) {
        if ((lifecycleOwner as? DiceDetectionFragment)?.isRemoving == true) {
            imageProxy.close()
            return
        }

        val bitmap = imageProxy.toBitmap()
        val correctedBitmap = rotateBitmap(bitmap, imageProxy.imageInfo.rotationDegrees)
        onFrameCaptured(correctedBitmap)
        imageProxy.close()
    }

    //Images need to be rotated to match ML
    private fun rotateBitmap(bitmap: Bitmap, rotationDegrees: Int): Bitmap {
        val matrix = Matrix().apply {
            postRotate(rotationDegrees.toFloat())
            if (isFrontCamera) postScale(-1f, 1f)
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun stopCamera() {
        cameraExecutor.shutdown()
        imageAnalyzer?.clearAnalyzer()
        cameraProvider?.unbindAll()
    }

    companion object {
        private const val TAG = "CameraManager"
    }
}
