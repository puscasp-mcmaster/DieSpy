package com.diespy.app.ui.join_party

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.diespy.app.R
import com.diespy.app.databinding.FragmentJoinPartyBinding
import com.diespy.app.managers.firestore.FireStoreManager
import com.diespy.app.managers.network.PublicNetworkManager
import com.diespy.app.managers.profile.SharedPrefManager
import com.diespy.app.ui.home.PartyAdapter
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
        val nm = PublicNetworkManager.getInstance(requireContext())
        viewLifecycleOwner.lifecycleScope.launch {
            nm.discoverServices {}
           // Thread.sleep(5000)
            Log.d("NetworkManager", "Ready to party")
            val partyItems = nm.discoveredDeviceMap.values.toList()

            val adapter = PartyAdapter(partyItems) { party ->
                SharedPrefManager.saveCurrentPartyId(requireContext(), party.id)
                SharedPrefManager.saveCurrentPartyName(requireContext(),party.name)
                nm.openClientSocket(party)

                findNavController().navigate(R.id.action_home_to_party)
            }
            binding.joinPartyRecycleView.layoutManager = LinearLayoutManager(requireContext())
            binding.joinPartyRecycleView.adapter = adapter

        }


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

        //reloads search.
        binding.joinPartyConnectButton.setOnClickListener {
            nm.discoverServices {}
            // Thread.sleep(5000)
            Log.d("NetworkManager", "Ready to party")
            val partyItems = nm.discoveredDeviceMap.values.toList()
        }
        binding.joinPartyHostButton.setOnClickListener {
            nm.initAsHost(requireContext())
            Log.d("NetworkManager", "Host init complete")
           /* Thread.sleep(100)
            nm.openClientSocket(null)
            Log.d("NetworkManager", "ClientSocket init complete")
            Thread.sleep(100)
            nm.sendMessageToHost("This is a client message");
            Log.d("NetworkManager", "Client Message Sent complete")
            Thread.sleep(100)
            Log.d("TEST", nm.getMessage())
            nm.sendMessageToClients("This is a host message")
            Log.d("NetworkManager", "Host Message Sent complete")
            Thread.sleep(100)
            Log.d("TEST", nm.getMessage())*/
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

        val userId = SharedPrefManager.getCurrentUserId(context)
        if (userId == null) {
            withContext(Dispatchers.Main) {
                showError("User not found.")
            }
            return
        }

        // Get party data
        val partyData = firestoreManager.queryDocument("Parties", "joinPw", password)
        val userIds = partyData?.get("userIds") as? List<*>
        val partyName = partyData?.get("name") as? String

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
                SharedPrefManager.saveCurrentPartyId(context, partyId)
                if (partyName != null) {
                    SharedPrefManager.saveCurrentPartyName(context, partyName)
                } else {
                    showError("Party name not found. Something went wrong.")
                    return@withContext
                }
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
