package com.diespy.app.ui.login

import android.os.Bundle
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

        // Retrieve logged in information and move to home screen if already logged in
        val savedUserId = SharedPrefManager.getLoggedInUserId(requireContext())
        if (!savedUserId.isNullOrEmpty()) {
            findNavController().navigate(R.id.action_login_to_home)
            return
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
            showError("Username and password cannot be empty")
            return
        }

        lifecycleScope.launch {
            val isAuthenticated = authManager.authenticate(username, password)

            if (isAuthenticated == 1) {

                val userDocumentId = fireStoreManager.getDocumentIdByField("Users", "username", username)

                if (userDocumentId != null) {
                    SharedPrefManager.saveUsername(requireContext(), username)
                    SharedPrefManager.saveLoggedInUserId(requireContext(), userDocumentId)

                    binding.loginErrorMessage.visibility = View.GONE
                    findNavController().navigate(R.id.action_login_to_home)
                } else {
                    showError("Error retrieving user data")
                }
            } else {
                binding.loginPwInput.text.clear()
                showError("Incorrect login, please try again")
            }
        }
    }

    private fun showError(message: String) {
        binding.loginErrorMessage.apply {
            text = message
            visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
