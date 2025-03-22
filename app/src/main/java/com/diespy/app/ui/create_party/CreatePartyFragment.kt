package com.diespy.app.ui.create_party

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.diespy.app.R
import com.diespy.app.databinding.FragmentCreatePartyBinding
import com.diespy.app.managers.party.PartyManager
import com.diespy.app.managers.profile.SharedPrefManager
import kotlinx.coroutines.launch

class CreatePartyFragment : Fragment() {
    private var _binding: FragmentCreatePartyBinding? = null
    private val binding get() = _binding!!
    private val partyManager = PartyManager() // Firestore manager for party creation

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCreatePartyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.createPartyButton.setOnClickListener {
            val partyName = binding.partyNameInput.text.toString().trim()
            val userId = SharedPrefManager.getLoggedInUserId(requireContext()) ?: ""

            if (partyName.isNotEmpty()) {
                lifecycleScope.launch {
                    val success = partyManager.createParty(partyName, userId)
                    if (success != null) {
                        SharedPrefManager.saveCurrentParty(requireContext(),success)
                        binding.partyNameInput.text.clear()
                        Toast.makeText(requireContext(), "Party Created!", Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.action_createParty_to_party, null,
                            NavOptions.Builder()
                                //this will pop back to home instead of coming back to create account screen
                                .setPopUpTo(R.id.homeFragment, true)
                                .build())
                    } else {
                        Toast.makeText(requireContext(), "Failed to create party", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(requireContext(), "Please enter all fields", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
