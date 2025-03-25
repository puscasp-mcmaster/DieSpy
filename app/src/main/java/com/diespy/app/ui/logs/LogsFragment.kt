package com.diespy.app.ui.logs

import android.app.AlertDialog
import android.graphics.Rect
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.diespy.app.R
import com.diespy.app.databinding.FragmentLogsBinding
import com.diespy.app.managers.logs.LogManager
import com.diespy.app.managers.logs.LogMessage
import com.diespy.app.managers.profile.SharedPrefManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class LogsFragment : Fragment() {

    private var _binding: FragmentLogsBinding? = null
    private val binding get() = _binding!!
    private lateinit var logManager: LogManager
    private lateinit var logAdapter: LogAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLogsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        logManager = LogManager(requireContext())
        logAdapter = LogAdapter(emptyList(), logManager) { refreshLogs() }
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        binding.recyclerView.adapter = logAdapter
        binding.recyclerView.addItemDecoration(LogAdapter.SpaceItemDecoration(16))
        loadLogs()

    }

    private fun loadLogs() {
        val currentParty = SharedPrefManager.getCurrentPartyId(requireContext()) ?: ""
        lifecycleScope.launch {
            val logs = logManager.loadLogs(currentParty)
            logAdapter.updateLogs(logs)
        }
    }

    private fun refreshLogs() {
        loadLogs()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class LogAdapter(
    private var logs: List<LogMessage>,
    private val logManager: LogManager,
    private val refreshCallback: () -> Unit
) : RecyclerView.Adapter<LogAdapter.LogViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.log_message_item, parent, false)
        return LogViewHolder(view)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        val log = logs[position]
        holder.bind(log, logManager, refreshCallback)
    }


    override fun getItemCount(): Int = logs.size

    fun updateLogs(newLogs: List<LogMessage>) {
        logs = newLogs
        notifyDataSetChanged()
    }

    class SpaceItemDecoration(private val verticalSpaceHeight: Int) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(
            outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State
        ) {
            outRect.bottom = verticalSpaceHeight
        }
    }

    class LogViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val logText: TextView = view.findViewById(R.id.logText)
        private val editButton: Button = view.findViewById(R.id.editButton)
        private val deleteButton: Button = view.findViewById(R.id.deleteButton)

        fun bind(log: LogMessage, logManager: LogManager, refreshCallback: () -> Unit) {
            val regex = Regex("""(\d+)s?:\s*(\d+)""")
            val countsMap = mutableMapOf<Int, Int>()
            regex.findAll(log.log).forEach { result ->
                val (faceStr, countStr) = result.destructured
                countsMap[faceStr.toInt()] = countStr.toInt()
            }
            var totalSum = 0
            for (face in 1..6) {
                totalSum += face * (countsMap[face] ?: 0)
            }
            val header = "${log.username.replaceFirstChar { it.titlecase() }} rolled: $totalSum\n"


            val formattedLog = "1: ${countsMap[1] ?: 0}      4: ${countsMap[4] ?: 0}\n" +
                    "2: ${countsMap[2] ?: 0}      5: ${countsMap[5] ?: 0}\n" +
                    "3: ${countsMap[3] ?: 0}      6: ${countsMap[6] ?: 0}"
            // Set the logText to display the username, computed total, and the formatted log.
            val fullText = header + formattedLog

            // Create a SpannableString from the full text and make the header bold.
            val spannable = SpannableString(fullText)
            spannable.setSpan(
                android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                0, header.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            logText.text = spannable

            editButton.setOnClickListener {
                val context = itemView.context
                val inflater = LayoutInflater.from(context)
                val dialogView = inflater.inflate(R.layout.dialog_edit_log_quantity, null)

                // Parse the existing log text into a map of face -> count.
                val currentLog = log.log
                val countsMapEdit = mutableMapOf(1 to 0, 2 to 0, 3 to 0, 4 to 0, 5 to 0, 6 to 0)
                regex.findAll(currentLog).forEach { result ->
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

                // Helper: function to update a counter.
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
                val customTitle = LayoutInflater.from(context).inflate(R.layout.custom_dialog_title, null)
                val dialog = AlertDialog.Builder(context)
                    .setCustomTitle(customTitle)
                    .setView(dialogView)
                    .setPositiveButton("Save") { _, _ ->
                        // Reassemble the new log string in the desired two-column format.
                        val newLog = "1s: ${face1CountText.text}      4s: ${face4CountText.text}\n" +
                                "2s: ${face2CountText.text}      5s: ${face5CountText.text}\n" +
                                "3s: ${face3CountText.text}      6s: ${face6CountText.text}"
                        val currentParty = SharedPrefManager.getCurrentPartyId(context) ?: ""
                        (context as? androidx.fragment.app.FragmentActivity)?.lifecycleScope?.launch {
                            logManager.updateLog(currentParty, log.id, newLog)
                            refreshCallback()
                        }
                    }
                    .setNegativeButton("Cancel") { dialogInterface, _ ->
                        dialogInterface.dismiss()
                    }
                    .create()
                dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
                dialog.show()

                dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setTextColor(ContextCompat.getColor(context, R.color.green))
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                    .setTextColor(ContextCompat.getColor(context, R.color.red))
            }

            deleteButton.setOnClickListener {
                val context = itemView.context
                val dialog = MaterialAlertDialogBuilder(context)
                    .setMessage("Are you sure you want to delete this roll?")
                    .setCancelable(false)
                    .setPositiveButton("Yes") { _, _ ->
                        val currentParty = SharedPrefManager.getCurrentPartyId(context) ?: ""
                        (context as? androidx.fragment.app.FragmentActivity)?.lifecycleScope?.launch {
                            logManager.deleteLog(currentParty, log.id)
                            refreshCallback()
                        }
                    }
                    .setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
                    .create()

                dialog.setOnShowListener {
                    dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                        ?.setTextColor(ContextCompat.getColor(context, R.color.red))
                    dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE)
                        ?.setTextColor(ContextCompat.getColor(context, R.color.black))
                }
                dialog.show()
            }
        }
    }
}
