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
import com.diespy.app.ml.models.MatchCandidate
import com.diespy.app.ml.models.TrackedDice
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
    private var lastDetectionTime: Long = System.currentTimeMillis()
    private var capturing = false
    private var currentToast: Toast? = null
    private val trackedDiceBuffer = mutableListOf<TrackedDice>()
    private val matchThreshold = 40f // px distance to consider same dice
    private var currentFrameCount = 0


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
                    val timeout = 4000L // 5 seconds
                    val minHistoryPerDie = 4
                    showToast("Capturing dice...")
                    while (System.currentTimeMillis() - startTime < timeout) {
                        // Wait a short interval (let frames build)
                        delay(50)

                        // Check how many dice have enough history
                        val stableDice =
                            trackedDiceBuffer.count { it.predictions.size >= minHistoryPerDie }

                        //counted at least one dice
                        if (stableDice >= 1) {
                            break // good to go
                        }
                    }

                    // Show roll button if we’re in a party
                    if (currentParty.isNotEmpty()) {
                        binding.showRollButton.visibility = View.VISIBLE
                    }

                    // Evaluate the result — even if it didn't meet threshold, still try
                    if (trackedDiceBuffer.count { it.predictions.size >= minHistoryPerDie } < 1) {
                        showToast("Not enough stable dice detected, please try again")
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
                delay(500)
                if (!isFrozen && System.currentTimeMillis() - lastDetectionTime > 1000) {
                    frameSumsBuffer.clear()
                    trackedDiceBuffer.clear()
                }
            }
        }
    }

    override fun onDetect(diceBoundingBoxes: List<DiceBoundingBox>, inferenceTime: Long) {
        if (!isAdded) return // Prevent crash if fragment is not attached
        currentFrameCount++
        requireActivity().runOnUiThread {

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



                // Track dice across frames using greedy matching
                val candidates = mutableListOf<MatchCandidate>()

                diceBoundingBoxes.forEach { box ->
                    val cx = (box.x1 + box.x2) / 2f
                    val cy = (box.y1 + box.y2) / 2f
                    trackedDiceBuffer.forEach { tracked ->
                        val dist = tracked.distanceTo(cx, cy)
                        if (dist < matchThreshold) {
                            candidates.add(MatchCandidate(tracked, box, dist))
                        }
                    }
                }

                val matchedTracked = mutableSetOf<TrackedDice>()
                val matchedBoxes = mutableSetOf<DiceBoundingBox>()

                candidates.sortedBy { it.distance }.forEach { candidate ->
                    if (candidate.tracked !in matchedTracked && candidate.box !in matchedBoxes) {
                        candidate.tracked.centerX = (candidate.box.x1 + candidate.box.x2) / 2f
                        candidate.tracked.centerY = (candidate.box.y1 + candidate.box.y2) / 2f
                        candidate.tracked.addPrediction(candidate.box.classIndex)

                        candidate.tracked.lastSeenFrame = currentFrameCount


                        matchedTracked.add(candidate.tracked)
                        matchedBoxes.add(candidate.box)
                    }
                }

                // Track new dice that didn’t match any existing one
                diceBoundingBoxes.filter { it !in matchedBoxes }.forEach { box ->
                    val cx = (box.x1 + box.x2) / 2f
                    val cy = (box.y1 + box.y2) / 2f
                    trackedDiceBuffer.add(TrackedDice(cx, cy, mutableListOf(box.classIndex)))
                }


                trackedDiceBuffer.removeAll { currentFrameCount - it.lastSeenFrame > 6 }
            }
        }
    }

    override fun onEmptyDetect() {
        val safeBinding = _binding ?: return
        if (!isAdded) return
        requireActivity().runOnUiThread {
            diceStatsManager.reset()
            safeBinding.overlay.clear()
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
        val faceCounts = IntArray(12) // supports classes 0–11
        trackedDiceBuffer.forEach { tracked ->
            tracked.getStablePrediction()?.let { classIndex ->
                if (classIndex in 0..11) {
                    faceCounts[classIndex]++
                }
            }
        }

        val breakdown = "1: ${faceCounts[0] + faceCounts[6]}      4: ${faceCounts[3] + faceCounts[9]}\n" +
                "2: ${faceCounts[1] + faceCounts[7]}      5: ${faceCounts[4] + faceCounts[10]}\n" +
                "3: ${faceCounts[2] + faceCounts[8]}      6: ${faceCounts[5] + faceCounts[11]}"

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
        currentFrameCount = 0
        capturing = false
        binding.freezeButton.text = "Capture"
        binding.freezeButton.isEnabled = true
        trackedDiceBuffer.clear()
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

