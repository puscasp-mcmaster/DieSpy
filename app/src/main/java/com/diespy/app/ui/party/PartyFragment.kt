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
import com.diespy.app.managers.firestore.FireStoreManager
import com.diespy.app.managers.profile.SharedPrefManager
import com.diespy.app.ui.utils.diceParse
import kotlinx.coroutines.launch

class PartyFragment : Fragment() {

    private var _binding: FragmentPartyBinding? = null
    private val binding get() = _binding!!

    // List to hold party members and an index for the current turn.
    private var partyMembers: List<String> = emptyList()
    private var currentTurnIndex: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPartyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load party name from SharedPrefManager.
        val partyName = SharedPrefManager.getCurrentPartyName(requireContext()) ?: "Party Name"
        binding.partyNameTextView.text = partyName

        // Load party members from FireStore.
        val partyId = SharedPrefManager.getCurrentPartyId(requireContext())
        if (partyId != null) {
            val fireStoreManager = FireStoreManager()
            viewLifecycleOwner.lifecycleScope.launch {
                partyMembers = fireStoreManager.getUsernamesForParty(partyId)
                // If only one member, duplicate it so current and next show the same.
                if (partyMembers.size == 1) {
                    partyMembers = listOf(partyMembers[0], partyMembers[0])
                }
                updateTurnUI()
            }
        } else {
            binding.currentTurnTextView.text = "No party selected"
            binding.nextTurnTextView.text = ""
        }

        // Retrieve the last roll using LogManager (unchanged).
        val logManager = LogManager(requireContext())
        viewLifecycleOwner.lifecycleScope.launch {
            val currentPartyId = SharedPrefManager.getCurrentPartyId(requireContext()) ?: ""
            val logs = logManager.loadLogs(currentPartyId)
            if (logs.isNotEmpty()) {
                val lastLog = logs.last()
                binding.rollUserNameTextView.text = "${lastLog.username.replaceFirstChar { it.titlecase() }} rolled:"

                val countsMap = diceParse(lastLog.log)
                binding.diceDetail1.text = "1: ${countsMap[0]}"
                binding.diceDetail2.text = "2: ${countsMap[1]}"
                binding.diceDetail3.text = "3: ${countsMap[2]}"
                binding.diceDetail4.text = "4: ${countsMap[3]}"
                binding.diceDetail5.text = "5: ${countsMap[4]}"
                binding.diceDetail6.text = "6: ${countsMap[5]}"
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

        // End Turn Button cycles through party members.
        binding.endTurnButton.setOnClickListener {
            if (partyMembers.size > 1) {
                currentTurnIndex = (currentTurnIndex + 1) % partyMembers.size
                updateTurnUI()
            } else {
                Toast.makeText(requireContext(), "Only one member in party", Toast.LENGTH_SHORT).show()
            }
        }

        // Simulate Roll Button remains unchanged.
        binding.simulateRollButton.setOnClickListener {
            Toast.makeText(requireContext(), "Simulate Roll functionality not implemented yet", Toast.LENGTH_SHORT).show()
        }
    }

    // Updates the current and next turn TextViews based on the partyMembers list and currentTurnIndex.
    private fun updateTurnUI() {
        if (partyMembers.isEmpty()) {
            binding.currentTurnTextView.text = "No members available"
            binding.nextTurnTextView.text = ""
        } else if (partyMembers.size == 1) {
            binding.currentTurnTextView.text = "It is ${partyMembers[0].replaceFirstChar { it.titlecase() }}'s Turn!"
            binding.nextTurnTextView.text = "${partyMembers[0].replaceFirstChar { it.titlecase() }}'s turn is next!"
        } else {
            binding.currentTurnTextView.text = "It is ${partyMembers[currentTurnIndex].replaceFirstChar { it.titlecase() }}'s Turn!"
            val nextIndex = (currentTurnIndex + 1) % partyMembers.size
            binding.nextTurnTextView.text = "${partyMembers[nextIndex].replaceFirstChar { it.titlecase() }}'s turn is next!"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
