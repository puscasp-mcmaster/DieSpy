package com.diespy.app.ui.party

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.diespy.app.databinding.FragmentPartyBinding
import com.diespy.app.managers.logs.LogManager
import com.diespy.app.managers.profile.SharedPrefManager
import kotlinx.coroutines.launch

class PartyFragment : Fragment() {

    private var _binding: FragmentPartyBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPartyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load party name from SharedPrefManager
        val partyName = SharedPrefManager.getCurrentPartyName(requireContext()) ?: "Party Name"
        binding.partyNameTextView.text = partyName

        // Set turn information with default text
        binding.currentTurnTextView.text = "It is Player 1's Turn!"
        binding.nextTurnTextView.text = "Player 2's turn is next!"

        // Retrieve the last roll using LogManager and update the previous roll section using regex
        val logManager = LogManager(requireContext())
        lifecycleScope.launch {
            val currentParty = SharedPrefManager.getCurrentPartyId(requireContext()) ?: ""
            val logs = logManager.loadLogs(currentParty)
            if (logs.isNotEmpty()) {
                val lastLog = logs.last()
                binding.rollUserNameTextView.text = "${lastLog.username} rolled:"

                // Use regex to extract dice counts; expects log format like "1s: 0", "2s: 3", etc.
                val regex = Regex("""(\d+)s?:\s*(\d+)""")
                val countsMap = mutableMapOf<Int, Int>()
                regex.findAll(lastLog.log).forEach { result ->
                    val (faceStr, countStr) = result.destructured
                    countsMap[faceStr.toInt()] = countStr.toInt()
                }

                // Populate each dice detail TextView
                binding.diceDetail1.text = "1: ${countsMap[1] ?: 0}"
                binding.diceDetail2.text = "2: ${countsMap[2] ?: 0}"
                binding.diceDetail3.text = "3: ${countsMap[3] ?: 0}"
                binding.diceDetail4.text = "4: ${countsMap[4] ?: 0}"
                binding.diceDetail5.text = "5: ${countsMap[5] ?: 0}"
                binding.diceDetail6.text = "6: ${countsMap[6] ?: 0}"
            } else {
                binding.rollUserNameTextView.text = "No previous roll."
                binding.diceDetail1.text = ""
                binding.diceDetail2.text = ""
                binding.diceDetail3.text = ""
                binding.diceDetail4.text = ""
                binding.diceDetail5.text = ""
                binding.diceDetail6.text = ""
            }
        }

        // End Turn Button listener
        binding.endTurnButton.setOnClickListener {
            Toast.makeText(requireContext(), "End Turn functionality not implemented yet", Toast.LENGTH_SHORT).show()
        }

        // Simulate Roll Button listener
        binding.simulateRollButton.setOnClickListener {
            Toast.makeText(requireContext(), "Simulate Roll functionality not implemented yet", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
