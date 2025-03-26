package com.diespy.app.ui.create_account

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.diespy.app.R
import com.diespy.app.databinding.FragmentCreateAccountBinding
import com.diespy.app.managers.authentication.AuthenticationManager
import com.diespy.app.managers.firestore.FireStoreManager
import com.diespy.app.managers.profile.SharedPrefManager
import com.diespy.app.ui.utils.showError
import kotlinx.coroutines.launch

class CreateAccountFragment : Fragment() {

    private val authManager = AuthenticationManager()
    private val fireStoreManager = FireStoreManager()
    private var _binding: FragmentCreateAccountBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCreateAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.createAccountButton.setOnClickListener {
            handleAccountCreation()
        }

        var isPwVisible1 = false
        var isPwVisible2 = false

        binding.passwordToggle1.setOnClickListener {
            val typeface = binding.createAccountPwInput1.typeface
            val selection = binding.createAccountPwInput1.selectionEnd

            isPwVisible1 = !isPwVisible1

            binding.createAccountPwInput1.inputType = if (isPwVisible1)
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            else
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

            binding.createAccountPwInput1.typeface = typeface
            binding.createAccountPwInput1.setSelection(selection)

            val icon = if (isPwVisible1) R.drawable.icon_eye_on else R.drawable.icon_eye_off
            binding.passwordToggle1.setImageResource(icon)
        }

        binding.passwordToggle2.setOnClickListener {
            val typeface = binding.createAccountPwInput2.typeface
            val selection = binding.createAccountPwInput2.selectionEnd

            isPwVisible2 = !isPwVisible2

            binding.createAccountPwInput2.inputType = if (isPwVisible2)
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            else
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

            binding.createAccountPwInput2.typeface = typeface
            binding.createAccountPwInput2.setSelection(selection)

            val icon = if (isPwVisible2) R.drawable.icon_eye_on else R.drawable.icon_eye_off
            binding.passwordToggle2.setImageResource(icon)
        }

    }

    private fun handleAccountCreation() {
        val username = binding.createAccountUsernameInput.text.toString().trim().lowercase()
        val password1 = binding.createAccountPwInput1.text.toString().trim()
        val password2 = binding.createAccountPwInput2.text.toString().trim()

        val validationError = validateInput(username, password1, password2)
        if (validationError != null) {
            binding.createAccountErrorMessage.showError(validationError)
            return
        }

        lifecycleScope.launch {
            val userExists = authManager.checkUserExists(username)

            if (userExists == 1) {
                binding.createAccountErrorMessage.showError("Username already exists.")
                return@launch
            }

            val success = authManager.createAccount(username, password1)
            if (success) {
                val userDocumentId = fireStoreManager.getDocumentIdByField("Users", "username", username)

                if (userDocumentId != null) {
                    SharedPrefManager.saveCurrentUsername(requireContext(), username)
                    SharedPrefManager.saveCurrentUserId(requireContext(), userDocumentId)
                }

                clearFields()
                binding.createAccountErrorMessage.text = "" // Clear error message
                findNavController().navigate(R.id.action_createAccount_to_home) // Navigate to home screen
            } else {
                binding.createAccountErrorMessage.showError("Failed to create account. Try again.")
            }
        }
    }

    private fun validateInput(username: String, password1: String, password2: String): String? {
        return when {
            username.isBlank() || password1.isBlank() -> "Username and password cannot be empty"
            password1 != password2 -> "Passwords do not match"
            password1.length < 6 -> "Password must be at least 6 characters"
            username.length > 12 -> "Username must be 12 characters or smaller"
            else -> null // No errors
        }
    }

    private fun clearFields() {
        binding.createAccountUsernameInput.text.clear()
        binding.createAccountPwInput1.text.clear()
        binding.createAccountPwInput2.text.clear()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
