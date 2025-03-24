package com.diespy.app.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.diespy.app.R
import com.diespy.app.databinding.FragmentProfileBinding
import com.diespy.app.managers.profile.SharedPrefManager

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toSettingsButton.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_settings)
        }

        binding.toChangePasswordButton.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_changePassword)
        }

        binding.logoutButton.setOnClickListener {
            handleLogout()
        }

    }

    private fun handleLogout() {
        SharedPrefManager.clearUserData(requireContext()) // Clears stored login info
        findNavController().navigate(R.id.action_profile_to_login) // Redirects to Login
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
