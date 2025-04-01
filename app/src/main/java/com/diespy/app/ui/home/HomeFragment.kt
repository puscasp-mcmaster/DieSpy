package com.diespy.app.ui.home


import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.diespy.app.R
import com.diespy.app.databinding.FragmentHomeBinding
import com.diespy.app.managers.firestore.FireStoreManager
import com.diespy.app.managers.network.PublicNetworkManager
import com.diespy.app.managers.profile.SharedPrefManager
import com.diespy.app.ui.utils.showError
import kotlinx.coroutines.launch
import android.net.wifi.p2p.WifiP2pManager
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val fireStoreManager = FireStoreManager()
    private val requestBluetoothPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("Permission", "BLUETOOTH_ADVERTISE permission granted")
        } else {
            Log.e("Permission", "BLUETOOTH_ADVERTISE permission denied")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {

                }
            })

        val nm = PublicNetworkManager.getInstance(requireContext())
        try {
            nm.stopBroadcast()
        } catch (e: Exception) {
            Log.e("Home", "Unable to stop broadcasting: ${e}")
        }
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
            dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
            dialog.window?.setDimAmount(0.8f)
            dialog.show()
        }


        val userId = SharedPrefManager.getCurrentUserId(requireContext())
        if (userId == null) {
            binding.homeErrorMessage.showError("User not logged in")
            binding.partyRecyclerView.visibility = View.GONE
            return
        }

        //Show all parties the user is in
        viewLifecycleOwner.lifecycleScope.launch {
            val partyItems = fireStoreManager.getAllPartiesForUser(userId)

            if (partyItems.isEmpty()) {
                binding.noPartiesFoundText.visibility = View.VISIBLE
                binding.partyRecyclerView.visibility = View.GONE
            } else {
                binding.noPartiesFoundText.visibility = View.GONE
                binding.partyRecyclerView.visibility = View.VISIBLE

                val adapter = PartyAdapter(partyItems) { party ->
                    SharedPrefManager.saveCurrentPartyId(requireContext(), party.id)
                    SharedPrefManager.saveCurrentPartyName(requireContext(), party.name)

                    lifecycleScope.launch {
                        val nm = PublicNetworkManager.getInstance(requireContext())
                        fireStoreManager.preloadPartyData(party.id)
                        findNavController().navigate(R.id.action_home_to_party)
                    }
                    
                }
                binding.partyRecyclerView.layoutManager = LinearLayoutManager(requireContext())
                binding.partyRecyclerView.adapter = adapter
            }
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }





}
