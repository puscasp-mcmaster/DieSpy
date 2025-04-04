package com.diespy.app.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.diespy.app.R
import com.diespy.app.databinding.FragmentSettingsBinding
import com.diespy.app.managers.firestore.FireStoreManager
import com.diespy.app.managers.profile.SharedPrefManager
import com.diespy.app.ui.utils.clearError
import com.diespy.app.ui.utils.showError
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
            binding.settingsErrorMessage.clearError()
            deleteAccountConfirmation()
        }
    }

    private fun deleteAccountConfirmation() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_delete_account, null)

        val alertDialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialogView.findViewById<Button>(R.id.cancelButton).setOnClickListener {
            alertDialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.yesButton).setOnClickListener {
            alertDialog.dismiss()
            handleDeleteAccount()
        }

        alertDialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        alertDialog.window?.setDimAmount(0.8f) // 0 = no dim, 1 = full black
        alertDialog.show()
    }

    private fun handleDeleteAccount() {
        val username = SharedPrefManager.getCurrentUsername(requireContext())
        if (username == null) {
            binding.settingsErrorMessage.showError("Username not found.")
            return
        }

        lifecycleScope.launch {
            val userDocumentId = fireStoreManager.getDocumentIdByField("Users", "username", username)

            if (userDocumentId == null) {
                binding.settingsErrorMessage.showError("User not found.")
                return@launch
            }

            val userId = SharedPrefManager.getCurrentUserId(requireContext())
            if (userId == null) {
                binding.settingsErrorMessage.showError("User ID not found.")
                return@launch
            }

            //First remove user from all parties and clean up empty ones
            val cleaned = fireStoreManager.removeUserFromAllParties(userId)

            if (!cleaned) {
                binding.settingsErrorMessage.showError("Failed to remove user from parties.")
                return@launch
            }

            //Then delete user
            val deleteSuccess = fireStoreManager.deleteDocument("Users", userDocumentId)
            if (deleteSuccess) {
                SharedPrefManager.clearCurrentUserData(requireContext())
                findNavController().navigate(R.id.action_settings_to_login)
                Toast.makeText(requireContext(), "You have successfully deleted the account.", Toast.LENGTH_SHORT).show()
            } else {
                binding.settingsErrorMessage.showError("Failed to delete account. Try again.")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
