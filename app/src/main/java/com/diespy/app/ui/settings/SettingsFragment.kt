package com.diespy.app.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.diespy.app.R
import com.diespy.app.databinding.FragmentSettingsBinding
import com.diespy.app.managers.firestore.FireStoreManager
import com.diespy.app.managers.profile.SharedPrefManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val fireStoreManager = FireStoreManager()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.deleteAccountButton.setOnClickListener {
            clearError()
            val dialog = MaterialAlertDialogBuilder(requireContext())
                .setMessage("Are you sure you want to delete? This action is permanent and all of your data will be lost.")
                .setCancelable(false)
                .setPositiveButton("Yes") { _, _ -> handleDeleteAccount() }
                .setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
                .create()

            dialog.setOnShowListener {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    ?.setTextColor(resources.getColor(R.color.red, null))
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                    ?.setTextColor(resources.getColor(R.color.black, null))
            }
            dialog.window?.setDimAmount(0.8f) // 0 = no dim, 1 = full black
            dialog.show()
        }
    }

    private fun handleDeleteAccount() {
        val username = SharedPrefManager.getCurrentUsername(requireContext())
        if (username == null) {
            showError("Username not found.")
            return
        }

        lifecycleScope.launch {
            val userDocumentId = fireStoreManager.getDocumentIdByField("Users", "username", username)

            if (userDocumentId == null) {
                showError("User not found.")
                return@launch
            }

            val userId = SharedPrefManager.getCurrentUserId(requireContext())
            if (userId == null) {
                showError("User ID not found.")
                return@launch
            }

            // First remove user from all parties and clean up empty ones
            val cleaned = fireStoreManager.removeUserFromAllParties(userId)

            if (!cleaned) {
                showError("Failed to remove user from parties.")
                return@launch
            }

            // Then delete user
            val deleteSuccess = fireStoreManager.deleteDocument("Users", userDocumentId)
            if (deleteSuccess) {
                SharedPrefManager.clearCurrentUserData(requireContext())
                findNavController().navigate(R.id.action_settings_to_login)
            } else {
                showError("Failed to delete account. Try again.")
            }
        }
    }


    private fun showError(message: String) {
        binding.settingsErrorMessage.text = message
        binding.settingsErrorMessage.visibility = View.VISIBLE
    }

    private fun clearError() {
        binding.settingsErrorMessage.text = ""
        binding.settingsErrorMessage.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
