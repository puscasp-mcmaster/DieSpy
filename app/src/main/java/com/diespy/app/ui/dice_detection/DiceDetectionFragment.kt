package com.diespy.app.ui.dice_detection

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
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
import com.diespy.app.R
import com.diespy.app.ml.detector.DiceDetector
import com.diespy.app.managers.camera.CameraManager
import com.diespy.app.databinding.FragmentDiceDetectionBinding
import com.diespy.app.managers.game.DiceStatsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.diespy.app.managers.logs.LogManager
import com.diespy.app.managers.logs.LogMessage
import com.diespy.app.managers.profile.SharedPrefManager
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

    private fun showSystemUI() {
        val window = requireActivity().window
        WindowCompat.setDecorFitsSystemWindows(window, true)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.show(WindowInsetsCompat.Type.systemBars())
    }
    override fun onPause() {
        super.onPause()
        showSystemUI()
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

        binding.showRollButton.setOnClickListener{
            showRollDialog()
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

        showRollDialog()
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


    private fun calculateMode(values: List<Int>): Int? {
        if (values.isEmpty()) return null
        return values.groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
    }

    //Popup
    private fun reformatLogString(logString: String): String {
        // Split the log string into lines; expects at least 6 lines like "1: <count>"
        val lines = logString.split("\n")
        return if (lines.size >= 6) {
            "   ${lines[0]}      ${lines[3]}\n" +
                    "   ${lines[1]}      ${lines[4]}\n" +
                    "   ${lines[2]}      ${lines[5]}"
        } else {
            logString
        }
    }

    private fun showRollDialog() {
        val currentParty = SharedPrefManager.getCurrentParty(requireContext()) ?: ""
        lifecycleScope.launch {
            val logs = logManager.loadLogs(currentParty)
            if (logs.isNotEmpty()) {
                val lastLog = logs.last()
                val username = lastLog.username
                val formattedLog = reformatLogString(lastLog.log)
                withContext(Dispatchers.Main) {
                    // Inflate the custom last roll dialog layout (which now has an Edit button)
                    val inflater = LayoutInflater.from(requireContext())
                    val dialogView = inflater.inflate(R.layout.dialog_last_roll, null)
                    val titleView = dialogView.findViewById<TextView>(R.id.dialogTitle)
                    val messageView = dialogView.findViewById<TextView>(R.id.dialogMessage)
                    val dismissButton = dialogView.findViewById<Button>(R.id.dismissButton)
                    val editButton = dialogView.findViewById<Button>(R.id.editButton)

                    // Set title and message using the last log data
                    titleView.text = "Last Dice Roll"
                    messageView.text = "${username.replaceFirstChar { it.titlecase() }} Rolled:\n$formattedLog"

                    val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setView(dialogView)
                        .create()

                    dismissButton.setOnClickListener { dialog.dismiss() }

                    // When Edit is pressed, dismiss the current dialog and open the edit dialog.
                    editButton.setOnClickListener {
                        dialog.dismiss()
                        openEditRollDialog(lastLog)
                    }
                    dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
                    dialog.show()
                }
            } else {
                withContext(Dispatchers.Main) {
                    showToast("No logs have been captured")
                }
            }
        }
    }

    private fun openEditRollDialog(lastLog: LogMessage) {
        val context = requireContext()
        val inflater = LayoutInflater.from(context)
        val dialogView = inflater.inflate(R.layout.dialog_edit_log_quantity, null)

        // Parse the existing log string into a map of face -> count.
        val regex = Regex("""(\d+)s?:\s*(\d+)""")
        val countsMapEdit = mutableMapOf(1 to 0, 2 to 0, 3 to 0, 4 to 0, 5 to 0, 6 to 0)
        regex.findAll(lastLog.log).forEach { result ->
            val (faceStr, countStr) = result.destructured
            countsMapEdit[faceStr.toInt()] = countStr.toInt()
        }

        // Retrieve views for each face.
        val face1CountText = dialogView.findViewById<TextView>(R.id.face1_count)
        val face1Minus = dialogView.findViewById<Button>(R.id.face1_minus)
        val face1Plus = dialogView.findViewById<Button>(R.id.face1_plus)

        val face2CountText = dialogView.findViewById<TextView>(R.id.face2_count)
        val face2Minus = dialogView.findViewById<Button>(R.id.face2_minus)
        val face2Plus = dialogView.findViewById<Button>(R.id.face2_plus)

        val face3CountText = dialogView.findViewById<TextView>(R.id.face3_count)
        val face3Minus = dialogView.findViewById<Button>(R.id.face3_minus)
        val face3Plus = dialogView.findViewById<Button>(R.id.face3_plus)

        val face4CountText = dialogView.findViewById<TextView>(R.id.face4_count)
        val face4Minus = dialogView.findViewById<Button>(R.id.face4_minus)
        val face4Plus = dialogView.findViewById<Button>(R.id.face4_plus)

        val face5CountText = dialogView.findViewById<TextView>(R.id.face5_count)
        val face5Minus = dialogView.findViewById<Button>(R.id.face5_minus)
        val face5Plus = dialogView.findViewById<Button>(R.id.face5_plus)

        val face6CountText = dialogView.findViewById<TextView>(R.id.face6_count)
        val face6Minus = dialogView.findViewById<Button>(R.id.face6_minus)
        val face6Plus = dialogView.findViewById<Button>(R.id.face6_plus)

        // Initialize counts from the parsed map.
        face1CountText.text = countsMapEdit[1].toString()
        face2CountText.text = countsMapEdit[2].toString()
        face3CountText.text = countsMapEdit[3].toString()
        face4CountText.text = countsMapEdit[4].toString()
        face5CountText.text = countsMapEdit[5].toString()
        face6CountText.text = countsMapEdit[6].toString()

        // Helper function to setup counter buttons.
        fun setupCounter(minusBtn: Button, plusBtn: Button, countText: TextView) {
            minusBtn.setOnClickListener {
                val current = countText.text.toString().toIntOrNull() ?: 0
                if (current > 0) countText.text = (current - 1).toString()
            }
            plusBtn.setOnClickListener {
                val current = countText.text.toString().toIntOrNull() ?: 0
                countText.text = (current + 1).toString()
            }
        }
        setupCounter(face1Minus, face1Plus, face1CountText)
        setupCounter(face2Minus, face2Plus, face2CountText)
        setupCounter(face3Minus, face3Plus, face3CountText)
        setupCounter(face4Minus, face4Plus, face4CountText)
        setupCounter(face5Minus, face5Plus, face5CountText)
        setupCounter(face6Minus, face6Plus, face6CountText)

        val customTitle = inflater.inflate(R.layout.custom_dialog_title, null)

        val editDialog = androidx.appcompat.app.AlertDialog.Builder(context)
            .setCustomTitle(customTitle)
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                // Reassemble the new log string in the desired two-column format.
                val newLog = "1: ${face1CountText.text}      4: ${face4CountText.text}\n" +
                        "2: ${face2CountText.text}      5: ${face5CountText.text}\n" +
                        "3: ${face3CountText.text}      6: ${face6CountText.text}"
                val currentParty = SharedPrefManager.getCurrentParty(context) ?: ""
                lifecycleScope.launch {
                    logManager.updateLog(currentParty, lastLog.id, newLog)
                    // Optionally refresh your logs or UI here.
                }
            }
            .setNegativeButton("Cancel") { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            .create()

        editDialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        editDialog.show()

        // Optionally, adjust button text colors.
        editDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
            .setTextColor(ContextCompat.getColor(context, R.color.green))
        editDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE)
            .setTextColor(ContextCompat.getColor(context, R.color.red))
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
