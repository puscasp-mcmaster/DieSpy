package com.diespy.app.ui.logs

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.diespy.app.R
import com.diespy.app.databinding.FragmentLogsBinding
import com.diespy.app.managers.logs.LogManager
import com.diespy.app.managers.logs.LogMessage
import com.diespy.app.managers.profile.SharedPrefManager
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

        logAdapter = LogAdapter(
            logs = emptyList(),
            logManager = logManager,
            refreshCallback = { loadLogs() },
            deleteCallback = { log -> showDeleteConfirmationDialog(log) }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext()).apply {
                stackFromEnd = true
            }
            adapter = logAdapter
            addItemDecoration(LogAdapter.SpaceItemDecoration(16))
        }

        val currentParty = SharedPrefManager.getCurrentPartyId(requireContext()) ?: return
        logManager.subscribeToLogs(currentParty) { newLogs ->
            _binding?.let { binding ->
                logAdapter.updateLogs(newLogs)
                val layoutManager = binding.recyclerView.layoutManager as? LinearLayoutManager
                layoutManager?.let {
                    val lastVisibleItem = it.findLastVisibleItemPosition()
                    // Use a threshold; adjust as needed
                    if (lastVisibleItem >= logAdapter.itemCount - 3) {
                        binding.recyclerView.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
                            override fun onLayoutChange(
                                v: View, left: Int, top: Int, right: Int, bottom: Int,
                                oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int
                            ) {
                                binding.recyclerView.removeOnLayoutChangeListener(this)
                                binding.recyclerView.smoothScrollToPosition(logAdapter.itemCount - 1)
                            }
                        })
                    }
                }
            }
        }
    }

    private fun loadLogs() {
        val currentParty = SharedPrefManager.getCurrentPartyId(requireContext()) ?: return
        lifecycleScope.launch {
            val logs = logManager.loadLogs(currentParty)
            _binding?.let { binding ->
                logAdapter.updateLogs(logs)
            }
        }
    }

    private fun showDeleteConfirmationDialog(log: LogMessage) {
        val context = requireContext()
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_delete_log, null)

        val alertDialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .create()

        dialogView.findViewById<Button>(R.id.noButton).setOnClickListener {
            alertDialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.yesButton).setOnClickListener {
            val currentParty = SharedPrefManager.getCurrentPartyId(context) ?: return@setOnClickListener
            lifecycleScope.launch {
                logManager.deleteLog(currentParty, log.id)
                loadLogs()
            }
            alertDialog.dismiss()
        }

        alertDialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        alertDialog.window?.setDimAmount(0.8f)
        alertDialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
