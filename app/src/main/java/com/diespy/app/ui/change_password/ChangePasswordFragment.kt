package com.diespy.app.ui.change_password

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.diespy.app.R
import com.diespy.app.databinding.FragmentChangePasswordBinding
import com.diespy.app.managers.authentication.AuthenticationManager
import com.diespy.app.managers.profile.SharedPrefManager
import kotlinx.coroutines.launch

class ChangePasswordFragment : Fragment() {

    private var _binding: FragmentChangePasswordBinding? = null
    private val binding get() = _binding!!
    private val authManager = AuthenticationManager()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChangePasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toProfileScreenButton.setOnClickListener {
            findNavController().navigate(R.id.action_changePassword_to_profile)
        }
        binding.changePasswordButton.setOnClickListener {
            handleChangePassword()
        }

    }
    private fun handleChangePassword() {
        val oldPassword = binding.oldPwInput.text.toString().trim().lowercase()
        val newPassword1 = binding.newPwInput1.text.toString().trim()
        val newPassword2 = binding.newPwInput2.text.toString().trim()

        val validationError = validateInput(oldPassword, newPassword1, newPassword2)
        if (validationError != null) {
            showError(validationError)
            return
        }

        lifecycleScope.launch {
            //Grab username
            val username = (SharedPrefManager.getUsername(requireContext()))?: ""
            if (username == ""){
                showError("Username error. Try again.")
                return@launch
            }
            //Try to make sure the username and password combo is correct
            val isAuthenticated = authManager.authenticate(username, oldPassword)

            if (isAuthenticated != 1) {
                showError("Wrong old password.")
                return@launch
            }

            //Change the password
            val success = authManager.changePassword(username,newPassword1)

            if (success) {
                Toast.makeText(requireContext(), "Password successfully changed.", Toast.LENGTH_SHORT).show()
                clearFields()
                binding.createAccountErrorMessage.text = "" // Clear error message
                findNavController().navigate(R.id.action_changePassword_to_profile) // Navigate to home screen
            } else {
                showError("Failed to change password. Try again.")
            }
        }
    }

    private fun validateInput(oldPassword: String, password1: String, password2: String): String? {
        return when {
            oldPassword.isBlank() || password1.isBlank() || password2.isBlank() -> "Password cannot be empty."
            password1 != password2 -> "Passwords do not match"
            password1.length < 6 -> "Password must be at least 6 characters"
            else -> null // No errors
        }
    }

    private fun showError(message: String) {
        binding.createAccountErrorMessage.apply {
            text = message
            visibility = View.VISIBLE
        }
    }

    private fun clearFields() {
        binding.oldPwInput.text.clear()
        binding.newPwInput1.text.clear()
        binding.newPwInput2.text.clear()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
