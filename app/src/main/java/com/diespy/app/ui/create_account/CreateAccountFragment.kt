package com.diespy.app.ui.create_account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.diespy.app.databinding.FragmentCreateAccountBinding
import com.diespy.app.managers.authentication.AuthenticationManager
import kotlinx.coroutines.launch

class CreateAccountFragment : Fragment() {

    private val authManager = AuthenticationManager()
    private var _binding: FragmentCreateAccountBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCreateAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.createAccountButton.setOnClickListener {
            val password1 = binding.createAccountPwInput1.text.toString()
            val password2 = binding.createAccountPwInput2.text.toString()
            val username = binding.createAccountUsernameInput.text.toString().lowercase()

            if (password1.isBlank() || username.isBlank()) {
                binding.createAccountErrorMessage.text = "Error: Username and Password cannot be empty"
                return@setOnClickListener
            }

            if (password1 == password2) {
                lifecycleScope.launch {
                    val userExists = authManager.checkUserExists(username)

                    if (userExists == 0) {
                        val success = authManager.createAccount(username, password1)

                        if (success) {
                            binding.createAccountErrorMessage.text = "Account Successfully Created!"
                            binding.createAccountUsernameInput.text.clear()
                            binding.createAccountPwInput1.text.clear()
                            binding.createAccountPwInput2.text.clear()
                        } else {
                            binding.createAccountErrorMessage.text = "Error: Failed to create account."
                        }
                    } else {
                        binding.createAccountUsernameInput.text.clear()
                        binding.createAccountErrorMessage.text = "Error: Username already exists"
                    }
                }
            } else {
                binding.createAccountPwInput1.text.clear()
                binding.createAccountPwInput2.text.clear()
                binding.createAccountErrorMessage.text = "Error: Passwords do not match"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
