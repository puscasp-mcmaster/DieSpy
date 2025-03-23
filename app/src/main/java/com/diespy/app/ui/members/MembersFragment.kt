package com.diespy.app.ui.members

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.diespy.app.R
import com.diespy.app.databinding.FragmentMembersBinding
import com.diespy.app.managers.firestore.FireStoreManager
import com.diespy.app.managers.profile.SharedPrefManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MembersFragment : Fragment() {

    private var _binding: FragmentMembersBinding? = null
    private val binding get() = _binding!!
    private val fireStoreManager = FireStoreManager()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMembersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val partyId = SharedPrefManager.getCurrentParty(requireContext())
        if (partyId == null) {
            Toast.makeText(requireContext(), "No party selected", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val memberUsernames = fireStoreManager.getUsernamesForParty(partyId)

            if (memberUsernames.isEmpty()) {
                binding.noMembersText.visibility = View.VISIBLE
                binding.membersRecyclerView.visibility = View.GONE
            } else {
                binding.noMembersText.visibility = View.GONE
                binding.membersRecyclerView.visibility = View.VISIBLE

                val adapter = MembersAdapter(memberUsernames)
                binding.membersRecyclerView.layoutManager = LinearLayoutManager(requireContext())
                binding.membersRecyclerView.adapter = adapter
            }
        }

        binding.leavePartyButton.setOnClickListener {
            val context = requireContext()
            val partyId = SharedPrefManager.getCurrentParty(context)
            val userId = SharedPrefManager.getLoggedInUserId(context)

            if (partyId != null && userId != null) {
                lifecycleScope.launch {
                    val success = FireStoreManager().updateDocument("Parties", partyId, mapOf(
                        "userIds" to com.google.firebase.firestore.FieldValue.arrayRemove(userId)
                    ))

                    if (success) {
                        SharedPrefManager.clearCurrentParty(context)
                        findNavController().navigate(R.id.action_members_to_home)
                    } else {
                        Toast.makeText(context, "Failed to leave party. Try again.", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(context, "Missing party or user info.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
