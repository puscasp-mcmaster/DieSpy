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

class DiceDetectionFragment : Fragment(), DiceDetector.DetectorListener {

    private var _binding: FragmentDiceDetectionBinding? = null
    private val binding get() = _binding!!

    private lateinit var cameraManager: CameraManager
    private var diceDetector: DiceDetector? = null
    private val diceStatsManager = DiceStatsManager()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDiceDetectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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

        cameraManager = CameraManager(requireContext(), viewLifecycleOwner) { frame ->
            diceDetector?.detect(frame)
        }

        requestCameraPermissions()
    }

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

    override fun onDestroy() {
        super.onDestroy()
        cameraManager.stopCamera()
        diceDetector?.close()
        diceDetector = null
    }

    override fun onEmptyDetect() {
        val safeBinding = _binding ?: return
        if (!isAdded) return  // Prevent crash if fragment is not attached

        requireActivity().runOnUiThread {
            diceStatsManager.reset()
            safeBinding.overlay.clear()
            safeBinding.statsCalc.text = diceStatsManager.getStatSummary()
            safeBinding.statsFaces.text = diceStatsManager.getFaceCounts()
        }
    }

    override fun onDetect(diceBoundingBoxes: List<DiceBoundingBox>, inferenceTime: Long) {
        val safeBinding = _binding ?: return
        if (!isAdded) return  // Prevent crash if fragment is not attached

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

    private fun showToast(message: String) {
        if (!isAdded) return
        lifecycleScope.launch(Dispatchers.Main) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}
