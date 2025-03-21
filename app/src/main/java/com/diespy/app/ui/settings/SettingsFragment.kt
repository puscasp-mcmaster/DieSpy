package com.diespy.app.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.diespy.app.R
import com.diespy.app.databinding.FragmentSettingsBinding
import com.diespy.app.managers.profile.SharedPrefManager

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toHomeScreenButton.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_home)
        }

        binding.logoutButton.setOnClickListener {
            handleLogout()
        }

        binding.toProfileScreenButton.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_profile)
        }
    }

    private fun handleLogout() {
        SharedPrefManager.clearUserData(requireContext()) // Clears stored login info
        findNavController().navigate(R.id.action_settings_to_login) // Redirects to Login
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
