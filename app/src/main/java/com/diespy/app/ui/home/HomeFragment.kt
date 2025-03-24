package com.diespy.app.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.diespy.app.R
import com.diespy.app.databinding.FragmentHomeBinding
import com.diespy.app.managers.firestore.FireStoreManager
import com.diespy.app.managers.profile.SharedPrefManager
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val fireStoreManager = FireStoreManager()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.addPartyButton.setOnClickListener {
            val dialogView = layoutInflater.inflate(R.layout.dialog_add_party, null)
            val dialog = AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create()

            dialogView.findViewById<Button>(R.id.createPartyButton).setOnClickListener {
                dialog.dismiss()
                findNavController().navigate(R.id.action_home_to_createParty)
            }

            dialogView.findViewById<Button>(R.id.joinPartyButton).setOnClickListener {
                dialog.dismiss()
                findNavController().navigate(R.id.action_home_to_joinParty)
            }

            dialog.show()
        }


        val userId = SharedPrefManager.getLoggedInUserId(requireContext())
        if (userId == null) {
            showError("User not logged in.")
            binding.partyRecyclerView.visibility = View.GONE
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val partyItems = fireStoreManager.getAllPartiesForUser(userId)

            if (partyItems.isEmpty()) {
                binding.noPartiesFoundText.visibility = View.VISIBLE
                binding.partyRecyclerView.visibility = View.GONE
            } else {
                binding.noPartiesFoundText.visibility = View.GONE
                binding.partyRecyclerView.visibility = View.VISIBLE

                val adapter = PartyAdapter(partyItems) { partyId ->
                    SharedPrefManager.saveCurrentParty(requireContext(), partyId)
                    findNavController().navigate(R.id.action_home_to_party)
                }
                binding.partyRecyclerView.layoutManager = LinearLayoutManager(requireContext())
                binding.partyRecyclerView.adapter = adapter
            }
        }

    }

    private fun showError(message: String) {
        binding.homeErrorMessage.text = message
        binding.homeErrorMessage.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
