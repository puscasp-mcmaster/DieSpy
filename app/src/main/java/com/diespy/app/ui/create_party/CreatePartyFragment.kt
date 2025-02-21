package com.diespy.app.ui.create_party

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.diespy.app.R
import com.diespy.app.databinding.FragmentCreatePartyBinding

class CreatePartyFragment : Fragment() {
    private var _binding: FragmentCreatePartyBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreatePartyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toPartyScreenButton.setOnClickListener {
            findNavController().navigate(R.id.action_createParty_to_party)
        }

        binding.toHomeScreenButton.setOnClickListener {
            findNavController().navigate(R.id.action_createParty_to_home)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
