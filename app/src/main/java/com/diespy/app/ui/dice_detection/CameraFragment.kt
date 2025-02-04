package com.diespy.app.ui.dice_detection

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.diespy.app.ml.models.DiceBoundingBox
import com.diespy.app.Constants.LABELS_PATH
import com.diespy.app.Constants.MODEL_PATH
import com.diespy.app.ml.detector.DiceDetector
import com.diespy.app.databinding.FragmentCameraBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * CameraFragment handles the camera preview, object detection,
 * and updating UI elements with the detected results.
 */
class CameraFragment : Fragment(), DiceDetector.DetectorListener {

    // View binding to interact with UI components in FragmentCameraBinding
    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!

    // Camera and detector-related components
    private lateinit var cameraExecutor: ExecutorService
    private var cameraProvider: ProcessCameraProvider? = null
    private var diceDetector: DiceDetector? = null

    // UI-related components
    private val statsView = StatsView() // Handles statistics of detected dice
    private val isFrontCamera = false  // Indicates if the front camera is being used (default: false)
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null

    /**
     * Inflates the view and sets up View Binding.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Cleans up View Binding to prevent memory leaks.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Called when the view is created. Initializes camera and object detector.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Executor for running camera operations in a background thread
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Initialize object detector asynchronously
        cameraExecutor.execute {
            diceDetector = DiceDetector(requireContext(), MODEL_PATH, LABELS_PATH, this) {
                showToast(it)
            }
        }

        // Check permissions before starting the camera
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }
    }

    /**
     * Initializes and starts the CameraX provider.
     */
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    /**
     * Binds camera preview and image analysis for object detection.
     */
    private fun bindCameraUseCases() {
        val cameraProvider =
            cameraProvider ?: throw IllegalStateException("Camera initialization failed.")

        val rotation = binding.viewFinder.display.rotation

        // Selects the back camera
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        // Configures camera preview
        preview = Preview.Builder()
            .setTargetRotation(rotation)
            .build()

        // Sets up image analysis pipeline
        imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetRotation(rotation)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()

        // Sets the frame analyzer for object detection
        imageAnalyzer?.setAnalyzer(cameraExecutor) { imageProxy ->
            processImage(imageProxy)
        }

        // Unbind previous camera use cases before rebinding
        cameraProvider.unbindAll()

        try {
            // Bind camera lifecycle to this fragment
            camera = cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageAnalyzer
            )
            preview?.surfaceProvider = binding.viewFinder.surfaceProvider
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    /**
     * Processes frames and sends them to the object detector.
     */
    private fun processImage(imageProxy: ImageProxy) {
        frameCounter++
        if (frameCounter % FRAME_SKIP_RATE != 0) {
            imageProxy.close()
            return
        }

        // Converts ImageProxy to Bitmap
        val bitmap = imageProxy.toBitmap()

        // Rotates and corrects orientation before passing to detector
        val correctedBitmap = rotateAndFlipBitmap(bitmap, imageProxy.imageInfo.rotationDegrees)

        imageProxy.close()

        // Runs object detection on the processed frame
        diceDetector?.detect(correctedBitmap)
    }

    /**
     * Rotates and flips the image if needed.
     * This ensures bounding boxes align correctly with detected dice.
     */
    private fun rotateAndFlipBitmap(bitmap: Bitmap, rotationDegrees: Int): Bitmap {
        val matrix = Matrix().apply {
            postRotate(rotationDegrees.toFloat()) // Rotates image based on sensor rotation
            if (isFrontCamera) {
                postScale(-1f, 1f) // Mirrors image if using front camera
            }
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * Checks if all required permissions are granted.
     */
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Handles camera permission requests.
     */
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            if (it[Manifest.permission.CAMERA] == true) {
                startCamera()
            }
        }

    /**
     * Cleans up resources when the fragment is destroyed.
     */
    override fun onDestroy() {
        super.onDestroy()
        diceDetector?.close()
        cameraExecutor.shutdown()
    }

    /**
     * Restarts the camera when the fragment is resumed.
     */
    override fun onResume() {
        super.onResume()
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
        }
    }

    /**
     * Clears bounding boxes and resets stats when no dice are detected.
     */
    override fun onEmptyDetect() {
        requireActivity().runOnUiThread {
            binding.overlay.clear()
            statsView.reset()
            binding.statsCalc.text = statsView.getStatSummary()
            binding.statsFaces.text = statsView.getFaceCounts()
        }
    }

    /**
     * Handles detection results, updates UI, and displays bounding boxes.
     */
    override fun onDetect(diceBoundingBoxes: List<DiceBoundingBox>, inferenceTime: Long) {
        requireActivity().runOnUiThread {
            statsView.updateStats(diceBoundingBoxes)

            // Updates UI with dice statistics
            binding.statsCalc.text = statsView.getStatSummary()
            binding.statsFaces.text = statsView.getFaceCounts()

            // Updates overlay with bounding boxes
            binding.overlay.apply {
                setResults(diceBoundingBoxes)
                invalidate()
            }
        }
    }

    /**
     * Displays a toast message on the UI thread.
     */
    private fun showToast(message: String) {
        lifecycleScope.launch(Dispatchers.Main) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Companion object stores constants used throughout the class.
     */
    companion object {
        private const val TAG = "CameraFragment" // Log tag for debugging
        private const val REQUEST_CODE_PERMISSIONS = 10 // Request code for camera permissions
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA) // Permissions needed
        private var frameCounter = 0 // Counter to skip frames for efficiency
        private const val FRAME_SKIP_RATE = 6 // Processes every 6th frame for performance
    }
}
