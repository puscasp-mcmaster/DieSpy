package com.diespy.app.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.diespy.app.R
import com.diespy.app.databinding.FragmentHomeBinding
import com.diespy.app.managers.firestore.FireStoreManager
import com.diespy.app.managers.profile.SharedPrefManager
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val fireStoreManager = FireStoreManager()
    private lateinit var partyAdapter: PartyListAdapter
    private val partyList = mutableListOf<Pair<String, String>>() // Pair<PartyId, PartyName>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        partyAdapter = PartyListAdapter(partyList) { partyId ->
            SharedPrefManager.saveCurrentParty(requireContext(), partyId)
            findNavController().navigate(R.id.action_home_to_party)
        }

        binding.partyRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.partyRecyclerView.adapter = partyAdapter

        loadUserParties()

        binding.toCreatePartyScreenButton.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_createParty)
        }
        binding.toJoinPartyScreenButton.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_joinParty)
        }
    }

    private fun loadUserParties() {
        val userId = SharedPrefManager.getLoggedInUserId(requireContext()) ?: return

        lifecycleScope.launch {
            val parties = fireStoreManager.getAllPartiesForUser(userId)
            partyList.clear()
            partyList.addAll(parties)
            partyAdapter.notifyDataSetChanged()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    class PartyListAdapter(
        private val items: List<Pair<String, String>>, // Pair<PartyId, PartyName>
        private val onItemClick: (String) -> Unit
    ) : RecyclerView.Adapter<PartyListAdapter.PartyViewHolder>() {

        inner class PartyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val nameText: TextView = itemView.findViewById(R.id.partyNameText)

            init {
                itemView.setOnClickListener {
                    val partyId = items[adapterPosition].first
                    onItemClick(partyId)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PartyViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_party, parent, false)
            return PartyViewHolder(view)
        }

        override fun onBindViewHolder(holder: PartyViewHolder, position: Int) {
            holder.nameText.text = items[position].second
        }

        override fun getItemCount() = items.size
    }
}
