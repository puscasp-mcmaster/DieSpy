package com.diespy.app.ui.create_account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.diespy.app.databinding.FragmentCreateAccountBinding
import com.diespy.app.managers.game.AuthenticationManager
import java.security.MessageDigest

class CreateAccountFragment : Fragment() {

    private var am: AuthenticationManager = AuthenticationManager()
    private var _binding: FragmentCreateAccountBinding? = null
    private val binding get() = _binding!!


    private fun String.encrypt(): String {
        val bytes = this.toByteArray()
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(bytes)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCreateAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.createAccountButton.setOnClickListener {
            val pw = binding.createAccountPwInput1.text.toString()
            val pw2 = binding.createAccountPwInput2.text.toString()
            val username = binding.createAccountUsernameInput.text.toString()

            if (pw == pw2) {
                val status = am.checkUserExists(requireContext(),username)

                if (status == 0) {
                    binding.createAccountErrorMessage.text = "Account Successfully Created!"
                    am.createAccount(requireContext(),username, pw)
                }
                else {
                    binding.createAccountUsernameInput.text.clear()
                    binding.createAccountErrorMessage.text =
                        "Error: Account with that username exists" // Set the error message
                }
            }

            else {
                binding.createAccountPwInput1.text.clear() // Clear the password field
                binding.createAccountPwInput2.text.clear() // Clear the password field
                binding.createAccountErrorMessage.text = "Error: Passwords do not match" // Set the error message
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
