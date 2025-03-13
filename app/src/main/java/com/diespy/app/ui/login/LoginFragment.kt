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

class LoginFragment : Fragment() {

    private val authManager = AuthenticationManager()
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toHomeScreenButton.setOnClickListener {
            val username = binding.loginUsernameInput.text.toString()
            val password = binding.loginPwInput.text.toString()

            if (username.isBlank() || password.isBlank()) {
                binding.loginErrorMessage.text = "Error: Username and password cannot be empty"
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val isAuthenticated = authManager.authenticate(username, password)

                if (isAuthenticated == 1) {
                    binding.loginErrorMessage.text = "" // Clear errors
                    findNavController().navigate(R.id.action_login_to_home)
                } else {
                    binding.loginPwInput.text.clear()
                    binding.loginErrorMessage.text = "Incorrect login, please try again"
                }
            }
        }

        binding.toCreateAccountButton.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_createAccount)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
