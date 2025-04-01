
package com.diespy.app.ui.join_party

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.diespy.app.R
import com.diespy.app.databinding.FragmentJoinPartyBinding
import com.diespy.app.managers.firestore.FireStoreManager
import com.diespy.app.managers.network.PublicNetworkManager
import com.diespy.app.managers.profile.SharedPrefManager
import com.diespy.app.ui.utils.showError
import com.diespy.app.ui.utils.clearError
import com.diespy.app.ui.home.PartyAdapter
import com.diespy.app.ui.home.PartyItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class JoinPartyFragment : Fragment() {

    private var _binding: FragmentJoinPartyBinding? = null
    private val binding get() = _binding!!
    private val firestoreManager = FireStoreManager()
    private var partyItems = ArrayList<PartyItem>()
    private var partyNames = ArrayList<String>()
    //_---------------------------------
    private val requestBluetoothPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("Permission", "BLUETOOTH_SCAN permission granted")
        } else {
            Log.e("Permission", "BLUETOOTH_SCAN permission denied")
        }
    }
    //_---------------------------------

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
            var partyName = ""
         //   nm.messageList.forEach {
          //      verifyParty(it)
        //    }
            val adapter = PartyAdapter(partyItems) { party ->
                partyName = party.name
                viewLifecycleOwner.lifecycleScope.launch {
                    if (partyName != "") {
                        joinPartyByField("name", partyName)
                        Log.d("JPF", "Joined party '${partyName}'")
                    }
                }
            }
            binding.joinPartyRecycleView.layoutManager = LinearLayoutManager(requireContext())
            binding.joinPartyRecycleView.adapter = adapter
        }

        binding.joinPartyButton.setOnClickListener {
            val inputPassword = binding.partyPasswordInput.text.toString().trim()
            binding.joinPartyErrorMessage.clearError()
            if (inputPassword.isEmpty()) {
                binding.joinPartyErrorMessage.showError("Please enter a party password.")
                return@setOnClickListener
            }

            lifecycleScope.launch {
                joinPartyByField("joinPw", inputPassword)
            }
        }

        //reloads search.
        binding.joinPartyConnectButton.setOnClickListener {
        //if we want to reload, enable
            partyNames.clear()
            partyItems.clear()
            nm.messageList.clear()

            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestBluetoothPermission.launch(Manifest.permission.BLUETOOTH_SCAN)
            } else {
                    binding.joinPartyNoUpdateText.text = "Searching for avaliable parties..."
                    Handler(Looper.getMainLooper()).postDelayed(
                        {
                            viewLifecycleOwner.lifecycleScope.launch {
                                var updateAdapter = false
                                nm.messageList.forEach {
                                    if(!partyNames.contains(it)) {
                                        Log.d("Messages", "New Party Recieved: $it")
                                        partyNames.add(it)
                                        verifyParty(it)
                                        updateAdapter = true
                                    }
                                }
                                binding.joinPartyRecycleView.adapter?.notifyDataSetChanged()
                                if(updateAdapter) {
                                    binding.joinPartyNoUpdateText.text = ""
                                } else {
                                    binding.joinPartyNoUpdateText.text = "No parties detected"
                                }

                            }
                            Log.d("JPF", "Listening done!")

                        }, 6000
                    )

                    nm.listen()

            }

        }
    }

    public suspend fun verifyParty(partyName: String) {
        try {
            val partyData = firestoreManager.queryDocument("Parties", "name", partyName)
        } catch(e: Exception) {
            Log.e("JoinPartyFragment", "Error trying to find party ${partyName}: ${e}")
            return
        }
        partyItems.add(PartyItem("e", partyName, 0))

    }

    private suspend fun joinPartyByField(field: String, value: String) {
        val context = requireContext()

        val partyId = firestoreManager.getDocumentIdByField("Parties", field, value)
        if (partyId == null) {
            withContext(Dispatchers.Main) {
                binding.joinPartyErrorMessage.showError("No party found with that password.")
            }
            return
        }

        val userId = SharedPrefManager.getCurrentUserId(context)
        if (userId == null) {
            withContext(Dispatchers.Main) {
                binding.joinPartyErrorMessage.showError("User not found.")
            }
            return
        }

        val partyData = firestoreManager.queryDocument("Parties", field, value)
        val userIds = partyData?.get("userIds") as? List<*>
        val partyName = partyData?.get("name") as? String


        if (userIds != null && userIds.contains(userId)) {
            withContext(Dispatchers.Main) {
                binding.joinPartyErrorMessage.showError("You're already in this party!")
            }
            return
        }

        val success = firestoreManager.updateDocument("Parties", partyId, mapOf(
            "userIds" to com.google.firebase.firestore.FieldValue.arrayUnion(userId)
        ))


        withContext(Dispatchers.Main) {
            if (success) {
                SharedPrefManager.saveCurrentPartyId(context, partyId)
                if (partyName != null) {
                    SharedPrefManager.saveCurrentPartyName(context, partyName)

                } else {
                    binding.joinPartyErrorMessage.showError("Party name not found. Something went wrong.")
                    return@withContext
                }
                Toast.makeText(context, "Party joined!", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_joinParty_to_party)
            } else {
                binding.joinPartyErrorMessage.showError("Failed to join the party. Please try again.")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
