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
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.diespy.app.managers.logs.LogManager
import com.diespy.app.managers.profile.SharedPrefManager
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.Date

class DiceDetectionFragment : Fragment(), DiceDetector.DetectorListener {
    private var _binding: FragmentDiceDetectionBinding? = null
    private val binding get() = _binding!!
    private lateinit var logManager: LogManager
    private lateinit var cameraManager: CameraManager
    private var diceDetector: DiceDetector? = null
    private val diceStatsManager = DiceStatsManager()
    private var isFrozen = false
    //Buffer to store dice values for each frame.
    private val frameSumsBuffer = mutableListOf<Int>()
    private val frameDiceBuffer = mutableListOf<List<Int>>()
    private var lastDetectionTime: Long = System.currentTimeMillis()



    override fun onResume() {
        super.onResume()
        hideSystemUI()
    }

    private fun hideSystemUI() {
        val window = requireActivity().window
        // Let the window extend into the system window areas, but you'll selectively hide components.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        // Hide only the navigation bars, leaving the status bar visible.
        controller.hide(WindowInsetsCompat.Type.navigationBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
    override fun onPause() {
        super.onPause()
        showSystemUI()
    }

    private fun showSystemUI() {
        val window = requireActivity().window
        WindowCompat.setDecorFitsSystemWindows(window, true)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.show(WindowInsetsCompat.Type.systemBars())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDiceDetectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        diceDetector = DiceDetector(
            requireContext(),
            MODEL_PATH,
            LABELS_PATH,
            this,
            ::showToast
        )
        logManager = LogManager(requireContext())
        cameraManager = CameraManager(requireContext(), viewLifecycleOwner) { frame ->
            //Only process frames if not frozen.
            if (!isFrozen) {
                diceDetector?.detect(frame)
            }
        }
        requestCameraPermissions()

        //Freeze/Unfreeze button handling.
        binding.freezeButton.setOnClickListener {
            if (!isFrozen) {
                //If there aren't enough frames, wait until we have at least 5.
                if (frameDiceBuffer.size < 5) {
                    showToast("Capturing dice...")
                    lifecycleScope.launch {
                        while (frameDiceBuffer.size < 5) {
                            delay(30)
                        }
                        freezeCameraAndComputeMode()
                    }
                } else {
                    freezeCameraAndComputeMode()
                }
            } else {
                unfreezeCamera()
            }
        }
        lifecycleScope.launch {
            while (isActive) {
                delay(1000)
                if (!isFrozen && System.currentTimeMillis() - lastDetectionTime > 1000) {
                    frameSumsBuffer.clear()
                    frameDiceBuffer.clear()
                }
            }
        }
    }

    override fun onDetect(diceBoundingBoxes: List<DiceBoundingBox>, inferenceTime: Long) {
        val safeBinding = _binding ?: return
        if (!isAdded) return // Prevent crash if fragment is not attached

        requireActivity().runOnUiThread {
            diceStatsManager.updateStats(diceBoundingBoxes)
//            safeBinding.statsCalc.text = diceStatsManager.getStatSummary()
//            safeBinding.statsFaces.text = diceStatsManager.getFaceCounts()
            safeBinding.overlay.apply {
                setResults(diceBoundingBoxes)
                invalidate()
            }

            //update the buffers if not frozen
            if (!isFrozen) {
                //logs last detection time so we can clear
                lastDetectionTime = System.currentTimeMillis()
                //Get individual dice values for this frame
                val frameDice = diceBoundingBoxes.map { it.classIndex + 1 }
                val frameSum = frameDice.sum()
                frameSumsBuffer.add(frameSum)
                if (frameSumsBuffer.size > 20) {
                    frameSumsBuffer.removeAt(0)
                }

                //Store the full list of predictions for breakdown later
                frameDiceBuffer.add(frameDice)
                if (frameDiceBuffer.size > 20) {
                    frameDiceBuffer.removeAt(0)
                }
            }
        }
    }


    override fun onEmptyDetect() {
        val safeBinding = _binding ?: return
        if (!isAdded) return
        requireActivity().runOnUiThread {
            diceStatsManager.reset()
            safeBinding.overlay.clear()
//            safeBinding.statsCalc.text = diceStatsManager.getStatSummary()
//            safeBinding.statsFaces.text = diceStatsManager.getFaceCounts()
        }
    }

    //Functions for freezing camera and calculations
    private fun freezeCameraAndComputeMode() {
        isFrozen = true
        cameraManager.stopCamera()
//        val modeValue = calculateMode(frameSumsBuffer)
        //count of each die face across the last 10 frames.
        val breakdown = getModeDiceBreakdown(frameDiceBuffer)
//        binding.modeTextView.text = "Mode: ${modeValue ?: "N/A"}\n" +
//                "$breakdown"
        binding.freezeButton.text = "Unfreeze"

        //Add to logs when freezing
        val username = SharedPrefManager.getUsername(requireContext()) ?: "User"
        val currentParty = SharedPrefManager.getCurrentParty(requireContext()) ?: ""
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm").format(Date())
        logManager.saveLog(username, breakdown, timestamp, currentParty)
    }

    private fun unfreezeCamera() {
        isFrozen = false
        cameraManager.startCamera(binding.viewFinder)
        frameSumsBuffer.clear()
        frameDiceBuffer.clear()
//        binding.modeTextView.text = "Mode: "
        binding.freezeButton.text = "Capture"
    }

    //Permissions for camera
    private fun requestCameraPermissions() {
        if (allPermissionsGranted()) {
            cameraManager.startCamera(binding.viewFinder)
        } else {
            requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions[Manifest.permission.CAMERA] == true) {
                cameraManager.startCamera(binding.viewFinder)
            } else {
                showToast("Camera permission is required.")
            }
        }

    //Helper functions for mode for last 10 frames
    private fun getModeDiceBreakdown(frames: List<List<Int>>): String {
        if (frames.isEmpty()) return "No frames"
        return (1..6).joinToString("\n") { face ->
            val counts = frames.map { frame -> frame.count { it == face } }
            val modeCount = counts.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: 0
            "$face: $modeCount"
        }
    }

    private fun calculateMode(values: List<Int>): Int? {
        if (values.isEmpty()) return null
        return values.groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
    }

    private fun showToast(message: String) {
        if (!isAdded) return
        lifecycleScope.launch(Dispatchers.Main) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraManager.stopCamera()
        diceDetector?.close()
        diceDetector = null
    }

    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}
