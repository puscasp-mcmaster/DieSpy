package com.diespy.app.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.diespy.app.R
import com.diespy.app.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toCreatePartyScreenButton.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_createParty)
        }
        binding.toJoinPartyScreenButton.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_joinParty)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
