package com.diespy.app.ui.login

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.diespy.app.R
import com.diespy.app.databinding.FragmentLoginBinding
import com.diespy.app.managers.authentication.AuthenticationManager
import kotlinx.coroutines.launch
import com.diespy.app.managers.firestore.FireStoreManager
import com.diespy.app.managers.profile.SharedPrefManager
import com.diespy.app.ui.utils.showError

class LoginFragment : Fragment() {

    private val authManager = AuthenticationManager()
    private val fireStoreManager = FireStoreManager()
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //Retrieve logged in information and move to home screen if already logged in
        val savedUserId = SharedPrefManager.getCurrentUserId(requireContext())
        if (!savedUserId.isNullOrEmpty()) {
            findNavController().navigate(R.id.action_login_to_home)
            return
        }

        var isPasswordVisible = false

        //Handling password view
        binding.passwordToggle.setOnClickListener {
            val selection = binding.loginPwInput.selectionEnd
            val typeface = binding.loginPwInput.typeface

            isPasswordVisible = !isPasswordVisible

            binding.loginPwInput.inputType = if (isPasswordVisible) {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }

            binding.loginPwInput.typeface = typeface
            binding.loginPwInput.setSelection(selection)

            val icon = if (isPasswordVisible) R.drawable.icon_eye_on else R.drawable.icon_eye_off
            binding.passwordToggle.setImageResource(icon)
        }

        binding.toHomeScreenButton.setOnClickListener {
            handleLogin()
        }

        binding.toCreateAccountButton.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_createAccount)
        }
    }

    private fun handleLogin() {
        val username = binding.loginUsernameInput.text.toString().trim().lowercase()
        val password = binding.loginPwInput.text.toString().trim()

        if (username.isBlank() || password.isBlank()) {
            binding.loginErrorMessage.showError("Username and password are empty")
            return
        }

        lifecycleScope.launch {
            val isAuthenticated = authManager.authenticate(username, password)

            if (isAuthenticated == 1) {

                val userDocumentId = fireStoreManager.getDocumentIdByField("Users", "username", username)

                if (userDocumentId != null) {
                    SharedPrefManager.saveCurrentUsername(requireContext(), username)
                    SharedPrefManager.saveCurrentUserId(requireContext(), userDocumentId)

                    binding.loginErrorMessage.visibility = View.GONE
                    findNavController().navigate(R.id.action_login_to_home)
                } else {
                    binding.loginErrorMessage.showError("Error retrieving user data")
                }
            } else {
                binding.loginPwInput.text.clear()
                binding.loginErrorMessage.showError("Incorrect login, please try again")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
