package com.diespy.app.ui.members

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.diespy.app.R
import com.diespy.app.databinding.FragmentMembersBinding
import com.diespy.app.managers.firestore.FireStoreManager
import com.diespy.app.managers.profile.PartyCacheManager
import com.diespy.app.managers.profile.SharedPrefManager
import com.diespy.app.ui.utils.showError
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch

class MembersFragment : Fragment() {
    private var membersListener: ListenerRegistration? = null
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

        val partyId = SharedPrefManager.getCurrentPartyId(requireContext())
        if (partyId == null) {
            binding.membersErrorText.text = "No party selected."
            binding.membersErrorText.visibility = View.VISIBLE
            binding.membersRecyclerView.visibility = View.GONE
            binding.noMembersText.visibility = View.GONE
            return
        }
        //Check Cache
        val cachedUsernames = PartyCacheManager.userIds.mapNotNull { PartyCacheManager.usernames[it] }
        if (cachedUsernames.isNotEmpty()) {
            binding.noMembersText.visibility = View.GONE
            binding.membersRecyclerView.visibility = View.VISIBLE
            val adapter = MembersAdapter(cachedUsernames)
            binding.membersRecyclerView.layoutManager = LinearLayoutManager(requireContext())
            binding.membersRecyclerView.adapter = adapter
        }

        val cachedCode = PartyCacheManager.joinPw
        if (!cachedCode.isNullOrEmpty()) {
            binding.partyCodeText.text = "Party Code: $cachedCode"
            binding.partyCodeText.visibility = View.VISIBLE
        } else {
            binding.partyCodeText.visibility = View.GONE
        }

        membersListener = subscribeToPartyMembers(partyId) { userIds ->
            lifecycleScope.launch {
                val partySnapshot = fireStoreManager.getDocumentById("Parties", partyId)
                val partyCode = partySnapshot?.get("joinPw") as? String

                if (!partyCode.isNullOrEmpty()) {
                    binding.partyCodeText.text = "Party Code: $partyCode"
                    binding.partyCodeText.visibility = View.VISIBLE
                }
                val memberUsernames = mutableListOf<String>()

                //cache usernames if possible
                for (id in userIds) {
                    val cached = PartyCacheManager.usernames[id]
                    if (cached != null) {
                        memberUsernames.add(cached)
                    } else {
                        val userData = fireStoreManager.getDocumentById("Users", id)
                        val username = userData?.get("username") as? String ?: "Unknown"
                        memberUsernames.add(username)
                        PartyCacheManager.usernames = PartyCacheManager.usernames + (id to username)
                    }
                }

                //Update cache with new userIds
                PartyCacheManager.userIds = userIds

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
        }


        binding.leavePartyButton.setOnClickListener {
            partyLeaveConfirmation()
        }
    }

    //Live updates for party members
    private fun subscribeToPartyMembers(
        partyId: String,
        onUpdate: (List<String>) -> Unit
    ): ListenerRegistration {
        val firestore = FirebaseFirestore.getInstance()
        val partyDocRef = firestore.collection("Parties").document(partyId)
        return partyDocRef.addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener

            val userIds = snapshot?.get("userIds") as? List<String> ?: emptyList()
            onUpdate(userIds)
        }
    }

    //Confirm for leave party, once done, clear cache
    private fun partyLeaveConfirmation() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_leave_party, null)

        val alertDialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialogView.findViewById<Button>(R.id.cancelButton).setOnClickListener {
            alertDialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.yesButton).setOnClickListener {
            alertDialog.dismiss()
            val context = requireContext()
            val partyId = SharedPrefManager.getCurrentPartyId(context)
            val userId = SharedPrefManager.getCurrentUserId(context)

            if (partyId != null && userId != null) {
                lifecycleScope.launch {
                    val success = fireStoreManager.leavePartyAndDeleteIfEmpty(partyId, userId)

                    if (success) {
                        SharedPrefManager.clearCurrentPartyData(context)
                        findNavController().navigate(R.id.action_members_to_home)
                        Toast.makeText(requireContext(), "You have successfully left the party", Toast.LENGTH_SHORT).show()
                    } else {
                        binding.membersErrorText.showError("Failed to leave party. Try again.")
                    }
                }
            } else {
                binding.membersErrorText.showError("Missing party or user info.")
            }
        }

        alertDialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        alertDialog.window?.setDimAmount(0.8f)
        alertDialog.show()
    }

    override fun onDestroyView() {
        membersListener?.remove()
        super.onDestroyView()
        _binding = null
    }
}
