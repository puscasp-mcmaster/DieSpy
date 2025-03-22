package com.diespy.app.ui.profile

import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.diespy.app.R
import com.diespy.app.databinding.FragmentProfileBinding
import com.diespy.app.managers.authentication.AuthenticationManager
import com.diespy.app.managers.firestore.FireStoreManager
import com.diespy.app.managers.profile.SharedPrefManager
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {
    private val fireStoreManager = FireStoreManager()
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val authManager = AuthenticationManager()
    private val usersCollection = "Users"



    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toChangePasswordButton.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_changePassword)
        }
        binding.deleteAccountButton.setOnClickListener {
            val dialog = MaterialAlertDialogBuilder(requireContext())
                .setMessage("Are you sure you want to delete? This action is permanent and all of " +
                        "your data will be lost.")
                .setCancelable(false)
                .setPositiveButton("Yes") { _, _ ->
                    handleDeleteAccount()
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

    private fun handleDeleteAccount() {
        val username = SharedPrefManager.getUsername(requireContext()) ?: return

        lifecycleScope.launch {
            val userDocumentId = fireStoreManager.getDocumentIdByField("Users", "username", username)

            if (userDocumentId == null) {
                Toast.makeText(requireContext(), "User not found", Toast.LENGTH_SHORT).show()
                return@launch // Exit coroutine if user is not found
            }

            val deleteSuccess = fireStoreManager.deleteDocument("Users", userDocumentId)
            if (deleteSuccess) {
                SharedPrefManager.clearUserData(requireContext()) // Clears stored login info
                findNavController().navigate(R.id.action_profile_to_login) // Redirects to Login
            } else {
                Toast.makeText(requireContext(), "Failed to delete account. Try again.", Toast.LENGTH_SHORT).show()
            }
            //TODO we need to add in deletion from parties here.
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
