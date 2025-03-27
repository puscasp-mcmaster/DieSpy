package com.diespy.app.ui.dice_detection

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.diespy.app.ml.models.DiceBoundingBox
import com.diespy.app.Constants.LABELS_PATH
import com.diespy.app.Constants.MODEL_PATH
import com.diespy.app.R
import com.diespy.app.ml.detector.DiceDetector
import com.diespy.app.managers.camera.CameraManager
import com.diespy.app.databinding.FragmentDiceDetectionBinding
import com.diespy.app.managers.game.DiceStatsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.diespy.app.managers.logs.LogManager
import com.diespy.app.managers.profile.SharedPrefManager
import com.diespy.app.ui.utils.diceParse
import com.diespy.app.ui.utils.showEditRollDialog
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
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
    private var capturing = false
    private var currentToast: Toast? = null


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
        val currentParty = SharedPrefManager.getCurrentPartyId(requireContext()) ?: ""
        if (currentParty == ""){
            binding.showRollButton.visibility = View.INVISIBLE

        }
        requestCameraPermissions()

        binding.freezeButton.setOnClickListener {
            if (!isFrozen && !capturing) {
                capturing = true
                binding.freezeButton.isEnabled = false
                binding.showRollButton.visibility = View.INVISIBLE
                viewLifecycleOwner.lifecycleScope.launch {
                    val startTime = System.currentTimeMillis()
                    // Wait up to 5000 ms (5 seconds) for at least 5 frames
                    showToast("Capturing dice...")
                    while (frameDiceBuffer.size < 5 && System.currentTimeMillis() - startTime < 5000) {
                        delay(50)
                    }
                    if (currentParty!="") binding.showRollButton.visibility = View.VISIBLE
                    if (frameDiceBuffer.size < 5) {
                        showToast("No dice detected, please try again")
                        capturing = false
                        binding.freezeButton.isEnabled = true
                    } else {
                        freezeCameraAndComputeMode()
                    }
                }
            } else if (isFrozen) {
                unfreezeCamera()
            }
        }
        binding.showRollButton.setOnClickListener{
                showRollDialog()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            while (isActive and !isFrozen) {
                delay(1000)
                if (!isFrozen && System.currentTimeMillis() - lastDetectionTime > 3000) {
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
//            diceStatsManager.updateStats(diceBoundingBoxes)
//            safeBinding.statsCalc.text = diceStatsManager.getStatSummary()
//            safeBinding.statsFaces.text = diceStatsManager.getFaceCounts()
//            safeBinding.overlay.apply {
//                setResults(diceBoundingBoxes)
//                invalidate()
//            }

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

    @SuppressLint("SetTextI18n")
    private fun freezeCameraAndComputeMode() {
        isFrozen = true
        cameraManager.stopCamera()
        capturing = false  // capturing is done
        binding.freezeButton.text = "Unfreeze"
        binding.freezeButton.isEnabled = true  // re-enable button

        //Compute dice breakdown and save log
        val breakdown = getModeDiceBreakdown(frameDiceBuffer)
        val username = SharedPrefManager.getCurrentUsername(requireContext()) ?: "User"
        val currentParty = SharedPrefManager.getCurrentPartyId(requireContext()) ?: ""
        if (currentParty != "") {
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
            logManager.saveLog(username, breakdown, timestamp, currentParty)
            showRollDialog()
        } else {
            showDiceBreakdownDialog(
                inflater = LayoutInflater.from(requireContext()),
                breakdown = breakdown,
                editable = false,
                onDismiss = { unfreezeCamera() }
            )
        }
    }

    @SuppressLint("SetTextI18n")
    private fun unfreezeCamera() {
        isFrozen = false
        cameraManager.startCamera(binding.viewFinder)
        frameSumsBuffer.clear()
        frameDiceBuffer.clear()
        capturing = false
        binding.freezeButton.text = "Capture"
        binding.freezeButton.isEnabled = true
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
        // For each dice face 1-6, compute the combined count (face + face+6) per frame,
        // then determine the most frequent count (the mode) across all frames.
        val modeCounts = (1..6).map { face ->
            val counts = frames.map { frame ->
                frame.count { it == face } + frame.count { it == face + 6 }
            }
            counts.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: 0
        }
        // Format the result into two columns.
        return "1: ${modeCounts[0]}      4: ${modeCounts[3]}\n" +
                "2: ${modeCounts[1]}      5: ${modeCounts[4]}\n" +
                "3: ${modeCounts[2]}      6: ${modeCounts[5]}"
    }

    //In a Party roll breakdown
    @SuppressLint("SetTextI18n")
    private fun showRollDialog() {
        currentToast?.cancel()
        val currentParty = SharedPrefManager.getCurrentPartyId(requireContext()) ?: ""
        viewLifecycleOwner.lifecycleScope.launch {
            val logs = logManager.loadLogs(currentParty)
            if (logs.isNotEmpty()) {
                val lastLog = logs.last()
                withContext(Dispatchers.Main) {
                    showDiceBreakdownDialog(
                        inflater = LayoutInflater.from(requireContext()),
                        breakdown = lastLog.log,
                        editable = true,
                        onEditClick = {
                            showEditRollDialog(
                                requireContext(),
                                diceParse(lastLog.log)
                            ) { updatedRolls ->
                                val updatedLog =
                                    "1: ${updatedRolls[0]}      4: ${updatedRolls[3]}\n" +
                                            "2: ${updatedRolls[1]}      5: ${updatedRolls[4]}\n" +
                                            "3: ${updatedRolls[2]}      6: ${updatedRolls[5]}"
                                val currentParty =
                                    SharedPrefManager.getCurrentPartyId(requireContext())
                                        ?: return@showEditRollDialog
                                viewLifecycleOwner.lifecycleScope.launch {
                                    logManager.updateLog(currentParty, lastLog.id, updatedLog)
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    //Local breakdown
    private fun showDiceBreakdownDialog(
        inflater: LayoutInflater,
        breakdown: String,
        editable: Boolean = false,
        onEditClick: (() -> Unit)? = null,
        onDismiss: (() -> Unit)? = null
    ) {
        val dialogView = inflater.inflate(R.layout.dialog_last_roll, null)

        val titleView = dialogView.findViewById<TextView>(R.id.dialogTitle)
        val totalText = dialogView.findViewById<TextView>(R.id.totalText)
        val row1left = dialogView.findViewById<TextView>(R.id.row1_left)
        val row1right = dialogView.findViewById<TextView>(R.id.row1_right)
        val row2left = dialogView.findViewById<TextView>(R.id.row2_left)
        val row2right = dialogView.findViewById<TextView>(R.id.row2_right)
        val row3left = dialogView.findViewById<TextView>(R.id.row3_left)
        val row3right = dialogView.findViewById<TextView>(R.id.row3_right)
        val dismissButton = dialogView.findViewById<Button>(R.id.dismissButton)
        val editButton = dialogView.findViewById<Button>(R.id.editButton)

        val rolls = diceParse(breakdown)
        val totalSum = rolls.withIndex().sumOf { (i, count) -> (i + 1) * count }

        titleView.text = "Dice Breakdown"
        totalText.text = "Total: $totalSum"
        row1left.text = "1: ${rolls[0]}"
        row1right.text = "4: ${rolls[3]}"
        row2left.text = "2: ${rolls[1]}"
        row2right.text = "5: ${rolls[4]}"
        row3left.text = "3: ${rolls[2]}"
        row3right.text = "6: ${rolls[5]}"

        editButton.visibility = if (editable) View.VISIBLE else View.GONE
        editButton.setOnClickListener {
            onEditClick?.invoke()
        }

        val dialog = AlertDialog.Builder(dialogView.context)
            .setView(dialogView)
            .create()

        dismissButton.setOnClickListener {
            onDismiss?.invoke()
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        dialog.window?.setDimAmount(0.8f)
        dialog.show()
    }

    private fun showToast(message: String) {
        currentToast?.cancel()
        if (!isAdded) return
        val toast = Toast.makeText(requireContext(), message, Toast.LENGTH_LONG)
        toast.show()
        currentToast = toast
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
