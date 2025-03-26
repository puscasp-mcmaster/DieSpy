package com.diespy.app.ui.party

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.diespy.app.R
import com.diespy.app.databinding.FragmentPartyBinding
import com.diespy.app.managers.logs.LogManager
import com.diespy.app.managers.party.PartyManager
import com.diespy.app.managers.profile.SharedPrefManager
import com.diespy.app.ui.utils.diceParse
import kotlinx.coroutines.launch

class PartyFragment : Fragment() {

    private var _binding: FragmentPartyBinding? = null
    private val binding get() = _binding!!

    // Local list of party members; UI turn info is now driven by Firebase.
    private var partyMembers: List<String> = emptyList()
    private lateinit var partyManager: PartyManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPartyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        partyManager = PartyManager()

        // Load party name from SharedPrefManager.
        val partyName = SharedPrefManager.getCurrentPartyName(requireContext()) ?: "Party Name"
        binding.partyNameTextView.text = partyName

        val partyId = SharedPrefManager.getCurrentPartyId(requireContext())
        if (partyId != null) {
            // Retrieve party members (if needed for additional UI logic).
            viewLifecycleOwner.lifecycleScope.launch {
                partyMembers = partyManager.getPartyMembersForTurn(partyId)
                // Now subscribe to turn order updates using the deciphered names.
                partyManager.subscribeToTurnOrder(partyId, partyMembers) { currentTurn, nextTurn ->
                    _binding?.let { binding ->
                        binding.currentTurnTextView.text =
                            "It is ${currentTurn.replaceFirstChar { it.titlecase() }}'s Turn!"
                        binding.nextTurnTextView.text =
                            "${nextTurn.replaceFirstChar { it.titlecase() }}'s turn is next!"
                    }
                }
            }

            // Subscribe to real-time log updates.
            partyManager.subscribeToLatestLog(partyId) { lastLog ->
                _binding?.let { binding ->
                    if (lastLog != null) {
                        // Compute the roll sum.
                        val countsMap = diceParse(lastLog.log)
                        val total = countsMap.withIndex().sumOf { (index, count) -> (index + 1) * count }

                        binding.rollUserNameTextView.text =
                            "${lastLog.username.replaceFirstChar { it.titlecase() }} rolled: $total"
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
            }
        } else {
            binding.currentTurnTextView.text = "No party selected"
            binding.nextTurnTextView.text = ""
        }

        // End Turn Button: When clicked, update the turn order in Firebase.
        binding.endTurnButton.setOnClickListener {
            val partyId = SharedPrefManager.getCurrentPartyId(requireContext())
            if (partyId != null) {
                viewLifecycleOwner.lifecycleScope.launch {
                    partyManager.updateTurnOrder(partyId)
                }
            } else {
                Toast.makeText(requireContext(), "No party selected", Toast.LENGTH_SHORT).show()
            }
        }

        // Simulate Roll Button remains unchanged.
        binding.simulateRollButton.setOnClickListener {
            findNavController().navigate(R.id.action_party_to_diceSim)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
