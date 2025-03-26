package com.diespy.app.ui.logs

import android.app.AlertDialog
import android.graphics.Rect
import android.text.Spannable
import android.text.SpannableString
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.diespy.app.R
import com.diespy.app.managers.logs.LogManager
import com.diespy.app.managers.logs.LogMessage
import com.diespy.app.managers.profile.SharedPrefManager
import kotlinx.coroutines.launch

class LogAdapter(
    private var logs: List<LogMessage>,
    private val logManager: LogManager,
    private val refreshCallback: () -> Unit,
    private val deleteCallback: (LogMessage) -> Unit
) : RecyclerView.Adapter<LogAdapter.LogViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.log_message_item, parent, false)
        return LogViewHolder(view)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        holder.bind(logs[position], logManager, refreshCallback, deleteCallback)
    }

    override fun getItemCount(): Int = logs.size

    fun updateLogs(newLogs: List<LogMessage>) {
        logs = newLogs
        notifyDataSetChanged()
    }

    class SpaceItemDecoration(private val verticalSpaceHeight: Int) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
            outRect.bottom = verticalSpaceHeight
        }
    }

    class LogViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val logText: TextView = view.findViewById(R.id.logText)
        private val editButton: Button = view.findViewById(R.id.editButton)
        private val deleteButton: Button = view.findViewById(R.id.deleteButton)

        fun bind(
            log: LogMessage,
            logManager: LogManager,
            refreshCallback: () -> Unit,
            deleteCallback: (LogMessage) -> Unit
        ) {
            val context = itemView.context
            val inflater = LayoutInflater.from(context)

            // Format and display log summary
            val regex = Regex("""(\d+)s?:\s*(\d+)""")
            val counts = (1..6).associateWith { 0 }.toMutableMap()
            regex.findAll(log.log).forEach { match ->
                val (face, count) = match.destructured
                counts[face.toInt()] = count.toInt()
            }
            val total = counts.entries.sumOf { it.key * it.value }
            val header = "${log.username.replaceFirstChar { it.titlecase() }} rolled: $total\n"
            val body = (1..3).joinToString("\n") { row ->
                val left = row
                val right = row + 3
                "$left: ${counts[left]}      $right: ${counts[right]}"
            }
            logText.text = SpannableString(header + body).apply {
                setSpan(android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 0, header.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            // Delete logic via callback
            deleteButton.setOnClickListener {
                deleteCallback(log)
            }

            // Edit logic
            editButton.setOnClickListener {
                val dialogView = inflater.inflate(R.layout.dialog_edit_log_quantity, null)
                val faceViews = (1..6).associateWith { face ->
                    Triple(
                        dialogView.findViewById<TextView>(context.resources.getIdentifier("face${face}_count", "id", context.packageName)),
                        dialogView.findViewById<Button>(context.resources.getIdentifier("face${face}_minus", "id", context.packageName)),
                        dialogView.findViewById<Button>(context.resources.getIdentifier("face${face}_plus", "id", context.packageName))
                    )
                }

                faceViews.forEach { (face, views) ->
                    views.first.text = counts[face].toString()
                    views.second.setOnClickListener {
                        val value = views.first.text.toString().toIntOrNull() ?: 0
                        if (value > 0) views.first.text = (value - 1).toString()
                    }
                    views.third.setOnClickListener {
                        val value = views.first.text.toString().toIntOrNull() ?: 0
                        views.first.text = (value + 1).toString()
                    }
                }

                val dialog = AlertDialog.Builder(context)
                    .setCustomTitle(inflater.inflate(R.layout.custom_dialog_title, null))
                    .setView(dialogView)
                    .setPositiveButton("Save") { _, _ ->
                        val newLog = (1..3).joinToString("\n") { row ->
                            val left = row
                            val right = row + 3
                            "${left}s: ${faceViews[left]?.first?.text}      ${right}s: ${faceViews[right]?.first?.text}"
                        }
                        val partyId = SharedPrefManager.getCurrentPartyId(context) ?: return@setPositiveButton
                        (context as? androidx.fragment.app.FragmentActivity)?.lifecycleScope?.launch {
                            logManager.updateLog(partyId, log.id, newLog)
                            refreshCallback()
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .create()

                dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
                dialog.window?.setDimAmount(0.8f)
                dialog.show()

                dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(context, R.color.primary_accent))
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(ContextCompat.getColor(context, R.color.secondary_accent))
            }
        }
    }
}
