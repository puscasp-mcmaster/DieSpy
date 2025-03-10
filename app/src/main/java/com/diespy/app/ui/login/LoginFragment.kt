package com.diespy.app.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.diespy.app.R
import com.diespy.app.databinding.FragmentLoginBinding
import com.diespy.app.managers.game.AuthenticationManager
import java.io.File
import java.security.MessageDigest

class LoginFragment : Fragment() {


    private var am: AuthenticationManager = AuthenticationManager()
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private fun String.encrypt(): String {
        val bytes = this.toByteArray()
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(bytes)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }



    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

            binding.toHomeScreenButton.setOnClickListener {
            val pw = binding.loginPwInput.text.toString()
            val username = binding.loginUsernameInput.text.toString()
            val status = am.authenticate(requireContext(),username, pw)
            if (status == 1) {
                binding.loginErrorMessage.text = ""
                findNavController().navigate(R.id.action_login_to_home)
            }
            binding.loginPwInput.text.clear() // Clear the password field
            binding.loginErrorMessage.text = "Incorrect login, please try again" // Set the error message



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
