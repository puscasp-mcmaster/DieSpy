package com.diespy.app.ui.members

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.diespy.app.R
import com.diespy.app.databinding.FragmentMembersBinding
import com.diespy.app.managers.firestore.FireStoreManager
import com.diespy.app.managers.profile.SharedPrefManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

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
            binding.membersErrorText.text = "No party selected."
            binding.membersErrorText.visibility = View.VISIBLE
            binding.membersRecyclerView.visibility = View.GONE
            binding.noMembersText.visibility = View.GONE
            return
        }


        lifecycleScope.launch {
            // Load members
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

            // Load and show party code
            val partySnapshot = fireStoreManager.getDocumentById("Parties", partyId)
            val partyCode = partySnapshot?.get("joinPw") as? String

            if (!partyCode.isNullOrEmpty()) {
                binding.partyCodeText.text = "Party Code: $partyCode"
                binding.partyCodeText.visibility = View.VISIBLE
            }
        }

        binding.leavePartyButton.setOnClickListener {
            val dialog = MaterialAlertDialogBuilder(requireContext())
                .setMessage("Are you sure you want to leave the party? You will not be able to join back without a join password.")
                .setCancelable(false)
                .setPositiveButton("Yes") { _, _ ->
                    val context = requireContext()
                    val partyId = SharedPrefManager.getCurrentParty(context)
                    val userId = SharedPrefManager.getLoggedInUserId(context)

                    if (partyId != null && userId != null) {
                        lifecycleScope.launch {
                            val success = fireStoreManager.updateDocument("Parties", partyId, mapOf(
                                "userIds" to com.google.firebase.firestore.FieldValue.arrayRemove(userId)
                            ))

                            if (success) {
                                SharedPrefManager.clearCurrentParty(context)
                                findNavController().navigate(R.id.action_members_to_home)
                            } else {
                                binding.membersErrorText.text = "Failed to leave party. Try again."
                                binding.membersErrorText.visibility = View.VISIBLE
                            }
                        }
                    } else {
                        binding.membersErrorText.text = "Missing party or user info."
                        binding.membersErrorText.visibility = View.VISIBLE
                    }
                }
                .setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
                .create()

            dialog.setOnShowListener {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    ?.setTextColor(resources.getColor(R.color.red, null))
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                    ?.setTextColor(resources.getColor(R.color.black, null))
            }
            dialog.show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
