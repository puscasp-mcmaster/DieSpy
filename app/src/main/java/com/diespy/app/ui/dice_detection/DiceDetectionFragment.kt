package com.diespy.app.ui.dice_detection

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.diespy.app.ml.models.DiceBoundingBox
import com.diespy.app.Constants.LABELS_PATH
import com.diespy.app.Constants.MODEL_PATH
import com.diespy.app.ml.detector.DiceDetector
import com.diespy.app.managers.camera.CameraManager
import com.diespy.app.databinding.FragmentDiceDetectionBinding
import com.diespy.app.managers.game.DiceStatsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * DiceDetectionFragment handles the UI, dice detection, and bounding box overlay.
 */
class DiceDetectionFragment : Fragment(), DiceDetector.DetectorListener {

    private var _binding: FragmentDiceDetectionBinding? = null
    private val binding get() = _binding!!

    private lateinit var cameraManager: CameraManager
    private var diceDetector: DiceDetector? = null
    private val diceStatsManager = DiceStatsManager() // Handles statistics of detected dice

    /**
     * Inflates the view and sets up View Binding.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDiceDetectionBinding.inflate(inflater, container, false)
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
     * Called when the view is created. Initializes camera and dice detector.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize dice detector asynchronously
        diceDetector = DiceDetector(
            requireContext(),
            MODEL_PATH,
            LABELS_PATH,
            this, // DetectorListener
            ::showToast // Passing your toast function as the showMessage lambda
        )

        // Initialize CameraManager and pass frames to diceDetector
        cameraManager = CameraManager(requireContext(), viewLifecycleOwner) { frame ->
            diceDetector?.detect(frame)
        }

        // Request permissions and start the camera only after granting permissions
        requestCameraPermissions()
    }

    /**
     * Requests camera permissions and starts the camera once granted.
     */
    private fun requestCameraPermissions() {
        if (allPermissionsGranted()) {
            cameraManager.startCamera(binding.viewFinder)
        } else {
            requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
        }
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
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions[Manifest.permission.CAMERA] == true) {
                cameraManager.startCamera(binding.viewFinder)
            } else {
                showToast("Camera permission is required.")
            }
        }

    /**
     * Cleans up resources when the fragment is destroyed.
     */
    override fun onDestroy() {
        super.onDestroy()

        // Stop camera to prevent new frames from coming in
        cameraManager.stopCamera()

        // Then close the detector to clean up
        diceDetector?.close()
        diceDetector = null
    }

    /**
     * Clears bounding boxes and resets stats when no dice are detected.
     */
    override fun onEmptyDetect() {
        val safeBinding = _binding ?: return // View is destroyed, skip
        requireActivity().runOnUiThread {
            diceStatsManager.reset()
            safeBinding.overlay.clear()
            safeBinding.statsCalc.text = diceStatsManager.getStatSummary()
            safeBinding.statsFaces.text = diceStatsManager.getFaceCounts()
        }
    }

    /**
     * Handles detection results, updates UI, and displays bounding boxes.
     */
    override fun onDetect(diceBoundingBoxes: List<DiceBoundingBox>, inferenceTime: Long) {
        val safeBinding = _binding ?: return
        requireActivity().runOnUiThread {
            diceStatsManager.updateStats(diceBoundingBoxes)
            safeBinding.statsCalc.text = diceStatsManager.getStatSummary()
            safeBinding.statsFaces.text = diceStatsManager.getFaceCounts()
            safeBinding.overlay.apply {
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

    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}
