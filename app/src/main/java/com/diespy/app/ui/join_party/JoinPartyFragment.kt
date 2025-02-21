package com.diespy.app.ui.join_party

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.diespy.app.R
import com.diespy.app.databinding.FragmentJoinPartyBinding

class JoinPartyFragment : Fragment() {

    private var _binding: FragmentJoinPartyBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentJoinPartyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toHomeScreenButton.setOnClickListener {
            findNavController().navigate(R.id.action_joinParty_to_home)
        }
        binding.toPartyScreenButton.setOnClickListener {
            findNavController().navigate(R.id.action_joinParty_to_party)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
