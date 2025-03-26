package com.diespy.app.ui.party

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.diespy.app.databinding.FragmentPartyBinding
import com.diespy.app.managers.firestore.FireStoreManager
import com.diespy.app.managers.logs.LogManager
import com.diespy.app.managers.party.PartyManager
import com.diespy.app.managers.profile.SharedPrefManager
import com.diespy.app.ui.party.TurnOrderAdapter
import com.diespy.app.ui.utils.diceParse
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class PartyFragment : Fragment() {

    private var _binding: FragmentPartyBinding? = null
    private val binding get() = _binding!!

    private val fireStoreManager = FireStoreManager()
    private lateinit var logManager: LogManager
    private lateinit var partyId: String
    private lateinit var partyManager: PartyManager
    // List of user IDs and corresponding usernames (in turn order)
    private var userIds: MutableList<String> = mutableListOf()
    private var usernames: MutableList<String> = mutableListOf()

    // Adapter for the draggable list of players
    private lateinit var turnOrderAdapter: TurnOrderAdapter

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

        partyId = SharedPrefManager.getCurrentPartyId(requireContext()) ?: run {
            binding.partyNameTextView.text = "No Party Selected"
            return
        }
        val partyName = SharedPrefManager.getCurrentPartyName(requireContext()) ?: "Party Name"
        binding.partyNameTextView.text = partyName

        // Subscribe to real-time log updates.
        partyManager.subscribeToLatestLog(partyId) { lastLog ->
            _binding?.let { binding ->
                if (lastLog != null) {
                    // Compute the roll sum.
                    val countsMap = diceParse(lastLog.log)
                    val total = countsMap.withIndex().sumOf { (index, count) -> (index + 1) * count }

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

        // Initialize RecyclerView with adapter
        turnOrderAdapter = TurnOrderAdapter(usernames, onEndTurnClicked = { handleEndTurn() })
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = turnOrderAdapter

        attachDragAndDrop()

        // Load the party's userIds from Firestore and then fetch corresponding usernames
        lifecycleScope.launch {
            val partyData = fireStoreManager.getDocumentById("Parties", partyId)
            partyData?.get("userIds")?.let { rawList ->
                @Suppress("UNCHECKED_CAST")
                userIds = (rawList as List<String>).toMutableList()
                usernames = mutableListOf()
                for (id in userIds) {
                    val userData = fireStoreManager.getDocumentById("Users", id)
                    val username = userData?.get("username") as? String ?: "Unknown"
                    usernames.add(username)
                }
                turnOrderAdapter.updatePlayers(usernames)
            }
        }

// Load previous roll info (with total displayed)
        lifecycleScope.launch {
            val logs = logManager.loadLogs(partyId)
            if (logs.isNotEmpty()) {
                val lastLog = logs.last()

                // Parse dice counts
                val countsMap = diceParse(lastLog.log)
                val total = countsMap.withIndex().sumOf { (index, count) -> (index + 1) * count }

                // Show: "Username rolled: 14"
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


        // Simulate Roll Button remains unchanged.
        binding.simulateRollButton.setOnClickListener {
            Toast.makeText(requireContext(), "Simulate Roll not implemented", Toast.LENGTH_SHORT).show()
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
                val movedUsername = usernames.removeAt(fromPos)
                usernames.add(toPos, movedUsername)
                val movedUserId = userIds.removeAt(fromPos)
                userIds.add(toPos, movedUserId)
                turnOrderAdapter.updatePlayers(usernames)
                updateTurnOrderFirestore()
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit
        })
        itemTouchHelper.attachToRecyclerView(binding.recyclerView)
    }

    private fun handleEndTurn() {
        // Rotate the list: move first element to the end.
        if (userIds.size < 2) return
        val firstUserId = userIds.removeAt(0)
        val firstUsername = usernames.removeAt(0)
        userIds.add(firstUserId)
        usernames.add(firstUsername)
        turnOrderAdapter.updatePlayers(usernames)
        updateTurnOrderFirestore()
    }

    private fun updateTurnOrderFirestore() {
        val data = mapOf("userIds" to userIds)
        lifecycleScope.launch {
            fireStoreManager.updateDocument("Parties", partyId, data)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
