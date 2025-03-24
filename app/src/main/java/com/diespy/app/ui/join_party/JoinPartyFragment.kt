package com.diespy.app.ui.join_party

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.diespy.app.R
import com.diespy.app.databinding.FragmentJoinPartyBinding
import com.diespy.app.managers.firestore.FireStoreManager
import com.diespy.app.managers.profile.SharedPrefManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class JoinPartyFragment : Fragment() {

    private var _binding: FragmentJoinPartyBinding? = null
    private val binding get() = _binding!!
    private val firestoreManager = FireStoreManager()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentJoinPartyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.joinPartyButton.setOnClickListener {
            val inputPassword = binding.partyPasswordInput.text.toString().trim()
            clearError()

            if (inputPassword.isEmpty()) {
                showError("Please enter a party password.")
                return@setOnClickListener
            }

            lifecycleScope.launch {
                joinPartyWithPassword(inputPassword)
            }
        }
    }

    private suspend fun joinPartyWithPassword(password: String) {
        val context = requireContext()

        val partyId = firestoreManager.getDocumentIdByField("Parties", "joinPw", password)
        if (partyId == null) {
            withContext(Dispatchers.Main) {
                showError("No party found with that password.")
            }
            return
        }

        val userId = SharedPrefManager.getLoggedInUserId(context)
        if (userId == null) {
            withContext(Dispatchers.Main) {
                showError("User not found.")
            }
            return
        }

        // Get party data
        val partyData = firestoreManager.queryDocument("Parties", "joinPw", password)
        val userIds = partyData?.get("userIds") as? List<*>

        if (userIds != null && userIds.contains(userId)) {
            withContext(Dispatchers.Main) {
                showError("You're already in this party!")
            }
            return
        }

        // Add user to the party's 'userIds'
        val success = firestoreManager.updateDocument("Parties", partyId, mapOf(
            "userIds" to com.google.firebase.firestore.FieldValue.arrayUnion(userId)
        ))

        withContext(Dispatchers.Main) {
            if (success) {
                SharedPrefManager.saveCurrentParty(context, partyId)
                Toast.makeText(context, "Party joined!", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_joinParty_to_party)
            } else {
                showError("Failed to join the party. Please try again.")
            }
        }
    }

    private fun showError(message: String) {
        binding.joinPartyErrorMessage.apply {
            text = message
            visibility = View.VISIBLE
        }
    }

    private fun clearError() {
        binding.joinPartyErrorMessage.apply {
            text = ""
            visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
