package com.diespy.app.ui.logs

import android.graphics.Rect
import android.text.Spannable
import android.text.SpannableString
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.diespy.app.R
import com.diespy.app.managers.logs.LogManager
import com.diespy.app.managers.logs.LogMessage
import com.diespy.app.managers.profile.SharedPrefManager
import com.diespy.app.ui.utils.diceParse
import com.diespy.app.ui.utils.showEditRollDialog
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

            //Format and display log summary
            val rolls = diceParse(log.log)
            var totalSum = 0
            rolls.forEachIndexed { index, count ->
                totalSum += (count * (index + 1))
            }
            val header = "${log.username.replaceFirstChar { it.titlecase() }} rolled: $totalSum\n"
            val body = (0..2).joinToString("\n") { row ->
                val left = row
                val right = row + 3
                "${left + 1}: ${rolls[left]}      ${right + 1}: ${rolls[right]}"
            }
            logText.text = SpannableString(header + body).apply {
                setSpan(android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 0, header.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            deleteButton.setOnClickListener {
                deleteCallback(log)
            }

            //Editing rolls
            editButton.setOnClickListener {
                val parsedRolls = diceParse(log.log)

                showEditRollDialog(context, parsedRolls) { updatedRolls ->
                    val newLog = "1: ${updatedRolls[0]}      4: ${updatedRolls[3]}\n" +
                            "2: ${updatedRolls[1]}      5: ${updatedRolls[4]}\n" +
                            "3: ${updatedRolls[2]}      6: ${updatedRolls[5]}"
                    val partyId =
                        SharedPrefManager.getCurrentPartyId(context) ?: return@showEditRollDialog
                    (context as? androidx.fragment.app.FragmentActivity)?.lifecycleScope?.launch {
                        logManager.updateLog(partyId, log.id, newLog)
                        refreshCallback()
                    }
                }
            }
        }
    }
}
