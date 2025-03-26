package com.diespy.app.ui.party

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.diespy.app.R
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.diespy.app.MainActivity
import com.diespy.app.databinding.FragmentPartyBinding
import com.diespy.app.managers.firestore.FireStoreManager
import com.diespy.app.managers.logs.LogManager
import com.diespy.app.managers.party.PartyManager
import com.diespy.app.managers.profile.PartyCacheManager
import com.diespy.app.managers.profile.SharedPrefManager
import com.diespy.app.ui.utils.diceParse
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import java.util.*

class PartyFragment : Fragment() {

    private var _binding: FragmentPartyBinding? = null
    private val binding get() = _binding!!

    private val fireStoreManager = FireStoreManager()
    private lateinit var logManager: LogManager
    private lateinit var partyManager: PartyManager
    private lateinit var partyId: String

    private var userIds: MutableList<String> = mutableListOf()
    private var usernames: MutableList<String> = mutableListOf()
    private lateinit var turnOrderAdapter: TurnOrderAdapter
    private var partyMembersListener: ListenerRegistration? = null
    private var lastLocalReorderTime: Long = 0L
    private val reorderCooldownMs = 300L // adjust as needed


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPartyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        logManager = LogManager(requireContext())
        partyManager = PartyManager()

        //Throws party leave confirm  if trying to go back after joining party
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    (activity as? MainActivity)?.let { main ->
                        val navController = main.supportFragmentManager
                            .findFragmentById(R.id.nav_host_fragment)
                            ?.findNavController()
                        if (navController != null) {
                            main.showLeavePartyConfirmation(navController)
                        }
                    }
                }
            })


        partyId = SharedPrefManager.getCurrentPartyId(requireContext()) ?: run {
            binding.partyNameTextView.text = "No Party Selected"
            return
        }
        val partyName = SharedPrefManager.getCurrentPartyName(requireContext()) ?: "Party Name"
        binding.partyNameTextView.text = partyName

        partyManager.subscribeToLatestLog(partyId) { lastLog ->
            _binding?.let { binding ->
                if (lastLog != null) {
                    val countsMap = diceParse(lastLog.log)
                    val total =
                        countsMap.withIndex().sumOf { (index, count) -> (index + 1) * count }

                    binding.rollUserNameTextView.text =
                        "${lastLog.username.replaceFirstChar { it.titlecase() }} rolled: $total"
                    binding.diceDetail1.text = "1: ${countsMap[0]}"
                    binding.diceDetail2.text = "2: ${countsMap[1]}"
                    binding.diceDetail3.text = "3: ${countsMap[2]}"
                    binding.diceDetail4.text = "4: ${countsMap[3]}"
                    binding.diceDetail5.text = "5: ${countsMap[4]}"
                    binding.diceDetail6.text = "6: ${countsMap[5]}"
                } else {
                    binding.rollUserNameTextView.text = "No previous roll."
                    binding.diceDetail1.text = ""
                    binding.diceDetail2.text = ""
                    binding.diceDetail3.text = ""
                    binding.diceDetail4.text = ""
                    binding.diceDetail5.text = ""
                    binding.diceDetail6.text = ""
                }
            }
        }

        turnOrderAdapter = TurnOrderAdapter(usernames, onEndTurnClicked = { handleEndTurn() })
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = turnOrderAdapter
        attachDragAndDrop()

        partyMembersListener = partyManager.subscribeToPartyMembers(partyId) { updatedUserIds ->
            val now = System.currentTimeMillis()
            if (now - lastLocalReorderTime < reorderCooldownMs) {
                return@subscribeToPartyMembers
            }

            // Cache userIds
            userIds = updatedUserIds.toMutableList()
            PartyCacheManager.userIds = userIds

            lifecycleScope.launch {
                val updatedUsernames = mutableListOf<String>()
                val missingIds = mutableListOf<String>()

                // Load from cache or mark as missing
                for (id in userIds) {
                    val cached = PartyCacheManager.usernames[id]
                    if (cached != null) {
                        updatedUsernames.add(cached)
                    } else {
                        updatedUsernames.add("...") // Temporary placeholder
                        missingIds.add(id)
                    }
                }

                usernames = updatedUsernames
                turnOrderAdapter.updatePlayers(usernames)

                // Load missing usernames in parallel
                if (missingIds.isNotEmpty()) {
                    val resolved = missingIds.map { id ->
                        async {
                            val userData = fireStoreManager.getDocumentById("Users", id)
                            val username = userData?.get("username") as? String ?: "Unknown"
                            id to username
                        }
                    }.awaitAll()

                    // Update cache and UI
                    PartyCacheManager.usernames += resolved.toMap()
                    usernames = userIds.map { PartyCacheManager.usernames[it] ?: "Unknown" }.toMutableList()
                    turnOrderAdapter.updatePlayers(usernames)
                }

                // Subscribe to turn order after usernames loaded
                if (userIds.isNotEmpty()) {
                    partyManager.subscribeToTurnOrder(partyId, userIds) { currentTurnUserId, _ ->
                        val currentTurnIndex = userIds.indexOf(currentTurnUserId)
                        if (currentTurnIndex != -1) {
                            PartyCacheManager.currentUserId = currentTurnUserId
                            PartyCacheManager.turnIndex = currentTurnIndex
                            turnOrderAdapter.setCurrentTurnIndex(currentTurnIndex)
                        }
                    }
                }
            }
        }

        binding.simulateRollButton.setOnClickListener {
            findNavController().navigate(R.id.action_party_to_diceSim)
        }
    }


        private fun attachDragAndDrop() {
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPos = viewHolder.adapterPosition
                val toPos = target.adapterPosition
                Collections.swap(usernames, fromPos, toPos)
                Collections.swap(userIds, fromPos, toPos)
                turnOrderAdapter.notifyItemMoved(fromPos, toPos)
                return true
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)

                val currentUserId = PartyCacheManager.currentUserId ?: return
                val newTurnIndex = userIds.indexOf(currentUserId)

                val data = mapOf(
                    "userIds" to userIds,
                    "turnIndex" to newTurnIndex
                )

                lifecycleScope.launch {
                    fireStoreManager.updateDocument("Parties", partyId, data)
                    lastLocalReorderTime = System.currentTimeMillis()
                }
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit
        })
        itemTouchHelper.attachToRecyclerView(binding.recyclerView)

        binding.simulateRollButton.setOnClickListener {
            findNavController().navigate(R.id.action_party_to_diceSim)
        }
    }

    private fun handleEndTurn() {
        lifecycleScope.launch {
            partyManager.updateTurnOrder(partyId)
        }
    }

    override fun onDestroyView() {
        partyMembersListener?.remove()
        _binding = null
        super.onDestroyView()
    }
}
