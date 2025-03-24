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
        var nm = PublicNetworkManager.getInstance(requireContext())
        binding.joinPartyButton.setOnClickListener {

            val inputPassword = binding.partyPasswordInput.text.toString().trim()

            if (inputPassword.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a party password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                joinPartyWithPassword(inputPassword)
            }
        }

        /*lifecycleScope.launch {
            val networkManager = NetworkManager(requireContext())

            networkManager.discoverServices { devices ->
                if (devices.isEmpty()) {
                    binding.joinPartyRecycleView.visibility = View.GONE
                } else {
                    binding.joinPartyRecycleView.visibility = View.VISIBLE

                    val adapter = JoinPartyAdapter(devices) { selectedDevice ->
                        Log.d("NetworkManager", "Selected device: ${selectedDevice.deviceName}")
                        networkManager.connectToDevice(selectedDevice)
                    }
                    binding.joinPartyRecycleView.layoutManager = LinearLayoutManager(requireContext())
                    binding.joinPartyRecycleView.adapter = adapter
                }
            }
        }*/


        binding.joinPartyConnectButton.setOnClickListener {
            Log.d("JoinParty", "Searching for avaliable devices.")
            nm.discoverServices { devices ->
                Log.d("NJoinParty", "Devices in callback: ${devices.size}")
                if (devices.isNotEmpty()) {
                    val deviceListString = devices.joinToString(separator = "\n") { device ->
                        device.deviceName
                    }
                    Log.d("JoinParty", "Devicestring: ${deviceListString}")
                    requireActivity().runOnUiThread {
                        binding.joinPartyListHeader.text = deviceListString
                    }
                } else {
                    Log.e("JoinParty", "No devices found!")
                    requireActivity().runOnUiThread {
                        binding.joinPartyListHeader.text = "No devices found."
                    }
                }
            }
            //Try to join selected wifip2p lobby*/
        }
        binding.joinPartyHostButton.setOnClickListener {
            nm.initAsHost()
        }


    }

    private suspend fun joinPartyWithPassword(password: String) {
        val context = requireContext()

        val partyId = firestoreManager.getDocumentIdByField("Parties", "joinPw", password)
        if (partyId == null) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "No party found with that password.", Toast.LENGTH_SHORT).show()
            }
            return
        }

        val userId = SharedPrefManager.getLoggedInUserId(context)
        if (userId == null) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "User not found.", Toast.LENGTH_SHORT).show()
            }
            return
        }

        // Get party data
        val partyData = firestoreManager.queryDocument("Parties", "joinPw", password)
        val userIds = partyData?.get("userIds") as? List<*>

        if (userIds != null && userIds.contains(userId)) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "You're already in this party!", Toast.LENGTH_SHORT).show()
            }
            return
        }

        // Add user to the party's 'userIds'
        val success = firestoreManager.updateDocument("Parties", partyId, mapOf(
            "userIds" to com.google.firebase.firestore.FieldValue.arrayUnion(userId)
        ))

        if (success) {
            SharedPrefManager.saveCurrentParty(context, partyId)
            withContext(Dispatchers.Main) {
                findNavController().navigate(R.id.action_joinParty_to_party)
            }
        } else {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Failed to join the party. Please try again.", Toast.LENGTH_SHORT).show()
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
